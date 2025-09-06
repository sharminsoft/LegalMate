package com.yourname.legalmate.LawyerPortal.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerLocationInfoActivity;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerProfileInfoActivity;
import com.yourname.legalmate.OuterActivities.LoginActivity;
import com.yourname.legalmate.R;

import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LawyerProfileFragment extends Fragment {

    private static final String TAG = "LawyerProfileFragment";
    private static final int TIMEOUT_DURATION = 30000; // 30 seconds

    // UI elements
    private CircleImageView profileImageView;
    private ImageView editProfileImageIcon;
    private TextView fullNameTextView, designationTextView;
    private LinearLayout profileSettingsLayout, contactLocationLayout, locationInformationLayout, professionalInfoLayout,
            consultationFeesLayout, appointmentSettingsLayout, securityAccountLayout,
            socialWebsiteLayout, logoutLayout;
    private ProgressBar loadingProgressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // Progress dialog
    private AlertDialog currentProgressDialog;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    // Profile data
    private String currentProfileImageUrl = "";
    private String currentFullName = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeFirebase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lawyer_profile, container, false);

        initializeViews(view);
        setupClickListeners();
        loadProfileData();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            // User not authenticated, redirect to login
            redirectToLogin();
        }
    }

    private void initializeViews(View view) {
        // Profile section
        profileImageView = view.findViewById(R.id.profileImageView);
        editProfileImageIcon = view.findViewById(R.id.editProfileImageIcon);
        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        designationTextView = view.findViewById(R.id.designationTextView);

        // Settings sections
        profileSettingsLayout = view.findViewById(R.id.profileSettingsLayout);
        contactLocationLayout = view.findViewById(R.id.contactLocationLayout);
        locationInformationLayout = view.findViewById(R.id.locationInformationLayout);
        professionalInfoLayout = view.findViewById(R.id.professionalInfoLayout);
        consultationFeesLayout = view.findViewById(R.id.consultationFeesLayout);
        appointmentSettingsLayout = view.findViewById(R.id.appointmentSettingsLayout);
        securityAccountLayout = view.findViewById(R.id.securityAccountLayout);
        socialWebsiteLayout = view.findViewById(R.id.socialWebsiteLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);

        // Optional: Loading indicator
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }

        // Set default values
        if (fullNameTextView != null) {
            fullNameTextView.setText("Loading...");
        }
        if (designationTextView != null) {
            designationTextView.setText("Lawyer");
        }
    }

    private void setupClickListeners() {
        // Profile settings sections
        if (profileSettingsLayout != null) {
            profileSettingsLayout.setOnClickListener(v -> openProfileInfoActivity("Profile Settings"));
        }
        if (contactLocationLayout != null) {
            contactLocationLayout.setOnClickListener(v -> openProfileInfoActivity("Contact & Location Settings"));
        }
        if (professionalInfoLayout != null) {
            professionalInfoLayout.setOnClickListener(v -> openProfileInfoActivity("Professional Information"));
        }
        if (consultationFeesLayout != null) {
            consultationFeesLayout.setOnClickListener(v -> openProfileInfoActivity("Consultation & Fees"));
        }
        if (appointmentSettingsLayout != null) {
            appointmentSettingsLayout.setOnClickListener(v -> openProfileInfoActivity("Appointment Settings"));
        }
        if (securityAccountLayout != null) {
            securityAccountLayout.setOnClickListener(v -> openProfileInfoActivity("Security & Account Settings"));
        }
        if (socialWebsiteLayout != null) {
            socialWebsiteLayout.setOnClickListener(v -> openProfileInfoActivity("Social & Website Links"));
        }

        // Logout functionality
        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        // Profile image edit
        if (editProfileImageIcon != null) {
            editProfileImageIcon.setOnClickListener(v -> openProfileInfoActivity("Profile Settings"));
        }
        if (profileImageView != null) {
            profileImageView.setOnClickListener(v -> openProfileInfoActivity("Profile Settings"));
        }

        locationInformationLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LawyerLocationInfoActivity.class);
            startActivity(intent);
        });
    }

    private void loadProfileData() {
        if (TextUtils.isEmpty(userId)) {
            showToast("User not authenticated");
            return;
        }

        showLoadingIndicator(true);
        startTimeout("Profile data loading timeout");

        // Load profile image from Documents collection first
        loadProfileImageFromDocuments();

        // Load basic profile data for name
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("BasicProfileSettings")
                .get()
                .addOnCompleteListener(task -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            cancelTimeout();
                            showLoadingIndicator(false);

                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null && document.exists()) {
                                    updateUIWithProfileData(document.getData());
                                } else {
                                    Log.d(TAG, "No profile data found");
                                    setDefaultProfileData();
                                }
                            } else {
                                Log.e(TAG, "Error loading profile data", task.getException());
                                showToast("Failed to load profile data");
                                setDefaultProfileData();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            cancelTimeout();
                            showLoadingIndicator(false);
                            Log.e(TAG, "Failed to load profile data", e);
                            showToast("Network error. Please try again.");
                            setDefaultProfileData();
                        });
                    }
                });
    }

    private void loadProfileImageFromDocuments() {
        if (TextUtils.isEmpty(userId)) return;

        // Load profile image from Documents collection
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("Documents")
                .get()
                .addOnCompleteListener(task -> {
                    if (getActivity() != null && task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                Object profileImageObj = data.get("profileImageUrl");
                                if (profileImageObj != null && !TextUtils.isEmpty(profileImageObj.toString())) {
                                    currentProfileImageUrl = profileImageObj.toString();
                                    loadProfileImage(currentProfileImageUrl);
                                    Log.d(TAG, "Profile image loaded from Documents: " + currentProfileImageUrl);
                                } else {
                                    // Fallback to default image
                                    Log.d(TAG, "No profile image found in Documents");
                                    if (profileImageView != null) {
                                        profileImageView.setImageResource(R.drawable.lowyer_placeholder);
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "Documents collection not found");
                            if (profileImageView != null) {
                                profileImageView.setImageResource(R.drawable.lowyer_placeholder);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error loading profile image from Documents", task.getException());
                        if (profileImageView != null) {
                            profileImageView.setImageResource(R.drawable.lowyer_placeholder);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load profile image from Documents", e);
                    if (getActivity() != null && profileImageView != null) {
                        profileImageView.setImageResource(R.drawable.lowyer_placeholder);
                    }
                });
    }

    private void updateUIWithProfileData(Map<String, Object> profileData) {
        if (profileData == null) {
            setDefaultProfileData();
            return;
        }

        // Update full name
        Object fullNameObj = profileData.get("fullName");
        if (fullNameObj != null && !TextUtils.isEmpty(fullNameObj.toString())) {
            currentFullName = fullNameObj.toString();
            if (fullNameTextView != null) {
                fullNameTextView.setText(currentFullName);
            }
        } else {
            currentFullName = "User Name";
            if (fullNameTextView != null) {
                fullNameTextView.setText(currentFullName);
            }
        }

        // Profile image is already loaded from Documents collection in loadProfileImageFromDocuments()
        // No need to load it again from BasicProfileSettings

        // Update designation based on professional info
        loadProfessionalInfo();
    }

    private void loadProfessionalInfo() {
        if (TextUtils.isEmpty(userId)) return;

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("ProfessionalInformation")
                .get()
                .addOnCompleteListener(task -> {
                    if (getActivity() != null && task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                // Update designation
                                Object experienceObj = data.get("experience");
                                String designation = "Lawyer";

                                if (experienceObj != null && !TextUtils.isEmpty(experienceObj.toString())) {
                                    try {
                                        int experience = Integer.parseInt(experienceObj.toString());
                                        if (experience >= 10) {
                                            designation = "Senior Lawyer";
                                        } else if (experience >= 5) {
                                            designation = "Experienced Lawyer";
                                        } else if (experience > 0) {
                                            designation = "Junior Lawyer";
                                        }
                                    } catch (NumberFormatException e) {
                                        designation = "Lawyer";
                                    }
                                }

                                if (designationTextView != null) {
                                    designationTextView.setText(designation);
                                }
                            }
                        }
                    }
                });
    }

    private void loadProfileImage(String imageUrl) {
        if (profileImageView != null && !TextUtils.isEmpty(imageUrl) && getActivity() != null) {
            Glide.with(getActivity())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.lowyer_placeholder)
                    .error(R.drawable.lowyer_placeholder)
                    .into(profileImageView);
        }
    }

    private void setDefaultProfileData() {
        currentFullName = "User Name";
        if (fullNameTextView != null) {
            fullNameTextView.setText(currentFullName);
        }
        if (designationTextView != null) {
            designationTextView.setText("Lawyer");
        }
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.lowyer_placeholder);
        }
    }

    private void showLogoutConfirmationDialog() {
        if (getActivity() == null) return;

        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        showProgressDialog("Logging out...");
        startTimeout("Logout timeout");

        try {
            // Sign out from Firebase
            mAuth.signOut();

            // Clear any local data if needed
            clearLocalData();

            // Navigate to login screen
            cancelTimeout();
            dismissProgressDialog();
            redirectToLogin();

        } catch (Exception e) {
            cancelTimeout();
            dismissProgressDialog();
            Log.e(TAG, "Error during logout", e);
            showToast("Logout failed. Please try again.");
        }
    }

    private void clearLocalData() {
        // Clear any cached data
        currentProfileImageUrl = "";
        currentFullName = "";

        // Clear other cached data as needed
        // You can add SharedPreferences clearing here if you use it
    }

    private void redirectToLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void openProfileInfoActivity(String sectionName) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LawyerProfileInfoActivity.class);
            intent.putExtra("section_name", sectionName);
            startActivity(intent);
        }
    }

    private void showLoadingIndicator(boolean show) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showProgressDialog(String message) {
        dismissProgressDialog();

        if (getActivity() == null) return;

        try {
            View progressView = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_1, null);
            TextView textView = progressView.findViewById(android.R.id.text1);
            textView.setText(message);
            textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
            textView.setCompoundDrawablePadding(16);

            ProgressBar progressBar = new ProgressBar(getActivity());
            progressBar.setIndeterminate(true);

            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(48, 32, 48, 32);
            layout.addView(progressBar);
            layout.addView(textView);

            currentProgressDialog = new MaterialAlertDialogBuilder(getActivity())
                    .setView(layout)
                    .setCancelable(false)
                    .create();

            currentProgressDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing progress dialog", e);
        }
    }

    private void dismissProgressDialog() {
        try {
            if (currentProgressDialog != null && currentProgressDialog.isShowing()) {
                currentProgressDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing progress dialog", e);
        } finally {
            currentProgressDialog = null;
        }
    }

    private void startTimeout(String timeoutMessage) {
        cancelTimeout();
        timeoutRunnable = () -> {
            Log.w(TAG, timeoutMessage);
            dismissProgressDialog();
            showLoadingIndicator(false);
            showToast("Operation timed out. Please try again.");
        };
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null && mainHandler != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload profile data when fragment resumes (in case data was updated)
        if (!TextUtils.isEmpty(userId)) {
            loadProfileData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissProgressDialog();
        cancelTimeout();

        // Clear handlers to prevent memory leaks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Additional cleanup if needed
    }
}