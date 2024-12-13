package com.bunny.ml.smartchef.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.bunny.ml.smartchef.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomAlertDialog {
    public interface OnDialogButtonClickListener {
        void onClick();
    }

    private final Context context;
    private Dialog dialog;
    private ImageView iconImageView;
    private TextView messageTextView, dialog_title;
    private Button positiveButton, negativeButton;

    public CustomAlertDialog(@NonNull Context context) {
        this.context = context;
        initDialog();
    }

    private void initDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        android.view.View dialogView = inflater.inflate(R.layout.dialog_custom_alert, null);

        iconImageView = dialogView.findViewById(R.id.dialog_icon);
        dialog_title = dialogView.findViewById(R.id.dialog_title);
        messageTextView = dialogView.findViewById(R.id.dialog_message);
        positiveButton = dialogView.findViewById(R.id.dialog_positive_button);
        negativeButton = dialogView.findViewById(R.id.dialog_negative_button);

        builder.setView(dialogView);
        dialog = builder.create();
        dialog.setCancelable(false);
    }

    public CustomAlertDialog setDialogTitle(@NonNull String title) {
        dialog_title.setVisibility(View.VISIBLE);
        dialog_title.setText(title);
        return this;
    }

    public CustomAlertDialog setTitleJustification(int justificationMode) {
        dialog_title.setJustificationMode(justificationMode);
        return this;
    }

    public CustomAlertDialog setTitleAlignment(int alignment) {
        dialog_title.setTextAlignment(alignment);
        return this;
    }

    public CustomAlertDialog setIcon(@NonNull Drawable icon) {
        iconImageView.setVisibility(View.VISIBLE);
        iconImageView.setImageDrawable(icon);
        return this;
    }

    public CustomAlertDialog setIconTint(@ColorInt int color) {
        ImageViewCompat.setImageTintList(iconImageView, android.content.res.ColorStateList.valueOf(color));
        return this;
    }

    public CustomAlertDialog setIconTintResource(@ColorRes int colorRes) {
        return setIconTint(ContextCompat.getColor(context, colorRes));
    }

    public CustomAlertDialog setMessage(@NonNull String message) {
        messageTextView.setVisibility(View.VISIBLE);
        messageTextView.setText(message);
        return this;
    }

    public CustomAlertDialog setMessageJustification(int justificationMode) {
        messageTextView.setJustificationMode(justificationMode);
        return this;
    }

    public CustomAlertDialog setMessageAlignment(int alignment) {
        messageTextView.setTextAlignment(alignment);
        return this;
    }

    public CustomAlertDialog setPositiveButton(@NonNull String text, @Nullable OnDialogButtonClickListener listener) {
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setText(text);
        positiveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick();
            }
            dialog.dismiss();
        });
        return this;
    }

    public CustomAlertDialog setNegativeButton(@NonNull String text, @Nullable OnDialogButtonClickListener listener) {
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setText(text);
        negativeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick();
            }
            dialog.dismiss();
        });
        return this;
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}