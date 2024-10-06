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
import com.github.kaiwinter.nfcsonos.rest.DiscoverService;
import com.github.kaiwinter.nfcsonos.rest.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.LoadFavoriteRequest;
import com.github.kaiwinter.nfcsonos.rest.PlaybackMetadataService;
import com.github.kaiwinter.nfcsonos.rest.PlaybackService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.CurrentItem;
import com.github.kaiwinter.nfcsonos.rest.model.Group;
import com.github.kaiwinter.nfcsonos.rest.model.Groups;
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
    public final MutableLiveData<String> trackTitle = new MutableLiveData<>();

    public final MutableLiveData<Integer> loadingContainerVisibility = new MutableLiveData<>(View.INVISIBLE);
    public final MutableLiveData<Integer> loadingDescriptionResId = new MutableLiveData<>();
    public final MutableLiveData<Integer> errorContainerVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<UserMessage> errorMessageMutableLiveData = new MutableLiveData<>();

    public final SingleLiveEvent<String> coverImageToLoad = new SingleLiveEvent<>();
    public final SingleLiveEvent<Integer> soundToPlay = new SingleLiveEvent<>();

    public SingleLiveEvent<Void> navigateToLoginActivity = new SingleLiveEvent<>();
    public SingleLiveEvent<RetryAction> navigateToDiscoverActivity = new SingleLiveEvent<>();
    public final SingleLiveEvent<UserMessage> showSnackbarMessage = new SingleLiveEvent<>();

    public final MutableLiveData<Integer> skipToPreviousButtonVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<Integer> playButtonVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<Integer> pauseButtonVisibility = new MutableLiveData<>(View.GONE);
    public final MutableLiveData<Integer> skipToNextButtonVisibility = new MutableLiveData<>(View.GONE);

    private final SharedPreferencesStore sharedPreferencesStore;
    private final AccessTokenManager accessTokenManager;
    private final FavoriteCache favoriteCache;
    private final ServiceFactory serviceFactory;

    public MainFragmentViewModel(SharedPreferencesStore sharedPreferencesStore, AccessTokenManager accessTokenManager, FavoriteCache favoriteCache, ServiceFactory serviceFactory) {
        this.sharedPreferencesStore = sharedPreferencesStore;
        this.accessTokenManager = accessTokenManager;
        this.favoriteCache = favoriteCache;
        this.serviceFactory = serviceFactory;
    }

    /**
     * Initializes the view. Loads the currently playing album and the player state and updates the view.
     * Also this is the entry point for a {@link RetryAction} and a scanned NFC tag.
     *
     * @param intent    the {@link Intent}, may contain a {@link RetryAction} or a NFC tag
     * @param arguments may contain a {@link Intent} with a NFC tag which was redirected to this Fragment
     */
    public void createInitialState(Intent intent, Bundle arguments) {
        if (!isUserLoggedIn()) {
            navigateToLoginActivity.call();
            return;
        }

        if (!isHouseholdAndGroupAvailable()) {
            navigateToDiscoverActivity.call();
            return;
        }

        if (arguments != null) {
            // May be passed from the MainActivity when the MainFragment wasn't selected when a NFC tag was scanned.
            Parcelable parcelable = arguments.getParcelable(MainActivity.NFC_SCANNED_INTENT);
            if (parcelable instanceof Intent) {
                intent = (Intent) parcelable;
            }
        }
        if (intent != null && intent.hasExtra(RetryAction.class.getSimpleName())) {
            handleRetryAction(intent);
        } else if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            handleNfcIntent(intent);
        } else {
            loadPlaybackMetadata();
        }

    }

    private void handleRetryAction(Intent intent) {
        RetryAction retryAction = intent.getParcelableExtra(RetryAction.class.getSimpleName());
        handleRetryAction(retryAction);
    }

    private void handleRetryAction(RetryAction retryAction) {
        if (retryAction == null) {
            return;
        }
        if (retryAction.getRetryActionType() == RetryActionType.RETRY_LOAD_FAVORITE) {
            String retryId = retryAction.getAdditionalId();
            loadAndStartFavorite(retryId);
        } else if (retryAction.getRetryActionType() == RetryActionType.RETRY_LOAD_METADATA) {
            loadPlaybackMetadata();
            loadPlayerState();
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
            if (ndef == null) {
                UserMessage userMessage = UserMessage.create(R.string.tag_invalid_unsupported);
                showSnackbarMessage.postValue(userMessage);
                return;
            }
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            NfcPayload nfcPayload = NfcPayloadUtil.parseMessage(ndefMessage);

            if (nfcPayload == null) {
                soundToPlay.postValue(R.raw.negative);
                showSnackbarMessage.postValue(UserMessage.create(R.string.tag_read_empty));

            } else {
                soundToPlay.postValue(R.raw.positive);
                loadAndStartFavorite(nfcPayload.getFavoriteId());
            }

        } catch (IOException e) {
            UserMessage userMessage = UserMessage.create(R.string.tag_gone);
            showSnackbarMessage.postValue(userMessage);
        } catch (FormatException | SecurityException e) {
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

    void loadAndStartFavorite(String favoriteId) {
        if (refreshTokenIfNeeded(() -> loadAndStartFavorite(favoriteId))) {
            return;
        }

        displayLoading(R.string.starting_favorite);
        albumTitle.setValue("");
        trackTitle.setValue("");
        showAlbumCoverAndTitle(favoriteId);

        String accessToken = sharedPreferencesStore.getAccessToken();
        FavoriteService service = serviceFactory.createFavoriteService(accessToken);

        LoadFavoriteRequest request = new LoadFavoriteRequest(favoriteId, new LoadFavoriteRequest.PlayModes(sharedPreferencesStore.getShufflePlayback()));

        service.loadFavorite(sharedPreferencesStore.getGroupId(), request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadPlayerState();
                    hideLoadingState();
                } else {
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
        PlaybackMetadataService service = serviceFactory.createPlaybackMetadataService(accessToken);

        service.loadPlaybackMetadata(sharedPreferencesStore.getGroupId()).enqueue(new Callback<>() {
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
                        trackTitle.setValue(currentItem.track.name);
                        String imageUrl = currentItem.track.imageUrl;
                        coverImageToLoad.setValue(imageUrl);
                    }
                    loadPlayerState();
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
        displayLoading(R.string.loading_playerstate);

        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = serviceFactory.createPlaybackService(accessToken);
        service.playbackStatus(sharedPreferencesStore.getGroupId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<PlaybackStatus> call, Response<PlaybackStatus> response) {
                if (response.isSuccessful()) {
                    hideLoadingState();
                    PlaybackStatus playbackStatus = response.body();
                    if (playbackStatus == null) {
                        return;
                    }

                    switch (playbackStatus.playbackState) {
                        case PLAYBACK_STATE_PLAYING, PLAYBACK_STATE_BUFFERING -> {
                            // show Play State
                            skipToPreviousButtonVisibility.postValue(View.VISIBLE);
                            playButtonVisibility.postValue(View.GONE);
                            pauseButtonVisibility.postValue(View.VISIBLE);
                            skipToNextButtonVisibility.postValue(View.VISIBLE);
                        }
                        case PLAYBACK_STATE_IDLE, PLAYBACK_STATE_PAUSED -> {
                            // show Pause State
                            skipToPreviousButtonVisibility.postValue(View.VISIBLE);
                            playButtonVisibility.postValue(View.VISIBLE);
                            pauseButtonVisibility.postValue(View.GONE);
                            skipToNextButtonVisibility.postValue(View.VISIBLE);
                        }
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
        if (refreshTokenIfNeeded(this::play)) {
            return;
        }

        displayLoading(R.string.start_playback);
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = serviceFactory.createPlaybackService(accessToken);
        service.play(sharedPreferencesStore.getGroupId()).enqueue(new Callback<>() {
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
        if (refreshTokenIfNeeded(this::pause)) {
            return;
        }

        displayLoading(R.string.stop_playback);
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = serviceFactory.createPlaybackService(accessToken);
        service.pause(sharedPreferencesStore.getGroupId()).enqueue(new Callback<>() {
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

    public void skipToPrevious() {
        if (refreshTokenIfNeeded(this::skipToPrevious)) {
            return;
        }

        displayLoading(R.string.skip_to_previous);
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = serviceFactory.createPlaybackService(accessToken);
        service.skipToPreviousTrack(sharedPreferencesStore.getGroupId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    hideLoadingState();
                    loadPlaybackMetadata();
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

    public void skipToNext() {
        if (refreshTokenIfNeeded(this::skipToNext)) {
            return;
        }

        displayLoading(R.string.skip_to_next);
        String accessToken = sharedPreferencesStore.getAccessToken();
        PlaybackService service = serviceFactory.createPlaybackService(accessToken);
        service.skipToNextTrack(sharedPreferencesStore.getGroupId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    hideLoadingState();
                    loadPlaybackMetadata();
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
            // Try to look up group ID, on success run retry action, on error go to DiscoverActivity
            lookupGroupId(() -> handleRetryAction(retryAction), () -> navigateToDiscoverActivity.postValue(retryAction));
        } else {
            soundToPlay.setValue(R.raw.negative);
            UserMessage userMessage = UserMessage.create(apiError);
            hideLoadingState(userMessage);
        }
    }

    private void handleError(Response<?> response) {
        APIError apiError = ServiceFactory.parseError(response);
        UserMessage userMessage = UserMessage.create(apiError);
        hideLoadingState(userMessage);
    }

    private void lookupGroupId(Runnable onSuccess, Runnable onError) {
        displayLoading(R.string.finding_previous_group);

        String householdId = sharedPreferencesStore.getHouseholdId();
        String groupCoordinatorId = sharedPreferencesStore.getGroupCoordinatorId();

        if (TextUtils.isEmpty(groupCoordinatorId)) {
            // Without coordinator ID we cannot look up the group ID
            onError.run();
            return;
        }

        DiscoverService service = serviceFactory.createDiscoverService(sharedPreferencesStore.getAccessToken());
        Call<Groups> groupsCall = service.getGroups(householdId);
        groupsCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Groups> call, Response<Groups> response) {
                if (response.isSuccessful()) {
                    Groups body = response.body();
                    if (body == null || body.groups == null) {
                        onError.run();
                        return;
                    }

                    for (Group group : body.groups) {
                        if (groupCoordinatorId.equals(group.coordinatorId)) {
                            sharedPreferencesStore.setHouseholdAndGroup(householdId, group.id, groupCoordinatorId);
                            onSuccess.run();
                            return;
                        }
                    }
                }
                onError.run();
            }

            @Override
            public void onFailure(Call<Groups> call, Throwable t) {
                onError.run();
            }
        });
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

    /**
     * Returns the setting if a sound should be played after a tag was scanned.
     * @return true when a sound should be played, false otherwise
     */
    public boolean shouldPlaySounds() {
        return sharedPreferencesStore.getPlaySounds();
    }
}
