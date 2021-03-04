package com.github.kaiwinter.nfcsonos.rest.favorite;

public class LoadFavoriteRequest {
    private final String favoriteId;
    private final boolean playOnCompletion = true;
    private final String action = "REPLACE";
    private final PlayModes playModes = new PlayModes();

    public LoadFavoriteRequest(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    static class PlayModes {
        boolean shuffle = false;
        boolean repeat = false;
    }
}
