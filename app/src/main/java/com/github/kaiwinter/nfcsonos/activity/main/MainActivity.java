package com.github.kaiwinter.nfcsonos.activity.main;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.activity.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.activity.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.activity.pair.PairActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityMainBinding;
import com.github.kaiwinter.nfcsonos.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.nfc.NfcPayload;
import com.github.kaiwinter.nfcsonos.nfc.NfcPayloadUtil;
import com.github.kaiwinter.nfcsonos.rest.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.GroupVolumeService;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    private SharedPreferencesStore sharedPreferencesStore;
    private AccessTokenManager accessTokenManager;
    private FavoriteCache favoriteCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferencesStore = new SharedPreferencesStore(getApplicationContext());
        accessTokenManager = new AccessTokenManager(getApplicationContext());
        favoriteCache = new FavoriteCache(getApplicationContext());

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

        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
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
    protected void onResume() {
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    /**
     * Checks if the intent is of type ACTION_NDEF_DISCOVERED and handles it accordingly. If intent is of a different type nothing is done.
     *
     * @param intent the {@link Intent}
     */
    private void handleIntent(Intent intent) {
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return;
        }
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
                Toast.makeText(this, R.string.tag_read_empty, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.tag_read_ok, Toast.LENGTH_SHORT).show();
                playSound(R.raw.positive);
                loadAndStartFavorite(nfcPayload.getFavoriteId());
            }

        } catch (FormatException | IOException e) {
            Snackbar.make(binding.coordinator, getString(R.string.tag_read_error, e.getMessage()), Snackbar.LENGTH_LONG).show();
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

        runOnUiThread(() -> binding.trackName.setText(""));
        showCoverImage(favoriteId);

        String accessToken = sharedPreferencesStore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        LoadFavoriteRequest request = new LoadFavoriteRequest(favoriteId);

        service.loadFavorite(sharedPreferencesStore.getGroupId(), request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    hideLoadingState(null);
                } else {
                    playSound(R.raw.negative);

                    APIError apiError = ServiceFactory.parseError(response);
                    if (response.code() == APIError.ERROR_RESOURCE_GONE_CODE && APIError.ERROR_RESOURCE_GONE.equals(apiError.errorCode)) {
                        startDiscoverActivity();
                        Snackbar.make(binding.coordinator, getString(R.string.group_id_changed), Snackbar.LENGTH_LONG).show();
                        hideLoadingState(null);
                        return;
                    }

                    String message = ServiceFactory.handleError(MainActivity.this, response);
                    hideLoadingState(message);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                playSound(R.raw.negative);
                hideLoadingState(getString(R.string.error_starting_favorite, t.getMessage()));
            }
        });
    }

    private void showCoverImage(String favoriteId) {
        favoriteCache.getFavorite(favoriteId, storedFavorite -> {
        runOnUiThread(() -> binding.trackName.setText(storedFavorite.name));

        String imageUrl = storedFavorite.imageUrl;
            loadAndShowCoverImage(imageUrl);
        }, this::hideLoadingState);
    }

    private void loadAndShowCoverImage(String imageUrl) {
        if (imageUrl != null) {
            RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {

                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target target, boolean isFirstResource) {
                    hideLoadingState(e.getMessage());
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            };
            Glide.with(getApplicationContext())
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


    public void coverImageClicked(View view) {
        loadPlaybackMetadata();
    }

    private void loadPlaybackMetadata() {
        if (refreshTokenIfNeeded(this::loadPlaybackMetadata)) {
            return;
        }

        displayLoading(getString(R.string.loading_metadata));
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackMetadataService service = ServiceFactory.createPlaybackMetadataService(accessToken);

        service.loadPlaybackMetadata(sharedPreferencesStore.getGroupId()).enqueue(new Callback<PlaybackMetadata>() {
            @Override
            public void onResponse(Call<PlaybackMetadata> call, Response<PlaybackMetadata> response) {
                if (!response.isSuccessful()) {
                    String message = ServiceFactory.handleError(MainActivity.this, response);
                    hideLoadingState(message);
                    return;
                }

                hideLoadingState(null);
                PlaybackMetadata playbackMetadata = response.body();
                if (playbackMetadata == null || playbackMetadata.container == null) {
                    return;
                }

                runOnUiThread(() -> binding.trackName.setText(playbackMetadata.container.name));

                String imageUrl = playbackMetadata.currentItem.track.imageUrl;
                loadAndShowCoverImage(imageUrl);
            }

            @Override
            public void onFailure(Call<PlaybackMetadata> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                setVolumeOnSonosGroup(5);
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                setVolumeOnSonosGroup(-5);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    private void setVolumeOnSonosGroup(int volumeDelta) {
        String accessToken = sharedPreferencesStore.getAccessToken();
        String groupId = sharedPreferencesStore.getGroupId();
        GroupVolumeService service = ServiceFactory.createGroupVolumeService(accessToken);
        Call<Void> call = service.setRelativeVolume(groupId, new GroupVolumeService.VolumeDeltaRequest(volumeDelta));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
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
