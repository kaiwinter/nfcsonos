package com.github.kaiwinter.nfcsonos.activity.main;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.activity.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.activity.pair.PairActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityTokenBinding;
import com.github.kaiwinter.nfcsonos.rest.APIError;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.favorite.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.favorite.LoadFavoriteRequest;
import com.github.kaiwinter.nfcsonos.rest.playbackmetadata.PlaybackMetadataService;
import com.github.kaiwinter.nfcsonos.rest.playbackmetadata.model.PlaybackMetadata;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesTokenStore;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import be.appfoundry.nfclibrary.activities.NfcActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TokenActivity extends NfcActivity {
    private static final String TAG = TokenActivity.class.getSimpleName();

    private ActivityTokenBinding binding;

    private SharedPreferencesTokenStore tokenstore;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenstore = new SharedPreferencesTokenStore(this);
        accessTokenManager = new AccessTokenManager(this);

        binding = ActivityTokenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void signOut() {
        tokenstore.setTokens(null, null, -1);

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
        try (AssetFileDescriptor afd = TokenActivity.this.getResources().openRawResourceFd(sound)) {
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
        if (refreshTokenIfNeeded(() -> loadAndStartFavorite(favoriteId))) {
            return;
        }
        displayLoading("Starte Favorit");

        runOnUiThread(() -> {
            binding.loadFavoriteStatus.setText("");
            binding.loadPlaybackMetadataStatus.setText("");
            binding.trackName.setText("");
            binding.coverImage.setImageResource(R.drawable.ic_nfc_green);
        });

        String accessToken = tokenstore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        LoadFavoriteRequest request = new LoadFavoriteRequest(favoriteId);

        service.loadFavorite(getString(R.string.group), request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int status = response.code();
                if (status == 200) {
                    runOnUiThread(() -> binding.loadFavoriteStatus.setText("Favorit gestartet"));
                    loadPlaybackMetadata();
                } else {
                    playSound(R.raw.negative);
                    hideLoading("Bereit zum Scannen");

                    runOnUiThread(() -> {
                        APIError apiError = ServiceFactory.parseError(response);
                        binding.loadFavoriteStatus.setText("Fehler beim Starten (" + status + "): " + apiError.error + ", " + apiError.errorDescription);
                        binding.coverImage.setImageResource(R.drawable.ic_nfc);
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                playSound(R.raw.negative);
                hideLoading("Fehler beim Starten: " + t.getMessage());
                runOnUiThread(() -> {
                    binding.coverImage.setImageResource(R.drawable.ic_nfc);
                    binding.status.setText("Bereit zum Scannen");
                });
            }
        });

    }

    public void loadPlaybackMetadata() {
        if (refreshTokenIfNeeded(this::loadPlaybackMetadata)) {
            return;
        }

        runOnUiThread(() -> binding.status.setText("Lade Metadaten"));
        String accessToken = tokenstore.getAccessToken();
        PlaybackMetadataService service = ServiceFactory.createPlaybackMetadataService(accessToken);

        service.loadPlaybackMetadata(getString(R.string.group)).enqueue(new Callback<PlaybackMetadata>() {
            @Override
            public void onResponse(Call<PlaybackMetadata> call, Response<PlaybackMetadata> response) {
                int status = response.code();
                if (status == 200) {
                    runOnUiThread(() -> binding.loadPlaybackMetadataStatus.setText("Metadaten geladen"));
                } else {
                    playSound(R.raw.negative);
                    hideLoading("Bereit zum Scannen");
                    APIError apiError = ServiceFactory.parseError(response);
                    runOnUiThread(() -> {
                        binding.loadPlaybackMetadataStatus.setText("Fehler beim Laden der Metadaten (" + status + "): " + apiError.error + ", " + apiError.errorDescription);
                        binding.coverImage.setImageResource(R.drawable.ic_nfc);
                    });
                    return;
                }

                PlaybackMetadata playbackMetadata = response.body();

                runOnUiThread(() -> binding.trackName.setText(playbackMetadata.container.name));

                if (playbackMetadata.currentItem == null) {
                    hideLoading("Bereit zum Scannen");
                    runOnUiThread(() -> binding.coverImage.setImageResource(R.drawable.ic_nfc));
                    return;
                }

                String imageUrl = playbackMetadata.currentItem.track.imageUrl;

                if (imageUrl != null) {
                    runOnUiThread(() -> binding.status.setText("Lade Cover"));
//                    Picasso.get()
//                        .load(Uri.parse(action.getResponse().currentItem.track.imageUrl))
//                        .into(binding.coverImage;

                    RequestListener requestListener = new RequestListener() {

                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target target, boolean isFirstResource) {
                            hideLoading("Bereit zum Scannen");
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                            hideLoading("Bereit zum Scannen");
                            return false;
                        }
                    };
                    Glide.with(TokenActivity.this)
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
                    binding.loadPlaybackMetadataStatus.setText("Fehler beim Laden der Metadaten: " + t.getMessage());
                    binding.coverImage.setImageResource(R.drawable.ic_nfc);
                });
                hideLoading("Bereit zum Scannen");
            }
        });
    }

    public void startPairActivity(View view) {
        Intent intent = new Intent(getApplicationContext(), PairActivity.class);
        startActivity(intent);
    }

    public void runTestTag(View view) {
        playSound(R.raw.positive);
        loadAndStartFavorite("63");
    }

    private void displayLoading(String statusMessage) {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.VISIBLE);
            binding.status.setText(statusMessage);
        });
    }

    private void hideLoading(String statusMessage) {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.INVISIBLE);
            binding.status.setText(statusMessage);
        });
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading("Refresh Access Token");
            accessTokenManager.refreshAccessToken(this, runnable, this::hideLoading);
            return true;
        }
        return false;
    }
}
