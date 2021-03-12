
package com.github.kaiwinter.nfcsonos.rest.playbackmetadata.model;


import com.github.kaiwinter.nfcsonos.rest.favorite.model.Image;
import com.github.kaiwinter.nfcsonos.rest.favorite.model.Service;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Container {

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    @SerializedName("id")
    public Id id;

    @SerializedName("service")
    public Service service;

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("images")
    public List<Image> images;

    @SerializedName("explicit")
    public Boolean explicit;
}
