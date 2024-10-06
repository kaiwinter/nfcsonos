package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.rest.model.PlaybackStatus;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * The playback namespace includes commands and events that enable your app to initiate and pause
 * group playback, and skip to next or previous track. Your app can also control and update playback
 * status and play modes, and receive notifications about those states and playback errors.
 */
public interface PlaybackService {

    /**
     * Use this command to get the current playback status (transport state) for the group, such as
     * PLAYBACK_STATE_IDLE, PLAYBACK_STATE_BUFFERING, or PLAYBACK_STATE_PLAYING. See the
     * playbackStatus object for details.
     *
     * @return Returns a playbackStatus object representing the state of the group.
     * If not successful, returns a globalError.
     */
    @GET("/control/api/v1/groups/{groupId}/playback")
    Call<PlaybackStatus> playbackStatus(@Path("groupId") String groupId);

    /**
     * Use the play command to initiate group playback.
     *
     * @param groupId groupId to determine the target of the command
     * @return Returns an empty body with a success value of true if playback was started on the
     * group or if the group was already playing.
     * In the event of a failure, returns a globalError or a playbackError. For example, if there’s
     * no audio source loaded on the group, your app will receive an ERROR_PLAYBACK_FAILED.
     */
    @Headers("Content-Type: application/json")
    @POST("/control/api/v1/groups/{groupId}/playback/play")
    Call<Void> play(@Path("groupId") String groupId);

    /**
     * Use the pause command to pause group playback.
     *
     * @param groupId groupId to determine the target of the command
     * @return Returns an empty body with a success value of true if playback was paused on the
     * group or if the group was already paused or idle.
     * In the event of a failure, returns a globalError or a playbackError. For example, if there’s
     * no audio source loaded on the group, your app will receive an ERROR_PLAYBACK_NO_CONTENT.
     */
    @Headers("Content-Type: application/json")
    @POST("/control/api/v1/groups/{groupId}/playback/pause")
    Call<Void> pause(@Path("groupId") String groupId);

    /**
     * Use the skipToPreviousTrack command in the playback namespace to skip to the previous track.
     *
     * @param groupId groupId to determine the target of the command
     * @return Returns an empty body
     */
    @Headers("Content-Type: application/json")
    @POST("/control/api/v1/groups/{groupId}/playback/skipToPreviousTrack")
    Call<Void> skipToPreviousTrack(@Path("groupId") String groupId);

    /**
     * Use the skipToNextTrack command in the playback namespace to skip to the next track.
     *
     * @param groupId groupId to determine the target of the command
     * @return Returns an empty body
     */
    @Headers("Content-Type: application/json")
    @POST("/control/api/v1/groups/{groupId}/playback/skipToNextTrack")
    Call<Void> skipToNextTrack(@Path("groupId") String groupId);
}
