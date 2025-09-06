package com.yourname.legalmate.LawyerPortal.Activities;

import static com.google.common.io.Files.getFileExtension;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.Chat.ChatListAdapter;
import com.yourname.legalmate.Chat.ChatListModel;
import com.yourname.legalmate.LawyerPortal.Adapters.DocumentAdapter;
import com.yourname.legalmate.LawyerPortal.Models.AttachedDocument;
import com.yourname.legalmate.R;
import com.yourname.legalmate.utils.CloudinaryConfig;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddCaseInfoActivity extends AppCompatActivity {

    private static final String TAG = "AddCaseInfo";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    TextView tvHeader;

    // UI Components - Client Details
    private TextInputEditText etClientName, etClientPhone, etClientEmail, etClientAddress, etClientNID;
    private Spinner spinnerClientType;

    // UI Components - Case Details
    private TextInputEditText etCaseTitle, etCaseDescription, etCourtName, etCaseNumber, etCourtLocation, etJudgeName;
    private Spinner spinnerCaseType;
    private Button btnFilingDate;
    private String selectedFilingDate = "";

    // UI Components - Court Date & Reminder
    private Button btnCourtDate, btnCourtTime;
    private Spinner spinnerReminderSettings;
    private TextInputEditText etHearingType;
    private String selectedCourtDate = "", selectedCourtTime = "";

    // UI Components - Parties Involved
    private TextInputEditText etPlaintiffNames, etDefendantNames, etOpposingLawyer, etOpposingPartyContact;

    // UI Components - Notes & Documents
    private TextInputEditText etPrivateNotes, etTags;
    private Button btnAttachDocuments;
    private RecyclerView rvAttachedDocuments;
    private DocumentAdapter documentAdapter;
    private List<AttachedDocument> attachedDocumentsList;

    // UI Components - Status & Tracking
    private Spinner spinnerCaseStatus, spinnerCaseStage;
    private TextInputEditText etExpectedOutcome;

    // Action Buttons
    private Button btnSaveDraft, btnSaveCase;

    // Progress Dialog
    private ProgressDialog progressDialog;

    // Document Upload
    private ActivityResultLauncher<Intent> documentPickerLauncher;
    private List<Uri> selectedDocumentUris;



    private String caseId; // For edit mode
    private String fromActivity; // To track source activity
    private boolean isEditMode = false; // Flag to determine if we're editing


    // Add these instance variables to your existing variables
    private Button btnAddClient;
    private MaterialCardView cardSelectedClient;
    private CircleImageView ivSelectedClientImage;
    private TextView tvSelectedClientName, tvSelectedClientType;
    private ImageView ivRemoveSelectedClient;

    // Client selection data
    private String selectedClientId = "";
    private String selectedClientName = "";
    private String selectedClientPhone = "";
    private String selectedClientEmail = "";
    private String selectedClientImageUrl = "";
    private String selectedClientType = "";

    // Bottom sheet components
    private BottomSheetDialog clientSelectionBottomSheet;
    private RecyclerView rvClientList;
    private ChatListAdapter clientSelectionAdapter;
    private List<ChatListModel> clientChatList;
    private SearchView searchViewClients;

    // Firebase Database for chat list
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_case_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get intent extras first
        getIntentExtras();


        initializeComponents();
        setupActionBar();
        setupSpinners();
        setupDateTimePickers();
        setupDocumentUpload();
        setupClickListeners();


        // Load existing case data if in edit mode
        if (isEditMode) {
            loadExistingCaseData();
        }

    }//==================================================

    // Add this new method to get intent extras
    private void getIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            caseId = intent.getStringExtra("caseId");
            fromActivity = intent.getStringExtra("fromActivity");

            // Determine if we're in edit mode
            if (caseId != null && !caseId.isEmpty()) {
                isEditMode = true;
            }

            Log.d(TAG, "Intent extras - caseId: " + caseId + ", fromActivity: " + fromActivity + ", isEditMode: " + isEditMode);
        }
    }

    // Add method to load existing case data
    private void loadExistingCaseData() {
        showProgressDialog("Loading case data...");

        DocumentReference caseRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId);

        // Load all case data sections
        loadExistingClientDetails();
    }

    private void loadExistingClientDetails() {
        DocumentReference clientRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Client Details")
                .document("client_info");

        clientRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate client details
                        etClientName.setText(documentSnapshot.getString("clientName"));
                        etClientPhone.setText(documentSnapshot.getString("clientPhone"));
                        etClientEmail.setText(documentSnapshot.getString("clientEmail"));
                        etClientAddress.setText(documentSnapshot.getString("clientAddress"));
                        etClientNID.setText(documentSnapshot.getString("clientNID"));

                        String clientType = documentSnapshot.getString("clientType");
                        setSpinnerSelection(spinnerClientType, clientType);

                        // Load selected client info if exists
                        String existingClientId = documentSnapshot.getString("clientId");
                        String existingClientImageUrl = documentSnapshot.getString("clientImageUrl");

                        if (existingClientId != null && !existingClientId.isEmpty()) {
                            selectedClientId = existingClientId;
                            selectedClientName = documentSnapshot.getString("clientName");
                            selectedClientImageUrl = existingClientImageUrl;
                            selectedClientType = clientType;

                            // Show selected client card
                            cardSelectedClient.setVisibility(View.VISIBLE);
                            tvSelectedClientName.setText(selectedClientName);
                            tvSelectedClientType.setText(selectedClientType);

                            // Load profile image
                            if (selectedClientImageUrl != null && !selectedClientImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(selectedClientImageUrl)
                                        .placeholder(R.drawable.ic_person_ai)
                                        .error(R.drawable.ic_person_ai)
                                        .circleCrop()
                                        .into(ivSelectedClientImage);
                            } else {
                                ivSelectedClientImage.setImageResource(R.drawable.ic_person_ai);
                            }
                        }
                    }
                    loadExistingCaseDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading client details: " + e.getMessage());
                    loadExistingCaseDetails(); // Continue loading
                });
    }



    private void loadExistingCaseDetails() {
        DocumentReference caseDetailsRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Case Details")
                .document("case_info");

        caseDetailsRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate case details
                        etCaseTitle.setText(documentSnapshot.getString("caseTitle"));
                        etCaseDescription.setText(documentSnapshot.getString("caseDescription"));
                        etCourtName.setText(documentSnapshot.getString("courtName"));
                        etCaseNumber.setText(documentSnapshot.getString("caseNumber"));
                        etCourtLocation.setText(documentSnapshot.getString("courtLocation"));
                        etJudgeName.setText(documentSnapshot.getString("judgeName"));

                        String caseType = documentSnapshot.getString("caseType");
                        setSpinnerSelection(spinnerCaseType, caseType);

                        // Handle filing date
                        String filingDate = documentSnapshot.getString("filingDate");
                        if (filingDate != null && !filingDate.isEmpty()) {
                            selectedFilingDate = filingDate;
                            btnFilingDate.setText("Filing Date: " + formatDate(filingDate));
                        }
                    }
                    loadExistingCourtDateReminder();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading case details: " + e.getMessage());
                    loadExistingCourtDateReminder(); // Continue loading
                });
    }

    private void loadExistingCourtDateReminder() {
        DocumentReference courtRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Court Date & Reminder")
                .document("court_info");

        courtRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate court date and reminder
                        String courtDate = documentSnapshot.getString("courtDate");
                        String courtTime = documentSnapshot.getString("courtTime");
                        String hearingType = documentSnapshot.getString("hearingType");
                        String reminderSettings = documentSnapshot.getString("reminderSettings");

                        if (courtDate != null && !courtDate.isEmpty()) {
                            selectedCourtDate = courtDate;
                            btnCourtDate.setText("Court Date: " + formatDate(courtDate));
                        }

                        if (courtTime != null && !courtTime.isEmpty()) {
                            selectedCourtTime = courtTime;
                            btnCourtTime.setText("Court Time: " + formatTime(courtTime));
                        }

                        etHearingType.setText(hearingType);
                        setSpinnerSelection(spinnerReminderSettings, reminderSettings);
                    }
                    loadExistingPartiesInvolved();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading court details: " + e.getMessage());
                    loadExistingPartiesInvolved(); // Continue loading
                });
    }

    private void loadExistingPartiesInvolved() {
        DocumentReference partiesRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Parties Involved")
                .document("parties_info");

        partiesRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate parties involved
                        etPlaintiffNames.setText(documentSnapshot.getString("plaintiffNames"));
                        etDefendantNames.setText(documentSnapshot.getString("defendantNames"));
                        etOpposingLawyer.setText(documentSnapshot.getString("opposingLawyer"));
                        etOpposingPartyContact.setText(documentSnapshot.getString("opposingPartyContact"));
                    }
                    loadExistingNotesDocuments();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading parties details: " + e.getMessage());
                    loadExistingNotesDocuments(); // Continue loading
                });
    }

    private void loadExistingNotesDocuments() {
        DocumentReference notesRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Notes & Documents")
                .document("notes_info");

        notesRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate notes and documents
                        etPrivateNotes.setText(documentSnapshot.getString("privateNotes"));
                        etTags.setText(documentSnapshot.getString("tags"));

                        // Load existing documents
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> existingDocs = (List<Map<String, Object>>) documentSnapshot.get("attachedDocuments");
                        if (existingDocs != null && !existingDocs.isEmpty()) {
                            loadExistingDocuments(existingDocs);
                        }
                    }
                    loadExistingStatusTracking();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notes and documents: " + e.getMessage());
                    loadExistingStatusTracking(); // Continue loading
                });
    }

    private void loadExistingDocuments(List<Map<String, Object>> existingDocs) {
        attachedDocumentsList.clear();

        for (Map<String, Object> docMap : existingDocs) {
            AttachedDocument document = new AttachedDocument();
            document.setName((String) docMap.get("name"));
            document.setUri((String) docMap.get("uri"));
            document.setStatus((String) docMap.get("status"));

            // Fix for size casting issue
            Object sizeObj = docMap.get("size");
            if (sizeObj != null) {
                if (sizeObj instanceof Long) {
                    document.setSize((Long) sizeObj);
                } else if (sizeObj instanceof Double) {
                    document.setSize(((Double) sizeObj).longValue());
                } else if (sizeObj instanceof Integer) {
                    document.setSize(((Integer) sizeObj).longValue());
                } else if (sizeObj instanceof String) {
                    try {
                        document.setSize(Long.parseLong((String) sizeObj));
                    } catch (NumberFormatException e) {
                        document.setSize(0L); // Default size
                    }
                } else {
                    document.setSize(0L); // Default size
                }
            } else {
                document.setSize(0L); // Default size if null
            }

            document.setCloudinaryUrl((String) docMap.get("cloudinaryUrl"));
            document.setCloudinaryPublicId((String) docMap.get("cloudinaryPublicId"));
            document.setUploadDate((String) docMap.get("uploadDate"));

            attachedDocumentsList.add(document);
        }

        if (!attachedDocumentsList.isEmpty()) {
            rvAttachedDocuments.setVisibility(RecyclerView.VISIBLE);
            documentAdapter.notifyDataSetChanged();
        }
    }

    private void loadExistingStatusTracking() {
        DocumentReference statusRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(caseId)
                .collection("Status & Tracking")
                .document("status_info");

        statusRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    hideProgressDialog();

                    if (documentSnapshot.exists()) {
                        // Populate status and tracking
                        String caseStatus = documentSnapshot.getString("caseStatus");
                        String caseStage = documentSnapshot.getString("caseStage");
                        String expectedOutcome = documentSnapshot.getString("expectedOutcome");

                        setSpinnerSelection(spinnerCaseStatus, caseStatus);
                        setSpinnerSelection(spinnerCaseStage, caseStage);
                        etExpectedOutcome.setText(expectedOutcome);
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e(TAG, "Error loading status tracking: " + e.getMessage());
                    Toast.makeText(this, "Error loading case data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    // Helper method to set spinner selection by value
    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value != null && spinner.getAdapter() != null) {
            ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }



    // Helper method to format time for display
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

        // Initialize UI Components - Client Details
        etClientName = findViewById(R.id.etClientName);
        etClientPhone = findViewById(R.id.etClientPhone);
        etClientEmail = findViewById(R.id.etClientEmail);
        etClientAddress = findViewById(R.id.etClientAddress);
        etClientNID = findViewById(R.id.etClientNID);
        spinnerClientType = findViewById(R.id.spinnerClientType);

        // Initialize UI Components - Case Details
        etCaseTitle = findViewById(R.id.etCaseTitle);
        etCaseDescription = findViewById(R.id.etCaseDescription);
        etCourtName = findViewById(R.id.etCourtName);
        etCaseNumber = findViewById(R.id.etCaseNumber);
        etCourtLocation = findViewById(R.id.etCourtLocation);
        etJudgeName = findViewById(R.id.etJudgeName);
        spinnerCaseType = findViewById(R.id.spinnerCaseType);
        btnFilingDate = findViewById(R.id.btnFilingDate);

        // Initialize UI Components - Court Date & Reminder
        btnCourtDate = findViewById(R.id.btnCourtDate);
        btnCourtTime = findViewById(R.id.btnCourtTime);
        spinnerReminderSettings = findViewById(R.id.spinnerReminderSettings);
        etHearingType = findViewById(R.id.etHearingType);

        // Initialize UI Components - Parties Involved
        etPlaintiffNames = findViewById(R.id.etPlaintiffNames);
        etDefendantNames = findViewById(R.id.etDefendantNames);
        etOpposingLawyer = findViewById(R.id.etOpposingLawyer);
        etOpposingPartyContact = findViewById(R.id.etOpposingPartyContact);

        // Initialize UI Components - Notes & Documents
        etPrivateNotes = findViewById(R.id.etPrivateNotes);
        etTags = findViewById(R.id.etTags);
        btnAttachDocuments = findViewById(R.id.btnAttachDocuments);
        rvAttachedDocuments = findViewById(R.id.rvAttachedDocuments);

        // Initialize UI Components - Status & Tracking
        spinnerCaseStatus = findViewById(R.id.spinnerCaseStatus);
        spinnerCaseStage = findViewById(R.id.spinnerCaseStage);
        etExpectedOutcome = findViewById(R.id.etExpectedOutcome);

        // Initialize Action Buttons
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnSaveCase = findViewById(R.id.btnSaveCase);

        tvHeader = findViewById(R.id.tvHeader);

        // Initialize Document List
        attachedDocumentsList = new ArrayList<>();
        selectedDocumentUris = new ArrayList<>();
        setupDocumentRecyclerView();

        if (isEditMode) {
            updateButtonsForEditMode();
        }



        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize new UI Components for client selection
        btnAddClient = findViewById(R.id.btnAddClient);
        cardSelectedClient = findViewById(R.id.cardSelectedClient);
        ivSelectedClientImage = findViewById(R.id.ivSelectedClientImage);
        tvSelectedClientName = findViewById(R.id.tvSelectedClientName);
        tvSelectedClientType = findViewById(R.id.tvSelectedClientType);
        ivRemoveSelectedClient = findViewById(R.id.ivRemoveSelectedClient);

        // Initialize client list
        clientChatList = new ArrayList<>();


    }

    // Update the setupActionBar method
    private void setupActionBar() {



        if (isEditMode && "CaseStatusActivity".equals(fromActivity)) {

            tvHeader.setText("Edit Case Info");
        } else {

            tvHeader.setText("Add New Case");
        }
    }

    private void setupSpinners() {
        // Setup Client Type Spinner
        ArrayAdapter<CharSequence> clientTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.client_types, android.R.layout.simple_spinner_item);
        clientTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClientType.setAdapter(clientTypeAdapter);

        // Setup Case Type Spinner
        ArrayAdapter<CharSequence> caseTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_types, android.R.layout.simple_spinner_item);
        caseTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCaseType.setAdapter(caseTypeAdapter);

        // Setup Reminder Settings Spinner
        ArrayAdapter<CharSequence> reminderAdapter = ArrayAdapter.createFromResource(this,
                R.array.reminder_options, android.R.layout.simple_spinner_item);
        reminderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderSettings.setAdapter(reminderAdapter);

        // Setup Case Status Spinner
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCaseStatus.setAdapter(statusAdapter);

        // Setup Case Stage Spinner
        ArrayAdapter<CharSequence> stageAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_stage_options, android.R.layout.simple_spinner_item);
        stageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCaseStage.setAdapter(stageAdapter);
    }

    private void setupDateTimePickers() {
        btnFilingDate.setOnClickListener(v -> showDatePicker((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedFilingDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            btnFilingDate.setText("Filing Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        }));

        btnCourtDate.setOnClickListener(v -> showDatePicker((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedCourtDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            btnCourtDate.setText("Court Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        }));

        btnCourtTime.setOnClickListener(v -> showTimePicker((view, hourOfDay, minute) -> {
            selectedCourtTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);

            int displayHour;
            String amPm;

            if (hourOfDay == 0) {
                displayHour = 12;
                amPm = "AM";
            } else if (hourOfDay < 12) {
                displayHour = hourOfDay;
                amPm = "AM";
            } else if (hourOfDay == 12) {
                displayHour = 12;
                amPm = "PM";
            } else {
                displayHour = hourOfDay - 12;
                amPm = "PM";
            }

            String displayTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm);
            btnCourtTime.setText("Court Time: " + displayTime);
        }));
    }

    private void showDatePicker(DatePickerDialog.OnDateSetListener listener) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker(TimePickerDialog.OnTimeSetListener listener) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                listener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void setupDocumentUpload() {
        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        List<Uri> newUris = new ArrayList<>();

                        if (data.getClipData() != null) {
                            // Multiple files selected
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri documentUri = data.getClipData().getItemAt(i).getUri();
                                newUris.add(documentUri);
                            }
                        } else if (data.getData() != null) {
                            // Single file selected
                            Uri documentUri = data.getData();
                            newUris.add(documentUri);
                        }

                        if (!newUris.isEmpty()) {
                            selectedDocumentUris.addAll(newUris);
                            updateDocumentList();
                            Toast.makeText(this, newUris.size() + " document(s) selected", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No documents selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupDocumentRecyclerView() {
        documentAdapter = new DocumentAdapter(attachedDocumentsList, document -> {
            // Remove document from list
            attachedDocumentsList.remove(document);
            documentAdapter.notifyDataSetChanged();

            if (attachedDocumentsList.isEmpty()) {
                rvAttachedDocuments.setVisibility(RecyclerView.GONE);
            }
        });

        rvAttachedDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvAttachedDocuments.setAdapter(documentAdapter);
    }

    private void setupClickListeners() {
        btnAttachDocuments.setOnClickListener(v -> checkPermissionAndPickDocuments());
        btnSaveDraft.setOnClickListener(v -> saveCaseData(true));
        btnSaveCase.setOnClickListener(v -> saveCaseData(false));

        btnAddClient.setOnClickListener(v -> showClientSelectionBottomSheet());
        ivRemoveSelectedClient.setOnClickListener(v -> removeSelectedClient());


    }

    /**
     * Check if Cloudinary is ready for uploads
     * Uses the centralized CloudinaryConfig class
     */
    private boolean isCloudinaryReady() {
        return CloudinaryConfig.canUpload();
    }

    private void checkPermissionAndPickDocuments() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            boolean hasImagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;

            if (!hasImagePermission) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                    showPermissionRationaleDialog();
                } else {
                    requestStoragePermission();
                }
            } else {
                openDocumentPicker();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showPermissionRationaleDialog();
                } else {
                    requestStoragePermission();
                }
            } else {
                openDocumentPicker();
            }
        } else {
            // Below Android 6
            openDocumentPicker();
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs storage permission to upload documents. Please grant permission to continue.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    dialog.dismiss();
                    requestStoragePermission();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Permission denied. Cannot upload documents.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // Allow PDF and images
        String[] mimeTypes = {"application/pdf", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            documentPickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No file manager found. Please install a file manager app.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateDocumentList() {
        for (Uri uri : selectedDocumentUris) {
            String fileName = getFileName(uri);
            long fileSize = getFileSize(uri);
            AttachedDocument document = new AttachedDocument(fileName, uri.toString(), "pending", fileSize);
            attachedDocumentsList.add(document);
        }

        documentAdapter.notifyDataSetChanged();
        if (!attachedDocumentsList.isEmpty()) {
            rvAttachedDocuments.setVisibility(RecyclerView.VISIBLE);
        }

        selectedDocumentUris.clear();
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size: " + e.getMessage());
        }
        return size;
    }

    private String getFileName(Uri uri) {
        String fileName = "Unknown";
        try {
            if (uri.getScheme().equals("content")) {
                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                    cursor.close();
                }
            } else {
                fileName = uri.getLastPathSegment();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name: " + e.getMessage());
        }
        return fileName;
    }

    // Update the saveCaseData method to handle edit mode
    private void saveCaseData(boolean isDraft) {
        if (!validateForm() && !isDraft) {
            return;
        }

        showProgressDialog("Saving case data...");

        String finalCaseId;
        if (isEditMode) {
            // Use existing case ID for edit mode
            finalCaseId = caseId;
        } else {
            // Generate new case ID for new case
            finalCaseId = db.collection("All Cases")
                    .document(currentUserId)
                    .collection("Cases")
                    .document().getId();
        }

        // Upload documents first if any new ones
        List<AttachedDocument> newDocuments = getNewDocuments();
        if (!newDocuments.isEmpty()) {
            uploadNewDocuments(finalCaseId, isDraft);
        } else {
            saveToFirestore(finalCaseId, isDraft, new ArrayList<>());
        }
    }


    // Helper method to get only new documents (not yet uploaded)
    private List<AttachedDocument> getNewDocuments() {
        List<AttachedDocument> newDocs = new ArrayList<>();
        for (AttachedDocument doc : attachedDocumentsList) {
            if ("pending".equals(doc.getStatus()) || doc.getCloudinaryUrl() == null) {
                newDocs.add(doc);
            }
        }
        return newDocs;
    }



    private boolean validateForm() {
        if (TextUtils.isEmpty(etClientName.getText())) {
            etClientName.setError("Client name is required");
            etClientName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etClientPhone.getText())) {
            etClientPhone.setError("Client phone is required");
            etClientPhone.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etCaseTitle.getText())) {
            etCaseTitle.setError("Case title is required");
            etCaseTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etCaseDescription.getText())) {
            etCaseDescription.setError("Case description is required");
            etCaseDescription.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etCourtName.getText())) {
            etCourtName.setError("Court name is required");
            etCourtName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(selectedFilingDate)) {
            Toast.makeText(this, "Please select filing date", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerClientType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select client type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerCaseType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select case type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerCaseStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select case status", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void uploadDocuments(String caseId, boolean isDraft) {
        // Check if Cloudinary is ready (initialized in Application class)
        if (!isCloudinaryReady()) {
            hideProgressDialog();
            Toast.makeText(this, "Upload service not available. Saving case without documents.", Toast.LENGTH_LONG).show();
            saveToFirestore(caseId, isDraft, new ArrayList<>());
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        int[] uploadCount = {0};
        int[] successCount = {0};
        int totalDocuments = attachedDocumentsList.size();

        updateProgressDialog("Uploading documents... (0/" + totalDocuments + ")");

        // Upload each document
        for (int i = 0; i < attachedDocumentsList.size(); i++) {
            AttachedDocument document = attachedDocumentsList.get(i);
            Uri documentUri = Uri.parse(document.getUri());

            uploadSingleDocument(documentUri, document, caseId, uploadCount, successCount, uploadedUrls, totalDocuments, isDraft);
        }
    }

    private void uploadSingleDocument(Uri documentUri, AttachedDocument document, String caseId,
                                      int[] uploadCount, int[] successCount, List<String> uploadedUrls,
                                      int totalDocuments, boolean isDraft) {
        try {
            Log.d(TAG, "Starting upload for: " + document.getName());

            // Generate unique public ID with proper extension
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = document.getName();
            String fileExtension = getFileExtension(fileName);

            // Create public ID with extension for better file handling
            String publicId = CloudinaryConfig.generateCaseDocumentPublicId(currentUserId, caseId, "doc_" + timestamp);

            // Add extension to public ID for PDF files
            if (fileExtension.equalsIgnoreCase("pdf")) {
                publicId += ".pdf";
            }

            // Determine resource type based on file
            String resourceType = determineResourceType(documentUri, document.getName());

            Log.d(TAG, "Resource type determined: " + resourceType + " for file: " + fileName);
            Log.d(TAG, "Public ID: " + publicId);

            // Upload to Cloudinary using URI directly
            MediaManager.get().upload(documentUri)
                    .option("public_id", publicId)
                    .option("resource_type", resourceType)
                    .option("folder", CloudinaryConfig.getLawyerDocumentFolder(currentUserId))
                    .option("use_filename", false) // Changed to false to use our custom public_id
                    .option("unique_filename", false) // Changed to false since we're providing unique public_id
                    // Add these options for better PDF handling
                    .option("format", fileExtension.equalsIgnoreCase("pdf") ? "pdf" : null)
                    .option("pages", true) // Enable page extraction for PDFs
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload started for: " + document.getName());
                            runOnUiThread(() -> {
                                document.setStatus("uploading");
                                documentAdapter.notifyDataSetChanged();
                            });
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            Log.d(TAG, "Upload progress: " + bytes + "/" + totalBytes + " for " + document.getName());
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            Log.d(TAG, "Upload successful for: " + document.getName());
                            Log.d(TAG, "Result data: " + resultData.toString());

                            uploadCount[0]++;
                            successCount[0]++;

                            String uploadedUrl = (String) resultData.get("secure_url");
                            String publicId = (String) resultData.get("public_id");

                            // For PDF files, ensure the URL has proper extension
                            if (fileExtension.equalsIgnoreCase("pdf") && uploadedUrl != null) {
                                // If URL doesn't have .pdf extension, add it
                                if (!uploadedUrl.toLowerCase().contains(".pdf")) {
                                    // Modify the URL to include .pdf extension
                                    uploadedUrl = uploadedUrl.replace("/upload/", "/upload/f_pdf/");
                                }
                            }

                            uploadedUrls.add(uploadedUrl);

                            String finalUploadedUrl = uploadedUrl;
                            runOnUiThread(() -> {
                                document.setStatus("uploaded");
                                document.setUri(finalUploadedUrl);
                                document.setCloudinaryUrl(finalUploadedUrl);
                                document.setCloudinaryPublicId(publicId);
                                document.setUploadDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                                documentAdapter.notifyDataSetChanged();

                                updateProgressDialog("Uploading documents... (" + uploadCount[0] + "/" + totalDocuments + ")");
                                checkUploadCompletion(uploadCount[0], successCount[0], totalDocuments, caseId, isDraft, uploadedUrls);
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Upload error for " + document.getName() + ": " + error.getDescription());
                            uploadCount[0]++;

                            runOnUiThread(() -> {
                                document.setStatus("failed");
                                documentAdapter.notifyDataSetChanged();

                                updateProgressDialog("Uploading documents... (" + uploadCount[0] + "/" + totalDocuments + ")");
                                Toast.makeText(AddCaseInfoActivity.this, "Failed: " + document.getName(), Toast.LENGTH_SHORT).show();

                                checkUploadCompletion(uploadCount[0], successCount[0], totalDocuments, caseId, isDraft, uploadedUrls);
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.d(TAG, "Upload rescheduled for: " + document.getName());
                        }
                    })
                    .dispatch();

        } catch (Exception e) {
            Log.e(TAG, "Exception during upload for " + document.getName() + ": " + e.getMessage());
            uploadCount[0]++;
            runOnUiThread(() -> {
                document.setStatus("failed");
                documentAdapter.notifyDataSetChanged();
                updateProgressDialog("Uploading documents... (" + uploadCount[0] + "/" + totalDocuments + ")");
                Toast.makeText(this, "Error: " + document.getName(), Toast.LENGTH_SHORT).show();
                checkUploadCompletion(uploadCount[0], successCount[0], totalDocuments, caseId, isDraft, uploadedUrls);
            });
        }
    }


    // Helper method to get file extension
    private String getFileExtension(String fileName) {
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return extension;
    }


    // Helper method to check if all uploads are completed
    private void checkUploadCompletion(int uploadCount, int successCount, int totalDocuments,
                                       String caseId, boolean isDraft, List<String> uploadedUrls) {
        if (uploadCount >= totalDocuments) {
            Log.d(TAG, "All uploads completed. Success: " + successCount + "/" + totalDocuments);
            saveToFirestore(caseId, isDraft, uploadedUrls);
        }
    }

    // Updated determineResourceType method
    private String determineResourceType(Uri uri, String fileName) {
        // First try to get MIME type from content resolver
        String mimeType = getContentResolver().getType(uri);

        // If MIME type is null or unknown, determine from file extension
        if (mimeType == null || mimeType.equals("application/octet-stream")) {
            String extension = getFileExtension(fileName);
            if (extension != null && !extension.isEmpty()) {
                switch (extension.toLowerCase()) {
                    case "pdf":
                        mimeType = "application/pdf";
                        break;
                    case "jpg":
                    case "jpeg":
                        mimeType = "image/jpeg";
                        break;
                    case "png":
                        mimeType = "image/png";
                        break;
                    case "gif":
                        mimeType = "image/gif";
                        break;
                    case "bmp":
                        mimeType = "image/bmp";
                        break;
                    case "webp":
                        mimeType = "image/webp";
                        break;
                    case "doc":
                    case "docx":
                        mimeType = "application/msword";
                        break;
                    case "xls":
                    case "xlsx":
                        mimeType = "application/vnd.ms-excel";
                        break;
                    default:
                        mimeType = "application/octet-stream";
                        break;
                }
            }
        }

        Log.d(TAG, "MIME type for " + fileName + ": " + mimeType);

        // Determine Cloudinary resource type
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "image";
            } else if (mimeType.equals("application/pdf")) {
                return "raw"; // PDFs should be uploaded as raw
            } else if (mimeType.startsWith("video/")) {
                return "video";
            }
        }

        // Default to raw for documents
        return "raw";
    }


    public static String generatePDFPreviewURL(String publicId, String cloudName) {
        if (publicId == null || publicId.isEmpty() || cloudName == null || cloudName.isEmpty()) {
            return null;
        }

        // Generate URL for first page of PDF as image
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/f_jpg,pg_1/" + publicId + ".jpg";
    }


    // Method to generate proper PDF viewing URL
    public static String generatePDFViewingURL(String cloudinaryUrl, String publicId) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }

        // If it's a PDF file, ensure proper format
        if (publicId != null && publicId.toLowerCase().contains(".pdf")) {
            // For PDF files, you can create a URL that forces download or opens in browser
            return cloudinaryUrl.replace("/upload/", "/upload/fl_attachment/");
        }

        return cloudinaryUrl;
    }

    // Update saveToFirestore method to handle edit mode
    private void saveToFirestore(String finalCaseId, boolean isDraft, List<String> documentUrls) {
        updateProgressDialog("Saving to database...");

        // Main case reference
        DocumentReference caseRef = db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .document(finalCaseId);

        // Save case ID to root field
        Map<String, Object> caseRoot = new HashMap<>();
        caseRoot.put("caseId", finalCaseId);
        caseRoot.put("layerId", currentUserId);

        if (!isEditMode) {
            caseRoot.put("createdAt", new Date());
        }

        caseRoot.put("updatedAt", new Date());
        caseRoot.put("isDraft", isDraft);
        caseRoot.put("caseTitle", etCaseTitle.getText().toString().trim());
        caseRoot.put("clientName", etClientName.getText().toString().trim());
        caseRoot.put("caseType", spinnerCaseType.getSelectedItem().toString());
        caseRoot.put("caseStatus", spinnerCaseStatus.getSelectedItem().toString());

        // Use set for new cases, update for existing cases
        if (isEditMode) {
            caseRef.update(caseRoot).addOnSuccessListener(aVoid -> {
                saveClientDetails(caseRef, isDraft, documentUrls);
            }).addOnFailureListener(e -> {
                hideProgressDialog();
                Toast.makeText(this, "Failed to update case: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            caseRef.set(caseRoot).addOnSuccessListener(aVoid -> {
                saveClientDetails(caseRef, isDraft, documentUrls);
            }).addOnFailureListener(e -> {
                hideProgressDialog();
                Toast.makeText(this, "Failed to save case: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveClientDetails(DocumentReference caseRef, boolean isDraft, List<String> documentUrls) {
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("clientId", selectedClientId); // Add this line
        clientData.put("clientName", etClientName.getText().toString().trim());
        clientData.put("clientPhone", etClientPhone.getText().toString().trim());
        clientData.put("clientEmail", etClientEmail.getText().toString().trim());
        clientData.put("clientAddress", etClientAddress.getText().toString().trim());
        clientData.put("clientNID", etClientNID.getText().toString().trim());
        clientData.put("clientType", spinnerClientType.getSelectedItem().toString());
        clientData.put("clientImageUrl", selectedClientImageUrl); // Add this line
        clientData.put("createdAt", new Date());

        caseRef.collection("Client Details").document("client_info").set(clientData)
                .addOnSuccessListener(aVoid -> saveCaseDetails(caseRef, isDraft, documentUrls))
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to save client details", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCaseDetails(DocumentReference caseRef, boolean isDraft, List<String> documentUrls) {
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("caseTitle", etCaseTitle.getText().toString().trim());
        caseData.put("caseType", spinnerCaseType.getSelectedItem().toString());
        caseData.put("caseDescription", etCaseDescription.getText().toString().trim());
        caseData.put("courtName", etCourtName.getText().toString().trim());
        caseData.put("caseNumber", etCaseNumber.getText().toString().trim());
        caseData.put("filingDate", selectedFilingDate);
        caseData.put("courtLocation", etCourtLocation.getText().toString().trim());
        caseData.put("judgeName", etJudgeName.getText().toString().trim());
        caseData.put("createdAt", new Date());

        caseRef.collection("Case Details").document("case_info").set(caseData)
                .addOnSuccessListener(aVoid -> saveCourtDateReminder(caseRef, isDraft, documentUrls))
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to save case details", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCourtDateReminder(DocumentReference caseRef, boolean isDraft, List<String> documentUrls) {
        Map<String, Object> courtData = new HashMap<>();
        courtData.put("courtDate", selectedCourtDate);
        courtData.put("courtTime", selectedCourtTime);
        courtData.put("reminderSettings", spinnerReminderSettings.getSelectedItem().toString());
        courtData.put("hearingType", etHearingType.getText().toString().trim());
        courtData.put("createdAt", new Date());

        caseRef.collection("Court Date & Reminder").document("court_info").set(courtData)
                .addOnSuccessListener(aVoid -> savePartiesInvolved(caseRef, isDraft, documentUrls))
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to save court details", Toast.LENGTH_SHORT).show();
                });
    }

    private void savePartiesInvolved(DocumentReference caseRef, boolean isDraft, List<String> documentUrls) {
        Map<String, Object> partiesData = new HashMap<>();
        partiesData.put("plaintiffNames", etPlaintiffNames.getText().toString().trim());
        partiesData.put("defendantNames", etDefendantNames.getText().toString().trim());
        partiesData.put("opposingLawyer", etOpposingLawyer.getText().toString().trim());
        partiesData.put("opposingPartyContact", etOpposingPartyContact.getText().toString().trim());
        partiesData.put("createdAt", new Date());

        caseRef.collection("Parties Involved").document("parties_info").set(partiesData)
                .addOnSuccessListener(aVoid -> saveNotesDocuments(caseRef, isDraft, documentUrls))
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to save parties details", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNotesDocuments(DocumentReference caseRef, boolean isDraft, List<String> documentUrls) {
        Map<String, Object> notesData = new HashMap<>();
        notesData.put("privateNotes", etPrivateNotes.getText().toString().trim());
        notesData.put("tags", etTags.getText().toString().trim());
        notesData.put("documentUrls", documentUrls);
        notesData.put("attachedDocuments", attachedDocumentsList);
        notesData.put("createdAt", new Date());

        caseRef.collection("Notes & Documents").document("notes_info").set(notesData)
                .addOnSuccessListener(aVoid -> saveStatusTracking(caseRef, isDraft))
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to save notes and documents", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveStatusTracking(DocumentReference caseRef, boolean isDraft) {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("caseStatus", spinnerCaseStatus.getSelectedItem().toString());
        statusData.put("caseStage", spinnerCaseStage.getSelectedItem().toString());
        statusData.put("expectedOutcome", etExpectedOutcome.getText().toString().trim());
        statusData.put("isDraft", isDraft);
        statusData.put("createdAt", new Date());

        caseRef.collection("Status & Tracking").document("status_info").set(statusData)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    String message = isDraft ? "Case saved as draft successfully!" : "Case saved successfully!";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    showSuccessDialog(message);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Failed to save status and tracking", Toast.LENGTH_SHORT).show();
                });
    }

    // Update the success dialog method
    private void showSuccessDialog(String message) {
        String finalMessage = isEditMode ?
                (message.contains("draft") ? "Case updated as draft successfully!" : "Case updated successfully!") :
                message;

        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(finalMessage)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();

                    // If coming from CaseStatusActivity, go back to it
                    if (isEditMode && "CaseStatusActivity".equals(fromActivity)) {
                        Intent intent = new Intent();
                        intent.putExtra("caseUpdated", true);
                        setResult(RESULT_OK, intent);
                    }

                    finish(); // Close the activity
                })
                .setCancelable(false)
                .show();
    }


    // Update the button text for edit mode
    private void updateButtonsForEditMode() {
        if (isEditMode) {
            btnSaveCase.setText("Update Case");
            btnSaveDraft.setText("Update Draft");
        } else {
            btnSaveCase.setText("Save Case");
            btnSaveDraft.setText("Save Draft");
        }
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

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "Permission granted! You can now upload documents.", Toast.LENGTH_SHORT).show();
                openDocumentPicker();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean shouldShowRationale = false;

                    for (String permission : permissions) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                            shouldShowRationale = true;
                            break;
                        }
                    }

                    if (!shouldShowRationale) {
                        showSettingsDialog();
                    } else {
                        Toast.makeText(this, "Permission denied. Cannot upload documents.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permission denied. Cannot upload documents.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Storage permission is required to upload documents. Please enable it in app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Permission denied. Cannot upload documents.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
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
    public void onBackPressed() {
        // Check if user has entered any data
        if (hasUserEnteredData()) {
            new AlertDialog.Builder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Do you want to save as draft before leaving?")
                    .setPositiveButton("Save Draft", (dialog, which) -> {
                        saveCaseData(true);
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUserEnteredData() {
        return !TextUtils.isEmpty(etClientName.getText()) ||
                !TextUtils.isEmpty(etClientPhone.getText()) ||
                !TextUtils.isEmpty(etCaseTitle.getText()) ||
                !TextUtils.isEmpty(etCaseDescription.getText()) ||
                !TextUtils.isEmpty(selectedFilingDate) ||
                !attachedDocumentsList.isEmpty();
    }


    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString; // Return original if parsing fails
        }
    }


    // Error Fix 2: Add missing uploadNewDocuments method
    private void uploadNewDocuments(String caseId, boolean isDraft) {
        // Check if Cloudinary is ready (initialized in Application class)
        if (!isCloudinaryReady()) {
            hideProgressDialog();
            Toast.makeText(this, "Upload service not available. Saving case without documents.", Toast.LENGTH_LONG).show();
            saveToFirestore(caseId, isDraft, new ArrayList<>());
            return;
        }

        List<AttachedDocument> newDocuments = getNewDocuments();
        List<String> uploadedUrls = new ArrayList<>();
        int[] uploadCount = {0};
        int[] successCount = {0};
        int totalDocuments = newDocuments.size();

        updateProgressDialog("Uploading documents... (0/" + totalDocuments + ")");

        // Upload each new document
        for (int i = 0; i < newDocuments.size(); i++) {
            AttachedDocument document = newDocuments.get(i);
            Uri documentUri = Uri.parse(document.getUri());

            uploadSingleDocument(documentUri, document, caseId, uploadCount, successCount, uploadedUrls, totalDocuments, isDraft);
        }
    }



    private void showClientSelectionBottomSheet() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_client_selection_bottom_sheet, null);
        clientSelectionBottomSheet = new BottomSheetDialog(this);
        clientSelectionBottomSheet.setContentView(bottomSheetView);

        // Initialize bottom sheet components
        rvClientList = bottomSheetView.findViewById(R.id.rvClientList);
        searchViewClients = bottomSheetView.findViewById(R.id.searchViewClients);
        ImageView ivCloseBottomSheet = bottomSheetView.findViewById(R.id.ivCloseBottomSheet);
        LinearLayout layoutEmptyState = bottomSheetView.findViewById(R.id.layoutEmptyState);

        // Setup RecyclerView
        clientSelectionAdapter = new ChatListAdapter(this, clientChatList, chatModel -> {
            selectClient(chatModel);
            clientSelectionBottomSheet.dismiss();
        });

        rvClientList.setLayoutManager(new LinearLayoutManager(this));
        rvClientList.setAdapter(clientSelectionAdapter);

        // Setup search functionality
        setupClientSearch();

        // Close button click
        ivCloseBottomSheet.setOnClickListener(v -> clientSelectionBottomSheet.dismiss());

        // Load client chat list
        loadClientChatList();

        clientSelectionBottomSheet.show();
    }

    private void setupClientSearch() {
        searchViewClients.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterClientList(newText);
                return true;
            }
        });
    }

    private void filterClientList(String query) {
        if (clientSelectionAdapter != null) {
            List<ChatListModel> filteredList = new ArrayList<>();

            for (ChatListModel client : clientChatList) {
                if (client.getOtherUserName().toLowerCase().contains(query.toLowerCase()) ||
                        client.getUserType().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(client);
                }
            }

            // Update adapter with filtered list
            clientSelectionAdapter = new ChatListAdapter(this, filteredList, chatModel -> {
                selectClient(chatModel);
                clientSelectionBottomSheet.dismiss();
            });

            rvClientList.setAdapter(clientSelectionAdapter);

            // Show/hide empty state
            LinearLayout layoutEmptyState = clientSelectionBottomSheet.findViewById(R.id.layoutEmptyState);
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void loadClientChatList() {
        if (currentUserId == null) {
            return;
        }

        // Show loading state
        showProgressDialog("Loading clients...");

        // First load from UserChats
        loadFromUserChats();

        // Then load from Chats directory
        loadFromChatsDirectory();
    }

    private void loadFromUserChats() {
        databaseReference.child("UserChats").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            clientChatList.clear();

                            for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                                try {
                                    String otherUserId = chatSnapshot.child("otherUserId").getValue(String.class);
                                    String otherUserName = chatSnapshot.child("otherUserName").getValue(String.class);
                                    String otherUserType = chatSnapshot.child("otherUserType").getValue(String.class);
                                    String otherUserImageUrl = chatSnapshot.child("otherUserImageUrl").getValue(String.class);
                                    String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                                    Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);

                                    if (otherUserId != null && otherUserType != null &&
                                            (otherUserType.toLowerCase().contains("client") ||
                                                    otherUserType.toLowerCase().contains("general"))) {

                                        ChatListModel chatModel = new ChatListModel(
                                                otherUserId,
                                                otherUserName != null ? otherUserName : "Unknown Client",
                                                otherUserType != null ? otherUserType : "Client",
                                                lastMessage != null ? lastMessage : "No messages yet",
                                                lastMessageTime != null ? lastMessageTime : 0,
                                                otherUserImageUrl
                                        );

                                        clientChatList.add(chatModel);
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Error parsing UserChats data", e);
                                }
                            }

                            updateClientListUI();
                        }

                        hideProgressDialog();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideProgressDialog();
                        Toast.makeText(AddCaseInfoActivity.this, "Failed to load clients", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFromChatsDirectory() {
        databaseReference.child("Chats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> processedUsers = new HashSet<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    if (chatId != null && chatId.contains(currentUserId)) {
                        try {
                            String clientUserId = extractOtherUserId(chatId, currentUserId);

                            if (clientUserId != null && !clientUserId.equals(currentUserId)
                                    && !processedUsers.contains(clientUserId)) {

                                processedUsers.add(clientUserId);
                                getClientDetailsForSelection(clientUserId, chatSnapshot);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing chat: " + chatId, e);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat directory", error.toException());
            }
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

    private void getClientDetailsForSelection(String clientId, DataSnapshot chatSnapshot) {
        // Try GeneralPersons collection first
        databaseReference.child("Users").child("GeneralPersons").child("GeneralPersons")
                .child(clientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String fullName = snapshot.child("fullName").getValue(String.class);
                            String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                            String location = snapshot.child("location").getValue(String.class);

                            String clientName = fullName != null ? fullName : "Unknown Client";
                            String userType = "Client" + (location != null ? " • " + location : "");

                            getLastMessageForSelection(chatSnapshot, clientId, clientName, userType, profileImageUrl);
                        }
                        // Note: Not falling back to Lawyers collection as we only want clients
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "Failed to load client details", error.toException());
                    }
                });
    }

    private void getLastMessageForSelection(DataSnapshot chatSnapshot, String clientId,
                                            String clientName, String userType, String profileImageUrl) {
        String lastMessage = "No messages yet";
        long lastTimestamp = 0;

        for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
            if (timestamp != null && timestamp > lastTimestamp) {
                lastTimestamp = timestamp;
                String message = messageSnapshot.child("message").getValue(String.class);
                if (message != null && !message.isEmpty()) {
                    lastMessage = message;
                }
            }
        }

        ChatListModel chatModel = new ChatListModel(
                clientId,
                clientName,
                userType,
                lastMessage,
                lastTimestamp,
                profileImageUrl
        );

        addToClientListIfNotExists(chatModel);
    }

    private void addToClientListIfNotExists(ChatListModel chatModel) {
        boolean exists = false;
        for (ChatListModel existing : clientChatList) {
            if (existing.getOtherUserId().equals(chatModel.getOtherUserId())) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            clientChatList.add(chatModel);
            updateClientListUI();
        }
    }

    private void updateClientListUI() {
        if (clientSelectionAdapter != null) {
            runOnUiThread(() -> {
                clientSelectionAdapter.notifyDataSetChanged();

                // Update empty state
                if (clientSelectionBottomSheet != null) {
                    LinearLayout layoutEmptyState = clientSelectionBottomSheet.findViewById(R.id.layoutEmptyState);
                    if (layoutEmptyState != null) {
                        layoutEmptyState.setVisibility(clientChatList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }
    }

    private void selectClient(ChatListModel chatModel) {
        selectedClientId = chatModel.getOtherUserId();
        selectedClientName = chatModel.getOtherUserName();
        selectedClientImageUrl = chatModel.getOtherUserImageUrl();
        selectedClientType = chatModel.getUserType();

        // Update UI to show selected client
        cardSelectedClient.setVisibility(View.VISIBLE);
        tvSelectedClientName.setText(selectedClientName);
        tvSelectedClientType.setText(selectedClientType);

        // Load profile image
        if (selectedClientImageUrl != null && !selectedClientImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(selectedClientImageUrl)
                    .placeholder(R.drawable.ic_person_ai)
                    .error(R.drawable.ic_person_ai)
                    .circleCrop()
                    .into(ivSelectedClientImage);
        } else {
            ivSelectedClientImage.setImageResource(R.drawable.ic_person_ai);
        }

        // Auto-fill client name
        etClientName.setText(selectedClientName);

        // Try to get additional client details from the selected client
        loadSelectedClientDetails();

        Toast.makeText(this, "Client selected: " + selectedClientName, Toast.LENGTH_SHORT).show();
    }

    private void loadSelectedClientDetails() {
        if (selectedClientId.isEmpty()) return;

        // Load additional client details from Users/GeneralPersons
        databaseReference.child("Users").child("GeneralPersons").child("GeneralPersons")
                .child(selectedClientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String phone = snapshot.child("phoneNumber").getValue(String.class);
                            String email = snapshot.child("email").getValue(String.class);
                            String address = snapshot.child("address").getValue(String.class);
                            String nid = snapshot.child("nid").getValue(String.class);

                            // Auto-fill the form fields
                            if (phone != null && !phone.isEmpty()) {
                                selectedClientPhone = phone;
                                etClientPhone.setText(phone);
                            }

                            if (email != null && !email.isEmpty()) {
                                selectedClientEmail = email;
                                etClientEmail.setText(email);
                            }

                            if (address != null && !address.isEmpty()) {
                                etClientAddress.setText(address);
                            }

                            if (nid != null && !nid.isEmpty()) {
                                etClientNID.setText(nid);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "Failed to load client details", error.toException());
                    }
                });
    }

    private void removeSelectedClient() {
        selectedClientId = "";
        selectedClientName = "";
        selectedClientPhone = "";
        selectedClientEmail = "";
        selectedClientImageUrl = "";
        selectedClientType = "";

        cardSelectedClient.setVisibility(View.GONE);

        // Clear auto-filled data
        etClientName.setText("");
        etClientPhone.setText("");
        etClientEmail.setText("");
        etClientAddress.setText("");
        etClientNID.setText("");

        Toast.makeText(this, "Client selection removed", Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}