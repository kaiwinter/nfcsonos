package com.github.kaiwinter.nfcsonos.util;

import android.content.Context;

import com.github.kaiwinter.nfcsonos.rest.model.APIError;

/**
 * Transports different error types from a ViewModel to the Fragment. {@link #getMessage(Context)}
 * is used to get the error message as a String.
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

    public static ErrorMessage createSimpleStringErrorMessage(String message) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.SIMPLE_STRING;
        errorMessage.simpleStringMessage = message;
        return errorMessage;
    }

    public static ErrorMessage createResourceIdErrorMessage(int resId, String replacementString) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.RESOURCE_ID_WITH_STRING;
        errorMessage.resId = resId;
        errorMessage.replacementString = replacementString;
        return errorMessage;
    }

    public static ErrorMessage createAPIErrorErrorMessage(APIError apiError) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.type = Type.API_ERROR;
        errorMessage.apiError = apiError;
        return errorMessage;
    }

    public String getMessage(Context context) {
        if (type == Type.SIMPLE_STRING) {
            return simpleStringMessage;
        } else if (type == Type.RESOURCE_ID_WITH_STRING) {
            return context.getString(resId, replacementString);
        } else if (type == Type.API_ERROR) {
            return apiError.toMessage(context);
        } else {
            return "Unknown error type: " + type.name();
        }
    }
}