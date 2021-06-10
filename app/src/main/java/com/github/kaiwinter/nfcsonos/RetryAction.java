package com.github.kaiwinter.nfcsonos;

import com.github.kaiwinter.nfcsonos.activity.main.RetryActionType;

public class RetryAction {
    private final RetryActionType retryActionType;
    private String additionalId;

    public RetryAction(RetryActionType retryActionType) {
        this.retryActionType = retryActionType;
    }

    public RetryAction(RetryActionType retryActionType, String additionalId) {
        this.retryActionType = retryActionType;
        this.additionalId = additionalId;
    }

    public RetryActionType getRetryActionType() {
        return retryActionType;
    }

    public String getAdditionalId() {
        return additionalId;
    }
}
