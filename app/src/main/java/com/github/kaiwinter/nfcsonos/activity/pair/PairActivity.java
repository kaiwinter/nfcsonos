package com.github.kaiwinter.nfcsonos.activity.pair;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.databinding.ActivityPairBinding;
import com.github.kaiwinter.nfcsonos.nfc.NfcPayload;
import com.github.kaiwinter.nfcsonos.nfc.NfcPayloadUtil;
import com.github.kaiwinter.nfcsonos.rest.model.Item;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

public class PairActivity extends AppCompatActivity {

    private ActivityPairBinding binding;
    private MaterialDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPairBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        FavoriteCache favoriteCache = new FavoriteCache(getApplicationContext());

        displayLoading(getString(R.string.loading_favorites));
        favoriteCache.loadFavorites(favorites -> runOnUiThread(() -> {
            binding.spinner.setItems(favorites.items);
            hideLoadingState(null);
        }), this::hideLoadingState);
    }

    @Override
    public void onResume() {
        super.onResume();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void writeTag(View view) {

        dialog = new MaterialDialog(this, MaterialDialog.getDEFAULT_BEHAVIOR())
                .title(null, getString(R.string.scan_tag))
                .message(null, getString(R.string.link_tag_message, getSelectedFavorite().name), null)
                .negativeButton(null, getString(R.string.cancel), null);

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
            Snackbar.make(binding.coordinator, R.string.no_selection, Snackbar.LENGTH_LONG).show();
            return;
        }

        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tagFromIntent == null) {
            return;
        }

        try (Ndef ndef = Ndef.get(tagFromIntent)) {
            ndef.connect();
            NfcPayload nfcPayload = new NfcPayload(getSelectedFavorite().id);
            NdefMessage ndefMessage = NfcPayloadUtil.createMessage(nfcPayload);

            ndef.writeNdefMessage(ndefMessage);
            Snackbar.make(binding.coordinator, R.string.tag_written, Snackbar.LENGTH_LONG).show();

        } catch (FormatException | IOException e) {
            Snackbar.make(binding.coordinator, getString(R.string.tag_written_error, e.getMessage()), Snackbar.LENGTH_LONG).show();
        } finally {
            dialog.dismiss();
        }
    }

    private void displayLoading(String loadingMessage) {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.VISIBLE);
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
            binding.loadingContainer.setVisibility(View.GONE);
        });
    }
}
