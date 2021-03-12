
package com.github.kaiwinter.nfcsonos.rest.model;


import com.google.gson.annotations.SerializedName;

public class Id {

    @SerializedName("serviceId")
    public String serviceId;

    @SerializedName("objectId")
    public String objectId;

    @SerializedName("accountId")
    public String accountId;
}
