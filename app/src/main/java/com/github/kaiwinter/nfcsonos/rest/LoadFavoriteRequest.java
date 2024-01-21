package com.github.kaiwinter.nfcsonos.rest;

public class LoadFavoriteRequest {

    // The identifier of the favorite
    private final String favoriteId;

    // (Optional) If true, the player automatically starts playback. If false or not provided, the player remains in the PLAYBACK_IDLE state.
    private final boolean playOnCompletion = true;

    // (Optional) Controls how the the player inserts the favorite into the shared queue, such as append, insert, insert next, or replace. If omitted, defaults to append.
    private final String action = "REPLACE";

    // (Optional) Defines the functionality of one or more play modes. You can set these to customize shuffle, repeat, repeat-one and crossfade.
    private final PlayModes playModes;

    public LoadFavoriteRequest(String favoriteId, PlayModes playModes) {
        this.favoriteId = favoriteId;
        this.playModes = playModes;
    }

    public String getFavoriteId() {
        return favoriteId;
    }

    public PlayModes getPlayModes() {
        return playModes;
    }

    public static class PlayModes {
        private boolean shuffle = false;
        private boolean repeat = false;

        public PlayModes(Boolean shuffle) {
            this.shuffle = shuffle;
        }

        public boolean isShuffle() {
            return shuffle;
        }
    }
}
