package com.yourname.legalmate.utils;

import android.content.Context;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static final String TAG = "CloudinaryConfig";

    private static final String CLOUDINARY_CLOUD_NAME = "dsa5waobt";
    private static final String CLOUDINARY_API_KEY = "393131226158217";
    private static final String CLOUDINARY_API_SECRET = "bVmYOWCc5qxZf5aVsYVa8rY_k_Y";

    private static boolean isInitialized = false;
    private static boolean initializationAttempted = false;

    public static synchronized void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "Cloudinary already initialized successfully");
            return;
        }

        if (initializationAttempted) {
            Log.w(TAG, "Cloudinary initialization already attempted. Check logs for errors.");
            return;
        }

        initializationAttempted = true;

        try {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            Context appContext = context.getApplicationContext();

            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
            config.put("api_key", CLOUDINARY_API_KEY);
            config.put("api_secret", CLOUDINARY_API_SECRET);
            config.put("secure", "true");

            Log.d(TAG, "Initializing Cloudinary with cloud_name: " + CLOUDINARY_CLOUD_NAME);

            MediaManager.init(appContext, config);
            isInitialized = true;

            Log.i(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary: " + e.getMessage(), e);
            isInitialized = false;
        }
    }

    public static boolean isReady() {
        if (!isInitialized) {
            Log.w(TAG, "Cloudinary not initialized");
            return false;
        }

        try {
            MediaManager mediaManager = MediaManager.get();
            boolean ready = mediaManager != null;

            if (!ready) {
                Log.w(TAG, "Cloudinary MediaManager is null");
            }

            return ready;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Cloudinary readiness: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean canUpload() {
        boolean ready = isReady();
        if (!ready) {
            Log.w(TAG, "Upload blocked - Cloudinary not ready. " +
                    "Initialized: " + isInitialized +
                    ", Attempted: " + initializationAttempted);
        }
        return ready;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static boolean wasInitializationAttempted() {
        return initializationAttempted;
    }

    public static synchronized void forceReinitialize(Context context) {
        Log.w(TAG, "Forcing Cloudinary re-initialization");
        isInitialized = false;
        initializationAttempted = false;
        initialize(context);
    }

    public static String getLawyerCaseDocumentFolder(String userId) {
        return "legalmate/lawyers/" + userId + "/case_documents";
    }

    public static String getLawyerProfileFolder(String userId) {
        return "legalmate/lawyers/" + userId + "/profile";
    }

    public static String getLawyerDocumentFolder(String userId) {
        return "legalmate/lawyers/" + userId + "/documents";
    }

    public static String generateCaseDocumentPublicId(String userId, String caseId, String documentType) {
        long timestamp = System.currentTimeMillis();
        return "lawyer_" + userId + "_case_" + caseId + "_" + documentType + "_" + timestamp;
    }

    public static String generateProfileDocumentPublicId(String userId, String fileType) {
        long timestamp = System.currentTimeMillis();
        return "lawyer_" + userId + "_profile_" + fileType + "_" + timestamp;
    }

    public static String generatePublicId(String userId, String fileType) {
        return getLawyerDocumentFolder(userId) + "/" + fileType + "_" + System.currentTimeMillis();
    }

    public static String generateCustomPublicId(String folder, String fileName) {
        return folder + "/" + fileName + "_" + System.currentTimeMillis();
    }

    public static String getCloudName() {
        return CLOUDINARY_CLOUD_NAME;
    }

    public static String getApiKey() {
        return CLOUDINARY_API_KEY;
    }

    public static String getApiSecret() {
        return CLOUDINARY_API_SECRET;
    }

    public static Map<String, String> getConfigurationMap() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
        config.put("api_key", CLOUDINARY_API_KEY);
        config.put("api_secret", CLOUDINARY_API_SECRET);
        config.put("secure", "true");
        return config;
    }

    public static String generatePDFViewingURL(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }

        if (cloudinaryUrl.contains(".pdf") || cloudinaryUrl.contains("f_pdf")) {
            return cloudinaryUrl.replace("/upload/", "/upload/fl_attachment:inline/");
        }

        return cloudinaryUrl;
    }

    public static String generatePDFDownloadURL(String cloudinaryUrl, String fileName) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }

        String downloadUrl = cloudinaryUrl.replace("/upload/", "/upload/fl_attachment/");

        if (fileName != null && !fileName.isEmpty()) {
            downloadUrl = downloadUrl.replace("/upload/fl_attachment/",
                    "/upload/fl_attachment:download:" + fileName + "/");
        }

        return downloadUrl;
    }

    public static String generatePDFPreviewURL(String publicId, String cloudName) {
        if (publicId == null || publicId.isEmpty() || cloudName == null || cloudName.isEmpty()) {
            return null;
        }

        String cleanPublicId = publicId.replace(".pdf", "");

        return "https://res.cloudinary.com/" + cloudName +
                "/image/upload/f_jpg,pg_1,w_300,h_400,c_fit/" + cleanPublicId + ".jpg";
    }

    public static boolean isPDFUrl(String url) {
        return url != null && (url.toLowerCase().contains(".pdf") ||
                url.toLowerCase().contains("f_pdf") ||
                url.toLowerCase().contains("/raw/"));
    }

    public static String fixPDFUrl(String url, String publicId) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        if (publicId != null && publicId.toLowerCase().endsWith(".pdf")) {
            if (!url.toLowerCase().contains(".pdf")) {
                url = url.replace("/upload/", "/upload/f_pdf/");
            }
        }

        return url;
    }

    public static MediaManager getMediaManager() {
        try {
            return isReady() ? MediaManager.get() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting MediaManager: " + e.getMessage());
            return null;
        }
    }

    public static String getDiagnosticInfo() {
        return String.format("CloudinaryConfig Status - " +
                        "Initialized: %s, " +
                        "Attempt Made: %s, " +
                        "MediaManager Available: %s, " +
                        "Cloud Name: %s",
                isInitialized,
                initializationAttempted,
                (getMediaManager() != null),
                CLOUDINARY_CLOUD_NAME
        );
    }

    public static synchronized void resetForTesting() {
        Log.w(TAG, "Resetting CloudinaryConfig state for testing");
        isInitialized = false;
        initializationAttempted = false;
    }
}