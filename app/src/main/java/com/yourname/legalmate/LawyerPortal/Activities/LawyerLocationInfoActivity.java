package com.yourname.legalmate.LawyerPortal.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.legalmate.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LawyerLocationInfoActivity extends AppCompatActivity {

    private static final String TAG = "LawyerLocationInfo";
    private static final int LOCATION_PERMISSION_CODE = 200;

    // UI Components
    private Toolbar toolbar;
    private MaterialButton btnGetLocation, btnUpdate;
    private AutoCompleteTextView actvDistrict, actvUpazila;
    private TextInputEditText etArea, etLatitude, etLongitude;
    private TextInputLayout tilDistrict, tilUpazila, tilArea, tilLatitude, tilLongitude;
    private MaterialCheckBox cbDhaka, cbChittagong, cbSylhet, cbRajshahi, cbBarisal, cbKhulna, cbRangpur, cbMymensingh;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    // Progress Dialog
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lawyer_location_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeFirebase();
        initializeViews();
        setupToolbar();
        initializeLocationServices();
        setupClickListeners();
        loadExistingData();
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
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Location components
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnUpdate = findViewById(R.id.btnUpdate);

        actvDistrict = findViewById(R.id.actvDistrict);
        actvUpazila = findViewById(R.id.actvUpazila);

        etArea = findViewById(R.id.etArea);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);

        tilDistrict = findViewById(R.id.tilDistrict);
        tilUpazila = findViewById(R.id.tilUpazila);
        tilArea = findViewById(R.id.tilArea);
        tilLatitude = findViewById(R.id.tilLatitude);
        tilLongitude = findViewById(R.id.tilLongitude);

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

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Location Information");
        }
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationDropdowns();
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

    private void setupClickListeners() {
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnUpdate.setOnClickListener(v -> updateLocationInfo());
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

    private void loadExistingData() {
        progressDialog = showProgressDialog("Loading location data...");

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("LocationInformation")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    dismissProgressDialog();

                    if (documentSnapshot.exists()) {
                        populateFields(documentSnapshot.getData());
                    } else {
                        Log.d(TAG, "No existing location data found");
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Log.e(TAG, "Error loading location data", e);
                    showToast("Failed to load existing data");
                });
    }

    private void populateFields(Map<String, Object> data) {
        if (data == null) return;

        // District and Upazila
        String district = (String) data.get("district");
        String upazila = (String) data.get("upazila");
        String area = (String) data.get("area");

        if (!TextUtils.isEmpty(district)) {
            actvDistrict.setText(district, false);
            setupUpazilaDropdown(district);
        }

        if (!TextUtils.isEmpty(upazila)) {
            actvUpazila.setText(upazila, false);
        }

        if (!TextUtils.isEmpty(area)) {
            etArea.setText(area);
        }

        // GPS Coordinates
        Double latitude = (Double) data.get("latitude");
        Double longitude = (Double) data.get("longitude");

        if (latitude != null && longitude != null) {
            currentLatitude = latitude;
            currentLongitude = longitude;
            etLatitude.setText(String.valueOf(latitude));
            etLongitude.setText(String.valueOf(longitude));
        }

        // Service Areas
        List<String> serviceAreas = (List<String>) data.get("serviceAreas");
        if (serviceAreas != null) {
            for (String serviceArea : serviceAreas) {
                setServiceAreaCheckbox(serviceArea, true);
            }
        }
    }

    private void setServiceAreaCheckbox(String area, boolean checked) {
        switch (area) {
            case "Dhaka":
                cbDhaka.setChecked(checked);
                break;
            case "Chittagong":
                cbChittagong.setChecked(checked);
                break;
            case "Sylhet":
                cbSylhet.setChecked(checked);
                break;
            case "Rajshahi":
                cbRajshahi.setChecked(checked);
                break;
            case "Barisal":
                cbBarisal.setChecked(checked);
                break;
            case "Khulna":
                cbKhulna.setChecked(checked);
                break;
            case "Rangpur":
                cbRangpur.setChecked(checked);
                break;
            case "Mymensingh":
                cbMymensingh.setChecked(checked);
                break;
        }
    }

    private void updateLocationInfo() {
        if (!validateForm()) {
            return;
        }

        progressDialog = showProgressDialog("Updating location information...");
        btnUpdate.setEnabled(false);

        Map<String, Object> locationInfo = buildLocationData();

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(userId)
                .collection("ProfileData")
                .document("LocationInformation")
                .set(locationInfo)
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    btnUpdate.setEnabled(true);
                    showToast("Location information updated successfully!");
                    Log.d(TAG, "Location info updated successfully");
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    btnUpdate.setEnabled(true);
                    showToast("Failed to update location information");
                    Log.e(TAG, "Error updating location info", e);
                });
    }

    private boolean validateForm() {
        clearErrors();
        boolean isValid = true;

        // District is required
        if (TextUtils.isEmpty(actvDistrict.getText().toString().trim())) {
            tilDistrict.setError("Please select your district");
            isValid = false;
        }

        // At least one service area must be selected
        if (!hasServiceAreaSelected()) {
            showToast("Please select at least one service area");
            isValid = false;
        }

        return isValid;
    }

    private boolean hasServiceAreaSelected() {
        return cbDhaka.isChecked() || cbChittagong.isChecked() || cbSylhet.isChecked() ||
                cbRajshahi.isChecked() || cbBarisal.isChecked() || cbKhulna.isChecked() ||
                cbRangpur.isChecked() || cbMymensingh.isChecked();
    }

    private void clearErrors() {
        tilDistrict.setError(null);
        tilUpazila.setError(null);
        tilArea.setError(null);
    }

    private Map<String, Object> buildLocationData() {
        Map<String, Object> locationInfo = new HashMap<>();

        locationInfo.put("district", actvDistrict.getText().toString().trim());
        locationInfo.put("upazila", actvUpazila.getText().toString().trim());
        locationInfo.put("area", etArea.getText().toString().trim());
        locationInfo.put("latitude", currentLatitude);
        locationInfo.put("longitude", currentLongitude);
        locationInfo.put("serviceAreas", getSelectedServiceAreas());
        locationInfo.put("isLocationVerified", currentLatitude != 0.0 && currentLongitude != 0.0);
        locationInfo.put("updatedAt", System.currentTimeMillis());

        return locationInfo;
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

    // Utility Methods
    private AlertDialog showProgressDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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
        dismissProgressDialog();
    }
}