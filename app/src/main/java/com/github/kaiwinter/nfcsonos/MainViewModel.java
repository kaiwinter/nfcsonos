package com.github.kaiwinter.nfcsonos;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.core.util.Supplier;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.kaiwinter.nfcsonos.activity.main.RetryAction;
import com.github.kaiwinter.nfcsonos.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.rest.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.LoadFavoriteRequest;
import com.github.kaiwinter.nfcsonos.rest.PlaybackMetadataService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.PlaybackMetadata;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {
    public final MutableLiveData<String> trackName = new MutableLiveData<>();

    public final MutableLiveData<Integer> loadingContainerVisibility = new MutableLiveData<>();
    public final MutableLiveData<Integer> loadingDescriptionResId = new MutableLiveData<>();
    public final MutableLiveData<Integer> errorContainerVisibility = new MutableLiveData<>();
    public final MutableLiveData<ErrorMessage> errorMessageMutableLiveData = new MutableLiveData<>();

    public final MutableLiveData<Integer> soundToPlay = new MutableLiveData<>();

    private final SharedPreferencesStore sharedPreferencesStore;
    private final AccessTokenManager accessTokenManager;
    private final FavoriteCache favoriteCache;

    public MainViewModel(SharedPreferencesStore sharedPreferencesStore, AccessTokenManager accessTokenManager, FavoriteCache favoriteCache) {
        this.sharedPreferencesStore = sharedPreferencesStore;
        this.accessTokenManager = accessTokenManager;
        this.favoriteCache = favoriteCache;
    }

    public String getAccessToken() {
        return sharedPreferencesStore.getAccessToken();
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
            accessTokenManager.refreshAccessToken(getActivity(), runnable, this::hideLoadingState);
            return true;
        }
        return false;
    }

    public void loadAndStartFavorite(String favoriteId) {
        displayLoading(R.string.starting_favorite);

        if (refreshTokenIfNeeded(() -> loadAndStartFavorite(favoriteId))) {
            return;
        }

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

                    APIError apiError = ServiceFactory.parseError(response);
                    if (response.code() == APIError.ERROR_RESOURCE_GONE_CODE && APIError.ERROR_RESOURCE_GONE.equals(apiError.errorCode)) {
                        startDiscoverActivity(RetryAction.RETRY_LOAD_FAVORITE, favoriteId);
                        Snackbar.make(binding.coordinator, getString(R.string.group_id_changed), Snackbar.LENGTH_LONG).show();
                        hideLoadingState();
                        return;
                    }

                    ErrorMessage errorMessage = ErrorMessage.createAPIErrorErrorMessage(apiError);
                    hideLoadingState(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                soundToPlay.setValue(R.raw.negative);
                ErrorMessage errorMessage = ErrorMessage.createResourceIdErrorMessage(R.string.error_starting_favorite, t.getMessage());
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
                if (!response.isSuccessful()) {
                    APIError apiError = ServiceFactory.parseError(response);
                    if (response.code() == APIError.ERROR_RESOURCE_GONE_CODE && APIError.ERROR_RESOURCE_GONE.equals(apiError.errorCode)) {
                        startDiscoverActivity(RetryAction.RETRY_LOAD_METADATA, null);
                        Snackbar.make(binding.coordinator, getString(R.string.group_id_changed), Snackbar.LENGTH_LONG).show();
                        hideLoadingState();
                        return;
                    }

                    ErrorMessage errorMessage = ErrorMessage.createAPIErrorErrorMessage(apiError);
                    hideLoadingState(errorMessage);
                    return;
                }

                hideLoadingState();
                PlaybackMetadata playbackMetadata = response.body();
                if (playbackMetadata == null || playbackMetadata.container == null) {
                    return;
                }

                trackName.setValue(playbackMetadata.container.name);

                String imageUrl = playbackMetadata.currentItem.track.imageUrl;
                loadAndShowCoverImage(imageUrl);
            }

            @Override
            public void onFailure(Call<PlaybackMetadata> call, Throwable t) {
                hideLoadingState(t.getMessage());
            }
        });
    }

    private void showCoverImage(String favoriteId) {
        favoriteCache.getFavorite(favoriteId, storedFavorite -> {
            trackName.setValue(storedFavorite.name);

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

    private void displayLoading(Integer resId) {
        loadingContainerVisibility.setValue(View.VISIBLE);
        loadingDescriptionResId.setValue(resId);
        errorContainerVisibility.setValue(View.GONE);
    }

    private void hideLoadingState() {
        loadingContainerVisibility.setValue(View.GONE);
    }

    private void hideLoadingState(String message) {
        ErrorMessage errorMessage = ErrorMessage.createSimpleStringErrorMessage(message);
        hideLoadingState(errorMessage);
    }

    private void hideLoadingState(ErrorMessage errorMessage) {
        errorMessageMutableLiveData.setValue(errorMessage);
        hideLoadingState();
    }

    public static class ErrorMessage {
        enum Type {
            SIMPLE_STRING,
            RESOURCE_ID_WITH_STRING,
            API_ERROR;
        }

        private Type type;
        private String simpleStringMessage;

        private int resId;
        private String replacementString;

        private APIError apiError;

        static ErrorMessage createSimpleStringErrorMessage(String message) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.type = Type.SIMPLE_STRING;
            errorMessage.simpleStringMessage = message;
            return errorMessage;
        }

        static ErrorMessage createResourceIdErrorMessage(int resId, String replacementString) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.type = Type.RESOURCE_ID_WITH_STRING;
            errorMessage.resId = resId;
            errorMessage.replacementString = replacementString;
            return errorMessage;
        }

        static ErrorMessage createAPIErrorErrorMessage(APIError apiError) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.type = Type.API_ERROR;
            errorMessage.apiError = apiError;
            return errorMessage;
        }

        public String getMessage(Context context) {
            if (type == Type.SIMPLE_STRING) {
                return simpleStringMessage;
            } else if (type == Type.RESOURCE_ID_WITH_STRING) {
                return context.getString(resId, replacementString);
            } else if (type == Type.API_ERROR) {
                return apiError.toMessage(context);
            } else {
                return "Unknown error type: " + type.name();
            }
        }
    }
}