
package com.github.kaiwinter.nfcsonos.rest.playbackmetadata.model;


import com.google.gson.annotations.SerializedName;

public class Artist {

    @SerializedName("name")
    public String name;

    @SerializedName("explicit")
    public Boolean explicit;
}
