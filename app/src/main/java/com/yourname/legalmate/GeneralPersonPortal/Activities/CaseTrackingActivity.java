package com.yourname.legalmate.GeneralPersonPortal.Activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.yourname.legalmate.GeneralPersonPortal.Adapters.ClientCaseAdapter;
import com.yourname.legalmate.GeneralPersonPortal.Models.ClientCase;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class CaseTrackingActivity extends AppCompatActivity {

    private static final String TAG = "CaseTrackingActivity";

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String currentUserId;

    // UI Components
    private ImageView ivBack, ivFilter;
    private TextView tvHeader;
    private SearchView searchView;
    private RecyclerView rvCases;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;

    // Adapter and Data
    private ClientCaseAdapter caseAdapter;
    private List<ClientCase> caseList;
    private List<ClientCase> filteredCaseList;

    // Progress Dialog
    private ProgressDialog progressDialog;

    // Bottom Sheet
    private BottomSheetDialog caseDetailsBottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_case_tracking);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupClickListeners();
        setupRecyclerView();
        setupSearch();
        loadClientCases();
    }

    private void initializeComponents() {
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI Components
        ivBack = findViewById(R.id.ivBack);
        ivFilter = findViewById(R.id.ivFilter);
        tvHeader = findViewById(R.id.tvHeader);
        searchView = findViewById(R.id.searchView);
        rvCases = findViewById(R.id.rvCases);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Lists
        caseList = new ArrayList<>();
        filteredCaseList = new ArrayList<>();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        ivFilter.setOnClickListener(v -> {
            // TODO: Implement filter functionality
            Toast.makeText(this, "Filter functionality coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        caseAdapter = new ClientCaseAdapter(this, filteredCaseList);
        caseAdapter.setOnCaseClickListener(this::showCaseDetailsBottomSheet);

        rvCases.setLayoutManager(new LinearLayoutManager(this));
        rvCases.setAdapter(caseAdapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCases(newText);
                return true;
            }
        });
    }

    // Fixed method: Using alternative approach instead of collectionGroup query
    private void loadClientCases() {
        showLoading(true);

        // First, get list of lawyers the client is connected to from chat
        loadLawyersFromChats();
    }

    private void loadLawyersFromChats() {
        Set<String> lawyerIds = new HashSet<>();

        // Load from UserChats to get lawyer IDs
        databaseReference.child("UserChats").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                                try {
                                    String otherUserId = chatSnapshot.child("otherUserId").getValue(String.class);
                                    String otherUserType = chatSnapshot.child("otherUserType").getValue(String.class);

                                    if (otherUserId != null && otherUserType != null &&
                                            otherUserType.toLowerCase().contains("lawyer")) {
                                        lawyerIds.add(otherUserId);
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Error parsing UserChats data", e);
                                }
                            }
                        }

                        // Also check Chats directory for additional lawyers
                        loadLawyersFromChatsDirectory(lawyerIds);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to load user chats", error.toException());
                        // Continue with empty lawyer list
                        loadCasesFromLawyers(new HashSet<>());
                    }
                });
    }

    private void loadLawyersFromChatsDirectory(Set<String> lawyerIds) {
        databaseReference.child("Chats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    if (chatId != null && chatId.contains(currentUserId)) {
                        String potentialLawyerId = extractOtherUserId(chatId, currentUserId);
                        if (potentialLawyerId != null && !potentialLawyerId.equals(currentUserId)) {
                            // Verify if this user is a lawyer by checking their profile
                            verifyAndAddLawyer(potentialLawyerId, lawyerIds);
                        }
                    }
                }

                // After processing all chats, load cases
                loadCasesFromLawyers(lawyerIds);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load chats directory", error.toException());
                loadCasesFromLawyers(lawyerIds);
            }
        });
    }

    private void verifyAndAddLawyer(String userId, Set<String> lawyerIds) {
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        lawyerIds.add(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error verifying lawyer: " + userId, e);
                });
    }

    private String extractOtherUserId(String chatId, String currentUserId) {
        if (chatId == null || currentUserId == null) return null;

        try {
            String[] parts = chatId.split("_");
            if (parts.length == 2) {
                return parts[0].equals(currentUserId) ? parts[1] : parts[0];
            }
        } catch (Exception e) {
            Log.w(TAG, "Error extracting other user ID from: " + chatId, e);
        }

        return null;
    }

    private void loadCasesFromLawyers(Set<String> lawyerIds) {
        if (lawyerIds.isEmpty()) {
            showLoading(false);
            showEmptyState(true);
            return;
        }

        List<String> lawyerList = new ArrayList<>(lawyerIds);
        int[] processedLawyers = {0};
        int totalLawyers = lawyerList.size();

        for (String layerId : lawyerList) {
            loadCasesFromSpecificLawyer(layerId, totalLawyers, processedLawyers);
        }
    }

    private void loadCasesFromSpecificLawyer(String layerId, int totalLawyers, int[] processedLawyers) {
        // Query specific lawyer's cases collection
        db.collection("All Cases")
                .document(layerId)
                .collection("Cases")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot caseDoc : queryDocumentSnapshots) {
                            String caseId = caseDoc.getId();

                            // Check if this case belongs to current client by checking Client Details
                            checkIfCaseBelongsToClient(caseId, layerId, caseDoc);
                        }
                    }

                    processedLawyers[0]++;
                    checkLawyerLoadingComplete(totalLawyers, processedLawyers[0]);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cases for lawyer " + layerId + ": " + e.getMessage());
                    processedLawyers[0]++;
                    checkLawyerLoadingComplete(totalLawyers, processedLawyers[0]);
                });
    }

    private void checkIfCaseBelongsToClient(String caseId, String layerId, DocumentSnapshot caseDoc) {
        db.collection("All Cases")
                .document(layerId)
                .collection("Cases")
                .document(caseId)
                .collection("Client Details")
                .document("client_info")
                .get()
                .addOnSuccessListener(clientDoc -> {
                    if (clientDoc.exists()) {
                        String clientId = clientDoc.getString("clientId");

                        // If this case belongs to current client, load full case details
                        if (currentUserId.equals(clientId)) {
                            loadFullCaseDetails(caseId, layerId, caseDoc);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking client details for case: " + caseId, e);
                });
    }

    private void loadFullCaseDetails(String caseId, String layerId, DocumentSnapshot caseRootDoc) {
        ClientCase clientCase = new ClientCase();
        clientCase.setCaseId(caseId);
        clientCase.setLayerId(layerId);

        // Set basic info from root document
        if (caseRootDoc.exists()) {
            clientCase.setCaseTitle(caseRootDoc.getString("caseTitle"));
            clientCase.setClientName(caseRootDoc.getString("clientName"));
            clientCase.setCaseType(caseRootDoc.getString("caseType"));
            clientCase.setCaseStatus(caseRootDoc.getString("caseStatus"));
        }

        // Load additional case details
        loadAdditionalCaseData(clientCase);
    }

    private void checkLawyerLoadingComplete(int totalLawyers, int processedLawyers) {
        if (processedLawyers >= totalLawyers) {
            // Add a delay to ensure all case loading operations complete
            new android.os.Handler().postDelayed(() -> {
                showLoading(false);

                if (caseList.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    filteredCaseList.clear();
                    filteredCaseList.addAll(caseList);
                    caseAdapter.notifyDataSetChanged();
                }
            }, 1000); // 1 second delay
        }
    }

    private void loadAdditionalCaseData(ClientCase clientCase) {
        // Load case details
        db.collection("All Cases")
                .document(clientCase.getLayerId())
                .collection("Cases")
                .document(clientCase.getCaseId())
                .collection("Case Details")
                .document("case_info")
                .get()
                .addOnSuccessListener(caseDetailsDoc -> {
                    if (caseDetailsDoc.exists()) {
                        clientCase.setCaseDescription(caseDetailsDoc.getString("caseDescription"));
                        clientCase.setCourtName(caseDetailsDoc.getString("courtName"));
                        clientCase.setCaseNumber(caseDetailsDoc.getString("caseNumber"));
                        clientCase.setFilingDate(caseDetailsDoc.getString("filingDate"));
                        clientCase.setJudgeName(caseDetailsDoc.getString("judgeName"));
                    }

                    // Load court date info
                    loadCourtDateInfo(clientCase);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading case details: " + e.getMessage());
                    loadCourtDateInfo(clientCase);
                });
    }

    private void loadCourtDateInfo(ClientCase clientCase) {
        db.collection("All Cases")
                .document(clientCase.getLayerId())
                .collection("Cases")
                .document(clientCase.getCaseId())
                .collection("Court Date & Reminder")
                .document("court_info")
                .get()
                .addOnSuccessListener(courtDoc -> {
                    if (courtDoc.exists()) {
                        clientCase.setCourtDate(courtDoc.getString("courtDate"));
                        clientCase.setCourtTime(courtDoc.getString("courtTime"));
                        clientCase.setHearingType(courtDoc.getString("hearingType"));
                    }

                    // Load status and tracking info
                    loadStatusTrackingInfo(clientCase);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading court date info: " + e.getMessage());
                    loadStatusTrackingInfo(clientCase);
                });
    }

    private void loadStatusTrackingInfo(ClientCase clientCase) {
        db.collection("All Cases")
                .document(clientCase.getLayerId())
                .collection("Cases")
                .document(clientCase.getCaseId())
                .collection("Status & Tracking")
                .document("status_info")
                .get()
                .addOnSuccessListener(statusDoc -> {
                    if (statusDoc.exists()) {
                        clientCase.setCaseStage(statusDoc.getString("caseStage"));
                        clientCase.setExpectedOutcome(statusDoc.getString("expectedOutcome"));
                    }

                    // Load lawyer info
                    loadLawyerInfo(clientCase);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading status tracking info: " + e.getMessage());
                    loadLawyerInfo(clientCase);
                });
    }

    private void loadLawyerInfo(ClientCase clientCase) {
        // Load lawyer basic profile info
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(clientCase.getLayerId())
                .collection("ProfileData")
                .document("BasicProfileSettings")
                .get()
                .addOnSuccessListener(lawyerDoc -> {
                    if (lawyerDoc.exists()) {
                        clientCase.setLawyerName(lawyerDoc.getString("fullName"));
                    }

                    // Load lawyer contact info
                    loadLawyerContactInfo(clientCase);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer basic info: " + e.getMessage());
                    loadLawyerContactInfo(clientCase);
                });
    }

    private void loadLawyerContactInfo(ClientCase clientCase) {
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(clientCase.getLayerId())
                .collection("ProfileData")
                .document("ContactLocationSettings")
                .get()
                .addOnSuccessListener(contactDoc -> {
                    if (contactDoc.exists()) {
                        clientCase.setLawyerPhone(contactDoc.getString("mobileNumber"));
                        clientCase.setLawyerEmail(contactDoc.getString("emailAddress"));
                    }

                    // Load lawyer professional info
                    loadLawyerProfessionalInfo(clientCase);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer contact info: " + e.getMessage());
                    loadLawyerProfessionalInfo(clientCase);
                });
    }

    private void loadLawyerProfessionalInfo(ClientCase clientCase) {
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(clientCase.getLayerId())
                .collection("ProfileData")
                .document("ProfessionalInformation")
                .get()
                .addOnSuccessListener(professionalDoc -> {
                    if (professionalDoc.exists()) {
                        @SuppressWarnings("unchecked")
                        List<String> practiceAreas = (List<String>) professionalDoc.get("practiceAreas");
                        if (practiceAreas != null && !practiceAreas.isEmpty()) {
                            clientCase.setLawyerSpecialization(practiceAreas.get(0) + " Specialist");
                        }

                        String experience = professionalDoc.getString("experience");
                        if (experience != null && !experience.isEmpty()) {
                            clientCase.setLawyerExperience(experience + " Years Experience");
                        }
                    }

                    // Load lawyer profile image
                    loadLawyerProfileImage(clientCase);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer professional info: " + e.getMessage());
                    loadLawyerProfileImage(clientCase);
                });
    }

    private void loadLawyerProfileImage(ClientCase clientCase) {
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(clientCase.getLayerId())
                .collection("ProfileData")
                .document("Documents")
                .get()
                .addOnSuccessListener(documentsDoc -> {
                    if (documentsDoc.exists()) {
                        clientCase.setLawyerImageUrl(documentsDoc.getString("profileImageUrl"));
                    }

                    // Add to case list
                    synchronized (caseList) {
                        caseList.add(clientCase);

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            if (!filteredCaseList.contains(clientCase)) {
                                filteredCaseList.add(clientCase);
                                caseAdapter.notifyItemInserted(filteredCaseList.size() - 1);
                            }

                            showEmptyState(filteredCaseList.isEmpty());
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer profile image: " + e.getMessage());

                    // Still add the case even if image loading fails
                    synchronized (caseList) {
                        caseList.add(clientCase);

                        runOnUiThread(() -> {
                            if (!filteredCaseList.contains(clientCase)) {
                                filteredCaseList.add(clientCase);
                                caseAdapter.notifyItemInserted(filteredCaseList.size() - 1);
                            }

                            showEmptyState(filteredCaseList.isEmpty());
                        });
                    }
                });
    }

    private void filterCases(String query) {
        filteredCaseList.clear();

        if (TextUtils.isEmpty(query)) {
            filteredCaseList.addAll(caseList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (ClientCase case_ : caseList) {
                if ((case_.getCaseTitle() != null && case_.getCaseTitle().toLowerCase().contains(lowerCaseQuery)) ||
                        (case_.getCaseType() != null && case_.getCaseType().toLowerCase().contains(lowerCaseQuery)) ||
                        (case_.getCaseStatus() != null && case_.getCaseStatus().toLowerCase().contains(lowerCaseQuery)) ||
                        (case_.getLawyerName() != null && case_.getLawyerName().toLowerCase().contains(lowerCaseQuery))) {
                    filteredCaseList.add(case_);
                }
            }
        }

        caseAdapter.notifyDataSetChanged();
        showEmptyState(filteredCaseList.isEmpty());
    }

    private void showCaseDetailsBottomSheet(ClientCase clientCase) {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_case_details_bottom_sheet, null);
        caseDetailsBottomSheet = new BottomSheetDialog(this);
        caseDetailsBottomSheet.setContentView(bottomSheetView);

        // Initialize bottom sheet components
        ImageView ivCloseBottomSheet = bottomSheetView.findViewById(R.id.ivCloseBottomSheet);

        // Basic Information
        TextView tvBottomCaseTitle = bottomSheetView.findViewById(R.id.tvBottomCaseTitle);
        TextView tvBottomCaseType = bottomSheetView.findViewById(R.id.tvBottomCaseType);
        TextView tvBottomCaseStatus = bottomSheetView.findViewById(R.id.tvBottomCaseStatus);
        TextView tvBottomCourtName = bottomSheetView.findViewById(R.id.tvBottomCourtName);

        // Lawyer Information
        CircleImageView ivBottomLawyerImage = bottomSheetView.findViewById(R.id.ivBottomLawyerImage);
        TextView tvBottomLawyerName = bottomSheetView.findViewById(R.id.tvBottomLawyerName);
        TextView tvBottomLawyerSpecialization = bottomSheetView.findViewById(R.id.tvBottomLawyerSpecialization);
        TextView tvBottomLawyerExperience = bottomSheetView.findViewById(R.id.tvBottomLawyerExperience);
        TextView tvBottomLawyerPhone = bottomSheetView.findViewById(R.id.tvBottomLawyerPhone);
        TextView tvBottomLawyerEmail = bottomSheetView.findViewById(R.id.tvBottomLawyerEmail);

        // Court Schedule
        TextView tvBottomNextCourtDate = bottomSheetView.findViewById(R.id.tvBottomNextCourtDate);
        TextView tvBottomCourtTime = bottomSheetView.findViewById(R.id.tvBottomCourtTime);
        TextView tvBottomHearingType = bottomSheetView.findViewById(R.id.tvBottomHearingType);

        // Case Progress
        TextView tvBottomCaseStage = bottomSheetView.findViewById(R.id.tvBottomCaseStage);
        TextView tvBottomExpectedOutcome = bottomSheetView.findViewById(R.id.tvBottomExpectedOutcome);

        // Populate data
        populateBottomSheetData(clientCase, tvBottomCaseTitle, tvBottomCaseType, tvBottomCaseStatus,
                tvBottomCourtName, ivBottomLawyerImage, tvBottomLawyerName, tvBottomLawyerSpecialization,
                tvBottomLawyerExperience, tvBottomLawyerPhone, tvBottomLawyerEmail, tvBottomNextCourtDate,
                tvBottomCourtTime, tvBottomHearingType, tvBottomCaseStage, tvBottomExpectedOutcome);

        // Close button click
        ivCloseBottomSheet.setOnClickListener(v -> caseDetailsBottomSheet.dismiss());

        caseDetailsBottomSheet.show();
    }

    private void populateBottomSheetData(ClientCase clientCase, TextView tvBottomCaseTitle,
                                         TextView tvBottomCaseType, TextView tvBottomCaseStatus,
                                         TextView tvBottomCourtName, CircleImageView ivBottomLawyerImage,
                                         TextView tvBottomLawyerName, TextView tvBottomLawyerSpecialization,
                                         TextView tvBottomLawyerExperience, TextView tvBottomLawyerPhone,
                                         TextView tvBottomLawyerEmail, TextView tvBottomNextCourtDate,
                                         TextView tvBottomCourtTime, TextView tvBottomHearingType,
                                         TextView tvBottomCaseStage, TextView tvBottomExpectedOutcome) {

        // Basic Information
        tvBottomCaseTitle.setText(clientCase.getCaseTitle() != null ? clientCase.getCaseTitle() : "N/A");
        tvBottomCaseType.setText(clientCase.getCaseType() != null ? clientCase.getCaseType() : "N/A");
        tvBottomCaseStatus.setText(clientCase.getCaseStatus() != null ? clientCase.getCaseStatus() : "N/A");
        tvBottomCourtName.setText(clientCase.getCourtName() != null ? clientCase.getCourtName() : "N/A");

        // Lawyer Information
        tvBottomLawyerName.setText(clientCase.getLawyerName() != null ?
                "Advocate " + clientCase.getLawyerName() : "N/A");
        tvBottomLawyerSpecialization.setText(clientCase.getLawyerSpecialization() != null ?
                clientCase.getLawyerSpecialization() : "Legal Specialist");
        tvBottomLawyerExperience.setText(clientCase.getLawyerExperience() != null ?
                clientCase.getLawyerExperience() : "Experienced Lawyer");
        tvBottomLawyerPhone.setText(clientCase.getLawyerPhone() != null ?
                clientCase.getLawyerPhone() : "Not available");
        tvBottomLawyerEmail.setText(clientCase.getLawyerEmail() != null ?
                clientCase.getLawyerEmail() : "Not available");

        // Load lawyer profile image
        if (clientCase.getLawyerImageUrl() != null && !clientCase.getLawyerImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(clientCase.getLawyerImageUrl())
                    .placeholder(R.drawable.ic_person_ai)
                    .error(R.drawable.ic_person_ai)
                    .circleCrop()
                    .into(ivBottomLawyerImage);
        } else {
            ivBottomLawyerImage.setImageResource(R.drawable.ic_person_ai);
        }

        // Court Schedule
        if (clientCase.getCourtDate() != null && !clientCase.getCourtDate().isEmpty()) {
            tvBottomNextCourtDate.setText(formatDate(clientCase.getCourtDate()));
        } else {
            tvBottomNextCourtDate.setText("Not scheduled");
        }

        if (clientCase.getCourtTime() != null && !clientCase.getCourtTime().isEmpty()) {
            tvBottomCourtTime.setText(formatTime(clientCase.getCourtTime()));
        } else {
            tvBottomCourtTime.setText("Not scheduled");
        }

        tvBottomHearingType.setText(clientCase.getHearingType() != null ?
                clientCase.getHearingType() : "Regular Hearing");

        // Case Progress
        tvBottomCaseStage.setText(clientCase.getCaseStage() != null ?
                clientCase.getCaseStage() : "Pre-trial");
        tvBottomExpectedOutcome.setText(clientCase.getExpectedOutcome() != null ?
                clientCase.getExpectedOutcome() : "Favorable resolution");
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString; // Return original if parsing fails
        }
    }

    private String formatTime(String time24Hour) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(time24Hour);
            return outputFormat.format(date);
        } catch (Exception e) {
            return time24Hour; // Return original if parsing fails
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvCases.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvCases.setVisibility(show ? View.GONE : View.VISIBLE);
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
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}