package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.rest.model.PlaybackMetadata;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PlaybackMetadataService {

    @GET("/control/api/v1/groups/{group}/playbackMetadata")
    Call<PlaybackMetadata> loadPlaybackMetadata(@Path("group") String group);

}
