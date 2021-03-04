package com.github.kaiwinter.nfcsonos.rest.favorite;

import com.github.kaiwinter.nfcsonos.rest.favorite.model.Favorites;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface FavoriteService {

    @POST("/control/api/v1/groups/FIXME/favorites")
    Call<Void> loadFavorite(@Body LoadFavoriteRequest request);

    @GET("/control/api/v1/households/FIXME/favorites")
    Call<Favorites> loadFavorites();
}
