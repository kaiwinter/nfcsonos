package com.github.kaiwinter.nfcsonos.rest.playbackmetadata;

import androidx.annotation.Nullable;

import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.playbackmetadata.model.PlaybackMetadata;

import java.io.IOException;

import retrofit2.Response;

public class PlaybackMetadataAction {

    private int status = -1;
    private PlaybackMetadata response;
    private String error;

    public void execute(@Nullable String accessToken, @Nullable String idToken, String group) {
        PlaybackMetadataService service = ServiceFactory.createPlaybackMetadataService(accessToken);

        try {
            Response<PlaybackMetadata> execute = service.loadPlaybackMetadata(group).execute();
            status = execute.code();
            this.error = execute.errorBody() == null ? "" : execute.errorBody().string();
            response = execute.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getStatus() {
        return status;
    }

    public PlaybackMetadata getResponse() {
        return response;
    }

    public String getError() {
        return error;
    }
}
