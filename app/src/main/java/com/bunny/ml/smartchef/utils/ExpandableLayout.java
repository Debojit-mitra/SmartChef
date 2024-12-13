package com.bunny.ml.smartchef.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bunny.ml.smartchef.R;

public class ExpandableLayout extends LinearLayout {

    private static final String TAG = "ExpandableLayout";
    private boolean expanded = false;
    private View headerView;
    private ImageView headerIcon;
    private TextView headerText;
    private ImageView expandIcon;

    // Store attributes
    private Drawable headerIconDrawable;
    private String headerTextString;
    private int headerIconTintColor = Color.TRANSPARENT;
    private int expandIconTintColor = Color.TRANSPARENT;

    public ExpandableLayout(Context context) {
        super(context);
        init(context, null);
    }

    public ExpandableLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ExpandableLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout);
            try {
                expanded = a.getBoolean(R.styleable.ExpandableLayout_isExpanded, expanded);

                // Get header icon
                int headerIconResId = a.getResourceId(R.styleable.ExpandableLayout_headerIcon, -1);
                if (headerIconResId != -1) {
                    headerIconDrawable = ContextCompat.getDrawable(context, headerIconResId);
                }

                // Get header text
                headerTextString = a.getString(R.styleable.ExpandableLayout_headerText);

                // Get tint colors
                if (a.hasValue(R.styleable.ExpandableLayout_headerIconTint)) {
                    headerIconTintColor = a.getColor(R.styleable.ExpandableLayout_headerIconTint, Color.TRANSPARENT);
                }

                if (a.hasValue(R.styleable.ExpandableLayout_expandIconTint)) {
                    expandIconTintColor = a.getColor(R.styleable.ExpandableLayout_expandIconTint, Color.TRANSPARENT);
                }
            } finally {
                a.recycle();
            }
        }
        setOrientation(VERTICAL);

        // Set click listener on the entire layout
        setOnClickListener(v -> toggleExpansion());

        Log.d(TAG, "Initialized with expanded = " + expanded);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupHeader();
        updateChildrenVisibility(false);
    }

    private void setupHeader() {
        if (getChildCount() > 0) {
            headerView = getChildAt(0);

            // Remove click listener from header view since we're using the parent's click
            headerView.setClickable(false);
            headerView.setFocusable(false);

            // Find header views
            headerIcon = headerView.findViewById(R.id.header_icon);
            headerText = headerView.findViewById(R.id.expandable_name);
            expandIcon = headerView.findViewById(R.id.expand_more_icon);

            // Apply attributes
            if (headerIcon != null) {
                if (headerIconDrawable != null) {
                    headerIcon.setImageDrawable(headerIconDrawable);
                }
                if (headerIconTintColor != Color.TRANSPARENT) {
                    headerIcon.setColorFilter(headerIconTintColor);
                }
            }

            if (headerText != null && headerTextString != null) {
                headerText.setText(headerTextString);
            }

            if (expandIcon != null && expandIconTintColor != Color.TRANSPARENT) {
                expandIcon.setColorFilter(expandIconTintColor);
            }

            // Update expand icon based on initial state
            updateExpandIcon();
        }
        Log.d(TAG, "Header setup complete");
    }

    private void updateExpandIcon() {
        if (expandIcon != null) {
            expandIcon.setRotation(expanded ? 180 : 0);
        }
    }

    private void toggleExpansion() {
        expanded = !expanded;
        Log.d(TAG, "Toggling expansion. New state: " + expanded);

        // Animate the expand icon
        if (expandIcon != null) {
            expandIcon.animate()
                    .rotation(expanded ? 180 : 0)
                    .setDuration(300)
                    .start();
        }

        updateChildrenVisibility(true);
    }

    private void updateChildrenVisibility(boolean animate) {
        for (int i = 1; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (expanded) {
                child.setVisibility(VISIBLE);
                if (animate) {
                    child.setAlpha(0f);
                    child.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                } else {
                    child.setAlpha(1f);
                }
            } else {
                if (animate) {
                    child.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                child.setVisibility(GONE);
                                requestLayout();
                            })
                            .start();
                } else {
                    child.setVisibility(GONE);
                }
            }
        }
        requestLayout();
    }

    // Rest of the methods remain the same...
    public void setHeaderIcon(@DrawableRes int iconResId) {
        headerIconDrawable = ContextCompat.getDrawable(getContext(), iconResId);
        if (headerIcon != null) {
            headerIcon.setImageDrawable(headerIconDrawable);
        }
    }

    public void setHeaderText(CharSequence text) {
        headerTextString = text.toString();
        if (headerText != null) {
            headerText.setText(headerTextString);
        }
    }

    public void setHeaderIconTint(int color) {
        headerIconTintColor = color;
        if (headerIcon != null) {
            headerIcon.setColorFilter(color);
        }
    }

    public void setExpandIconTint(int color) {
        expandIconTintColor = color;
        if (expandIcon != null) {
            expandIcon.setColorFilter(color);
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            this.expanded = expanded;
            updateExpandIcon();
            updateChildrenVisibility(false);
        }
    }
}