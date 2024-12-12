package com.bunny.ml.smartchef.utils;

import static com.bunny.ml.smartchef.utils.Extras.isDarkMode;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.core.content.ContextCompat;

import com.bunny.ml.smartchef.R;

import java.util.ArrayList;
import java.util.List;

public class CustomOTPView extends LinearLayout {
    private static final int DEFAULT_OTP_LENGTH = 6;
    private static int BOX_SIZE_DP = 48; // Default box size in dp
    private static final float BOX_MARGIN_DP = 8; // Default margin in dp
    private final List<PinEntryEditText> otpDigitViews;
    private OnOTPCompleteListener otpCompleteListener;
    private int otpLength;
    private boolean isError = false;

    public interface OnOTPCompleteListener {
        void onOTPComplete(String otp);
    }

    public CustomOTPView(Context context) {
        this(context, null);
    }

    public CustomOTPView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomOTPView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        adjustBoxSize(context);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomOTPView);
        otpLength = typedArray.getInt(R.styleable.CustomOTPView_otpLength, DEFAULT_OTP_LENGTH);
        typedArray.recycle();

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        otpDigitViews = new ArrayList<>();
        setupOTPFields(context);
    }

    private void setupOTPFields(Context context) {
        for (int i = 0; i < otpLength; i++) {
            PinEntryEditText editText = new PinEntryEditText(getContext());
            setupDigitView(context, editText, i);
            otpDigitViews.add(editText);
            addView(editText);

            // Add spacing between boxes except for the last one
            if (i < otpLength - 1) {
                Space space = new Space(getContext());
                space.setLayoutParams(new LayoutParams(dpToPx(BOX_MARGIN_DP, getResources().getDisplayMetrics()), 0));
                addView(space);
            }
        }
    }

    private static int dpToPx(float dpValue, DisplayMetrics metrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, metrics);
    }

    private void setupDigitView(Context context, final PinEntryEditText editText, final int index) {
        int boxSize = dpToPx(BOX_SIZE_DP, getResources().getDisplayMetrics());
        LayoutParams params = new LayoutParams(boxSize, boxSize);
        editText.setLayoutParams(params);

        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
        editText.setGravity(Gravity.CENTER);
        editText.setMaxLines(1);
        editText.setTextColor(ContextCompat.getColor(getContext(), R.color.mode_black));
        editText.setBackgroundResource(R.drawable.otp_box_background);
        editText.setTextCursorDrawable(R.drawable.custom_cursor);
        if (context instanceof Activity && isDarkMode((Activity) context)) {
            editText.setAlpha(0.7f);
        } else {
            editText.setAlpha(0.5f);
        }
        if (index == 0) {
            editText.requestFocus();
            editText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        }


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    isError = false;
                    updateViewState();

                    // Move to next digit
                    if (index < otpLength - 1) {
                        otpDigitViews.get(index + 1).requestFocus();
                        editText.setAlpha(1f);
                    }

                    // Check if OTP is complete
                    if (isOTPComplete()) {
                        if (otpCompleteListener != null) {
                            otpCompleteListener.onOTPComplete(getOTP());
                        }
                    }
                }
            }
        });

        // Handle backspace
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                    editText.getText().toString().isEmpty() &&
                    index > 0) {
                otpDigitViews.get(index - 1).requestFocus();
                otpDigitViews.get(index - 1).setText("");
                if (editText.getText().toString().isEmpty()) {
                    if (context instanceof Activity && isDarkMode((Activity) context)) {
                        editText.setAlpha(0.7f);
                    } else {
                        editText.setAlpha(0.5f);
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void setError(boolean error) {
        isError = error;
        updateViewState();
    }

    private void updateViewState() {
        int backgroundRes = isError ?
                R.drawable.otp_box_background_error :
                R.drawable.otp_box_background;

        for (PinEntryEditText digitView : otpDigitViews) {
            digitView.setBackgroundResource(backgroundRes);
        }
    }

    public boolean isOTPComplete() {
        for (PinEntryEditText digitView : otpDigitViews) {
            if (digitView.getText().toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public String getOTP() {
        StringBuilder otp = new StringBuilder();
        for (PinEntryEditText digitView : otpDigitViews) {
            otp.append(digitView.getText().toString());
        }
        return otp.toString();
    }

    public void clearOTP() {
        for (PinEntryEditText digitView : otpDigitViews) {
            digitView.setText("");
        }
        otpDigitViews.get(0).requestFocus();
        isError = false;
        updateViewState();
    }

    public void setOnOTPCompleteListener(OnOTPCompleteListener listener) {
        this.otpCompleteListener = listener;
    }

    public void setOTP(String otp) {
        if (otp == null || otp.length() != otpLength) {
            throw new IllegalArgumentException("OTP length must match the view length of " + otpLength);
        }

        for (int i = 0; i < otpLength; i++) {
            PinEntryEditText digitView = otpDigitViews.get(i);
            digitView.setText(String.valueOf(otp.charAt(i)));
            digitView.setAlpha(1f);  // Set full opacity for filled boxes
        }

        // Notify listener if OTP is complete
        if (otpCompleteListener != null) {
            otpCompleteListener.onOTPComplete(otp);
        }
    }

    private void adjustBoxSize(Context context) {
        // Get screen width in pixels
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidthPx = displayMetrics.widthPixels;

        // Convert screen width to dp
        float screenWidthDp = screenWidthPx / displayMetrics.density;

        // Adjust BOX_SIZE_DP based on screen width
        int numberOfBoxes = 6; // Example: Number of OTP boxes
        BOX_SIZE_DP = (int) (screenWidthDp / (numberOfBoxes + 2)); // Add padding factor
    }

}
