package com.yourname.legalmate.GeneralPersonPortal.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yourname.legalmate.GeneralPersonPortal.Activities.BookConsultationActivity;
import com.yourname.legalmate.GeneralPersonPortal.Activities.CaseTrackingActivity;
import com.yourname.legalmate.GeneralPersonPortal.Activities.FaqsLegalTipsActivity;
import com.yourname.legalmate.GeneralPersonPortal.Activities.MyCalendarActivity;
import com.yourname.legalmate.GeneralPersonPortal.Adapters.SliderAdapter;
import com.yourname.legalmate.GeneralPersonPortal.Adapters.SuggestedLawyerAdapter;
import com.yourname.legalmate.GeneralPersonPortal.Models.SuggestedLawyerModel;
import com.yourname.legalmate.GeneralPersonPortal.Models.SliderItem;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GeneralHomeFragment extends Fragment implements SliderAdapter.OnSliderClickListener {

    private static final String TAG = "GeneralHomeFragment";

    // Header views
    private CircleImageView ivUserProfile;
    private ImageView ivNotification;
    private TextView tvGreetingMessage;

    // Slider
    private RecyclerView rvSlider;
    private SliderAdapter sliderAdapter;
    private List<SliderItem> sliderItems;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Service CardViews
    private CardView cvCaseTracking;
    private CardView cvBookConsultation;
    private CardView cvCalendar;
    private CardView cvFaqsLegalTips;


    // Recent Activities & Suggested Lawyers
    private RecyclerView rvRecentActivities, rvSuggestLawyers;

    // User data
    private String userName = "";
    private String userProfileImageUrl = "";

    // Suggested Lawyers
    private SuggestedLawyerAdapter suggestedLawyerAdapter;
    private List<SuggestedLawyerModel> suggestedLawyers;

    // Loading state variables
    private int totalLawyersToLoad = 0;
    private int loadedLawyersCount = 0;
    private List<SuggestedLawyerModel> allLoadedLawyers = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews(view);

        // Setup RecyclerViews
        setupSliderRecyclerView();
        setupSuggestedLawyersRecyclerView();

        // Setup service click listeners
        setupServiceClickListeners();

        // Setup header listeners
        setupHeaderListeners();

        // Load user data first, then slider data
        loadUserData();

        // Setup recent activities
        setupRecentActivities();

        // Load suggested lawyers
        loadSuggestedLawyers();

        return view;
    }

    private void initViews(View view) {
        // Header views
        ivUserProfile = view.findViewById(R.id.ivUserProfile);
        ivNotification = view.findViewById(R.id.ivNotification);
        tvGreetingMessage = view.findViewById(R.id.tvGreetingMessage);

        // Slider
        rvSlider = view.findViewById(R.id.rvSlider);

        // Service CardViews
        cvCaseTracking = view.findViewById(R.id.cvCaseTracking);
        cvBookConsultation = view.findViewById(R.id.cvBookConsultation);
        cvCalendar = view.findViewById(R.id.cvCalendar);
        cvFaqsLegalTips = view.findViewById(R.id.cvFaqsLegalTips);


        // Recent Activities and Suggested Lawyers
        rvRecentActivities = view.findViewById(R.id.rvRecentActivities);
        rvSuggestLawyers = view.findViewById(R.id.rvSuggestLawyers);

        // Initialize suggested lawyers list
        suggestedLawyers = new ArrayList<>();
    }

    private void setupSuggestedLawyersRecyclerView() {
        try {
            suggestedLawyerAdapter = new SuggestedLawyerAdapter(getContext(), suggestedLawyers);

            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            rvSuggestLawyers.setLayoutManager(layoutManager);
            rvSuggestLawyers.setAdapter(suggestedLawyerAdapter);

            // Add item decoration for spacing
            rvSuggestLawyers.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    outRect.right = 16; // Add right margin
                }
            });

            Log.d(TAG, "Suggested lawyers RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up suggested lawyers RecyclerView", e);
        }
    }

    private void loadSuggestedLawyers() {
        Log.d(TAG, "Loading suggested lawyers...");

        if (db == null) {
            Log.e(TAG, "Firestore instance is null");
            return;
        }

        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalLawyersToLoad = queryDocumentSnapshots.size();
                    loadedLawyersCount = 0;
                    allLoadedLawyers.clear();

                    Log.d(TAG, "Total lawyers to load: " + totalLawyersToLoad);

                    if (totalLawyersToLoad == 0) {
                        Log.d(TAG, "No lawyers found in database");
                        showNoLawyersMessage();
                        return;
                    }

                    for (QueryDocumentSnapshot lawyerDoc : queryDocumentSnapshots) {
                        String lawyerId = lawyerDoc.getId();
                        loadLawyerProfileForSuggestion(lawyerId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading suggested lawyers", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load suggested lawyers", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLawyerProfileForSuggestion(String lawyerId) {
        db.collection("Users")
                .document("Lawyers")
                .collection("Lawyers")
                .document(lawyerId)
                .collection("ProfileData")
                .get()
                .addOnSuccessListener(profileSnapshots -> {
                    SuggestedLawyerModel lawyer = buildSuggestedLawyerFromProfile(lawyerId, profileSnapshots.getDocuments());

                    loadedLawyersCount++;
                    Log.d(TAG, "Loaded lawyer " + loadedLawyersCount + "/" + totalLawyersToLoad + ": " +
                            (lawyer != null ? lawyer.getFullName() : "null"));

                    if (lawyer != null && lawyer.isQualifiedForSuggestion()) {
                        allLoadedLawyers.add(lawyer);
                        Log.d(TAG, "Added qualified lawyer: " + lawyer.getFullName() +
                                ", Rating: " + lawyer.getRating() +
                                ", Total qualified: " + allLoadedLawyers.size());
                    } else if (lawyer != null) {
                        Log.d(TAG, "Lawyer not qualified: " + lawyer.getFullName() +
                                ", Complete: " + lawyer.isProfileComplete() +
                                ", Verified: " + lawyer.isVerified() +
                                ", Active: " + lawyer.isActive() +
                                ", Rating: " + lawyer.getRating());
                    }

                    // Process when all lawyers are loaded
                    if (loadedLawyersCount >= totalLawyersToLoad) {
                        processSuggestedLawyers();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile for suggested lawyer: " + lawyerId, e);
                    loadedLawyersCount++;

                    // Still process if this was the last one
                    if (loadedLawyersCount >= totalLawyersToLoad) {
                        processSuggestedLawyers();
                    }
                });
    }

    private SuggestedLawyerModel buildSuggestedLawyerFromProfile(String lawyerId, List<DocumentSnapshot> profileDocs) {
        SuggestedLawyerModel lawyer = new SuggestedLawyerModel();
        lawyer.setLawyerId(lawyerId);

        try {
            boolean hasBasicInfo = false;
            boolean hasProfessionalInfo = false;

            for (DocumentSnapshot doc : profileDocs) {
                if (!doc.exists()) continue;

                String sectionName = doc.getId();
                Log.d(TAG, "Processing section: " + sectionName + " for lawyer: " + lawyerId);

                switch (sectionName) {
                    case "BasicProfileSettings":
                        String fullName = doc.getString("fullName");
                        if (fullName != null && !fullName.trim().isEmpty()) {
                            lawyer.setFullName(fullName);
                            hasBasicInfo = true;
                        }
                        break;

                    case "ProfessionalInformation":
                        List<String> practiceAreas = (List<String>) doc.get("practiceAreas");
                        if (practiceAreas != null && !practiceAreas.isEmpty()) {
                            lawyer.setPracticeAreas(practiceAreas);
                            hasProfessionalInfo = true;
                        }
                        lawyer.setExperience(doc.getString("experience"));
                        lawyer.setChamberName(doc.getString("chamberName"));
                        break;

                    case "Documents":
                        lawyer.setProfileImageUrl(doc.getString("profileImageUrl"));
                        break;

                    case "ProfileStatus":
                        lawyer.setVerified(doc.getBoolean("isVerified") != null ? doc.getBoolean("isVerified") : false);
                        lawyer.setActive(doc.getBoolean("isActive") != null ? doc.getBoolean("isActive") : false);
                        lawyer.setProfileStatus(doc.getString("profileStatus"));

                        // Get rating and review count
                        Double rating = doc.getDouble("rating");
                        lawyer.setRating(rating != null ? rating : 0.0);

                        Long totalReviews = doc.getLong("totalReviews");
                        lawyer.setReviewCount(totalReviews != null ? totalReviews.intValue() : 0);

                        Log.d(TAG, "Lawyer " + lawyerId + " - Rating: " + lawyer.getRating() +
                                ", Reviews: " + lawyer.getReviewCount() +
                                ", Verified: " + lawyer.isVerified() +
                                ", Active: " + lawyer.isActive());
                        break;

                    case "ConsultationFees":
                        lawyer.setAvailable(doc.getBoolean("isAvailable") != null ? doc.getBoolean("isAvailable") : false);
                        break;
                }
            }

            // Validate if lawyer has minimum required info
            if (!hasBasicInfo) {
                Log.w(TAG, "Lawyer " + lawyerId + " missing basic info");
                return null;
            }

            return lawyer;

        } catch (Exception e) {
            Log.e(TAG, "Error building suggested lawyer profile for " + lawyerId, e);
            return null;
        }
    }

    private void processSuggestedLawyers() {
        Log.d(TAG, "Processing " + allLoadedLawyers.size() + " qualified lawyers");

        if (allLoadedLawyers.isEmpty()) {
            Log.d(TAG, "No qualified lawyers found");
            showNoLawyersMessage();
            return;
        }

        // Sort by rating first (highest to lowest), then by review count
        Collections.sort(allLoadedLawyers, (l1, l2) -> {
            // First sort by rating
            int ratingCompare = Double.compare(l2.getRating(), l1.getRating());
            if (ratingCompare != 0) {
                return ratingCompare;
            }
            // If ratings are equal, sort by review count
            return Integer.compare(l2.getReviewCount(), l1.getReviewCount());
        });

        // Take top lawyers (max 10)
        List<SuggestedLawyerModel> topLawyers = new ArrayList<>();
        int maxLawyers = Math.min(10, allLoadedLawyers.size());

        for (int i = 0; i < maxLawyers; i++) {
            topLawyers.add(allLoadedLawyers.get(i));
        }

        Log.d(TAG, "Selected top " + topLawyers.size() + " lawyers for display");

        // Update UI on main thread
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                try {
                    suggestedLawyers.clear();
                    suggestedLawyers.addAll(topLawyers);

                    if (suggestedLawyerAdapter != null) {
                        suggestedLawyerAdapter.updateData(topLawyers); // সরাসরি পাস


                    Log.d(TAG, "Adapter updated with " + suggestedLawyers.size() + " lawyers");

                        // Make RecyclerView visible
                        if (rvSuggestLawyers != null) {
                            rvSuggestLawyers.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e(TAG, "SuggestedLawyerAdapter is null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating suggested lawyers UI", e);
                }
            });
        }
    }

    private void showNoLawyersMessage() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "No suggested lawyers available at the moment", Toast.LENGTH_SHORT).show();
                }

                // Hide RecyclerView if no data
                if (rvSuggestLawyers != null) {
                    rvSuggestLawyers.setVisibility(View.GONE);
                }
            });
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("Users")
                    .document("GeneralPersons")
                    .collection("GeneralPersons")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                userName = document.getString("name");
                                if (userName == null || userName.isEmpty()) {
                                    userName = currentUser.getDisplayName();
                                    if (userName == null || userName.isEmpty()) {
                                        userName = "User";
                                    }
                                }

                                userProfileImageUrl = document.getString("profileImageUrl");
                                if (userProfileImageUrl == null) {
                                    userProfileImageUrl = "";
                                }

                                updateUserUI();
                                Log.d(TAG, "User data loaded: " + userName);
                            } else {
                                userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
                                userProfileImageUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";
                                updateUserUI();
                            }
                        } else {
                            Log.e(TAG, "Error getting user data: ", task.getException());
                            userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
                            userProfileImageUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";
                            updateUserUI();
                        }

                        // Load slider data after user data is loaded
                        loadSliderData();
                    });
        } else {
            userName = "User";
            userProfileImageUrl = "";
            updateUserUI();
            loadSliderData();
        }
    }

    private void updateUserUI() {
        if (getContext() == null) return;

        try {
            // Update profile image
            if (userProfileImageUrl != null && !userProfileImageUrl.isEmpty()) {
                Glide.with(getContext())
                        .load(userProfileImageUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_user_profile)
                        .error(R.drawable.ic_user_profile)
                        .into(ivUserProfile);
            } else {
                ivUserProfile.setImageResource(R.drawable.ic_user_profile);
            }

            // Update greeting TextView if available
            if (tvGreetingMessage != null) {
                String greetingMessage = getGreetingMessage();
                tvGreetingMessage.setText(greetingMessage);
            }

            Log.d(TAG, "UI updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating user UI", e);
        }
    }

    private String getGreetingMessage() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }

        // Extract first name from full name
        String firstName = userName;
        if (userName.contains(" ")) {
            firstName = userName.split(" ")[0];
        }

        return greeting + ", " + firstName + "!";
    }

    private void setupHeaderListeners() {
        if (ivUserProfile != null) {
            ivUserProfile.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Profile clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Notifications clicked", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupServiceClickListeners() {
        // Case Tracking click listener
        if (cvCaseTracking != null) {
            cvCaseTracking.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), CaseTrackingActivity.class);
                startActivity(intent);
            });
        }

        // Book Consultation click listener
        if (cvBookConsultation != null) {
            cvBookConsultation.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), BookConsultationActivity.class);
                startActivity(intent);
            });
        }

        // Calendar click listener
        if (cvCalendar != null) {
            cvCalendar.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), MyCalendarActivity.class);
                startActivity(intent);
            });
        }



        // FAQs/Legal Tips click listener
        if (cvFaqsLegalTips != null) {
            cvFaqsLegalTips.setOnClickListener(v -> {

                Intent intent = new Intent(getContext(), FaqsLegalTipsActivity.class);
                startActivity(intent);


            });
        }


    }

    private void setupSliderRecyclerView() {
        try {
            sliderItems = new ArrayList<>();
            sliderAdapter = new SliderAdapter(getContext(), sliderItems);
            sliderAdapter.setOnSliderClickListener(this);

            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            rvSlider.setLayoutManager(layoutManager);
            rvSlider.setAdapter(sliderAdapter);

            // Add item decoration for spacing
            rvSlider.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    outRect.right = 24; // Add right margin
                }
            });

            Log.d(TAG, "Slider RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up slider RecyclerView", e);
        }
    }

    private void setupRecentActivities() {
        try {
            LinearLayoutManager recentActivitiesLayoutManager = new LinearLayoutManager(getContext());
            rvRecentActivities.setLayoutManager(recentActivitiesLayoutManager);
            Log.d(TAG, "Recent activities RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up recent activities RecyclerView", e);
        }
    }

    private void loadSliderData() {
        if (db == null) {
            Log.e(TAG, "Firestore instance is null");
            return;
        }

        db.collection("General Person Home Slider")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sliderItems.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                SliderItem item = document.toObject(SliderItem.class);
                                sliderItems.add(item);
                                Log.d(TAG, "Slider item loaded: " + item.getSliderTitle());
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing slider item: ", e);
                            }
                        }

                        // Update adapter with new data
                        if (sliderAdapter != null) {
                            sliderAdapter.updateData(sliderItems);
                        }

                        Log.d(TAG, "Total slider items loaded: " + sliderItems.size());

                        if (sliderItems.isEmpty()) {
                            showEmptySliderState();
                        }

                    } else {
                        Log.e(TAG, "Error getting slider data: ", task.getException());
                        showSliderErrorState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load slider data: ", e);
                    showSliderErrorState();
                });
    }

    private void showEmptySliderState() {
        if (getContext() != null) {
            Toast.makeText(getContext(), "No slider content available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSliderErrorState() {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Failed to load slider content", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSliderClick(SliderItem sliderItem, int position) {
        Log.d(TAG, "Slider clicked: " + sliderItem.getSliderTitle());

        if (getContext() != null) {
            Toast.makeText(getContext(), "Clicked: " + sliderItem.getSliderTitle(), Toast.LENGTH_SHORT).show();
        }

        // Enhanced navigation logic based on slider content
        if (sliderItem.getSliderTitle() != null) {
            String title = sliderItem.getSliderTitle().toLowerCase();
            switch (title) {
                case "track your case status":
                case "case tracking":
                    if (cvCaseTracking != null) {
                        cvCaseTracking.performClick();
                    }
                    break;

                case "talk to a lawyer now":
                case "book consultation":
                    if (cvBookConsultation != null) {
                        cvBookConsultation.performClick();
                    }
                    break;


                case "faqs":
                case "legal tips":
                    if (cvFaqsLegalTips != null) {
                        cvFaqsLegalTips.performClick();
                    }
                    break;

                default:
                    Log.d(TAG, "No specific action defined for: " + sliderItem.getSliderTitle());
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when fragment becomes visible again
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            loadUserData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clear references to avoid memory leaks
        if (sliderAdapter != null) {
            sliderAdapter.setOnSliderClickListener(null);
            sliderAdapter = null;
        }

        if (suggestedLawyerAdapter != null) {
            suggestedLawyerAdapter = null;
        }

        if (suggestedLawyers != null) {
            suggestedLawyers.clear();
            suggestedLawyers = null;
        }

        if (allLoadedLawyers != null) {
            allLoadedLawyers.clear();
            allLoadedLawyers = null;
        }

        if (sliderItems != null) {
            sliderItems.clear();
            sliderItems = null;
        }

        // Clear view references
        ivUserProfile = null;
        ivNotification = null;
        tvGreetingMessage = null;
        cvCaseTracking = null;
        cvBookConsultation = null;
        cvCalendar = null;
        cvFaqsLegalTips = null;
        rvSlider = null;
        rvRecentActivities = null;
        rvSuggestLawyers = null;

        // Clear Firebase references
        db = null;
        mAuth = null;

        Log.d(TAG, "Fragment destroyed and references cleared");
    }
}