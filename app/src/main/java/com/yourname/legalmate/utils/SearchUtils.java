package com.yourname.legalmate.utils;

import android.text.TextUtils;
import java.util.List;

public class SearchUtils {

    public static boolean containsIgnoreCase(String text, String query) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(query)) {
            return false;
        }
        return text.toLowerCase().contains(query.toLowerCase());
    }

    public static boolean listContainsIgnoreCase(List<String> list, String query) {
        if (list == null || list.isEmpty() || TextUtils.isEmpty(query)) {
            return false;
        }
        for (String item : list) {
            if (containsIgnoreCase(item, query)) {
                return true;
            }
        }
        return false;
    }

    public static String highlightSearchTerm(String text, String query) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(query)) {
            return text;
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int index = lowerText.indexOf(lowerQuery);
        if (index != -1) {
            StringBuilder highlighted = new StringBuilder();
            highlighted.append(text.substring(0, index));
            highlighted.append("<b>");
            highlighted.append(text.substring(index, index + query.length()));
            highlighted.append("</b>");
            highlighted.append(text.substring(index + query.length()));
            return highlighted.toString();
        }

        return text;
    }

    public static boolean isValidBangladeshiMobile(String mobile) {
        if (TextUtils.isEmpty(mobile)) {
            return false;
        }

        // Remove any spaces or dashes
        mobile = mobile.replaceAll("[\\s-]", "");

        // Check for +880 format
        if (mobile.startsWith("+880")) {
            return mobile.length() == 14 && mobile.substring(4).matches("1[3-9]\\d{8}");
        }

        // Check for 01 format
        if (mobile.startsWith("01")) {
            return mobile.length() == 11 && mobile.matches("01[3-9]\\d{8}");
        }

        // Check for 880 format without +
        if (mobile.startsWith("880")) {
            return mobile.length() == 13 && mobile.substring(3).matches("1[3-9]\\d{8}");
        }

        return false;
    }

    public static String formatBangladeshiMobile(String mobile) {
        if (!isValidBangladeshiMobile(mobile)) {
            return mobile;
        }

        mobile = mobile.replaceAll("[\\s-]", "");

        if (mobile.startsWith("+880")) {
            return mobile.substring(0, 4) + " " + mobile.substring(4, 6) + " " +
                    mobile.substring(6, 10) + " " + mobile.substring(10);
        } else if (mobile.startsWith("01")) {
            return mobile.substring(0, 2) + " " + mobile.substring(2, 6) + " " +
                    mobile.substring(6, 9) + " " + mobile.substring(9);
        }

        return mobile;
    }
}