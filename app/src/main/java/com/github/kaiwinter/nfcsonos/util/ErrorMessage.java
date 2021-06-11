package com.github.kaiwinter.nfcsonos.util;

import android.content.Context;

import com.github.kaiwinter.nfcsonos.rest.model.APIError;

/**
 * Transports different error types from a ViewModel (or any Context-less object) to a Fragment (or
 * any other Context-aware object). {@link #getMessage(Context)} is used to get the error message
 * as a String.
 */
public class ErrorMessage {
    /**
     * Defines different types of {@link ErrorMessage}s.
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
        API_ERROR;
    }

    private Type type;
    private String simpleStringMessage;

    private int resId;
    private String replacementString;

    private APIError apiError;

    /**
     * Creates an {@link ErrorMessage} wrapper for a simple String error message.
     *
     * @param message the error message
     * @return an {@link ErrorMessage} object
     */
    public static ErrorMessage create(String message) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.SIMPLE_STRING;
        errorMessage.simpleStringMessage = message;
        return errorMessage;
    }

    /**
     * Creates an {@link ErrorMessage} wrapper for an resource ID (R.string).
     *
     * @param resId the resource ID
     * @return an {@link ErrorMessage} object
     */
    public static ErrorMessage create(int resId) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.RESOURCE_ID_WITH_STRING;
        errorMessage.resId = resId;
        return errorMessage;
    }

    /**
     * Creates an {@link ErrorMessage} wrapper for an resource ID (R.string) which uses an string to be substituted into the resource string.
     *
     * @param resId             the resource ID
     * @param replacementString the string which is used as formatArgs.
     * @return an {@link ErrorMessage} object
     */
    public static ErrorMessage create(int resId, String replacementString) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.RESOURCE_ID_WITH_STRING;
        errorMessage.resId = resId;
        errorMessage.replacementString = replacementString;
        return errorMessage;
    }

    /**
     * Creates an {@link ErrorMessage} wrapper for an error which originates from an {@link APIError}.
     *
     * @param apiError the {@link APIError}
     * @return an {@link ErrorMessage} object
     */
    public static ErrorMessage create(APIError apiError) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.API_ERROR;
        errorMessage.apiError = apiError;
        return errorMessage;
    }

    /**
     * Returns the message from this {@link ErrorMessage}.
     *
     * @param context some of the error {@link Type}s needs a context to be evaluated. Mainly to
     *                access resources to build formatted error messages.
     * @return the message from this {@link ErrorMessage}
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