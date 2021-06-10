package com.github.kaiwinter.nfcsonos.activity.main;

/**
 * If an action which runs on the {@link MainActivity} fails because of a changed household or group
 * the {@link com.github.kaiwinter.nfcsonos.activity.discover.DiscoverActivity} is started to choose
 * a new one. Afterwards the {@link MainActivity} executes the previous action with the help of this
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
        public static final String ID_FOR_RETRY_ACTION = "ID_FOR_RETRY_ACTION";
    }
}