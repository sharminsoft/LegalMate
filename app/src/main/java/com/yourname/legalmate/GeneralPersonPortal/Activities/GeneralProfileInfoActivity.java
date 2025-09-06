package com.yourname.legalmate.GeneralPersonPortal.Activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeneralProfileInfoActivity extends AppCompatActivity {

    private static final String TAG = "GeneralProfileInfo";

    // UI Components
    private Toolbar toolbar;
    private LinearLayout contentContainer;
    private ProgressDialog progressDialog;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    // Data
    private String sectionName;
    private String userId;
    private String userEmail;
    private Map<String, Object> userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_profile_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeData();
        initializeFirebase();
        initializeViews();
        setupToolbar();
        loadUserData();
    }

    private void initializeData() {
        Intent intent = getIntent();
        sectionName = intent.getStringExtra("section");
        userId = intent.getStringExtra("userId");
        userEmail = intent.getStringExtra("email");

        if (sectionName == null) sectionName = "Profile Information";
        if (userId == null || userId.isEmpty()) {
            finish();
            return;
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        contentContainer = findViewById(R.id.contentContainer);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(sectionName);
        }
    }

    private void loadUserData() {
        showProgressDialog("Loading profile data...");

        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .get()
                .addOnSuccessListener(this::handleDataLoaded)
                .addOnFailureListener(this::handleDataLoadError);
    }

    private void handleDataLoaded(DocumentSnapshot document) {
        hideProgressDialog();

        if (document.exists()) {
            userData = document.getData();
            if (userData == null) userData = new HashMap<>();
        } else {
            userData = new HashMap<>();
            Log.d(TAG, "User document doesn't exist, creating default data");
        }

        populateContent();
    }

    private void handleDataLoadError(Exception e) {
        hideProgressDialog();
        Log.e(TAG, "Error loading user data", e);
        Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
        userData = new HashMap<>();
        populateContent();
    }

    private void populateContent() {
        contentContainer.removeAllViews();

        switch (sectionName) {
            case "Personal Information":
                createPersonalInformationSection();
                break;
            case "Account Settings":
                createAccountSettingsSection();
                break;
            case "Payment History":
                createPaymentHistorySection();
                break;
            case "Notification Settings":
                createNotificationSettingsSection();
                break;
            case "Support and Help Center":
                createSupportHelpSection();
                break;
            default:
                createPersonalInformationSection();
                break;
        }
    }

    private void createPersonalInformationSection() {
        // Profile Header Card
        MaterialCardView headerCard = createCard();
        LinearLayout headerLayout = createVerticalLayout();

        TextView headerTitle = createSectionTitle("Profile Information");
        TextView headerSubtitle = createSubtitle("Manage your personal details");

        headerLayout.addView(headerTitle);
        headerLayout.addView(headerSubtitle);
        headerCard.addView(headerLayout);
        contentContainer.addView(headerCard);

        // Personal Details Card
        MaterialCardView detailsCard = createCard();
        LinearLayout detailsLayout = createVerticalLayout();

        // Full Name
        TextInputLayout nameLayout = createTextInputLayout("Full Name");
        TextInputEditText nameEditText = createEditText();
        nameEditText.setText(getStringFromUserData("name", ""));
        nameLayout.addView(nameEditText);
        detailsLayout.addView(nameLayout);

        // Email (Read-only)
        TextInputLayout emailLayout = createTextInputLayout("Email Address");
        TextInputEditText emailEditText = createEditText();
        emailEditText.setText(userEmail);
        emailEditText.setEnabled(false);
        emailLayout.addView(emailEditText);
        detailsLayout.addView(emailLayout);

        // Phone Number
        TextInputLayout phoneLayout = createTextInputLayout("Phone Number");
        TextInputEditText phoneEditText = createEditText();
        phoneEditText.setText(getStringFromUserData("phone", ""));
        phoneLayout.addView(phoneEditText);
        detailsLayout.addView(phoneLayout);

        // Date of Birth
        TextInputLayout dobLayout = createTextInputLayout("Date of Birth");
        TextInputEditText dobEditText = createEditText();
        dobEditText.setText(getStringFromUserData("dateOfBirth", ""));
        dobEditText.setOnClickListener(v -> showDatePicker(dobEditText));
        dobLayout.addView(dobEditText);
        detailsLayout.addView(dobLayout);

        // Address
        TextInputLayout addressLayout = createTextInputLayout("Address");
        TextInputEditText addressEditText = createEditText();
        addressEditText.setText(getStringFromUserData("address", ""));
        addressLayout.addView(addressEditText);
        detailsLayout.addView(addressLayout);

        // City
        TextInputLayout cityLayout = createTextInputLayout("City");
        TextInputEditText cityEditText = createEditText();
        cityEditText.setText(getStringFromUserData("city", ""));
        cityLayout.addView(cityEditText);
        detailsLayout.addView(cityLayout);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> savePersonalInformation(
                nameEditText.getText().toString().trim(),
                phoneEditText.getText().toString().trim(),
                dobEditText.getText().toString().trim(),
                addressEditText.getText().toString().trim(),
                cityEditText.getText().toString().trim()
        ));
        detailsLayout.addView(saveButton);

        detailsCard.addView(detailsLayout);
        contentContainer.addView(detailsCard);
    }

    private void createAccountSettingsSection() {
        // Account Settings Header
        MaterialCardView headerCard = createCard();
        LinearLayout headerLayout = createVerticalLayout();

        TextView headerTitle = createSectionTitle("Account Settings");
        TextView headerSubtitle = createSubtitle("Manage your account preferences");

        headerLayout.addView(headerTitle);
        headerLayout.addView(headerSubtitle);
        headerCard.addView(headerLayout);
        contentContainer.addView(headerCard);

        // Account Information Card
        MaterialCardView accountCard = createCard();
        LinearLayout accountLayout = createVerticalLayout();

        TextView accountTitle = createCardTitle("Account Information");
        accountLayout.addView(accountTitle);

        // Account Status
        LinearLayout statusLayout = createHorizontalLayout();
        TextView statusLabel = createLabel("Account Status:");
        TextView statusValue = createValue("Active");
        statusLayout.addView(statusLabel);
        statusLayout.addView(statusValue);
        accountLayout.addView(statusLayout);

        // Member Since
        LinearLayout memberLayout = createHorizontalLayout();
        TextView memberLabel = createLabel("Member Since:");
        TextView memberValue = createValue(getFormattedDate("createdAt"));
        memberLayout.addView(memberLabel);
        memberLayout.addView(memberValue);
        accountLayout.addView(memberLayout);

        // Sign-in Method
        LinearLayout methodLayout = createHorizontalLayout();
        TextView methodLabel = createLabel("Sign-in Method:");
        TextView methodValue = createValue(getStringFromUserData("signInMethod", "email"));
        methodLayout.addView(methodLabel);
        methodLayout.addView(methodValue);
        accountLayout.addView(methodLayout);

        accountCard.addView(accountLayout);
        contentContainer.addView(accountCard);

        // Security Settings Card
        MaterialCardView securityCard = createCard();
        LinearLayout securityLayout = createVerticalLayout();

        TextView securityTitle = createCardTitle("Security Settings");
        securityLayout.addView(securityTitle);

        // Change Password Button (only for email users)
        if ("email".equals(getStringFromUserData("signInMethod", ""))) {
            MaterialButton changePasswordButton = createActionButton("Change Password");
            changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
            securityLayout.addView(changePasswordButton);
        }

        // Delete Account Button
        MaterialButton deleteAccountButton = createActionButton("Delete Account");
        deleteAccountButton.setBackgroundTintList(getColorStateList(android.R.color.holo_red_dark));
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
        securityLayout.addView(deleteAccountButton);

        securityCard.addView(securityLayout);
        contentContainer.addView(securityCard);
    }

    private void createPaymentHistorySection() {
        // Payment History Header
        MaterialCardView headerCard = createCard();
        LinearLayout headerLayout = createVerticalLayout();

        TextView headerTitle = createSectionTitle("Payment History");
        TextView headerSubtitle = createSubtitle("View your transaction history");

        headerLayout.addView(headerTitle);
        headerLayout.addView(headerSubtitle);
        headerCard.addView(headerLayout);
        contentContainer.addView(headerCard);

        // No Payments Card
        MaterialCardView noPaymentsCard = createCard();
        LinearLayout noPaymentsLayout = createVerticalLayout();
        noPaymentsLayout.setGravity(android.view.Gravity.CENTER);

        TextView noPaymentsTitle = createCardTitle("No Payment History");
        TextView noPaymentsText = createSubtitle("You haven't made any payments yet.");

        noPaymentsLayout.addView(noPaymentsTitle);
        noPaymentsLayout.addView(noPaymentsText);
        noPaymentsCard.addView(noPaymentsLayout);
        contentContainer.addView(noPaymentsCard);
    }

    private void createNotificationSettingsSection() {
        // Notification Settings Header
        MaterialCardView headerCard = createCard();
        LinearLayout headerLayout = createVerticalLayout();

        TextView headerTitle = createSectionTitle("Notification Settings");
        TextView headerSubtitle = createSubtitle("Manage your notification preferences");

        headerLayout.addView(headerTitle);
        headerLayout.addView(headerSubtitle);
        headerCard.addView(headerLayout);
        contentContainer.addView(headerCard);

        // Notification Preferences Card
        MaterialCardView preferencesCard = createCard();
        LinearLayout preferencesLayout = createVerticalLayout();

        TextView preferencesTitle = createCardTitle("Notification Preferences");
        preferencesLayout.addView(preferencesTitle);

        // Email Notifications
        LinearLayout emailNotifLayout = createSwitchLayout(
                "Email Notifications",
                "Receive updates via email",
                getBooleanFromUserData("emailNotifications", true)
        );
        preferencesLayout.addView(emailNotifLayout);

        // Push Notifications
        LinearLayout pushNotifLayout = createSwitchLayout(
                "Push Notifications",
                "Receive push notifications on your device",
                getBooleanFromUserData("pushNotifications", true)
        );
        preferencesLayout.addView(pushNotifLayout);

        // Case Updates
        LinearLayout caseUpdatesLayout = createSwitchLayout(
                "Case Updates",
                "Get notified about case status changes",
                getBooleanFromUserData("caseUpdates", true)
        );
        preferencesLayout.addView(caseUpdatesLayout);

        // Marketing Emails
        LinearLayout marketingLayout = createSwitchLayout(
                "Marketing Emails",
                "Receive promotional offers and updates",
                getBooleanFromUserData("marketingEmails", false)
        );
        preferencesLayout.addView(marketingLayout);

        // Save Button
        MaterialButton saveButton = createSaveButton();
        saveButton.setOnClickListener(v -> saveNotificationSettings(
                ((SwitchCompat) emailNotifLayout.findViewWithTag("switch")).isChecked(),
                ((SwitchCompat) pushNotifLayout.findViewWithTag("switch")).isChecked(),
                ((SwitchCompat) caseUpdatesLayout.findViewWithTag("switch")).isChecked(),
                ((SwitchCompat) marketingLayout.findViewWithTag("switch")).isChecked()
        ));
        preferencesLayout.addView(saveButton);

        preferencesCard.addView(preferencesLayout);
        contentContainer.addView(preferencesCard);
    }

    private void createSupportHelpSection() {
        // Support Header
        MaterialCardView headerCard = createCard();
        LinearLayout headerLayout = createVerticalLayout();

        TextView headerTitle = createSectionTitle("Support & Help Center");
        TextView headerSubtitle = createSubtitle("Get help and contact support");

        headerLayout.addView(headerTitle);
        headerLayout.addView(headerSubtitle);
        headerCard.addView(headerLayout);
        contentContainer.addView(headerCard);

        // Contact Support Card
        MaterialCardView contactCard = createCard();
        LinearLayout contactLayout = createVerticalLayout();

        TextView contactTitle = createCardTitle("Contact Support");
        contactLayout.addView(contactTitle);

        MaterialButton emailSupportButton = createActionButton("Email Support");
        emailSupportButton.setOnClickListener(v -> sendSupportEmail());
        contactLayout.addView(emailSupportButton);

        MaterialButton callSupportButton = createActionButton("Call Support");
        callSupportButton.setOnClickListener(v -> callSupport());
        contactLayout.addView(callSupportButton);

        contactCard.addView(contactLayout);
        contentContainer.addView(contactCard);

        // FAQ Card
        MaterialCardView faqCard = createCard();
        LinearLayout faqLayout = createVerticalLayout();

        TextView faqTitle = createCardTitle("Frequently Asked Questions");
        faqLayout.addView(faqTitle);

        MaterialButton viewFaqButton = createActionButton("View FAQ");
        viewFaqButton.setOnClickListener(v -> openFAQ());
        faqLayout.addView(viewFaqButton);

        faqCard.addView(faqLayout);
        contentContainer.addView(faqCard);
    }

    // Helper Methods for UI Creation
    private MaterialCardView createCard() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(16));
        card.setLayoutParams(params);
        card.setCardElevation(dpToPx(4));
        card.setRadius(dpToPx(12));
        card.setContentPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        card.setCardBackgroundColor(getColor(android.R.color.white));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(getColor(android.R.color.transparent));
        return card;
    }

    private LinearLayout createVerticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return layout;
    }

    private LinearLayout createHorizontalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        layout.setLayoutParams(params);
        return layout;
    }

    private TextView createSectionTitle(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(28);
        textView.setTextColor(getColor(android.R.color.black));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        textView.setLayoutParams(params);
        textView.setLetterSpacing(0.01f);
        return textView;
    }

    private TextView createSubtitle(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(getColor(android.R.color.darker_gray));
        textView.setAlpha(0.8f);
        return textView;
    }

    private TextView createCardTitle(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(18);
        textView.setTextColor(getColor(android.R.color.black));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(16));
        textView.setLayoutParams(params);
        return textView;
    }

    private TextView createLabel(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(getColor(android.R.color.darker_gray));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textView.setLayoutParams(params);
        return textView;
    }

    private TextView createValue(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(getColor(android.R.color.black));
        textView.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textView.setLayoutParams(params);
        return textView;
    }

    private TextInputLayout createTextInputLayout(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(16));
        layout.setLayoutParams(params);
        return layout;
    }

    private TextInputEditText createEditText() {
        TextInputEditText editText = new TextInputEditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return editText;
    }

    private MaterialButton createSaveButton() {
        MaterialButton button = new MaterialButton(this);
        button.setText("Save Changes");
        button.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_dark));
        button.setTextColor(getColor(android.R.color.white));
        button.setCornerRadius(dpToPx(12));
        button.setTextSize(16);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setAllCaps(false);
        button.setElevation(dpToPx(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
        );
        params.setMargins(0, dpToPx(32), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private MaterialButton createActionButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_dark));
        button.setTextColor(getColor(android.R.color.white));
        button.setCornerRadius(dpToPx(12));
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setElevation(dpToPx(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(52)
        );
        params.setMargins(0, 0, 0, dpToPx(12));
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout createSwitchLayout(String title, String subtitle, boolean isChecked) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dpToPx(12), 0, dpToPx(12));
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        container.setLayoutParams(containerParams);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textLayout.setLayoutParams(textParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(getColor(android.R.color.black));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(12);
        subtitleView.setTextColor(getColor(android.R.color.darker_gray));

        textLayout.addView(titleView);
        textLayout.addView(subtitleView);

        SwitchCompat switchView = new SwitchCompat(this);
        switchView.setChecked(isChecked);
        switchView.setTag("switch");

        container.addView(textLayout);
        container.addView(switchView);

        return container;
    }

    // Data Helper Methods
    private String getStringFromUserData(String key, String defaultValue) {
        if (userData != null && userData.containsKey(key)) {
            Object value = userData.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        return defaultValue;
    }

    private boolean getBooleanFromUserData(String key, boolean defaultValue) {
        if (userData != null && userData.containsKey(key)) {
            Object value = userData.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return defaultValue;
    }

    private String getFormattedDate(String key) {
        if (userData != null && userData.containsKey(key)) {
            Object value = userData.get(key);
            if (value instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) value;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return sdf.format(timestamp.toDate());
            }
        }
        return "N/A";
    }

    // Action Methods
    private void savePersonalInformation(String name, String phone, String dob, String address, String city) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Saving changes...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("dateOfBirth", dob);
        updates.put("address", address);
        updates.put("city", city);
        updates.put("profileComplete", true);
        updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Update local data
                    userData.putAll(updates);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNotificationSettings(boolean email, boolean push, boolean caseUpdates, boolean marketing) {
        showProgressDialog("Saving notification settings...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("emailNotifications", email);
        updates.put("pushNotifications", push);
        updates.put("caseUpdates", caseUpdates);
        updates.put("marketingEmails", marketing);
        updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Notification settings updated", Toast.LENGTH_SHORT).show();
                    userData.putAll(updates);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e(TAG, "Error updating notification settings", e);
                    Toast.makeText(this, "Error updating settings", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    editText.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showChangePasswordDialog() {
        // Implementation for password change dialog
        Toast.makeText(this, "Change password feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        showProgressDialog("Deleting account...");

        // First delete Firestore data
        firestore.collection("Users")
                .document("GeneralPersons")
                .collection("GeneralPersons")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Then delete Firebase Auth user
                    if (currentUser != null) {
                        currentUser.delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    hideProgressDialog();
                                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                    // Navigate to login
                                    Intent intent = new Intent(this, com.yourname.legalmate.OuterActivities.LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    hideProgressDialog();
                                    Log.e(TAG, "Error deleting user account", e);
                                    Toast.makeText(this, "Error deleting account", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e(TAG, "Error deleting user data", e);
                    Toast.makeText(this, "Error deleting account data", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSupportEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@legalmate.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - LegalMate");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi,\n\nI need help with:\n\n[Please describe your issue here]\n\nUser ID: " + userId + "\nEmail: " + userEmail);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void callSupport() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:+1234567890"));
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFAQ() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://legalmate.com/faq"));
        try {
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open FAQ", Toast.LENGTH_SHORT).show();
        }
    }

    // Utility Methods
    private void showProgressDialog(String message) {
        if (progressDialog != null) {
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}