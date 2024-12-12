package com.bunny.ml.smartchef.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

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


}
