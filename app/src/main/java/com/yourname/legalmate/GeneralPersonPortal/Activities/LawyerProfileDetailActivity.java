package com.yourname.legalmate.GeneralPersonPortal.Activities;

import static androidx.core.graphics.drawable.DrawableCompat.setTint;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.Chat.ChatActivity;
import com.yourname.legalmate.GeneralPersonPortal.Models.LawyerModel;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LawyerProfileDetailActivity extends AppCompatActivity {

    private static final String TAG = "LawyerProfileDetail";

    // UI Components
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private CircleImageView ivProfileImage;
    private TextView tvLawyerName, tvChamberName, tvRating, tvReviewCount;
    private RatingBar ratingBar;
    private MaterialButton btnMessage, btnCall, btnBookAppointment;
    private FloatingActionButton fabFavorite;
    private ImageButton btnDirections;
    private ProgressBar progressBar;

    // Info TextViews
    private TextView tvExperienceCount, tvConsultationFee, tvAvailabilityStatus;
    private TextView tvShortBio, tvBarRegistration, tvEducation, tvLanguages;
    private TextView tvOfficeAddress, tvWorkingHours;
    private ImageView ivAvailabilityIcon;

    // Layouts - Changed from FlexboxLayout to LinearLayout
    private LinearLayout layoutPracticeAreas;
    private LinearLayout layoutConsultationTypes;

    // Data
    private FirebaseFirestore db;
    FirebaseAuth auth;
    private Calendar selectedDate;
    private String selectedTime;

    private String lawyerId;
    private LawyerModel currentLawyer;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lawyer_profile_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupToolbar();
        setupClickListeners();
        getLawyerIdFromIntent();
        loadLawyerProfile();
    }

    private void initializeComponents() {
        // Initialize Firebase

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvLawyerName = findViewById(R.id.tvLawyerName);
        tvChamberName = findViewById(R.id.tvChamberName);
        ratingBar = findViewById(R.id.ratingBar);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        progressBar = findViewById(R.id.progressBar);

        // Action buttons
        btnMessage = findViewById(R.id.btnMessage);
        btnCall = findViewById(R.id.btnCall);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        fabFavorite = findViewById(R.id.fabFavorite);
        btnDirections = findViewById(R.id.btnDirections);

        // Info components
        tvExperienceCount = findViewById(R.id.tvExperienceCount);
        tvConsultationFee = findViewById(R.id.tvConsultationFee);
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus);
        ivAvailabilityIcon = findViewById(R.id.ivAvailabilityIcon);
        tvShortBio = findViewById(R.id.tvShortBio);
        tvBarRegistration = findViewById(R.id.tvBarRegistration);
        tvEducation = findViewById(R.id.tvEducation);
        tvLanguages = findViewById(R.id.tvLanguages);
        tvOfficeAddress = findViewById(R.id.tvOfficeAddress);
        tvWorkingHours = findViewById(R.id.tvWorkingHours);

        // Layouts - Updated to use LinearLayout instead of FlexboxLayout
        layoutPracticeAreas = findViewById(R.id.layoutPracticeAreas);
        layoutConsultationTypes = findViewById(R.id.layoutConsultationTypes);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupClickListeners() {
        btnMessage.setOnClickListener(v -> openChat());
        btnCall.setOnClickListener(v -> makePhoneCall());
        btnBookAppointment.setOnClickListener(v -> bookAppointment());
        fabFavorite.setOnClickListener(v -> toggleFavorite());
        btnDirections.setOnClickListener(v -> openDirections());
    }

    private void getLawyerIdFromIntent() {
        lawyerId = getIntent().getStringExtra("lawyer_id");
        if (TextUtils.isEmpty(lawyerId)) {
            showToast("Invalid lawyer information");
            finish();
        }
    }

    private void loadLawyerProfile() {
        showLoading(true);

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(lawyerId)
                .collection("ProfileData")
                .get()
                .addOnSuccessListener(profileSnapshots -> {
                    currentLawyer = buildLawyerFromProfile(lawyerId, profileSnapshots.getDocuments());

                    if (currentLawyer != null) {
                        populateUI();
                    } else {
                        showToast("Failed to load lawyer profile");
                        finish();
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer profile", e);
                    showToast("Failed to load lawyer profile");
                    showLoading(false);
                    finish();
                });
    }

    private LawyerModel buildLawyerFromProfile(String lawyerId, List<DocumentSnapshot> profileDocs) {
        LawyerModel lawyer = new LawyerModel();
        lawyer.setLawyerId(lawyerId);

        try {
            for (DocumentSnapshot doc : profileDocs) {
                String sectionName = doc.getId();

                switch (sectionName) {
                    case "BasicProfileSettings":
                        buildBasicProfile(lawyer, doc);
                        break;
                    case "ContactLocationSettings":
                        buildContactLocation(lawyer, doc);
                        break;
                    case "ProfessionalInformation":
                        buildProfessionalInfo(lawyer, doc);
                        break;
                    case "ConsultationFees":
                        buildConsultationFees(lawyer, doc);
                        break;
                    case "Documents":
                        buildDocuments(lawyer, doc);
                        break;
                    case "ProfileStatus":
                        buildProfileStatus(lawyer, doc);
                        break;
                    case "AppointmentSettings":
                        buildAppointmentSettings(lawyer, doc);
                        break;
                }
            }

            return lawyer;
        } catch (Exception e) {
            Log.e(TAG, "Error building lawyer profile", e);
            return null;
        }
    }

    private void buildBasicProfile(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setFullName(doc.getString("fullName"));
        lawyer.setDateOfBirth(doc.getString("dateOfBirth"));
        lawyer.setGender(doc.getString("gender"));
        lawyer.setShortBio(doc.getString("shortBio"));
        lawyer.setPreferredLanguage(doc.getString("preferredLanguage"));
    }

    private void buildContactLocation(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setMobileNumber(doc.getString("mobileNumber"));
        lawyer.setEmailAddress(doc.getString("emailAddress"));
        lawyer.setOfficeAddress(doc.getString("officeAddress"));
        lawyer.setWorkingDays((List<String>) doc.get("workingDays"));
        lawyer.setWorkingStartTime(doc.getString("workingStartTime"));
        lawyer.setWorkingEndTime(doc.getString("workingEndTime"));
    }

    private void buildProfessionalInfo(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setPracticeAreas((List<String>) doc.get("practiceAreas"));
        lawyer.setExperience(doc.getString("experience"));
        lawyer.setChamberName(doc.getString("chamberName"));
        lawyer.setBarRegistrationNumber(doc.getString("barRegistrationNumber"));
        lawyer.setEnrollmentYear(doc.getString("enrollmentYear"));
        lawyer.setLlbInstitution(doc.getString("llbInstitution"));
        lawyer.setLlbYear(doc.getString("llbYear"));
    }

    private void buildConsultationFees(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setFeeType(doc.getString("feeType"));
        lawyer.setFixedFee(doc.getString("fixedFee"));
        lawyer.setMinFee(doc.getString("minFee"));
        lawyer.setMaxFee(doc.getString("maxFee"));
        lawyer.setAvailable(doc.getBoolean("isAvailable") != null ? doc.getBoolean("isAvailable") : false);
        lawyer.setConsultationTypes((List<String>) doc.get("consultationTypes"));
    }

    private void buildDocuments(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setProfileImageUrl(doc.getString("profileImageUrl"));
        lawyer.setIdCardUrl(doc.getString("idCardUrl"));
        lawyer.setBarCertificateUrl(doc.getString("barCertificateUrl"));
    }

    private void buildProfileStatus(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setProfileStatus(doc.getString("profileStatus"));
        lawyer.setVerified(doc.getBoolean("isVerified") != null ? doc.getBoolean("isVerified") : false);
        lawyer.setActive(doc.getBoolean("isActive") != null ? doc.getBoolean("isActive") : false);

        // Get rating and totalReviews from ProfileStatus
        Double rating = doc.getDouble("rating");
        lawyer.setRating(rating != null ? rating : 0.0);

        Long totalReviews = doc.getLong("totalReviews");
        lawyer.setReviewCount(totalReviews != null ? totalReviews.intValue() : 0);
    }

    private void buildAppointmentSettings(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setAppointmentBookingEnabled(doc.getBoolean("appointmentBookingEnabled") != null ?
                doc.getBoolean("appointmentBookingEnabled") : false);
    }

    private void populateUI() {
        // Set collapsing toolbar title
        collapsingToolbar.setTitle("Lawyer Profile");
        collapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
        collapsingToolbar.setExpandedTitleColor(Color.WHITE);

        // Load profile image
        if (!TextUtils.isEmpty(currentLawyer.getProfileImageUrl())) {
            Glide.with(this)
                    .load(currentLawyer.getProfileImageUrl())
                    .placeholder(R.drawable.lowyer_placeholder)
                    .error(R.drawable.lowyer_placeholder)
                    .into(ivProfileImage);
        }

        // Basic info
        tvLawyerName.setText(currentLawyer.getFullName());
        tvChamberName.setText(currentLawyer.getChamberName());

        // Rating and reviews
        double rating = currentLawyer.getRating();
        ratingBar.setRating((float) rating);
        tvRating.setText(String.format("%.1f", rating));
        tvReviewCount.setText(String.format("(%d reviews)", currentLawyer.getReviewCount()));

        // Quick info cards
        tvExperienceCount.setText(currentLawyer.getExperience() + "+");
        setupConsultationFee();
        setupAvailabilityStatus();

        // About section
        tvShortBio.setText(currentLawyer.getShortBio());

        // Practice areas
        setupPracticeAreas();

        // Professional details
        tvBarRegistration.setText(currentLawyer.getBarRegistrationNumber());
        setupEducation();
        setupLanguages();

        // Contact information
        tvOfficeAddress.setText(currentLawyer.getOfficeAddress());
        setupWorkingHours();

        // Consultation types
        setupConsultationTypes();
    }

    private void setupConsultationFee() {
        String feeText = "৳0";
        if ("fixed".equals(currentLawyer.getFeeType()) && !TextUtils.isEmpty(currentLawyer.getFixedFee())) {
            feeText = "৳" + currentLawyer.getFixedFee();
        } else if ("ranged".equals(currentLawyer.getFeeType()) && !TextUtils.isEmpty(currentLawyer.getMinFee())) {
            if (!TextUtils.isEmpty(currentLawyer.getMaxFee())) {
                feeText = "৳" + currentLawyer.getMinFee() + "-" + currentLawyer.getMaxFee();
            } else {
                feeText = "৳" + currentLawyer.getMinFee() + "+";
            }
        }
        tvConsultationFee.setText(feeText);
    }

    private void setupAvailabilityStatus() {
        if (currentLawyer.isAvailable()) {
            tvAvailabilityStatus.setText("Available");
            ivAvailabilityIcon.setImageResource(R.drawable.ic_check_circle);
            ivAvailabilityIcon.setColorFilter(getColor(R.color.success));
        } else {
            tvAvailabilityStatus.setText("Busy");
            ivAvailabilityIcon.setImageResource(R.drawable.ic_cancel);
            ivAvailabilityIcon.setColorFilter(getColor(R.color.error));
        }
    }

    /**
     * Updated method to work with LinearLayout instead of FlexboxLayout
     * Creates practice area chips in a flowing layout manually
     */
    private void setupPracticeAreas() {
        layoutPracticeAreas.removeAllViews();

        if (currentLawyer.getPracticeAreas() != null && !currentLawyer.getPracticeAreas().isEmpty()) {
            LinearLayout currentRow = null;
            int maxChipsPerRow = 3; // Adjust based on your needs
            int currentChipCount = 0;

            for (String area : currentLawyer.getPracticeAreas()) {
                // Create new row if needed
                if (currentRow == null || currentChipCount >= maxChipsPerRow) {
                    currentRow = new LinearLayout(this);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    layoutPracticeAreas.addView(currentRow);
                    currentChipCount = 0;
                }

                // Create chip
                Chip chip = new Chip(this);
                chip.setText(area);
                chip.setChipBackgroundColorResource(R.color.primary_container);
                chip.setTextColor(getColor(R.color.on_primary_container));
                chip.setClickable(false);

                // Set layout params with margins
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                chipParams.setMargins(0, 0, 16, 8);
                chip.setLayoutParams(chipParams);

                currentRow.addView(chip);
                currentChipCount++;
            }
        }
    }

    private void setupEducation() {
        String education = "";
        if (!TextUtils.isEmpty(currentLawyer.getLlbInstitution()) &&
                !TextUtils.isEmpty(currentLawyer.getLlbYear())) {
            education = "LLB from " + currentLawyer.getLlbInstitution() + " (" + currentLawyer.getLlbYear() + ")";
        } else {
            education = "Education information not available";
        }
        tvEducation.setText(education);
    }

    private void setupLanguages() {
        String language = currentLawyer.getPreferredLanguage();
        if ("both".equals(language)) {
            tvLanguages.setText("Bangla, English");
        } else if ("bangla".equals(language)) {
            tvLanguages.setText("Bangla");
        } else if ("english".equals(language)) {
            tvLanguages.setText("English");
        } else {
            tvLanguages.setText("Not specified");
        }
    }

    private void setupWorkingHours() {
        StringBuilder workingHours = new StringBuilder();

        if (currentLawyer.getWorkingDays() != null && !currentLawyer.getWorkingDays().isEmpty()) {
            // Create a readable format for working days
            List<String> days = currentLawyer.getWorkingDays();
            for (int i = 0; i < days.size(); i++) {
                workingHours.append(capitalizeFirst(days.get(i)));
                if (i < days.size() - 1) {
                    workingHours.append(", ");
                }
            }

            // Add working time if available
            if (!TextUtils.isEmpty(currentLawyer.getWorkingStartTime()) &&
                    !TextUtils.isEmpty(currentLawyer.getWorkingEndTime())) {
                workingHours.append(": ");
                workingHours.append(formatTime(currentLawyer.getWorkingStartTime()));
                workingHours.append(" - ");
                workingHours.append(formatTime(currentLawyer.getWorkingEndTime()));
            }
        } else {
            workingHours.append("Working hours not specified");
        }

        tvWorkingHours.setText(workingHours.toString());
    }

    /**
     * Helper method to capitalize first letter of a string
     */
    private String capitalizeFirst(String text) {
        if (TextUtils.isEmpty(text)) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Helper method to format time string (e.g., "09:00" to "9:00 AM")
     */
    private String formatTime(String time) {
        if (TextUtils.isEmpty(time)) return time;

        try {
            // Assuming time is in 24-hour format like "09:00"
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            String amPm = (hour >= 12) ? "PM" : "AM";
            if (hour > 12) hour -= 12;
            if (hour == 0) hour = 12;

            return String.format("%d:%02d %s", hour, minute, amPm);
        } catch (Exception e) {
            return time; // Return original if parsing fails
        }
    }

    private void setupConsultationTypes() {
        layoutConsultationTypes.removeAllViews();

        if (currentLawyer.getConsultationTypes() != null && !currentLawyer.getConsultationTypes().isEmpty()) {
            for (String type : currentLawyer.getConsultationTypes()) {
                try {
                    View typeView = getLayoutInflater().inflate(R.layout.item_consultation_type, null);

                    ImageView icon = typeView.findViewById(R.id.ivConsultationIcon);
                    TextView text = typeView.findViewById(R.id.tvConsultationType);

                    text.setText(type);

                    // Set appropriate icon based on consultation type
                    switch (type.toLowerCase()) {
                        case "online":
                            icon.setImageResource(R.drawable.ic_video_call);
                            break;
                        case "in-person":
                        case "in person":
                        case "offline":
                            icon.setImageResource(R.drawable.ic_person);
                            break;
                        case "phone":
                            icon.setImageResource(R.drawable.ic_call);
                            break;
                        case "chat":
                            icon.setImageResource(R.drawable.ic_chat);
                            break;
                        default:
                            icon.setImageResource(R.drawable.ic_help);
                            break;
                    }

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                    );
                    params.setMargins(8, 0, 8, 0);
                    typeView.setLayoutParams(params);

                    layoutConsultationTypes.addView(typeView);
                } catch (Exception e) {
                    Log.e(TAG, "Error inflating consultation type view", e);
                }
            }
        }
    }

    private void openChat() {
        try {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("lawyer_id", currentLawyer.getLawyerId());
            intent.putExtra("lawyer_name", currentLawyer.getFullName());
            intent.putExtra("lawyer_image", currentLawyer.getProfileImageUrl());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening chat", e);
            showToast("Chat feature not available");
        }
    }

    private void makePhoneCall() {
        if (!TextUtils.isEmpty(currentLawyer.getMobileNumber())) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + currentLawyer.getMobileNumber()));
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error making phone call", e);
                showToast("Unable to make phone call");
            }
        } else {
            showToast("Phone number not available");
        }
    }

    private void bookAppointment() {
        if (currentLawyer.isAppointmentBookingEnabled()) {
            try {
                showAppointmentBottomSheet();
            } catch (Exception e) {
                Log.e(TAG, "Error opening appointment booking", e);
                showToast("Appointment booking not available");
            }
        } else {
            showToast("Appointment booking is not enabled for this lawyer");
        }
    }


    private void showAppointmentBottomSheet() {
        // Create bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_appointment_booking, null);

        // Initialize UI components from bottom sheet
        TextInputLayout tilDate = bottomSheetView.findViewById(R.id.tilDate);
        TextInputLayout tilTime = bottomSheetView.findViewById(R.id.tilTime);
        TextInputLayout tilCaseTitle = bottomSheetView.findViewById(R.id.tilCaseTitle);
        TextInputLayout tilDescription = bottomSheetView.findViewById(R.id.tilDescription);

        TextInputEditText etDate = bottomSheetView.findViewById(R.id.etDate);
        TextInputEditText etTime = bottomSheetView.findViewById(R.id.etTime);
        TextInputEditText etCaseTitle = bottomSheetView.findViewById(R.id.etCaseTitle);
        TextInputEditText etDescription = bottomSheetView.findViewById(R.id.etDescription);

        MaterialButton btnBookNow = bottomSheetView.findViewById(R.id.btnBookNow);
        MaterialButton btnCancel = bottomSheetView.findViewById(R.id.btnCancel);
        TextView tvLawyerName = bottomSheetView.findViewById(R.id.tvLawyerName);
        TextView tvLawyerSpecialty = bottomSheetView.findViewById(R.id.tvLawyerSpecialty);
        CircleImageView ivLawyerPhoto = bottomSheetView.findViewById(R.id.ivLawyerPhoto);

        // Set lawyer info
        tvLawyerName.setText(currentLawyer.getFullName());
        if (currentLawyer.getPracticeAreas() != null && !currentLawyer.getPracticeAreas().isEmpty()) {
            tvLawyerSpecialty.setText(currentLawyer.getPracticeAreas().get(0));
        }

        // Load lawyer photo
        if (!TextUtils.isEmpty(currentLawyer.getProfileImageUrl())) {
            Glide.with(this)
                    .load(currentLawyer.getProfileImageUrl())
                    .placeholder(R.drawable.lowyer_placeholder)
                    .error(R.drawable.lowyer_placeholder)
                    .into(ivLawyerPhoto);
        }

        // Make date and time fields non-editable (picker only)
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etTime.setFocusable(false);
        etTime.setClickable(true);

        // Date picker
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        // Time picker
        etTime.setOnClickListener(v -> showTimePicker(etTime));

        // Cancel button
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Book appointment button
        btnBookNow.setOnClickListener(v -> {
            String caseTitle = etCaseTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            // Validation
            if (validateAppointmentData(tilDate, tilTime, tilCaseTitle, tilDescription,
                    date, time, caseTitle, description)) {

                // Show loading
                btnBookNow.setEnabled(false);
                btnBookNow.setText("Booking...");

                // Create appointment
                createAppointment(date, time, caseTitle, description,
                        bottomSheetDialog, btnBookNow);
            }
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }


    private void showDatePicker(TextInputEditText etDate) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etDate.setText(dateFormat.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        // Set maximum date to 30 days from now
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_MONTH, 30);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void showTimePicker(TextInputEditText etTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfHour) -> {
                    Calendar timeCalendar = Calendar.getInstance();
                    timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    timeCalendar.set(Calendar.MINUTE, minuteOfHour);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    selectedTime = timeFormat.format(timeCalendar.getTime());
                    etTime.setText(selectedTime);
                },
                hour,
                minute,
                false // Use 12-hour format
        );

        timePickerDialog.show();
    }

    private boolean validateAppointmentData(TextInputLayout tilDate, TextInputLayout tilTime,
                                            TextInputLayout tilCaseTitle, TextInputLayout tilDescription,
                                            String date, String time, String caseTitle, String description) {

        boolean isValid = true;

        // Reset errors
        tilDate.setError(null);
        tilTime.setError(null);
        tilCaseTitle.setError(null);
        tilDescription.setError(null);

        // Validate date
        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Please select a date");
            isValid = false;
        }

        // Validate time
        if (TextUtils.isEmpty(time)) {
            tilTime.setError("Please select a time");
            isValid = false;
        }

        // Validate case title
        if (TextUtils.isEmpty(caseTitle)) {
            tilCaseTitle.setError("Please enter case title");
            isValid = false;
        } else if (caseTitle.length() < 5) {
            tilCaseTitle.setError("Case title must be at least 5 characters");
            isValid = false;
        }

        // Validate description
        if (TextUtils.isEmpty(description)) {
            tilDescription.setError("Please enter case description");
            isValid = false;
        } else if (description.length() < 10) {
            tilDescription.setError("Description must be at least 10 characters");
            isValid = false;
        }

        // Check if user is authenticated
        if (auth.getCurrentUser() == null) {
            showToast("Please login to book appointment");
            isValid = false;
        }

        return isValid;
    }

    private void createAppointment(String date, String time, String caseTitle, String description,
                                   BottomSheetDialog dialog, MaterialButton btnBookNow) {

        String currentUserId = auth.getCurrentUser().getUid();

        // Create appointment data
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("clientId", currentUserId);
        appointmentData.put("lawyerId", currentLawyer.getLawyerId());
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("caseTitle", caseTitle);
        appointmentData.put("description", description);
        appointmentData.put("status", "pending");
        appointmentData.put("createdAt", Timestamp.now());
        appointmentData.put("appointmentId", ""); // Initially empty, will be updated with document ID

        // Add to Firestore
        db.collection("Appointments")
                .add(appointmentData)
                .addOnSuccessListener(documentReference -> {
                    String appointmentId = documentReference.getId();
                    Log.d(TAG, "Appointment created with ID: " + appointmentId);

                    // Update the document with its own ID in the appointmentId field
                    documentReference.update("appointmentId", appointmentId)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "AppointmentId field updated successfully");

                                // Reset button state
                                btnBookNow.setEnabled(true);
                                btnBookNow.setText("Book Appointment");

                                // Close dialog
                                dialog.dismiss();

                                // Show success message
                                showToast("Appointment booked successfully!");

                                // Show confirmation dialog
                                showAppointmentConfirmation(appointmentId, date, time);
                            })
                            .addOnFailureListener(updateError -> {
                                Log.e(TAG, "Error updating appointmentId field", updateError);
                                // Still show success since main appointment was created

                                // Reset button state
                                btnBookNow.setEnabled(true);
                                btnBookNow.setText("Book Appointment");

                                // Close dialog
                                dialog.dismiss();

                                showToast("Appointment booked successfully!");
                                showAppointmentConfirmation(appointmentId, date, time);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating appointment", e);

                    // Reset button state
                    btnBookNow.setEnabled(true);
                    btnBookNow.setText("Book Appointment");

                    showToast("Failed to book appointment. Please try again.");
                });
    }

    private void showAppointmentConfirmation(String appointmentId, String date, String time) {
        // Create a simple confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Appointment Confirmed")
                .setMessage("Your appointment has been booked successfully!\n\n" +
                        "Date: " + date + "\n" +
                        "Time: " + time + "\n" +
                        "Lawyer: " + currentLawyer.getFullName() + "\n\n" +
                        "You will receive a confirmation shortly.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("View Appointments", (dialog, which) -> {
                    // Redirect to appointments activity
                    // Intent intent = new Intent(this, AppointmentsActivity.class);
                    // startActivity(intent);
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }


    private void toggleFavorite() {
        isFavorite = !isFavorite;

        if (isFavorite) {
            fabFavorite.setImageResource(R.drawable.ic_favorite_filled);
            showToast("Added to favorites");
        } else {
            fabFavorite.setImageResource(R.drawable.ic_favorite_border);
            showToast("Removed from favorites");
        }

        // Save favorite status
        saveFavoriteStatus();
    }

    private void saveFavoriteStatus() {
        // TODO: Implementation for saving favorite status to Firebase
        // This would typically save to current user's favorites collection
        try {
            // Example implementation:
            // String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // db.collection("Users").document(currentUserId)
            //   .collection("Favorites").document(currentLawyer.getLawyerId())
            //   .set(favoriteData);
        } catch (Exception e) {
            Log.e(TAG, "Error saving favorite status", e);
        }
    }

    private void openDirections() {
        if (!TextUtils.isEmpty(currentLawyer.getOfficeAddress())) {
            try {
                String address = Uri.encode(currentLawyer.getOfficeAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?daddr=" + address));

                // Check if there's an app that can handle this intent
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    showToast("No map application found");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening directions", e);
                showToast("Unable to open directions");
            }
        } else {
            showToast("Address not available");
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
    }
}