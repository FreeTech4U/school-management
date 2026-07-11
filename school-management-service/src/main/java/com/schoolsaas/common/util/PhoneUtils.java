package com.schoolsaas.common.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class PhoneUtils {

    public static String normalizeGuineanPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        // Remove all non-numeric characters except +
        String cleaned = phone.replaceAll("[^0-9+]", "");

        if (cleaned.startsWith("00224")) {
            cleaned = "+" + cleaned.substring(2);
        } else if (cleaned.startsWith("224")) {
            cleaned = "+" + cleaned;
        } else if (cleaned.startsWith("0") && cleaned.length() == 10) {
            // Local format 0XXXXXXXXX -> +224XXXXXXXXX
            cleaned = "+224" + cleaned.substring(1);
        } else if (cleaned.length() == 9 && !cleaned.startsWith("+")) {
            // Local format without leading zero 6XXXXXXXX -> +2246XXXXXXXX
            cleaned = "+224" + cleaned;
        }

        return cleaned;
    }

    public static boolean isValidGuineanPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        String normalized = normalizeGuineanPhoneNumber(phone);
        return normalized != null && normalized.matches("^\\+224\\d{9}$");
    }
}
