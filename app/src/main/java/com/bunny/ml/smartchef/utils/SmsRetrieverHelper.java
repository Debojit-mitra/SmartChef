package com.bunny.ml.smartchef.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsRetrieverHelper {
    private static final String SMS_RETRIEVED_ACTION = SmsRetriever.SMS_RETRIEVED_ACTION;
    private final Context context;
    private SmsBroadcastReceiver smsReceiver;
    private SmsRetrieverCallback callback;

    public interface SmsRetrieverCallback {
        void onSuccess(String otp);

        void onFailure();
    }

    public SmsRetrieverHelper(Context context) {
        this.context = context;
    }

    public void startSmsRetriever(SmsRetrieverCallback callback) {
        this.callback = callback;

        // Unregister any existing receiver first
        unregisterReceiver();

        // Start new SMS retriever client
        SmsRetrieverClient client = SmsRetriever.getClient(context);
        Task<Void> task = client.startSmsRetriever();

        task.addOnSuccessListener(aVoid -> {
            Log.d("SMS_RETRIEVER", "SMS retriever started successfully");
            registerReceiver();
        });

        task.addOnFailureListener(e -> {
            Log.e("SMS_RETRIEVER", "Failed to start SMS retriever", e);
            if (callback != null) {
                callback.onFailure();
            }
        });
    }


    private void registerReceiver() {
        if (smsReceiver == null) {
            smsReceiver = new SmsBroadcastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter(SMS_RETRIEVED_ACTION);
        context.registerReceiver(smsReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    public void unregisterReceiver() {
        if (smsReceiver != null) {
            try {
                context.unregisterReceiver(smsReceiver);
                smsReceiver = null;
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
            }
        }
    }

    private String extractOTP(String message) {
        if (message == null) return null;

        Log.d("SMS_RETRIEVER", "Received message: " + message);

        // Pattern to match a 6-digit number
        Pattern pattern = Pattern.compile("\\b\\d{6}\\b");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String otp = matcher.group(0);
            Log.d("SMS_RETRIEVER", "Extracted OTP: " + otp);
            return otp;
        }
        Log.d("SMS_RETRIEVER", "No OTP found in message");
        return null;
    }

    private class SmsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                if (status != null) {
                    Log.d("SMS_RETRIEVER", "Status code: " + status.getStatusCode());
                    switch (status.getStatusCode()) {
                        case CommonStatusCodes.SUCCESS:
                            String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                            if (callback != null && message != null) {
                                String otp = extractOTP(message);
                                if (otp != null) {
                                    Log.d("SMS_RETRIEVER", "Successfully extracted OTP: " + otp);
                                    callback.onSuccess(otp);
                                } else {
                                    Log.d("SMS_RETRIEVER", "Failed to extract OTP from message");
                                    callback.onFailure();
                                }
                            }
                            break;
                        case CommonStatusCodes.TIMEOUT:
                            Log.d("SMS_RETRIEVER", "Timeout occurred");
                            if (callback != null) {
                                callback.onFailure();
                            }
                            break;
                    }
                }
            }
        }
    }
}