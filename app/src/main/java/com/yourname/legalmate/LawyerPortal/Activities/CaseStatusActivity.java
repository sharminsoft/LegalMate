package com.yourname.legalmate.LawyerPortal.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.cardview.widget.CardView;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CaseStatusActivity extends AppCompatActivity {

    // Add this at the top of CaseStatusActivity class
    private static final int EDIT_CASE_REQUEST_CODE = 1001;

    private static final String TAG = "CaseStatusActivity";

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String caseId;

    // UI Components - Client Details Card
    private CardView cardClientDetails;
    private TextView tvClientName, tvClientPhone, tvClientEmail, tvClientAddress, tvClientType;

    // UI Components - Case Details Card
    private CardView cardCaseDetails;
    private TextView tvCaseTitle, tvCaseType, tvCaseDescription, tvCourtName,
            tvCaseNumber, tvFilingDate, tvCourtLocation, tvJudgeName;

    // UI Components - Status & Tracking Card
    private CardView cardStatusTracking;
    private Spinner spinnerCaseStatus, spinnerCaseStage;
    private EditText etExpectedOutcome;
    private TextView tvLastUpdated;
    private Button btnUpdateStatus;

    // Loading and Error States
    private LinearLayout layoutLoading, layoutError;
    private ScrollView layoutContent;
    private TextView tvErrorMessage;
    private Button btnRetry;

    // Progress Dialog
    private ProgressDialog progressDialog;

    ImageButton btnEdit, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_case_status);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupActionBar();
        setupSpinners();
        setupClickListeners();

        // Get case ID from intent
        caseId = getIntent().getStringExtra("caseId");
        if (caseId != null) {
            loadCaseData();
        } else {
            showError("Invalid case ID");
        }
    }

    private void initializeComponents() {
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI Components - Client Details
        cardClientDetails = findViewById(R.id.cardClientDetails);
        tvClientName = findViewById(R.id.tvClientName);
        tvClientPhone = findViewById(R.id.tvClientPhone);
        tvClientEmail = findViewById(R.id.tvClientEmail);
        tvClientAddress = findViewById(R.id.tvClientAddress);
        tvClientType = findViewById(R.id.tvClientType);

        // Initialize UI Components - Case Details
        cardCaseDetails = findViewById(R.id.cardCaseDetails);
        tvCaseTitle = findViewById(R.id.tvCaseTitle);
        tvCaseType = findViewById(R.id.tvCaseType);
        tvCaseDescription = findViewById(R.id.tvCaseDescription);
        tvCourtName = findViewById(R.id.tvCourtName);
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        tvFilingDate = findViewById(R.id.tvFilingDate);
        tvCourtLocation = findViewById(R.id.tvCourtLocation);
        tvJudgeName = findViewById(R.id.tvJudgeName);

        // Initialize UI Components - Status & Tracking
        cardStatusTracking = findViewById(R.id.cardStatusTracking);
        spinnerCaseStatus = findViewById(R.id.spinnerCaseStatus);
        spinnerCaseStage = findViewById(R.id.spinnerCaseStage);
        etExpectedOutcome = findViewById(R.id.etExpectedOutcome);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);

        // Initialize Loading and Error States
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutError = findViewById(R.id.layoutError);
        layoutContent = findViewById(R.id.layoutContent);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry = findViewById(R.id.btnRetry);

        btnEdit = findViewById(R.id.btnEdit);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Case Status & Details");
        }
    }

    private void setupSpinners() {
        // Setup Case Status Spinner
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCaseStatus.setAdapter(statusAdapter);

        // Setup Case Stage Spinner
        ArrayAdapter<CharSequence> stageAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_stage_options, android.R.layout.simple_spinner_item);
        stageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCaseStage.setAdapter(stageAdapter);
    }

    private void setupClickListeners() {
        btnUpdateStatus.setOnClickListener(v -> updateCaseStatus());
        btnRetry.setOnClickListener(v -> loadCaseData());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(CaseStatusActivity.this, AddCaseInfoActivity.class);
            intent.putExtra("caseId", caseId);
            intent.putExtra("fromActivity", "CaseStatusActivity");
            startActivityForResult(intent, EDIT_CASE_REQUEST_CODE);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCaseData() {
        showLoading();

        DocumentReference caseRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId);

        // Load main case data
        caseRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        loadClientDetails();
                    } else {
                        showError("Case not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading case data: " + e.getMessage());
                    showError("Failed to load case data: " + e.getMessage());
                });
    }

    private void loadClientDetails() {
        DocumentReference clientRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Client Details")
                .document("client_info");

        clientRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate client details
                        tvClientName.setText(documentSnapshot.getString("clientName"));
                        tvClientPhone.setText(documentSnapshot.getString("clientPhone"));
                        tvClientEmail.setText(documentSnapshot.getString("clientEmail"));
                        tvClientAddress.setText(documentSnapshot.getString("clientAddress"));
                        tvClientType.setText(documentSnapshot.getString("clientType"));
                    }
                    loadCaseDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading client details: " + e.getMessage());
                    loadCaseDetails(); // Continue loading even if client details fail
                });
    }

    private void loadCaseDetails() {
        DocumentReference caseDetailsRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Case Details")
                .document("case_info");

        caseDetailsRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate case details
                        tvCaseTitle.setText(documentSnapshot.getString("caseTitle"));
                        tvCaseType.setText(documentSnapshot.getString("caseType"));
                        tvCaseDescription.setText(documentSnapshot.getString("caseDescription"));
                        tvCourtName.setText(documentSnapshot.getString("courtName"));
                        tvCaseNumber.setText(documentSnapshot.getString("caseNumber"));

                        String filingDate = documentSnapshot.getString("filingDate");
                        if (filingDate != null && !filingDate.isEmpty()) {
                            tvFilingDate.setText(formatDate(filingDate));
                        }

                        tvCourtLocation.setText(documentSnapshot.getString("courtLocation"));
                        tvJudgeName.setText(documentSnapshot.getString("judgeName"));
                    }
                    loadStatusTracking();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading case details: " + e.getMessage());
                    loadStatusTracking(); // Continue loading even if case details fail
                });
    }

    private void loadStatusTracking() {
        DocumentReference statusRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Status & Tracking")
                .document("status_info");

        statusRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate status and tracking
                        String caseStatus = documentSnapshot.getString("caseStatus");
                        String caseStage = documentSnapshot.getString("caseStage");
                        String expectedOutcome = documentSnapshot.getString("expectedOutcome");

                        setSpinnerSelection(spinnerCaseStatus, caseStatus);
                        setSpinnerSelection(spinnerCaseStage, caseStage);
                        etExpectedOutcome.setText(expectedOutcome);

                        // Set last updated
                        Date updatedAt = documentSnapshot.getDate("createdAt");
                        if (updatedAt != null) {
                            String formattedDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(updatedAt);
                            tvLastUpdated.setText("Last updated: " + formattedDate);
                        }
                    }
                    showContent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading status tracking: " + e.getMessage());
                    showContent(); // Show content even if status fails to load
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value != null && spinner.getAdapter() != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString; // Return original string if parsing fails
        }
    }

    private void updateCaseStatus() {
        if (!validateStatusForm()) {
            return;
        }

        showProgressDialog("Updating case status...");

        DocumentReference statusRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Status & Tracking")
                .document("status_info");

        // Prepare updated data
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("caseStatus", spinnerCaseStatus.getSelectedItem().toString());
        statusData.put("caseStage", spinnerCaseStage.getSelectedItem().toString());
        statusData.put("expectedOutcome", etExpectedOutcome.getText().toString().trim());
        statusData.put("updatedAt", new Date());

        // Update status tracking
        statusRef.update(statusData)
                .addOnSuccessListener(aVoid -> {
                    // Also update main case document for quick access
                    updateMainCaseDocument();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating status: " + e.getMessage());
                });
    }

    private void updateMainCaseDocument() {
        DocumentReference caseRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId);

        Map<String, Object> caseData = new HashMap<>();
        caseData.put("caseStatus", spinnerCaseStatus.getSelectedItem().toString());
        caseData.put("updatedAt", new Date());

        caseRef.update(caseData)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showSuccessDialog();

                    // Update last updated text
                    String currentTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                    tvLastUpdated.setText("Last updated: " + currentTime);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    // Status was updated but main document failed - still show success
                    showSuccessDialog();
                    Log.e(TAG, "Error updating main case document: " + e.getMessage());
                });
    }

    private boolean validateStatusForm() {
        if (spinnerCaseStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select case status", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerCaseStage.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select case stage", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Case status updated successfully!")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }

    private void showError(String errorMessage) {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        tvErrorMessage.setText(errorMessage);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Add this method to handle the result from AddCaseInfoActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_CASE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("caseUpdated", false)) {
                // Case was updated, reload the data
                Toast.makeText(this, "Case updated successfully", Toast.LENGTH_SHORT).show();
                loadCaseData(); // Reload all case data
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}