package com.yourname.legalmate.OuterActivities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.GeneralPersonPortal.Activities.GeneralPersonDashboardActivity;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerDashboardActivity;
import com.yourname.legalmate.R;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    // Duration of wait time in milliseconds
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Using a Handler to delay the authentication check
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserAuthenticationAndNavigate();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void checkUserAuthenticationAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in, check their role
            Log.d(TAG, "User is logged in: " + currentUser.getEmail());
            checkUserRoleAndNavigate(currentUser.getUid());
        } else {
            // User is not logged in, navigate to GreetingActivity
            Log.d(TAG, "User is not logged in, navigating to GreetingActivity");
            navigateToGreeting();
        }
    }

    private void checkUserRoleAndNavigate(String userId) {
        Log.d(TAG, "Checking user role for userId: " + userId);

        // First check in Lawyers collection
        firestore.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .get()
                .addOnCompleteListener(lawyerTask -> {
                    if (lawyerTask.isSuccessful()) {
                        DocumentSnapshot lawyerDoc = lawyerTask.getResult();
                        if (lawyerDoc.exists()) {
                            String role = lawyerDoc.getString("role");
                            if ("lawyer".equals(role)) {
                                Log.d(TAG, "User is a lawyer, navigating to LawyerDashboardActivity");
                                navigateToLawyerDashboard();
                                return;
                            }
                        }
                    } else {
                        Log.e(TAG, "Error checking lawyer collection", lawyerTask.getException());
                    }

                    // If not found in lawyers or error occurred, check in general users collection
                    firestore.collection("Users")
                            .document("GeneralPersons")
                            .collection("GeneralPersons")
                            .document(userId)
                            .get()
                            .addOnCompleteListener(generalTask -> {
                                if (generalTask.isSuccessful()) {
                                    DocumentSnapshot generalDoc = generalTask.getResult();
                                    if (generalDoc.exists()) {
                                        String role = generalDoc.getString("role");
                                        Log.d(TAG, "User is a general person with role: " + role + ", navigating to GeneralPersonDashboardActivity");
                                        navigateToGeneralPersonDashboard();
                                    } else {
                                        // User not found in any collection, sign them out and go to greeting
                                        Log.w(TAG, "User not found in any collection, signing out");
                                        mAuth.signOut();
                                        navigateToGreeting();
                                    }
                                } else {
                                    Log.e(TAG, "Error checking general user collection", generalTask.getException());
                                    // In case of error, still navigate to greeting for safety
                                    navigateToGreeting();
                                }
                            });
                });
    }

    private void navigateToGreeting() {
        Intent intent = new Intent(SplashActivity.this, GreetingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLawyerDashboard() {
        Intent intent = new Intent(SplashActivity.this, LawyerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToGeneralPersonDashboard() {
        Intent intent = new Intent(SplashActivity.this, GeneralPersonDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}