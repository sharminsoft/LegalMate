package com.yourname.legalmate.OuterActivities;

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
import com.yourname.legalmate.LawyerPortal.Activities.LawyerDashboardActivity;
import com.yourname.legalmate.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI Components
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvForgotPassword, tvCreateAccountLink;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressDialog progressDialog;

    // Modern way to handle activity results
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeFirebase();
        setupGoogleSignIn();
        setupClickListeners();

        // Check Google Sign-in configuration (for debugging)
        checkGoogleSignInConfiguration();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvCreateAccountLink = findViewById(R.id.tvCreateAccountLink);

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
                                    updateProgressDialog("Checking account status...");

                                    // Check if user exists in Firebase Authentication
                                    checkUserExistsAndAuthenticate(account);
                                } else {
                                    Log.e(TAG, "Account or ID token is null");
                                    hideProgressDialog();
                                    Toast.makeText(LoginActivity.this, "Failed to get account information", Toast.LENGTH_LONG).show();
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
                                Toast.makeText(LoginActivity.this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    loginWithEmail();
                }
            }
        });

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle forgot password functionality
                handleForgotPassword();
            }
        });

        tvCreateAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to SelectRoleActivity
                Intent intent = new Intent(LoginActivity.this, SelectRoleActivity.class);
                startActivity(intent);
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

    private void checkUserExistsAndAuthenticate(GoogleSignInAccount account) {
        String email = account.getEmail();

        // Update progress message
        updateProgressDialog("Verifying account...");

        // Check if user exists in Firebase Authentication by trying to sign in
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Google authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Check if this is a new user or existing user
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                            if (isNewUser) {
                                // New user - delete the account and ask them to create account first
                                Log.d(TAG, "New user detected, deleting account and redirecting to create account");

                                updateProgressDialog("Account not found...");

                                user.delete().addOnCompleteListener(deleteTask -> {
                                    hideProgressDialog();
                                    Toast.makeText(LoginActivity.this,
                                            "No account found with this email. Please create an account first.",
                                            Toast.LENGTH_LONG).show();

                                    // Navigate to create account
                                    Intent intent = new Intent(LoginActivity.this, SelectRoleActivity.class);
                                    startActivity(intent);
                                });
                            } else {
                                // Existing user - proceed with login
                                Log.d(TAG, "Existing user found, checking role");
                                updateProgressDialog("Checking user profile...");
                                checkUserRoleAndNavigate(user.getUid());
                            }
                        }
                    } else {
                        Log.e(TAG, "Google authentication failed", task.getException());
                        hideProgressDialog();
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginWithEmail() {
        showProgressDialog("Signing in...");

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Attempting email login for: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update progress message
                            updateProgressDialog("Checking user profile...");
                            checkUserRoleAndNavigate(user.getUid());
                        }
                    } else {
                        Log.e(TAG, "signInWithEmail:failure", task.getException());
                        hideProgressDialog();
                        String errorMessage = "Login failed. ";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record")) {
                                    errorMessage = "No account found with this email address.";
                                } else if (exceptionMessage.contains("password is invalid")) {
                                    errorMessage = "Incorrect password. Please try again.";
                                } else if (exceptionMessage.contains("badly formatted")) {
                                    errorMessage = "Please enter a valid email address.";
                                } else {
                                    errorMessage += exceptionMessage;
                                }
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRoleAndNavigate(String userId) {
        updateProgressDialog("Loading dashboard...");

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

                                updateProgressDialog("Opening lawyer dashboard...");

                                new android.os.Handler().postDelayed(() -> {
                                    hideProgressDialog();
                                    navigateToLawyerDashboard();
                                }, 500);
                                return;
                            }
                        }
                    }

                    // If not found in lawyers, check in general users collection
                    firestore.collection("Users")
                            .document("GeneralPersons")
                            .collection("GeneralPersons")
                            .document(userId)
                            .get()
                            .addOnCompleteListener(generalTask -> {
                                if (generalTask.isSuccessful()) {
                                    DocumentSnapshot generalDoc = generalTask.getResult();
                                    if (generalDoc.exists()) {
                                        Log.d(TAG, "User is a general person, navigating to GeneralPersonDashboardActivity");

                                        updateProgressDialog("Opening dashboard...");

                                        new android.os.Handler().postDelayed(() -> {
                                            hideProgressDialog();
                                            navigateToGeneralPersonDashboard();
                                        }, 500);
                                    } else {
                                        // User not found in any collection
                                        Log.w(TAG, "User not found in any collection");
                                        hideProgressDialog();
                                        Toast.makeText(LoginActivity.this,
                                                "User profile not found. Please contact support.",
                                                Toast.LENGTH_LONG).show();

                                        // Sign out the user
                                        mAuth.signOut();
                                    }
                                } else {
                                    Log.e(TAG, "Error checking general user", generalTask.getException());
                                    hideProgressDialog();
                                    Toast.makeText(LoginActivity.this,
                                            "Error loading user profile. Please try again.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Please enter your email address first");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            return;
        }

        showProgressDialog("Sending password reset email...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideProgressDialog();
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = "Failed to send reset email. ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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
        }

        return isValid;
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
        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

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
            // User is already signed in, check role and navigate
            checkUserRoleAndNavigate(currentUser.getUid());
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

    private void navigateToLawyerDashboard() {
        Intent intent = new Intent(this, LawyerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToGeneralPersonDashboard() {
        Intent intent = new Intent(this, GeneralPersonDashboardActivity.class);
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