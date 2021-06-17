package com.github.kaiwinter.nfcsonos.main.model;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.main.MainActivity;
import com.github.kaiwinter.nfcsonos.main.model.RetryAction.RetryActionType;
import com.github.kaiwinter.nfcsonos.main.nfc.NfcPayload;
import com.github.kaiwinter.nfcsonos.main.nfc.NfcPayloadUtil;
import com.github.kaiwinter.nfcsonos.rest.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.LoadFavoriteRequest;
import com.github.kaiwinter.nfcsonos.rest.PlaybackMetadataService;
import com.github.kaiwinter.nfcsonos.rest.PlaybackService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.CurrentItem;
import com.github.kaiwinter.nfcsonos.rest.model.PlaybackMetadata;
import com.github.kaiwinter.nfcsonos.rest.model.PlaybackStatus;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.SingleLiveEvent;
import com.github.kaiwinter.nfcsonos.util.UserMessage;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainFragmentViewModel extends ViewModel {
    public final MutableLiveData<String> albumTitle = new MutableLiveData<>();

    public final MutableLiveData<Integer> loadingContainerVisibility = new MutableLiveData<>(View.INVISIBLE);
    public final MutableLiveData<Integer> loadingDescriptionResId = new MutableLiveData<>();
    public final MutableLiveData<Integer> errorContainerVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<UserMessage> errorMessageMutableLiveData = new MutableLiveData<>();

    public final MutableLiveData<String> coverImageToLoad = new MutableLiveData<>();
    public final MutableLiveData<Integer> soundToPlay = new MutableLiveData<>();

    public final SingleLiveEvent<Void> navigateToLoginActivity = new SingleLiveEvent<>();
    public final SingleLiveEvent<RetryAction> navigateToDiscoverActivity = new SingleLiveEvent<>();
    public final SingleLiveEvent<UserMessage> showSnackbarMessage = new SingleLiveEvent<>();

    public final MutableLiveData<Integer> playButtonVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<Integer> pauseButtonVisibility = new MutableLiveData<>(View.GONE);

    private final SharedPreferencesStore sharedPreferencesStore;
    private final AccessTokenManager accessTokenManager;
    private final FavoriteCache favoriteCache;

    public MainFragmentViewModel(SharedPreferencesStore sharedPreferencesStore, AccessTokenManager accessTokenManager, FavoriteCache favoriteCache) {
        this.sharedPreferencesStore = sharedPreferencesStore;
        this.accessTokenManager = accessTokenManager;
        this.favoriteCache = favoriteCache;
    }

    /**
     * Initializes the view. Loads the currently playing album and the player state and updates the view.
     * Also this is the entry point for a {@link RetryAction} and a scanned NFC tag.
     *
     * @param intent    the {@link Intent}, may contain a {@link RetryAction} or a NFC tag
     * @param arguments may contain a {@link Intent} with a NFC tag which was redirected to this Fragment
     */
    public void createInitialState(Intent intent, Bundle arguments) {
        // check token here to avoid race condition between loadPlaybackMetadata() and loadPlayerState()
        Intent finalIntent = intent;
        if (refreshTokenIfNeeded(() -> createInitialState(finalIntent, arguments))) {
            return;
        }

        if (!isUserLoggedIn()) {
            navigateToLoginActivity.postValue(null);
            return;
        }

        if (!isHouseholdAndGroupAvailable()) {
            navigateToDiscoverActivity.postValue(null);
            return;
        }

        if (arguments != null) {
            // May be passed from the MainActivity when the MainFragment wasn't selected when a NFC tag was scanned.
            Parcelable parcelable = arguments.getParcelable(MainActivity.NFC_SCANNED_INTENT);
            if (parcelable instanceof Intent) {
                intent = (Intent) parcelable;
            }
        }
        if (intent.hasExtra(RetryAction.class.getSimpleName())) {
            handleRetryAction(intent);
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            handleNfcIntent(intent);
        } else {
            loadPlaybackMetadata();
        }
        loadPlayerState();
    }

    private void handleRetryAction(Intent intent) {
        RetryAction retryAction = intent.getParcelableExtra(RetryAction.class.getSimpleName());
        if (retryAction.getRetryActionType() == RetryActionType.RETRY_LOAD_FAVORITE) {
            String retryId = retryAction.getAdditionalId();
            loadAndStartFavorite(retryId);
        } else if (retryAction.getRetryActionType() == RetryActionType.RETRY_LOAD_METADATA) {
            loadPlaybackMetadata();
        }
    }

    /**
     * Handles a scanned tag.
     *
     * @param intent the {@link Intent} which contains a {@link NfcAdapter#EXTRA_TAG}
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
                soundToPlay.postValue(R.raw.negative);
                showSnackbarMessage.postValue(UserMessage.create(R.string.tag_read_empty));

            } else {
                soundToPlay.postValue(R.raw.positive);
                showSnackbarMessage.postValue(UserMessage.create(R.string.tag_read_ok));
                loadAndStartFavorite(nfcPayload.getFavoriteId());
            }

        } catch (FormatException | IOException e) {
            UserMessage userMessage = UserMessage.create(R.string.tag_read_error, e.getMessage());
            showSnackbarMessage.postValue(userMessage);
        }
    }

    private boolean isUserLoggedIn() {
        return !TextUtils.isEmpty(sharedPreferencesStore.getAccessToken());
    }

    private boolean isHouseholdAndGroupAvailable() {
        boolean householdSelected = !TextUtils.isEmpty(sharedPreferencesStore.getHouseholdId());
        boolean groupSelected = !TextUtils.isEmpty(sharedPreferencesStore.getGroupId());
        return householdSelected && groupSelected;
    }

    private boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading(R.string.refresh_access_token);
            accessTokenManager.refreshAccessToken(runnable, this::hideLoadingState);
            return true;
        }
        return false;
    }

    private void loadAndStartFavorite(String favoriteId) {
        if (refreshTokenIfNeeded(() -> loadAndStartFavorite(favoriteId))) {
            return;
        }

        displayLoading(R.string.starting_favorite);
        albumTitle.setValue("");
        showAlbumCoverAndTitle(favoriteId);

        String accessToken = sharedPreferencesStore.getAccessToken();
        FavoriteService service = ServiceFactory.createFavoriteService(accessToken);

        LoadFavoriteRequest request = new LoadFavoriteRequest(favoriteId);

        service.loadFavorite(sharedPreferencesStore.getGroupId(), request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    hideLoadingState();
                } else {
                    soundToPlay.setValue(R.raw.negative);

                    handleError(response, new RetryAction(RetryActionType.RETRY_LOAD_FAVORITE, favoriteId));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                soundToPlay.setValue(R.raw.negative);
                UserMessage userMessage = UserMessage.create(R.string.error_starting_favorite, t.getMessage());
                hideLoadingState(userMessage);
            }
        });
    }

    private void loadPlaybackMetadata() {
        if (refreshTokenIfNeeded(this::loadPlaybackMetadata)) {
            return;
        }

        displayLoading(R.string.loading_metadata);

        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackMetadataService service = ServiceFactory.createPlaybackMetadataService(accessToken);

        service.loadPlaybackMetadata(sharedPreferencesStore.getGroupId()).enqueue(new Callback<PlaybackMetadata>() {
            @Override
            public void onResponse(Call<PlaybackMetadata> call, Response<PlaybackMetadata> response) {
                if (response.isSuccessful()) {
                    hideLoadingState();
                    PlaybackMetadata playbackMetadata = response.body();
                    if (playbackMetadata == null || playbackMetadata.container == null) {
                        return;
                    }

                    albumTitle.setValue(playbackMetadata.container.name);

                    CurrentItem currentItem = playbackMetadata.currentItem;
                    if (currentItem != null) {
                        String imageUrl = currentItem.track.imageUrl;
                        coverImageToLoad.setValue(imageUrl);
                    }
                } else {
                    handleError(response, new RetryAction(RetryActionType.RETRY_LOAD_METADATA));
                }
            }

            @Override
            public void onFailure(Call<PlaybackMetadata> call, Throwable t) {
                UserMessage userMessage = UserMessage.create(R.string.error_loading_metadata, t.getMessage());
                hideLoadingState(userMessage);
            }
        });
    }

    /**
     * Loads if the player is playing or paused.
     */
    private void loadPlayerState() {
        if (refreshTokenIfNeeded(this::loadPlayerState)) {
            return;
        }

        displayLoading(R.string.loading_playerstate);

        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = ServiceFactory.createPlaybackService(accessToken);
        service.playbackStatus(sharedPreferencesStore.getGroupId()).enqueue(new Callback<PlaybackStatus>() {
            @Override
            public void onResponse(Call<PlaybackStatus> call, Response<PlaybackStatus> response) {
                if (response.isSuccessful()) {
                    hideLoadingState();
                    PlaybackStatus playbackStatus = response.body();
                    if (playbackStatus == null) {
                        return;
                    }

                    switch (playbackStatus.playbackState) {
                        case PLAYBACK_STATE_PLAYING:
                        case PLAYBACK_STATE_BUFFERING:
                            // show Play Button
                            playButtonVisibility.postValue(View.GONE);
                            pauseButtonVisibility.postValue(View.VISIBLE);
                            break;

                        case PLAYBACK_STATE_IDLE:
                        case PLAYBACK_STATE_PAUSED:
                            // show Pause Button
                            playButtonVisibility.postValue(View.VISIBLE);
                            pauseButtonVisibility.postValue(View.GONE);
                            break;
                    }
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<PlaybackStatus> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    /**
     * Sets the player state to playing.
     */
    public void play() {
        displayLoading(R.string.start_playback);
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = ServiceFactory.createPlaybackService(accessToken);
        service.play(sharedPreferencesStore.getGroupId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    playButtonVisibility.postValue(View.GONE);
                    pauseButtonVisibility.postValue(View.VISIBLE);
                    hideLoadingState();
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    /**
     * Sets the player state to paused.
     */
    public void pause() {
        displayLoading(R.string.stop_playback);
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = ServiceFactory.createPlaybackService(accessToken);
        service.pause(sharedPreferencesStore.getGroupId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    playButtonVisibility.postValue(View.VISIBLE);
                    pauseButtonVisibility.postValue(View.GONE);
                    hideLoadingState();
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    /**
     * Handles the error of a service request. If Sonos returns "ERROR_RESOURCE_GONE", the user gets
     * redirected to the {@link DiscoverActivity}.
     * Afterwards the retryAction is called.
     *
     * @param response    the response which contains the error
     * @param retryAction the {@link RetryAction} which is called when the user returns from the
     *                    {@link DiscoverActivity}
     */
    private void handleError(Response<?> response, RetryAction retryAction) {
        APIError apiError = ServiceFactory.parseError(response);
        if (response.code() == APIError.ERROR_RESOURCE_GONE_CODE && APIError.ERROR_RESOURCE_GONE.equals(apiError.errorCode)) {
            navigateToDiscoverActivity.postValue(retryAction);
            hideLoadingState();
        } else {
            UserMessage userMessage = UserMessage.create(apiError);
            hideLoadingState(userMessage);
        }
    }

    private void handleError(Response<?> response) {
        APIError apiError = ServiceFactory.parseError(response);
        UserMessage userMessage = UserMessage.create(apiError);
        hideLoadingState(userMessage);
    }

    private void showAlbumCoverAndTitle(String favoriteId) {
        favoriteCache.getFavorite(favoriteId, storedFavorite -> {
            albumTitle.setValue(storedFavorite.name);

            String imageUrl = storedFavorite.imageUrl;
            coverImageToLoad.setValue(imageUrl);
        }, this::hideLoadingState);
    }

    private void displayLoading(Integer resId) {
        loadingContainerVisibility.setValue(View.VISIBLE);
        loadingDescriptionResId.setValue(resId);
        errorContainerVisibility.setValue(View.GONE);
    }

    private void hideLoadingState() {
        loadingContainerVisibility.setValue(View.INVISIBLE);
    }

    private void hideLoadingState(String message) {
        UserMessage userMessage = UserMessage.create(message);
        hideLoadingState(userMessage);
    }

    private void hideLoadingState(UserMessage userMessage) {
        errorMessageMutableLiveData.setValue(userMessage);
        hideLoadingState();
    }
}