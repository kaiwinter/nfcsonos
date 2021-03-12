package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.rest.model.Favorites;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FavoriteService {

    @POST("/control/api/v1/groups/{group}/favorites")
    Call<Void> loadFavorite(@Path("group") String group, @Body LoadFavoriteRequest request);

    @GET("/control/api/v1/households/{household}/favorites")
    Call<Favorites> loadFavorites(@Path("household") String household);
}
