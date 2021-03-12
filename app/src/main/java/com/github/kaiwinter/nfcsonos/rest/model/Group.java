package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Group {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("coordinatorId")
    public String coordinatorId;

    @SerializedName("playbackState")
    public String playbackState;

    @SerializedName("playerIds")
    public List<String> playerIds;

    @Override
    public String toString() {
        return name;
    }
}