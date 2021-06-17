package com.github.kaiwinter.nfcsonos.util;

import android.content.Context;

import com.github.kaiwinter.nfcsonos.rest.model.APIError;

/**
 * Transports a message from a ViewModel (or any Context-less object) to a Fragment (or any other
 * Context-aware object). {@link #getMessage(Context)} is used to get the error message as a String.
 */
public class UserMessage {
    /**
     * Defines different types of {@link UserMessage}s.
     */
    enum Type {
        /**
         * A plain String.
         */
        SIMPLE_STRING,

        /**
         * A resource ID (R.string).
         */
        RESOURCE_ID,

        /**
         * A resource ID (R.string) and a string which is used as formatArgs.
         */
        RESOURCE_ID_WITH_STRING,

        /**
         * A {@link APIError}.
         */
        API_ERROR
    }

    private Type type;
    private String simpleStringMessage;

    private int resId;
    private String replacementString;

    private APIError apiError;

    /**
     * Creates an {@link UserMessage} wrapper for a simple String error message.
     *
     * @param message the error message
     * @return an {@link UserMessage} object
     */
    public static UserMessage create(String message) {
        UserMessage userMessage = new UserMessage();
        userMessage.type = Type.SIMPLE_STRING;
        userMessage.simpleStringMessage = message;
        return userMessage;
    }

    /**
     * Creates an {@link UserMessage} wrapper for an resource ID (R.string).
     *
     * @param resId the resource ID
     * @return an {@link UserMessage} object
     */
    public static UserMessage create(int resId) {
        UserMessage userMessage = new UserMessage();
        userMessage.type = Type.RESOURCE_ID_WITH_STRING;
        userMessage.resId = resId;
        return userMessage;
    }

    /**
     * Creates an {@link UserMessage} wrapper for an resource ID (R.string) which uses an string to be substituted into the resource string.
     *
     * @param resId             the resource ID
     * @param replacementString the string which is used as formatArgs.
     * @return an {@link UserMessage} object
     */
    public static UserMessage create(int resId, String replacementString) {
        UserMessage userMessage = new UserMessage();
        userMessage.type = Type.RESOURCE_ID_WITH_STRING;
        userMessage.resId = resId;
        userMessage.replacementString = replacementString;
        return userMessage;
    }

    /**
     * Creates an {@link UserMessage} wrapper for an error which originates from an {@link APIError}.
     *
     * @param apiError the {@link APIError}
     * @return an {@link UserMessage} object
     */
    public static UserMessage create(APIError apiError) {
        UserMessage userMessage = new UserMessage();
        userMessage.type = Type.API_ERROR;
        userMessage.apiError = apiError;
        return userMessage;
    }

    /**
     * Returns the message from this {@link UserMessage}.
     *
     * @param context some of the error {@link Type}s needs a context to be evaluated. Mainly to
     *                access resources to build formatted error messages.
     * @return the message from this {@link UserMessage}
     */
    public String getMessage(Context context) {
        if (context == null) {
            return "";
        }
        if (type == Type.SIMPLE_STRING) {
            return simpleStringMessage;
        } else if (type == Type.RESOURCE_ID) {
            return context.getString(resId);
        } else if (type == Type.RESOURCE_ID_WITH_STRING) {
            return context.getString(resId, replacementString);
        } else if (type == Type.API_ERROR) {
            return apiError.toMessage(context);
        } else {
            return "Unknown error type: " + type.name();
        }
    }
}