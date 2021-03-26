package com.github.kaiwinter.nfcsonos.rest.model;

import com.google.gson.annotations.SerializedName;

import retrofit2.Response;

/**
 * If the SONOS API returns an error body it gets translated into an object of this type.
 * <p>
 * See {@link com.github.kaiwinter.nfcsonos.rest.ServiceFactory#parseError(Response)}
 */
public class APIError {

    /**
     * HTTP code which is returned with {@link #ERROR_RESOURCE_GONE}.
     */
    public static final int ERROR_RESOURCE_GONE_CODE = 410;

    /**
     * errorCode which is returned if a Group doesn't exist anymore.
     */
    public static final String ERROR_RESOURCE_GONE = "ERROR_RESOURCE_GONE";

    @SerializedName("errorCode")
    public String errorCode;

    @SerializedName("reason")
    public String reason;
}
