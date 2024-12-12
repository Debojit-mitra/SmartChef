package com.bunny.ml.smartchef.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.bunny.ml.smartchef.R;

import java.util.Objects;

public class LoadingDialog {
    private final Dialog dialog;
    private boolean isShowing = false;
    private final TextView messageText;

    public LoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.progress_dialog);

        // Make dialog background transparent
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Set background dim amount
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(0.5f);

        dialog.setCancelable(false);

        // Initialize message TextView
        messageText = dialog.findViewById(R.id.loadingMessage);

        // Add dismiss listener to track dialog state
        dialog.setOnDismissListener(dialogInterface -> isShowing = false);
    }

    public void show() {
        if (!isShowing && dialog != null) {
            try {
                if (messageText != null) {
                    messageText.setVisibility(View.GONE);
                }
                dialog.show();
                isShowing = true;
            } catch (Exception e) {
                Log.e("LoadingDialog", Objects.requireNonNull(e.getMessage()));
            }
        }
    }

    public void show(String message) {
        if (!isShowing && dialog != null) {
            try {
                if (messageText != null) {
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message);
                }
                dialog.show();
                isShowing = true;
            } catch (Exception e) {
                Log.e("LoadingDialog", Objects.requireNonNull(e.getMessage()));
            }
        }
    }

    public void updateMessage(String message) {
        if (messageText != null && isShowing) {
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(message);
        }
    }

    public void dismiss() {
        try {
            if (isShowing && dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                isShowing = false;
            }
        } catch (Exception e) {
            Log.e("LoadingDialog", Objects.requireNonNull(e.getMessage()));
        }
    }

    public boolean isShowing() {
        return isShowing;
    }
}