package com.yourname.legalmate.GeneralPersonPortal.Activities;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.GeneralPersonPortal.Models.AppointmentModel;
import com.yourname.legalmate.LawyerPortal.Adapters.UpcomingAppointmentsAdapter;
import com.yourname.legalmate.LawyerPortal.CustomCalendarView;
import com.yourname.legalmate.LawyerPortal.Models.AppointmentSchedule;
import com.yourname.legalmate.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MyCalendarActivity extends AppCompatActivity {

    private static final String TAG = "MyCalendarActivity";

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // UI Components
    private CustomCalendarView customCalendarView;
    private RecyclerView rvUpcomingAppointments;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private View loadingOverlay;
    private TextView tvCurrentMonth;
    private TextView tvUpcomingCount;
    private TextView tvEmptyMessage;

    // Navigation Components
    private ImageButton btnBack;

    // Data - Appointments
    private List<AppointmentSchedule> allAppointmentSchedules;
    private List<AppointmentSchedule> upcomingAppointmentSchedules;
    private Map<String, List<AppointmentSchedule>> dateToAppointmentSchedulesMap;
    private Set<String> appointmentScheduledDates;
    private Set<String> appointmentPastDates;
    private Map<String, String> lawyerNamesCache;
    private Set<String> processedAppointmentIds;

    // Adapter
    private UpcomingAppointmentsAdapter upcomingAppointmentsAdapter;

    // Date formatters
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat displayTimeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_calendar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupCalendar();
        setupRecyclerView();
        loadAppointmentSchedules();
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

        // Initialize UI components
        customCalendarView = findViewById(R.id.customCalendarView);
        rvUpcomingAppointments = findViewById(R.id.rvUpcomingAppointments);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Initialize data structures for Appointments
        allAppointmentSchedules = new ArrayList<>();
        upcomingAppointmentSchedules = new ArrayList<>();
        dateToAppointmentSchedulesMap = new HashMap<>();
        appointmentScheduledDates = new HashSet<>();
        appointmentPastDates = new HashSet<>();
        lawyerNamesCache = new HashMap<>();
        processedAppointmentIds = new HashSet<>();
    }

    private void setupCalendar() {
        // Set calendar to current date
        customCalendarView.setDate(System.currentTimeMillis());
        updateMonthDisplay();

        // Set date selection listener
        customCalendarView.setOnDateSelectedListener((date, year, month, dayOfMonth) -> {
            Log.d(TAG, "Date selected: " + date);
            handleDateSelection(date);
        });

        // Setup navigation buttons
        findViewById(R.id.btnPreviousMonth).setOnClickListener(v -> {
            customCalendarView.goToPreviousMonth();
            updateMonthDisplay();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            customCalendarView.goToNextMonth();
            updateMonthDisplay();
        });

        findViewById(R.id.btnToday).setOnClickListener(v -> {
            customCalendarView.goToCurrentMonth();
            customCalendarView.setDate(System.currentTimeMillis());
            updateMonthDisplay();
        });
    }

    private void setupRecyclerView() {
        // Setup for appointments
        upcomingAppointmentsAdapter = new UpcomingAppointmentsAdapter(upcomingAppointmentSchedules,
                appointment -> {
                    List<AppointmentSchedule> singleAppointment = new ArrayList<>();
                    singleAppointment.add(appointment);
                    showAppointmentDetailsBottomSheet(singleAppointment, appointment.getAppointmentDate());
                }, lawyerNamesCache);

        rvUpcomingAppointments.setAdapter(upcomingAppointmentsAdapter);
        rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void handleDateSelection(String date) {
        if (dateToAppointmentSchedulesMap.containsKey(date)) {
            List<AppointmentSchedule> appointmentsForDate = dateToAppointmentSchedulesMap.get(date);
            showAppointmentDetailsBottomSheet(appointmentsForDate, date);
        } else {
            Toast.makeText(this, "No appointments scheduled for this date", Toast.LENGTH_SHORT).show();
        }
    }

    // ============ APPOINTMENTS LOADING METHODS ============

    private void loadAppointmentSchedules() {
        showProgress();
        Log.d(TAG, "Loading appointment schedules for client: " + currentUserId);

        // Clear existing appointment data
        allAppointmentSchedules.clear();
        upcomingAppointmentSchedules.clear();
        dateToAppointmentSchedulesMap.clear();
        appointmentScheduledDates.clear();
        appointmentPastDates.clear();
        lawyerNamesCache.clear();
        processedAppointmentIds.clear(); // Clear processed IDs

        // Get current date for comparison
        String currentDateStr = dateFormatter.format(new Date());

        // Query appointments where client ID matches current user
        db.collection("Appointments")
                .whereEqualTo("clientId", currentUserId)
                .whereEqualTo("status", "approved") // Only load approved appointments
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No approved appointments found for client");
                        hideProgress();
                        showEmptyState("No approved appointments found",
                                "Your approved appointments with lawyers will appear here.");
                        return;
                    }

                    int totalAppointments = queryDocumentSnapshots.size();
                    AtomicInteger processedAppointments = new AtomicInteger(0);

                    Log.d(TAG, "Found " + totalAppointments + " approved appointments to process");

                    for (DocumentSnapshot appointmentDoc : queryDocumentSnapshots.getDocuments()) {
                        AppointmentModel appointment = appointmentDoc.toObject(AppointmentModel.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(appointmentDoc.getId());
                            processAppointmentSchedule(appointment, currentDateStr,
                                    processedAppointments, totalAppointments);
                        } else {
                            int processed = processedAppointments.incrementAndGet();
                            if (processed >= totalAppointments) {
                                finishLoadingAppointmentSchedules(currentDateStr);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading appointments: " + e.getMessage());
                    hideProgress();
                    showEmptyState("Failed to load appointments",
                            "Please check your internet connection and try again.");
                    Toast.makeText(this, "Failed to load appointments: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void processAppointmentSchedule(AppointmentModel appointment, String currentDateStr,
                                            AtomicInteger processedAppointments, int totalAppointments) {

        // Check for duplicates using appointment ID
        String appointmentId = appointment.getAppointmentId();
        if (appointmentId != null && processedAppointmentIds.contains(appointmentId)) {
            int processed = processedAppointments.incrementAndGet();
            Log.d(TAG, "Skipping duplicate appointment: " + appointmentId);

            if (processed >= totalAppointments) {
                finishLoadingAppointmentSchedules(currentDateStr);
            }
            return;
        }

        // Validate appointment date
        if (appointment.getDate() == null || appointment.getDate().trim().isEmpty()) {
            int processed = processedAppointments.incrementAndGet();
            Log.d(TAG, "Skipping appointment with no date: " + appointment.getCaseTitle());

            if (processed >= totalAppointments) {
                finishLoadingAppointmentSchedules(currentDateStr);
            }
            return;
        }

        // Mark this appointment as processed
        if (appointmentId != null) {
            processedAppointmentIds.add(appointmentId);
        }

        // Create AppointmentSchedule object
        AppointmentSchedule schedule = new AppointmentSchedule();
        schedule.setAppointmentId(appointment.getAppointmentId());
        schedule.setCaseTitle(appointment.getCaseTitle() != null ? appointment.getCaseTitle() : "Consultation");
        schedule.setClientId(appointment.getClientId());
        schedule.setClientName("Me"); // Since this is client view, we know it's "me"
        schedule.setAppointmentDate(appointment.getDate());
        schedule.setAppointmentTime(appointment.getTime() != null ? appointment.getTime() : "");
        schedule.setDescription(appointment.getDescription() != null ? appointment.getDescription() : "");
        schedule.setStatus(appointment.getStatus());

        // Load lawyer name for display purposes
        loadLawyerNameForAppointment(schedule, appointment.getLawyerId(), () -> {
            // Add to collections
            synchronized (this) {
                String appointmentDate = schedule.getAppointmentDate();

                allAppointmentSchedules.add(schedule);
                appointmentScheduledDates.add(appointmentDate);

                // Determine if date is past or future
                if (isDateInPast(appointmentDate, currentDateStr)) {
                    appointmentPastDates.add(appointmentDate);
                } else {
                    upcomingAppointmentSchedules.add(schedule);
                }

                // Add to date mapping
                if (!dateToAppointmentSchedulesMap.containsKey(appointmentDate)) {
                    dateToAppointmentSchedulesMap.put(appointmentDate, new ArrayList<>());
                }
                dateToAppointmentSchedulesMap.get(appointmentDate).add(schedule);
            }

            int processed = processedAppointments.incrementAndGet();
            Log.d(TAG, "Processed appointment: " + schedule.getCaseTitle() + " (" + processed + "/" + totalAppointments + ")");

            if (processed >= totalAppointments) {
                finishLoadingAppointmentSchedules(currentDateStr);
            }
        });
    }

    private void loadLawyerNameForAppointment(AppointmentSchedule schedule, String lawyerId, Runnable onComplete) {
        if (lawyerId == null || lawyerId.isEmpty()) {
            onComplete.run();
            return;
        }

        // Check cache first
        if (lawyerNamesCache.containsKey(lawyerId)) {
            schedule.setDescription("Lawyer: " + lawyerNamesCache.get(lawyerId));
            onComplete.run();
            return;
        }

        // Load from Firestore
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(lawyerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lawyerName = documentSnapshot.getString("name");
                        if (lawyerName != null && !lawyerName.isEmpty()) {
                            lawyerNamesCache.put(lawyerId, lawyerName);
                            // Store lawyer name in description or create a separate field
                            String existingDesc = schedule.getDescription();
                            String newDesc = "Lawyer: " + lawyerName;
                            if (existingDesc != null && !existingDesc.isEmpty()) {
                                newDesc = newDesc + " | " + existingDesc;
                            }
                            schedule.setDescription(newDesc);
                        } else {
                            // Fallback to email
                            String email = documentSnapshot.getString("email");
                            if (email != null) {
                                lawyerNamesCache.put(lawyerId, email);
                                schedule.setDescription("Lawyer: " + email);
                            } else {
                                schedule.setDescription("Lawyer: Unknown");
                            }
                        }
                    } else {
                        schedule.setDescription("Lawyer: Unknown");
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer name: " + e.getMessage());
                    schedule.setDescription("Lawyer: Unknown");
                    onComplete.run();
                });
    }

    private void finishLoadingAppointmentSchedules(String currentDateStr) {
        hideProgress();

        Log.d(TAG, "Finishing appointment loading. Total: " + allAppointmentSchedules.size() +
                ", Upcoming: " + upcomingAppointmentSchedules.size());

        if (allAppointmentSchedules.isEmpty()) {
            showEmptyState("No appointments scheduled",
                    "Your approved appointments with lawyers will appear here.");
        } else {
            showSchedules();

            // Sort upcoming appointments by date and time
            Collections.sort(upcomingAppointmentSchedules, (a1, a2) -> {
                try {
                    Date date1 = dateFormatter.parse(a1.getAppointmentDate());
                    Date date2 = dateFormatter.parse(a2.getAppointmentDate());

                    if (date1 != null && date2 != null) {
                        int dateComparison = date1.compareTo(date2);
                        if (dateComparison == 0) {
                            // Same date, compare by time
                            String time1 = a1.getAppointmentTime();
                            String time2 = a2.getAppointmentTime();

                            if (time1 != null && time2 != null && !time1.isEmpty() && !time2.isEmpty()) {
                                try {
                                    Date timeDate1 = timeFormatter.parse(time1);
                                    Date timeDate2 = timeFormatter.parse(time2);
                                    if (timeDate1 != null && timeDate2 != null) {
                                        return timeDate1.compareTo(timeDate2);
                                    }
                                } catch (ParseException e) {
                                    Log.e(TAG, "Error comparing appointment times: " + e.getMessage());
                                }
                            }
                        }
                        return dateComparison;
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error comparing appointment dates: " + e.getMessage());
                }
                return 0;
            });

            // Update adapter
            if (upcomingAppointmentsAdapter != null) {
                upcomingAppointmentsAdapter.updateData(upcomingAppointmentSchedules);
            }

            // Apply calendar markings
            applyAppointmentCalendarMarkings();

            // Update count display
            String countText = upcomingAppointmentSchedules.size() == 1 ? "1 appointment" : upcomingAppointmentSchedules.size() + " appointments";
            tvUpcomingCount.setText(countText);

            String message = String.format("Loaded %d appointments (%d upcoming, %d past)",
                    allAppointmentSchedules.size(), upcomingAppointmentSchedules.size(),
                    allAppointmentSchedules.size() - upcomingAppointmentSchedules.size());

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyAppointmentCalendarMarkings() {
        // Separate upcoming and past appointment dates
        Set<String> upcomingDates = new HashSet<>();
        Set<String> pastDatesSet = new HashSet<>();

        for (String date : appointmentScheduledDates) {
            if (appointmentPastDates.contains(date)) {
                pastDatesSet.add(date);
            } else {
                upcomingDates.add(date);
            }
        }

        // Apply markings to custom calendar
        customCalendarView.setUpcomingDates(upcomingDates);
        customCalendarView.setPastDates(pastDatesSet);

        Log.d(TAG, "Applied appointment calendar markings - GREEN (upcoming): " + upcomingDates.size() +
                ", RED (past): " + pastDatesSet.size());
    }

    // ============ BOTTOM SHEET METHODS ============

    private void showAppointmentDetailsBottomSheet(List<AppointmentSchedule> appointments, String date) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_appointment_details, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Initialize bottom sheet views
        TextView tvDate = bottomSheetView.findViewById(R.id.tvDate);
        TextView tvAppointmentCount = bottomSheetView.findViewById(R.id.tvAppointmentCount);
        RecyclerView rvAppointments = bottomSheetView.findViewById(R.id.rvAppointments);

        // Set date with proper formatting
        try {
            Date parsedDate = dateFormatter.parse(date);
            if (parsedDate != null) {
                tvDate.setText(displayDateFormatter.format(parsedDate));
            } else {
                tvDate.setText(date);
            }
        } catch (ParseException e) {
            tvDate.setText(date);
        }

        // Set appointment count
        String countText = appointments.size() == 1 ? "1 appointment scheduled" : appointments.size() + " appointments scheduled";
        tvAppointmentCount.setText(countText);

        // Sort appointments by time
        List<AppointmentSchedule> sortedAppointments = new ArrayList<>(appointments);
        Collections.sort(sortedAppointments, (a1, a2) -> {
            String time1 = a1.getAppointmentTime();
            String time2 = a2.getAppointmentTime();

            if (time1 != null && time2 != null && !time1.isEmpty() && !time2.isEmpty()) {
                try {
                    Date timeDate1 = timeFormatter.parse(time1);
                    Date timeDate2 = timeFormatter.parse(time2);
                    if (timeDate1 != null && timeDate2 != null) {
                        return timeDate1.compareTo(timeDate2);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error sorting appointments by time: " + e.getMessage());
                }
            }
            return 0;
        });

        // Setup RecyclerView for appointments in bottom sheet
        UpcomingAppointmentsAdapter bottomSheetAdapter = new UpcomingAppointmentsAdapter(
                sortedAppointments, null, lawyerNamesCache);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setAdapter(bottomSheetAdapter);

        bottomSheetDialog.show();
    }

    // ============ UTILITY METHODS ============

    private void updateMonthDisplay() {
        Calendar currentMonth = customCalendarView.getCurrentMonth();
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCurrentMonth.setText(monthYearFormat.format(currentMonth.getTime()));
    }

    private boolean isDateInPast(String dateStr, String currentDateStr) {
        try {
            Date date = dateFormatter.parse(dateStr);
            Date currentDate = dateFormatter.parse(currentDateStr);

            return date != null && currentDate != null && date.before(currentDate);
        } catch (ParseException e) {
            Log.e(TAG, "Error comparing dates: " + e.getMessage());
            return false;
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        rvUpcomingAppointments.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState(String title, String message) {
        rvUpcomingAppointments.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);

        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    private void showSchedules() {
        rvUpcomingAppointments.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh appointment data when activity resumes
        loadAppointmentSchedules();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (lawyerNamesCache != null) {
            lawyerNamesCache.clear();
        }
        if (processedAppointmentIds != null) {
            processedAppointmentIds.clear();
        }
    }
}