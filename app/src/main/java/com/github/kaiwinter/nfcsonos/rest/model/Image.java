
package com.github.kaiwinter.nfcsonos.rest.model;


import com.google.gson.annotations.SerializedName;

public class Image {

    @SerializedName("url")
    public String url;

    @SerializedName("height")
    public Integer height;

    @SerializedName("width")
    public Integer width;
}
