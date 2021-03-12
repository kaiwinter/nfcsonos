
package com.github.kaiwinter.nfcsonos.rest.favorite.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Favorites {

    @SerializedName("version")
    public String version;

    @SerializedName("items")
    public List<Item> items;
}