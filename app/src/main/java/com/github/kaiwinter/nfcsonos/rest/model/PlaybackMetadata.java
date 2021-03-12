
package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

public class PlaybackMetadata {

    @SerializedName("container")
    public Container container;

    @SerializedName("currentItem")
    public CurrentItem currentItem;

    @SerializedName("nextItem")
    public NextItem nextItem;
}
