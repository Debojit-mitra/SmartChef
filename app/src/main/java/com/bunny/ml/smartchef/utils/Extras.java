package com.bunny.ml.smartchef.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.bunny.ml.smartchef.R;

public class Extras {
    public static boolean isDarkMode(Activity activity) {
        return (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public void hideWithFadeAnimation(View view) {
        view.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                        view.setAlpha(1f); // Reset alpha for future animations
                    }
                });
    }

    public void showWithFadeAnimation(final View view) {
        // Set alpha to 0 but make view VISIBLE
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Create fade in animation
        view.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setAlpha(1f);
                    }
                });
    }

    public static void slideDownToPosition(final View viewToSlide, final View targetPosition) {
        // Get the locations of both views
        int[] slideViewLocation = new int[2];
        int[] targetLocation = new int[2];

        viewToSlide.getLocationInWindow(slideViewLocation);
        targetPosition.getLocationInWindow(targetLocation);

        // Calculate the distance to slide (difference in Y positions)
        float slideDistance = targetLocation[1] - slideViewLocation[1];

        // Start the animation from current position
        viewToSlide.setTranslationY(0);

        // Animate to the new position
        viewToSlide.animate()
                .translationY(slideDistance)
                .setDuration(500)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(null);
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static int getSpanCount(@NonNull Context context, int layoutType) {
        int screenWidth;
        int screenHeight;
        int itemWidth;

        // Get the WindowManager and screen dimensions
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            screenWidth = windowMetrics.getBounds().width();
            screenHeight = windowMetrics.getBounds().height();

            // Convert the genre item width dimension from dp to pixels
            itemWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    context.getResources().getDimension(R.dimen.genre_item_width),
                    context.getResources().getDisplayMetrics()
            );
        } else {
            // Fallback values if windowManager is not available
            screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            itemWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    100, // Default item width in dp
                    context.getResources().getDisplayMetrics()
            );
        }

        boolean isLargeScreen = screenWidth >= context.getResources().getDimensionPixelSize(R.dimen.large_screen_width);
        boolean isLandscape = screenWidth > screenHeight;
        int calculatedSpanCount = screenWidth / itemWidth;

        switch (layoutType) {
            case 1:
                // For layout_1
                if (isLandscape) {
                    return isLargeScreen ? Math.max(4, calculatedSpanCount) : 3;
                } else {
                    return isLargeScreen ? Math.max(3, calculatedSpanCount) : 2;
                }
            case 2:
                // For layout_2
                if (isLandscape) {
                    return isLargeScreen ? Math.max(5, calculatedSpanCount) : 4;
                } else {
                    return isLargeScreen ? Math.max(4, calculatedSpanCount) : 3;
                }
            case 3:
                // For layout_3
                if (isLandscape) {
                    return isLargeScreen ? 3 : 2;
                } else {
                    return isLargeScreen ? 2 : 1;
                }
            default:
                // Default case
                if (isLandscape) {
                    return Math.max(3, calculatedSpanCount);
                } else {
                    return Math.max(2, calculatedSpanCount);
                }
        }
    }


}
