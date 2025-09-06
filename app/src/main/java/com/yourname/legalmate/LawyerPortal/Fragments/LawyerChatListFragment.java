package com.yourname.legalmate.LawyerPortal.Fragments;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yourname.legalmate.Chat.ChatActivity;
import com.yourname.legalmate.Chat.ChatListAdapter;
import com.yourname.legalmate.Chat.ChatListModel;
import com.yourname.legalmate.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LawyerChatListFragment extends Fragment {

    private static final String TAG = "LawyerChatListFragment";

    private RecyclerView recyclerViewChatList;
    private LinearLayout layoutEmptyState;
    private TextView textViewEmpty;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private ValueEventListener chatListListener;

    private ChatListAdapter chatListAdapter;
    private List<ChatListModel> chatList;
    private String currentLawyerId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lawyer_message, container, false);

        Log.d(TAG, "LawyerChatListFragment created");

        initializeFirebase();
        getCurrentLawyerId();
        initializeViews(view);
        setupRecyclerView();
        loadChatList();

        return view;
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void getCurrentLawyerId() {
        if (firebaseAuth.getCurrentUser() != null) {
            currentLawyerId = firebaseAuth.getCurrentUser().getUid();
            Log.d(TAG, "Current lawyer ID: " + currentLawyerId);
        }
    }

    private void initializeViews(View view) {
        recyclerViewChatList = view.findViewById(R.id.recycler_view_chat_list);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        textViewEmpty = view.findViewById(R.id.text_view_empty);
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getContext(), chatList, this::openChat);

        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChatList.setAdapter(chatListAdapter);
    }

    private void openChat(ChatListModel chatModel) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("client_id", chatModel.getOtherUserId());
        intent.putExtra("client_name", chatModel.getOtherUserName());
        intent.putExtra("client_image", chatModel.getOtherUserImageUrl());
        startActivity(intent);

        Log.d(TAG, "Opening chat with client: " + chatModel.getOtherUserName());
    }

    private void loadChatList() {
        if (currentLawyerId == null) {
            updateEmptyState();
            return;
        }

        loadFromUserChats();
        loadFromChatsDirectory();
    }

    private void loadFromUserChats() {
        Log.d(TAG, "Loading chats from UserChats node");

        databaseReference.child("UserChats").child(currentLawyerId)
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
                                                otherUserName != null ? otherUserName : "Unknown Client",
                                                otherUserType != null ? otherUserType : "Client",
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

                    if (chatId != null && chatId.contains(currentLawyerId)) {
                        try {
                            String clientUserId = extractOtherUserId(chatId, currentLawyerId);

                            if (clientUserId != null && !clientUserId.equals(currentLawyerId)
                                    && !processedUsers.contains(clientUserId)) {

                                processedUsers.add(clientUserId);
                                Log.d(TAG, "Found chat with client: " + clientUserId);

                                getClientDetails(clientUserId, chatSnapshot);
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
                if (textViewEmpty != null) {
                    textViewEmpty.setText("Error loading chats");
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    recyclerViewChatList.setVisibility(View.GONE);
                }
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

    private void getClientDetails(String clientId, DataSnapshot chatSnapshot) {
        // First try GeneralPersons collection
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

                            getLastMessage(chatSnapshot, clientId, clientName, userType, profileImageUrl);
                        } else {
                            // Try Lawyers collection as fallback
                            databaseReference.child("Users").child("Lawyers").child("Lawyers")
                                    .child(clientId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot lawyerSnapshot) {
                                            String fullName = "Unknown User";
                                            String profileImageUrl = null;
                                            String userType = "User";

                                            if (lawyerSnapshot.exists()) {
                                                fullName = lawyerSnapshot.child("fullName").getValue(String.class);
                                                profileImageUrl = lawyerSnapshot.child("profileImageUrl").getValue(String.class);
                                                String specialization = lawyerSnapshot.child("specialization").getValue(String.class);
                                                userType = "Lawyer" + (specialization != null ? " • " + specialization : "");
                                            }

                                            getLastMessage(chatSnapshot, clientId,
                                                    fullName != null ? fullName : "Unknown User",
                                                    userType, profileImageUrl);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.w(TAG, "Lawyer details loading failed", error.toException());
                                            getLastMessage(chatSnapshot, clientId, "Unknown User", "User", null);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        getLastMessage(chatSnapshot, clientId, "Unknown Client", "Client", null);
                    }
                });
    }

    private void getLastMessage(DataSnapshot chatSnapshot, String clientId,
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

        addToChatListIfNotExists(chatModel);
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
            if (chatList.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                recyclerViewChatList.setVisibility(View.GONE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                recyclerViewChatList.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "Empty state updated: " + (chatList.isEmpty() ? "VISIBLE" : "HIDDEN"));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed");

        if (currentLawyerId != null) {
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