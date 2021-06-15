package com.github.kaiwinter.nfcsonos.main;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.FragmentMainBinding;
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.main.model.MainFragmentViewModel;
import com.github.kaiwinter.nfcsonos.main.model.MainFragmentViewModelFactory;
import com.github.kaiwinter.nfcsonos.main.model.RetryAction;
import com.github.kaiwinter.nfcsonos.main.model.RetryAction.RetryActionType;
import com.github.kaiwinter.nfcsonos.main.nfc.NfcPayload;
import com.github.kaiwinter.nfcsonos.main.nfc.NfcPayloadUtil;
import com.github.kaiwinter.nfcsonos.util.SnackbarUtil;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileDescriptor;
import java.io.IOException;

public class MainFragment extends Fragment {
    private static final String TAG = MainFragment.class.getSimpleName();

    private FragmentMainBinding binding;

    private MainFragmentViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentMainBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this, new MainFragmentViewModelFactory(getActivity().getApplication())).get(MainFragmentViewModel.class);

        viewModel.trackName.observe(this, binding.trackName::setText);

        viewModel.errorContainerVisibility.observe(this, binding.errorContainer::setVisibility);
        viewModel.errorMessageMutableLiveData.observe(this, errorMessage -> {
            String message = errorMessage.getMessage(getActivity());
            binding.errorContainer.setVisibility(View.VISIBLE);
            binding.errorDescription.setText(message);
        });
        viewModel.loadingContainerVisibility.observe(this, binding.loadingContainer::setVisibility);
        viewModel.loadingDescriptionResId.observe(this, binding.loadingDescription::setText);

        viewModel.coverImageToLoad.observe(this, this::loadAndShowCoverImage);
        viewModel.soundToPlay.observe(this, this::playSound);

        viewModel.playButtonVisibility.observe(this, binding.playButton::setVisibility);
        viewModel.pauseButtonVisibility.observe(this, binding.pauseButton::setVisibility);

        viewModel.navigateToDiscoverActivity.observe(this, this::startDiscoverActivity);

        binding.playButton.setOnClickListener(__ -> viewModel.play());
        binding.pauseButton.setOnClickListener(__ -> viewModel.pause());

        createInitialState();
    }
    private void createInitialState() {
        // check token here to avoid race condition between loadPlaybackMetadata() and loadPlayerState()
        if (viewModel.refreshTokenIfNeeded(this::createInitialState)) {
            return;
        }

        if (!viewModel.isUserLoggedIn()) {
            startLoginActivity();
            return;
        }

        if (!viewModel.isHouseholdAndGroupAvailable()) {
            startDiscoverActivity(null);
            return;
        }

        Intent intent = getActivity().getIntent();
        Bundle arguments = getArguments();
        if (arguments != null) {
            // May be passed from the MainActivity when the MainFragment wasn't selected when a NFC tag was scanned.
            Parcelable parcelable = arguments.getParcelable(MainActivity.NFC_SCANNED_INTENT);
            if (parcelable instanceof Intent) {
                intent = (Intent) parcelable;
            }
        }
        RetryAction retryAction = intent.getParcelableExtra(RetryAction.class.getSimpleName());
        if (retryAction != null) {
            if (retryAction.getRetryActionType() == RetryActionType.RETRY_LOAD_FAVORITE) {
                String retryId = retryAction.getAdditionalId();
                viewModel.loadAndStartFavorite(retryId);
            } else if (retryAction.getRetryActionType() == RetryActionType.RETRY_LOAD_METADATA) {
                viewModel.loadPlaybackMetadata();
            }
        } else {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                handleNfcIntent(intent);
            } else {
                viewModel.loadPlaybackMetadata();
            }
        }
        viewModel.loadPlayerState();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return binding.getRoot();
    }

    /**
     * Checks if the intent is of type ACTION_NDEF_DISCOVERED and handles it accordingly. If intent is of a different type nothing is done.
     *
     * @param intent the {@link Intent}
     */
    public void handleNfcIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tagFromIntent == null) {
            return;
        }

        try (Ndef ndef = Ndef.get(tagFromIntent)) {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            NfcPayload nfcPayload = NfcPayloadUtil.parseMessage(ndefMessage);

            if (nfcPayload == null) {
                playSound(R.raw.negative);
                Toast.makeText(getActivity(), R.string.tag_read_empty, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.tag_read_ok, Toast.LENGTH_SHORT).show();
                playSound(R.raw.positive);
                viewModel.loadAndStartFavorite(nfcPayload.getFavoriteId());
            }

        } catch (FormatException | IOException e) {
            SnackbarUtil.createAndShowSnackbarAboveBottomNav(getActivity(), binding.coordinator, getString(R.string.tag_read_error, e.getMessage()), Snackbar.LENGTH_LONG);
        }
    }

    /**
     * Sets the volume to the middle value if it is muted currently.
     */
//    private void setVolume() {
//        AudioManager audioManager = (AudioManager) getActivity().getSystemService(AUDIO_SERVICE);
//        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
//            int volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
//            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
//        }
//    }
    private void playSound(int sound) {
        //setVolume();
        try (AssetFileDescriptor afd = getResources().openRawResourceFd(sound)) {
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(fileDescriptor, afd.getStartOffset(), afd.getLength());
            player.setLooping(false);
            player.prepare();
            player.start();
        } catch (IOException ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        // Make the app exit if back is pressed on login activity. Else the user returns to the Login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void startDiscoverActivity(RetryAction retryAction) {
        Intent intent = new Intent(getActivity(), DiscoverActivity.class);
        if (retryAction != null) {
            intent.putExtra(RetryActionType.class.getSimpleName(), retryAction);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadAndShowCoverImage(String imageUrl) {
        if (imageUrl != null) {
            RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {

                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            };
            Glide.with(getActivity())
                    .load(Uri.parse(imageUrl))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .timeout(10000)
                    .placeholder(R.drawable.cover_placeholder)
                    .fitCenter()
                    .error(R.drawable.error)
                    .listener(requestListener)
                    .into(binding.coverImage);
        }
    }
}