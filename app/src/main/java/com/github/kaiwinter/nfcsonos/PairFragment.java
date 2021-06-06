package com.github.kaiwinter.nfcsonos;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.kaiwinter.nfcsonos.databinding.FragmentPairBinding;
import com.github.kaiwinter.nfcsonos.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.nfc.NfcPayload;
import com.github.kaiwinter.nfcsonos.nfc.NfcPayloadUtil;
import com.github.kaiwinter.nfcsonos.rest.model.Item;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

public class PairFragment extends Fragment {

    private FragmentPairBinding binding;
    private MaterialDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentPairBinding.inflate(getLayoutInflater());
        FavoriteCache favoriteCache = new FavoriteCache(getActivity());

        displayLoading(getString(R.string.loading_favorites));
        favoriteCache.loadFavorites(favorites -> getActivity().runOnUiThread(() -> {
            binding.spinner.setItems(favorites.items);
            hideLoadingState(null);
            binding.linkButton.setEnabled(true);
        }), this::hideLoadingState);

        binding.linkButton.setOnClickListener(__ -> writeTag());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
//        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
//        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
//        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
//        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void writeTag() {

        dialog = new MaterialDialog(getActivity(), MaterialDialog.getDEFAULT_BEHAVIOR())
                .title(null, getString(R.string.scan_tag))
                .message(null, getString(R.string.link_tag_message, getSelectedFavorite().name), null)
                .negativeButton(null, getString(R.string.cancel), null);

        dialog.show();
    }

    private Item getSelectedFavorite() {
        return (Item) binding.spinner.getItems().get(binding.spinner.getSelectedIndex());
    }

    public void handleIntent(Intent intent) {

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
        getActivity().runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.VISIBLE);
            binding.loadingDescription.setText(loadingMessage);
            binding.errorContainer.setVisibility(View.GONE);
        });
    }

    private void hideLoadingState(String errormessage) {
        getActivity().runOnUiThread(() -> {
            if (!TextUtils.isEmpty(errormessage)) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorDescription.setText(errormessage);
            }
            binding.loadingContainer.setVisibility(View.GONE);
        });
    }
}