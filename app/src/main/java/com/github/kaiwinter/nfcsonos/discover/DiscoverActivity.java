package com.github.kaiwinter.nfcsonos.discover;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import android.widget.ArrayAdapter;

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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverActivity extends AppCompatActivity {

    private ActivityDiscoverBinding binding;

    private SharedPreferencesStore sharedPreferencesStore;
    private ServiceFactory serviceFactory;

    private RetryAction retryAction; // Contains a RetryAction, which is just passed back

    private Households households;
    private Groups groups;

    private int selectedHouseholdIndex = -1;
    private int selectedGroupIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        retryAction = getIntent().getParcelableExtra(RetryAction.class.getSimpleName());

        sharedPreferencesStore = new SharedPreferencesStore(this);
        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        serviceFactory = new ServiceFactory(ServiceFactory.API_ENDPOINT, accessTokenManager);

        binding = ActivityDiscoverBinding.inflate(getLayoutInflater());
        binding.selectButton.setOnClickListener(__ -> selectHouseholdAndGroup());
        setContentView(binding.getRoot());

        binding.household.setOnItemClickListener((parent, view, position, id) -> {
            selectedHouseholdIndex = position;
            loadGroups(households.households.get(position).id);
        });

        binding.group.setOnItemClickListener((parent, view, position, id) -> {
            selectedGroupIndex = position;
            binding.selectButton.setEnabled(true);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        loadHouseholds();
    }

    private void loadHouseholds() {
        displayLoading(getString(R.string.loading_households));

        DiscoverService service = serviceFactory.createDiscoverService(this::displayRefreshLoadingState);
        Call<Households> household = service.getHouseholds();
        household.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Households> call, Response<Households> response) {
                if (response.isSuccessful()) {
                    households = response.body();
                    ArrayAdapter<Household> adapter = new ArrayAdapter<>(DiscoverActivity.this, android.R.layout.simple_dropdown_item_1line, households.households);
                    binding.household.setAdapter(adapter);

                    if (households.households == null || households.households.isEmpty()) {
                        UserMessage userMessage = UserMessage.create(R.string.no_household_found);
                        hideLoadingState(userMessage);
                        return;
                    }
                    selectedHouseholdIndex = 0;
                    binding.household.setText(households.households.get(0).toString(), false);
                    loadGroups(households.households.get(0).id);
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

        DiscoverService service = serviceFactory.createDiscoverService(this::displayRefreshLoadingState);
        Call<Groups> group = service.getGroups(householdId);
        group.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Groups> call, Response<Groups> response) {
                if (response.isSuccessful()) {
                    groups = response.body();
                    if (isNullOrEmpty(groups.groups)) {
                        UserMessage userMessage = UserMessage.create(R.string.no_group_found);
                        hideLoadingState(userMessage);
                        return;
                    }

                    ArrayAdapter<Group> adapter = new ArrayAdapter<>(DiscoverActivity.this, android.R.layout.simple_dropdown_item_1line, groups.groups);
                    binding.group.setAdapter(adapter);

                    var currentlySelectedGroup = sharedPreferencesStore.getGroupCoordinatorId();
                    for (int i=0; i<groups.groups.size(); i++) {
                        if (Objects.equals(groups.groups.get(i).coordinatorId, currentlySelectedGroup)) {
                            selectedGroupIndex = i;
                            binding.group.setText(groups.groups.get(i).toString(), false);
                            break;
                        }
                    }
                    binding.selectButton.setEnabled(selectedGroupIndex != -1);
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
        if (households == null || groups == null || selectedHouseholdIndex < 0 || selectedGroupIndex < 0) {
            return;
        }
        Household selectedHousehold = households.households.get(selectedHouseholdIndex);
        Group selectedGroup = groups.groups.get(selectedGroupIndex);
        if (selectedHousehold == null) {
            UserMessage userMessage = UserMessage.create(R.string.no_household_selected);
            hideLoadingState(userMessage);
            return;
        } else if (selectedGroup == null) {
            UserMessage userMessage = UserMessage.create(R.string.no_group_selected);
            hideLoadingState(userMessage);
            return;
        }

        sharedPreferencesStore.setHouseholdAndGroup(selectedHousehold.id, selectedGroup.id, selectedGroup.coordinatorId, selectedHousehold.name, selectedGroup.name);
        switchToMainActivity();
    }

    private void switchToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        if (retryAction != null) {
            intent.putExtra(RetryAction.class.getSimpleName(), retryAction);
        }
        startActivity(intent);
        finish();
    }

    private void displayRefreshLoadingState() {
        displayLoading(getString(R.string.refresh_access_token));
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

    private boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
