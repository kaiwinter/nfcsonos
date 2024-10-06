package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

/**
 * The set of allowed transport actions as defined by the playback policies calculated by the player.
 */
public class AvailablePlaybackActions {
    @SerializedName("canPlay")
    public Boolean canPlay;

    @SerializedName("canSkip")
    public Boolean canSkip;

    @SerializedName("canSkipBack")
    public Boolean canSkipBack;

    @SerializedName("canSkipToPrevious")
    public Boolean canSkipToPrevious;

    @SerializedName("canPause")
    public Boolean canPause;

    @SerializedName("canStop")
    public Boolean canStop;

}
