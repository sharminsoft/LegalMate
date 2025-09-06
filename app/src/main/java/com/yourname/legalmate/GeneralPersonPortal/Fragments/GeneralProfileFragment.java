package com.yourname.legalmate.GeneralPersonPortal.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.GeneralPersonPortal.Activities.GeneralProfileInfoActivity;
import com.yourname.legalmate.OuterActivities.LoginActivity;
import com.yourname.legalmate.R;
import com.yourname.legalmate.utils.CloudinaryConfig;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GeneralProfileFragment extends Fragment {

    private static final String TAG = "GeneralProfileFragment";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI Components
    private TextView fullNameTextView, emailTextView;
    private LinearLayout personalInfoLayout, accountSettingsLayout, paymentHistoryLayout,
            notificationSettingsLayout, supportHelpLayout, privacyPolicyLayout, logoutLayout;
    private CircleImageView profileImageView;
    private ImageView editProfileImageIcon;
    private ProgressBar uploadProgressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    public GeneralProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_profile, container, false);

        initializeFirebase();
        initializeViews(view);
        setupActivityResultLaunchers();
        loadUserProfile();
        setupClickListeners();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    private void initializeViews(@NonNull View view) {
        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        profileImageView = view.findViewById(R.id.profileImageView);
        editProfileImageIcon = view.findViewById(R.id.editProfileImageIcon);
        personalInfoLayout = view.findViewById(R.id.personalInfoLayout);
        accountSettingsLayout = view.findViewById(R.id.accountSettingsLayout);
        paymentHistoryLayout = view.findViewById(R.id.paymentHistoryLayout);
        notificationSettingsLayout = view.findViewById(R.id.notificationSettingsLayout);
        supportHelpLayout = view.findViewById(R.id.supportHelpLayout);
        privacyPolicyLayout = view.findViewById(R.id.privacyPolicyLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);

        // Add progress bar (you might need to add this to your layout)
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar);
        if (uploadProgressBar != null) {
            uploadProgressBar.setVisibility(View.GONE);
        }
    }

    private void setupActivityResultLaunchers() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadImageToCloudinary(selectedImageUri);
                        }
                    }
                }
        );

        // Permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        showToast("Permission denied. Cannot access gallery.");
                    }
                }
        );
    }

    private void loadUserProfile() {
        if (currentUser == null) return;

        setUserEmail();
        loadUserDataFromFirestore();
    }

    private void setUserEmail() {
        String userEmail = currentUser.getEmail();
        if (userEmail != null) {
            emailTextView.setText(userEmail);
        }
    }

    private void loadUserDataFromFirestore() {
        String userId = currentUser.getUid();
        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .get()
                .addOnSuccessListener(this::handleUserDataSuccess)
                .addOnFailureListener(this::handleUserDataFailure);
    }

    private void handleUserDataSuccess(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot.exists()) {
            String name = documentSnapshot.getString("name");
            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

            setUserName(name);
            loadProfileImage(profileImageUrl);
            Log.d(TAG, "User profile loaded successfully");
        } else {
            Log.d(TAG, "User document doesn't exist in Firestore");
            setUserName(null);
        }
    }

    private void handleUserDataFailure(Exception e) {
        Log.e(TAG, "Error loading user profile", e);
        setUserName(null);
    }

    private void setUserName(String name) {
        if (name != null && !name.isEmpty()) {
            fullNameTextView.setText(name);
        } else {
            String displayName = currentUser.getDisplayName();
            fullNameTextView.setText(displayName != null && !displayName.isEmpty() ? displayName : "User Name");
        }
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty() && getContext() != null) {
            Glide.with(getContext())
                    .load(imageUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person_ai) // Add a placeholder image
                    .error(R.drawable.ic_person_ai)
                    .into(profileImageView);
        }
    }

    private void setupClickListeners() {
        personalInfoLayout.setOnClickListener(v -> navigateToProfileInfo("Personal Information"));
        accountSettingsLayout.setOnClickListener(v -> navigateToProfileInfo("Account Settings"));
        paymentHistoryLayout.setOnClickListener(v -> navigateToProfileInfo("Payment History"));
        notificationSettingsLayout.setOnClickListener(v -> navigateToProfileInfo("Notification Settings"));
        supportHelpLayout.setOnClickListener(v -> navigateToProfileInfo("Support and Help Center"));
        privacyPolicyLayout.setOnClickListener(v -> openPrivacyPolicy());
        logoutLayout.setOnClickListener(v -> showLogoutConfirmationDialog());

        // Profile image edit click listener
        editProfileImageIcon.setOnClickListener(v -> checkPermissionAndOpenGallery());
    }

    private void checkPermissionAndOpenGallery() {
        String permission = getRequiredPermission();

        // For older Android versions, no permission needed
        if (permission == null) {
            openImagePicker();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            if (shouldShowRequestPermissionRationale(permission)) {
                showPermissionRationaleDialog(permission);
            } else {
                permissionLauncher.launch(permission);
            }
        }
    }


    // Replace your existing getRequiredPermission() method with this:
    private String getRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33+)
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6+ (API 23+)
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        return null; // No permission needed for older versions
    }

    private void showPermissionRationaleDialog(String permission) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission Required")
                .setMessage("This app needs access to your gallery to select profile pictures.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    permissionLauncher.launch(permission);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Create a chooser intent
            Intent chooserIntent = Intent.createChooser(intent, "Select Profile Picture");

            // Add additional intents for camera and gallery
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");

            Intent[] additionalIntents = {galleryIntent};
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, additionalIntents);

            if (chooserIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                imagePickerLauncher.launch(chooserIntent);
            } else {
                // Fallback method
                openImagePickerFallback();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
            openImagePickerFallback();
        }
    }


    private void openImagePickerFallback() {
        try {
            // Try different intent types
            Intent[] intents = {
                    new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                    new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
            };

            for (Intent intent : intents) {
                if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    imagePickerLauncher.launch(intent);
                    return;
                }
            }

            // If none work, show error
            showToast("No gallery app available. Please install a gallery app.");

        } catch (Exception e) {
            Log.e(TAG, "All image picker methods failed", e);
            showToast("Unable to access gallery. Please check your device settings.");
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (currentUser == null) {
            showToast("User not authenticated");
            return;
        }

        if (!CloudinaryConfig.canUpload()) {
            showToast("Cloudinary not ready. Please try again later.");
            Log.e(TAG, "Cloudinary upload failed: " + CloudinaryConfig.getDiagnosticInfo());
            return;
        }

        showUploadProgress(true);

        String userId = currentUser.getUid();
        String folder = "ClientPortal/profileImage";
        String publicId = generateProfileImagePublicId(userId);

        Map<String, Object> options = new HashMap<>();
        options.put("public_id", publicId);
        options.put("folder", folder);
        options.put("resource_type", "image");
        options.put("transformation", "w_300,h_300,c_fill,g_face");

        try {
            MediaManager.get()
                    .upload(imageUri)
                    .options(options)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload started: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((bytes * 100) / totalBytes);
                            Log.d(TAG, "Upload progress: " + progress + "%");
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            Log.d(TAG, "Upload successful: " + requestId);

                            String imageUrl = (String) resultData.get("secure_url");
                            if (imageUrl != null) {
                                saveImageUrlToFirestore(imageUrl);
                            } else {
                                showUploadProgress(false);
                                showToast("Upload failed: Invalid response");
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Upload failed: " + error.getDescription());
                            showUploadProgress(false);
                            showToast("Upload failed: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                        }
                    })
                    .dispatch();

        } catch (Exception e) {
            Log.e(TAG, "Error starting upload", e);
            showUploadProgress(false);
            showToast("Error starting upload: " + e.getMessage());
        }
    }

    private String generateProfileImagePublicId(String userId) {
        long timestamp = System.currentTimeMillis();
        return "general_user_" + userId + "_profile_" + timestamp;
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        if (currentUser == null) {
            showUploadProgress(false);
            return;
        }

        String userId = currentUser.getUid();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("profileImageUrl", imageUrl);

        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile image URL saved successfully");
                    showUploadProgress(false);
                    loadProfileImage(imageUrl);
                    showToast("Profile picture updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving image URL to Firestore", e);
                    showUploadProgress(false);
                    showToast("Failed to update profile picture");
                });
    }

    private void showUploadProgress(boolean show) {
        if (uploadProgressBar != null) {
            uploadProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (editProfileImageIcon != null) {
            editProfileImageIcon.setEnabled(!show);
            editProfileImageIcon.setAlpha(show ? 0.5f : 1.0f);
        }
    }

    private void navigateToProfileInfo(String sectionName) {
        Intent intent = new Intent(getActivity(), GeneralProfileInfoActivity.class);
        intent.putExtra("section", sectionName);
        intent.putExtra("userId", currentUser != null ? currentUser.getUid() : "");
        intent.putExtra("email", currentUser != null ? currentUser.getEmail() : "");
        startActivity(intent);
    }

    private void openPrivacyPolicy() {
        try {
            String privacyPolicyUrl = "https://yourwebsite.com/privacy-policy";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showToast("No browser app found to open the link");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening privacy policy", e);
            showToast("Unable to open privacy policy");
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void performLogout() {
        try {
            showToast("Logging out...");
            mAuth.signOut();
            navigateToLogin();
            Log.d(TAG, "User logged out successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            showToast("Error occurred while logging out");
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadUserProfile();
        }
    }

    public void refreshProfile() {
        if (currentUser != null) {
            loadUserProfile();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isUserAuthenticated()) {
            navigateToLogin();
        }
    }

    private boolean isUserAuthenticated() {
        return mAuth.getCurrentUser() != null;
    }
}