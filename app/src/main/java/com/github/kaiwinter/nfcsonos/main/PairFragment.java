package com.github.kaiwinter.nfcsonos.main;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.FragmentPairBinding;
import com.github.kaiwinter.nfcsonos.main.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.main.nfc.NfcPayload;
import com.github.kaiwinter.nfcsonos.main.nfc.NfcPayloadUtil;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.Item;
import com.github.kaiwinter.nfcsonos.util.SnackbarUtil;
import com.github.kaiwinter.nfcsonos.util.UserMessage;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

public class PairFragment extends Fragment {

    private FragmentPairBinding binding;
    private AlertDialog pairDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentPairBinding.inflate(getLayoutInflater());

        FavoriteCache favoriteCache = new FavoriteCache(getActivity(), new ServiceFactory(ServiceFactory.API_ENDPOINT));

        displayLoading(getString(R.string.loading_favorites));
        favoriteCache.loadFavorites(favorites -> {
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                        hideLoadingState();
                        if (favorites.items.isEmpty()) {
                            binding.noSonosFavoriteHint.setVisibility(View.VISIBLE);
                        } else {
                            binding.spinner.setItems(favorites.items);
                            binding.linkButton.setEnabled(true);
                        }
                    }
            );
        }, this::hideLoadingState);

        binding.linkButton.setOnClickListener(__ -> writeTag());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return binding.getRoot();
    }

    public void writeTag() {
        pairDialog = new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.scan_tag)
                .setMessage(getString(R.string.pair_tag_message, getSelectedFavorite().name))
                .setPositiveButton(getString(R.string.cancel), null)
                .create();

        pairDialog.show();
    }

    private Item getSelectedFavorite() {
        return (Item) binding.spinner.getItems().get(binding.spinner.getSelectedIndex());
    }

    /**
     * Returns true when the user clicked the pairing button previously and the pairing dialog is currently shown.
     *
     * @return true when the pairing dialog is currently shown, else false
     */
    public boolean isPairingActive() {
        return pairDialog != null && pairDialog.isShowing();
    }

    public void handleNfcIntent(Intent intent) {

        if (!isPairingActive()) {
            return;
        }

        if (binding.spinner.getSelectedIndex() < 0) {
            SnackbarUtil.createAndShowSnackbarAboveBottomNav(getActivity(), binding.coordinator, R.string.no_selection, Snackbar.LENGTH_LONG);
            return;
        }

        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tagFromIntent == null) {
            return;
        }

        try (Ndef ndef = Ndef.get(tagFromIntent)) {
            if (ndef == null) {
                SnackbarUtil.createAndShowSnackbarAboveBottomNav(getActivity(), binding.coordinator, R.string.tag_invalid, Snackbar.LENGTH_LONG);
                return;
            }
            ndef.connect();
            NfcPayload nfcPayload = new NfcPayload(getSelectedFavorite().id);
            NdefMessage ndefMessage = NfcPayloadUtil.createMessage(nfcPayload);

            ndef.writeNdefMessage(ndefMessage);
            SnackbarUtil.createAndShowSnackbarAboveBottomNav(getActivity(), binding.coordinator, R.string.tag_written, Snackbar.LENGTH_LONG);

        } catch (FormatException | IOException e) {
            SnackbarUtil.createAndShowSnackbarAboveBottomNav(getActivity(), binding.coordinator, getString(R.string.tag_written_error, e.getMessage()), Snackbar.LENGTH_LONG);
        } finally {
            pairDialog.dismiss();
        }
    }

    private void displayLoading(String loadingMessage) {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.VISIBLE);
            binding.loadingDescription.setText(loadingMessage);
            binding.errorContainer.setVisibility(View.GONE);
        });
    }

    private void hideLoadingState() {
        runOnUiThread(() -> binding.loadingContainer.setVisibility(View.INVISIBLE));
    }

    private void hideLoadingState(UserMessage userMessage) {
        String message = userMessage.getMessage(getActivity());
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(message)) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorDescription.setText(message);
            }
            binding.loadingContainer.setVisibility(View.INVISIBLE);
        });
    }

    private void runOnUiThread(Runnable runnable) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(runnable);
    }
}