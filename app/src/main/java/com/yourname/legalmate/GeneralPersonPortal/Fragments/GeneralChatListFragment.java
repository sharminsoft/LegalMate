package com.yourname.legalmate.GeneralPersonPortal.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yourname.legalmate.Chat.ChatListAdapter;
import com.yourname.legalmate.Chat.ChatListModel;
import com.yourname.legalmate.Chat.ChatActivity;
import com.yourname.legalmate.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeneralChatListFragment extends Fragment {

    private static final String TAG = "GeneralChatListFragment";

    private RecyclerView recyclerViewChatList;
    private LinearLayout layoutEmpty;
    private TextView textViewEmpty;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private ValueEventListener chatListListener;

    private ChatListAdapter chatListAdapter;
    private List<ChatListModel> chatList;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_message, container, false);

        Log.d(TAG, "GeneralChatListFragment created");

        if (!initializeFirebase()) {
            showErrorState("Please log in first");
            return view;
        }

        initializeViews(view);
        setupRecyclerView();
        loadChatList();

        return view;
    }

    private boolean initializeFirebase() {
        try {
            firebaseAuth = FirebaseAuth.getInstance();
            databaseReference = FirebaseDatabase.getInstance().getReference();

            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
                Log.d(TAG, "Current user ID: " + currentUserId);
                return true;
            } else {
                Log.e(TAG, "User not authenticated");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            return false;
        }
    }

    private void initializeViews(View view) {
        recyclerViewChatList = view.findViewById(R.id.recycler_view_chat_list);
        layoutEmpty = view.findViewById(R.id.text_view_empty);
        textViewEmpty = view.findViewById(R.id.text_view_empty_message);

        Log.d(TAG, "Views initialized");
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getContext(), chatList, this::openChat);

        if (recyclerViewChatList != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerViewChatList.setLayoutManager(layoutManager);
            recyclerViewChatList.setAdapter(chatListAdapter);
            Log.d(TAG, "RecyclerView setup complete");
        }
    }

    private void openChat(ChatListModel chatModel) {
        if (getContext() != null && chatModel != null) {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("lawyer_id", chatModel.getOtherUserId());
            intent.putExtra("lawyer_name", chatModel.getOtherUserName());
            intent.putExtra("lawyer_image", chatModel.getOtherUserImageUrl());

            Log.d(TAG, "Opening chat with: " + chatModel.getOtherUserName());
            startActivity(intent);
        }
    }

    private void loadChatList() {
        if (currentUserId == null) {
            showErrorState("User not authenticated");
            return;
        }

        loadFromUserChats();
        loadFromChatsDirectory();
    }

    private void loadFromUserChats() {
        Log.d(TAG, "Loading chats from UserChats node");

        databaseReference.child("UserChats").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "UserChats data received: " + snapshot.getChildrenCount() + " chats");

                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            chatList.clear();

                            for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                                try {
                                    String otherUserId = chatSnapshot.child("otherUserId").getValue(String.class);
                                    String otherUserName = chatSnapshot.child("otherUserName").getValue(String.class);
                                    String otherUserType = chatSnapshot.child("otherUserType").getValue(String.class);
                                    String otherUserImageUrl = chatSnapshot.child("otherUserImageUrl").getValue(String.class);
                                    String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                                    Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);

                                    if (otherUserId != null) {
                                        ChatListModel chatModel = new ChatListModel(
                                                otherUserId,
                                                otherUserName != null ? otherUserName : "Unknown User",
                                                otherUserType != null ? otherUserType : "User",
                                                lastMessage != null ? lastMessage : "No messages yet",
                                                lastMessageTime != null ? lastMessageTime : 0,
                                                otherUserImageUrl
                                        );

                                        chatList.add(chatModel);
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Error parsing UserChats data", e);
                                }
                            }

                            sortAndUpdateChatList();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "UserChats loading cancelled", error.toException());
                    }
                });
    }

    private void loadFromChatsDirectory() {
        Log.d(TAG, "Loading chats from Chats directory");

        chatListListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Chats directory data received: " + snapshot.getChildrenCount() + " total chats");

                Set<String> processedUsers = new HashSet<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    Log.d(TAG, "Processing chat ID: " + chatId);

                    if (chatId != null && chatId.contains(currentUserId)) {
                        try {
                            String otherUserId = extractOtherUserId(chatId, currentUserId);

                            if (otherUserId != null && !otherUserId.equals(currentUserId)
                                    && !processedUsers.contains(otherUserId)) {

                                processedUsers.add(otherUserId);
                                Log.d(TAG, "Found chat with user: " + otherUserId);

                                processUserChat(otherUserId, chatSnapshot);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing chat: " + chatId, e);
                        }
                    }
                }

                if (processedUsers.isEmpty() && chatList.isEmpty()) {
                    updateEmptyState();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Chats loading cancelled", error.toException());
                showErrorState("Error loading chats: " + error.getMessage());
            }
        };

        databaseReference.child("Chats").addValueEventListener(chatListListener);
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

    private void processUserChat(String userId, DataSnapshot chatSnapshot) {
        databaseReference.child("Users").child("Lawyers").child("Lawyers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot lawyerSnapshot) {
                        if (lawyerSnapshot.exists()) {
                            String fullName = lawyerSnapshot.child("fullName").getValue(String.class);
                            String profileImageUrl = lawyerSnapshot.child("profileImageUrl").getValue(String.class);
                            String specialization = lawyerSnapshot.child("specialization").getValue(String.class);

                            String[] lastMessageData = extractLastMessage(chatSnapshot);

                            ChatListModel chatModel = new ChatListModel(
                                    userId,
                                    fullName != null ? fullName : "Unknown Lawyer",
                                    "Lawyer" + (specialization != null ? " • " + specialization : ""),
                                    lastMessageData[0],
                                    Long.parseLong(lastMessageData[1]),
                                    profileImageUrl
                            );

                            addToChatListIfNotExists(chatModel);
                        } else {
                            // Try GeneralPersons collection
                            databaseReference.child("Users").child("GeneralPersons").child("GeneralPersons").child(userId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot clientSnapshot) {
                                            String fullName = "Unknown User";
                                            String profileImageUrl = null;
                                            String userType = "User";

                                            if (clientSnapshot.exists()) {
                                                fullName = clientSnapshot.child("fullName").getValue(String.class);
                                                profileImageUrl = clientSnapshot.child("profileImageUrl").getValue(String.class);
                                                userType = "Client";
                                            }

                                            String[] lastMessageData = extractLastMessage(chatSnapshot);

                                            ChatListModel chatModel = new ChatListModel(
                                                    userId,
                                                    fullName != null ? fullName : "Unknown User",
                                                    userType,
                                                    lastMessageData[0],
                                                    Long.parseLong(lastMessageData[1]),
                                                    profileImageUrl
                                            );

                                            addToChatListIfNotExists(chatModel);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.w(TAG, "User details loading failed for: " + userId, error.toException());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "User details loading failed for: " + userId, error.toException());
                    }
                });
    }

    private void addToChatListIfNotExists(ChatListModel chatModel) {
        boolean exists = false;
        for (ChatListModel existing : chatList) {
            if (existing.getOtherUserId().equals(chatModel.getOtherUserId())) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            chatList.add(chatModel);
            sortAndUpdateChatList();
        }
    }

    private String[] extractLastMessage(DataSnapshot chatSnapshot) {
        String lastMessage = "No messages yet";
        long lastTimestamp = 0;

        try {
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
        } catch (Exception e) {
            Log.w(TAG, "Error extracting last message", e);
        }

        return new String[]{lastMessage, String.valueOf(lastTimestamp)};
    }

    private void sortAndUpdateChatList() {
        try {
            Collections.sort(chatList, new Comparator<ChatListModel>() {
                @Override
                public int compare(ChatListModel o1, ChatListModel o2) {
                    return Long.compare(o2.getLastMessageTime(), o1.getLastMessageTime());
                }
            });

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (chatListAdapter != null) {
                        chatListAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Chat list updated with " + chatList.size() + " chats");
                    }
                    updateEmptyState();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sorting chat list", e);
        }
    }

    private void updateEmptyState() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            boolean isEmpty = chatList.isEmpty();

            if (layoutEmpty != null) {
                layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            if (recyclerViewChatList != null) {
                recyclerViewChatList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }

            Log.d(TAG, "Empty state updated: " + (isEmpty ? "VISIBLE" : "HIDDEN"));
        });
    }

    private void showErrorState(String message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (layoutEmpty != null) {
                layoutEmpty.setVisibility(View.VISIBLE);
                if (textViewEmpty != null) {
                    textViewEmpty.setText(message);
                }
            }

            if (recyclerViewChatList != null) {
                recyclerViewChatList.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed");

        if (currentUserId != null && chatListAdapter != null) {
            loadChatList();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (chatListListener != null && databaseReference != null) {
            databaseReference.child("Chats").removeEventListener(chatListListener);
        }

        Log.d(TAG, "Fragment destroyed");
    }
}