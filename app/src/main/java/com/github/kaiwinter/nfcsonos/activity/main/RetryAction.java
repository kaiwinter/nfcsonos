package com.github.kaiwinter.nfcsonos.activity.main;

/**
 * Holds a {@link RetryActionType} and an optional ID.
 */
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

    /**
     * If an action which runs on the {@link MainFragmentViewModel} fails because of a changed household or group
     * the {@link com.github.kaiwinter.nfcsonos.activity.discover.DiscoverActivity} is started to choose
     * a new one. Afterwards the {@link MainFragment} executes the previous action with the help of this
     * enum.
     */
    public enum RetryActionType {
        /**
         * Retry loading a favorite.
         */
        RETRY_LOAD_FAVORITE,

        /**
         * Retry loading playback metadata.
         */
        RETRY_LOAD_METADATA;

        public static class INTENT_EXTRA_KEYS {
            /** Intent extra key to transport an optional ID for the retry action. */
            public static final String ID_FOR_RETRY_ACTION = "ID_FOR_RETRY_ACTION";
        }
    }
}
