package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

public class APIError {

    @SerializedName("errorCode")
    public String errorCode;

    @SerializedName("reason")
    public String reason;
}
