package com.github.kaiwinter.nfcsonos.activity.discover;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.activity.main.MainActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityDiscoverBinding;
import com.github.kaiwinter.nfcsonos.rest.DiscoverService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.Group;
import com.github.kaiwinter.nfcsonos.rest.model.Groups;
import com.github.kaiwinter.nfcsonos.rest.model.Household;
import com.github.kaiwinter.nfcsonos.rest.model.Households;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.Collections;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverActivity extends AppCompatActivity {

    private ActivityDiscoverBinding binding;

    @Inject
    SharedPreferencesStore sharedPreferencesStore;

    @Inject
    AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDiscoverBinding.inflate(getLayoutInflater());
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

        DiscoverService service = ServiceFactory.createDiscoverService(sharedPreferencesStore.getAccessToken());
        Call<Households> household = service.getHouseholds();
        household.enqueue(new Callback<Households>() {
            @Override
            public void onResponse(Call<Households> call, Response<Households> response) {
                if (response.isSuccessful()) {
                    Households body = response.body();
                    binding.household.setItems(body.households);
                    if (body.households == null || body.households.isEmpty()) {
                        hideLoadingState(getString(R.string.no_household_found));
                        return;
                    }
                    loadGroups(body.households.get(0).id);
                } else {
                    String message = ServiceFactory.handleError(DiscoverActivity.this, response);
                    hideLoadingState(message);
                }
            }

            @Override
            public void onFailure(Call<Households> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    private void loadGroups(String householdId) {
        displayLoading(getString(R.string.loading_groups));
        binding.group.setItems(Collections.emptyList());

        if (refreshTokenIfNeeded(this::loadHouseholds)) {
            return;
        }

        DiscoverService service = ServiceFactory.createDiscoverService(sharedPreferencesStore.getAccessToken());
        Call<Groups> group = service.getGroups(householdId);
        group.enqueue(new Callback<Groups>() {
            @Override
            public void onResponse(Call<Groups> call, Response<Groups> response) {
                if (response.isSuccessful()) {
                    Groups body = response.body();
                    if (body.groups == null || body.groups.isEmpty()) {
                        hideLoadingState(getString(R.string.no_group_found));
                        return;
                    }
                    binding.group.setItems(body.groups);
                    binding.selectButton.setEnabled(true);
                } else {
                    String message = ServiceFactory.handleError(DiscoverActivity.this, response);
                    hideLoadingState(message);
                }
                hideLoadingState(null);
            }

            @Override
            public void onFailure(Call<Groups> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    public void selectHouseholdAndGroup(View view) {
        if (binding.household.getItems() == null || binding.group.getItems() == null) {
            return;
        }
        Household selectedHousehold = (Household) binding.household.getItems().get(binding.household.getSelectedIndex());
        Group selectedGroup = (Group) binding.group.getItems().get(binding.group.getSelectedIndex());
        if (selectedHousehold == null) {
            hideLoadingState(getString(R.string.no_household_selected));
            return;
        } else if (selectedGroup == null) {
            hideLoadingState(getString(R.string.no_group_selected));
            return;
        }

        sharedPreferencesStore.setHouseholdAndGroup(selectedHousehold.id, selectedGroup.id);
        switchToMainActivity();
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading(getString(R.string.refresh_access_token));
            accessTokenManager.refreshAccessToken(this, runnable, this::hideLoadingState);
            return true;
        }
        return false;
    }

    private void switchToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
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

    private void hideLoadingState(String errormessage) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(errormessage)) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorDescription.setText(errormessage);
            }
            binding.loadingContainer.setVisibility(View.INVISIBLE);
            binding.authContainer.setEnabled(true);
        });
    }
}
