package com.github.kaiwinter.nfcsonos.main.nfc;

import com.google.gson.annotations.SerializedName;

/**
 * The payload which is stored on a NFC tag.
 */
public class NfcPayload {

    @SerializedName("favoriteId")
    private final String favoriteId;

    public NfcPayload(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    public String getFavoriteId() {
        return favoriteId;
    }
}
