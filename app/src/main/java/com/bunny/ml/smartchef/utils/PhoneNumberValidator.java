package com.bunny.ml.smartchef.utils;

public class PhoneNumberValidator {
    private static final String INDIA_COUNTRY_CODE = "+91";
    private static final int PHONE_NUMBER_LENGTH = 10;
    private static final String VALID_PHONE_PATTERN = "^[6-9]\\d{9}$";

    public static ValidationResult validateIndianPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return new ValidationResult(true, null); // Empty is allowed, no error
        }

        // Check for invalid starting digit (0-5)
        if (phoneNumber.matches("^[0-5].*")) {
            return new ValidationResult(false,
                    "Phone number cannot start with " + phoneNumber.charAt(0));
        }

        // Check for valid pattern and length
        if (!phoneNumber.matches(VALID_PHONE_PATTERN)) {
            if (phoneNumber.length() != PHONE_NUMBER_LENGTH) {
                return new ValidationResult(false,
                        "Phone number must be " + PHONE_NUMBER_LENGTH + " digits long");
            }
            return new ValidationResult(false,
                    "Invalid phone number format");
        }

        return new ValidationResult(true, null);
    }

    public static String formatWithCountryCode(String phoneNumber) {
        return INDIA_COUNTRY_CODE + phoneNumber;
    }

    // Result class to hold validation state
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
