package com.github.kaiwinter.nfcsonos.activity.main;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesTokenStore;
import com.github.kaiwinter.nfcsonos.activity.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.activity.pair.PairActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityTokenBinding;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.favorite.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.favorite.LoadFavoriteRequest;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenstore = new SharedPreferencesTokenStore(this);

        binding = ActivityTokenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        displayAuthorized();
    }

    private void displayAuthorized() {
        binding.authorized.setVisibility(View.VISIBLE);
        binding.notAuthorized.setVisibility(View.GONE);
        binding.loadingContainer.setVisibility(View.INVISIBLE);
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
        try (AssetFileDescriptor afd = TokenActivity.this.getResources().openRawResourceFd(sound);) {
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
        binding.status.setText("Starte Favorit");
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.loadFavoriteStatus.setText("");
        binding.loadPlaybackMetadataStatus.setText("");
        binding.trackName.setText("");
        binding.coverImage.setImageResource(R.drawable.ic_nfc_green);

        String accessToken = tokenstore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        LoadFavoriteRequest request = new LoadFavoriteRequest(favoriteId);

        service.loadFavorite(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int status = response.code();
                if (status == 200) {
                    binding.loadFavoriteStatus.setText("Favorit gestartet");
                    loadPlaybackMetadata();
                } else {
                    playSound(R.raw.negative);
                    binding.loadFavoriteStatus.setText("Fehler beim Starten (" + status + "): " + "FIXME KW");
                    binding.coverImage.setImageResource(R.drawable.ic_nfc);
                    binding.loadingContainer.setVisibility(View.INVISIBLE);
                    binding.status.setText("Bereit zum Scannen");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                playSound(R.raw.negative);
                binding.loadFavoriteStatus.setText("Fehler beim Starten: " + t.getMessage());
                binding.coverImage.setImageResource(R.drawable.ic_nfc);
                binding.loadingContainer.setVisibility(View.INVISIBLE);
                binding.status.setText("Bereit zum Scannen");
            }
        });

    }

    public void loadPlaybackMetadata() {
        binding.status.setText("Lade Metadaten");
//        PlaybackMetadataAction action = new PlaybackMetadataAction();
//
//        ClientAuthentication clientAuthentication = new ClientSecretBasic(mConfiguration.getClientSecret());
//
//        mExecutor.submit(new ExceptionAwareRunnable(() -> {
//            mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, clientAuthentication, action);
//            int status = action.getStatus();
//
//            runOnUiThread(() -> {
//                if (status == 200) {
//                    binding.loadPlaybackMetadataStatus.setText("Metadaten geladen");
//                } else {
//                    playSound(R.raw.negative);
//                    binding.loadPlaybackMetadataStatus.setText("Fehler beim Laden der Metadaten (" + status + "): " + action.getError());
//                    binding.coverImage.setImageResource(R.drawable.ic_nfc);
//                    binding.loadingContainer.setVisibility(View.INVISIBLE);
//                    binding.status.setText("Bereit zum Scannen");
//                    return;
//                }
//
//                PlaybackMetadata response = action.getResponse();
//                String containerName = response.container.name;
//                binding.trackName.setText(containerName);
//
//                if (response.currentItem == null) {
//                    binding.loadingContainer.setVisibility(View.INVISIBLE);
//                    binding.status.setText("Bereit zum Scannen");
//                    binding.coverImage.setImageResource(R.drawable.ic_nfc);
//                    return;
//                }
//
//                String imageUrl = response.currentItem.track.imageUrl;
//
//                if (imageUrl != null) {
//                    binding.status.setText("Lade Cover");
////                    Picasso.get()
////                        .load(Uri.parse(action.getResponse().currentItem.track.imageUrl))
////                        .into(binding.coverImage;
//
//                    RequestListener requestListener = new RequestListener() {
//
//                        @Override
//                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
//                            binding.loadingContainer.setVisibility(View.INVISIBLE);
//                            binding.status.setText("Bereit zum Scannen");
//                            return false;
//                        }
//
//                        @Override
//                        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
//                            binding.loadingContainer.setVisibility(View.INVISIBLE);
//                            binding.status.setText("Bereit zum Scannen");
//                            return false;
//                        }
//                    };
//                    Glide.with(TokenActivity.this)
//                        .load(Uri.parse(imageUrl))
//                        .timeout(10000)
//                        .placeholder(R.drawable.ic_nfc_green)
//                        .fitCenter()
//                        .error(R.drawable.error)
//                        .listener(requestListener)
//                        .into(binding.coverImage);
//                }
//
//            });
//        }, throwable -> runOnUiThread(() -> {
//            playSound(R.raw.negative);
//            binding.loadPlaybackMetadataStatus.setText("Fehler beim Laden der Metadaten: " + throwable.getMessage());
//            binding.coverImage.setImageResource(R.drawable.ic_nfc);
//            binding.loadingContainer.setVisibility(View.INVISIBLE);
//            binding.status.setText("Bereit zum Scannen");
//        })));
    }

    public void startPairActivity(View view) {
        Intent intent = new Intent(getApplicationContext(), PairActivity.class);
        startActivity(intent);
    }

    public void runTestTag(View view) {
        playSound(R.raw.positive);
        loadAndStartFavorite("63");
    }
}
