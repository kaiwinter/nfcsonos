package com.github.kaiwinter.nfcsonos.rest.discover.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Player {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("websocketUrl")
    public String websocketUrl;

    @SerializedName("softwareVersion")
    public String softwareVersion;

    @SerializedName("apiVersion")
    public String apiVersion;

    @SerializedName("minApiVersion")
    public String minApiVersion;

    @SerializedName("isUnregistered")
    public Boolean isUnregistered;

    @SerializedName("capabilities")
    public List<String> capabilities = null;

    @SerializedName("deviceIds")
    public List<String> deviceIds;
}