package com.yourname.legalmate.GeneralPersonPortal.AccountCreation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.GeneralPersonPortal.Activities.GeneralPersonDashboardActivity;
import com.yourname.legalmate.OuterActivities.LoginActivity;
import com.yourname.legalmate.R;

import java.util.HashMap;
import java.util.Map;

public class GenPerCreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "GenPerCreateAccount";
    private static final int RC_SIGN_IN = 9001;

    // UI Components
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private MaterialButton btnCreateAccount, btnGoogleSignIn;
    private TextView tvLoginLink;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressDialog progressDialog;

    // Variables
    private String role;

    // Modern way to handle activity results
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gen_per_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get role from intent
        role = getIntent().getStringExtra("role");

        initializeViews();
        initializeFirebase();
        setupGoogleSignIn();
        setupClickListeners();

        // Check Google Sign-in configuration (for debugging)
        checkGoogleSignInConfiguration();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading, please wait...");
        progressDialog.setCancelable(false);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupGoogleSignIn() {
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize the activity result launcher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Log.d(TAG, "Google Sign-in activity result received with code: " + result.getResultCode());

                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // Update progress message
                            updateProgressDialog("Processing your account...");

                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null && account.getIdToken() != null) {
                                    Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
                                    Log.d(TAG, "ID Token: " + (account.getIdToken() != null ? "Present" : "Null"));

                                    // Update progress message
                                    updateProgressDialog("Authenticating with Server...");

                                    firebaseAuthWithGoogle(account.getIdToken());
                                } else {
                                    Log.e(TAG, "Account or ID token is null");
                                    hideProgressDialog();
                                    Toast.makeText(GenPerCreateAccountActivity.this, "Failed to get account information", Toast.LENGTH_LONG).show();
                                }
                            } catch (ApiException e) {
                                Log.w(TAG, "Google sign in failed with code: " + e.getStatusCode(), e);
                                hideProgressDialog();
                                handleGoogleSignInError(e);
                            }
                        } else {
                            Log.d(TAG, "Google Sign-In cancelled or failed. Result code: " + result.getResultCode());
                            hideProgressDialog();
                            if (result.getResultCode() != RESULT_CANCELED) {
                                Toast.makeText(GenPerCreateAccountActivity.this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    createAccountWithEmail();
                }
            }
        });

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login activity
                Intent intent = new Intent(GenPerCreateAccountActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-in process");

        // Check if Google Play Services is available
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available. Result code: " + resultCode);
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 2404).show();
            } else {
                Toast.makeText(this, "Google Play Services not supported on this device", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Show progress dialog and keep it until complete success or failure
        showProgressDialog("Signing in with Google...");

        // Sign out from previous account to ensure account picker shows
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Previous account signed out");

            // Revoke access to ensure fresh sign-in
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, revokeTask -> {
                Log.d(TAG, "Access revoked, launching sign-in intent");

                // Update progress message
                updateProgressDialog("Please select your Google account...");

                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
    }

    private void handleGoogleSignInError(ApiException e) {
        String errorMessage;
        int statusCode = e.getStatusCode();

        Log.e(TAG, "Google Sign-in error with status code: " + statusCode);

        switch (statusCode) {
            case 12501: // SIGN_IN_CANCELLED
                Log.d(TAG, "Sign-in cancelled by user");
                return; // Don't show toast for cancellation
            case 12500: // SIGN_IN_CURRENTLY_IN_PROGRESS
                Log.d(TAG, "Sign-in already in progress");
                return; // Don't show error for this
            case 10: // DEVELOPER_ERROR
                errorMessage = "Google Sign-in configuration error. Please contact support.";
                Log.e(TAG, "Developer error - check SHA1 fingerprint and web client ID");
                break;
            case 7: // NETWORK_ERROR
                errorMessage = "Network error. Please check your internet connection.";
                break;
            case 8: // INTERNAL_ERROR
                errorMessage = "Internal error occurred. Please try again later.";
                break;
            case 4: // SIGN_IN_REQUIRED
                errorMessage = "Sign-in required. Please try again.";
                break;
            case 5: // INVALID_ACCOUNT
                errorMessage = "Invalid account selected. Please try with a different account.";
                break;
            case 17: // TIMEOUT
                errorMessage = "Sign-in timed out. Please try again.";
                break;
            case 16: // API_NOT_CONNECTED
                errorMessage = "Google services not available. Please try again.";
                break;
            case 6: // RESOLUTION_REQUIRED
                errorMessage = "User interaction required. Please try again.";
                break;
            default:
                errorMessage = "Sign-in failed (Error code: " + statusCode + "). Please try again.";
                Log.e(TAG, "Unknown Google Sign-in error: " + statusCode);
                break;
        }
        Toast.makeText(GenPerCreateAccountActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: Starting Firebase authentication");

        // Update progress message
        updateProgressDialog("Signing in to Server...");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            String userId = user.getUid();
                            String userName = user.getDisplayName() != null ? user.getDisplayName() : "Unknown User";
                            String userEmail = user.getEmail() != null ? user.getEmail() : "No Email Provided";

                            Log.d(TAG, "User authenticated: " + userEmail);

                            // Update progress message
                            updateProgressDialog("Setting up your account...");

                            // Check if general person already exists in Firestore
                            firestore.collection("Users")
                                    .document("GeneralPersons")
                                    .collection("GeneralPersons")
                                    .document(userId)
                                    .get()
                                    .addOnCompleteListener(getTask -> {
                                        if (getTask.isSuccessful()) {
                                            DocumentSnapshot document = getTask.getResult();
                                            if (!document.exists()) {
                                                Log.d(TAG, "Creating new general person document");

                                                // Update progress message
                                                updateProgressDialog("Creating your profile...");

                                                // General person doesn't exist, create new document
                                                saveGeneralPersonToFirestore(user, userName, userEmail, "google");
                                            } else {
                                                // General person already exists, just navigate to main activity
                                                Log.d(TAG, "General person already exists in Firestore");

                                                // Update progress message
                                                updateProgressDialog("Completing sign in...");

                                                // Add a small delay to show the completion message
                                                new android.os.Handler().postDelayed(() -> {
                                                    hideProgressDialog();
                                                    navigateToNextActivity();
                                                }, 500);
                                            }
                                        } else {
                                            Log.e(TAG, "Failed to check general person existence", getTask.getException());
                                            hideProgressDialog();
                                            navigateToNextActivity(); // Navigate anyway
                                        }
                                    });
                        }

                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        hideProgressDialog();
                        String errorMessage = "Authentication failed: ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        } else {
                            errorMessage += "Unknown error occurred";
                        }
                        Toast.makeText(GenPerCreateAccountActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createAccountWithEmail() {
        showProgressDialog("Creating your account...");

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Attempting email registration for: " + email);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update progress message
                            updateProgressDialog("Setting up your profile...");

                            // Save general person data to Firestore
                            saveGeneralPersonToFirestore(user, name, email, "email");
                        }
                    } else {
                        Log.e(TAG, "createUserWithEmail:failure", task.getException());
                        hideProgressDialog();
                        String errorMessage = "Registration failed. ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }
                        Toast.makeText(GenPerCreateAccountActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveGeneralPersonToFirestore(FirebaseUser user, String name, String email, String signInMethod) {
        String userId = user.getUid();

        // Create general person data map
        Map<String, Object> generalPersonData = new HashMap<>();
        generalPersonData.put("uid", userId);
        generalPersonData.put("name", name);
        generalPersonData.put("profileImageUrl", "");
        generalPersonData.put("email", email);
        generalPersonData.put("role", "general_person");
        generalPersonData.put("signInMethod", signInMethod);
        generalPersonData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        generalPersonData.put("profileComplete", false);

        // Save to Users/GeneralPersons subcollection
        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .set(generalPersonData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "General person data saved successfully");

                    // Update progress message
                    updateProgressDialog("Finalizing your account...");

                    // Add a small delay to show the completion message
                    new android.os.Handler().postDelayed(() -> {
                        hideProgressDialog();
                        Toast.makeText(GenPerCreateAccountActivity.this,
                                "Account created successfully!", Toast.LENGTH_SHORT).show();
                        navigateToNextActivity();
                    }, 500);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving general person data", e);
                    hideProgressDialog();
                    Toast.makeText(GenPerCreateAccountActivity.this,
                            "Error saving account data: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Delete the created user account since Firestore save failed
                    user.delete();
                });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Clear previous errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Name is required");
            isValid = false;
        } else if (name.length() < 2) {
            tilName.setError("Name must be at least 2 characters");
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    // Add this method to check Google Sign-in configuration (for debugging)
    private void checkGoogleSignInConfiguration() {
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            Log.d(TAG, "Last signed in account: " + (account != null ? account.getEmail() : "None"));

            // Check if client ID is properly configured
            String clientId = getString(R.string.default_web_client_id);
            Log.d(TAG, "Web client ID configured: " + !clientId.isEmpty());

            if (clientId.equals("your_web_client_id_here") ||
                    clientId.contains("example") ||
                    clientId.contains("default") ||
                    clientId.length() < 50) {
                Log.e(TAG, "Web client ID not properly configured! Current value: " + clientId);
                Toast.makeText(this, "Google Sign-in configuration error. Check web client ID.", Toast.LENGTH_LONG).show();
            }

            // Check Google Play Services availability
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
            Log.d(TAG, "Google Play Services availability: " +
                    (resultCode == ConnectionResult.SUCCESS ? "Available" : "Not available (" + resultCode + ")"));

        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Sign-in configuration", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            // User is already signed in, navigate to next activity
            navigateToNextActivity();
        }
    }

    // Progress Dialog Helper Methods
    private void showProgressDialog(String message) {
        if (progressDialog != null) {
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        }
    }

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void navigateToNextActivity() {
        // Navigate to GeneralPersonDashboardActivity
        Intent intent = new Intent(this, GeneralPersonDashboardActivity.class);
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't auto-dismiss progress dialog on resume for Google Sign-in
        // It should only be dismissed when the process is complete
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }

}