
package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Item {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("images")
    public List<Image> images = null;

    @SerializedName("service")
    public Service service;

    @Override
    public String toString() {
        return name;
    }
}
