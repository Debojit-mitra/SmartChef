package com.bunny.ml.smartchef.utils;

import static com.bunny.ml.smartchef.utils.Extras.isDarkMode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.Objects;

public class PinEntryEditText extends AppCompatEditText {

    public PinEntryEditText(Context context) {
        super(context);
        init();
    }

    private void init() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        setTypeface(Typeface.DEFAULT_BOLD);
        setLongClickable(false);
        setTextIsSelectable(false);
        // Prevent copy/paste menu
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        // Disable context menu
        setOnLongClickListener(v -> true);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            setAlpha(1f);
        } else {
            String text = Objects.requireNonNull(getText()).toString();
            if (text.isEmpty()) {
                if (getContext() instanceof Activity && isDarkMode((Activity) getContext())) {
                    setAlpha(0.7f);
                } else {
                    setAlpha(0.5f);
                }
            }
        }
    }
}
