package com.github.kaiwinter.nfcsonos.rest.playbackmetadata;

import com.github.kaiwinter.nfcsonos.rest.playbackmetadata.model.PlaybackMetadata;
import retrofit2.Call;
import retrofit2.http.GET;

public interface PlaybackMetadataService {

    @GET("/control/api/v1/groups/FIXME/playbackMetadata")
    Call<PlaybackMetadata> loadPlaybackMetadata();

}
