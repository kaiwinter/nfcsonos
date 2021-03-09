package com.github.kaiwinter.nfcsonos.activity.discover;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.activity.main.TokenActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityDiscoverBinding;
import com.github.kaiwinter.nfcsonos.rest.APIError;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.discover.DiscoverService;
import com.github.kaiwinter.nfcsonos.rest.discover.model.Group;
import com.github.kaiwinter.nfcsonos.rest.discover.model.Groups;
import com.github.kaiwinter.nfcsonos.rest.discover.model.Household;
import com.github.kaiwinter.nfcsonos.rest.discover.model.Households;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesTokenStore;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverActivity extends AppCompatActivity {

    private ActivityDiscoverBinding binding;

    private SharedPreferencesTokenStore tokenstore;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenstore = new SharedPreferencesTokenStore(this);
        accessTokenManager = new AccessTokenManager(this);

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
        displayLoading("Loading Households");

        if (refreshTokenIfNeeded(this::loadHouseholds)) {
            return;
        }

        DiscoverService service = ServiceFactory.createDiscoverService(tokenstore.getAccessToken());
        Call<Households> household = service.getHouseholds();
        household.enqueue(new Callback<Households>() {
            @Override
            public void onResponse(Call<Households> call, Response<Households> response) {
                if (response.code() == 200) {
                    Households body = response.body();
                    binding.household.setItems(body.households);
                    loadGroups(body.households.get(0).id);
                } else {
                    APIError apiError = ServiceFactory.parseError(response);
                    String message = getString(R.string.login_error, apiError.error + " (" + response.code() + ", " + apiError.errorDescription + ")");
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
        displayLoading("Loading Groups");
        binding.group.setItems(Collections.emptyList());

        if (refreshTokenIfNeeded(this::loadHouseholds)) {
            return;
        }

        DiscoverService service = ServiceFactory.createDiscoverService(tokenstore.getAccessToken());
        Call<Groups> group = service.getGroups(householdId);
        group.enqueue(new Callback<Groups>() {
            @Override
            public void onResponse(Call<Groups> call, Response<Groups> response) {
                if (response.code() == 200) {
                    binding.group.setItems(response.body().groups);
                } else {
                    APIError apiError = ServiceFactory.parseError(response);
                    String message = getString(R.string.login_error, apiError.error + " (" + response.code() + ", " + apiError.errorDescription + ")");
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
        Household selectedHousehold = (Household) binding.household.getItems().get(binding.household.getSelectedIndex());
        Group selectedGroup = (Group) binding.group.getItems().get(binding.group.getSelectedIndex());
        if (selectedHousehold == null) {
            hideLoadingState("No Household selected");
            return;
        } else if (selectedGroup == null) {
            hideLoadingState("No Group selected");
            return;
        }

        tokenstore.setHouseholdAndGroup(selectedHousehold.id, selectedGroup.id);
        switchToMainActivity();
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading("Refreshing access token");
            accessTokenManager.refreshAccessToken(this, runnable, this::hideLoadingState);
            return true;
        }
        return false;
    }

    private void switchToMainActivity() {
        startActivity(new Intent(this, TokenActivity.class));
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