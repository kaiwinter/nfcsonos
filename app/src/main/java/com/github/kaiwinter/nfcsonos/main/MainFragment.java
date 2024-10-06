package com.github.kaiwinter.nfcsonos.main;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.SnackbarUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

        viewModel.albumTitle.observe(this, binding.albumTitle::setText);
        viewModel.trackTitle.observe(this, binding.trackTitle::setText);

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

        viewModel.skipToPreviousButtonVisibility.observe(this, binding.skipToPreviousButton::setVisibility);
        viewModel.playButtonVisibility.observe(this, binding.playButton::setVisibility);
        viewModel.pauseButtonVisibility.observe(this, binding.pauseButton::setVisibility);
        viewModel.skipToNextButtonVisibility.observe(this, binding.skipToNextButton::setVisibility);

        viewModel.navigateToLoginActivity.observe(this, __ -> startLoginActivity());
        viewModel.navigateToDiscoverActivity.observe(this, this::startDiscoverActivity);
        viewModel.showSnackbarMessage.observe(this, errorMessage -> SnackbarUtil.createAndShowSnackbarAboveBottomNav(getActivity(), binding.coordinator, errorMessage.getMessage(getActivity()), Snackbar.LENGTH_LONG));

        binding.skipToPreviousButton.setOnClickListener(__ -> viewModel.skipToPrevious());
        binding.playButton.setOnClickListener(__ -> viewModel.play());
        binding.pauseButton.setOnClickListener(__ -> viewModel.pause());
        binding.skipToNextButton.setOnClickListener(__ -> viewModel.skipToNext());

        checkForNfcAdapter();
    }

    private void checkForNfcAdapter() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle(R.string.no_nfc_adapter_title)
                    .setMessage(R.string.no_nfc_adapter_message)
                    .setPositiveButton("OK", null)
                    .create().show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getActivity().getIntent();
        getActivity().setIntent(null);
        Bundle arguments = getArguments();
        viewModel.createInitialState(intent, arguments);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return binding.getRoot();
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
        if (!viewModel.shouldPlaySounds()) {
            return;
        }
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
            intent.putExtra(RetryAction.class.getSimpleName(), retryAction);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadAndShowCoverImage(String imageUrl) {
        if (imageUrl != null) {
            RequestListener<Drawable> requestListener = new RequestListener<>() {

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

    public void handleNfcIntent(Intent intent) {
        viewModel.handleNfcIntent(intent);
    }
}