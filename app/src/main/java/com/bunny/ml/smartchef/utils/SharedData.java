package com.bunny.ml.smartchef.utils;

public class SharedData {

    public static final String BASE_URL = "https://smartchefapi.zerotwobunny.fun/api/v1/";

    public static String phoneNumber;

    public static String getPhoneNumber() {
        return phoneNumber;
    }

    public static void setPhoneNumber(String phoneNumber) {
        SharedData.phoneNumber = phoneNumber;
    }
}
