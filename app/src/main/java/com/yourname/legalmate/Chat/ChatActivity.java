package com.yourname.legalmate.Chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // UI Components
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend, buttonBack;
    private TextView textViewTitle, textViewSubtitle, textViewOnlineStatus, textViewEmptyState;
    private ImageView imageViewProfile;
    private ProgressBar progressBarLoading;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View mainLayout;

    // Firebase Components
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseFirestore firestore;
    private ChildEventListener messageListener;

    // Adapter and Data
    private MessageAdapter messageAdapter;
    private List<MessageModel> messageList;

    // User Information
    private UserProfile currentUser;
    private UserProfile otherUser;
    private String chatId;

    // State Management
    private final AtomicBoolean isLoadingMessages = new AtomicBoolean(false);
    private final AtomicBoolean isProfileLoaded = new AtomicBoolean(false);
    private boolean isDestroyed = false;
    private boolean isKeyboardVisible = false;

    // User Profile Data Class
    private static class UserProfile {
        String id;
        String name;
        String type;
        String imageUrl;
        String specialization;
        String experience;
        String location;

        UserProfile(String id) {
            this.id = id;
            this.name = "Unknown User";
            this.type = "User";
            this.imageUrl = "";
        }

        boolean isValid() {
            return id != null && !id.isEmpty() && name != null && !name.isEmpty();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        // Keyboard handling for message input
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setupWindowInsets();

        if (!initializeComponents()) {
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupKeyboardListener(); // New method for keyboard handling
        loadUserProfiles();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // New method to handle keyboard visibility
    private void setupKeyboardListener() {
        mainLayout = findViewById(R.id.main);

        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = mainLayout.getRootView().getHeight() - mainLayout.getHeight();
                boolean keyboardNowVisible = heightDiff > 200; // Threshold for keyboard detection

                if (keyboardNowVisible != isKeyboardVisible) {
                    isKeyboardVisible = keyboardNowVisible;

                    if (isKeyboardVisible) {
                        // Keyboard is visible - scroll to bottom
                        scrollToBottomSmooth();
                    }
                }
            }
        });
    }

    private boolean initializeComponents() {
        try {
            // Initialize Firebase
            firebaseAuth = FirebaseAuth.getInstance();
            databaseReference = FirebaseDatabase.getInstance().getReference();
            firestore = FirebaseFirestore.getInstance();

            // Enable offline persistence
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            } catch (Exception e) {
                Log.d(TAG, "Firebase persistence already enabled");
            }

            // Initialize current user first
            if (!initializeCurrentUser()) {
                showError("Please log in first");
                return false;
            }

            // Initialize other user
            if (!initializeOtherUser()) {
                showError("Invalid user data received");
                return false;
            }

            // Generate chat ID
            chatId = generateChatId(currentUser.id, otherUser.id);
            if (chatId == null || chatId.isEmpty()) {
                showError("Failed to generate chat ID");
                return false;
            }

            // Debug logs
            Log.d(TAG, "Current User ID: " + currentUser.id + ", Name: " + currentUser.name);
            Log.d(TAG, "Other User ID: " + otherUser.id + ", Name: " + otherUser.name);
            Log.d(TAG, "Chat ID: " + chatId);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Initialization failed", e);
            showError("Initialization failed: " + e.getMessage());
            return false;
        }
    }

    private boolean initializeCurrentUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No authenticated user found");
            return false;
        }

        currentUser = new UserProfile(user.getUid());

        // Set basic info from Firebase Auth
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            currentUser.name = user.getDisplayName().trim();
        } else if (user.getEmail() != null) {
            currentUser.name = user.getEmail().split("@")[0];
        } else {
            currentUser.name = "User";
        }

        // Set default type (will be updated from Firestore)
        currentUser.type = "Client";

        Log.d(TAG, "Current user initialized: " + currentUser.id + " - " + currentUser.name);
        return true;
    }

    private boolean initializeOtherUser() {
        if (getIntent() == null) {
            Log.e(TAG, "No intent data received");
            return false;
        }

        // Try different intent keys for flexibility
        String otherUserId = null;
        String otherUserName = null;
        String otherUserImage = null;
        String otherUserType = null;

        // Check for lawyer data
        if (getIntent().hasExtra("lawyer_id")) {
            otherUserId = getIntent().getStringExtra("lawyer_id");
            otherUserName = getIntent().getStringExtra("lawyer_name");
            otherUserImage = getIntent().getStringExtra("lawyer_image");
            otherUserType = "Lawyer";
            Log.d(TAG, "Loading lawyer data from intent");
        }
        // Check for client data
        else if (getIntent().hasExtra("client_id")) {
            otherUserId = getIntent().getStringExtra("client_id");
            otherUserName = getIntent().getStringExtra("client_name");
            otherUserImage = getIntent().getStringExtra("client_image");
            otherUserType = "Client";
            Log.d(TAG, "Loading client data from intent");
        }
        // Check for general user data
        else if (getIntent().hasExtra("user_id")) {
            otherUserId = getIntent().getStringExtra("user_id");
            otherUserName = getIntent().getStringExtra("user_name");
            otherUserImage = getIntent().getStringExtra("user_image");
            otherUserType = getIntent().getStringExtra("user_type");
            Log.d(TAG, "Loading general user data from intent");
        }

        // Validate user ID
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            Log.e(TAG, "Other user ID is null or empty");
            return false;
        }

        // Check if trying to chat with yourself
        if (otherUserId.equals(currentUser.id)) {
            Log.e(TAG, "Cannot start chat with yourself");
            showError("Cannot start chat with yourself");
            return false;
        }

        // Initialize other user
        otherUser = new UserProfile(otherUserId.trim());

        if (otherUserName != null && !otherUserName.trim().isEmpty()) {
            otherUser.name = otherUserName.trim();
        }

        if (otherUserImage != null && !otherUserImage.trim().isEmpty()) {
            otherUser.imageUrl = otherUserImage.trim();
        }

        if (otherUserType != null && !otherUserType.trim().isEmpty()) {
            otherUser.type = otherUserType.trim();
        } else {
            otherUser.type = "User"; // Default type
        }

        Log.d(TAG, "Other user initialized: " + otherUser.id + " - " + otherUser.name + " (" + otherUser.type + ")");
        return true;
    }

    private void initializeViews() {
        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        buttonBack = findViewById(R.id.button_back);
        textViewTitle = findViewById(R.id.text_view_title);
        textViewSubtitle = findViewById(R.id.text_view_subtitle);
        textViewOnlineStatus = findViewById(R.id.text_view_online_status);
        imageViewProfile = findViewById(R.id.image_view_profile);
        progressBarLoading = findViewById(R.id.progress_bar_loading);
        textViewEmptyState = findViewById(R.id.text_view_empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        // Set initial UI state
        textViewTitle.setText(otherUser.name);
        setButtonEnabled(buttonSend, false);
        showLoading(true);

        // Setup swipe refresh - FIXED: Clear messages before loading to prevent duplicates
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // Clear existing messages to prevent duplicates
                clearMessageList();
                // Reload messages
                loadMessages();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUser.id);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);

        // Show/hide empty state
        updateEmptyState();
    }

    private void setupClickListeners() {
        buttonSend.setOnClickListener(v -> sendMessage());
        buttonBack.setOnClickListener(v -> finish());

        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                setButtonEnabled(buttonSend, hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle edit text focus for keyboard
        editTextMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Delay scroll to ensure keyboard is fully visible
                editTextMessage.postDelayed(() -> scrollToBottomSmooth(), 300);
            }
        });
    }

    // Method to clear message list - FIXED: Prevents duplicate messages
    private void clearMessageList() {
        if (messageList != null && !messageList.isEmpty()) {
            int size = messageList.size();
            messageList.clear();
            if (messageAdapter != null) {
                messageAdapter.notifyItemRangeRemoved(0, size);
            }
            updateEmptyState();
        }
    }

    private void loadUserProfiles() {
        loadCurrentUserProfile(() -> {
            loadOtherUserProfile(() -> {
                isProfileLoaded.set(true);
                runOnUiThread(() -> {
                    updateUI();
                    showLoading(false);
                });
                loadMessages();
            });
        });
    }

    private void loadCurrentUserProfile(Runnable onComplete) {
        // Try Lawyer collection first
        firestore.collection("Users").document("Lawyers").collection("Lawyers")
                .document(currentUser.id)
                .collection("ProfileData").document("BasicProfileSettings")
                .get(Source.CACHE)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        handleCurrentUserLawyerProfile(documentSnapshot, onComplete);
                    } else {
                        // Try server if cache miss
                        loadCurrentUserLawyerFromServer(onComplete);
                    }
                })
                .addOnFailureListener(e -> loadCurrentUserLawyerFromServer(onComplete));
    }

    private void loadCurrentUserLawyerFromServer(Runnable onComplete) {
        firestore.collection("Users").document("Lawyers").collection("Lawyers")
                .document(currentUser.id)
                .collection("ProfileData").document("BasicProfileSettings")
                .get(Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        handleCurrentUserLawyerProfile(documentSnapshot, onComplete);
                    } else {
                        loadCurrentUserClient(onComplete);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading current user lawyer profile", e);
                    loadCurrentUserClient(onComplete);
                });
    }

    private void handleCurrentUserLawyerProfile(DocumentSnapshot documentSnapshot, Runnable onComplete) {
        currentUser.type = "Lawyer";

        String fullName = documentSnapshot.getString("fullName");
        if (fullName != null && !fullName.isEmpty()) {
            currentUser.name = fullName;
        }

        // Load lawyer documents
        firestore.collection("Users").document("Lawyers").collection("Lawyers")
                .document(currentUser.id)
                .collection("ProfileData").document("Documents")
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        String imageUrl = docSnapshot.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            currentUser.imageUrl = imageUrl;
                        }
                    }
                    saveUserProfileToRealtimeDB(currentUser, true);
                    Log.d(TAG, "Current user is Lawyer: " + currentUser.name);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading current user documents", e);
                    saveUserProfileToRealtimeDB(currentUser, true);
                    if (onComplete != null) onComplete.run();
                });
    }

    private void loadCurrentUserClient(Runnable onComplete) {
        firestore.collection("Users").document("GeneralPersons").collection("GeneralPersons")
                .document(currentUser.id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser.type = "Client";

                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            currentUser.name = fullName;
                        }

                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            currentUser.imageUrl = imageUrl;
                        }

                        currentUser.location = documentSnapshot.getString("location");

                        saveUserProfileToRealtimeDB(currentUser, true);
                        Log.d(TAG, "Current user is Client: " + currentUser.name);
                    } else {
                        Log.w(TAG, "Current user not found in any collection");
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading current user client profile", e);
                    if (onComplete != null) onComplete.run();
                });
    }

    private void loadOtherUserProfile(Runnable onComplete) {
        // Try Lawyer collection first
        firestore.collection("Users").document("Lawyers").collection("Lawyers")
                .document(otherUser.id)
                .collection("ProfileData").document("BasicProfileSettings")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        handleOtherUserLawyerProfile(documentSnapshot, onComplete);
                    } else {
                        loadOtherUserClient(onComplete);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading other user lawyer profile", e);
                    loadOtherUserClient(onComplete);
                });
    }

    private void handleOtherUserLawyerProfile(DocumentSnapshot documentSnapshot, Runnable onComplete) {
        otherUser.type = "Lawyer";

        String fullName = documentSnapshot.getString("fullName");
        if (fullName != null && !fullName.isEmpty()) {
            otherUser.name = fullName;
        }

        // Load lawyer documents
        firestore.collection("Users").document("Lawyers").collection("Lawyers")
                .document(otherUser.id)
                .collection("ProfileData").document("Documents")
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        String imageUrl = docSnapshot.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            otherUser.imageUrl = imageUrl;
                        }
                    }
                    loadLawyerProfessionalInfo(onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer documents", e);
                    loadLawyerProfessionalInfo(onComplete);
                });
    }

    private void loadLawyerProfessionalInfo(Runnable onComplete) {
        firestore.collection("Users").document("Lawyers").collection("Lawyers")
                .document(otherUser.id)
                .collection("ProfileData").document("ProfessionalInformation")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get practice areas as specialization
                        @SuppressWarnings("unchecked")
                        List<String> practiceAreas = (List<String>) documentSnapshot.get("practiceAreas");
                        if (practiceAreas != null && !practiceAreas.isEmpty()) {
                            otherUser.specialization = String.join(", ", practiceAreas);
                        }

                        otherUser.experience = documentSnapshot.getString("experience");
                    }

                    saveUserProfileToRealtimeDB(otherUser, false);
                    Log.d(TAG, "Other user is Lawyer: " + otherUser.name);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lawyer professional info", e);
                    saveUserProfileToRealtimeDB(otherUser, false);
                    if (onComplete != null) onComplete.run();
                });
    }

    private void loadOtherUserClient(Runnable onComplete) {
        firestore.collection("Users").document("GeneralPersons").collection("GeneralPersons")
                .document(otherUser.id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        otherUser.type = "Client";

                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            otherUser.name = fullName;
                        }

                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            otherUser.imageUrl = imageUrl;
                        }

                        otherUser.location = documentSnapshot.getString("location");

                        saveUserProfileToRealtimeDB(otherUser, false);
                        Log.d(TAG, "Other user is Client: " + otherUser.name);
                    } else {
                        Log.w(TAG, "Other user not found in any collection");
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading other user client profile", e);
                    if (onComplete != null) onComplete.run();
                });
    }

    private void saveUserProfileToRealtimeDB(UserProfile user, boolean isCurrentUser) {
        if (!user.isValid()) {
            Log.w(TAG, "Cannot save user profile: invalid data");
            return;
        }

        String profilePath = user.type.equals("Lawyer") ?
                "LawyerProfiles/" + user.id :
                "ClientProfiles/" + user.id;

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.id);
        userData.put("fullName", user.name);
        userData.put("userType", user.type);
        userData.put("profileImageUrl", user.imageUrl != null ? user.imageUrl : "");
        userData.put("lastUpdated", ServerValue.TIMESTAMP);

        if (user.specialization != null) {
            userData.put("specialization", user.specialization);
        }
        if (user.experience != null) {
            userData.put("experience", user.experience);
        }
        if (user.location != null) {
            userData.put("location", user.location);
        }

        databaseReference.child(profilePath)
                .updateChildren(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile saved to Realtime DB: " + user.name);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user profile to Realtime DB", e);
                });
    }

    private void updateUI() {
        if (isDestroyed) return;

        textViewTitle.setText(otherUser.name);

        // Build subtitle
        StringBuilder subtitle = new StringBuilder(otherUser.type);
        if (otherUser.specialization != null && !otherUser.specialization.isEmpty()) {
            subtitle.append(" • ").append(otherUser.specialization);
        }
        if (otherUser.experience != null && !otherUser.experience.isEmpty()) {
            subtitle.append(" • ").append(otherUser.experience).append(" years");
        }
        if (otherUser.location != null && !otherUser.location.isEmpty()) {
            subtitle.append(" • ").append(otherUser.location);
        }

        textViewSubtitle.setText(subtitle.toString());
        textViewSubtitle.setVisibility(View.VISIBLE);

        loadProfileImage();
    }

    private void loadProfileImage() {
        if (imageViewProfile == null || isDestroyed) return;

        if (otherUser.imageUrl != null && !otherUser.imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(otherUser.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_person_ai)
                    .error(R.drawable.ic_person_ai)
                    .circleCrop()
                    .into(imageViewProfile);
        } else {
            imageViewProfile.setImageResource(R.drawable.ic_person_ai);
        }
    }

    private String generateChatId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            return null;
        }
        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            showToast("Please enter a message");
            return;
        }

        // Enhanced validation
        if (currentUser == null || currentUser.id == null || currentUser.id.isEmpty()) {
            Log.e(TAG, "Current user is null or invalid");
            showError("User authentication error. Please restart the app.");
            return;
        }

        if (otherUser == null || otherUser.id == null || otherUser.id.isEmpty()) {
            Log.e(TAG, "Other user is null or invalid");
            showError("Recipient user information missing. Please try again.");
            return;
        }

        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "Chat ID is null or empty");
            chatId = generateChatId(currentUser.id, otherUser.id);
            if (chatId == null) {
                showError("Failed to generate chat session. Please try again.");
                return;
            }
        }

        Log.d(TAG, "Sending message from " + currentUser.id + " to " + otherUser.id);
        setButtonEnabled(buttonSend, false);
        sendMessageToFirebase(messageText);
    }

    private void sendMessageToFirebase(String messageText) {
        long timestamp = System.currentTimeMillis();

        DatabaseReference chatRef = databaseReference.child("Chats").child(chatId);
        DatabaseReference messageRef = chatRef.push();
        String messageId = messageRef.getKey();

        if (messageId == null) {
            handleSendFailure("Failed to generate message ID");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("senderId", currentUser.id);
        messageData.put("receiverId", otherUser.id);
        messageData.put("message", messageText);
        messageData.put("timestamp", ServerValue.TIMESTAMP);
        messageData.put("senderName", currentUser.name);
        messageData.put("senderType", currentUser.type);
        messageData.put("senderImageUrl", currentUser.imageUrl != null ? currentUser.imageUrl : "");

        messageRef.setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");
                    handleSendSuccess();
                    updateChatMetadata(messageText, timestamp);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                    handleSendFailure("Failed to send message: " + e.getMessage());
                });
    }

    private void handleSendSuccess() {
        if (isDestroyed) return;

        runOnUiThread(() -> {
            editTextMessage.setText("");
            setButtonEnabled(buttonSend, false);
            updateEmptyState();
        });
    }

    private void handleSendFailure(String errorMessage) {
        if (isDestroyed) return;

        runOnUiThread(() -> {
            showToast(errorMessage);
            setButtonEnabled(buttonSend, true);
        });
    }

    private void updateChatMetadata(String lastMessage, long timestamp) {
        Map<String, Object> updates = new HashMap<>();

        // Chat metadata
        String metadataPath = "ChatMetadata/" + chatId;
        updates.put(metadataPath + "/lastMessage", lastMessage);
        updates.put(metadataPath + "/lastMessageTime", ServerValue.TIMESTAMP);
        updates.put(metadataPath + "/lastSenderId", currentUser.id);

        // Current user chat
        String currentUserPath = "UserChats/" + currentUser.id + "/" + chatId;
        updates.put(currentUserPath + "/otherUserId", otherUser.id);
        updates.put(currentUserPath + "/otherUserName", otherUser.name);
        updates.put(currentUserPath + "/otherUserType", otherUser.type);
        updates.put(currentUserPath + "/otherUserImageUrl", otherUser.imageUrl != null ? otherUser.imageUrl : "");
        updates.put(currentUserPath + "/lastMessage", lastMessage);
        updates.put(currentUserPath + "/lastMessageTime", ServerValue.TIMESTAMP);
        updates.put(currentUserPath + "/chatId", chatId);

        // Other user chat
        String otherUserPath = "UserChats/" + otherUser.id + "/" + chatId;
        updates.put(otherUserPath + "/otherUserId", currentUser.id);
        updates.put(otherUserPath + "/otherUserName", currentUser.name);
        updates.put(otherUserPath + "/otherUserType", currentUser.type);
        updates.put(otherUserPath + "/otherUserImageUrl", currentUser.imageUrl != null ? currentUser.imageUrl : "");
        updates.put(otherUserPath + "/lastMessage", lastMessage);
        updates.put(otherUserPath + "/lastMessageTime", ServerValue.TIMESTAMP);
        updates.put(otherUserPath + "/chatId", chatId);

        databaseReference.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat metadata updated successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update chat metadata", e));
    }

    private void loadMessages() {
        if (chatId == null || isLoadingMessages.getAndSet(true)) {
            return;
        }

        DatabaseReference chatRef = databaseReference.child("Chats").child(chatId);

        if (messageListener != null) {
            chatRef.removeEventListener(messageListener);
        }

        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (isDestroyed) return;

                try {
                    MessageModel messageModel = parseMessageFromSnapshot(snapshot);
                    if (messageModel != null) {
                        runOnUiThread(() -> {
                            // Check for duplicate messages before adding - FIXED
                            boolean isDuplicate = false;
                            for (MessageModel existingMessage : messageList) {
                                if (existingMessage.getMessageId().equals(messageModel.getMessageId())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (!isDuplicate) {
                                messageList.add(messageModel);
                                messageAdapter.notifyItemInserted(messageList.size() - 1);
                                updateEmptyState();
                                scrollToBottom();
                            }
                        });
                        Log.d(TAG, "Message added: " + messageModel.getMessage());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing message", e);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if (isDestroyed) return;

                String messageId = snapshot.child("messageId").getValue(String.class);
                if (messageId != null) {
                    runOnUiThread(() -> {
                        for (int i = 0; i < messageList.size(); i++) {
                            if (messageList.get(i).getMessageId().equals(messageId)) {
                                messageList.remove(i);
                                messageAdapter.notifyItemRemoved(i);
                                updateEmptyState();
                                break;
                            }
                        }
                    });
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoadingMessages.set(false);
                Log.e(TAG, "Failed to load messages", error.toException());
                if (!isDestroyed) {
                    runOnUiThread(() -> showToast("Failed to load messages: " + error.getMessage()));
                }
            }
        };

        chatRef.addChildEventListener(messageListener);
        isLoadingMessages.set(false);
    }

    private MessageModel parseMessageFromSnapshot(DataSnapshot snapshot) {
        try {
            String messageId = snapshot.child("messageId").getValue(String.class);
            String senderId = snapshot.child("senderId").getValue(String.class);
            String receiverId = snapshot.child("receiverId").getValue(String.class);
            String message = snapshot.child("message").getValue(String.class);
            Long timestamp = snapshot.child("timestamp").getValue(Long.class);
            String senderName = snapshot.child("senderName").getValue(String.class);
            String senderType = snapshot.child("senderType").getValue(String.class);
            String senderImageUrl = snapshot.child("senderImageUrl").getValue(String.class);

            if (messageId != null && senderId != null && receiverId != null &&
                    message != null && timestamp != null) {

                return new MessageModel(
                        messageId, senderId, receiverId, message, timestamp,
                        senderName != null ? senderName : "Unknown User",
                        senderType, senderImageUrl
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing message data", e);
        }
        return null;
    }

    private void updateEmptyState() {
        if (textViewEmptyState == null || isDestroyed) return;

        boolean isEmpty = messageList.isEmpty();
        textViewEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty && isProfileLoaded.get()) {
            textViewEmptyState.setText("Start your conversation with " + otherUser.name);
        }
    }

    private void scrollToBottom() {
        if (messageList.size() > 0 && !isDestroyed) {
            recyclerViewMessages.scrollToPosition(messageList.size() - 1);
        }
    }

    // New method for smooth scrolling - FIXED: Better keyboard handling
    private void scrollToBottomSmooth() {
        if (messageList.size() > 0 && !isDestroyed) {
            recyclerViewMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    // Utility Methods
    private void showLoading(boolean show) {
        if (progressBarLoading != null && !isDestroyed) {
            progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        if (button != null && !isDestroyed) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void showToast(String message) {
        if (!isDestroyed) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        Log.e(TAG, message);
        showToast(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update user online status
        updateOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Update user offline status
        updateOnlineStatus(false);
    }

    private void updateOnlineStatus(boolean isOnline) {
        if (currentUser != null && currentUser.isValid()) {
            Map<String, Object> presenceData = new HashMap<>();
            presenceData.put("online", isOnline);
            presenceData.put("lastSeen", ServerValue.TIMESTAMP);

            databaseReference.child("UserPresence").child(currentUser.id)
                    .updateChildren(presenceData)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update presence", e));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;

        // Remove message listener
        if (messageListener != null && chatId != null) {
            databaseReference.child("Chats").child(chatId).removeEventListener(messageListener);
        }

        // Update offline status
        updateOnlineStatus(false);

        Log.d(TAG, "ChatActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // Additional helper methods for better user experience
    private void showTypingIndicator(boolean show) {
        // Implementation for typing indicator
        if (textViewOnlineStatus != null && !isDestroyed) {
            if (show) {
                textViewOnlineStatus.setText("typing...");
                textViewOnlineStatus.setVisibility(View.VISIBLE);
            } else {
                textViewOnlineStatus.setVisibility(View.GONE);
            }
        }
    }

    // Method to handle network connectivity changes
    private void handleNetworkChange(boolean isConnected) {
        if (!isDestroyed) {
            if (!isConnected) {
                showToast("No internet connection");
            } else {
                // Refresh messages when connection is restored
                clearMessageList();
                loadMessages();
            }
        }
    }

    // Method to mark messages as read
    private void markMessagesAsRead() {
        if (chatId == null || currentUser == null || !currentUser.isValid()) return;

        Map<String, Object> readStatus = new HashMap<>();
        readStatus.put("lastReadTime", ServerValue.TIMESTAMP);
        readStatus.put("userId", currentUser.id);

        databaseReference.child("MessageReadStatus").child(chatId).child(currentUser.id)
                .updateChildren(readStatus)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to mark messages as read", e));
    }

    // Method to get message count
    public int getMessageCount() {
        return messageList != null ? messageList.size() : 0;
    }

    // Method to clear chat (if needed)
    private void clearChat() {
        if (messageList != null) {
            int size = messageList.size();
            messageList.clear();
            if (messageAdapter != null) {
                messageAdapter.notifyItemRangeRemoved(0, size);
            }
            updateEmptyState();
        }
    }

    // Method to handle message deletion
    private void deleteMessage(String messageId) {
        if (chatId == null || messageId == null) return;

        databaseReference.child("Chats").child(chatId).child(messageId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message deleted successfully");
                    showToast("Message deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete message", e);
                    showToast("Failed to delete message");
                });
    }

    // Method to report inappropriate content
    private void reportMessage(String messageId, String reason) {
        if (chatId == null || messageId == null || currentUser == null) return;

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("chatId", chatId);
        reportData.put("messageId", messageId);
        reportData.put("reporterId", currentUser.id);
        reportData.put("reporterName", currentUser.name);
        reportData.put("reason", reason);
        reportData.put("timestamp", ServerValue.TIMESTAMP);

        databaseReference.child("MessageReports").push()
                .setValue(reportData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message reported successfully");
                    showToast("Message reported");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to report message", e);
                    showToast("Failed to report message");
                });
    }

    // Method to block user
    private void blockUser() {
        if (currentUser == null || otherUser == null) return;

        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blockedUserId", otherUser.id);
        blockData.put("blockedUserName", otherUser.name);
        blockData.put("timestamp", ServerValue.TIMESTAMP);

        databaseReference.child("BlockedUsers").child(currentUser.id).child(otherUser.id)
                .setValue(blockData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User blocked successfully");
                    showToast("User blocked");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to block user", e);
                    showToast("Failed to block user");
                });
    }

    // Method to check if user is blocked
    private void checkIfUserIsBlocked(Runnable onNotBlocked, Runnable onBlocked) {
        if (currentUser == null || otherUser == null) {
            if (onNotBlocked != null) onNotBlocked.run();
            return;
        }

        databaseReference.child("BlockedUsers").child(currentUser.id).child(otherUser.id)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        if (onBlocked != null) onBlocked.run();
                    } else {
                        if (onNotBlocked != null) onNotBlocked.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking block status", e);
                    if (onNotBlocked != null) onNotBlocked.run();
                });
    }

    // Method to handle message retry
    private void retryFailedMessage(MessageModel failedMessage) {
        if (failedMessage == null) return;

        sendMessageToFirebase(failedMessage.getMessage());
    }

    // Method to save draft message
    private void saveDraftMessage() {
        String draftText = editTextMessage.getText().toString().trim();
        if (draftText.isEmpty() || chatId == null || currentUser == null) return;

        Map<String, Object> draftData = new HashMap<>();
        draftData.put("message", draftText);
        draftData.put("timestamp", ServerValue.TIMESTAMP);

        databaseReference.child("MessageDrafts").child(currentUser.id).child(chatId)
                .setValue(draftData)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to save draft", e));
    }

    // Method to load draft message
    private void loadDraftMessage() {
        if (chatId == null || currentUser == null) return;

        databaseReference.child("MessageDrafts").child(currentUser.id).child(chatId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String draftMessage = snapshot.child("message").getValue(String.class);
                        if (draftMessage != null && !draftMessage.isEmpty() && !isDestroyed) {
                            runOnUiThread(() -> editTextMessage.setText(draftMessage));

                            // Remove draft after loading
                            snapshot.getRef().removeValue();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load draft", e));
    }
}