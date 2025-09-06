package com.yourname.legalmate.LawyerPortal.Fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yourname.legalmate.GeneralPersonPortal.Models.AppointmentModel;
import com.yourname.legalmate.LawyerPortal.Activities.AddCaseInfoActivity;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerAppointmentActivity;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerCalendarActivity;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerDocumentsActivity;
import com.yourname.legalmate.LawyerPortal.Adapters.TodaySchedulesAdapter;
import com.yourname.legalmate.LawyerPortal.Models.TodayScheduleItem;
import com.yourname.legalmate.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class LawyerHomeFragment extends Fragment {

    private static final String TAG = "LawyerHomeFragment";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // Header Views
    private LinearLayout ll_header;
    private ImageView iv_profile_picture;
    private LinearLayout ll_greeting_container;
    private TextView app_bar_title;
    private ImageView iv_notification;
    private TextView tv_greeting;

    // Today's Appointments Card Views
    private CardView cv_todays_appointments;
    private LinearLayout ll_appointments_content;
    private TextView tv_appointments_title;
    private RecyclerView rv_today_schedules;
    private TextView tv_no_schedules_today;
    private ImageView iv_lady_justice;

    // Statistics Overview Card Views
    private CardView cv_statistics_overview;
    private LinearLayout ll_total_clients_stat;
    private TextView tv_total_clients_count;
    private LinearLayout ll_ongoing_cases_stat;
    private TextView tv_ongoing_cases_count;
    private LinearLayout ll_closed_cases_stat;
    private TextView tv_closed_cases_count;
    private LinearLayout ll_upcoming_appointments_stat;
    private TextView tv_upcoming_appointments_count;

    // Quick Access Section Views
    private TextView tv_quick_access_title;
    private LinearLayout ll_quick_access_container;

    // First Row Quick Access
    private LinearLayout ll_quick_access_row_1;
    private CardView cv_add_case;
    private LinearLayout ll_add_case_content;
    private ImageView iv_add_case_icon;
    private TextView tv_add_case_text;

    private CardView cv_view_calendar;
    private LinearLayout ll_view_calendar_content;
    private ImageView iv_view_calendar_icon;
    private TextView tv_view_calendar_text;

    // Second Row Quick Access
    private LinearLayout ll_quick_access_row_2;
    private CardView cv_book_and_appoinment;
    private LinearLayout ll_case_history_content;
    private ImageView iv_book_and_appoinment_icon;
    private TextView tv_book_appointment_text;

    private CardView cv_manage_documents;
    private LinearLayout ll_manage_documents_content;
    private ImageView iv_manage_documents_icon;
    private TextView tv_manage_documents_text;

    // Recent Activity Section Views
    private TextView tv_recent_activity_title;
    private RecyclerView rv_recent_activity;

    // Today's Schedules Data
    private List<TodayScheduleItem> allTodaySchedules;
    private List<TodayScheduleItem> displaySchedules; // Max 3 for preview
    private TodaySchedulesAdapter todaySchedulesAdapter;
    private Map<String, String> clientNamesCache;

    // Date formatters
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat displayTimeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lawyer_home, container, false);


        TodaySchedulesAdapter.viewName="Home";

        // Initialize Firebase
        initializeFirebase();

        // Initialize all views
        initializeViews(view);

        // Initialize today's schedules data
        initializeTodaySchedulesData();

        // Setup RecyclerView for today's schedules
        setupTodaySchedulesRecyclerView();

        // Setup click listeners for Quick Access cards
        setupQuickAccessClickListeners();

        // Load lawyer profile data
        loadLawyerProfileData();

        // Load today's schedules
        loadTodaySchedules();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (userId.isEmpty()) {
            Log.e(TAG, "User not authenticated");
            if (getContext() != null) {
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeViews(View view) {
        // Header Views
        ll_header = view.findViewById(R.id.ll_header);
        iv_profile_picture = view.findViewById(R.id.iv_profile_picture);
        ll_greeting_container = view.findViewById(R.id.ll_greeting_container);
        app_bar_title = view.findViewById(R.id.app_bar_title);
        iv_notification = view.findViewById(R.id.iv_notification);
        tv_greeting = view.findViewById(R.id.tv_greeting);

        // Today's Appointments Card Views
        cv_todays_appointments = view.findViewById(R.id.cv_todays_appointments);
        ll_appointments_content = view.findViewById(R.id.ll_appointments_content);
        tv_appointments_title = view.findViewById(R.id.tv_appointments_title);
        rv_today_schedules = view.findViewById(R.id.rv_today_schedules);
        tv_no_schedules_today = view.findViewById(R.id.tv_no_schedules_today);
        iv_lady_justice = view.findViewById(R.id.iv_lady_justice);

        // Statistics Overview Card Views
        cv_statistics_overview = view.findViewById(R.id.cv_statistics_overview);
        ll_total_clients_stat = view.findViewById(R.id.ll_total_clients_stat);
        tv_total_clients_count = view.findViewById(R.id.tv_total_clients_count);
        ll_ongoing_cases_stat = view.findViewById(R.id.ll_ongoing_cases_stat);
        tv_ongoing_cases_count = view.findViewById(R.id.tv_ongoing_cases_count);
        ll_closed_cases_stat = view.findViewById(R.id.ll_closed_cases_stat);
        tv_closed_cases_count = view.findViewById(R.id.tv_closed_cases_count);
        ll_upcoming_appointments_stat = view.findViewById(R.id.ll_upcoming_appointments_stat);
        tv_upcoming_appointments_count = view.findViewById(R.id.tv_upcoming_appointments_count);

        // Quick Access Section Views
        tv_quick_access_title = view.findViewById(R.id.tv_quick_access_title);
        ll_quick_access_container = view.findViewById(R.id.ll_quick_access_container);

        // First Row Quick Access
        ll_quick_access_row_1 = view.findViewById(R.id.ll_quick_access_row_1);
        cv_add_case = view.findViewById(R.id.cv_add_case);
        ll_add_case_content = view.findViewById(R.id.ll_add_case_content);
        iv_add_case_icon = view.findViewById(R.id.iv_add_case_icon);
        tv_add_case_text = view.findViewById(R.id.tv_add_case_text);

        cv_view_calendar = view.findViewById(R.id.cv_view_calendar);
        ll_view_calendar_content = view.findViewById(R.id.ll_view_calendar_content);
        iv_view_calendar_icon = view.findViewById(R.id.iv_view_calendar_icon);
        tv_view_calendar_text = view.findViewById(R.id.tv_view_calendar_text);

        // Second Row Quick Access
        ll_quick_access_row_2 = view.findViewById(R.id.ll_quick_access_row_2);
        cv_book_and_appoinment = view.findViewById(R.id.cv_book_and_appoinment);
        ll_case_history_content = view.findViewById(R.id.ll_case_history_content);
        iv_book_and_appoinment_icon = view.findViewById(R.id.iv_book_and_appoinment_icon);
        tv_book_appointment_text = view.findViewById(R.id.tv_book_appointment_text);

        cv_manage_documents = view.findViewById(R.id.cv_manage_documents);
        ll_manage_documents_content = view.findViewById(R.id.ll_manage_documents_content);
        iv_manage_documents_icon = view.findViewById(R.id.iv_manage_documents_icon);
        tv_manage_documents_text = view.findViewById(R.id.tv_manage_documents_text);

        // Recent Activity Section Views
        tv_recent_activity_title = view.findViewById(R.id.tv_recent_activity_title);
        rv_recent_activity = view.findViewById(R.id.rv_recent_activity);
    }

    private void initializeTodaySchedulesData() {
        allTodaySchedules = new ArrayList<>();
        displaySchedules = new ArrayList<>();
        clientNamesCache = new HashMap<>();
    }

    private void setupTodaySchedulesRecyclerView() {
        todaySchedulesAdapter = new TodaySchedulesAdapter(displaySchedules, getContext());
        rv_today_schedules.setLayoutManager(new LinearLayoutManager(getContext()));
        rv_today_schedules.setAdapter(todaySchedulesAdapter);
        rv_today_schedules.setNestedScrollingEnabled(false);
    }

    private void loadTodaySchedules() {
        Log.d(TAG, "Loading today's schedules for user: " + userId);

        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot load schedules: User ID is empty");
            return;
        }

        // Clear existing data completely and immediately update UI
        allTodaySchedules.clear();
        displaySchedules.clear();
        clientNamesCache.clear();

        // Immediately update UI to show empty state while loading
        if (todaySchedulesAdapter != null && getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                rv_today_schedules.setVisibility(View.GONE);
                tv_no_schedules_today.setVisibility(View.VISIBLE);
                tv_no_schedules_today.setText("Loading schedules...");
                todaySchedulesAdapter.notifyDataSetChanged();
            });
        }

        // Get today's date
        String todayDate = dateFormatter.format(new Date());
        Log.d(TAG, "Today's date: " + todayDate);

        // Load both court cases and appointments
        AtomicInteger completedRequests = new AtomicInteger(0);
        int totalRequests = 2;

        // Load court cases for today
        loadTodayCourtCases(todayDate, () -> {
            if (completedRequests.incrementAndGet() >= totalRequests) {
                // Add a small delay to ensure all async operations are completed
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> finishLoadingTodaySchedules());
                }
            }
        });

        // Load appointments for today
        loadTodayAppointments(todayDate, () -> {
            if (completedRequests.incrementAndGet() >= totalRequests) {
                // Add a small delay to ensure all async operations are completed
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> finishLoadingTodaySchedules());
                }
            }
        });
    }

    private void loadTodayCourtCases(String todayDate, Runnable onComplete) {
        Log.d(TAG, "Loading today's court cases for date: " + todayDate);

        db.collection("All Cases")
                .document(userId)
                .collection("Cases")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No cases found for user");
                        onComplete.run();
                        return;
                    }

                    // Create a temporary list to collect court cases for today
                    List<TodayScheduleItem> todayCourtCases = new ArrayList<>();
                    AtomicInteger processedCases = new AtomicInteger(0);
                    int totalCases = queryDocumentSnapshots.size();

                    for (QueryDocumentSnapshot caseDoc : queryDocumentSnapshots) {
                        String caseId = caseDoc.getId();
                        String caseTitle = caseDoc.getString("caseTitle");
                        String clientName = caseDoc.getString("clientName");
                        String caseStatus = caseDoc.getString("caseStatus");

                        // Skip closed cases
                        if (caseStatus != null && (caseStatus.equalsIgnoreCase("Closed") ||
                                caseStatus.equalsIgnoreCase("Completed") ||
                                caseStatus.equalsIgnoreCase("Dismissed"))) {
                            if (processedCases.incrementAndGet() >= totalCases) {
                                // Add all court cases to main list at once
                                synchronized (allTodaySchedules) {
                                    allTodaySchedules.addAll(todayCourtCases);
                                }
                                onComplete.run();
                            }
                            continue;
                        }

                        // Check court date for this case
                        db.collection("All Cases")
                                .document(userId)
                                .collection("Cases")
                                .document(caseId)
                                .collection("Court Date & Reminder")
                                .document("court_info")
                                .get()
                                .addOnSuccessListener(courtDoc -> {
                                    if (courtDoc.exists()) {
                                        String courtDate = courtDoc.getString("courtDate");
                                        String courtTime = courtDoc.getString("courtTime");

                                        if (todayDate.equals(courtDate)) {
                                            // This is a court case for today
                                            TodayScheduleItem scheduleItem = new TodayScheduleItem();
                                            scheduleItem.setType(TodayScheduleItem.TYPE_COURT_CASE);
                                            scheduleItem.setTitle(caseTitle != null ? caseTitle : "Court Case");
                                            scheduleItem.setSubtitle(clientName != null ? clientName : "Unknown Client");
                                            scheduleItem.setTime(formatTime(courtTime));
                                            scheduleItem.setDate(courtDate);

                                            // Add to temporary list instead of main list directly
                                            todayCourtCases.add(scheduleItem);
                                            Log.d(TAG, "Added court case for today: " + caseTitle);
                                        }
                                    }

                                    if (processedCases.incrementAndGet() >= totalCases) {
                                        // Add all court cases to main list at once
                                        synchronized (allTodaySchedules) {
                                            allTodaySchedules.addAll(todayCourtCases);
                                        }
                                        onComplete.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading court info: " + e.getMessage());
                                    if (processedCases.incrementAndGet() >= totalCases) {
                                        // Add all court cases to main list at once
                                        synchronized (allTodaySchedules) {
                                            allTodaySchedules.addAll(todayCourtCases);
                                        }
                                        onComplete.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cases: " + e.getMessage());
                    onComplete.run();
                });
    }

    private void loadTodayAppointments(String todayDate, Runnable onComplete) {
        Log.d(TAG, "Loading today's appointments for date: " + todayDate);

        db.collection("Appointments")
                .whereEqualTo("lawyerId", userId)
                .whereEqualTo("status", "approved")
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No appointments found for today");
                        onComplete.run();
                        return;
                    }

                    // Create a temporary list to collect appointments for today
                    List<TodayScheduleItem> todayAppointments = new ArrayList<>();
                    AtomicInteger processedAppointments = new AtomicInteger(0);
                    int totalAppointments = queryDocumentSnapshots.size();

                    for (DocumentSnapshot appointmentDoc : queryDocumentSnapshots.getDocuments()) {
                        AppointmentModel appointment = appointmentDoc.toObject(AppointmentModel.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(appointmentDoc.getId());

                            // Create schedule item for appointment
                            TodayScheduleItem scheduleItem = new TodayScheduleItem();
                            scheduleItem.setType(TodayScheduleItem.TYPE_APPOINTMENT);
                            scheduleItem.setTitle(appointment.getCaseTitle() != null ?
                                    appointment.getCaseTitle() : "Client Appointment");
                            scheduleItem.setTime(formatTime(appointment.getTime()));
                            scheduleItem.setDate(appointment.getDate());
                            scheduleItem.setClientId(appointment.getClientId());

                            // Load client name
                            loadClientName(appointment.getClientId(), clientName -> {
                                scheduleItem.setSubtitle(clientName);

                                // Add to temporary list
                                todayAppointments.add(scheduleItem);
                                Log.d(TAG, "Added appointment for today: " + appointment.getCaseTitle());

                                if (processedAppointments.incrementAndGet() >= totalAppointments) {
                                    // Add all appointments to main list at once
                                    synchronized (allTodaySchedules) {
                                        allTodaySchedules.addAll(todayAppointments);
                                    }
                                    onComplete.run();
                                }
                            });
                        } else {
                            if (processedAppointments.incrementAndGet() >= totalAppointments) {
                                // Add all appointments to main list at once
                                synchronized (allTodaySchedules) {
                                    allTodaySchedules.addAll(todayAppointments);
                                }
                                onComplete.run();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading appointments: " + e.getMessage());
                    onComplete.run();
                });
    }

    private void loadClientName(String clientId, ClientNameCallback callback) {
        if (clientId == null || clientId.isEmpty()) {
            callback.onClientNameLoaded("Unknown Client");
            return;
        }

        // Check cache first
        if (clientNamesCache.containsKey(clientId)) {
            callback.onClientNameLoaded(clientNamesCache.get(clientId));
            return;
        }

        // Load from Firestore
        db.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(clientId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String clientName = "Unknown Client";
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            clientName = name;
                        } else {
                            String email = documentSnapshot.getString("email");
                            if (email != null) {
                                clientName = email;
                            }
                        }
                    }

                    clientNamesCache.put(clientId, clientName);
                    callback.onClientNameLoaded(clientName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading client name: " + e.getMessage());
                    callback.onClientNameLoaded("Unknown Client");
                });
    }

    private interface ClientNameCallback {
        void onClientNameLoaded(String clientName);
    }

    private void finishLoadingTodaySchedules() {
        if (getActivity() == null || !isAdded()) {
            return;
        }

        Log.d(TAG, "Finishing today's schedules loading. Total: " + allTodaySchedules.size());

        // Remove duplicates based on title, subtitle, and time combination
        List<TodayScheduleItem> uniqueSchedules = removeDuplicateSchedules(allTodaySchedules);

        // Replace allTodaySchedules with unique list
        allTodaySchedules.clear();
        allTodaySchedules.addAll(uniqueSchedules);

        if (allTodaySchedules.isEmpty()) {
            // Show empty state
            rv_today_schedules.setVisibility(View.GONE);
            tv_no_schedules_today.setVisibility(View.VISIBLE);
            tv_no_schedules_today.setText("No schedules for today");
        } else {
            // Sort schedules by time
            Collections.sort(allTodaySchedules, (s1, s2) -> {
                String time1 = s1.getTime();
                String time2 = s2.getTime();

                if (time1 != null && time2 != null && !time1.isEmpty() && !time2.isEmpty()) {
                    try {
                        Date timeDate1 = timeFormatter.parse(time1);
                        Date timeDate2 = timeFormatter.parse(time2);
                        if (timeDate1 != null && timeDate2 != null) {
                            return timeDate1.compareTo(timeDate2);
                        }
                    } catch (ParseException e) {
                        return time1.compareTo(time2);
                    }
                }
                return 0;
            });

            // Clear and populate display list
            displaySchedules.clear();
            int maxItems = Math.min(3, allTodaySchedules.size());
            for (int i = 0; i < maxItems; i++) {
                displaySchedules.add(allTodaySchedules.get(i));
            }

            // Update UI
            rv_today_schedules.setVisibility(View.VISIBLE);
            tv_no_schedules_today.setVisibility(View.GONE);

            // Update appointment title with count
            if (allTodaySchedules.size() > 3) {
                tv_appointments_title.setText("Today's Schedule (" + allTodaySchedules.size() + " total)");
            } else {
                tv_appointments_title.setText("Today's Schedule");
            }
        }

        // Always notify adapter at the end
        if (todaySchedulesAdapter != null) {
            todaySchedulesAdapter.notifyDataSetChanged();
        }
    }

    private List<TodayScheduleItem> removeDuplicateSchedules(List<TodayScheduleItem> schedules) {
        List<TodayScheduleItem> uniqueList = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        for (TodayScheduleItem schedule : schedules) {
            // Create unique key based on title, subtitle, time, and type
            String uniqueKey = (schedule.getTitle() != null ? schedule.getTitle() : "") +
                    "_" + (schedule.getSubtitle() != null ? schedule.getSubtitle() : "") +
                    "_" + (schedule.getTime() != null ? schedule.getTime() : "") +
                    "_" + schedule.getType();

            if (!uniqueKeys.contains(uniqueKey)) {
                uniqueKeys.add(uniqueKey);
                uniqueList.add(schedule);
            } else {
                Log.d(TAG, "Duplicate schedule removed: " + schedule.getTitle());
            }
        }

        return uniqueList;
    }

    private String formatTime(String time) {
        if (time == null || time.isEmpty()) {
            return "";
        }

        try {
            Date timeDate = timeFormatter.parse(time);
            if (timeDate != null) {
                return displayTimeFormatter.format(timeDate);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting time: " + e.getMessage());
        }

        return time; // Return original if formatting fails
    }

    private void showTodaySchedulesBottomSheet() {
        if (allTodaySchedules.isEmpty()) {
            Toast.makeText(getContext(), "No schedules for today", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_today_schedules, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Initialize bottom sheet views
        TextView tvDate = bottomSheetView.findViewById(R.id.tvTodayDate);
        TextView tvScheduleCount = bottomSheetView.findViewById(R.id.tvScheduleCount);
        RecyclerView rvAllSchedules = bottomSheetView.findViewById(R.id.rvTodaySchedules);

        // Set date
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText("Today - " + todayDate);

        // Set schedule count
        String countText = allTodaySchedules.size() == 1 ?
                "1 schedule" : allTodaySchedules.size() + " schedules";
        tvScheduleCount.setText(countText);

        // Setup RecyclerView with all schedules
        TodaySchedulesAdapter bottomSheetAdapter = new TodaySchedulesAdapter(allTodaySchedules, getContext());
        TodaySchedulesAdapter.viewName = "bottomSheet";
        rvAllSchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllSchedules.setAdapter(bottomSheetAdapter);

        bottomSheetDialog.show();
    }

    private void setupQuickAccessClickListeners() {
        // Add New Case Card Click Listener
        cv_add_case.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCaseInfoActivity.class);
            startActivity(intent);
        });

        // View Calendar Card Click Listener
        cv_view_calendar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LawyerCalendarActivity.class);
            startActivity(intent);
        });

        // Book Appointment Card Click Listener
        cv_book_and_appoinment.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LawyerAppointmentActivity.class);
            startActivity(intent);
        });

        // Manage Documents Card Click Listener
        cv_manage_documents.setOnClickListener(v -> {

            Intent intent = new Intent(getActivity(), LawyerDocumentsActivity.class);
            startActivity(intent);
        });

        // Statistics Click Listeners
        ll_total_clients_stat.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Total Clients: " + (tv_total_clients_count != null ? tv_total_clients_count.getText() : "125"), Toast.LENGTH_SHORT).show();
        });

        ll_ongoing_cases_stat.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Ongoing Cases: " + (tv_ongoing_cases_count != null ? tv_ongoing_cases_count.getText() : "35"), Toast.LENGTH_SHORT).show();
        });

        ll_closed_cases_stat.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Closed Cases: " + (tv_closed_cases_count != null ? tv_closed_cases_count.getText() : "90"), Toast.LENGTH_SHORT).show();
        });

        ll_upcoming_appointments_stat.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Upcoming Appointments: " + (tv_upcoming_appointments_count != null ? tv_upcoming_appointments_count.getText() : "5"), Toast.LENGTH_SHORT).show();
        });

        // Today's Appointments Card Click Listener - Show Bottom Sheet
        cv_todays_appointments.setOnClickListener(v -> {
            showTodaySchedulesBottomSheet();
        });

        ll_appointments_content.setOnClickListener(v -> {
            showTodaySchedulesBottomSheet();
        });

        // Notification icon click listener
        iv_notification.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notifications clicked", Toast.LENGTH_SHORT).show();
        });

        // Profile picture click listener
        iv_profile_picture.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Profile clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadLawyerProfileData() {
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot load profile data: User ID is empty");
            return;
        }

        Log.d(TAG, "Loading profile data for user: " + userId);

        // Create a map to store the retrieved data
        Map<String, Object> lawyerData = new HashMap<>();

        // Counter to track completed requests
        AtomicInteger completedRequests = new AtomicInteger(0);
        int totalRequests = 2; // We're fetching 2 documents

        // Get Basic Profile Settings (contains full name)
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("BasicProfileSettings")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        lawyerData.put("fullName", fullName);
                        Log.d(TAG, "Full Name retrieved: " + fullName);
                    } else {
                        Log.w(TAG, "BasicProfileSettings document does not exist");
                    }

                    // Check if all requests are completed
                    if (completedRequests.incrementAndGet() == totalRequests) {
                        updateUIWithProfileData(lawyerData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting basic profile: ", e);
                    if (completedRequests.incrementAndGet() == totalRequests) {
                        updateUIWithProfileData(lawyerData);
                    }
                });

        // Get Documents (contains profile image URL)
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("Documents")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        lawyerData.put("profileImageUrl", profileImageUrl);
                        Log.d(TAG, "Profile Image URL retrieved: " + profileImageUrl);
                    } else {
                        Log.w(TAG, "Documents document does not exist");
                    }

                    // Check if all requests are completed
                    if (completedRequests.incrementAndGet() == totalRequests) {
                        updateUIWithProfileData(lawyerData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting documents: ", e);
                    if (completedRequests.incrementAndGet() == totalRequests) {
                        updateUIWithProfileData(lawyerData);
                    }
                });
    }

    private void updateUIWithProfileData(Map<String, Object> lawyerData) {
        if (getActivity() == null || !isAdded()) {
            Log.w(TAG, "Fragment not attached, skipping UI update");
            return;
        }

        // Update profile image
        String profileImageUrl = (String) lawyerData.get("profileImageUrl");
        if (profileImageUrl != null && !profileImageUrl.isEmpty() && iv_profile_picture != null) {
            Log.d(TAG, "Loading profile image: " + profileImageUrl);
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .centerCrop()
                    .into(iv_profile_picture);
        } else {
            Log.d(TAG, "No profile image URL available, using default");
            if (iv_profile_picture != null) {
                iv_profile_picture.setImageResource(R.drawable.ic_profile);
            }
        }

        // Update greeting with lawyer name
        String fullName = (String) lawyerData.get("fullName");
        if (fullName != null && !fullName.isEmpty() && tv_greeting != null) {
            // Extract first name or use full name
            String displayName = getFirstName(fullName);
            String greetingMessage = getGreetingMessage() + ", " + displayName;
            tv_greeting.setText(greetingMessage);
            Log.d(TAG, "Updated greeting: " + greetingMessage);
        } else {
            Log.d(TAG, "No full name available, using default greeting");
            if (tv_greeting != null) {
                tv_greeting.setText(getGreetingMessage() + ", Counselor");
            }
        }
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Counselor";
        }

        String[] nameParts = fullName.trim().split("\\s+");
        return nameParts[0]; // Return first part of the name
    }

    private String getGreetingMessage() {
        // Get current hour to determine appropriate greeting
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            return "Good Morning";
        } else if (hour < 17) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }

    // Method to update statistics dynamically
    public void updateStatistics(int totalClients, int ongoingCases, int closedCases, int upcomingAppointments) {
        if (tv_total_clients_count != null) {
            tv_total_clients_count.setText(String.valueOf(totalClients));
        }
        if (tv_ongoing_cases_count != null) {
            tv_ongoing_cases_count.setText(String.valueOf(ongoingCases));
        }
        if (tv_closed_cases_count != null) {
            tv_closed_cases_count.setText(String.valueOf(closedCases));
        }
        if (tv_upcoming_appointments_count != null) {
            tv_upcoming_appointments_count.setText(String.valueOf(upcomingAppointments));
        }
    }

    // Method to manually refresh profile data and schedules
    public void refreshProfileData() {
        loadLawyerProfileData();
        loadTodaySchedules();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data and today's schedules when fragment resumes
        loadLawyerProfileData();
        loadTodaySchedules();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (clientNamesCache != null) {
            clientNamesCache.clear();
        }
        if (allTodaySchedules != null) {
            allTodaySchedules.clear();
        }
        if (displaySchedules != null) {
            displaySchedules.clear();
        }
    }
}