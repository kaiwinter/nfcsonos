package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

public class Household {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @Override
    public String toString() {
        return name;
    }
}
