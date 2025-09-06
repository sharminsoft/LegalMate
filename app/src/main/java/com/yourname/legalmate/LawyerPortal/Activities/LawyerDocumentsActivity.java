package com.yourname.legalmate.LawyerPortal.Activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.yourname.legalmate.LawyerPortal.Adapters.DocumentTemplateAdapter;
import com.yourname.legalmate.LawyerPortal.Models.DocumentTemplate;
import com.yourname.legalmate.LawyerPortal.Models.DocumentField;
import com.yourname.legalmate.LawyerPortal.Models.DocumentTemplateConfig;
import com.yourname.legalmate.R;
import com.yourname.legalmate.utils.PDFGeneratorUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LawyerDocumentsActivity extends AppCompatActivity {

    private static final String TAG = "LawyerDocumentsActivity";

    private RecyclerView recyclerDocumentTemplates;
    private LinearLayout emptyStateLayout;
    private ExtendedFloatingActionButton fabCreateDocument;
    private MaterialButton btnBangla, btnEnglish;
    private DocumentTemplateAdapter adapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String currentUserId;
    private String currentLanguage = "en";

    private List<DocumentTemplate> documentList = new ArrayList<>();

    // BottomSheet variables
    private BottomSheetDialog bottomSheetDialog;
    private LinearLayout fieldsContainer;
    private TextView tvBottomSheetTitle, tvBottomSheetSubtitle, tvProgress;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnSave, btnCancel, btnCloseBottomSheet;
    private Map<String, TextInputEditText> fieldViews;
    private DocumentField[] currentFields;
    private String currentDocumentType, currentDisplayType;

    // Permission handling
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> requestManageStorageLauncher;
    private DocumentTemplate pendingPDFDocument;

    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lawyer_documents);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initPermissionLaunchers();
        initFirebase();
        initViews();
        setupRecyclerView();
        setupLanguageToggle();
        loadDocuments();
    }

    private void initPermissionLaunchers() {
        // For Android 13+ (API 33+) and below
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, currentLanguage.equals("bn") ?
                                "অনুমতি প্রদান করা হয়েছে" : "Permission granted", Toast.LENGTH_SHORT).show();
                        if (pendingPDFDocument != null) {
                            generatePDFAfterPermission(pendingPDFDocument);
                        }
                    } else {
                        handlePermissionDenied();
                    }
                });

        // For Android 11+ (API 30+) MANAGE_EXTERNAL_STORAGE
        requestManageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, currentLanguage.equals("bn") ?
                                    "স্টোরেজ অনুমতি প্রদান করা হয়েছে" : "Storage permission granted", Toast.LENGTH_SHORT).show();
                            if (pendingPDFDocument != null) {
                                generatePDFAfterPermission(pendingPDFDocument);
                            }
                        } else {
                            handlePermissionDenied();
                        }
                    }
                });
    }

    private void initFirebase() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        recyclerDocumentTemplates = findViewById(R.id.recyclerDocumentTemplates);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        fabCreateDocument = findViewById(R.id.fabCreateDocument);
        btnBangla = findViewById(R.id.btnBangla);
        btnEnglish = findViewById(R.id.btnEnglish);
        toolbar= findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        fabCreateDocument.setOnClickListener(v -> showDocumentTypeSelectionDialog());
    }

    private void setupRecyclerView() {
        adapter = new DocumentTemplateAdapter(documentList, currentLanguage, this::onDocumentClick);
        recyclerDocumentTemplates.setLayoutManager(new LinearLayoutManager(this));
        recyclerDocumentTemplates.setAdapter(adapter);
    }

    private void setupLanguageToggle() {
        btnEnglish.setOnClickListener(v -> {
            currentLanguage = "en";
            updateLanguageButtons();
            adapter.updateLanguage(currentLanguage);
            updateEmptyStateText();
        });

        btnBangla.setOnClickListener(v -> {
            currentLanguage = "bn";
            updateLanguageButtons();
            adapter.updateLanguage(currentLanguage);
            updateEmptyStateText();
        });

        updateLanguageButtons();
    }

    private void updateLanguageButtons() {
        if (currentLanguage.equals("en")) {
            btnEnglish.setTextColor(getColor(R.color.colorPrimary));
            btnBangla.setTextColor(getColor(android.R.color.darker_gray));
            btnEnglish.setSelected(true);
            btnBangla.setSelected(false);
        } else {
            btnBangla.setTextColor(getColor(R.color.colorPrimary));
            btnEnglish.setTextColor(getColor(android.R.color.darker_gray));
            btnBangla.setSelected(true);
            btnEnglish.setSelected(false);
        }
    }

    private void updateEmptyStateText() {
        // Update empty state text based on language
    }

    private void showDocumentTypeSelectionDialog() {
        String[] types = {"Petition", "Affidavit", "Contract"};
        String[] typesBangla = {"আবেদন", "হলফনামা", "চুক্তি"};

        String[] displayTypes = currentLanguage.equals("bn") ? typesBangla : types;
        String[] actualTypes = {DocumentTemplateConfig.TYPE_PETITION,
                DocumentTemplateConfig.TYPE_AFFIDAVIT,
                DocumentTemplateConfig.TYPE_CONTRACT};

        new MaterialAlertDialogBuilder(this)
                .setTitle(currentLanguage.equals("bn") ? "নথির ধরন নির্বাচন করুন" : "Select Document Type")
                .setItems(displayTypes, (dialog, which) -> {
                    showDocumentCreationDialog(actualTypes[which], displayTypes[which]);
                })
                .show();
    }

    private void showDocumentCreationDialog(String documentType, String displayType) {
        currentDocumentType = documentType;
        currentDisplayType = displayType;
        currentFields = DocumentTemplateConfig.getFieldsByType(documentType);

        // Create BottomSheet Dialog
        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogStyle);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_document_creation, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        initBottomSheetViews(bottomSheetView);
        setupBottomSheetHeader(displayType);
        createFormFields();
        setupBottomSheetButtons();
        setupProgressTracking();
        configureBottomSheetBehavior();

        bottomSheetDialog.show();
    }

    private void initBottomSheetViews(View bottomSheetView) {
        fieldsContainer = bottomSheetView.findViewById(R.id.fieldsContainer);
        tvBottomSheetTitle = bottomSheetView.findViewById(R.id.tvBottomSheetTitle);
        tvBottomSheetSubtitle = bottomSheetView.findViewById(R.id.tvBottomSheetSubtitle);
        tvProgress = bottomSheetView.findViewById(R.id.tvProgress);
        progressIndicator = bottomSheetView.findViewById(R.id.progressIndicator);
        btnSave = bottomSheetView.findViewById(R.id.btnSave);
        btnCancel = bottomSheetView.findViewById(R.id.btnCancel);
        btnCloseBottomSheet = bottomSheetView.findViewById(R.id.btnCloseBottomSheet);

        fieldViews = new HashMap<>();
    }

    private void setupBottomSheetHeader(String displayType) {
        String title = currentLanguage.equals("bn") ?
                displayType + " তৈরি করুন" : "Create " + displayType;

        String subtitle = currentLanguage.equals("bn") ?
                "নিচের বিবরণ পূরণ করুন" : "Fill in the details below";

        tvBottomSheetTitle.setText(title);
        tvBottomSheetSubtitle.setText(subtitle);
    }

    private void createFormFields() {
        fieldsContainer.removeAllViews();
        fieldViews.clear();

        for (int i = 0; i < currentFields.length; i++) {
            DocumentField field = currentFields[i];

            LinearLayout fieldContainer = new LinearLayout(this);
            fieldContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            containerParams.setMargins(0, 0, 0, 24);
            fieldContainer.setLayoutParams(containerParams);

            TextInputLayout inputLayout = new TextInputLayout(this, null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            inputLayout.setHint(currentLanguage.equals("bn") ? field.getLabelBangla() : field.getLabel());
            inputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            inputLayout.setBoxStrokeColor(getColor(R.color.colorPrimary));
            float radius = 12f;
            inputLayout.setBoxCornerRadii(radius, radius, radius, radius);

            TextInputEditText editText = new TextInputEditText(inputLayout.getContext());

            switch (field.getType()) {
                case "number":
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;
                case "date":
                    editText.setInputType(InputType.TYPE_NULL);
                    editText.setFocusable(false);
                    editText.setClickable(true);
                    editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_calendar, 0);
                    editText.setOnClickListener(v -> showDatePickerForField(editText));
                    break;
                case "textarea":
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    editText.setLines(3);
                    editText.setMaxLines(6);
                    editText.setVerticalScrollBarEnabled(true);
                    break;
                case "email":
                    editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    break;
                case "phone":
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    break;
                default:
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    break;
            }

            if (field.getHint() != null) {
                String hint = currentLanguage.equals("bn") ? field.getHintBangla() : field.getHint();
                if (hint != null) {
                    inputLayout.setHelperText(hint);
                }
            }

            if (field.isRequired()) {
                String currentHint = inputLayout.getHint().toString();
                inputLayout.setHint(currentHint + " *");
            }

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updateProgress();
                    validateField(field, editText, inputLayout);
                }
            });

            inputLayout.addView(editText);
            fieldContainer.addView(inputLayout);
            fieldsContainer.addView(fieldContainer);

            fieldViews.put(field.getKey(), editText);
        }

        updateProgress();
    }

    private void showDatePickerForField(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.DatePickerDialogStyle,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    editText.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void validateField(DocumentField field, TextInputEditText editText, TextInputLayout inputLayout) {
        String value = editText.getText().toString().trim();

        if (field.isRequired() && value.isEmpty()) {
            inputLayout.setError(currentLanguage.equals("bn") ?
                    "এই ক্ষেত্রটি প্রয়োজনীয়" : "This field is required");
        } else if (!value.isEmpty()) {
            switch (field.getType()) {
                case "email":
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                        inputLayout.setError(currentLanguage.equals("bn") ?
                                "সঠিক ইমেইল দিন" : "Enter valid email");
                    } else {
                        inputLayout.setError(null);
                    }
                    break;
                case "phone":
                    if (value.length() < 10) {
                        inputLayout.setError(currentLanguage.equals("bn") ?
                                "সঠিক ফোন নম্বর দিন" : "Enter valid phone number");
                    } else {
                        inputLayout.setError(null);
                    }
                    break;
                default:
                    inputLayout.setError(null);
                    break;
            }
        } else {
            inputLayout.setError(null);
        }
    }

    private void setupProgressTracking() {
        progressIndicator.setMax(currentFields.length);
        updateProgress();
    }

    private void updateProgress() {
        int filledFields = 0;
        for (DocumentField field : currentFields) {
            TextInputEditText editText = fieldViews.get(field.getKey());
            if (editText != null && !editText.getText().toString().trim().isEmpty()) {
                filledFields++;
            }
        }

        progressIndicator.setProgress(filledFields);
        tvProgress.setText(filledFields + "/" + currentFields.length + " completed");

        boolean canSave = validateAllRequiredFields();
        btnSave.setEnabled(canSave);
        btnSave.setAlpha(canSave ? 1.0f : 0.6f);
    }

    private boolean validateAllRequiredFields() {
        for (DocumentField field : currentFields) {
            if (field.isRequired()) {
                TextInputEditText editText = fieldViews.get(field.getKey());
                if (editText == null || editText.getText().toString().trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setupBottomSheetButtons() {
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        btnCloseBottomSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            if (validateAllRequiredFields()) {
                saveDocumentFromBottomSheet();
            } else {
                showValidationErrors();
            }
        });
    }

    private void showValidationErrors() {
        for (DocumentField field : currentFields) {
            if (field.isRequired()) {
                TextInputEditText editText = fieldViews.get(field.getKey());
                if (editText != null && editText.getText().toString().trim().isEmpty()) {
                    editText.requestFocus();
                    editText.getParent().requestChildFocus(editText, editText);
                    break;
                }
            }
        }

        Toast.makeText(this, currentLanguage.equals("bn") ?
                "দয়া করে সকল প্রয়োজনীয় ক্ষেত্র পূরণ করুন" :
                "Please fill all required fields", Toast.LENGTH_SHORT).show();
    }

    private void saveDocumentFromBottomSheet() {
        btnSave.setText(currentLanguage.equals("bn") ? "সংরক্ষণ করা হচ্ছে..." : "Saving...");
        btnSave.setEnabled(false);

        Map<String, Object> formData = new HashMap<>();
        for (DocumentField field : currentFields) {
            TextInputEditText editText = fieldViews.get(field.getKey());
            if (editText != null) {
                String value = editText.getText().toString().trim();
                formData.put(field.getKey(), value);
            }
        }

        DocumentTemplate document = new DocumentTemplate();
        document.setType(currentDocumentType);
        document.setTitle(currentDisplayType);
        document.setTitleBangla(getBanglaTitle(currentDocumentType));
        document.setFormData(formData);
        document.setLanguage(currentLanguage);
        document.setCreatedAt(new Date());
        document.setUpdatedAt(new Date());
        document.setStatus("draft");

        DocumentReference docRef = firestore.collection("Document Creation")
                .document(currentUserId)
                .collection(currentDocumentType)
                .document();

        document.setId(docRef.getId());

        docRef.set(document)
                .addOnSuccessListener(aVoid -> {
                    btnSave.setText(currentLanguage.equals("bn") ? "সংরক্ষণ করুন" : "Save Document");
                    btnSave.setEnabled(true);

                    bottomSheetDialog.dismiss();

                    Toast.makeText(this, currentLanguage.equals("bn") ?
                            "নথি সফলভাবে সংরক্ষিত হয়েছে" :
                            "Document saved successfully", Toast.LENGTH_SHORT).show();

                    loadDocuments();
                })
                .addOnFailureListener(e -> {
                    btnSave.setText(currentLanguage.equals("bn") ? "সংরক্ষণ করুন" : "Save Document");
                    btnSave.setEnabled(true);

                    Log.e(TAG, "Error saving document", e);
                    Toast.makeText(this, currentLanguage.equals("bn") ?
                            "নথি সংরক্ষণে ত্রুটি হয়েছে: " + e.getMessage() :
                            "Error saving document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void configureBottomSheetBehavior() {
        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.6));
        }
    }

    private String getBanglaTitle(String documentType) {
        switch (documentType) {
            case DocumentTemplateConfig.TYPE_PETITION:
                return "আবেদন";
            case DocumentTemplateConfig.TYPE_AFFIDAVIT:
                return "হলফনামা";
            case DocumentTemplateConfig.TYPE_CONTRACT:
                return "চুক্তি";
            default:
                return "";
        }
    }

    private void loadDocuments() {
        if (currentUserId.isEmpty()) return;

        documentList.clear();

        String[] types = {DocumentTemplateConfig.TYPE_PETITION,
                DocumentTemplateConfig.TYPE_AFFIDAVIT,
                DocumentTemplateConfig.TYPE_CONTRACT};

        final int[] completedTypes = {0};

        for (String type : types) {
            firestore.collection("Document Creation")
                    .document(currentUserId)
                    .collection(type)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            DocumentTemplate template = document.toObject(DocumentTemplate.class);
                            if (template != null) {
                                template.setId(document.getId());
                                documentList.add(template);
                            }
                        }
                        completedTypes[0]++;
                        if (completedTypes[0] == types.length) {
                            updateUI();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading documents for type: " + type, e);
                        completedTypes[0]++;
                        if (completedTypes[0] == types.length) {
                            Toast.makeText(this, currentLanguage.equals("bn") ?
                                    "নথি লোড করতে ত্রুটি হয়েছে" :
                                    "Error loading documents", Toast.LENGTH_SHORT).show();
                            updateUI();
                        }
                    });
        }
    }

    private void updateUI() {
        if (documentList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerDocumentTemplates.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerDocumentTemplates.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void onDocumentClick(DocumentTemplate document) {
        showDocumentOptionsDialog(document);
    }

    private void showDocumentOptionsDialog(DocumentTemplate document) {
        String title = currentLanguage.equals("bn") ? document.getTitleBangla() : document.getTitle();

        String[] options = currentLanguage.equals("bn") ?
                new String[]{"দেখুন", "সম্পাদনা করুন", "PDF তৈরি করুন", "মুছে ফেলুন"} :
                new String[]{"View", "Edit", "Generate PDF", "Delete"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showViewDocumentBottomSheet(document);
                            break;
                        case 1:
                            showEditDocumentBottomSheet(document);
                            break;
                        case 2:
                            generatePDF(document);
                            break;
                        case 3:
                            deleteDocument(document);
                            break;
                    }
                })
                .show();
    }

    // Convert ViewDocument to BottomSheet
    private void showViewDocumentBottomSheet(DocumentTemplate document) {
        BottomSheetDialog viewBottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogStyle);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_view_document, null);
        viewBottomSheetDialog.setContentView(bottomSheetView);

        // Initialize views
        TextView titleView = bottomSheetView.findViewById(R.id.tvViewTitle);
        TextView subtitleView = bottomSheetView.findViewById(R.id.tvViewSubtitle);
        ScrollView scrollView = bottomSheetView.findViewById(R.id.scrollViewContent);
        LinearLayout contentContainer = bottomSheetView.findViewById(R.id.contentContainer);
        MaterialButton btnClose = bottomSheetView.findViewById(R.id.btnCloseView);

        String title = currentLanguage.equals("bn") ? document.getTitleBangla() : document.getTitle();
        titleView.setText(title);
        subtitleView.setText(currentLanguage.equals("bn") ? "নথির বিস্তারিত তথ্য" : "Document Details");

        DocumentField[] fields = DocumentTemplateConfig.getFieldsByType(document.getType());

        for (DocumentField field : fields) {
            if (document.getFormData().containsKey(field.getKey())) {
                String label = currentLanguage.equals("bn") ? field.getLabelBangla() : field.getLabel();
                Object valueObj = document.getFormData().get(field.getKey());
                String value = valueObj != null ? valueObj.toString() : "";

                if (!value.isEmpty()) {
                    // Create material card for each field
                    MaterialCardView cardView = new MaterialCardView(this);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    cardParams.setMargins(0, 0, 0, 16);
                    cardView.setLayoutParams(cardParams);
                    cardView.setCardElevation(4);
                    cardView.setRadius(12);

                    LinearLayout itemLayout = new LinearLayout(this);
                    itemLayout.setOrientation(LinearLayout.VERTICAL);
                    itemLayout.setPadding(24, 20, 24, 20);

                    // Label
                    TextView labelView = new TextView(this);
                    labelView.setText(label);
                    labelView.setTextSize(14);
                    labelView.setTypeface(null, android.graphics.Typeface.BOLD);
                    labelView.setTextColor(getColor(R.color.colorPrimary));

                    // Value
                    TextView valueView = new TextView(this);
                    valueView.setText(value);
                    valueView.setTextSize(16);
                    LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    valueParams.setMargins(0, 8, 0, 0);
                    valueView.setLayoutParams(valueParams);

                    itemLayout.addView(labelView);
                    itemLayout.addView(valueView);
                    cardView.addView(itemLayout);
                    contentContainer.addView(cardView);
                }
            }
        }

        btnClose.setOnClickListener(v -> viewBottomSheetDialog.dismiss());

        // Configure BottomSheet behavior
        View bottomSheetInternal = viewBottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.7));
        }

        viewBottomSheetDialog.show();
    }

    // Convert EditDocument to BottomSheet
    private void showEditDocumentBottomSheet(DocumentTemplate document) {
        BottomSheetDialog editBottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogStyle);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_document_creation, null);
        editBottomSheetDialog.setContentView(bottomSheetView);

        // Initialize views
        LinearLayout fieldsContainer = bottomSheetView.findViewById(R.id.fieldsContainer);
        TextView tvBottomSheetTitle = bottomSheetView.findViewById(R.id.tvBottomSheetTitle);
        TextView tvBottomSheetSubtitle = bottomSheetView.findViewById(R.id.tvBottomSheetSubtitle);
        TextView tvProgress = bottomSheetView.findViewById(R.id.tvProgress);
        LinearProgressIndicator progressIndicator = bottomSheetView.findViewById(R.id.progressIndicator);
        MaterialButton btnSave = bottomSheetView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = bottomSheetView.findViewById(R.id.btnCancel);
        MaterialButton btnCloseBottomSheet = bottomSheetView.findViewById(R.id.btnCloseBottomSheet);

        Map<String, TextInputEditText> editFieldViews = new HashMap<>();

        String title = currentLanguage.equals("bn") ? document.getTitleBangla() : document.getTitle();
        tvBottomSheetTitle.setText((currentLanguage.equals("bn") ? "সম্পাদনা করুন: " : "Edit: ") + title);
        tvBottomSheetSubtitle.setText(currentLanguage.equals("bn") ?
                "তথ্য পরিবর্তন করুন" : "Modify the information");

        DocumentField[] fields = DocumentTemplateConfig.getFieldsByType(document.getType());
        progressIndicator.setMax(fields.length);

        // Create edit fields
        for (DocumentField field : fields) {
            LinearLayout fieldContainer = new LinearLayout(this);
            fieldContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            containerParams.setMargins(0, 0, 0, 24);
            fieldContainer.setLayoutParams(containerParams);

            TextInputLayout inputLayout = new TextInputLayout(this, null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            inputLayout.setHint(currentLanguage.equals("bn") ? field.getLabelBangla() : field.getLabel());
            inputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            inputLayout.setBoxStrokeColor(getColor(R.color.colorPrimary));
            float radius = 12f;
            inputLayout.setBoxCornerRadii(radius, radius, radius, radius);

            TextInputEditText editText = new TextInputEditText(inputLayout.getContext());

            // Pre-fill with existing data
            if (document.getFormData().containsKey(field.getKey())) {
                Object existingValueObj = document.getFormData().get(field.getKey());
                String existingValue = existingValueObj != null ? existingValueObj.toString() : "";
                editText.setText(existingValue);
            }

            // Set input type based on field type
            switch (field.getType()) {
                case "number":
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;
                case "date":
                    editText.setInputType(InputType.TYPE_NULL);
                    editText.setFocusable(false);
                    editText.setClickable(true);
                    editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_calendar, 0);
                    editText.setOnClickListener(v -> showDatePickerForField(editText));
                    break;
                case "textarea":
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    editText.setLines(3);
                    editText.setMaxLines(6);
                    editText.setVerticalScrollBarEnabled(true);
                    break;
                case "email":
                    editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    break;
                case "phone":
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    break;
                default:
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    break;
            }

            if (field.getHint() != null) {
                String hint = currentLanguage.equals("bn") ? field.getHintBangla() : field.getHint();
                if (hint != null) {
                    inputLayout.setHelperText(hint);
                }
            }

            if (field.isRequired()) {
                String currentHint = inputLayout.getHint().toString();
                inputLayout.setHint(currentHint + " *");
            }

            // Add TextWatcher for progress tracking
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updateEditProgress(fields, editFieldViews, progressIndicator, tvProgress, btnSave);
                    validateField(field, editText, inputLayout);
                }
            });

            inputLayout.addView(editText);
            fieldContainer.addView(inputLayout);
            fieldsContainer.addView(fieldContainer);
            editFieldViews.put(field.getKey(), editText);
        }

        // Update initial progress
        updateEditProgress(fields, editFieldViews, progressIndicator, tvProgress, btnSave);

        // Setup buttons
        btnCancel.setOnClickListener(v -> editBottomSheetDialog.dismiss());
        btnCloseBottomSheet.setOnClickListener(v -> editBottomSheetDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            if (validateAllEditRequiredFields(fields, editFieldViews)) {
                updateDocumentFromBottomSheet(document, fields, editFieldViews, editBottomSheetDialog, btnSave);
            } else {
                showEditValidationErrors(fields, editFieldViews);
            }
        });

        // Configure BottomSheet behavior
        View bottomSheetInternal = editBottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        }

        editBottomSheetDialog.show();
    }

    private void updateEditProgress(DocumentField[] fields, Map<String, TextInputEditText> fieldViews,
                                    LinearProgressIndicator progressIndicator, TextView tvProgress, MaterialButton btnSave) {
        int filledFields = 0;
        for (DocumentField field : fields) {
            TextInputEditText editText = fieldViews.get(field.getKey());
            if (editText != null && !editText.getText().toString().trim().isEmpty()) {
                filledFields++;
            }
        }

        progressIndicator.setProgress(filledFields);
        tvProgress.setText(filledFields + "/" + fields.length + " completed");

        boolean canSave = validateAllEditRequiredFields(fields, fieldViews);
        btnSave.setEnabled(canSave);
        btnSave.setAlpha(canSave ? 1.0f : 0.6f);
    }

    private boolean validateAllEditRequiredFields(DocumentField[] fields, Map<String, TextInputEditText> fieldViews) {
        for (DocumentField field : fields) {
            if (field.isRequired()) {
                TextInputEditText editText = fieldViews.get(field.getKey());
                if (editText == null || editText.getText().toString().trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showEditValidationErrors(DocumentField[] fields, Map<String, TextInputEditText> fieldViews) {
        for (DocumentField field : fields) {
            if (field.isRequired()) {
                TextInputEditText editText = fieldViews.get(field.getKey());
                if (editText != null && editText.getText().toString().trim().isEmpty()) {
                    editText.requestFocus();
                    editText.getParent().requestChildFocus(editText, editText);
                    break;
                }
            }
        }

        Toast.makeText(this, currentLanguage.equals("bn") ?
                "দয়া করে সকল প্রয়োজনীয় ক্ষেত্র পূরণ করুন" :
                "Please fill all required fields", Toast.LENGTH_SHORT).show();
    }

    private void updateDocumentFromBottomSheet(DocumentTemplate document, DocumentField[] fields,
                                               Map<String, TextInputEditText> fieldViews,
                                               BottomSheetDialog bottomSheetDialog, MaterialButton btnSave) {
        btnSave.setText(currentLanguage.equals("bn") ? "আপডেট করা হচ্ছে..." : "Updating...");
        btnSave.setEnabled(false);

        Map<String, Object> formData = new HashMap<>();
        for (DocumentField field : fields) {
            TextInputEditText editText = fieldViews.get(field.getKey());
            if (editText != null) {
                String value = editText.getText().toString().trim();
                formData.put(field.getKey(), value);
            }
        }

        document.setFormData(formData);
        document.setUpdatedAt(new Date());

        firestore.collection("Document Creation")
                .document(currentUserId)
                .collection(document.getType())
                .document(document.getId())
                .set(document)
                .addOnSuccessListener(aVoid -> {
                    btnSave.setText(currentLanguage.equals("bn") ? "আপডেট করুন" : "Update");
                    btnSave.setEnabled(true);

                    bottomSheetDialog.dismiss();

                    Toast.makeText(this, currentLanguage.equals("bn") ?
                            "নথি সফলভাবে আপডেট হয়েছে" :
                            "Document updated successfully", Toast.LENGTH_SHORT).show();
                    loadDocuments();
                })
                .addOnFailureListener(e -> {
                    btnSave.setText(currentLanguage.equals("bn") ? "আপডেট করুন" : "Update");
                    btnSave.setEnabled(true);

                    Log.e(TAG, "Error updating document", e);
                    Toast.makeText(this, currentLanguage.equals("bn") ?
                            "নথি আপডেটে ত্রুটি হয়েছে: " + e.getMessage() :
                            "Error updating document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Modern permission handling for PDF generation
    private void generatePDF(DocumentTemplate document) {
        pendingPDFDocument = document;

        if (hasStoragePermission()) {
            generatePDFAfterPermission(document);
        } else {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // For Android 13+, we don't need WRITE_EXTERNAL_STORAGE for app-specific directories
            // But if you want to save in public directories, you might need MANAGE_EXTERNAL_STORAGE
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11-12
            // For Android 11-12, check MANAGE_EXTERNAL_STORAGE if saving to public directories
            return Environment.isExternalStorageManager() ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else { // Android 10 and below
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // For Android 13+, we can use app-specific storage without permission
            // But show a dialog explaining this
            new MaterialAlertDialogBuilder(this)
                    .setTitle(currentLanguage.equals("bn") ? "PDF সংরক্ষণ" : "PDF Storage")
                    .setMessage(currentLanguage.equals("bn") ?
                            "PDF ফাইলটি অ্যাপের স্টোরেজে সংরক্ষিত হবে। আপনি ফাইল ম্যানেজার থেকে এটি খুঁজে পেতে পারেন।" :
                            "The PDF will be saved to app storage. You can find it in your file manager.")
                    .setPositiveButton(currentLanguage.equals("bn") ? "এগিয়ে যান" : "Continue", (dialog, which) -> {
                        generatePDFAfterPermission(pendingPDFDocument);
                    })
                    .setNegativeButton(currentLanguage.equals("bn") ? "বাতিল" : "Cancel", null)
                    .show();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11-12
            // For Android 11-12, show dialog and request MANAGE_EXTERNAL_STORAGE
            new MaterialAlertDialogBuilder(this)
                    .setTitle(currentLanguage.equals("bn") ? "স্টোরেজ অনুমতি প্রয়োজন" : "Storage Permission Required")
                    .setMessage(currentLanguage.equals("bn") ?
                            "PDF তৈরি করার জন্য স্টোরেজ অ্যাক্সেস প্রয়োজন। অনুমতি দিন।" :
                            "Storage access is required to generate PDF. Please grant permission.")
                    .setPositiveButton(currentLanguage.equals("bn") ? "অনুমতি দিন" : "Grant Permission", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            requestManageStorageLauncher.launch(intent);
                        } catch (Exception e) {
                            // Fallback to regular permission
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                    })
                    .setNegativeButton(currentLanguage.equals("bn") ? "বাতিল" : "Cancel", null)
                    .show();
        } else { // Android 10 and below
            // For Android 10 and below, request WRITE_EXTERNAL_STORAGE
            new MaterialAlertDialogBuilder(this)
                    .setTitle(currentLanguage.equals("bn") ? "স্টোরেজ অনুমতি প্রয়োজন" : "Storage Permission Required")
                    .setMessage(currentLanguage.equals("bn") ?
                            "PDF তৈরি করার জন্য স্টোরেজ অ্যাক্সেস প্রয়োজন।" :
                            "Storage access is required to generate PDF.")
                    .setPositiveButton(currentLanguage.equals("bn") ? "অনুমতি দিন" : "Grant Permission", (dialog, which) -> {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    })
                    .setNegativeButton(currentLanguage.equals("bn") ? "বাতিল" : "Cancel", null)
                    .show();
        }
    }

    private void handlePermissionDenied() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(currentLanguage.equals("bn") ? "অনুমতি প্রয়োজন" : "Permission Required")
                .setMessage(currentLanguage.equals("bn") ?
                        "PDF তৈরি করার জন্য স্টোরেজ অনুমতি প্রয়োজন। সেটিংস থেকে অনুমতি দিন।" :
                        "Storage permission is required to generate PDF. Please enable it from settings.")
                .setPositiveButton(currentLanguage.equals("bn") ? "সেটিংসে যান" : "Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(currentLanguage.equals("bn") ? "বাতিল" : "Cancel", null)
                .show();
    }

    private void generatePDFAfterPermission(DocumentTemplate document) {
        Toast.makeText(this, currentLanguage.equals("bn") ?
                "PDF তৈরি করা হচ্ছে..." : "Generating PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            PDFGeneratorUtil.PDFResult result = PDFGeneratorUtil.generateDocumentPDF(
                    this, document, currentLanguage);

            runOnUiThread(() -> {
                if (result.success) {
                    Toast.makeText(this, currentLanguage.equals("bn") ?
                                    "PDF সফলভাবে তৈরি হয়েছে" : "PDF generated successfully",
                            Toast.LENGTH_LONG).show();
                    openPDF(result.filePath);
                } else {
                    Toast.makeText(this, currentLanguage.equals("bn") ?
                                    "PDF তৈরিতে ত্রুটি: " + result.errorMessage :
                                    "Error generating PDF: " + result.errorMessage,
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();

        pendingPDFDocument = null;
    }

    private void openPDF(String filePath) {
        try {
            File file = new File(filePath);
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, currentLanguage.equals("bn") ?
                        "PDF দেখার জন্য কোন অ্যাপ্লিকেশন নেই" :
                        "No PDF viewer app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDocument(DocumentTemplate document) {
        String title = currentLanguage.equals("bn") ? document.getTitleBangla() : document.getTitle();

        new MaterialAlertDialogBuilder(this)
                .setTitle(currentLanguage.equals("bn") ? "নিশ্চিত করুন" : "Confirm Delete")
                .setMessage(currentLanguage.equals("bn") ?
                        "আপনি কি এই " + title + " মুছে ফেলতে চান?" :
                        "Are you sure you want to delete this " + title + "?")
                .setPositiveButton(currentLanguage.equals("bn") ? "মুছে ফেলুন" : "Delete",
                        (dialog, which) -> {
                            firestore.collection("Document Creation")
                                    .document(currentUserId)
                                    .collection(document.getType())
                                    .document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, currentLanguage.equals("bn") ?
                                                "নথি সফলভাবে মুছে ফেলা হয়েছে" :
                                                "Document deleted successfully", Toast.LENGTH_SHORT).show();
                                        loadDocuments();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error deleting document", e);
                                        Toast.makeText(this, currentLanguage.equals("bn") ?
                                                "নথি মুছতে ত্রুটি হয়েছে: " + e.getMessage() :
                                                "Error deleting document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                .setNegativeButton(currentLanguage.equals("bn") ? "বাতিল" : "Cancel", null)
                .show();
    }
}