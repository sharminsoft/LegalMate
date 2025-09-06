package com.yourname.legalmate.LawyerPortal.Activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.R;
import com.yourname.legalmate.utils.CloudinaryConfig;
import com.yourname.legalmate.utils.YearPickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LawyerProfileInfoActivity extends AppCompatActivity {

    private static final String TAG = "LawyerProfileInfo";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int TIMEOUT_DURATION = 30000; // 30 seconds

    // UI Components
    private MaterialToolbar toolbar;
    private LinearLayout contentContainer;
    private String sectionName;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // Image handling
    private CircleImageView currentProfileImage;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imageLauncher;
    private ActivityResultLauncher<Intent> documentLauncher;

    // Data storage
    private Map<String, Object> currentSectionData = new HashMap<>();
    private boolean isDataLoaded = false;

    // Progress dialogs - Using MaterialAlertDialog with custom layout
    private AlertDialog currentProgressDialog;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lawyer_profile_info);

        initializeFirebase();
        initializeViews();
        initializeActivityResultLaunchers();
        setupToolbar();
        loadSectionData();
        checkStoragePermissions();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (TextUtils.isEmpty(userId)) {
            showErrorAndFinish("User not authenticated");
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        contentContainer = findViewById(R.id.contentContainer);

        // Get section name from Intent
        Intent intent = getIntent();
        sectionName = intent.getStringExtra("section_name");
        if (TextUtils.isEmpty(sectionName)) {
            sectionName = "Unknown Section";
        }
    }

    private void initializeActivityResultLaunchers() {
        imageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleImageSelection(result));

        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleDocumentSelection(result));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(sectionName);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSectionData() {
        showProgressDialog("Loading data...");
        startTimeout("Loading data timeout");

        String documentName = getSectionDocumentName(sectionName);

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document(documentName)
                .get()
                .addOnCompleteListener(task -> {
                    runOnUiThread(() -> {
                        cancelTimeout();
                        dismissProgressDialog();

                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                currentSectionData = document.getData();
                                if (currentSectionData == null) {
                                    currentSectionData = new HashMap<>();
                                }
                                Log.d(TAG, "Data loaded for section: " + sectionName);
                            } else {
                                currentSectionData = new HashMap<>();
                                Log.d(TAG, "No existing data for section: " + sectionName);
                            }
                            isDataLoaded = true;
                            setupUIForSection(sectionName);
                        } else {
                            Log.e(TAG, "Error loading data", task.getException());
                            showToast("Failed to load data");
                            currentSectionData = new HashMap<>();
                            isDataLoaded = true;
                            setupUIForSection(sectionName);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        cancelTimeout();
                        dismissProgressDialog();
                        Log.e(TAG, "Failed to load data", e);
                        showToast("Network error. Please try again.");
                        currentSectionData = new HashMap<>();
                        isDataLoaded = true;
                        setupUIForSection(sectionName);
                    });
                });
    }

    private String getSectionDocumentName(String sectionName) {
        switch (sectionName) {
            case "Profile Settings": return "BasicProfileSettings";
            case "Contact & Location Settings": return "ContactLocationSettings";
            case "Professional Information": return "ProfessionalInformation";
            case "Consultation & Fees": return "ConsultationFees";
            case "Appointment Settings": return "AppointmentSettings";
            case "Security & Account Settings": return "SecurityAccountSettings";
            case "Social & Website Links": return "SocialWebsiteLinks";
            case "Documents": return "Documents";
            default: return "BasicProfileSettings";
        }
    }

    private void setupUIForSection(String sectionName) {

        if (contentContainer != null) {
            contentContainer.removeAllViews();
        }

        switch (sectionName) {
            case "Profile Settings":
                setupProfileSettings();
                break;
            case "Contact & Location Settings":
                setupContactLocationSettings();
                break;
            case "Professional Information":
                setupProfessionalInformation();
                break;
            case "Consultation & Fees":
                setupConsultationFees();
                break;
            case "Appointment Settings":
                setupAppointmentSettings();
                break;
            case "Security & Account Settings":
                setupSecurityAccountSettings();
                break;
            case "Social & Website Links":
                setupSocialWebsiteLinks();
                break;
            default:

                break;
        }
    }

    private void setupProfileSettings() {
        // Profile Photo Section
        currentProfileImage = new CircleImageView(this);
        currentProfileImage.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
        contentContainer.addView(currentProfileImage);

        // Load existing profile image
        String profileImageUrl = getStringValue("profileImageUrl");
        if (!TextUtils.isEmpty(profileImageUrl)) {
            Glide.with(this).load(profileImageUrl).into(currentProfileImage);
        }

        // Change Profile Photo Button
        MaterialButton changeProfilePhotoButton = createButton("Change Profile Photo", R.drawable.ic_camera);
        changeProfilePhotoButton.setOnClickListener(v -> selectImage());
        contentContainer.addView(changeProfilePhotoButton);

        // Full Name
        TextInputLayout fullNameLayout = createTextInputLayout("Full Name");
        TextInputEditText fullNameEditText = createTextInputEditText();
        fullNameEditText.setText(getStringValue("fullName"));
        fullNameLayout.addView(fullNameEditText);
        contentContainer.addView(fullNameLayout);

        // Gender Selection
        TextView genderLabel = createLabel("Gender");
        contentContainer.addView(genderLabel);

        RadioGroup genderRadioGroup = new RadioGroup(this);
        MaterialRadioButton maleRadio = createRadioButton("Male");
        MaterialRadioButton femaleRadio = createRadioButton("Female");
        MaterialRadioButton otherRadio = createRadioButton("Other");

        genderRadioGroup.addView(maleRadio);
        genderRadioGroup.addView(femaleRadio);
        genderRadioGroup.addView(otherRadio);

        String gender = getStringValue("gender");
        if ("male".equals(gender)) maleRadio.setChecked(true);
        else if ("female".equals(gender)) femaleRadio.setChecked(true);
        else if ("other".equals(gender)) otherRadio.setChecked(true);

        contentContainer.addView(genderRadioGroup);

        // Date of Birth
        TextInputLayout dobLayout = createTextInputLayout("Date of Birth");
        TextInputEditText dobEditText = createTextInputEditText();
        dobEditText.setText(getStringValue("dateOfBirth"));
        dobEditText.setOnClickListener(v -> showDatePicker(dobEditText));
        dobLayout.addView(dobEditText);
        contentContainer.addView(dobLayout);

        // Language Preferences
        TextView languageLabel = createLabel("Preferred Language");
        contentContainer.addView(languageLabel);

        RadioGroup languageRadioGroup = new RadioGroup(this);
        MaterialRadioButton banglaRadio = createRadioButton("Bangla");
        MaterialRadioButton englishRadio = createRadioButton("English");
        MaterialRadioButton bothRadio = createRadioButton("Both");

        languageRadioGroup.addView(banglaRadio);
        languageRadioGroup.addView(englishRadio);
        languageRadioGroup.addView(bothRadio);

        String language = getStringValue("preferredLanguage");
        if ("bangla".equals(language)) banglaRadio.setChecked(true);
        else if ("english".equals(language)) englishRadio.setChecked(true);
        else if ("both".equals(language)) bothRadio.setChecked(true);

        contentContainer.addView(languageRadioGroup);

        // Short Bio
        TextInputLayout bioLayout = createTextInputLayout("Short Bio / About Me");
        TextInputEditText bioEditText = createTextInputEditText();
        bioEditText.setText(getStringValue("shortBio"));
        bioEditText.setMinLines(3);
        bioLayout.addView(bioEditText);
        contentContainer.addView(bioLayout);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveProfileSettings(
                fullNameEditText, dobEditText, bioEditText,
                genderRadioGroup, languageRadioGroup));
        contentContainer.addView(saveButton);
    }

    private void setupContactLocationSettings() {
        // Phone Number
        TextInputLayout phoneLayout = createTextInputLayout("Phone Number");
        TextInputEditText phoneEditText = createTextInputEditText();
        phoneEditText.setText(getStringValue("mobileNumber"));
        phoneLayout.addView(phoneEditText);
        contentContainer.addView(phoneLayout);

        // Email Address
        TextInputLayout emailLayout = createTextInputLayout("Email Address");
        TextInputEditText emailEditText = createTextInputEditText();
        emailEditText.setText(getStringValue("emailAddress"));
        emailLayout.addView(emailEditText);
        contentContainer.addView(emailLayout);

        // Office Address
        TextInputLayout addressLayout = createTextInputLayout("Office Address");
        TextInputEditText addressEditText = createTextInputEditText();
        addressEditText.setText(getStringValue("officeAddress"));
        addressEditText.setMinLines(2);
        addressLayout.addView(addressEditText);
        contentContainer.addView(addressLayout);

        // Working Days
        TextView workingDaysLabel = createLabel("Working Days");
        contentContainer.addView(workingDaysLabel);

        List<String> workingDays = getListValue("workingDays");

        MaterialCheckBox saturdayCheckBox = createCheckBox("Saturday");
        saturdayCheckBox.setChecked(workingDays.contains("Saturday"));
        contentContainer.addView(saturdayCheckBox);

        MaterialCheckBox sundayCheckBox = createCheckBox("Sunday");
        sundayCheckBox.setChecked(workingDays.contains("Sunday"));
        contentContainer.addView(sundayCheckBox);

        MaterialCheckBox mondayCheckBox = createCheckBox("Monday");
        mondayCheckBox.setChecked(workingDays.contains("Monday"));
        contentContainer.addView(mondayCheckBox);

        MaterialCheckBox tuesdayCheckBox = createCheckBox("Tuesday");
        tuesdayCheckBox.setChecked(workingDays.contains("Tuesday"));
        contentContainer.addView(tuesdayCheckBox);

        MaterialCheckBox wednesdayCheckBox = createCheckBox("Wednesday");
        wednesdayCheckBox.setChecked(workingDays.contains("Wednesday"));
        contentContainer.addView(wednesdayCheckBox);

        MaterialCheckBox thursdayCheckBox = createCheckBox("Thursday");
        thursdayCheckBox.setChecked(workingDays.contains("Thursday"));
        contentContainer.addView(thursdayCheckBox);

        MaterialCheckBox fridayCheckBox = createCheckBox("Friday");
        fridayCheckBox.setChecked(workingDays.contains("Friday"));
        contentContainer.addView(fridayCheckBox);

        // Working Hours
        TextInputLayout startTimeLayout = createTextInputLayout("Working Start Time");
        TextInputEditText startTimeEditText = createTextInputEditText();
        startTimeEditText.setText(getStringValue("workingStartTime"));
        startTimeEditText.setOnClickListener(v -> showTimePicker(startTimeEditText));
        startTimeLayout.addView(startTimeEditText);
        contentContainer.addView(startTimeLayout);

        TextInputLayout endTimeLayout = createTextInputLayout("Working End Time");
        TextInputEditText endTimeEditText = createTextInputEditText();
        endTimeEditText.setText(getStringValue("workingEndTime"));
        endTimeEditText.setOnClickListener(v -> showTimePicker(endTimeEditText));
        endTimeLayout.addView(endTimeEditText);
        contentContainer.addView(endTimeLayout);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveContactLocationSettings(
                phoneEditText, emailEditText, addressEditText,
                startTimeEditText, endTimeEditText,
                saturdayCheckBox, sundayCheckBox, mondayCheckBox, tuesdayCheckBox,
                wednesdayCheckBox, thursdayCheckBox, fridayCheckBox));
        contentContainer.addView(saveButton);
    }

    private void setupProfessionalInformation() {
        // Practice Areas
        TextView practiceAreasLabel = createLabel("Practice Areas");
        contentContainer.addView(practiceAreasLabel);

        List<String> practiceAreas = getListValue("practiceAreas");

        MaterialCheckBox criminalCheckBox = createCheckBox("Criminal Law");
        criminalCheckBox.setChecked(practiceAreas.contains("Criminal Law"));
        contentContainer.addView(criminalCheckBox);

        MaterialCheckBox civilCheckBox = createCheckBox("Civil Law");
        civilCheckBox.setChecked(practiceAreas.contains("Civil Law"));
        contentContainer.addView(civilCheckBox);

        MaterialCheckBox familyCheckBox = createCheckBox("Family Law");
        familyCheckBox.setChecked(practiceAreas.contains("Family Law"));
        contentContainer.addView(familyCheckBox);

        MaterialCheckBox corporateCheckBox = createCheckBox("Corporate Law");
        corporateCheckBox.setChecked(practiceAreas.contains("Corporate Law"));
        contentContainer.addView(corporateCheckBox);

        MaterialCheckBox propertyCheckBox = createCheckBox("Property Law");
        propertyCheckBox.setChecked(practiceAreas.contains("Property Law"));
        contentContainer.addView(propertyCheckBox);

        MaterialCheckBox otherCheckBox = createCheckBox("Other");
        // Check if there are any practice areas not in the predefined list
        boolean hasOther = false;
        String otherPracticeArea = "";
        for (String area : practiceAreas) {
            if (!isPredefinedPracticeArea(area)) {
                hasOther = true;
                otherPracticeArea = area;
                break;
            }
        }
        otherCheckBox.setChecked(hasOther);
        contentContainer.addView(otherCheckBox);

        // Other Practice Area Text Field
        TextInputLayout otherPracticeLayout = createTextInputLayout("Specify Other Practice Area");
        TextInputEditText otherPracticeEditText = createTextInputEditText();
        otherPracticeEditText.setText(otherPracticeArea);
        otherPracticeLayout.addView(otherPracticeEditText);
        otherPracticeLayout.setVisibility(hasOther ? View.VISIBLE : View.GONE);
        contentContainer.addView(otherPracticeLayout);

        otherCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                otherPracticeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Years of Experience
        TextInputLayout experienceLayout = createTextInputLayout("Years of Experience");
        TextInputEditText experienceEditText = createTextInputEditText();
        experienceEditText.setText(getStringValue("experience"));
        experienceLayout.addView(experienceEditText);
        contentContainer.addView(experienceLayout);

        // Chamber Name
        TextInputLayout chamberLayout = createTextInputLayout("Law Firm / Chamber Name");
        TextInputEditText chamberEditText = createTextInputEditText();
        chamberEditText.setText(getStringValue("chamberName"));
        chamberLayout.addView(chamberEditText);
        contentContainer.addView(chamberLayout);

        // BAR Registration Number
        TextInputLayout barLayout = createTextInputLayout("BAR Registration Number");
        TextInputEditText barEditText = createTextInputEditText();
        barEditText.setText(getStringValue("barRegistrationNumber"));
        barLayout.addView(barEditText);
        contentContainer.addView(barLayout);

        // Enrollment Year
        TextInputLayout enrollmentLayout = createTextInputLayout("Enrollment Year");
        TextInputEditText enrollmentEditText = createTextInputEditText();
        enrollmentEditText.setText(getStringValue("enrollmentYear"));
        enrollmentEditText.setOnClickListener(v -> showEnrollmentYearPicker(enrollmentEditText));
        enrollmentLayout.addView(enrollmentEditText);
        contentContainer.addView(enrollmentLayout);

        // LLB Institution
        TextInputLayout llbInstLayout = createTextInputLayout("LLB Institution");
        TextInputEditText llbInstEditText = createTextInputEditText();
        llbInstEditText.setText(getStringValue("llbInstitution"));
        llbInstLayout.addView(llbInstEditText);
        contentContainer.addView(llbInstLayout);

        // LLB Year
        TextInputLayout llbYearLayout = createTextInputLayout("LLB Graduation Year");
        TextInputEditText llbYearEditText = createTextInputEditText();
        llbYearEditText.setText(getStringValue("llbYear"));
        llbYearEditText.setOnClickListener(v -> showGraduationYearPicker(llbYearEditText));
        llbYearLayout.addView(llbYearEditText);
        contentContainer.addView(llbYearLayout);

        // Upload Legal Certificates Button
        MaterialButton uploadCertificateButton = createButton("Upload Legal Certificates", R.drawable.ic_upload);
        uploadCertificateButton.setOnClickListener(v -> selectDocument());
        contentContainer.addView(uploadCertificateButton);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveProfessionalInformation(
                experienceEditText, chamberEditText, barEditText, enrollmentEditText,
                llbInstEditText, llbYearEditText, otherPracticeEditText,
                criminalCheckBox, civilCheckBox, familyCheckBox, corporateCheckBox,
                propertyCheckBox, otherCheckBox));
        contentContainer.addView(saveButton);
    }

    private void setupConsultationFees() {
        // Fee Type Selection
        TextView feeTypeLabel = createLabel("Fee Type");
        contentContainer.addView(feeTypeLabel);

        RadioGroup feeTypeRadioGroup = new RadioGroup(this);
        MaterialRadioButton fixedFeeRadio = createRadioButton("Fixed Fee");
        MaterialRadioButton rangedFeeRadio = createRadioButton("Ranged Fee");

        feeTypeRadioGroup.addView(fixedFeeRadio);
        feeTypeRadioGroup.addView(rangedFeeRadio);

        String feeType = getStringValue("feeType");
        if ("fixed".equals(feeType)) fixedFeeRadio.setChecked(true);
        else if ("ranged".equals(feeType)) rangedFeeRadio.setChecked(true);

        contentContainer.addView(feeTypeRadioGroup);

        // Fixed Fee Layout
        LinearLayout fixedFeeLayout = new LinearLayout(this);
        fixedFeeLayout.setOrientation(LinearLayout.VERTICAL);

        TextInputLayout fixedFeeInputLayout = createTextInputLayout("Consultation Fee (Fixed)");
        TextInputEditText fixedFeeEditText = createTextInputEditText();
        fixedFeeEditText.setText(getStringValue("fixedFee"));
        fixedFeeInputLayout.addView(fixedFeeEditText);
        fixedFeeLayout.addView(fixedFeeInputLayout);

        contentContainer.addView(fixedFeeLayout);

        // Ranged Fee Layout
        LinearLayout rangedFeeLayout = new LinearLayout(this);
        rangedFeeLayout.setOrientation(LinearLayout.VERTICAL);

        TextInputLayout minFeeLayout = createTextInputLayout("Minimum Fee");
        TextInputEditText minFeeEditText = createTextInputEditText();
        minFeeEditText.setText(getStringValue("minFee"));
        minFeeLayout.addView(minFeeEditText);
        rangedFeeLayout.addView(minFeeLayout);

        TextInputLayout maxFeeLayout = createTextInputLayout("Maximum Fee");
        TextInputEditText maxFeeEditText = createTextInputEditText();
        maxFeeEditText.setText(getStringValue("maxFee"));
        maxFeeLayout.addView(maxFeeEditText);
        rangedFeeLayout.addView(maxFeeLayout);

        contentContainer.addView(rangedFeeLayout);

        // Initial visibility setup
        if ("fixed".equals(feeType)) {
            fixedFeeLayout.setVisibility(View.VISIBLE);
            rangedFeeLayout.setVisibility(View.GONE);
        } else if ("ranged".equals(feeType)) {
            fixedFeeLayout.setVisibility(View.GONE);
            rangedFeeLayout.setVisibility(View.VISIBLE);
        }

        // Fee type change listener
        feeTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rangedFeeRadio.getId()) {
                fixedFeeLayout.setVisibility(View.GONE);
                rangedFeeLayout.setVisibility(View.VISIBLE);
            } else {
                fixedFeeLayout.setVisibility(View.VISIBLE);
                rangedFeeLayout.setVisibility(View.GONE);
            }
        });

        // Availability Toggle
        TextView availabilityLabel = createLabel("Availability Status");
        contentContainer.addView(availabilityLabel);

        MaterialSwitch availabilitySwitch = new MaterialSwitch(this);
        availabilitySwitch.setText("Available for Consultation");
        availabilitySwitch.setChecked(getBooleanValue("isAvailable"));
        contentContainer.addView(availabilitySwitch);

        // Consultation Types
        TextView consultationTypeLabel = createLabel("Preferred Consultation Types");
        contentContainer.addView(consultationTypeLabel);

        List<String> consultationTypes = getListValue("consultationTypes");

        MaterialCheckBox inPersonCheckBox = createCheckBox("In-Person");
        inPersonCheckBox.setChecked(consultationTypes.contains("In-Person"));
        contentContainer.addView(inPersonCheckBox);

        MaterialCheckBox onlineCheckBox = createCheckBox("Online");
        onlineCheckBox.setChecked(consultationTypes.contains("Online"));
        contentContainer.addView(onlineCheckBox);

        MaterialCheckBox phoneCheckBox = createCheckBox("Phone");
        phoneCheckBox.setChecked(consultationTypes.contains("Phone"));
        contentContainer.addView(phoneCheckBox);

        MaterialCheckBox chatCheckBox = createCheckBox("Chat");
        chatCheckBox.setChecked(consultationTypes.contains("Chat"));
        contentContainer.addView(chatCheckBox);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveConsultationFees(
                feeTypeRadioGroup, fixedFeeEditText, minFeeEditText, maxFeeEditText,
                availabilitySwitch, inPersonCheckBox, onlineCheckBox, phoneCheckBox, chatCheckBox));
        contentContainer.addView(saveButton);
    }

    private void setupAppointmentSettings() {
        // Appointment Booking Toggle
        TextView bookingToggleLabel = createLabel("Appointment Booking");
        contentContainer.addView(bookingToggleLabel);

        MaterialSwitch bookingSwitch = new MaterialSwitch(this);
        bookingSwitch.setText("Enable Appointment Booking");
        bookingSwitch.setChecked(getBooleanValue("appointmentBookingEnabled"));
        contentContainer.addView(bookingSwitch);

        // Time Slots
        TextInputLayout startTimeLayout = createTextInputLayout("Available Slot Start Time");
        TextInputEditText startTimeEditText = createTextInputEditText();
        startTimeEditText.setText(getStringValue("slotStartTime"));
        startTimeEditText.setOnClickListener(v -> showTimePicker(startTimeEditText));
        startTimeLayout.addView(startTimeEditText);
        contentContainer.addView(startTimeLayout);

        TextInputLayout endTimeLayout = createTextInputLayout("Available Slot End Time");
        TextInputEditText endTimeEditText = createTextInputEditText();
        endTimeEditText.setText(getStringValue("slotEndTime"));
        endTimeEditText.setOnClickListener(v -> showTimePicker(endTimeEditText));
        endTimeLayout.addView(endTimeEditText);
        contentContainer.addView(endTimeLayout);

        // Max Appointments Per Day
        TextInputLayout maxAppointmentsLayout = createTextInputLayout("Max Appointments Per Day");
        TextInputEditText maxAppointmentsEditText = createTextInputEditText();
        maxAppointmentsEditText.setText(getStringValue("maxAppointmentsPerDay"));
        maxAppointmentsLayout.addView(maxAppointmentsEditText);
        contentContainer.addView(maxAppointmentsLayout);

        // Notification Settings
        TextView notificationLabel = createLabel("Notification Settings");
        contentContainer.addView(notificationLabel);

        MaterialCheckBox emailNotificationCheckBox = createCheckBox("Enable Email Notifications");
        emailNotificationCheckBox.setChecked(getBooleanValue("emailNotificationEnabled"));
        contentContainer.addView(emailNotificationCheckBox);

        MaterialCheckBox smsNotificationCheckBox = createCheckBox("Enable SMS Notifications");
        smsNotificationCheckBox.setChecked(getBooleanValue("smsNotificationEnabled"));
        contentContainer.addView(smsNotificationCheckBox);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveAppointmentSettings(
                bookingSwitch, startTimeEditText, endTimeEditText, maxAppointmentsEditText,
                emailNotificationCheckBox, smsNotificationCheckBox));
        contentContainer.addView(saveButton);
    }

    private void setupSecurityAccountSettings() {
        // Change Password
        TextInputLayout currentPasswordLayout = createTextInputLayout("Current Password");
        TextInputEditText currentPasswordEditText = createTextInputEditText();
        currentPasswordEditText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordLayout.addView(currentPasswordEditText);
        contentContainer.addView(currentPasswordLayout);

        TextInputLayout newPasswordLayout = createTextInputLayout("New Password");
        TextInputEditText newPasswordEditText = createTextInputEditText();
        newPasswordEditText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordLayout.addView(newPasswordEditText);
        contentContainer.addView(newPasswordLayout);

        TextInputLayout confirmPasswordLayout = createTextInputLayout("Confirm New Password");
        TextInputEditText confirmPasswordEditText = createTextInputEditText();
        confirmPasswordEditText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordLayout.addView(confirmPasswordEditText);
        contentContainer.addView(confirmPasswordLayout);

        // Change Password Button
        MaterialButton changePasswordButton = createButton("Change Password", R.drawable.ic_key);
        changePasswordButton.setOnClickListener(v -> changePassword(
                currentPasswordEditText, newPasswordEditText, confirmPasswordEditText));
        contentContainer.addView(changePasswordButton);

        // Enable 2-Factor Authentication
        MaterialCheckBox twoFactorCheckBox = createCheckBox("Enable 2-Factor Authentication");
        twoFactorCheckBox.setChecked(getBooleanValue("twoFactorEnabled"));
        contentContainer.addView(twoFactorCheckBox);

        // Logout from Other Devices
        MaterialButton logoutDevicesButton = createButton("Logout from Other Devices", R.drawable.ic_logout);
        logoutDevicesButton.setOnClickListener(v -> logoutFromOtherDevices());
        contentContainer.addView(logoutDevicesButton);

        // Delete Account
        MaterialButton deleteAccountButton = createButton("Delete Account", R.drawable.ic_delete);
        deleteAccountButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.error_color));
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
        contentContainer.addView(deleteAccountButton);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveSecuritySettings(twoFactorCheckBox));
        contentContainer.addView(saveButton);
    }

    private void setupSocialWebsiteLinks() {
        // Facebook Link
        TextInputLayout facebookLayout = createTextInputLayout("Facebook Link");
        TextInputEditText facebookEditText = createTextInputEditText();
        facebookEditText.setText(getStringValue("facebookLink"));
        facebookLayout.addView(facebookEditText);
        contentContainer.addView(facebookLayout);

        // LinkedIn Link
        TextInputLayout linkedinLayout = createTextInputLayout("LinkedIn Link");
        TextInputEditText linkedinEditText = createTextInputEditText();
        linkedinEditText.setText(getStringValue("linkedInLink"));
        linkedinLayout.addView(linkedinEditText);
        contentContainer.addView(linkedinLayout);

        // Website Link
        TextInputLayout websiteLayout = createTextInputLayout("Website Link");
        TextInputEditText websiteEditText = createTextInputEditText();
        websiteEditText.setText(getStringValue("websiteLink"));
        websiteLayout.addView(websiteEditText);
        contentContainer.addView(websiteLayout);

        // Video Introduction URL
        TextInputLayout videoUrlLayout = createTextInputLayout("Video Introduction URL");
        TextInputEditText videoUrlEditText = createTextInputEditText();
        videoUrlEditText.setText(getStringValue("videoIntroLink"));
        videoUrlLayout.addView(videoUrlEditText);
        contentContainer.addView(videoUrlLayout);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveSocialWebsiteLinks(
                facebookEditText, linkedinEditText, websiteEditText, videoUrlEditText));
        contentContainer.addView(saveButton);
    }

    // Helper methods for creating UI components
    private MaterialButton createButton(String text, int iconResource) {
        MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        if (iconResource != 0) {
            button.setIconResource(iconResource);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 8);
        button.setLayoutParams(params);
        return button;
    }

    private MaterialButton createSaveButton() {
        MaterialButton button = new MaterialButton(this);
        button.setText("Save Changes");
        button.setIconResource(R.drawable.ic_save);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 32, 0, 16);
        button.setLayoutParams(params);
        return button;
    }

    private TextInputLayout createTextInputLayout(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        layout.setLayoutParams(params);
        return layout;
    }

    private TextInputEditText createTextInputEditText() {
        TextInputEditText editText = new TextInputEditText(this);
        editText.setId(View.generateViewId());
        return editText;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(16);
        label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 8);
        label.setLayoutParams(params);
        return label;
    }

    private MaterialRadioButton createRadioButton(String text) {
        MaterialRadioButton radioButton = new MaterialRadioButton(this);
        radioButton.setText(text);
        radioButton.setId(View.generateViewId());
        return radioButton;
    }

    private MaterialCheckBox createCheckBox(String text) {
        MaterialCheckBox checkBox = new MaterialCheckBox(this);
        checkBox.setText(text);
        checkBox.setId(View.generateViewId());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        checkBox.setLayoutParams(params);
        return checkBox;
    }

    // Data helper methods
    private String getStringValue(String key) {
        if (currentSectionData != null && currentSectionData.containsKey(key)) {
            Object value = currentSectionData.get(key);
            return value != null ? value.toString() : "";
        }
        return "";
    }

    private boolean getBooleanValue(String key) {
        if (currentSectionData != null && currentSectionData.containsKey(key)) {
            Object value = currentSectionData.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> getListValue(String key) {
        if (currentSectionData != null && currentSectionData.containsKey(key)) {
            Object value = currentSectionData.get(key);
            if (value instanceof List) {
                return (List<String>) value;
            }
        }
        return new ArrayList<>();
    }

    private boolean isPredefinedPracticeArea(String area) {
        return area.equals("Criminal Law") || area.equals("Civil Law") ||
                area.equals("Family Law") || area.equals("Corporate Law") ||
                area.equals("Property Law");
    }

    // Save methods for each section
    private void saveProfileSettings(TextInputEditText fullNameEditText,
                                     TextInputEditText dobEditText,
                                     TextInputEditText bioEditText,
                                     RadioGroup genderRadioGroup,
                                     RadioGroup languageRadioGroup) {

        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullNameEditText.getText().toString().trim());
        data.put("dateOfBirth", dobEditText.getText().toString().trim());
        data.put("shortBio", bioEditText.getText().toString().trim());
        data.put("gender", getSelectedRadioValue(genderRadioGroup));
        data.put("preferredLanguage", getSelectedRadioValue(languageRadioGroup));
        data.put("updatedAt", System.currentTimeMillis());

        if (selectedImageUri != null) {
            uploadImageAndSave(data, "BasicProfileSettings");
        } else {
            saveToFirestore(data, "BasicProfileSettings");
        }
    }

    private void saveContactLocationSettings(TextInputEditText phoneEditText,
                                             TextInputEditText emailEditText,
                                             TextInputEditText addressEditText,
                                             TextInputEditText startTimeEditText,
                                             TextInputEditText endTimeEditText,
                                             MaterialCheckBox... dayCheckBoxes) {

        Map<String, Object> data = new HashMap<>();
        data.put("mobileNumber", phoneEditText.getText().toString().trim());
        data.put("emailAddress", emailEditText.getText().toString().trim());
        data.put("officeAddress", addressEditText.getText().toString().trim());
        data.put("workingStartTime", startTimeEditText.getText().toString().trim());
        data.put("workingEndTime", endTimeEditText.getText().toString().trim());

        List<String> workingDays = new ArrayList<>();
        String[] dayNames = {"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (int i = 0; i < dayCheckBoxes.length && i < dayNames.length; i++) {
            if (dayCheckBoxes[i].isChecked()) {
                workingDays.add(dayNames[i]);
            }
        }
        data.put("workingDays", workingDays);
        data.put("updatedAt", System.currentTimeMillis());

        saveToFirestore(data, "ContactLocationSettings");
    }

    private void saveProfessionalInformation(TextInputEditText experienceEditText,
                                             TextInputEditText chamberEditText,
                                             TextInputEditText barEditText,
                                             TextInputEditText enrollmentEditText,
                                             TextInputEditText llbInstEditText,
                                             TextInputEditText llbYearEditText,
                                             TextInputEditText otherPracticeEditText,
                                             MaterialCheckBox... practiceCheckBoxes) {

        Map<String, Object> data = new HashMap<>();
        data.put("experience", experienceEditText.getText().toString().trim());
        data.put("chamberName", chamberEditText.getText().toString().trim());
        data.put("barRegistrationNumber", barEditText.getText().toString().trim());
        data.put("enrollmentYear", enrollmentEditText.getText().toString().trim());
        data.put("llbInstitution", llbInstEditText.getText().toString().trim());
        data.put("llbYear", llbYearEditText.getText().toString().trim());

        List<String> practiceAreas = new ArrayList<>();
        String[] areaNames = {"Criminal Law", "Civil Law", "Family Law", "Corporate Law", "Property Law"};
        for (int i = 0; i < practiceCheckBoxes.length - 1 && i < areaNames.length; i++) {
            if (practiceCheckBoxes[i].isChecked()) {
                practiceAreas.add(areaNames[i]);
            }
        }

        // Handle "Other" practice area
        if (practiceCheckBoxes[practiceCheckBoxes.length - 1].isChecked()) {
            String otherArea = otherPracticeEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(otherArea)) {
                practiceAreas.add(otherArea);
            }
        }

        data.put("practiceAreas", practiceAreas);
        data.put("updatedAt", System.currentTimeMillis());

        saveToFirestore(data, "ProfessionalInformation");
    }

    private void saveConsultationFees(RadioGroup feeTypeRadioGroup,
                                      TextInputEditText fixedFeeEditText,
                                      TextInputEditText minFeeEditText,
                                      TextInputEditText maxFeeEditText,
                                      MaterialSwitch availabilitySwitch,
                                      MaterialCheckBox... consultationCheckBoxes) {

        Map<String, Object> data = new HashMap<>();
        String feeType = getSelectedRadioValue(feeTypeRadioGroup);
        data.put("feeType", feeType);

        if ("fixed".equals(feeType)) {
            data.put("fixedFee", fixedFeeEditText.getText().toString().trim());
            data.put("minFee", "");
            data.put("maxFee", "");
        } else {
            data.put("minFee", minFeeEditText.getText().toString().trim());
            data.put("maxFee", maxFeeEditText.getText().toString().trim());
            data.put("fixedFee", "");
        }

        data.put("isAvailable", availabilitySwitch.isChecked());

        List<String> consultationTypes = new ArrayList<>();
        String[] typeNames = {"In-Person", "Online", "Phone", "Chat"};
        for (int i = 0; i < consultationCheckBoxes.length && i < typeNames.length; i++) {
            if (consultationCheckBoxes[i].isChecked()) {
                consultationTypes.add(typeNames[i]);
            }
        }
        data.put("consultationTypes", consultationTypes);
        data.put("updatedAt", System.currentTimeMillis());

        saveToFirestore(data, "ConsultationFees");
    }

    private void saveAppointmentSettings(MaterialSwitch bookingSwitch,
                                         TextInputEditText startTimeEditText,
                                         TextInputEditText endTimeEditText,
                                         TextInputEditText maxAppointmentsEditText,
                                         MaterialCheckBox emailCheckBox,
                                         MaterialCheckBox smsCheckBox) {

        Map<String, Object> data = new HashMap<>();
        data.put("appointmentBookingEnabled", bookingSwitch.isChecked());
        data.put("slotStartTime", startTimeEditText.getText().toString().trim());
        data.put("slotEndTime", endTimeEditText.getText().toString().trim());
        data.put("maxAppointmentsPerDay", maxAppointmentsEditText.getText().toString().trim());
        data.put("emailNotificationEnabled", emailCheckBox.isChecked());
        data.put("smsNotificationEnabled", smsCheckBox.isChecked());
        data.put("updatedAt", System.currentTimeMillis());

        saveToFirestore(data, "AppointmentSettings");
    }

    private void saveSecuritySettings(MaterialCheckBox twoFactorCheckBox) {
        Map<String, Object> data = new HashMap<>();
        data.put("twoFactorEnabled", twoFactorCheckBox.isChecked());
        data.put("updatedAt", System.currentTimeMillis());

        saveToFirestore(data, "SecurityAccountSettings");
    }

    private void saveSocialWebsiteLinks(TextInputEditText facebookEditText,
                                        TextInputEditText linkedinEditText,
                                        TextInputEditText websiteEditText,
                                        TextInputEditText videoUrlEditText) {

        Map<String, Object> data = new HashMap<>();
        data.put("facebookLink", facebookEditText.getText().toString().trim());
        data.put("linkedInLink", linkedinEditText.getText().toString().trim());
        data.put("websiteLink", websiteEditText.getText().toString().trim());
        data.put("videoIntroLink", videoUrlEditText.getText().toString().trim());
        data.put("updatedAt", System.currentTimeMillis());

        saveToFirestore(data, "SocialWebsiteLinks");
    }

    // Utility methods
    private String getSelectedRadioValue(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            MaterialRadioButton selectedRadio = findViewById(selectedId);
            if (selectedRadio != null) {
                String text = selectedRadio.getText().toString().toLowerCase();
                if (text.equals("male") || text.equals("female") || text.equals("other")) {
                    return text;
                } else if (text.equals("bangla") || text.equals("english") || text.equals("both")) {
                    return text;
                } else if (text.equals("fixed fee")) {
                    return "fixed";
                } else if (text.equals("ranged fee")) {
                    return "ranged";
                }
            }
        }
        return "";
    }

    private void uploadImageAndSave(Map<String, Object> data, String documentName) {
        showProgressDialog("Uploading image...");
        startTimeout("Image upload timeout");

        String publicId = CloudinaryConfig.generateProfileDocumentPublicId(userId, "profile_image");

        try {
            MediaManager.get().upload(selectedImageUri)
                    .option("folder", CloudinaryConfig.getLawyerProfileFolder(userId))
                    .option("public_id", publicId)
                    .option("resource_type", "auto")
                    .option("use_filename", true)
                    .option("unique_filename", false)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Profile image upload started");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Update progress if needed
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            runOnUiThread(() -> {
                                cancelTimeout();
                                String imageUrl = (String) resultData.get("secure_url");
                                data.put("profileImageUrl", imageUrl);
                                dismissProgressDialog();
                                saveToFirestore(data, documentName);
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                cancelTimeout();
                                dismissProgressDialog();
                                showToast("Image upload failed: " + error.getDescription());
                                // Save without image URL
                                saveToFirestore(data, documentName);
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            // Handle reschedule if needed
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            cancelTimeout();
            dismissProgressDialog();
            showToast("Image upload failed: " + e.getMessage());
            saveToFirestore(data, documentName);
        }
    }

    private void saveToFirestore(Map<String, Object> data, String documentName) {
        showProgressDialog("Saving changes...");
        startTimeout("Save timeout");

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document(documentName)
                .set(data)
                .addOnCompleteListener(task -> {
                    runOnUiThread(() -> {
                        cancelTimeout();
                        dismissProgressDialog();

                        if (task.isSuccessful()) {
                            showToast("Changes saved successfully!");
                            if (currentSectionData != null) {
                                currentSectionData.putAll(data);
                            }
                        } else {
                            showToast("Failed to save changes: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            Log.e(TAG, "Error saving data", task.getException());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        cancelTimeout();
                        dismissProgressDialog();
                        Log.e(TAG, "Failed to save data", e);
                        showToast("Network error. Please try again.");
                    });
                });
    }

    // Image and document selection methods
    private void selectImage() {
        if (hasStoragePermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imageLauncher.launch(intent);
        } else {
            checkStoragePermissions();
        }
    }

    private void selectDocument() {
        if (hasStoragePermission()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {"application/pdf", "image/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            documentLauncher.launch(intent);
        } else {
            checkStoragePermissions();
        }
    }

    private void handleImageSelection(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedUri = result.getData().getData();
            if (selectedUri != null) {
                selectedImageUri = selectedUri;
                if (currentProfileImage != null) {
                    Glide.with(this).load(selectedUri).into(currentProfileImage);
                }
                showToast("Image selected. Save to upload.");
            }
        }
    }

    private void handleDocumentSelection(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedUri = result.getData().getData();
            if (selectedUri != null) {
                showToast("Document selected: " + selectedUri.getLastPathSegment());
                // Handle document upload logic here
            }
        }
    }

    // Date and time picker methods
    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        parseCurrentDate(editText.getText().toString().trim(), calendar);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                    editText.setText(date);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void showTimePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        parseCurrentTime(editText.getText().toString().trim(), calendar);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    editText.setText(time);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

        timePickerDialog.show();
    }

    private void showEnrollmentYearPicker(TextInputEditText editText) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int defaultYear = parseYear(editText.getText().toString().trim(), currentYear);

        YearPickerDialog.showEnrollmentYearPicker(this, defaultYear, year ->
                editText.setText(String.valueOf(year)));
    }

    private void showGraduationYearPicker(TextInputEditText editText) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int defaultYear = parseYear(editText.getText().toString().trim(), currentYear);

        YearPickerDialog.showGraduationYearPicker(this, defaultYear, year ->
                editText.setText(String.valueOf(year)));
    }

    // Security methods
    private void changePassword(TextInputEditText currentPasswordEditText,
                                TextInputEditText newPasswordEditText,
                                TextInputEditText confirmPasswordEditText) {

        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            showToast("Please enter your current password");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            showToast("Please enter a new password");
            return;
        }

        if (newPassword.length() < 6) {
            showToast("New password must be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showToast("New passwords do not match");
            return;
        }

        // Implement password change logic with Firebase Auth
        showToast("Password change functionality will be implemented");
    }

    private void logoutFromOtherDevices() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout from Other Devices")
                .setMessage("Are you sure you want to logout from all other devices?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Implement logout from other devices logic
                    showToast("Logged out from other devices");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Implement account deletion logic
                    showToast("Account deletion functionality will be implemented");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Permission and utility methods
    private void checkStoragePermissions() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_CODE);
        }
    }

    private boolean hasStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void parseCurrentDate(String currentDate, Calendar calendar) {
        if (!TextUtils.isEmpty(currentDate)) {
            try {
                String[] dateParts = currentDate.split("/");
                if (dateParts.length == 3) {
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
                    calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[2]));
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse date: " + currentDate);
            }
        }
    }

    private void parseCurrentTime(String currentTime, Calendar calendar) {
        if (!TextUtils.isEmpty(currentTime)) {
            try {
                String[] timeParts = currentTime.split(":");
                if (timeParts.length == 2) {
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse time: " + currentTime);
            }
        }
    }

    private int parseYear(String yearString, int defaultYear) {
        if (!TextUtils.isEmpty(yearString)) {
            try {
                return Integer.parseInt(yearString);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Failed to parse year: " + yearString);
            }
        }
        return defaultYear;
    }

    // Fixed Progress Dialog Methods - Using MaterialAlertDialog
    private void showProgressDialog(String message) {
        dismissProgressDialog();

        try {
            View progressView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
            TextView textView = progressView.findViewById(android.R.id.text1);
            textView.setText(message);
            textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
            textView.setCompoundDrawablePadding(16);

            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(48, 32, 48, 32);
            layout.addView(progressBar);
            layout.addView(textView);

            currentProgressDialog = new MaterialAlertDialogBuilder(this)
                    .setView(layout)
                    .setCancelable(false)
                    .create();

            currentProgressDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing progress dialog", e);
        }
    }

    private void dismissProgressDialog() {
        try {
            if (currentProgressDialog != null && currentProgressDialog.isShowing()) {
                currentProgressDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing progress dialog", e);
        } finally {
            currentProgressDialog = null;
        }
    }

    // Timeout management methods
    private void startTimeout(String timeoutMessage) {
        cancelTimeout();
        timeoutRunnable = () -> {
            Log.w(TAG, timeoutMessage);
            dismissProgressDialog();
            showToast("Operation timed out. Please try again.");
        };
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null && mainHandler != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Storage permission granted");
            } else {
                showToast("Storage permission is required to select images and documents");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        cancelTimeout();

        // Clear handlers to prevent memory leaks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dismiss progress dialog when activity is paused to prevent window leaks
        dismissProgressDialog();
    }
}