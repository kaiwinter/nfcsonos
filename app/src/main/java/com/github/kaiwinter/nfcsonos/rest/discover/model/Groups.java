package com.github.kaiwinter.nfcsonos.rest.discover.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Groups {

    @SerializedName("groups")
    public List<Group> groups;

    @SerializedName("players")
    public List<Player> players;

    @SerializedName("partial")
    public Boolean partial;
}