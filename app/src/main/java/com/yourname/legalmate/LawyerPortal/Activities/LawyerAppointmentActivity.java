package com.yourname.legalmate.LawyerPortal.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.GeneralPersonPortal.Models.AppointmentModel;
import com.yourname.legalmate.LawyerPortal.Adapters.LawyerAppointmentAdapter;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LawyerAppointmentActivity extends AppCompatActivity implements LawyerAppointmentAdapter.OnLawyerAppointmentClickListener {

    private static final String TAG = "LawyerAppointmentActivity";

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewAppointments;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipPending, chipApproved, chipCancelled;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Data
    private LawyerAppointmentAdapter appointmentAdapter;
    private List<AppointmentModel> allAppointmentsList; // Original list
    private List<AppointmentModel> filteredAppointmentsList; // Filtered list
    private String currentUserId;
    private String currentFilter = "all"; // Current filter state

    // Cache for client names to avoid repeated queries
    private Map<String, String> clientNamesCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lawyer_appoinment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        checkUserAuthentication();
    }

    private void initializeComponents() {
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar);
        recyclerViewAppointments = findViewById(R.id.recyclerViewAppointments);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // Filter components
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipApproved = findViewById(R.id.chipApproved);
        chipCancelled = findViewById(R.id.chipCancelled);

        // Initialize data
        allAppointmentsList = new ArrayList<>();
        filteredAppointmentsList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        appointmentAdapter = new LawyerAppointmentAdapter(this, filteredAppointmentsList, clientNamesCache);
        appointmentAdapter.setOnLawyerAppointmentClickListener(this);

        recyclerViewAppointments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAppointments.setAdapter(appointmentAdapter);
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            String newFilter = "all";

            if (checkedId == R.id.chipPending) {
                newFilter = "pending";
            } else if (checkedId == R.id.chipApproved) {
                newFilter = "approved";
            } else if (checkedId == R.id.chipCancelled) {
                newFilter = "cancelled";
            }

            if (!newFilter.equals(currentFilter)) {
                currentFilter = newFilter;
                filterAppointments();
            }
        });
    }

    private void checkUserAuthentication() {
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            loadAppointments();
        } else {
            showToast("Please login to view appointments");
            finish();
        }
    }

    private void loadAppointments() {
        showLoading(true);

        // Query appointments where lawyer ID matches current user
        db.collection("Appointments")
                .whereEqualTo("lawyerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allAppointmentsList.clear();
                    clientNamesCache.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        AppointmentModel appointment = document.toObject(AppointmentModel.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(document.getId());
                            allAppointmentsList.add(appointment);

                            // Load client name for each appointment
                            loadClientName(appointment);
                        }
                    }

                    filterAppointments();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading appointments", e);
                    showToast("Failed to load appointments");
                    showLoading(false);
                    updateUI();
                });
    }

    private void filterAppointments() {
        filteredAppointmentsList.clear();

        for (AppointmentModel appointment : allAppointmentsList) {
            boolean shouldInclude = false;

            switch (currentFilter) {
                case "all":
                    shouldInclude = true;
                    break;
                case "pending":
                    shouldInclude = "pending".equalsIgnoreCase(appointment.getStatus());
                    break;
                case "approved":
                    shouldInclude = "approved".equalsIgnoreCase(appointment.getStatus());
                    break;
                case "cancelled":
                    shouldInclude = "cancelled".equalsIgnoreCase(appointment.getStatus());
                    break;
            }

            if (shouldInclude) {
                filteredAppointmentsList.add(appointment);
            }
        }

        updateUI();
    }

    private void loadClientName(AppointmentModel appointment) {
        if (appointment.getClientId() != null && !appointment.getClientId().isEmpty()) {

            // Check cache first
            if (clientNamesCache.containsKey(appointment.getClientId())) {
                appointmentAdapter.notifyDataSetChanged();
                return;
            }

            // Correct path - directly from GeneralPersons document
            db.collection("Users")
                    .document("GeneralPersons")
                    .collection("GeneralPersons")
                    .document(appointment.getClientId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("name"); // 'name' field from account creation
                            if (fullName != null && !fullName.isEmpty()) {
                                clientNamesCache.put(appointment.getClientId(), fullName);
                                appointmentAdapter.notifyDataSetChanged();
                            } else {
                                // Fallback - try email if name is not available
                                String email = documentSnapshot.getString("email");
                                if (email != null) {
                                    clientNamesCache.put(appointment.getClientId(), email);
                                    appointmentAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading client name for appointment: " + appointment.getAppointmentId(), e);
                    });
        }
    }

    private void updateUI() {
        if (filteredAppointmentsList.isEmpty()) {
            recyclerViewAppointments.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            updateEmptyStateMessage();
        } else {
            recyclerViewAppointments.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            appointmentAdapter.updateAppointments(filteredAppointmentsList);
        }
    }

    private void updateEmptyStateMessage() {
        String message;
        switch (currentFilter) {
            case "pending":
                message = "No pending appointment requests found.\nPending requests from clients will appear here.";
                break;
            case "approved":
                message = "No approved appointments found.\nYour approved appointments will appear here.";
                break;
            case "cancelled":
                message = "No cancelled appointments found.\nCancelled appointments will appear here.";
                break;
            default:
                message = "No appointment requests found.\nClient appointment requests will appear here.";
                break;
        }
        tvEmptyMessage.setText(message);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // LawyerAppointmentAdapter.OnLawyerAppointmentClickListener implementation
    @Override
    public void onAppointmentClick(AppointmentModel appointment) {
        showAppointmentDetails(appointment);
    }

    @Override
    public void onApproveClick(AppointmentModel appointment) {
        showApprovalConfirmationDialog(appointment);
    }

    @Override
    public void onRejectClick(AppointmentModel appointment) {
        showRejectionConfirmationDialog(appointment);
    }

    @Override
    public void onClientDetailsClick(AppointmentModel appointment) {
        showClientDetailsDialog(appointment);
    }

    private void showAppointmentDetails(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Appointment Request Details")
                .setMessage(buildAppointmentDetailsMessage(appointment))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Client Details", (dialog, which) -> {
                    onClientDetailsClick(appointment);
                    dialog.dismiss();
                })
                .show();
    }

    private String buildAppointmentDetailsMessage(AppointmentModel appointment) {
        StringBuilder message = new StringBuilder();
        message.append("Case Title: ").append(appointment.getCaseTitle() != null ? appointment.getCaseTitle() : "N/A").append("\n\n");
        message.append("Client: ").append(getClientName(appointment.getClientId())).append("\n\n");
        message.append("Date: ").append(appointment.getDate() != null ? appointment.getDate() : "N/A").append("\n");
        message.append("Time: ").append(appointment.getTime() != null ? appointment.getTime() : "N/A").append("\n\n");
        message.append("Status: ").append(capitalizeFirst(appointment.getStatus())).append("\n\n");

        if (appointment.getDescription() != null && !appointment.getDescription().isEmpty()) {
            message.append("Description: ").append(appointment.getDescription());
        }

        return message.toString();
    }

    private void showClientDetailsDialog(AppointmentModel appointment) {
        if (appointment.getClientId() == null || appointment.getClientId().isEmpty()) {
            showToast("Client information not available");
            return;
        }

        // Show loading dialog
        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Loading Client Details")
                .setMessage("Please wait...")
                .setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = loadingDialog.create();
        dialog.show();

        // Fetch detailed client information
        fetchClientDetails(appointment.getClientId(), new ClientDetailsCallback() {
            @Override
            public void onSuccess(Map<String, Object> clientData) {
                dialog.dismiss();
                displayClientDetailsDialog(clientData);
            }

            @Override
            public void onFailure(String error) {
                dialog.dismiss();
                showToast("Failed to load client details: " + error);
            }
        });
    }

    private void fetchClientDetails(String clientId, ClientDetailsCallback callback) {
        // Correct path - directly from GeneralPersons document
        db.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(clientId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Convert document to map and add available fields
                        Map<String, Object> clientData = new HashMap<>();

                        // Get data from the document based on GenPerCreateAccountActivity structure
                        if (documentSnapshot.getString("name") != null) {
                            clientData.put("fullName", documentSnapshot.getString("name"));
                        }
                        if (documentSnapshot.getString("email") != null) {
                            clientData.put("email", documentSnapshot.getString("email"));
                        }
                        if (documentSnapshot.getString("uid") != null) {
                            clientData.put("uid", documentSnapshot.getString("uid"));
                        }
                        if (documentSnapshot.getString("role") != null) {
                            clientData.put("role", documentSnapshot.getString("role"));
                        }
                        if (documentSnapshot.getString("signInMethod") != null) {
                            clientData.put("signInMethod", documentSnapshot.getString("signInMethod"));
                        }
                        if (documentSnapshot.get("createdAt") != null) {
                            clientData.put("createdAt", documentSnapshot.get("createdAt"));
                        }

                        callback.onSuccess(clientData);
                    } else {
                        callback.onFailure("Client profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching client details", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private void displayClientDetailsDialog(Map<String, Object> clientData) {
        StringBuilder details = new StringBuilder();

        if (clientData.get("fullName") != null) {
            details.append("Name: ").append(clientData.get("fullName")).append("\n\n");
        }

        if (clientData.get("email") != null) {
            details.append("Email: ").append(clientData.get("email")).append("\n\n");
        }

        if (clientData.get("role") != null) {
            details.append("Role: ").append(capitalizeFirst(clientData.get("role").toString())).append("\n\n");
        }

        if (clientData.get("signInMethod") != null) {
            details.append("Sign-in Method: ").append(capitalizeFirst(clientData.get("signInMethod").toString())).append("\n\n");
        }

        if (clientData.get("createdAt") != null) {
            details.append("Member Since: ").append(clientData.get("createdAt").toString()).append("\n\n");
        }

        if (details.length() == 0) {
            details.append("No detailed information available for this client.");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Client Details")
                .setMessage(details.toString().trim())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private interface ClientDetailsCallback {
        void onSuccess(Map<String, Object> clientData);
        void onFailure(String error);
    }

    private String getClientName(String clientId) {
        if (clientId != null && clientNamesCache.containsKey(clientId)) {
            return clientNamesCache.get(clientId);
        }
        return "Loading...";
    }

    private void showApprovalConfirmationDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Approve Appointment")
                .setMessage("Are you sure you want to approve this appointment request?\n\n" +
                        "Client: " + getClientName(appointment.getClientId()) + "\n" +
                        "Case: " + (appointment.getCaseTitle() != null ? appointment.getCaseTitle() : "N/A") + "\n" +
                        "Date: " + (appointment.getDate() != null ? appointment.getDate() : "N/A") + " at " +
                        (appointment.getTime() != null ? appointment.getTime() : "N/A"))
                .setPositiveButton("Yes, Approve", (dialog, which) -> {
                    approveAppointment(appointment);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showRejectionConfirmationDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reject Appointment")
                .setMessage("Are you sure you want to reject this appointment request?\n\n" +
                        "Client: " + getClientName(appointment.getClientId()) + "\n" +
                        "Case: " + (appointment.getCaseTitle() != null ? appointment.getCaseTitle() : "N/A") + "\n" +
                        "Date: " + (appointment.getDate() != null ? appointment.getDate() : "N/A") + " at " +
                        (appointment.getTime() != null ? appointment.getTime() : "N/A"))
                .setPositiveButton("Yes, Reject", (dialog, which) -> {
                    rejectAppointment(appointment);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void approveAppointment(AppointmentModel appointment) {
        showLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("Appointments")
                .document(appointment.getAppointmentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    showToast("Appointment approved successfully");

                    // Update local data
                    appointment.setStatus("approved");
                    filterAppointments(); // Re-filter to reflect changes
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error approving appointment", e);
                    showLoading(false);
                    showToast("Failed to approve appointment");
                });
    }

    private void rejectAppointment(AppointmentModel appointment) {
        showLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("Appointments")
                .document(appointment.getAppointmentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    showToast("Appointment rejected successfully");

                    // Update local data
                    appointment.setStatus("cancelled");
                    filterAppointments(); // Re-filter to reflect changes
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error rejecting appointment", e);
                    showLoading(false);
                    showToast("Failed to reject appointment");
                });
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return "N/A";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload appointments when returning to this activity
        if (currentUserId != null) {
            loadAppointments();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources if needed
        if (clientNamesCache != null) {
            clientNamesCache.clear();
        }
    }
}