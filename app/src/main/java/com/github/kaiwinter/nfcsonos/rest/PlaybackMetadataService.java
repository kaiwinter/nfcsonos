package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.rest.model.PlaybackMetadata;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * The playbackMetadata namespace includes commands and events that enable your app to receive
 * notification on changes to metadata for the currently playing track and the next track.
 */
public interface PlaybackMetadataService {

    /**
     * Use this command to poll for metadata changes.
     *
     * @param group groupId to determine the target of the command
     * @return Returns a {@link PlaybackMetadata} object for the target group.
     */
    @GET("/control/api/v1/groups/{group}/playbackMetadata")
    Call<PlaybackMetadata> loadPlaybackMetadata(@Path("group") String group);
}
