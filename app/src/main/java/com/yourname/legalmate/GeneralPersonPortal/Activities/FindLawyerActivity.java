package com.yourname.legalmate.GeneralPersonPortal.Activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yourname.legalmate.Chat.ChatActivity;
import com.yourname.legalmate.GeneralPersonPortal.Adapters.LawyerAdapter;
import com.yourname.legalmate.GeneralPersonPortal.Models.LawyerModel;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FindLawyerActivity extends AppCompatActivity implements LawyerAdapter.OnLawyerClickListener {

    private static final String TAG = "FindLawyerActivity";

    // UI Components
    private ImageButton btnBack;
    private TextInputEditText etSearch;
    private MaterialButton btnFilter;
    private TextView tvResultsCount, tvSort;
    private RecyclerView rvLawyers;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;

    // Filter Dialog Components (will be created programmatically)
    private AlertDialog filterDialog;
    private MaterialCheckBox cbCriminalLaw, cbCivilLaw, cbFamilyLaw, cbCorporateLaw, cbPropertyLaw;
    private MaterialCheckBox cbOnline, cbInPerson, cbPhone, cbChat;
    private MaterialCheckBox cbSaturday, cbSunday, cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday;
    private MaterialRadioButton rbAllFees, rbUnder1000, rb1000to3000, rbAbove3000;
    private MaterialRadioButton rbAllExp, rbUnder5Years, rb5to10Years, rbAbove10Years;
    private MaterialRadioButton rbMale, rbFemale, rbAnyGender;

    // Sort Dialog Components
    private AlertDialog sortDialog;
    private MaterialRadioButton rbSortRating, rbSortExperience, rbSortFee, rbSortName;

    // Data and Adapters
    private FirebaseFirestore db;
    private LawyerAdapter lawyerAdapter;
    private List<LawyerModel> allLawyers;
    private List<LawyerModel> filteredLawyers;
    private ExecutorService searchExecutor;

    // Search and Filter Variables
    private String currentSearchQuery = "";
    private FilterCriteria currentFilter = new FilterCriteria();
    private String currentSortBy = "rating"; // Default sort

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_lawyer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupRecyclerView();
        setupClickListeners();
        setupSearchListener();
        loadLawyers();
    }

    private void initializeComponents() {
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        searchExecutor = Executors.newSingleThreadExecutor();

        // Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        tvSort = findViewById(R.id.tvSort);
        rvLawyers = findViewById(R.id.rvLawyers);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);

        // Initialize data lists
        allLawyers = new ArrayList<>();
        filteredLawyers = new ArrayList<>();
    }

    private void setupRecyclerView() {
        lawyerAdapter = new LawyerAdapter(this, filteredLawyers, this);
        rvLawyers.setLayoutManager(new LinearLayoutManager(this));
        rvLawyers.setAdapter(lawyerAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnFilter.setOnClickListener(v -> showFilterDialog());
        tvSort.setOnClickListener(v -> showSortDialog());
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                performSearch();
            }
        });
    }

    private void loadLawyers() {
        showLoading(true);

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allLawyers.clear();

                    for (QueryDocumentSnapshot lawyerDoc : queryDocumentSnapshots) {
                        String lawyerId = lawyerDoc.getId();
                        loadLawyerProfile(lawyerId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyers", e);
                    showLoading(false);
                    showToast("Failed to load lawyers");
                });
    }

    private void loadLawyerProfile(String lawyerId) {
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(lawyerId)
                .collection("ProfileData")
                .get()
                .addOnSuccessListener(profileSnapshots -> {
                    LawyerModel lawyer = buildLawyerFromProfile(lawyerId, profileSnapshots.getDocuments());

                    if (lawyer != null && lawyer.isProfileComplete() && lawyer.isVerified()) {
                        allLawyers.add(lawyer);

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            performSearch();
                            showLoading(false);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile for lawyer: " + lawyerId, e);
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

        // Rating get করার code add করুন:
        Double rating = doc.getDouble("rating");
        lawyer.setRating(rating != null ? rating : 0.0);

        // totalReviews field get করার code add:
        Long totalReviews = doc.getLong("totalReviews");
        lawyer.setReviewCount(totalReviews != null ? totalReviews.intValue() : 0);
    }

    private void buildAppointmentSettings(LawyerModel lawyer, DocumentSnapshot doc) {
        lawyer.setAppointmentBookingEnabled(doc.getBoolean("appointmentBookingEnabled") != null ?
                doc.getBoolean("appointmentBookingEnabled") : false);
    }



    private void performSearch() {
        searchExecutor.execute(() -> {
            List<LawyerModel> results = new ArrayList<>();

            for (LawyerModel lawyer : allLawyers) {
                if (matchesSearchCriteria(lawyer)) {
                    results.add(lawyer);
                }
            }

            // Sort results
            sortLawyers(results, currentSortBy);

            runOnUiThread(() -> {
                filteredLawyers.clear();
                filteredLawyers.addAll(results);
                lawyerAdapter.notifyDataSetChanged();
                updateResultsCount();
                updateEmptyState();
            });
        });
    }

    private boolean matchesSearchCriteria(LawyerModel lawyer) {
        // Search query check
        if (!TextUtils.isEmpty(currentSearchQuery)) {
            String query = currentSearchQuery.toLowerCase();
            String name = lawyer.getFullName() != null ? lawyer.getFullName().toLowerCase() : "";
            String bio = lawyer.getShortBio() != null ? lawyer.getShortBio().toLowerCase() : "";
            String chamber = lawyer.getChamberName() != null ? lawyer.getChamberName().toLowerCase() : "";

            boolean nameMatch = name.contains(query);
            boolean bioMatch = bio.contains(query);
            boolean chamberMatch = chamber.contains(query);
            boolean practiceAreaMatch = false;

            if (lawyer.getPracticeAreas() != null) {
                for (String area : lawyer.getPracticeAreas()) {
                    if (area.toLowerCase().contains(query)) {
                        practiceAreaMatch = true;
                        break;
                    }
                }
            }

            if (!nameMatch && !bioMatch && !chamberMatch && !practiceAreaMatch) {
                return false;
            }
        }

        // Apply filters
        return matchesFilter(lawyer);
    }

    private boolean matchesFilter(LawyerModel lawyer) {
        // Practice Area Filter
        if (currentFilter.hasSelectedPracticeAreas()) {
            boolean hasMatchingPracticeArea = false;
            if (lawyer.getPracticeAreas() != null) {
                for (String area : lawyer.getPracticeAreas()) {
                    if (currentFilter.selectedPracticeAreas.contains(area)) {
                        hasMatchingPracticeArea = true;
                        break;
                    }
                }
            }
            if (!hasMatchingPracticeArea) return false;
        }

        // Consultation Type Filter
        if (currentFilter.hasSelectedConsultationTypes()) {
            boolean hasMatchingConsultationType = false;
            if (lawyer.getConsultationTypes() != null) {
                for (String type : lawyer.getConsultationTypes()) {
                    if (currentFilter.selectedConsultationTypes.contains(type)) {
                        hasMatchingConsultationType = true;
                        break;
                    }
                }
            }
            if (!hasMatchingConsultationType) return false;
        }

        // Working Days Filter
        if (currentFilter.hasSelectedWorkingDays()) {
            boolean hasMatchingWorkingDay = false;
            if (lawyer.getWorkingDays() != null) {
                for (String day : lawyer.getWorkingDays()) {
                    if (currentFilter.selectedWorkingDays.contains(day)) {
                        hasMatchingWorkingDay = true;
                        break;
                    }
                }
            }
            if (!hasMatchingWorkingDay) return false;
        }

        // Fee Range Filter
        if (!currentFilter.feeRange.equals("all")) {
            double fee = getConsultationFee(lawyer);
            switch (currentFilter.feeRange) {
                case "under1000":
                    if (fee >= 1000) return false;
                    break;
                case "1000to3000":
                    if (fee < 1000 || fee > 3000) return false;
                    break;
                case "above3000":
                    if (fee <= 3000) return false;
                    break;
            }
        }

        // Experience Filter
        if (!currentFilter.experienceRange.equals("all")) {
            int experience = getExperienceYears(lawyer);
            switch (currentFilter.experienceRange) {
                case "under5":
                    if (experience >= 5) return false;
                    break;
                case "5to10":
                    if (experience < 5 || experience > 10) return false;
                    break;
                case "above10":
                    if (experience <= 10) return false;
                    break;
            }
        }

        // Gender Filter
        if (!currentFilter.gender.equals("any")) {
            if (!currentFilter.gender.equals(lawyer.getGender())) return false;
        }

        return true;
    }

    private double getConsultationFee(LawyerModel lawyer) {
        try {
            if ("fixed".equals(lawyer.getFeeType()) && lawyer.getFixedFee() != null) {
                return Double.parseDouble(lawyer.getFixedFee());
            } else if ("ranged".equals(lawyer.getFeeType()) && lawyer.getMinFee() != null) {
                return Double.parseDouble(lawyer.getMinFee());
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse fee for lawyer: " + lawyer.getLawyerId());
        }
        return 0;
    }

    private int getExperienceYears(LawyerModel lawyer) {
        try {
            if (lawyer.getExperience() != null) {
                return Integer.parseInt(lawyer.getExperience());
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse experience for lawyer: " + lawyer.getLawyerId());
        }
        return 0;
    }

    private void sortLawyers(List<LawyerModel> lawyers, String sortBy) {
        switch (sortBy) {
            case "rating":
                Collections.sort(lawyers, (l1, l2) -> Double.compare(l2.getRating(), l1.getRating()));
                break;
            case "experience":
                Collections.sort(lawyers, (l1, l2) -> Integer.compare(getExperienceYears(l2), getExperienceYears(l1)));
                break;
            case "fee":
                Collections.sort(lawyers, (l1, l2) -> Double.compare(getConsultationFee(l1), getConsultationFee(l2)));
                break;
            case "name":
                Collections.sort(lawyers, (l1, l2) -> {
                    String name1 = l1.getFullName() != null ? l1.getFullName() : "";
                    String name2 = l2.getFullName() != null ? l2.getFullName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_lawyer_filter, null);

        initializeFilterDialogComponents(dialogView);
        setCurrentFilterValues();

        builder.setView(dialogView)
                .setTitle("Filter Lawyers")
                .setPositiveButton("Apply", (dialog, which) -> {
                    applyFilters();
                    performSearch();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear All", (dialog, which) -> {
                    clearAllFilters();
                    performSearch();
                });

        filterDialog = builder.create();
        filterDialog.show();
    }

    private void initializeFilterDialogComponents(View dialogView) {
        // Practice Areas
        cbCriminalLaw = dialogView.findViewById(R.id.cbCriminalLaw);
        cbCivilLaw = dialogView.findViewById(R.id.cbCivilLaw);
        cbFamilyLaw = dialogView.findViewById(R.id.cbFamilyLaw);
        cbCorporateLaw = dialogView.findViewById(R.id.cbCorporateLaw);
        cbPropertyLaw = dialogView.findViewById(R.id.cbPropertyLaw);

        // Consultation Types
        cbOnline = dialogView.findViewById(R.id.cbOnline);
        cbInPerson = dialogView.findViewById(R.id.cbInPerson);
        cbPhone = dialogView.findViewById(R.id.cbPhone);
        cbChat = dialogView.findViewById(R.id.cbChat);

        // Working Days
        cbSaturday = dialogView.findViewById(R.id.cbSaturday);
        cbSunday = dialogView.findViewById(R.id.cbSunday);
        cbMonday = dialogView.findViewById(R.id.cbMonday);
        cbTuesday = dialogView.findViewById(R.id.cbTuesday);
        cbWednesday = dialogView.findViewById(R.id.cbWednesday);
        cbThursday = dialogView.findViewById(R.id.cbThursday);
        cbFriday = dialogView.findViewById(R.id.cbFriday);

        // Fee Range
        rbAllFees = dialogView.findViewById(R.id.rbAllFees);
        rbUnder1000 = dialogView.findViewById(R.id.rbUnder1000);
        rb1000to3000 = dialogView.findViewById(R.id.rb1000to3000);
        rbAbove3000 = dialogView.findViewById(R.id.rbAbove3000);

        // Experience Range
        rbAllExp = dialogView.findViewById(R.id.rbAllExp);
        rbUnder5Years = dialogView.findViewById(R.id.rbUnder5Years);
        rb5to10Years = dialogView.findViewById(R.id.rb5to10Years);
        rbAbove10Years = dialogView.findViewById(R.id.rbAbove10Years);

        // Gender
        rbMale = dialogView.findViewById(R.id.rbMale);
        rbFemale = dialogView.findViewById(R.id.rbFemale);
        rbAnyGender = dialogView.findViewById(R.id.rbAnyGender);
    }

    private void setCurrentFilterValues() {
        // Set practice areas
        cbCriminalLaw.setChecked(currentFilter.selectedPracticeAreas.contains("Criminal Law"));
        cbCivilLaw.setChecked(currentFilter.selectedPracticeAreas.contains("Civil Law"));
        cbFamilyLaw.setChecked(currentFilter.selectedPracticeAreas.contains("Family Law"));
        cbCorporateLaw.setChecked(currentFilter.selectedPracticeAreas.contains("Corporate Law"));
        cbPropertyLaw.setChecked(currentFilter.selectedPracticeAreas.contains("Property Law"));

        // Set consultation types
        cbOnline.setChecked(currentFilter.selectedConsultationTypes.contains("Online"));
        cbInPerson.setChecked(currentFilter.selectedConsultationTypes.contains("In-Person"));
        cbPhone.setChecked(currentFilter.selectedConsultationTypes.contains("Phone"));
        cbChat.setChecked(currentFilter.selectedConsultationTypes.contains("Chat"));

        // Set working days
        cbSaturday.setChecked(currentFilter.selectedWorkingDays.contains("Saturday"));
        cbSunday.setChecked(currentFilter.selectedWorkingDays.contains("Sunday"));
        cbMonday.setChecked(currentFilter.selectedWorkingDays.contains("Monday"));
        cbTuesday.setChecked(currentFilter.selectedWorkingDays.contains("Tuesday"));
        cbWednesday.setChecked(currentFilter.selectedWorkingDays.contains("Wednesday"));
        cbThursday.setChecked(currentFilter.selectedWorkingDays.contains("Thursday"));
        cbFriday.setChecked(currentFilter.selectedWorkingDays.contains("Friday"));

        // Set fee range
        switch (currentFilter.feeRange) {
            case "all": rbAllFees.setChecked(true); break;
            case "under1000": rbUnder1000.setChecked(true); break;
            case "1000to3000": rb1000to3000.setChecked(true); break;
            case "above3000": rbAbove3000.setChecked(true); break;
        }

        // Set experience range
        switch (currentFilter.experienceRange) {
            case "all": rbAllExp.setChecked(true); break;
            case "under5": rbUnder5Years.setChecked(true); break;
            case "5to10": rb5to10Years.setChecked(true); break;
            case "above10": rbAbove10Years.setChecked(true); break;
        }

        // Set gender
        switch (currentFilter.gender) {
            case "any": rbAnyGender.setChecked(true); break;
            case "male": rbMale.setChecked(true); break;
            case "female": rbFemale.setChecked(true); break;
        }
    }

    private void applyFilters() {
        currentFilter = new FilterCriteria();

        // Practice Areas
        if (cbCriminalLaw.isChecked()) currentFilter.selectedPracticeAreas.add("Criminal Law");
        if (cbCivilLaw.isChecked()) currentFilter.selectedPracticeAreas.add("Civil Law");
        if (cbFamilyLaw.isChecked()) currentFilter.selectedPracticeAreas.add("Family Law");
        if (cbCorporateLaw.isChecked()) currentFilter.selectedPracticeAreas.add("Corporate Law");
        if (cbPropertyLaw.isChecked()) currentFilter.selectedPracticeAreas.add("Property Law");

        // Consultation Types
        if (cbOnline.isChecked()) currentFilter.selectedConsultationTypes.add("Online");
        if (cbInPerson.isChecked()) currentFilter.selectedConsultationTypes.add("In-Person");
        if (cbPhone.isChecked()) currentFilter.selectedConsultationTypes.add("Phone");
        if (cbChat.isChecked()) currentFilter.selectedConsultationTypes.add("Chat");

        // Working Days
        if (cbSaturday.isChecked()) currentFilter.selectedWorkingDays.add("Saturday");
        if (cbSunday.isChecked()) currentFilter.selectedWorkingDays.add("Sunday");
        if (cbMonday.isChecked()) currentFilter.selectedWorkingDays.add("Monday");
        if (cbTuesday.isChecked()) currentFilter.selectedWorkingDays.add("Tuesday");
        if (cbWednesday.isChecked()) currentFilter.selectedWorkingDays.add("Wednesday");
        if (cbThursday.isChecked()) currentFilter.selectedWorkingDays.add("Thursday");
        if (cbFriday.isChecked()) currentFilter.selectedWorkingDays.add("Friday");

        // Fee Range
        if (rbUnder1000.isChecked()) currentFilter.feeRange = "under1000";
        else if (rb1000to3000.isChecked()) currentFilter.feeRange = "1000to3000";
        else if (rbAbove3000.isChecked()) currentFilter.feeRange = "above3000";
        else currentFilter.feeRange = "all";

        // Experience Range
        if (rbUnder5Years.isChecked()) currentFilter.experienceRange = "under5";
        else if (rb5to10Years.isChecked()) currentFilter.experienceRange = "5to10";
        else if (rbAbove10Years.isChecked()) currentFilter.experienceRange = "above10";
        else currentFilter.experienceRange = "all";

        // Gender
        if (rbMale.isChecked()) currentFilter.gender = "male";
        else if (rbFemale.isChecked()) currentFilter.gender = "female";
        else currentFilter.gender = "any";
    }

    private void clearAllFilters() {
        currentFilter = new FilterCriteria();
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_lawyer_sort, null);

        rbSortRating = dialogView.findViewById(R.id.rbSortRating);
        rbSortExperience = dialogView.findViewById(R.id.rbSortExperience);
        rbSortFee = dialogView.findViewById(R.id.rbSortFee);
        rbSortName = dialogView.findViewById(R.id.rbSortName);

        // Set current selection
        switch (currentSortBy) {
            case "rating": rbSortRating.setChecked(true); break;
            case "experience": rbSortExperience.setChecked(true); break;
            case "fee": rbSortFee.setChecked(true); break;
            case "name": rbSortName.setChecked(true); break;
        }

        builder.setView(dialogView)
                .setTitle("Sort By")
                .setPositiveButton("Apply", (dialog, which) -> {
                    if (rbSortRating.isChecked()) currentSortBy = "rating";
                    else if (rbSortExperience.isChecked()) currentSortBy = "experience";
                    else if (rbSortFee.isChecked()) currentSortBy = "fee";
                    else if (rbSortName.isChecked()) currentSortBy = "name";

                    updateSortText();
                    performSearch();
                })
                .setNegativeButton("Cancel", null);

        sortDialog = builder.create();
        sortDialog.show();
    }

    private void updateSortText() {
        String sortText = "Sort by: ";
        switch (currentSortBy) {
            case "rating": sortText += "Rating"; break;
            case "experience": sortText += "Experience"; break;
            case "fee": sortText += "Fee"; break;
            case "name": sortText += "Name"; break;
        }
        tvSort.setText(sortText);
    }

    private void updateResultsCount() {
        int count = filteredLawyers.size();
        String countText = count + " lawyer" + (count != 1 ? "s" : "") + " found";
        tvResultsCount.setText(countText);
    }

    private void updateEmptyState() {
        if (filteredLawyers.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            rvLawyers.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            rvLawyers.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvLawyers.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLawyerClick(LawyerModel lawyer) {
        // Navigate to lawyer profile detail activity
        Intent intent = new Intent(this, LawyerProfileDetailActivity.class);
        intent.putExtra("lawyer_id", lawyer.getLawyerId());
        startActivity(intent);
    }

    @Override
    public void onMessageClick(LawyerModel lawyer) {
        // Navigate to chat activity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("lawyer_id", lawyer.getLawyerId());
        intent.putExtra("lawyer_name", lawyer.getFullName());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(LawyerModel lawyer, int position) {
        // Toggle favorite status
        lawyer.setFavorite(!lawyer.isFavorite());
        lawyerAdapter.notifyItemChanged(position);

        String message = lawyer.isFavorite() ? "Added to favorites" : "Removed from favorites";
        showToast(message);

        // TODO: Save favorite status to database
        saveFavoriteStatus(lawyer);
    }

    private void saveFavoriteStatus(LawyerModel lawyer) {
        // Implementation for saving favorite status to Firebase
        // This would typically save to current user's favorites collection
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchExecutor != null && !searchExecutor.isShutdown()) {
            searchExecutor.shutdown();
        }
    }

    // Filter Criteria Class
    private static class FilterCriteria {
        List<String> selectedPracticeAreas = new ArrayList<>();
        List<String> selectedConsultationTypes = new ArrayList<>();
        List<String> selectedWorkingDays = new ArrayList<>();
        String feeRange = "all";
        String experienceRange = "all";
        String gender = "any";

        boolean hasSelectedPracticeAreas() {
            return !selectedPracticeAreas.isEmpty();
        }

        boolean hasSelectedConsultationTypes() {
            return !selectedConsultationTypes.isEmpty();
        }

        boolean hasSelectedWorkingDays() {
            return !selectedWorkingDays.isEmpty();
        }
    }
}