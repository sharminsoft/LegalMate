package com.yourname.legalmate;

import android.app.Application;
import android.util.Log;
import com.yourname.legalmate.utils.CloudinaryConfig;

public class LegalMateApplication extends Application {

    private static final String TAG = "LegalMateApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Application starting...");

        // Initialize Cloudinary
        initializeCloudinary();

        Log.d(TAG, "Application initialization completed");
    }

    private void initializeCloudinary() {
        try {
            CloudinaryConfig.initialize(this);

            // Log initialization status
            if (CloudinaryConfig.isReady()) {
                Log.i(TAG, "Cloudinary initialization successful");
            } else {
                Log.w(TAG, "Cloudinary initialization completed but not ready");

            }
        } catch (Exception e) {
            Log.e(TAG, "Error during Cloudinary initialization", e);
        }
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "Application terminating...");
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "Application low memory warning");
        super.onLowMemory();
    }
}