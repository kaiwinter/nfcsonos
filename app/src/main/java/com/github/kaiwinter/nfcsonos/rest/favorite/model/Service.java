
package com.github.kaiwinter.nfcsonos.rest.favorite.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Service {

    @SerializedName("name")
    public String name;

    @SerializedName("id")
    public String id;

    @SerializedName("images")
    public List<Image> images;
}
