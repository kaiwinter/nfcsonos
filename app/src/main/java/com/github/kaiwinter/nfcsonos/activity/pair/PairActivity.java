package com.github.kaiwinter.nfcsonos.activity.pair;

import android.content.Intent;
import android.nfc.FormatException;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.ActivityPairBinding;
import com.github.kaiwinter.nfcsonos.rest.APIError;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.favorite.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.favorite.model.Favorites;
import com.github.kaiwinter.nfcsonos.rest.favorite.model.Item;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesTokenStore;
import com.google.android.material.snackbar.Snackbar;

import be.appfoundry.nfclibrary.activities.NfcActivity;
import be.appfoundry.nfclibrary.exceptions.InsufficientCapacityException;
import be.appfoundry.nfclibrary.exceptions.ReadOnlyTagException;
import be.appfoundry.nfclibrary.exceptions.TagNotPresentException;
import be.appfoundry.nfclibrary.utilities.sync.NfcWriteUtilityImpl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PairActivity extends NfcActivity {

    private ActivityPairBinding binding;

    private MaterialDialog dialog;

    private SharedPreferencesTokenStore tokenstore;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenstore = new SharedPreferencesTokenStore(this);
        accessTokenManager = new AccessTokenManager(this);

        binding = ActivityPairBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        loadFavorites();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFavorites() {
        if (refreshTokenIfNeeded(this::loadFavorites)) {
            return;
        }
        binding.pairLoadingContainer.setVisibility(View.VISIBLE);
        binding.pairLoadingStatus.setText(R.string.loading_favorites);

        String accessToken = tokenstore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        service.loadFavorites(tokenstore.getHouseholdId()).enqueue(new Callback<Favorites>() {
            @Override
            public void onResponse(Call<Favorites> call, Response<Favorites> response) {
                if (response.isSuccessful()) {
                    Favorites favorites = response.body();

                    runOnUiThread(() -> {
                        binding.spinner.setItems(favorites.items);
                        hideLoading("Bereit");
                    });
                } else {
                    APIError apiError = ServiceFactory.parseError(response);
                    String message = getString(R.string.login_error, apiError.error + " (" + response.code() + ", " + apiError.errorDescription + ")");
                    hideLoading(message);
                }
            }

            @Override
            public void onFailure(Call<Favorites> call, Throwable t) {
                hideLoading("Fehler: " + t.getMessage());
            }
        });
    }

    public void writeTag(View view) {

        dialog = new MaterialDialog(this, MaterialDialog.getDEFAULT_BEHAVIOR())
                .title(null, "Scan tag")
                .message(null, "This will write a link to '" + getSelectedFavorite().name + "' on the tag. Scanning the tag later on will start this favorite.", null)
                .negativeButton(null, "Cancel", null);

        dialog.show();
    }

    private Item getSelectedFavorite() {
        return (Item) binding.spinner.getItems().get(binding.spinner.getSelectedIndex());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        if (binding.spinner.getSelectedIndex() < 0) {
            Snackbar.make(binding.coordinator, "No selection", Snackbar.LENGTH_LONG).show();
            return;
        }

        try {
            new NfcWriteUtilityImpl().writeTextToTagFromIntent(getSelectedFavorite().id, getIntent());
            Snackbar.make(binding.coordinator, "Tag successfully written", Snackbar.LENGTH_LONG).show();
        } catch (FormatException | ReadOnlyTagException | InsufficientCapacityException | TagNotPresentException e) {
            Log.e("PairActivity", Log.getStackTraceString(e));
            Snackbar.make(binding.coordinator, "Error writing tag", Snackbar.LENGTH_LONG).show();
        } finally {
            dialog.dismiss();
        }
    }

    private void hideLoading(String statusMessage) {
        runOnUiThread(() -> {
            binding.pairLoadingContainer.setVisibility(View.INVISIBLE);
            binding.pairLoadingStatus.setText(statusMessage);
        });
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            binding.pairLoadingStatus.setText(R.string.refresh_access_token);
            accessTokenManager.refreshAccessToken(this, runnable, this::hideLoading);
            return true;
        }
        return false;
    }
}
