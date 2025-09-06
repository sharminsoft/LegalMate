package com.yourname.legalmate.LawyerPortal.AccountCreation;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.LawyerPortal.Activities.LawyerDashboardActivity;
import com.yourname.legalmate.R;
import com.yourname.legalmate.utils.CloudinaryConfig;
import com.yourname.legalmate.utils.YearPickerDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LawyerProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "LawyerProfileSetup";
    private static final int STORAGE_PERMISSION_CODE = 100;

    // UI Components
    private CircleImageView ivProfilePhoto;
    private MaterialButton btnSelectPhoto, btnUploadIdCard, btnUploadBarCertificate;
    private MaterialButton btnSaveDraft, btnCompleteProfile;
    private ProgressBar progressBar;

    // Section 1: Basic Profile Settings
    private TextInputEditText etFullName, etDateOfBirth, etShortBio;
    private TextInputLayout tilFullName, tilDateOfBirth, tilShortBio;
    private MaterialRadioButton rbMale, rbFemale, rbOther;
    private MaterialRadioButton rbBangla, rbEnglish, rbBoth;
    private RadioGroup rgGender, rgLanguage;

    // Section 2: Contact & Location Settings
    private TextInputEditText etMobileNumber, etEmailAddress, etOfficeAddress;
    private TextInputEditText etWorkingStartTime, etWorkingEndTime;
    private TextInputLayout tilMobileNumber, tilEmailAddress, tilOfficeAddress;
    private TextInputLayout tilWorkingStartTime, tilWorkingEndTime;
    private MaterialCheckBox cbSaturday, cbSunday, cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday;

    // Section 3: Professional Information
    private TextInputEditText etBarRegistrationNumber, etEnrollmentYear, etChamberName, etExperience;
    private TextInputEditText etLlbInstitution, etLlbYear, etOtherPracticeArea;
    private TextInputLayout tilBarRegistrationNumber, tilEnrollmentYear, tilChamberName, tilExperience;
    private TextInputLayout tilLlbInstitution, tilLlbYear, tilOtherPracticeArea;
    private MaterialCheckBox cbCriminalLaw, cbCivilLaw, cbFamilyLaw, cbCorporateLaw, cbPropertyLaw, cbOther;

    // Section 4: Consultation & Fees
    private TextInputEditText etFixedFee, etMinFee, etMaxFee;
    private TextInputLayout tilFixedFee, tilMinFee, tilMaxFee;
    private MaterialRadioButton rbFixedFee, rbRangedFee;
    private RadioGroup rgFeeType;
    private MaterialSwitch switchAvailability;
    private MaterialCheckBox cbInPerson, cbOnline, cbPhone, cbChat;
    private LinearLayout layoutFeeRange;

    // Section 5: Appointment Settings
    private TextInputEditText etSlotStartTime, etSlotEndTime, etMaxAppointments;
    private TextInputLayout tilSlotStartTime, tilSlotEndTime, tilMaxAppointments;
    private MaterialSwitch switchAppointmentBooking;
    private MaterialCheckBox cbEmailNotification, cbSmsNotification;

    // Section 6: Social & Website Links
    private TextInputEditText etFacebookLink, etLinkedInLink, etWebsiteLink, etVideoIntroLink;
    private TextInputLayout tilFacebookLink, tilLinkedInLink, tilWebsiteLink, tilVideoIntroLink;

    // Section 7: Profile Visibility Settings
    private MaterialSwitch switchPublicProfile;
    private MaterialCheckBox cbShowEmail, cbShowPhone, cbShowOfficeAddress, cbShowFees;

    // Section 8: Additional Information
    private TextInputEditText etNidPassport, etPresentAddress, etPermanentAddress;
    private TextInputEditText etLlmInstitution, etLlmYear, etSpecialTraining, etCustomIntro;
    private TextInputLayout tilNidPassport, tilPresentAddress, tilPermanentAddress;
    private TextInputLayout tilLlmInstitution, tilLlmYear, tilSpecialTraining, tilCustomIntro;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // Image and document handling
    private Uri selectedProfileImageUri;
    private Uri selectedIdCardUri;
    private Uri selectedBarCertificateUri;

    // Upload URLs
    private String profileImageUrl = "";
    private String idCardUrl = "";
    private String barCertificateUrl = "";

    // Activity result launchers
    private ActivityResultLauncher<Intent> profileImageLauncher;
    private ActivityResultLauncher<Intent> idCardLauncher;
    private ActivityResultLauncher<Intent> barCertificateLauncher;

    // Upload progress tracking
    private AlertDialog uploadProgressDialog;
    private AlertDialog saveProgressDialog;


    //for getting location

    // Add these constants
    private static final int LOCATION_PERMISSION_CODE = 200;

    // Add these UI components for location
    private AutoCompleteTextView actvDistrict, actvUpazila;
    private TextInputEditText etArea, etLatitude, etLongitude;
    private TextInputLayout tilDistrict, tilUpazila, tilArea, tilLatitude, tilLongitude;
    private MaterialButton btnGetLocation;
    private MaterialCheckBox cbDhaka, cbChittagong, cbSylhet, cbRajshahi, cbBarisal, cbKhulna, cbRangpur, cbMymensingh;

    // Add these variables
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lawyer_profile_setup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeFirebase();
        initializeViews();
        initializeLocationServices();
        initializeActivityResultLaunchers();
        setupClickListeners();
        checkStoragePermissions();

    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationDropdowns();
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
        // Profile photo and bottom buttons
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnUploadIdCard = findViewById(R.id.btnUploadIdCard);
        btnUploadBarCertificate = findViewById(R.id.btnUploadBarCertificate);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);
        progressBar = findViewById(R.id.progressBar);

        // Initialize all sections
        initializeBasicProfileSettings();
        initializeContactLocationSettings();
        initializeLocationSettings();
        // In onCreate() after existing initializations
        initializeLocationServices();
        initializeLocationSettings();  // Add to initializeViews()
        setupLocationClickListeners(); // Add to setupClickListeners()
        initializeProfessionalInformation();
        initializeConsultationFees();
        initializeAppointmentSettings();
        initializeSocialWebsiteLinks();
        initializeProfileVisibilitySettings();
        initializeAdditionalInformation();
    }


    // Add this method in initializeViews()
    private void initializeLocationSettings() {
        // District and Upazila dropdowns
        actvDistrict = findViewById(R.id.actvDistrict);
        actvUpazila = findViewById(R.id.actvUpazila);

        // Location text fields
        etArea = findViewById(R.id.etArea);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);

        // Layout wrappers
        tilDistrict = findViewById(R.id.tilDistrict);
        tilUpazila = findViewById(R.id.tilUpazila);
        tilArea = findViewById(R.id.tilArea);
        tilLatitude = findViewById(R.id.tilLatitude);
        tilLongitude = findViewById(R.id.tilLongitude);

        // Get location button
        btnGetLocation = findViewById(R.id.btnGetLocation);

        // Service area checkboxes
        cbDhaka = findViewById(R.id.cbDhaka);
        cbChittagong = findViewById(R.id.cbChittagong);
        cbSylhet = findViewById(R.id.cbSylhet);
        cbRajshahi = findViewById(R.id.cbRajshahi);
        cbBarisal = findViewById(R.id.cbBarisal);
        cbKhulna = findViewById(R.id.cbKhulna);
        cbRangpur = findViewById(R.id.cbRangpur);
        cbMymensingh = findViewById(R.id.cbMymensingh);
    }

    // Section 1: Basic Profile Settings
    private void initializeBasicProfileSettings() {
        etFullName = findViewById(R.id.etFullName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etShortBio = findViewById(R.id.etShortBio);

        tilFullName = findViewById(R.id.tilFullName);
        tilDateOfBirth = findViewById(R.id.tilDateOfBirth);
        tilShortBio = findViewById(R.id.tilShortBio);

        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);

        rbBangla = findViewById(R.id.rbBangla);
        rbEnglish = findViewById(R.id.rbEnglish);
        rbBoth = findViewById(R.id.rbBoth);

        rgGender = findViewById(R.id.rgGender);
        rgLanguage = findViewById(R.id.rgLanguage);
    }

    // Section 2: Contact & Location Settings
    private void initializeContactLocationSettings() {
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etEmailAddress = findViewById(R.id.etEmailAddress);
        etOfficeAddress = findViewById(R.id.etOfficeAddress);
        etWorkingStartTime = findViewById(R.id.etWorkingStartTime);
        etWorkingEndTime = findViewById(R.id.etWorkingEndTime);

        tilMobileNumber = findViewById(R.id.tilMobileNumber);
        tilEmailAddress = findViewById(R.id.tilEmailAddress);
        tilOfficeAddress = findViewById(R.id.tilOfficeAddress);
        tilWorkingStartTime = findViewById(R.id.tilWorkingStartTime);
        tilWorkingEndTime = findViewById(R.id.tilWorkingEndTime);

        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);
        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
    }

    // Section 3: Professional Information
    private void initializeProfessionalInformation() {
        etBarRegistrationNumber = findViewById(R.id.etBarRegistrationNumber);
        etEnrollmentYear = findViewById(R.id.etEnrollmentYear);
        etChamberName = findViewById(R.id.etChamberName);
        etExperience = findViewById(R.id.etExperience);
        etLlbInstitution = findViewById(R.id.etLlbInstitution);
        etLlbYear = findViewById(R.id.etLlbYear);
        etOtherPracticeArea = findViewById(R.id.etOtherPracticeArea);

        tilBarRegistrationNumber = findViewById(R.id.tilBarRegistrationNumber);
        tilEnrollmentYear = findViewById(R.id.tilEnrollmentYear);
        tilChamberName = findViewById(R.id.tilChamberName);
        tilExperience = findViewById(R.id.tilExperience);
        tilLlbInstitution = findViewById(R.id.tilLlbInstitution);
        tilLlbYear = findViewById(R.id.tilLlbYear);
        tilOtherPracticeArea = findViewById(R.id.tilOtherPracticeArea);

        cbCriminalLaw = findViewById(R.id.cbCriminalLaw);
        cbCivilLaw = findViewById(R.id.cbCivilLaw);
        cbFamilyLaw = findViewById(R.id.cbFamilyLaw);
        cbCorporateLaw = findViewById(R.id.cbCorporateLaw);
        cbPropertyLaw = findViewById(R.id.cbPropertyLaw);
        cbOther = findViewById(R.id.cbOther);
    }

    // Section 4: Consultation & Fees
    private void initializeConsultationFees() {
        etFixedFee = findViewById(R.id.etFixedFee);
        etMinFee = findViewById(R.id.etMinFee);
        etMaxFee = findViewById(R.id.etMaxFee);

        tilFixedFee = findViewById(R.id.tilFixedFee);
        tilMinFee = findViewById(R.id.tilMinFee);
        tilMaxFee = findViewById(R.id.tilMaxFee);

        rbFixedFee = findViewById(R.id.rbFixedFee);
        rbRangedFee = findViewById(R.id.rbRangedFee);
        rgFeeType = findViewById(R.id.rgFeeType);

        layoutFeeRange = findViewById(R.id.layoutFeeRange);
        switchAvailability = findViewById(R.id.switchAvailability);

        cbInPerson = findViewById(R.id.cbInPerson);
        cbOnline = findViewById(R.id.cbOnline);
        cbPhone = findViewById(R.id.cbPhone);
        cbChat = findViewById(R.id.cbChat);
    }

    // Section 5: Appointment Settings
    private void initializeAppointmentSettings() {
        etSlotStartTime = findViewById(R.id.etSlotStartTime);
        etSlotEndTime = findViewById(R.id.etSlotEndTime);
        etMaxAppointments = findViewById(R.id.etMaxAppointments);

        tilSlotStartTime = findViewById(R.id.tilSlotStartTime);
        tilSlotEndTime = findViewById(R.id.tilSlotEndTime);
        tilMaxAppointments = findViewById(R.id.tilMaxAppointments);

        switchAppointmentBooking = findViewById(R.id.switchAppointmentBooking);
        cbEmailNotification = findViewById(R.id.cbEmailNotification);
        cbSmsNotification = findViewById(R.id.cbSmsNotification);
    }

    // Section 6: Social & Website Links
    private void initializeSocialWebsiteLinks() {
        etFacebookLink = findViewById(R.id.etFacebookLink);
        etLinkedInLink = findViewById(R.id.etLinkedInLink);
        etWebsiteLink = findViewById(R.id.etWebsiteLink);
        etVideoIntroLink = findViewById(R.id.etVideoIntroLink);

        tilFacebookLink = findViewById(R.id.tilFacebookLink);
        tilLinkedInLink = findViewById(R.id.tilLinkedInLink);
        tilWebsiteLink = findViewById(R.id.tilWebsiteLink);
        tilVideoIntroLink = findViewById(R.id.tilVideoIntroLink);
    }

    // Section 7: Profile Visibility Settings
    private void initializeProfileVisibilitySettings() {
        switchPublicProfile = findViewById(R.id.switchPublicProfile);
        cbShowEmail = findViewById(R.id.cbShowEmail);
        cbShowPhone = findViewById(R.id.cbShowPhone);
        cbShowOfficeAddress = findViewById(R.id.cbShowOfficeAddress);
        cbShowFees = findViewById(R.id.cbShowFees);
    }

    // Section 8: Additional Information
    private void initializeAdditionalInformation() {
        etNidPassport = findViewById(R.id.etNidPassport);
        etPresentAddress = findViewById(R.id.etPresentAddress);
        etPermanentAddress = findViewById(R.id.etPermanentAddress);
        etLlmInstitution = findViewById(R.id.etLlmInstitution);
        etLlmYear = findViewById(R.id.etLlmYear);
        etSpecialTraining = findViewById(R.id.etSpecialTraining);
        etCustomIntro = findViewById(R.id.etCustomIntro);

        tilNidPassport = findViewById(R.id.tilNidPassport);
        tilPresentAddress = findViewById(R.id.tilPresentAddress);
        tilPermanentAddress = findViewById(R.id.tilPermanentAddress);
        tilLlmInstitution = findViewById(R.id.tilLlmInstitution);
        tilLlmYear = findViewById(R.id.tilLlmYear);
        tilSpecialTraining = findViewById(R.id.tilSpecialTraining);
        tilCustomIntro = findViewById(R.id.tilCustomIntro);
    }

    private void initializeActivityResultLaunchers() {
        profileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleImageSelection(result, "profile"));

        idCardLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleDocumentSelection(result, "idCard"));

        barCertificateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleDocumentSelection(result, "barCertificate"));
    }

    private void handleImageSelection(androidx.activity.result.ActivityResult result, String type) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedUri = result.getData().getData();
            if (selectedUri != null) {
                selectedProfileImageUri = selectedUri;
                Glide.with(this)
                        .load(selectedUri)
                        .into(ivProfilePhoto);
                Log.d(TAG, "Profile image selected: " + selectedUri.toString());
            }
        }
    }

    private void setupLocationDropdowns() {
        // Bangladesh Districts
        String[] districts = {
                "Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Barisal", "Khulna", "Rangpur", "Mymensingh",
                "Comilla", "Gazipur", "Narayanganj", "Tangail", "Manikganj", "Munshiganj", "Narsingdi",
                "Faridpur", "Madaripur", "Rajbari", "Gopalganj", "Shariatpur", "Kishoreganj", "Netrokona",
                "Sherpur", "Jamalpur", "Cox's Bazar", "Feni", "Lakshmipur", "Noakhali", "Chandpur",
                "Brahmanbaria", "Habiganj", "Moulvibazar", "Sunamganj", "Bogura", "Joypurhat", "Naogaon",
                "Natore", "Chapainawabganj", "Pabna", "Sirajganj", "Jessore", "Jhenaidah", "Kushtia",
                "Magura", "Meherpur", "Narail", "Chuadanga", "Satkhira", "Bagerhat", "Pirojpur",
                "Jhalokathi", "Patuakhali", "Barguna", "Bhola", "Dinajpur", "Gaibandha", "Kurigram",
                "Lalmonirhat", "Nilphamari", "Panchagarh", "Thakurgaon", "Bandarban", "Khagrachhari", "Rangamati"
        };

        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, districts);
        actvDistrict.setAdapter(districtAdapter);

        // District selection listener
        actvDistrict.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDistrict = districts[position];
            setupUpazilaDropdown(selectedDistrict);
        });
    }

    private void setupUpazilaDropdown(String district) {
        String[] upazilas = getUpazilasByDistrict(district);
        ArrayAdapter<String> upazilaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, upazilas);
        actvUpazila.setAdapter(upazilaAdapter);
    }

    private String[] getUpazilasByDistrict(String district) {
        switch (district) {
            case "Dhaka":
                return new String[]{"Dhamrai", "Dohar", "Keraniganj", "Nawabganj", "Savar"};
            case "Chittagong":
                return new String[]{"Anwara", "Banshkhali", "Boalkhali", "Chandanaish", "Fatikchhari",
                        "Hathazari", "Lohagara", "Mirsharai", "Patiya", "Rangunia", "Raozan", "Sandwip",
                        "Satkania", "Sitakunda"};
            case "Sylhet":
                return new String[]{"Balaganj", "Beanibazar", "Bishwanath", "Companigonj", "Fenchuganj",
                        "Golapganj", "Gowainghat", "Jaintiapur", "Kanaighat", "Osmani Nagar", "Zakiganj"};
            case "Rajshahi":
                return new String[]{"Bagha", "Bagmara", "Charghat", "Durgapur", "Godagari", "Mohanpur",
                        "Paba", "Puthia", "Tanore"};
            case "Barisal":
                return new String[]{"Agailjhara", "Babuganj", "Bakerganj", "Banaripara", "Barisal Sadar",
                        "Gournadi", "Hizla", "Mehendiganj", "Muladi", "Wazirpur"};
            case "Khulna":
                return new String[]{"Batiaghata", "Dacope", "Dumuria", "Dighalia", "Koyra", "Paikgachha",
                        "Phultala", "Rupsa", "Terokhada"};
            case "Rangpur":
                return new String[]{"Badarganj", "Gangachhara", "Kaunia", "Mithapukur", "Pirgachha",
                        "Pirganj", "Rangpur Sadar", "Taraganj"};
            case "Mymensingh":
                return new String[]{"Bhaluka", "Dhobaura", "Fulbaria", "Gaffargaon", "Gauripur",
                        "Haluaghat", "Ishwarganj", "Muktagachha", "Mymensingh Sadar", "Nandail",
                        "Phulpur", "Trishal"};
            case "Comilla":
                return new String[]{"Barura", "Brahmanpara", "Burichang", "Chandina", "Chauddagram",
                        "Comilla Sadar Dakshin", "Daudkandi", "Debidwar", "Homna", "Laksam", "Manoharganj",
                        "Meghna", "Muradnagar", "Nangalkot", "Titas"};
            case "Gazipur":
                return new String[]{"Gazipur Sadar", "Kaliakair", "Kaliganj", "Kapasia", "Sreepur"};
            case "Narayanganj":
                return new String[]{"Araihazar", "Bandar", "Narayanganj Sadar", "Rupganj", "Sonargaon"};
            case "Tangail":
                return new String[]{"Basail", "Bhuapur", "Delduar", "Dhanbari", "Ghatail", "Gopalpur",
                        "Kalihati", "Madhupur", "Mirzapur", "Nagarpur", "Sakhipur", "Tangail Sadar"};
            case "Manikganj":
                return new String[]{"Daulatpur", "Ghior", "Harirampur", "Manikganj Sadar", "Saturia",
                        "Shivalaya", "Singair"};
            case "Munshiganj":
                return new String[]{"Gazaria", "Lohajang", "Munshiganj Sadar", "Sirajdikhan", "Sreenagar",
                        "Tongibari"};
            case "Narsingdi":
                return new String[]{"Belabo", "Monohardi", "Narsingdi Sadar", "Palash", "Raipura",
                        "Shibpur"};
            case "Faridpur":
                return new String[]{"Alfadanga", "Bhanga", "Boalmari", "Charbhadrasan", "Faridpur Sadar",
                        "Madhukhali", "Nagarkanda", "Sadarpur", "Saltha"};
            case "Madaripur":
                return new String[]{"Kalkini", "Madaripur Sadar", "Rajoir", "Shibchar"};
            case "Rajbari":
                return new String[]{"Baliakandi", "Goalandaghat", "Pangsha", "Rajbari Sadar", "Kalukhali"};
            case "Gopalganj":
                return new String[]{"Gopalganj Sadar", "Kashiani", "Kotalipara", "Muksudpur", "Tungipara"};
            case "Shariatpur":
                return new String[]{"Bhedarganj", "Damudya", "Gosairhat", "Naria", "Shariatpur Sadar",
                        "Zajira"};
            case "Kishoreganj":
                return new String[]{"Austagram", "Bajitpur", "Bhairab", "Hossainpur", "Itna", "Karimganj",
                        "Katiadi", "Kishoreganj Sadar", "Kuliarchar", "Mithamain", "Nikli", "Pakundia",
                        "Tarail"};
            case "Netrokona":
                return new String[]{"Atpara", "Barhatta", "Durgapur", "Kalmakanda", "Kendua",
                        "Khaliajuri", "Madan", "Mohanganj", "Netrokona Sadar", "Purbadhala"};
            case "Sherpur":
                return new String[]{"Jhenaigati", "Nakla", "Nalitabari", "Sherpur Sadar", "Sreebardi"};
            case "Jamalpur":
                return new String[]{"Bakshiganj", "Dewanganj", "Islampur", "Jamalpur Sadar", "Madarganj",
                        "Melandaha", "Sarishabari"};
            case "Cox's Bazar":
                return new String[]{"Chakaria", "Cox's Bazar Sadar", "Kutubdia", "Maheshkhali", "Pekua",
                        "Ramu", "Teknaf", "Ukhia"};
            case "Feni":
                return new String[]{"Chhagalnaiya", "Daganbhuiyan", "Feni Sadar", "Fulgazi", "Parshuram",
                        "Sonagazi"};
            case "Lakshmipur":
                return new String[]{"Kamalnagar", "Lakshmipur Sadar", "Raipur", "Ramganj", "Ramgati"};
            case "Noakhali":
                return new String[]{"Begumganj", "Chatkhil", "Companiganj", "Hatiya", "Kabirhat",
                        "Noakhali Sadar", "Senbagh", "Sonaimuri", "Subarnachar"};
            case "Chandpur":
                return new String[]{"Chandpur Sadar", "Faridganj", "Haimchar", "Hajiganj", "Kachua",
                        "Matlab Dakshin", "Matlab Uttar", "Shahrasti"};
            case "Brahmanbaria":
                return new String[]{"Akhaura", "Ashuganj", "Bancharampur", "Brahmanbaria Sadar", "Kasba",
                        "Nabinagar", "Nasirnagar", "Sarail"};
            case "Habiganj":
                return new String[]{"Ajmiriganj", "Bahubal", "Baniyachong", "Chunarughat", "Habiganj Sadar",
                        "Lakhai", "Madhabpur", "Nabiganj"};
            case "Moulvibazar":
                return new String[]{"Barlekha", "Juri", "Kamalganj", "Kulaura", "Moulvibazar Sadar",
                        "Rajnagar", "Sreemangal"};
            case "Sunamganj":
                return new String[]{"Bishwamvarpur", "Chhatak", "Dakshin Sunamganj", "Derai", "Dharampasha",
                        "Dowarabazar", "Jagannathpur", "Jamalganj", "Sulla", "Sunamganj Sadar", "Tahirpur"};
            case "Bogura":
                return new String[]{"Adamdighi", "Bogura Sadar", "Dhunat", "Dhupchanchia", "Gabtali",
                        "Kahaloo", "Nandigram", "Sariakandi", "Shajahanpur", "Sherpur", "Shibganj", "Sonatola"};
            case "Joypurhat":
                return new String[]{"Akkelpur", "Joypurhat Sadar", "Kalai", "Khetlal", "Panchbibi"};
            case "Naogaon":
                return new String[]{"Atrai", "Badalgachhi", "Dhamoirhat Dhamoirhat", "Manda", "Mahadevpur",
                        "Naogaon Sadar", "Niamatpur", "Patnitala", "Porsha", "Raninagar", "Sapahar"};
            case "Natore":
                return new String[]{"Bagatipara", "Baraigram", "Gurudaspur", "Lalpur", "Natore Sadar",
                        "Singra"};
            case "Chapainawabganj":
                return new String[]{"Bholahat", "Chapainawabganj Sadar", "Gomastapur", "Nachole", "Shibganj"};
            case "Pabna":
                return new String[]{"Atgharia", "Bera", "Bhangura", "Chatmohar", "Faridpur", "Ishwardi",
                        "Pabna Sadar", "Santhia", "Sujanagar"};
            case "Sirajganj":
                return new String[]{"Belkuchi", "Chauhali", "Kamarkhanda", "Kazipur", "Raiganj",
                        "Shahjadpur", "Sirajganj Sadar", "Tarash", "Ullahpara"};
            case "Jessore":
                return new String[]{"Abhaynagar", "Bagherpara", "Chaugachha", "Jhikargachha", "Keshabpur",
                        "Jessore Sadar", "Manirampur", "Sharsha"};
            case "Jhenaidah":
                return new String[]{"Harinakunda", "Jhenaidah Sadar", "Kaliganj", "Kotchandpur", "Maheshpur",
                        "Shailkupa"};
            case "Kushtia":
                return new String[]{"Bheramara", "Daulatpur", "Khoksa", "Kumarkhali", "Kushtia Sadar",
                        "Mirpur"};
            case "Magura":
                return new String[]{"Magura Sadar", "Mohammadpur", "Shalikha", "Sreepur"};
            case "Meherpur":
                return new String[]{"Gangni", "Meherpur Sadar", "Mujibnagar"};
            case "Narail":
                return new String[]{"Kalia", "Lohagara", "Narail Sadar"};
            case "Chuadanga":
                return new String[]{"Alamdanga", "Chuadanga Sadar", "Damurhuda", "Jibannagar"};
            case "Satkhira":
                return new String[]{"Assasuni", "Debhata", "Kalaroa", "Kaliganj", "Satkhira Sadar",
                        "Shyamnagar", "Tala"};
            case "Bagerhat":
                return new String[]{"Bagerhat Sadar", "Chitalmari", "Fakirhat", "Kachua", "Mollahat",
                        "Mongla", "Morrelganj", "Rampal", "Sarankhola"};
            case "Pirojpur":
                return new String[]{"Bhandaria", "Kawkhali", "Mathbaria", "Nazirpur", "Nesarabad",
                        "Pirojpur Sadar", "Zianagar"};
            case "Jhalokathi":
                return new String[]{"Jhalokathi Sadar", "Kathalia", "Nalchity", "Rajapur"};
            case "Patuakhali":
                return new String[]{"Bauphal", "Dashmina", "Dumki", "Galachipa", "Kalapara", "Mirzaganj",
                        "Patuakhali Sadar", "Rangabali"};
            case "Barguna":
                return new String[]{"Amtali", "Bamna", "Barguna Sadar", "Betagi", "Patharghata", "Taltali"};
            case "Bhola":
                return new String[]{"Bhola Sadar", "Burhanuddin", "Char Fasson", "Daulatkhan", "Lalmohan",
                        "Manpura", "Tazumuddin"};
            case "Dinajpur":
                return new String[]{"Birampur", "Birganj", "Biral", "Bochaganj", "Chirirbandar",
                        "Dinajpur Sadar", "Fulbari", "Ghoraghat", "Hakimpur", "Kaharole", "Khansama",
                        "Nawabganj", "Parbatipur"};
            case "Gaibandha":
                return new String[]{"Fulchhari", "Gaibandha Sadar", "Gobindaganj", "Palashbari", "Sadullapur",
                        "Saghata", "Sundarganj"};
            case "Kurigram":
                return new String[]{"Bhurungamari", "Char Rajibpur", "Chilmari", "Kurigram Sadar", "Nageshwari",
                        "Phulbari", "Rajarhat", "Raumari", "Ulipur"};
            case "Lalmonirhat":
                return new String[]{"Aditmari", "Hatibandha", "Kaliganj", "Lalmonirhat Sadar", "Patgram"};
            case "Nilphamari":
                return new String[]{"Dimla", "Domar", "Jaldhaka", "Kishoreganj", "Nilphamari Sadar", "Saidpur"};
            case "Panchagarh":
                return new String[]{"Atwari", "Boda", "Debiganj", "Panchagarh Sadar", "Tetulia"};
            case "Thakurgaon":
                return new String[]{"Baliadangi", "Haripur", "Pirganj", "Ranisankail", "Thakurgaon Sadar"};
            case "Bandarban":
                return new String[]{"Alikadam", "Bandarban Sadar", "Lama", "Naikhongchhari", "Rowangchhari",
                        "Ruma", "Thanchi"};
            case "Khagrachhari":
                return new String[]{"Dighinala", "Khagrachhari Sadar", "Lakshmichhari", "Mahalchhari",
                        "Manikchhari", "Matiranga", "Panchhari", "Ramgarh"};
            case "Rangamati":
                return new String[]{"Baghaichhari", "Barkal", "Belaichhari", "Juraichhari", "Kaptai",
                        "Kawkhali", "Langadu", "Naniarchar", "Rajasthali", "Rangamati Sadar"};
            default:
                return new String[]{"Select District First"};
        }
    }

    private void handleDocumentSelection(androidx.activity.result.ActivityResult result, String type) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedUri = result.getData().getData();
            if (selectedUri != null) {
                Log.d(TAG, "Document selected for " + type + ": " + selectedUri.toString());
                switch (type) {
                    case "idCard":
                        selectedIdCardUri = selectedUri;
                        updateButtonState(btnUploadIdCard, "ID Card Selected ✓");
                        Log.d(TAG, "ID Card URI set: " + selectedIdCardUri.toString());
                        break;
                    case "barCertificate":
                        selectedBarCertificateUri = selectedUri;
                        updateButtonState(btnUploadBarCertificate, "Bar Certificate Selected ✓");
                        Log.d(TAG, "Bar Certificate URI set: " + selectedBarCertificateUri.toString());
                        break;
                }
            }
        }
    }

    // Add in setupClickListeners()
    private void setupLocationClickListeners() {
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
    }

    private void getCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        if (!isLocationEnabled()) {
            showToast("Please enable GPS/Location services");
            return;
        }

        btnGetLocation.setEnabled(false);
        btnGetLocation.setText("Getting Location...");

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            btnGetLocation.setEnabled(true);
                            btnGetLocation.setText("Get My Location");

                            if (location != null) {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();

                                // Display coordinates
                                etLatitude.setText(String.valueOf(currentLatitude));
                                etLongitude.setText(String.valueOf(currentLongitude));

                                // Reverse geocoding to get address
                                getAddressFromCoordinates(currentLatitude, currentLongitude);

                                showToast("Location captured successfully!");
                                Log.d(TAG, "Location: " + currentLatitude + ", " + currentLongitude);
                            } else {
                                showToast("Unable to get current location. Try again.");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnGetLocation.setEnabled(true);
                        btnGetLocation.setText("Get My Location");
                        showToast("Failed to get location: " + e.getMessage());
                        Log.e(TAG, "Location error", e);
                    });
        } catch (SecurityException e) {
            btnGetLocation.setEnabled(true);
            btnGetLocation.setText("Get My Location");
            showToast("Location permission denied");
        }
    }

    private void getAddressFromCoordinates(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Extract area/locality
                String area = address.getSubLocality() != null ? address.getSubLocality() :
                        address.getLocality() != null ? address.getLocality() : "";

                if (!TextUtils.isEmpty(area)) {
                    etArea.setText(area);
                }

                // Try to match with district
                String addressLine = address.getAddressLine(0);
                if (addressLine != null) {
                    matchDistrictFromAddress(addressLine);
                }

                Log.d(TAG, "Address found: " + addressLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            showToast("Unable to get address from coordinates");
        }
    }

    private void matchDistrictFromAddress(String fullAddress) {
        // Simple matching logic for major districts
        String[] districts = {"Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Barisal",
                "Khulna", "Rangpur", "Mymensingh"};

        for (String district : districts) {
            if (fullAddress.toLowerCase().contains(district.toLowerCase())) {
                actvDistrict.setText(district, false);
                setupUpazilaDropdown(district);
                break;
            }
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_CODE);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager != null && (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        );
    }





    private boolean hasServiceAreaSelected() {
        return cbDhaka.isChecked() || cbChittagong.isChecked() || cbSylhet.isChecked() ||
                cbRajshahi.isChecked() || cbBarisal.isChecked() || cbKhulna.isChecked() ||
                cbRangpur.isChecked() || cbMymensingh.isChecked();
    }

    private List<String> getSelectedServiceAreas() {
        List<String> serviceAreas = new ArrayList<>();

        if (cbDhaka.isChecked()) serviceAreas.add("Dhaka");
        if (cbChittagong.isChecked()) serviceAreas.add("Chittagong");
        if (cbSylhet.isChecked()) serviceAreas.add("Sylhet");
        if (cbRajshahi.isChecked()) serviceAreas.add("Rajshahi");
        if (cbBarisal.isChecked()) serviceAreas.add("Barisal");
        if (cbKhulna.isChecked()) serviceAreas.add("Khulna");
        if (cbRangpur.isChecked()) serviceAreas.add("Rangpur");
        if (cbMymensingh.isChecked()) serviceAreas.add("Mymensingh");

        return serviceAreas;
    }

    private void updateButtonState(MaterialButton button, String text) {
        button.setText(text);
        button.setIconResource(R.drawable.ic_check);
    }

    private void setupClickListeners() {
        // Photo and document uploads
        btnSelectPhoto.setOnClickListener(v -> selectImage());
        btnUploadIdCard.setOnClickListener(v -> selectDocument(idCardLauncher));
        btnUploadBarCertificate.setOnClickListener(v -> selectDocument(barCertificateLauncher));

        // Date and time pickers
        etDateOfBirth.setOnClickListener(v -> showDatePicker(etDateOfBirth));
        etEnrollmentYear.setOnClickListener(v -> showEnrollmentYearPicker());
        etLlbYear.setOnClickListener(v -> showGraduationYearPicker(etLlbYear));
        etLlmYear.setOnClickListener(v -> showGraduationYearPicker(etLlmYear));

        etWorkingStartTime.setOnClickListener(v -> showTimePicker(etWorkingStartTime));
        etWorkingEndTime.setOnClickListener(v -> showTimePicker(etWorkingEndTime));
        etSlotStartTime.setOnClickListener(v -> showTimePicker(etSlotStartTime));
        etSlotEndTime.setOnClickListener(v -> showTimePicker(etSlotEndTime));

        // Fee type selection
        rgFeeType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRangedFee) {
                layoutFeeRange.setVisibility(View.VISIBLE);
                tilFixedFee.setVisibility(View.GONE);
            } else {
                layoutFeeRange.setVisibility(View.GONE);
                tilFixedFee.setVisibility(View.VISIBLE);
            }
        });

        // Other practice area visibility
        cbOther.setOnCheckedChangeListener((buttonView, isChecked) ->
                tilOtherPracticeArea.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Bottom buttons
        btnSaveDraft.setOnClickListener(v -> saveDraft());
        btnCompleteProfile.setOnClickListener(v -> completeProfile());
    }

    private void showTimePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Parse current time if exists
        parseCurrentTime(editText.getText().toString().trim(), calendar);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    editText.setText(time);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

        timePickerDialog.show();
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

    private void checkStoragePermissions() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_CODE);
        }
    }



    private void selectImage() {
        if (hasStoragePermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            profileImageLauncher.launch(intent);
        } else {
            checkStoragePermissions();
        }
    }

    private void selectDocument(ActivityResultLauncher<Intent> launcher) {
        if (hasStoragePermission()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {"application/pdf", "image/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            launcher.launch(intent);
        } else {
            checkStoragePermissions();
        }
    }

    private boolean hasStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        parseCurrentDate(editText.getText().toString().trim(), calendar);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    editText.setText(date);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        if (editText == etDateOfBirth) {
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -100);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        }

        datePickerDialog.show();
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

    private void showEnrollmentYearPicker() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int defaultYear = parseYear(etEnrollmentYear.getText().toString().trim(), currentYear);

        YearPickerDialog.showEnrollmentYearPicker(this, defaultYear, year ->
                etEnrollmentYear.setText(String.valueOf(year)));
    }

    private void showGraduationYearPicker(TextInputEditText editText) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int defaultYear = parseYear(editText.getText().toString().trim(), currentYear);

        YearPickerDialog.showGraduationYearPicker(this, defaultYear, year ->
                editText.setText(String.valueOf(year)));
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

    private boolean isValidMobileNumber(String mobile) {
        if (mobile.startsWith("+880")) {
            return mobile.length() == 14 && mobile.substring(4).matches("1[3-9]\\d{8}");
        } else if (mobile.startsWith("01")) {
            return mobile.length() == 11 && mobile.matches("01[3-9]\\d{8}");
        }
        return false;
    }

    private boolean validateForm(boolean isComplete) {
        boolean isValid = true;
        clearErrors();

        // Basic Profile Settings validation
        isValid &= validateTextField(etFullName, tilFullName, "Full name is required");
        isValid &= validateTextField(etDateOfBirth, tilDateOfBirth, "Date of birth is required");
        isValid &= validateTextField(etShortBio, tilShortBio, "Short bio is required");
        isValid &= validateRadioGroupSelection("gender");
        isValid &= validateRadioGroupSelection("language");

        // Contact & Location Settings validation
        if (TextUtils.isEmpty(etMobileNumber.getText().toString().trim())) {
            tilMobileNumber.setError("Mobile number is required");
            isValid = false;
        } else {
            String mobile = etMobileNumber.getText().toString().trim();
            if (!isValidMobileNumber(mobile)) {
                tilMobileNumber.setError("Please enter a valid mobile number");
                isValid = false;
            }
        }

        if (TextUtils.isEmpty(actvDistrict.getText().toString().trim())) {
            tilDistrict.setError("Please select your district");
            isValid = false;
        }

        if (!hasServiceAreaSelected()) {
            showToast("Please select at least one service area");
            isValid = false;
        }

        isValid &= validateTextField(etOfficeAddress, tilOfficeAddress, "Office address is required");
        isValid &= validateWorkingDays();

        // Professional Information validation
        isValid &= validateTextField(etBarRegistrationNumber, tilBarRegistrationNumber, "Bar registration number is required");
        isValid &= validateTextField(etEnrollmentYear, tilEnrollmentYear, "Enrollment year is required");
        isValid &= validateTextField(etChamberName, tilChamberName, "Chamber name is required");
        isValid &= validateTextField(etExperience, tilExperience, "Experience is required");
        isValid &= validateTextField(etLlbInstitution, tilLlbInstitution, "LLB institution is required");
        isValid &= validateTextField(etLlbYear, tilLlbYear, "LLB graduation year is required");
        isValid &= validatePracticeAreas();

        // Consultation & Fees validation
        isValid &= validateConsultationFees();
        isValid &= validateConsultationTypes();

        // Additional Information validation
        isValid &= validateTextField(etNidPassport, tilNidPassport, "NID/Passport number is required");
        isValid &= validateTextField(etPresentAddress, tilPresentAddress, "Present address is required");
        isValid &= validateTextField(etPermanentAddress, tilPermanentAddress, "Permanent address is required");

        // Complete profile specific validations
        if (isComplete) {
            isValid &= validateRequiredFiles();
        }

        return isValid;
    }

    private boolean validateTextField(TextInputEditText editText, TextInputLayout layout, String errorMessage) {
        if (TextUtils.isEmpty(editText.getText().toString().trim())) {
            layout.setError(errorMessage);
            return false;
        }
        return true;
    }

    private boolean validateRadioGroupSelection(String type) {
        boolean isSelected = false;
        String message = "";

        switch (type) {
            case "gender":
                isSelected = rbMale.isChecked() || rbFemale.isChecked() || rbOther.isChecked();
                message = "Please select gender";
                break;
            case "language":
                isSelected = rbBangla.isChecked() || rbEnglish.isChecked() || rbBoth.isChecked();
                message = "Please select preferred language";
                break;
        }

        if (!isSelected) {
            showToast(message);
            return false;
        }
        return true;
    }

    private boolean validateWorkingDays() {
        boolean hasWorkingDay = cbSaturday.isChecked() || cbSunday.isChecked() || cbMonday.isChecked() ||
                cbTuesday.isChecked() || cbWednesday.isChecked() || cbThursday.isChecked() || cbFriday.isChecked();

        if (!hasWorkingDay) {
            showToast("Please select at least one working day");
            return false;
        }
        return true;
    }

    private boolean validatePracticeAreas() {
        boolean hasSelection = cbCriminalLaw.isChecked() || cbCivilLaw.isChecked() ||
                cbFamilyLaw.isChecked() || cbCorporateLaw.isChecked() ||
                cbPropertyLaw.isChecked() || cbOther.isChecked();

        if (!hasSelection) {
            showToast("Please select at least one practice area");
            return false;
        }

        if (cbOther.isChecked() && TextUtils.isEmpty(etOtherPracticeArea.getText().toString().trim())) {
            tilOtherPracticeArea.setError("Please specify other practice area");
            return false;
        }

        return true;
    }

    private boolean validateConsultationFees() {
        if (rbFixedFee.isChecked()) {
            if (TextUtils.isEmpty(etFixedFee.getText().toString().trim())) {
                tilFixedFee.setError("Please enter consultation fee");
                return false;
            }
        } else if (rbRangedFee.isChecked()) {
            if (TextUtils.isEmpty(etMinFee.getText().toString().trim())) {
                tilMinFee.setError("Please enter minimum fee");
                return false;
            }
            if (TextUtils.isEmpty(etMaxFee.getText().toString().trim())) {
                tilMaxFee.setError("Please enter maximum fee");
                return false;
            }
        } else {
            showToast("Please select fee type");
            return false;
        }
        return true;
    }

    private boolean validateConsultationTypes() {
        boolean hasConsultationType = cbInPerson.isChecked() || cbOnline.isChecked() ||
                cbPhone.isChecked() || cbChat.isChecked();

        if (!hasConsultationType) {
            showToast("Please select at least one consultation type");
            return false;
        }
        return true;
    }

    private boolean validateRequiredFiles() {
        if (selectedProfileImageUri == null) {
            showToast("Please select a profile photo");
            return false;
        }
        if (selectedIdCardUri == null) {
            showToast("Please upload ID card");
            return false;
        }
        if (selectedBarCertificateUri == null) {
            showToast("Please upload bar certificate");
            return false;
        }
        return true;
    }

    private void clearErrors() {
        TextInputLayout[] layouts = {
                tilFullName, tilDateOfBirth, tilShortBio, tilMobileNumber, tilEmailAddress, tilOfficeAddress,
                tilWorkingStartTime, tilWorkingEndTime, tilBarRegistrationNumber, tilEnrollmentYear,
                tilChamberName, tilExperience, tilLlbInstitution, tilLlbYear, tilOtherPracticeArea,
                tilFixedFee, tilMinFee, tilMaxFee, tilSlotStartTime, tilSlotEndTime, tilMaxAppointments,
                tilFacebookLink, tilLinkedInLink, tilWebsiteLink, tilVideoIntroLink, tilNidPassport,
                tilPresentAddress, tilPermanentAddress, tilLlmInstitution, tilLlmYear, tilSpecialTraining,
                tilCustomIntro
        };

        for (TextInputLayout layout : layouts) {
            layout.setError(null);
        }
    }

    private void saveDraft() {
        if (validateForm(false)) {
            uploadFiles(false);
        }
    }

    private void completeProfile() {
        if (validateForm(true)) {
            uploadFiles(true);
        }
    }

    private void uploadFiles(boolean isComplete) {
        setButtonsEnabled(false);

        int totalUploads = countFilesToUpload();
        Log.d(TAG, "Total files to upload: " + totalUploads);

        if (totalUploads == 0) {
            Log.d(TAG, "No files to upload, saving to Firestore directly");
            saveToFirestore(isComplete);
            return;
        }

        if (!CloudinaryConfig.canUpload()) {
            Log.e(TAG, "Cloudinary not ready for upload");
            showToast("File upload service not available");
            setButtonsEnabled(true);
            return;
        }

        uploadProgressDialog = showProgressDialog("Uploading files...");
        progressBar.setProgress(0);

        UploadTracker tracker = new UploadTracker(totalUploads, isComplete);

        if (selectedProfileImageUri != null) {
            Log.d(TAG, "Starting profile image upload: " + selectedProfileImageUri.toString());
            uploadToCloudinary(selectedProfileImageUri, "profile_image", tracker, "profile");
        }
        if (selectedIdCardUri != null) {
            Log.d(TAG, "Starting ID card upload: " + selectedIdCardUri.toString());
            uploadToCloudinary(selectedIdCardUri, "id_card", tracker, "idCard");
        }
        if (selectedBarCertificateUri != null) {
            Log.d(TAG, "Starting bar certificate upload: " + selectedBarCertificateUri.toString());
            uploadToCloudinary(selectedBarCertificateUri, "bar_certificate", tracker, "barCertificate");
        }
    }

    private int countFilesToUpload() {
        int count = 0;
        if (selectedProfileImageUri != null) count++;
        if (selectedIdCardUri != null) count++;
        if (selectedBarCertificateUri != null) count++;
        return count;
    }

    private void uploadToCloudinary(Uri fileUri, String fileType, UploadTracker tracker, String urlType) {
        Log.d(TAG, "uploadToCloudinary called for: " + fileType + " with URI: " + fileUri.toString());

        String publicId = CloudinaryConfig.generateProfileDocumentPublicId(userId, fileType);
        Log.d(TAG, "Generated public ID: " + publicId);

        try {
            MediaManager.get().upload(fileUri)
                    .option("folder", CloudinaryConfig.getLawyerProfileFolder(userId))
                    .option("public_id", publicId)
                    .option("resource_type", "auto")
                    .option("use_filename", true)
                    .option("unique_filename", false)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, fileType + " upload started with request ID: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((bytes * 100) / totalBytes);
                            Log.d(TAG, fileType + " upload progress: " + progress + "%");
                            updateUploadProgress(tracker.completedUploads, tracker.totalUploads, progress);
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            Log.d(TAG, fileType + " upload successful. URL: " + url);
                            Log.d(TAG, "Full result data: " + resultData.toString());

                            setUploadUrl(urlType, url);
                            tracker.onUploadComplete();
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, fileType + " upload failed. Request ID: " + requestId);
                            Log.e(TAG, "Error code: " + error.getCode());
                            Log.e(TAG, "Error description: " + error.getDescription());

                            runOnUiThread(() -> {
                                showToast(fileType + " upload failed: " + error.getDescription());
                            });

                            tracker.onUploadComplete();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, fileType + " upload rescheduled. Request ID: " + requestId +
                                    ", Error: " + error.getDescription());
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            Log.e(TAG, "Exception during " + fileType + " upload", e);
            runOnUiThread(() -> {
                showToast(fileType + " upload failed: " + e.getMessage());
            });
            tracker.onUploadComplete();
        }
    }

    private void setUploadUrl(String type, String url) {
        Log.d(TAG, "Setting upload URL for " + type + ": " + url);
        switch (type) {
            case "profile":
                profileImageUrl = url;
                Log.d(TAG, "Profile image URL set: " + profileImageUrl);
                break;
            case "idCard":
                idCardUrl = url;
                Log.d(TAG, "ID card URL set: " + idCardUrl);
                break;
            case "barCertificate":
                barCertificateUrl = url;
                Log.d(TAG, "Bar certificate URL set: " + barCertificateUrl);
                break;
        }
    }

    private void updateUploadProgress(int completed, int total, int currentProgress) {
        int overallProgress = (completed * 100 / total) + (currentProgress / total);
        runOnUiThread(() -> progressBar.setProgress(overallProgress));
    }

    private class UploadTracker {
        int totalUploads;
        int completedUploads = 0;
        boolean isComplete;

        UploadTracker(int totalUploads, boolean isComplete) {
            this.totalUploads = totalUploads;
            this.isComplete = isComplete;
            Log.d(TAG, "UploadTracker created. Total uploads: " + totalUploads + ", isComplete: " + isComplete);
        }

        synchronized void onUploadComplete() {
            completedUploads++;
            Log.d(TAG, "Upload completed. Progress: " + completedUploads + "/" + totalUploads);
            updateUploadProgress(completedUploads, totalUploads, 0);

            if (completedUploads >= totalUploads) {
                Log.d(TAG, "All uploads completed. Saving to Firestore...");
                runOnUiThread(() -> {
                    dismissProgressDialog(uploadProgressDialog);

                    Log.d(TAG, "Final URLs - Profile: " + profileImageUrl +
                            ", ID Card: " + idCardUrl +
                            ", Bar Certificate: " + barCertificateUrl);

                    saveToFirestore(isComplete);
                });
            }
        }
    }

    private void saveToFirestore(boolean isComplete) {
        saveProgressDialog = showProgressDialog("Saving profile data...");

        Log.d(TAG, "About to save to Firestore. URLs - Profile: " + profileImageUrl +
                ", ID Card: " + idCardUrl +
                ", Bar Certificate: " + barCertificateUrl);

        Map<String, Map<String, Object>> sectionsData = buildAllSectionsData(isComplete);

        List<Task<Void>> saveTasks = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : sectionsData.entrySet()) {
            String sectionName = entry.getKey();
            Map<String, Object> sectionData = entry.getValue();

            Task<Void> saveTask = db.collection("Users")
                    .document("Lawyers")
                    .collection("Lawyers")
                    .document(userId)
                    .collection("ProfileData")
                    .document(sectionName)
                    .set(sectionData);

            saveTasks.add(saveTask);
        }

        Tasks.whenAll(saveTasks)
                .addOnCompleteListener(task -> {
                    dismissProgressDialog(saveProgressDialog);
                    setButtonsEnabled(true);

                    if (task.isSuccessful()) {
                        handleSaveSuccess(isComplete);
                    } else {
                        handleSaveError(task.getException());
                    }
                });
    }

    private Map<String, Map<String, Object>> buildAllSectionsData(boolean isComplete) {
        Map<String, Map<String, Object>> sectionsData = new HashMap<>();

        // Section 1: Basic Profile Settings
        Map<String, Object> basicProfile = new HashMap<>();
        basicProfile.put("fullName", getText(etFullName));
        basicProfile.put("dateOfBirth", getText(etDateOfBirth));
        basicProfile.put("gender", getSelectedGender());
        basicProfile.put("preferredLanguage", getSelectedLanguage());
        basicProfile.put("shortBio", getText(etShortBio));
        basicProfile.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("BasicProfileSettings", basicProfile);

        // Section 2: Contact & Location Settings
        Map<String, Object> contactLocation = new HashMap<>();
        contactLocation.put("mobileNumber", getText(etMobileNumber));
        contactLocation.put("emailAddress", getText(etEmailAddress));
        contactLocation.put("officeAddress", getText(etOfficeAddress));
        contactLocation.put("workingDays", getSelectedWorkingDays());
        contactLocation.put("workingStartTime", getText(etWorkingStartTime));
        contactLocation.put("workingEndTime", getText(etWorkingEndTime));
        contactLocation.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("ContactLocationSettings", contactLocation);

        // Section 3: Professional Information
        Map<String, Object> professionalInfo = new HashMap<>();
        professionalInfo.put("practiceAreas", getSelectedPracticeAreas());
        professionalInfo.put("experience", getText(etExperience));
        professionalInfo.put("chamberName", getText(etChamberName));
        professionalInfo.put("barRegistrationNumber", getText(etBarRegistrationNumber));
        professionalInfo.put("enrollmentYear", getText(etEnrollmentYear));
        professionalInfo.put("llbInstitution", getText(etLlbInstitution));
        professionalInfo.put("llbYear", getText(etLlbYear));
        professionalInfo.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("ProfessionalInformation", professionalInfo);

        // Section 4: Consultation & Fees
        Map<String, Object> consultationFees = new HashMap<>();
        consultationFees.put("feeType", getSelectedFeeType());
        consultationFees.put("fixedFee", getText(etFixedFee));
        consultationFees.put("minFee", getText(etMinFee));
        consultationFees.put("maxFee", getText(etMaxFee));
        consultationFees.put("isAvailable", switchAvailability.isChecked());
        consultationFees.put("consultationTypes", getSelectedConsultationTypes());
        consultationFees.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("ConsultationFees", consultationFees);

        // Section 5: Appointment Settings
        Map<String, Object> appointmentSettings = new HashMap<>();
        appointmentSettings.put("appointmentBookingEnabled", switchAppointmentBooking.isChecked());
        appointmentSettings.put("slotStartTime", getText(etSlotStartTime));
        appointmentSettings.put("slotEndTime", getText(etSlotEndTime));
        appointmentSettings.put("maxAppointmentsPerDay", getText(etMaxAppointments));
        appointmentSettings.put("emailNotificationEnabled", cbEmailNotification.isChecked());
        appointmentSettings.put("smsNotificationEnabled", cbSmsNotification.isChecked());
        appointmentSettings.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("AppointmentSettings", appointmentSettings);

        // Section 6: Social & Website Links
        Map<String, Object> socialWebsiteLinks = new HashMap<>();
        socialWebsiteLinks.put("facebookLink", getText(etFacebookLink));
        socialWebsiteLinks.put("linkedInLink", getText(etLinkedInLink));
        socialWebsiteLinks.put("websiteLink", getText(etWebsiteLink));
        socialWebsiteLinks.put("videoIntroLink", getText(etVideoIntroLink));
        socialWebsiteLinks.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("SocialWebsiteLinks", socialWebsiteLinks);

        // Section 7: Profile Visibility Settings
        Map<String, Object> visibilitySettings = new HashMap<>();
        visibilitySettings.put("publicProfile", switchPublicProfile.isChecked());
        visibilitySettings.put("showEmail", cbShowEmail.isChecked());
        visibilitySettings.put("showPhone", cbShowPhone.isChecked());
        visibilitySettings.put("showOfficeAddress", cbShowOfficeAddress.isChecked());
        visibilitySettings.put("showFees", cbShowFees.isChecked());
        visibilitySettings.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("ProfileVisibilitySettings", visibilitySettings);

        // Section 8: Additional Information
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("nidPassport", getText(etNidPassport));
        additionalInfo.put("presentAddress", getText(etPresentAddress));
        additionalInfo.put("permanentAddress", getText(etPermanentAddress));
        additionalInfo.put("llmInstitution", getText(etLlmInstitution));
        additionalInfo.put("llmYear", getText(etLlmYear));
        additionalInfo.put("specialTraining", getText(etSpecialTraining));
        additionalInfo.put("customIntro", getText(etCustomIntro));
        additionalInfo.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("AdditionalInformation", additionalInfo);

        // Documents Section
        Log.d(TAG, "Building Documents section with URLs - Profile: " + profileImageUrl +
                ", ID Card: " + idCardUrl +
                ", Bar Certificate: " + barCertificateUrl);

        Map<String, Object> documentsInfo = new HashMap<>();
        documentsInfo.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        documentsInfo.put("idCardUrl", idCardUrl != null ? idCardUrl : "");
        documentsInfo.put("barCertificateUrl", barCertificateUrl != null ? barCertificateUrl : "");
        documentsInfo.put("updatedAt", System.currentTimeMillis());
        sectionsData.put("Documents", documentsInfo);

        // Profile Status Section
        Map<String, Object> profileStatus = new HashMap<>();
        profileStatus.put("profileStatus", isComplete ? "pending_approval" : "draft");
        profileStatus.put("isVerified", false);
        profileStatus.put("isActive", false);
        profileStatus.put("userType", "lawyer");
        profileStatus.put("rating", 0.0); // Initial rating field added
        profileStatus.put("totalReviews", 0);
        profileStatus.put("createdAt", System.currentTimeMillis());
        profileStatus.put("updatedAt", System.currentTimeMillis());
        profileStatus.put("completedSections", getCompletedSections());
        profileStatus.put("totalSections", 8); // Updated to 8 sections
        sectionsData.put("ProfileStatus", profileStatus);

        // Location Information Section
        Map<String, Object> locationInfo = new HashMap<>();
        locationInfo.put("district", actvDistrict.getText().toString().trim());
        locationInfo.put("upazila", actvUpazila.getText().toString().trim());
        locationInfo.put("area", getText(etArea));
        locationInfo.put("latitude", currentLatitude);
        locationInfo.put("longitude", currentLongitude);
        locationInfo.put("serviceAreas", getSelectedServiceAreas());
        locationInfo.put("isLocationVerified", currentLatitude != 0.0 && currentLongitude != 0.0);
        locationInfo.put("updatedAt", System.currentTimeMillis());

        sectionsData.put("LocationInformation", locationInfo);

        return sectionsData;
    }



    private List<String> getCompletedSections() {
        List<String> completedSections = new ArrayList<>();

        // Check Basic Profile Settings
        if (!TextUtils.isEmpty(getText(etFullName)) &&
                !TextUtils.isEmpty(getText(etDateOfBirth)) &&
                !TextUtils.isEmpty(getText(etShortBio)) &&
                (rbMale.isChecked() || rbFemale.isChecked() || rbOther.isChecked()) &&
                (rbBangla.isChecked() || rbEnglish.isChecked() || rbBoth.isChecked())) {
            completedSections.add("BasicProfileSettings");
        }

        // Check Contact & Location Settings
        if (!TextUtils.isEmpty(getText(etMobileNumber)) &&
                !TextUtils.isEmpty(getText(etOfficeAddress)) &&
                hasWorkingDaySelected()) {
            completedSections.add("ContactLocationSettings");
        }

        // Check Professional Information
        if (!TextUtils.isEmpty(getText(etBarRegistrationNumber)) &&
                !TextUtils.isEmpty(getText(etEnrollmentYear)) &&
                !TextUtils.isEmpty(getText(etChamberName)) &&
                !TextUtils.isEmpty(getText(etExperience)) &&
                !TextUtils.isEmpty(getText(etLlbInstitution)) &&
                !TextUtils.isEmpty(getText(etLlbYear)) &&
                !getSelectedPracticeAreas().isEmpty()) {
            completedSections.add("ProfessionalInformation");
        }

        // Check Consultation & Fees
        if (hasValidFeeSettings() && hasConsultationTypeSelected()) {
            completedSections.add("ConsultationFees");
        }

        // Check Appointment Settings
        if (switchAppointmentBooking.isChecked()) {
            completedSections.add("AppointmentSettings");
        }

        // Check Social & Website Links
        if (!TextUtils.isEmpty(getText(etFacebookLink)) ||
                !TextUtils.isEmpty(getText(etLinkedInLink)) ||
                !TextUtils.isEmpty(getText(etWebsiteLink)) ||
                !TextUtils.isEmpty(getText(etVideoIntroLink))) {
            completedSections.add("SocialWebsiteLinks");
        }

        // Check Profile Visibility Settings (Always completed as it has default values)
        completedSections.add("ProfileVisibilitySettings");

        // Check Additional Information
        if (!TextUtils.isEmpty(getText(etNidPassport)) &&
                !TextUtils.isEmpty(getText(etPresentAddress)) &&
                !TextUtils.isEmpty(getText(etPermanentAddress))) {
            completedSections.add("AdditionalInformation");
        }

        // Check Documents
        if (!TextUtils.isEmpty(profileImageUrl) ||
                !TextUtils.isEmpty(idCardUrl) ||
                !TextUtils.isEmpty(barCertificateUrl)) {
            completedSections.add("Documents");
        }

        return completedSections;
    }

    private boolean hasWorkingDaySelected() {
        return cbSaturday.isChecked() || cbSunday.isChecked() || cbMonday.isChecked() ||
                cbTuesday.isChecked() || cbWednesday.isChecked() || cbThursday.isChecked() ||
                cbFriday.isChecked();
    }

    private boolean hasValidFeeSettings() {
        if (rbFixedFee.isChecked()) {
            return !TextUtils.isEmpty(getText(etFixedFee));
        } else if (rbRangedFee.isChecked()) {
            return !TextUtils.isEmpty(getText(etMinFee)) && !TextUtils.isEmpty(getText(etMaxFee));
        }
        return false;
    }

    private boolean hasConsultationTypeSelected() {
        return cbInPerson.isChecked() || cbOnline.isChecked() || cbPhone.isChecked() || cbChat.isChecked();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText().toString().trim();
    }

    private String getSelectedGender() {
        if (rbMale.isChecked()) return "male";
        if (rbFemale.isChecked()) return "female";
        if (rbOther.isChecked()) return "other";
        return "";
    }

    private String getSelectedLanguage() {
        if (rbBangla.isChecked()) return "bangla";
        if (rbEnglish.isChecked()) return "english";
        if (rbBoth.isChecked()) return "both";
        return "";
    }

    private List<String> getSelectedWorkingDays() {
        List<String> workingDays = new ArrayList<>();

        if (cbSaturday.isChecked()) workingDays.add("Saturday");
        if (cbSunday.isChecked()) workingDays.add("Sunday");
        if (cbMonday.isChecked()) workingDays.add("Monday");
        if (cbTuesday.isChecked()) workingDays.add("Tuesday");
        if (cbWednesday.isChecked()) workingDays.add("Wednesday");
        if (cbThursday.isChecked()) workingDays.add("Thursday");
        if (cbFriday.isChecked()) workingDays.add("Friday");

        return workingDays;
    }

    private List<String> getSelectedPracticeAreas() {
        List<String> practiceAreas = new ArrayList<>();

        if (cbCriminalLaw.isChecked()) practiceAreas.add("Criminal Law");
        if (cbCivilLaw.isChecked()) practiceAreas.add("Civil Law");
        if (cbFamilyLaw.isChecked()) practiceAreas.add("Family Law");
        if (cbCorporateLaw.isChecked()) practiceAreas.add("Corporate Law");
        if (cbPropertyLaw.isChecked()) practiceAreas.add("Property Law");

        if (cbOther.isChecked()) {
            String otherArea = getText(etOtherPracticeArea);
            if (!TextUtils.isEmpty(otherArea)) {
                practiceAreas.add(otherArea);
            }
        }

        return practiceAreas;
    }

    private String getSelectedFeeType() {
        if (rbFixedFee.isChecked()) return "fixed";
        if (rbRangedFee.isChecked()) return "ranged";
        return "";
    }

    private List<String> getSelectedConsultationTypes() {
        List<String> consultationTypes = new ArrayList<>();

        if (cbInPerson.isChecked()) consultationTypes.add("In-Person");
        if (cbOnline.isChecked()) consultationTypes.add("Online");
        if (cbPhone.isChecked()) consultationTypes.add("Phone");
        if (cbChat.isChecked()) consultationTypes.add("Chat");

        return consultationTypes;
    }

    private void handleSaveSuccess(boolean isComplete) {
        String message = isComplete ?
                "Profile submitted successfully! Wait for admin approval." :
                "Draft saved successfully!";
        showToast(message);

        if (isComplete) {
            navigateToDashboard();
        }
    }

    private void handleSaveError(Exception exception) {
        String errorMessage = "Failed to save profile: " +
                (exception != null ? exception.getMessage() : "Unknown error");
        showToast(errorMessage);
        Log.e(TAG, "Error saving profile", exception);
    }

    // Utility Methods
    private AlertDialog showProgressDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        return dialog;
    }

    private void dismissProgressDialog(AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, LawyerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnCompleteProfile.setEnabled(enabled);
        btnSaveDraft.setEnabled(enabled);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Location permission granted. You can now get your location.");
            } else {
                showToast("Location permission is required to auto-detect your location");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog(uploadProgressDialog);
        dismissProgressDialog(saveProgressDialog);
    }
}