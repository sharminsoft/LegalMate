package com.yourname.legalmate.LawyerPortal.Activities;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yourname.legalmate.GeneralPersonPortal.Models.AppointmentModel;
import com.yourname.legalmate.LawyerPortal.Adapters.UpcomingCasesAdapter;
import com.yourname.legalmate.LawyerPortal.Adapters.UpcomingAppointmentsAdapter;
import com.yourname.legalmate.LawyerPortal.CustomCalendarView;
import com.yourname.legalmate.LawyerPortal.Models.CourtSchedule;
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

public class LawyerCalendarActivity extends AppCompatActivity {

    private static final String TAG = "LawyerCalendarActivity";

    // Calendar Mode Constants
    public static final String MODE_CASES = "cases";
    public static final String MODE_APPOINTMENTS = "appointments";

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // UI Components
    private CustomCalendarView customCalendarView;
    private RecyclerView rvUpcomingItems;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private View loadingOverlay;
    private TextView tvCurrentMonth;
    private TextView tvUpcomingCount;
    private TextView tvModeTitle;
    private TextView tvEmptyMessage;

    // Filter Components
    private ImageButton btnBack, btnFilter;
    private ChipGroup chipGroupMode;
    private Chip chipCases, chipAppointments;

    // Data - Cases
    private List<CourtSchedule> allCourtSchedules;
    private List<CourtSchedule> upcomingCourtSchedules;
    private Map<String, List<CourtSchedule>> dateToCourtSchedulesMap;
    private Set<String> courtScheduledDates;
    private Set<String> courtPastDates;
    private Set<String> processedCaseIds;

    // Data - Appointments
    private List<AppointmentSchedule> allAppointmentSchedules;
    private List<AppointmentSchedule> upcomingAppointmentSchedules;
    private Map<String, List<AppointmentSchedule>> dateToAppointmentSchedulesMap;
    private Set<String> appointmentScheduledDates;
    private Set<String> appointmentPastDates;
    private Map<String, String> clientNamesCache;

    // Adapters
    private UpcomingCasesAdapter upcomingCasesAdapter;
    private UpcomingAppointmentsAdapter upcomingAppointmentsAdapter;

    // Current Mode
    private String currentMode = MODE_CASES;

    // Date formatters
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat displayTimeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calender);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupCalendar();
        setupRecyclerView();
        setupModeFilter();
        loadCurrentModeData();
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
        rvUpcomingItems = findViewById(R.id.rvUpcomingCases); // Reusing the same RecyclerView
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        tvModeTitle = findViewById(R.id.tvModeTitle); // Add this TextView in layout
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage); // Add this TextView in empty state layout

        btnBack = findViewById(R.id.btnBack);
        btnFilter = findViewById(R.id.btnFilter);

        // Filter Components
        chipGroupMode = findViewById(R.id.chipGroupMode); // Add this ChipGroup in layout
        chipCases = findViewById(R.id.chipCases); // Add this Chip in layout
        chipAppointments = findViewById(R.id.chipAppointments); // Add this Chip in layout

        btnBack.setOnClickListener(v -> finish());

        // Initialize data structures for Cases
        allCourtSchedules = new ArrayList<>();
        upcomingCourtSchedules = new ArrayList<>();
        dateToCourtSchedulesMap = new HashMap<>();
        courtScheduledDates = new HashSet<>();
        courtPastDates = new HashSet<>();
        processedCaseIds = new HashSet<>();

        // Initialize data structures for Appointments
        allAppointmentSchedules = new ArrayList<>();
        upcomingAppointmentSchedules = new ArrayList<>();
        dateToAppointmentSchedulesMap = new HashMap<>();
        appointmentScheduledDates = new HashSet<>();
        appointmentPastDates = new HashSet<>();
        clientNamesCache = new HashMap<>();
    }

    private void setupCalendar() {
        // Set calendar to current date
        customCalendarView.setDate(System.currentTimeMillis());
        updateMonthDisplay();

        // Set date selection listener
        customCalendarView.setOnDateSelectedListener((date, year, month, dayOfMonth) -> {
            Log.d(TAG, "Date selected: " + date + " in mode: " + currentMode);
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

    private void setupModeFilter() {
        btnFilter.setOnClickListener(v -> showModeSelectionBottomSheet());

        // Setup chip selection listener (if using chips directly)
        if (chipGroupMode != null) {
            chipGroupMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;

                int checkedId = checkedIds.get(0);
                String newMode = MODE_CASES;

                if (checkedId == R.id.chipAppointments) {
                    newMode = MODE_APPOINTMENTS;
                }

                if (!newMode.equals(currentMode)) {
                    switchMode(newMode);
                }
            });
        }

        // Set initial mode
        updateModeDisplay();
    }

    private void showModeSelectionBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_mode_selection, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Initialize bottom sheet views
        TextView tvTitle = bottomSheetView.findViewById(R.id.tvModeSelectionTitle);
        View layoutCases = bottomSheetView.findViewById(R.id.layoutCasesMode);
        View layoutAppointments = bottomSheetView.findViewById(R.id.layoutAppointmentsMode);
        TextView tvCasesTitle = bottomSheetView.findViewById(R.id.tvCasesTitle);
        TextView tvCasesDesc = bottomSheetView.findViewById(R.id.tvCasesDescription);
        TextView tvAppointmentsTitle = bottomSheetView.findViewById(R.id.tvAppointmentsTitle);
        TextView tvAppointmentsDesc = bottomSheetView.findViewById(R.id.tvAppointmentsDescription);

        // Set content
        tvTitle.setText("Select Calendar Mode");
        tvCasesTitle.setText("Court Cases Schedule");
        tvCasesDesc.setText("View your court hearing dates, case schedules, and upcoming court appearances");
        tvAppointmentsTitle.setText("Client Appointments");
        tvAppointmentsDesc.setText("View your client meeting schedules, consultation appointments, and client visits");

        // Set click listeners
        layoutCases.setOnClickListener(v -> {
            if (!currentMode.equals(MODE_CASES)) {
                switchMode(MODE_CASES);
            }
            bottomSheetDialog.dismiss();
        });

        layoutAppointments.setOnClickListener(v -> {
            if (!currentMode.equals(MODE_APPOINTMENTS)) {
                switchMode(MODE_APPOINTMENTS);
            }
            bottomSheetDialog.dismiss();
        });

        // Highlight current mode
        if (currentMode.equals(MODE_CASES)) {
            layoutCases.setBackgroundResource(R.drawable.selected_mode_background);
        } else {
            layoutAppointments.setBackgroundResource(R.drawable.selected_mode_background);
        }

        bottomSheetDialog.show();
    }

    private void switchMode(String newMode) {
        Log.d(TAG, "Switching mode from " + currentMode + " to " + newMode);

        currentMode = newMode;
        updateModeDisplay();
        setupRecyclerView(); // Re-setup adapter for new mode
        loadCurrentModeData();
    }

    private void updateModeDisplay() {
        if (tvModeTitle != null) {
            if (currentMode.equals(MODE_CASES)) {
                tvModeTitle.setText("Court Cases Calendar");
            } else {
                tvModeTitle.setText("Appointments Calendar");
            }
        }

        // Update chip selection
        if (chipGroupMode != null) {
            if (currentMode.equals(MODE_CASES)) {
                chipCases.setChecked(true);
            } else {
                chipAppointments.setChecked(true);
            }
        }
    }

    private void setupRecyclerView() {
        if (currentMode.equals(MODE_CASES)) {
            // Setup for cases
            upcomingCasesAdapter = new UpcomingCasesAdapter(upcomingCourtSchedules, schedule -> {
                List<CourtSchedule> singleSchedule = new ArrayList<>();
                singleSchedule.add(schedule);
                showCaseDetailsBottomSheet(singleSchedule, schedule.getCourtDate());
            });
            rvUpcomingItems.setAdapter(upcomingCasesAdapter);
        } else {
            // Setup for appointments
            upcomingAppointmentsAdapter = new UpcomingAppointmentsAdapter(upcomingAppointmentSchedules,
                    appointment -> {
                        List<AppointmentSchedule> singleAppointment = new ArrayList<>();
                        singleAppointment.add(appointment);
                        showAppointmentDetailsBottomSheet(singleAppointment, appointment.getAppointmentDate());
                    }, clientNamesCache);
            rvUpcomingItems.setAdapter(upcomingAppointmentsAdapter);
        }

        rvUpcomingItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadCurrentModeData() {
        if (currentMode.equals(MODE_CASES)) {
            loadCourtSchedulesAdvanced();
        } else {
            loadAppointmentSchedulesAdvanced();
        }
    }

    private void handleDateSelection(String date) {
        if (currentMode.equals(MODE_CASES)) {
            if (dateToCourtSchedulesMap.containsKey(date)) {
                List<CourtSchedule> schedulesForDate = dateToCourtSchedulesMap.get(date);
                showCaseDetailsBottomSheet(schedulesForDate, date);
            } else {
                Toast.makeText(this, "No court cases scheduled for this date", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (dateToAppointmentSchedulesMap.containsKey(date)) {
                List<AppointmentSchedule> appointmentsForDate = dateToAppointmentSchedulesMap.get(date);
                showAppointmentDetailsBottomSheet(appointmentsForDate, date);
            } else {
                Toast.makeText(this, "No appointments scheduled for this date", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ============ APPOINTMENTS LOADING METHODS ============

    private void loadAppointmentSchedulesAdvanced() {
        showProgress();
        Log.d(TAG, "Loading appointment schedules for lawyer: " + currentUserId);

        // Clear existing appointment data
        allAppointmentSchedules.clear();
        upcomingAppointmentSchedules.clear();
        dateToAppointmentSchedulesMap.clear();
        appointmentScheduledDates.clear();
        appointmentPastDates.clear();
        clientNamesCache.clear();

        // Get current date for comparison
        String currentDateStr = dateFormatter.format(new Date());

        // Query appointments where lawyer ID matches current user
        db.collection("Appointments")
                .whereEqualTo("lawyerId", currentUserId)
                .whereEqualTo("status", "approved") // Only load approved appointments
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No approved appointments found for lawyer");
                        hideProgress();
                        showEmptyState("No approved appointments found",
                                "Your approved client appointments will appear here once they are scheduled.");
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

        // Validate appointment date
        if (appointment.getDate() == null || appointment.getDate().trim().isEmpty()) {
            int processed = processedAppointments.incrementAndGet();
            Log.d(TAG, "Skipping appointment with no date: " + appointment.getCaseTitle());

            if (processed >= totalAppointments) {
                finishLoadingAppointmentSchedules(currentDateStr);
            }
            return;
        }

        // Create AppointmentSchedule object
        AppointmentSchedule schedule = new AppointmentSchedule();
        schedule.setAppointmentId(appointment.getAppointmentId());
        schedule.setCaseTitle(appointment.getCaseTitle() != null ? appointment.getCaseTitle() : "Consultation");
        schedule.setClientId(appointment.getClientId());
        schedule.setAppointmentDate(appointment.getDate());
        schedule.setAppointmentTime(appointment.getTime() != null ? appointment.getTime() : "");
        schedule.setDescription(appointment.getDescription() != null ? appointment.getDescription() : "");
        schedule.setStatus(appointment.getStatus());

        // Load client name
        loadClientNameForAppointment(schedule, () -> {
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

    private void loadClientNameForAppointment(AppointmentSchedule schedule, Runnable onComplete) {
        if (schedule.getClientId() == null || schedule.getClientId().isEmpty()) {
            onComplete.run();
            return;
        }

        // Check cache first
        if (clientNamesCache.containsKey(schedule.getClientId())) {
            schedule.setClientName(clientNamesCache.get(schedule.getClientId()));
            onComplete.run();
            return;
        }

        // Load from Firestore
        db.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(schedule.getClientId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String clientName = documentSnapshot.getString("name");
                        if (clientName != null && !clientName.isEmpty()) {
                            clientNamesCache.put(schedule.getClientId(), clientName);
                            schedule.setClientName(clientName);
                        } else {
                            // Fallback to email
                            String email = documentSnapshot.getString("email");
                            if (email != null) {
                                clientNamesCache.put(schedule.getClientId(), email);
                                schedule.setClientName(email);
                            } else {
                                schedule.setClientName("Unknown Client");
                            }
                        }
                    } else {
                        schedule.setClientName("Unknown Client");
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading client name: " + e.getMessage());
                    schedule.setClientName("Unknown Client");
                    onComplete.run();
                });
    }

    private void finishLoadingAppointmentSchedules(String currentDateStr) {
        hideProgress();

        Log.d(TAG, "Finishing appointment loading. Total: " + allAppointmentSchedules.size() +
                ", Upcoming: " + upcomingAppointmentSchedules.size());

        if (allAppointmentSchedules.isEmpty()) {
            showEmptyState("No appointments scheduled",
                    "Your approved client appointments will appear here.");
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
            tvUpcomingCount.setText(upcomingAppointmentSchedules.size() + " appointments");

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

    // ============ EXISTING COURT CASES METHODS (Unchanged) ============

    private void loadCourtSchedulesAdvanced() {
        showProgress();

        // Clear existing data
        allCourtSchedules.clear();
        upcomingCourtSchedules.clear();
        dateToCourtSchedulesMap.clear();
        courtScheduledDates.clear();
        courtPastDates.clear();
        processedCaseIds.clear();

        // Calculate extended date range
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.MONTH, -3);

        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 6);

        String startDateStr = dateFormatter.format(startDate.getTime());
        String endDateStr = dateFormatter.format(endDate.getTime());
        String currentDateStr = dateFormatter.format(new Date());

        Log.d(TAG, "Loading court schedules from " + startDateStr + " to " + endDateStr);

        // Query all cases for current user
        CollectionReference casesRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases");

        casesRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No cases found for user");
                        hideProgress();
                        showEmptyState("No court cases found",
                                "Your court hearing schedules will appear here once cases are added.");
                        return;
                    }

                    int totalCases = queryDocumentSnapshots.size();
                    AtomicInteger processedCases = new AtomicInteger(0);
                    AtomicInteger completedCases = new AtomicInteger(0);

                    Log.d(TAG, "Found " + totalCases + " cases to process");

                    for (QueryDocumentSnapshot caseDoc : queryDocumentSnapshots) {
                        String caseId = caseDoc.getId();
                        String caseTitle = caseDoc.getString("caseTitle");
                        String clientName = caseDoc.getString("clientName");
                        String caseType = caseDoc.getString("caseType");
                        String caseStatus = caseDoc.getString("caseStatus");

                        // Skip if case is closed
                        if (caseStatus != null && (caseStatus.equalsIgnoreCase("Closed") ||
                                caseStatus.equalsIgnoreCase("Completed") ||
                                caseStatus.equalsIgnoreCase("Dismissed"))) {
                            int completed = completedCases.incrementAndGet();

                            if (completed >= totalCases) {
                                finishLoadingCourtSchedules(currentDateStr);
                            }
                            continue;
                        }

                        // Load court info for active case
                        loadCourtInfoAdvanced(caseId, caseTitle, clientName, caseType,
                                startDateStr, endDateStr, currentDateStr,
                                processedCases, completedCases, totalCases);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cases: " + e.getMessage());
                    hideProgress();
                    showEmptyState("Failed to load court schedules",
                            "Please check your internet connection and try again.");
                    Toast.makeText(this, "Failed to load court schedules: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // Rest of the existing court loading methods remain the same...
    // (loadCourtInfoAdvanced, isValidCourtDate, loadCompleteScheduleInfo, etc.)
    // I'll include the key ones for completeness:

    private void finishLoadingCourtSchedules(String currentDateStr) {
        hideProgress();

        if (allCourtSchedules.isEmpty()) {
            showEmptyState("No court schedules found",
                    "Your court hearing schedules will appear here.");
        } else {
            showSchedules();

            // Sort and update adapter
            Collections.sort(upcomingCourtSchedules, (s1, s2) -> {
                // Same sorting logic as before
                try {
                    Date date1 = dateFormatter.parse(s1.getCourtDate());
                    Date date2 = dateFormatter.parse(s2.getCourtDate());
                    if (date1 != null && date2 != null) {
                        return date1.compareTo(date2);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error comparing dates: " + e.getMessage());
                }
                return 0;
            });

            if (upcomingCasesAdapter != null) {
                upcomingCasesAdapter.updateData(upcomingCourtSchedules);
            }

            applyCourtCalendarMarkings();
            tvUpcomingCount.setText(upcomingCourtSchedules.size() + " cases");
        }
    }

    private void applyCourtCalendarMarkings() {
        Set<String> upcomingDates = new HashSet<>();
        Set<String> pastDatesSet = new HashSet<>();

        for (String date : courtScheduledDates) {
            if (courtPastDates.contains(date)) {
                pastDatesSet.add(date);
            } else {
                upcomingDates.add(date);
            }
        }

        customCalendarView.setUpcomingDates(upcomingDates);
        customCalendarView.setPastDates(pastDatesSet);
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
                sortedAppointments, null, clientNamesCache);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setAdapter(bottomSheetAdapter);

        bottomSheetDialog.show();
    }

    private void showCaseDetailsBottomSheet(List<CourtSchedule> schedules, String date) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_case_details, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Initialize bottom sheet views
        TextView tvDate = bottomSheetView.findViewById(R.id.tvDate);
        TextView tvCaseCount = bottomSheetView.findViewById(R.id.tvCaseCount);
        RecyclerView rvCases = bottomSheetView.findViewById(R.id.rvCases);

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

        // Set case count
        String countText = schedules.size() == 1 ? "1 case scheduled" : schedules.size() + " cases scheduled";
        tvCaseCount.setText(countText);

        // Sort schedules by time
        List<CourtSchedule> sortedSchedules = new ArrayList<>(schedules);
        Collections.sort(sortedSchedules, (s1, s2) -> {
            String time1 = s1.getCourtTime();
            String time2 = s2.getCourtTime();

            if (time1 != null && time2 != null && !time1.isEmpty() && !time2.isEmpty()) {
                try {
                    Date timeDate1 = timeFormatter.parse(time1);
                    Date timeDate2 = timeFormatter.parse(time2);
                    if (timeDate1 != null && timeDate2 != null) {
                        return timeDate1.compareTo(timeDate2);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error sorting cases by time: " + e.getMessage());
                }
            }
            return 0;
        });

        // Setup RecyclerView for cases in bottom sheet
        UpcomingCasesAdapter bottomSheetAdapter = new UpcomingCasesAdapter(sortedSchedules, null);
        rvCases.setLayoutManager(new LinearLayoutManager(this));
        rvCases.setAdapter(bottomSheetAdapter);

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
        rvUpcomingItems.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState(String title, String message) {
        rvUpcomingItems.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);

        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    private void showSchedules() {
        rvUpcomingItems.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    // ============ REMAINING COURT LOADING METHODS ============

    private void loadCourtInfoAdvanced(String caseId, String caseTitle, String clientName,
                                       String caseType, String startDateStr, String endDateStr,
                                       String currentDateStr, AtomicInteger processedCases,
                                       AtomicInteger completedCases, int totalCases) {

        db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Court Date & Reminder")
                .document("court_info")
                .get()
                .addOnSuccessListener(courtDoc -> {
                    int completed = completedCases.incrementAndGet();

                    if (courtDoc.exists()) {
                        String courtDate = courtDoc.getString("courtDate");
                        String courtTime = courtDoc.getString("courtTime");
                        String hearingType = courtDoc.getString("hearingType");
                        String reminderSettings = courtDoc.getString("reminderSettings");

                        // Validate and process court date
                        if (isValidCourtDate(courtDate, startDateStr, endDateStr)) {
                            processedCases.incrementAndGet();

                            Log.d(TAG, "Processing case: " + caseTitle + " - Date: " + courtDate +
                                    " (" + completed + "/" + totalCases + ")");

                            // Load additional case details
                            loadCompleteScheduleInfo(caseId, caseTitle, clientName, caseType,
                                    courtDate, courtTime, hearingType, reminderSettings, currentDateStr);
                        }
                    }

                    // Check if all cases are processed
                    if (completed >= totalCases) {
                        new android.os.Handler().postDelayed(() -> {
                            finishLoadingCourtSchedules(currentDateStr);
                        }, 500);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading court info for case " + caseId + ": " + e.getMessage());
                    int completed = completedCases.incrementAndGet();

                    if (completed >= totalCases) {
                        new android.os.Handler().postDelayed(() -> {
                            finishLoadingCourtSchedules(currentDateStr);
                        }, 500);
                    }
                });
    }

    private boolean isValidCourtDate(String courtDate, String startDateStr, String endDateStr) {
        if (courtDate == null || courtDate.trim().isEmpty()) {
            return false;
        }

        try {
            Date date = dateFormatter.parse(courtDate);
            Date startDate = dateFormatter.parse(startDateStr);
            Date endDate = dateFormatter.parse(endDateStr);

            return date != null && startDate != null && endDate != null &&
                    (date.equals(startDate) || date.after(startDate)) &&
                    (date.equals(endDate) || date.before(endDate));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing court date: " + courtDate + " - " + e.getMessage());
            return false;
        }
    }

    private void loadCompleteScheduleInfo(String caseId, String caseTitle, String clientName,
                                          String caseType, String courtDate, String courtTime,
                                          String hearingType, String reminderSettings, String currentDateStr) {

        // Check if this case has already been processed to prevent duplicates
        synchronized (this) {
            String uniqueKey = caseId + "_" + courtDate;
            if (processedCaseIds.contains(uniqueKey)) {
                Log.d(TAG, "Case already processed, skipping: " + caseTitle + " on " + courtDate);
                return;
            }
            processedCaseIds.add(uniqueKey);
        }

        // Load case details for complete information
        db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Case Details")
                .document("case_info")
                .get()
                .addOnSuccessListener(caseDoc -> {
                    String courtName = "";
                    String courtLocation = "";
                    String judgeName = "";
                    String caseNumber = "";

                    if (caseDoc.exists()) {
                        courtName = caseDoc.getString("courtName");
                        courtLocation = caseDoc.getString("courtLocation");
                        judgeName = caseDoc.getString("judgeName");
                        caseNumber = caseDoc.getString("caseNumber");
                    }

                    // Create comprehensive CourtSchedule object
                    CourtSchedule schedule = createAdvancedSchedule(caseId, caseTitle, clientName, caseType,
                            courtDate, courtTime, courtName, courtLocation, judgeName,
                            hearingType, reminderSettings, caseNumber);

                    // Add to appropriate collections with thread safety
                    addScheduleToCollections(schedule, currentDateStr);

                    Log.d(TAG, "Added advanced schedule: " + caseTitle + " on " + courtDate);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading complete case details: " + e.getMessage());

                    // Create basic schedule even if case details fail
                    CourtSchedule basicSchedule = createBasicSchedule(caseId, caseTitle, clientName,
                            caseType, courtDate, courtTime, hearingType, reminderSettings);

                    addScheduleToCollections(basicSchedule, currentDateStr);
                });
    }

    private void addScheduleToCollections(CourtSchedule schedule, String currentDateStr) {
        synchronized (this) {
            String courtDate = schedule.getCourtDate();

            allCourtSchedules.add(schedule);
            courtScheduledDates.add(courtDate);

            // Determine if date is past or future
            if (isDateInPast(courtDate, currentDateStr)) {
                courtPastDates.add(courtDate);
            } else {
                upcomingCourtSchedules.add(schedule);
            }

            // Add to date mapping
            if (!dateToCourtSchedulesMap.containsKey(courtDate)) {
                dateToCourtSchedulesMap.put(courtDate, new ArrayList<>());
            }
            dateToCourtSchedulesMap.get(courtDate).add(schedule);
        }
    }

    private CourtSchedule createAdvancedSchedule(String caseId, String caseTitle, String clientName,
                                                 String caseType, String courtDate, String courtTime,
                                                 String courtName, String courtLocation, String judgeName,
                                                 String hearingType, String reminderSettings, String caseNumber) {
        CourtSchedule schedule = new CourtSchedule();
        schedule.setCaseId(caseId);
        schedule.setCaseTitle(caseTitle != null ? caseTitle : "Untitled Case");
        schedule.setClientName(clientName != null ? clientName : "Unknown Client");
        schedule.setCaseType(caseType != null ? caseType : "General");
        schedule.setCourtDate(courtDate);
        schedule.setCourtTime(courtTime != null ? courtTime : "");
        schedule.setCourtName(courtName != null ? courtName : "");
        schedule.setCourtLocation(courtLocation != null ? courtLocation : "");
        schedule.setJudgeName(judgeName != null ? judgeName : "");
        schedule.setHearingType(hearingType != null ? hearingType : "General Hearing");
        schedule.setReminderSettings(reminderSettings != null ? reminderSettings : "");

        return schedule;
    }

    private CourtSchedule createBasicSchedule(String caseId, String caseTitle, String clientName,
                                              String caseType, String courtDate, String courtTime,
                                              String hearingType, String reminderSettings) {
        CourtSchedule schedule = new CourtSchedule();
        schedule.setCaseId(caseId);
        schedule.setCaseTitle(caseTitle != null ? caseTitle : "Untitled Case");
        schedule.setClientName(clientName != null ? clientName : "Unknown Client");
        schedule.setCaseType(caseType != null ? caseType : "General");
        schedule.setCourtDate(courtDate);
        schedule.setCourtTime(courtTime != null ? courtTime : "");
        schedule.setHearingType(hearingType != null ? hearingType : "General Hearing");
        schedule.setReminderSettings(reminderSettings != null ? reminderSettings : "");

        return schedule;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh current mode data when activity resumes
        loadCurrentModeData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (clientNamesCache != null) {
            clientNamesCache.clear();
        }
        if (processedCaseIds != null) {
            processedCaseIds.clear();
        }
    }
}