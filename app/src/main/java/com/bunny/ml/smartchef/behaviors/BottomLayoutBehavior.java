package com.bunny.ml.smartchef.behaviors;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.bunny.ml.smartchef.R;

public class BottomLayoutBehavior extends CoordinatorLayout.Behavior<View> {
    private int totalDyDistance = 0;
    private boolean isShowing = true;
    private Context context;

    public BottomLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull View child,
                                       @NonNull View directTargetChild,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                               @NonNull View child,
                               @NonNull View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed,
                               int type,
                               @NonNull int[] consumed) {
        totalDyDistance += dyConsumed;

        if (totalDyDistance > 0 && isShowing) {
            // Scrolling up, hide the bottom layout
            hideBottomLayout(child);
        } else if (totalDyDistance < 0 && !isShowing) {
            // Scrolling down, show the bottom layout
            showBottomLayout(child);
        }

        // Reset total distance when direction changes
        if ((dyConsumed > 0 && totalDyDistance < 0) || (dyConsumed < 0 && totalDyDistance > 0)) {
            totalDyDistance = dyConsumed;
        }
    }

    private void hideBottomLayout(View child) {
        child.animate()
                .translationY(child.getHeight())
                .setDuration(200)
                .withEndAction(() -> updateNavigationBarColor(true))
                .start();
        isShowing = false;
    }

    private void showBottomLayout(View child) {
        child.animate()
                .translationY(0)
                .setDuration(200)
                .withEndAction(() -> updateNavigationBarColor(false))
                .start();
        isShowing = true;
    }

    private void updateNavigationBarColor(boolean bottomHidden) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            Window window = activity.getWindow();

            // Update navigation bar color
            int colorRes = bottomHidden ? R.color.mode_background : R.color.mode;
            window.setNavigationBarColor(context.getColor(colorRes));

            // Update navigation bar icons color
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                int appearance = bottomHidden ?
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS :
                        0;
                insetsController.setSystemBarsAppearance(
                        appearance,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        }
    }
}