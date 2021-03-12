
package com.github.kaiwinter.nfcsonos.rest.model;


import com.google.gson.annotations.SerializedName;

public class Album {

    @SerializedName("name")
    public String name;

    @SerializedName("explicit")
    public Boolean explicit;
}
