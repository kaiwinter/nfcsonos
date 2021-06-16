package com.github.kaiwinter.nfcsonos.main.model;

import android.text.TextUtils;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.main.model.RetryAction.RetryActionType;
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
import com.github.kaiwinter.nfcsonos.util.ErrorMessage;
import com.github.kaiwinter.nfcsonos.util.SingleLiveEvent;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainFragmentViewModel extends ViewModel {
    public final MutableLiveData<String> trackName = new MutableLiveData<>();

    public final MutableLiveData<Integer> loadingContainerVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<Integer> loadingDescriptionResId = new MutableLiveData<>();
    public final MutableLiveData<Integer> errorContainerVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<ErrorMessage> errorMessageMutableLiveData = new MutableLiveData<>();

    public final MutableLiveData<String> coverImageToLoad = new MutableLiveData<>();
    public final MutableLiveData<Integer> soundToPlay = new MutableLiveData<>();

    public final SingleLiveEvent<RetryAction> navigateToDiscoverActivity = new SingleLiveEvent<>();

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

    public boolean isUserLoggedIn() {
        return !TextUtils.isEmpty(sharedPreferencesStore.getAccessToken());
    }

    public boolean isHouseholdAndGroupAvailable() {
        boolean householdSelected = !TextUtils.isEmpty(sharedPreferencesStore.getHouseholdId());
        boolean groupSelected = !TextUtils.isEmpty(sharedPreferencesStore.getGroupId());
        return householdSelected && groupSelected;
    }

    public boolean refreshTokenIfNeeded(Runnable runnable) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            displayLoading(R.string.refresh_access_token);
            accessTokenManager.refreshAccessToken(runnable, this::hideLoadingState);
            return true;
        }
        return false;
    }

    public void loadAndStartFavorite(String favoriteId) {
        if (refreshTokenIfNeeded(() -> loadAndStartFavorite(favoriteId))) {
            return;
        }

        displayLoading(R.string.starting_favorite);
        trackName.setValue("");
        showCoverImage(favoriteId);

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
                ErrorMessage errorMessage = ErrorMessage.create(R.string.error_starting_favorite, t.getMessage());
                hideLoadingState(errorMessage);
            }
        });
    }

    public void loadPlaybackMetadata() {
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

                    trackName.setValue(playbackMetadata.container.name);

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
                ErrorMessage errorMessage = ErrorMessage.create(R.string.error_loading_metadata, t.getMessage());
                hideLoadingState(errorMessage);
            }
        });
    }

    /**
     * Loads if the player is playing or paused.
     */
    public void loadPlayerState() {
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
            ErrorMessage errorMessage = ErrorMessage.create(apiError);
            hideLoadingState(errorMessage);
        }
    }

    private void handleError(Response<?> response) {
        APIError apiError = ServiceFactory.parseError(response);
        ErrorMessage errorMessage = ErrorMessage.create(apiError);
        hideLoadingState(errorMessage);
    }

    private void showCoverImage(String favoriteId) {
        favoriteCache.getFavorite(favoriteId, storedFavorite -> {
            trackName.setValue(storedFavorite.name);

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
        loadingContainerVisibility.setValue(View.GONE);
    }

    private void hideLoadingState(String message) {
        ErrorMessage errorMessage = ErrorMessage.create(message);
        hideLoadingState(errorMessage);
    }

    private void hideLoadingState(ErrorMessage errorMessage) {
        errorMessageMutableLiveData.setValue(errorMessage);
        hideLoadingState();
    }
}