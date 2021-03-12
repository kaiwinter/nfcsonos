
package com.github.kaiwinter.nfcsonos.rest.playbackmetadata.model;


import com.github.kaiwinter.nfcsonos.rest.favorite.model.Image;
import com.github.kaiwinter.nfcsonos.rest.favorite.model.Service;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Track {

    @SerializedName("type")
    public String type;

    @SerializedName("name")
    public String name;

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("images")
    public List<Image> images;

    @SerializedName("album")
    public Album album;

    @SerializedName("artist")
    public Artist artist;

    @SerializedName("id")
    public Id id;

    @SerializedName("service")
    public Service service;

    @SerializedName("durationMillis")
    public Integer durationMillis;

    @SerializedName("explicit")
    public Boolean explicit;
}
