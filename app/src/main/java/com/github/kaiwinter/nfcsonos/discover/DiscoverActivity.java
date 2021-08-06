package com.github.kaiwinter.nfcsonos.discover;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.ActivityDiscoverBinding;
import com.github.kaiwinter.nfcsonos.main.MainActivity;
import com.github.kaiwinter.nfcsonos.main.model.RetryAction;
import com.github.kaiwinter.nfcsonos.rest.DiscoverService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.Group;
import com.github.kaiwinter.nfcsonos.rest.model.Groups;
import com.github.kaiwinter.nfcsonos.rest.model.Household;
import com.github.kaiwinter.nfcsonos.rest.model.Households;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.UserMessage;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverActivity extends AppCompatActivity {

    private ActivityDiscoverBinding binding;

    private SharedPreferencesStore sharedPreferencesStore;
    private AccessTokenManager accessTokenManager;
    private ServiceFactory serviceFactory;

    private RetryAction retryAction; // Contains a RetryAction, which is just passed back

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        retryAction = getIntent().getParcelableExtra(RetryAction.class.getSimpleName());

        sharedPreferencesStore = new SharedPreferencesStore(this);
        accessTokenManager = new AccessTokenManager(this);
        serviceFactory = new ServiceFactory(ServiceFactory.API_ENDPOINT);

        binding = ActivityDiscoverBinding.inflate(getLayoutInflater());
        binding.selectButton.setOnClickListener(__ -> selectHouseholdAndGroup());
        setContentView(binding.getRoot());

        binding.household.setOnItemSelectedListener((MaterialSpinner.OnItemSelectedListener<Household>) (view, position, id, item) ->
                loadGroups(item.id)
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        loadHouseholds();
    }

    private void loadHouseholds() {
        displayLoading(getString(R.string.loading_households));

        if (refreshTokenIfNeeded(this::loadHouseholds)) {
            return;
        }

        DiscoverService service = serviceFactory.createDiscoverService(sharedPreferencesStore.getAccessToken());
        Call<Households> household = service.getHouseholds();
        household.enqueue(new Callback<Households>() {
            @Override
            public void onResponse(Call<Households> call, Response<Households> response) {
                if (response.isSuccessful()) {
                    Households body = response.body();
                    binding.household.setItems(body.households);
                    if (body.households == null || body.households.isEmpty()) {
                        UserMessage userMessage = UserMessage.create(R.string.no_household_found);
                        hideLoadingState(userMessage);
                        return;
                    }
                    loadGroups(body.households.get(0).id);
                } else {
                    APIError apiError = ServiceFactory.parseError(response);
                    UserMessage userMessage = UserMessage.create(apiError);
                    hideLoadingState(userMessage);
                }
            }

            @Override
            public void onFailure(Call<Households> call, Throwable t) {
                UserMessage userMessage = UserMessage.create(t.getMessage());
                hideLoadingState(userMessage);
            }
        });
    }

    private void loadGroups(String householdId) {
        displayLoading(getString(R.string.loading_groups));
        binding.group.setItems(Collections.emptyList());

        if (refreshTokenIfNeeded(this::loadHouseholds)) {
            return;
        }

        DiscoverService service = serviceFactory.createDiscoverService(sharedPreferencesStore.getAccessToken());
        Call<Groups> group = service.getGroups(householdId);
        group.enqueue(new Callback<Groups>() {
            @Override
            public void onResponse(Call<Groups> call, Response<Groups> response) {
                if (response.isSuccessful()) {
                    Groups body = response.body();
                    if (body.groups == null || body.groups.isEmpty()) {
                        UserMessage userMessage = UserMessage.create(R.string.no_group_found);
                        hideLoadingState(userMessage);
                        return;
                    }
                    binding.group.setItems(body.groups);
                    binding.selectButton.setEnabled(true);
                } else {
                    APIError apiError = ServiceFactory.parseError(response);
                    UserMessage userMessage = UserMessage.create(apiError);
                    hideLoadingState(userMessage);
                }
                hideLoadingState();
            }

            @Override
            public void onFailure(Call<Groups> call, Throwable t) {
                UserMessage userMessage = UserMessage.create(t.getMessage());
                hideLoadingState(userMessage);
            }
        });
    }

    public void selectHouseholdAndGroup() {
        if (binding.household.getItems() == null || binding.group.getItems() == null) {
            return;
        }
        Household selectedHousehold = (Household) binding.household.getItems().get(binding.household.getSelectedIndex());
        Group selectedGroup = (Group) binding.group.getItems().get(binding.group.getSelectedIndex());
        if (selectedHousehold == null) {
            UserMessage userMessage = UserMessage.create(R.string.no_household_selected);
            hideLoadingState(userMessage);
            return;
        } else if (selectedGroup == null) {
            UserMessage userMessage = UserMessage.create(R.string.no_group_selected);
            hideLoadingState(userMessage);
            return;
        }

        sharedPreferencesStore.setHouseholdAndGroup(selectedHousehold.id, selectedGroup.id);
        switchToMainActivity();
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading(getString(R.string.refresh_access_token));
            accessTokenManager.refreshAccessToken(runnable, this::hideLoadingState);
            return true;
        }
        return false;
    }

    private void switchToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        if (retryAction != null) {
            intent.putExtra(RetryAction.class.getSimpleName(), retryAction);
        }
        startActivity(intent);
        finish();
    }

    private void displayLoading(String loadingMessage) {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.VISIBLE);
            binding.authContainer.setEnabled(false);

            binding.loadingDescription.setText(loadingMessage);
            binding.errorContainer.setVisibility(View.GONE);
        });
    }

    private void hideLoadingState() {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.INVISIBLE);
            binding.authContainer.setEnabled(true);
        });
    }

    private void hideLoadingState(UserMessage userMessage) {
        String message = userMessage.getMessage(this);
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(message)) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorDescription.setText(message);
            }
            binding.loadingContainer.setVisibility(View.INVISIBLE);
            binding.authContainer.setEnabled(true);
        });
    }
}
