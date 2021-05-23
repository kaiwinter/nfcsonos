package com.github.kaiwinter.nfcsonos.rest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GroupVolumeService {

    /**
     * Increases or decreases group volume, and unmute the group if muted.
     *
     * @param groupId     groupId to determine the target of the command
     * @param volumeDelta An integer between -100 and 100 (including those values) indicating the amount to increase or decrease the current group volume.
     *                    The group coordinator adds this value to the current group volume and then keeps the result in the range of 0 to 100.
     * @return
     */
    @POST("/control/api/v1/groups/{groupId}/groupVolume/relative")
    Call<Void> setRelativeVolume(@Path("groupId") String groupId, @Body VolumeDeltaRequest volumeDelta);

    class VolumeDeltaRequest {
        private final int volumeDelta;

        public VolumeDeltaRequest(int volumeDelta) {
            this.volumeDelta = volumeDelta;
        }
    }
}
