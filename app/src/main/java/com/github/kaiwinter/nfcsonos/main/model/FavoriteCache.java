package com.github.kaiwinter.nfcsonos.main.model;

import android.content.Context;

import androidx.core.util.Consumer;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.rest.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.Favorites;
import com.github.kaiwinter.nfcsonos.rest.model.Item;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.UserMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A cache to store all favorites of the user (return of {@link FavoriteService#getFavorites(String)}.
 */
public class FavoriteCache {

    private static final String CACHE_FILENAME = "favorites";

    private final Context context;
    private final SharedPreferencesStore sharedPreferencesStore;
    private final AccessTokenManager accessTokenManager;
    private final ServiceFactory serviceFactory;

    public FavoriteCache(Context context, ServiceFactory serviceFactory) {
        this.context = context;
        this.sharedPreferencesStore = new SharedPreferencesStore(context);
        this.accessTokenManager = new AccessTokenManager(context);
        this.serviceFactory = serviceFactory;
    }

    /**
     * Tries to find the favorite in the cache. If it is not there all favorites are loaded and the favorite is returned (if available)
     *
     * @param favoriteId the ID of the favorite
     * @param onSuccess  {@link Consumer} which is called with the loaded favorites
     * @param onError    {@link Consumer} which is called with a possible error message
     */
    public void getFavorite(String favoriteId, Consumer<StoredFavorite> onSuccess, Consumer<UserMessage> onError) {
        File file = new File(context.getFilesDir(), CACHE_FILENAME);
        if (file.exists()) {
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
                Map<String, StoredFavorite> storedFavorites = (Map<String, StoredFavorite>) inputStream.readObject();
                StoredFavorite storedFavorite = storedFavorites.get(favoriteId);
                if (storedFavorite == null) {
                    updateFavorites(favoriteId, onSuccess, onError);
                    return;
                }
                onSuccess.accept(storedFavorite);
                return;
            } catch (IOException | ClassNotFoundException e) {
                UserMessage userMessage = UserMessage.create(R.string.error, e.getMessage());
                onError.accept(userMessage);
            }
        }
        updateFavorites(favoriteId, onSuccess, onError);
    }

    void updateFavorites(String favoriteId, Consumer<StoredFavorite> onSuccess, Consumer<UserMessage> onError) {
        loadFavorites(favorites -> {
            for (Item item : favorites.items) {
                if (favoriteId.equals(item.id)) {
                    StoredFavorite foundFavorite = StoredFavorite.fromItem(item);
                    onSuccess.accept(foundFavorite);
                    return;
                }
            }
            UserMessage userMessage = UserMessage.create(R.string.favorite_not_found_in_cache, favoriteId);
            onError.accept(userMessage);
        }, onError);
    }

    /**
     * Gets the list of all favorites from the REST service.
     *
     * @param onSuccess {@link Consumer} which is called with the loaded favorites
     * @param onError   {@link Consumer} which is called with a possible error message
     */
    public void loadFavorites(Consumer<Favorites> onSuccess, Consumer<UserMessage> onError) {
        if (refreshTokenIfNeeded(() -> loadFavorites(onSuccess, onError), onError)) {
            return;
        }

        String accessToken = sharedPreferencesStore.getAccessToken();
        FavoriteService service = serviceFactory.createFavoriteService(accessToken);

        service.getFavorites(sharedPreferencesStore.getHouseholdId()).enqueue(new Callback<Favorites>() {
            @Override
            public void onResponse(Call<Favorites> call, Response<Favorites> response) {
                if (response.isSuccessful()) {
                    Favorites favorites = response.body();
                    onSuccess.accept(favorites);
                    try {
                        persistFavorites(favorites.items);
                    } catch (IOException e) {
                        UserMessage userMessage = UserMessage.create(R.string.error, e.getMessage());
                        onError.accept(userMessage);
                    }
                } else {
                    APIError apiError = ServiceFactory.parseError(response);
                    UserMessage userMessage = UserMessage.create(apiError);
                    onError.accept(userMessage);
                }
            }

            @Override
            public void onFailure(Call<Favorites> call, Throwable t) {
                UserMessage userMessage = UserMessage.create(R.string.error, t.getMessage());
                onError.accept(userMessage);
            }
        });
    }

    private void persistFavorites(List<Item> items) throws IOException {
        Map<String, StoredFavorite> storedFavorites = new HashMap<>();
        for (Item item : items) {
            storedFavorites.put(item.id, StoredFavorite.fromItem(item));
        }

        File file = new File(context.getFilesDir(), CACHE_FILENAME);
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            outputStream.writeObject(storedFavorites);
        }
    }

    private boolean refreshTokenIfNeeded(Runnable runnable, Consumer<UserMessage> onError) {
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            accessTokenManager.refreshAccessToken(runnable, onError);
            return true;
        }
        return false;
    }

}