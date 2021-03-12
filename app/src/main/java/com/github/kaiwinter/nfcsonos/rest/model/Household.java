package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

public class Household {

    @SerializedName("id")
    public String id;

    @Override
    public String toString() {
        return id;
    }
}
