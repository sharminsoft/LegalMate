package com.yourname.legalmate.GeneralPersonPortal.Activities;

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
import com.yourname.legalmate.GeneralPersonPortal.Adapters.AppointmentAdapter;
import com.yourname.legalmate.GeneralPersonPortal.Models.AppointmentModel;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.List;

public class BookConsultationActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentClickListener {

    private static final String TAG = "BookConsultationActivity";

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
    private AppointmentAdapter appointmentAdapter;
    private List<AppointmentModel> allAppointmentsList; // Original list
    private List<AppointmentModel> filteredAppointmentsList; // Filtered list
    private String currentUserId;
    private String currentFilter = "all"; // Current filter state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_consultation);

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
        appointmentAdapter = new AppointmentAdapter(this, filteredAppointmentsList);
        appointmentAdapter.setOnAppointmentClickListener(this);

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

        // Using the fixed query without orderBy to avoid index requirement
        db.collection("Appointments")
                .whereEqualTo("clientId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allAppointmentsList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        AppointmentModel appointment = document.toObject(AppointmentModel.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(document.getId());
                            allAppointmentsList.add(appointment);

                            // Load lawyer name for each appointment
                            loadLawyerName(appointment);
                        }
                    }

                    // Sort the list in code by createdAt descending (if you have createdAt field)
                    // allAppointmentsList.sort((a1, a2) -> {
                    //     if (a1.getCreatedAt() != null && a2.getCreatedAt() != null) {
                    //         return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                    //     }
                    //     return 0;
                    // });

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

    private void loadLawyerName(AppointmentModel appointment) {
        if (appointment.getLawyerId() != null && !appointment.getLawyerId().isEmpty()) {
            db.collection("Users")
                    .document("Lawyers")
                    .collection("Lawyers")
                    .document(appointment.getLawyerId())
                    .collection("ProfileData")
                    .document("BasicProfileSettings")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                appointment.setLawyerName("Advocate " + fullName);
                                appointmentAdapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading lawyer name for appointment: " + appointment.getAppointmentId(), e);
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
                message = "No pending appointments found.\nYour pending appointments will appear here.";
                break;
            case "approved":
                message = "No approved appointments found.\nYour confirmed appointments will appear here.";
                break;
            case "cancelled":
                message = "No cancelled appointments found.\nYour cancelled appointments will appear here.";
                break;
            default:
                message = "You haven't booked any appointments yet.\nStart by finding a lawyer and booking a consultation.";
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

    // AppointmentAdapter.OnAppointmentClickListener implementation
    @Override
    public void onAppointmentClick(AppointmentModel appointment) {
        showAppointmentDetails(appointment);
    }

    @Override
    public void onCancelClick(AppointmentModel appointment) {
        showCancelConfirmationDialog(appointment);
    }

    @Override
    public void onRescheduleClick(AppointmentModel appointment) {
        // TODO: Implement reschedule functionality
        showToast("Reschedule feature coming soon");
    }

    private void showAppointmentDetails(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Appointment Details")
                .setMessage(buildAppointmentDetailsMessage(appointment))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Lawyer Details", (dialog, which) -> {

                    Intent lawyerDetailsIntent = new Intent(BookConsultationActivity.this, LawyerProfileDetailActivity.class);
                    lawyerDetailsIntent.putExtra("lawyer_id", appointment.getLawyerId());
                    startActivity(lawyerDetailsIntent);
                    dialog.dismiss();
                })
                .show();
    }

    private String buildAppointmentDetailsMessage(AppointmentModel appointment) {
        StringBuilder message = new StringBuilder();
        message.append("Case Title: ").append(appointment.getCaseTitle()).append("\n\n");
        message.append("Lawyer: ").append(appointment.getLawyerName() != null ?
                appointment.getLawyerName() : "Loading...").append("\n\n");
        message.append("Date: ").append(appointment.getDate()).append("\n");
        message.append("Time: ").append(appointment.getTime()).append("\n\n");
        message.append("Status: ").append(capitalizeFirst(appointment.getStatus())).append("\n\n");

        if (appointment.getDescription() != null && !appointment.getDescription().isEmpty()) {
            message.append("Description: ").append(appointment.getDescription());
        }

        return message.toString();
    }

    private void showCancelConfirmationDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?\n\n" +
                        "Case: " + appointment.getCaseTitle() + "\n" +
                        "Date: " + appointment.getDate() + " at " + appointment.getTime())
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    cancelAppointment(appointment);
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void cancelAppointment(AppointmentModel appointment) {
        showLoading(true);

        db.collection("Appointments")
                .document(appointment.getAppointmentId())
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    showToast("Appointment cancelled successfully");

                    // Update local data
                    appointment.setStatus("cancelled");
                    filterAppointments(); // Re-filter to reflect changes
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cancelling appointment", e);
                    showLoading(false);
                    showToast("Failed to cancel appointment");
                });
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
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
    }
}