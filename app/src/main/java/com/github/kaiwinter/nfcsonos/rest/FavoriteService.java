package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.rest.model.Favorites;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FavoriteService {

    /**
     * Activate a favorite within the default playback session. Operates as if the listener selected the favorite using the Sonos app. This command interrupts any active private playback sessions.
     *
     * @param groupId groupId to determine the target of the command
     * @param request
     * @return Returns an empty body with a success value of true if successful. In the event of a failure, returns a globalError.
     */
    @POST("/control/api/v1/groups/{groupId}/favorites")
    Call<Void> loadFavorite(@Path("groupId") String groupId, @Body LoadFavoriteRequest request);

    /**
     * Get the list of favorites for a household.
     *
     * @param household householdId to determine the target of the command
     * @return a {@link Favorites} object, which is an array of favorite objects with a version number. In the event of a failure, returns a globalError.
     */
    @GET("/control/api/v1/households/{household}/favorites")
    Call<Favorites> getFavorites(@Path("household") String household);
}
