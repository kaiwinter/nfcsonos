package com.github.kaiwinter.nfcsonos.activity.main;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.activity.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.activity.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.activity.pair.PairActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityMainBinding;
import com.github.kaiwinter.nfcsonos.rest.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.LoadFavoriteRequest;
import com.github.kaiwinter.nfcsonos.rest.PlaybackMetadataService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.PlaybackMetadata;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import be.appfoundry.nfclibrary.activities.NfcActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends NfcActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    private SharedPreferencesStore sharedPreferencesStore;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferencesStore = new SharedPreferencesStore(this);
        accessTokenManager = new AccessTokenManager(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (TextUtils.isEmpty(sharedPreferencesStore.getAccessToken())) {
            startLoginActivity();
            return;
        }

        if (!isHouseholdAndGroupAvailable()) {
            startDiscoverActivity();
            return;
        }
        //signOut();
    }

    private void signOut() {
        sharedPreferencesStore.setTokens(null, null, -1);
        sharedPreferencesStore.setHouseholdAndGroup(null, null);

        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        List<String> nfcMessages = getNfcMessages();
        if (nfcMessages.isEmpty()) {
            playSound(R.raw.negative);
            Toast.makeText(this, "Tag is empty", Toast.LENGTH_SHORT).show();
        } else if (nfcMessages.size() > 1) {
            playSound(R.raw.negative);
            Toast.makeText(this, "Tag contains " + nfcMessages.size() + " messages", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Tag ok", Toast.LENGTH_SHORT).show();
            playSound(R.raw.positive);
            loadAndStartFavorite(nfcMessages.get(0));
        }
    }

    /**
     * Sets the volume to the middle value if it is muted currently.
     */
    private void setVolume() {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            int volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }
    }

    private void playSound(int sound) {
        //setVolume();
        try (AssetFileDescriptor afd = MainActivity.this.getResources().openRawResourceFd(sound)) {
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

    public void loadAndStartFavorite(String favoriteId) {
        displayLoading(getString(R.string.starting_favorite));

        if (refreshTokenIfNeeded(() -> loadAndStartFavorite(favoriteId))) {
            return;
        }

        runOnUiThread(() -> {
            binding.trackName.setText("");
            binding.coverImage.setImageResource(R.drawable.ic_nfc_green);
        });

        String accessToken = sharedPreferencesStore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        LoadFavoriteRequest request = new LoadFavoriteRequest(favoriteId);

        service.loadFavorite(sharedPreferencesStore.getGroupId(), request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadPlaybackMetadata();
                } else {
                    playSound(R.raw.negative);

                    APIError apiError = ServiceFactory.parseError(response);
                    if (response.code() == APIError.ERROR_RESOURCE_GONE_CODE && APIError.ERROR_RESOURCE_GONE.equals(apiError.errorCode)) {
                        startDiscoverActivity();
                        Snackbar.make(binding.coordinator, getString(R.string.group_id_changed), Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String message = ServiceFactory.handleError(MainActivity.this, response);
                    runOnUiThread(() -> {
                        hideLoadingState(message);
                        binding.coverImage.setImageResource(R.drawable.ic_nfc);
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                playSound(R.raw.negative);
                hideLoadingState(getString(R.string.error_starting_favorite, t.getMessage()));
                runOnUiThread(() -> binding.coverImage.setImageResource(R.drawable.ic_nfc));
            }
        });

    }

    public void loadPlaybackMetadata() {
        if (refreshTokenIfNeeded(this::loadPlaybackMetadata)) {
            return;
        }

        runOnUiThread(() -> displayLoading(getString(R.string.loading_metadata)));
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackMetadataService service = ServiceFactory.createPlaybackMetadataService(accessToken);

        service.loadPlaybackMetadata(sharedPreferencesStore.getGroupId()).enqueue(new Callback<PlaybackMetadata>() {
            @Override
            public void onResponse(Call<PlaybackMetadata> call, Response<PlaybackMetadata> response) {
                if (!response.isSuccessful()) {
                    playSound(R.raw.negative);
                    String message = ServiceFactory.handleError(MainActivity.this, response);
                    hideLoadingState(message);
                    runOnUiThread(() -> binding.coverImage.setImageResource(R.drawable.ic_nfc));
                    return;
                }

                PlaybackMetadata playbackMetadata = response.body();

                runOnUiThread(() -> binding.trackName.setText(playbackMetadata.container.name));

                if (playbackMetadata.currentItem == null) {
                    runOnUiThread(() -> binding.coverImage.setImageResource(R.drawable.ic_nfc));
                    return;
                }

                String imageUrl = playbackMetadata.currentItem.track.imageUrl;

                if (imageUrl != null) {
                    runOnUiThread(() -> displayLoading(getString(R.string.loading_cover)));
//                    Picasso.get()
//                        .load(Uri.parse(action.getResponse().currentItem.track.imageUrl))
//                        .into(binding.coverImage;

                    RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {

                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target target, boolean isFirstResource) {
                            hideLoadingState(e.getMessage());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                            hideLoadingState(null);
                            return false;
                        }
                    };
                    Glide.with(MainActivity.this)
                            .load(Uri.parse(imageUrl))
                            .timeout(10000)
                            .placeholder(R.drawable.ic_nfc_green)
                            .fitCenter()
                            .error(R.drawable.error)
                            .listener(requestListener)
                            .into(binding.coverImage);
                }
            }

            @Override
            public void onFailure(Call<PlaybackMetadata> call, Throwable t) {
                playSound(R.raw.negative);
                runOnUiThread(() -> {
                    hideLoadingState(getString(R.string.error_loading_metadata, t.getMessage()));
                    binding.coverImage.setImageResource(R.drawable.ic_nfc);
                });
            }
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        // Make the app exit if back is pressed on login activity. Else the user returns to the Login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startDiscoverActivity() {
        Intent intent = new Intent(getApplicationContext(), DiscoverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean isHouseholdAndGroupAvailable() {
        boolean householdSelected = !TextUtils.isEmpty(sharedPreferencesStore.getHouseholdId());
        boolean groupSelected = !TextUtils.isEmpty(sharedPreferencesStore.getGroupId());
        return householdSelected && groupSelected;
    }

    public void startPairActivity(View view) {
        Intent intent = new Intent(getApplicationContext(), PairActivity.class);
        startActivity(intent);
    }

    public void runTestTag(View view) {
        playSound(R.raw.positive);
        loadAndStartFavorite("63");
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading(getString(R.string.refresh_access_token));
            accessTokenManager.refreshAccessToken(this, runnable, this::hideLoadingState);
            return true;
        }
        return false;
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
