package com.bunny.ml.smartchef.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public abstract class SwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final Paint paint;
    private final Context context;
    private final int swipeDirection;
    private final SwipeConfig leftConfig;
    private final SwipeConfig rightConfig;
    private final int horizontalMargin;
    private final float cornerRadius;

    public static class SwipeConfig {
        @ColorInt
        public final int backgroundColor;
        public final Drawable icon;
        public final Drawable alternateIcon;
        public final int iconSize;

        public SwipeConfig(Context context, @ColorInt int backgroundColor, @DrawableRes int iconRes, @DrawableRes int alternateIconRes, int iconSizeDp) {
            this.backgroundColor = backgroundColor;
            this.icon = iconRes != 0 ? ContextCompat.getDrawable(context, iconRes) : null;
            this.alternateIcon = alternateIconRes != 0 ? ContextCompat.getDrawable(context, alternateIconRes) : null;
            this.iconSize = (int) (context.getResources().getDisplayMetrics().density * iconSizeDp);
        }
    }

    public SwipeCallback(Context context, int swipeDirection, SwipeConfig leftConfig, SwipeConfig rightConfig) {
        super(0, swipeDirection);
        this.context = context;
        this.swipeDirection = swipeDirection;
        this.leftConfig = leftConfig;
        this.rightConfig = rightConfig;

        paint = new Paint();
        paint.setAntiAlias(true);

        horizontalMargin = (int) (context.getResources().getDisplayMetrics().density * 5);
        cornerRadius = context.getResources().getDisplayMetrics().density * 12;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        if (Math.abs(dX) == 0) return;

        int itemHeight = itemView.getHeight() - 5;
        int itemTop = itemView.getTop();
        int itemBottom = itemTop + itemHeight;

        SwipeConfig config = dX > 0 ? rightConfig : leftConfig;
        if (config == null) return;

        // Draw background
        RectF backgroundRect;
        if (dX > 0) { // Swiping right
            backgroundRect = new RectF(
                    itemView.getLeft() + horizontalMargin,
                    itemTop,
                    itemView.getLeft() + dX,
                    itemBottom
            );
        } else { // Swiping left
            backgroundRect = new RectF(
                    itemView.getRight() + dX,
                    itemTop,
                    itemView.getRight() - horizontalMargin,
                    itemBottom
            );
        }

        paint.setColor(config.backgroundColor);
        c.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, paint);

        // Draw icon
        if (config.icon != null) {
            // Get the current item's state
            boolean useAlternateIcon = shouldUseAlternateIcon(viewHolder);
            Drawable activeIcon = getActiveIcon(config, useAlternateIcon);

            int iconMargin = (itemHeight - config.iconSize) / 2;
            int iconTop = itemTop + iconMargin;
            int iconBottom = iconTop + config.iconSize;

            if (dX > 0) { // Swiping right
                int iconLeft = itemView.getLeft() + horizontalMargin + iconMargin;
                int iconRight = iconLeft + config.iconSize;
                activeIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            } else { // Swiping left
                int iconRight = itemView.getRight() - horizontalMargin - iconMargin;
                int iconLeft = iconRight - config.iconSize;
                activeIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            }
            activeIcon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    protected abstract boolean shouldUseAlternateIcon(RecyclerView.ViewHolder viewHolder);

    protected Drawable getActiveIcon(SwipeConfig config, boolean useAlternate) {
        return useAlternate && config.alternateIcon != null ? config.alternateIcon : config.icon;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.4f;
    }
}