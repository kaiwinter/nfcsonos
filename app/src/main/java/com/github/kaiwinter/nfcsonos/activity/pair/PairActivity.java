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
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.favorite.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.favorite.model.Favorites;
import com.github.kaiwinter.nfcsonos.rest.favorite.model.Item;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesTokenStore;
import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.materialspinner.MaterialSpinner;

import be.appfoundry.nfclibrary.activities.NfcActivity;
import be.appfoundry.nfclibrary.exceptions.InsufficientCapacityException;
import be.appfoundry.nfclibrary.exceptions.ReadOnlyTagException;
import be.appfoundry.nfclibrary.exceptions.TagNotPresentException;
import be.appfoundry.nfclibrary.utilities.sync.NfcWriteUtilityImpl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PairActivity extends NfcActivity {

    private static final String TAG = PairActivity.class.getSimpleName();

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

        MaterialSpinner spinner = binding.spinner;
        spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<Item>() {

            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Item item) {
                Snackbar.make(view, "Clicked " + item.toString(), Snackbar.LENGTH_LONG).show();
            }
        });

        ActionBar supportActionBar = getSupportActionBar();
        //supportActionBar.setTitle("moduleName");
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

    @Override
    public void onResume() {
        super.onResume();
    }

    private void loadFavorites() {
        if (refreshTokenIfNeeded(this::loadFavorites)) {
            return;
        }
        binding.pairLoadingContainer.setVisibility(View.VISIBLE);
        binding.pairLoadingStatus.setText("Lade Favoriten");

        String accessToken = tokenstore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        service.loadFavorites(getString(R.string.household)).enqueue(new Callback<Favorites>() {
            @Override
            public void onResponse(Call<Favorites> call, Response<Favorites> response) {
                int status = response.code();
                if (status != 200) {
                    // FIXME KW: response.message() durch fehler body ersetzen
                    Snackbar.make(binding.coordinator, "Error: " + response.message(), Snackbar.LENGTH_LONG).show();
                    hideLoading("Bereit");
                    return;
                }

                Favorites favorites = response.body();

                runOnUiThread(() -> {
                    binding.spinner.setItems(favorites.items);
                    hideLoading("Bereit");
                });
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
        Log.d(TAG, "onNewIntent");

        if (dialog == null || !dialog.isShowing()) {
            Log.d(TAG, "onNewIntent - cancel - dialog not showing");
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
            binding.pairLoadingStatus.setText("Refresh Access Token");
            accessTokenManager.refreshAccessToken(this, runnable, this::hideLoading);
            return true;
        }
        return false;
    }
}
