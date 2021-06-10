package com.github.kaiwinter.nfcsonos.util;

import android.app.Activity;
import android.view.View;

import com.github.kaiwinter.nfcsonos.R;
import com.google.android.material.snackbar.Snackbar;

public class SnackbarUtil {
    private SnackbarUtil() {
        // empty
    }

    /**
     * Shows a Snackbar above the bottom navigation bar.
     *
     * @param activity the Activity to lookup the nav_view
     * @param view     The view to find a parent from.
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Can be LENGTH_SHORT, LENGTH_LONG, LENGTH_INDEFINITE, or a custom duration in milliseconds.
     */
    public static void createAndShowSnackbarAboveBottomNav(Activity activity, View view, String text, int duration) {
        Snackbar snackbar = Snackbar.make(view, text, duration);
        snackbar.setAnchorView(activity.findViewById(R.id.nav_view));
        snackbar.show();
    }

    /**
     * Shows a Snackbar above the bottom navigation bar.
     *
     * @param activity the Activity to lookup the nav_view
     * @param view     The view to find a parent from.
     * @param resId    The resource id of the string resource to use.
     * @param duration How long to display the message. Can be LENGTH_SHORT, LENGTH_LONG, LENGTH_INDEFINITE, or a custom duration in milliseconds.
     */
    public static void createAndShowSnackbarAboveBottomNav(Activity activity, View view, int resId, int duration) {
        createAndShowSnackbarAboveBottomNav(activity, view, activity.getString(resId), duration);
    }
}
