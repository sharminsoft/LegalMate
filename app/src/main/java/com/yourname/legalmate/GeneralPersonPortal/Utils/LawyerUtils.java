package com.yourname.legalmate.GeneralPersonPortal.Utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.yourname.legalmate.GeneralPersonPortal.Models.SuggestedLawyerModel;

import java.util.List;

public class LawyerUtils {

    private static final String TAG = "LawyerUtils";

    /**
     * Validates if a lawyer model has all required fields
     */
    public static boolean isValidLawyer(SuggestedLawyerModel lawyer) {
        if (lawyer == null) {
            Log.w(TAG, "Lawyer model is null");
            return false;
        }

        // Check required fields
        if (lawyer.getLawyerId() == null || lawyer.getLawyerId().trim().isEmpty()) {
            Log.w(TAG, "Lawyer ID is null or empty");
            return false;
        }

        if (lawyer.getFullName() == null || lawyer.getFullName().trim().isEmpty()) {
            Log.w(TAG, "Lawyer full name is null or empty");
            return false;
        }

        return true;
    }

    /**
     * Validates if a lawyer is qualified for suggestion
     */
    public static boolean isQualifiedForSuggestion(SuggestedLawyerModel lawyer) {
        if (!isValidLawyer(lawyer)) {
            return false;
        }

        // Must be verified and active
        if (!lawyer.isVerified() || !lawyer.isActive()) {
            Log.d(TAG, "Lawyer not qualified - not verified or active: " + lawyer.getFullName());
            return false;
        }

        // Must have rating greater than 0
        if (lawyer.getRating() <= 0.0) {
            Log.d(TAG, "Lawyer not qualified - no rating: " + lawyer.getFullName());
            return false;
        }

        // Must have complete profile
        if (!lawyer.isProfileComplete()) {
            Log.d(TAG, "Lawyer not qualified - incomplete profile: " + lawyer.getFullName());
            return false;
        }

        return true;
    }

    /**
     * Safely gets the first practice area or returns default
     */
    public static String getFirstPracticeArea(List<String> practiceAreas, String defaultValue) {
        if (practiceAreas == null || practiceAreas.isEmpty()) {
            return defaultValue;
        }

        String firstArea = practiceAreas.get(0);
        return (firstArea != null && !firstArea.trim().isEmpty()) ? firstArea : defaultValue;
    }

    /**
     * Formats designation with "& More" if multiple practice areas
     */
    public static String formatDesignation(List<String> practiceAreas) {
        if (practiceAreas == null || practiceAreas.isEmpty()) {
            return "Legal Practitioner";
        }

        String designation = practiceAreas.get(0);
        if (practiceAreas.size() > 1) {
            designation += " & More";
        }

        return designation;
    }

    /**
     * Safely formats rating to one decimal place
     */
    public static String formatRating(double rating) {
        if (rating <= 0) {
            return "0.0";
        }
        return String.format("%.1f", rating);
    }

    /**
     * Formats review count text with proper pluralization
     */
    public static String formatReviewText(int reviewCount) {
        return "(" + reviewCount + " review" + (reviewCount != 1 ? "s" : "") + ")";
    }

    /**
     * Shows a toast message safely
     */
    public static void showToast(Context context, String message) {
        if (context != null && message != null) {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast", e);
            }
        }
    }

    /**
     * Shows a long toast message safely
     */
    public static void showLongToast(Context context, String message) {
        if (context != null && message != null) {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing long toast", e);
            }
        }
    }

    /**
     * Logs lawyer details for debugging
     */
    public static void logLawyerDetails(SuggestedLawyerModel lawyer) {
        if (lawyer == null) {
            Log.d(TAG, "Lawyer is null");
            return;
        }

        Log.d(TAG, "Lawyer Details:");
        Log.d(TAG, "  ID: " + lawyer.getLawyerId());
        Log.d(TAG, "  Name: " + lawyer.getFullName());
        Log.d(TAG, "  Rating: " + lawyer.getRating());
        Log.d(TAG, "  Reviews: " + lawyer.getReviewCount());
        Log.d(TAG, "  Verified: " + lawyer.isVerified());
        Log.d(TAG, "  Active: " + lawyer.isActive());
        Log.d(TAG, "  Available: " + lawyer.isAvailable());
        Log.d(TAG, "  Profile Complete: " + lawyer.isProfileComplete());
        Log.d(TAG, "  Practice Areas: " + (lawyer.getPracticeAreas() != null ? lawyer.getPracticeAreas().size() : 0));
    }

    /**
     * Validates image URL
     */
    public static boolean isValidImageUrl(String imageUrl) {
        return imageUrl != null &&
                !imageUrl.trim().isEmpty() &&
                (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"));
    }

    /**
     * Gets safe string value (returns empty string if null)
     */
    public static String getSafeString(String value) {
        return value != null ? value : "";
    }

    /**
     * Gets safe string value with default
     */
    public static String getSafeString(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
}