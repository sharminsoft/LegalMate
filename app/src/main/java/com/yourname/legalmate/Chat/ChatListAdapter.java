package com.yourname.legalmate.Chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private Context context;
    private List<ChatListModel> chatList;
    private OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(ChatListModel chatModel);
    }

    public ChatListAdapter(Context context, List<ChatListModel> chatList,
                           OnChatClickListener onChatClickListener) {
        this.context = context;
        this.chatList = chatList;
        this.onChatClickListener = onChatClickListener;
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        ChatListModel chatModel = chatList.get(position);
        holder.bind(chatModel);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public class ChatListViewHolder extends RecyclerView.ViewHolder {
        TextView userName, lastMessage, messageTime, userType;
        CircleImageView userProfileImage;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.tv_user_name);
            lastMessage = itemView.findViewById(R.id.tv_last_message);
            messageTime = itemView.findViewById(R.id.tv_message_time);
            userType = itemView.findViewById(R.id.tv_user_type);
            userProfileImage = itemView.findViewById(R.id.iv_user_profile);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onChatClickListener != null) {
                    onChatClickListener.onChatClick(chatList.get(position));
                }
            });
        }

        public void bind(ChatListModel chatModel) {
            userName.setText(chatModel.getOtherUserName());
            lastMessage.setText(chatModel.getLastMessage());
            messageTime.setText(formatTime(chatModel.getLastMessageTime()));

            // Set user type with proper formatting
            String userTypeText = chatModel.getUserType();
            if (userTypeText != null) {
                userType.setText(userTypeText.toUpperCase());

                // Set different colors for different user types
                if ("LAWYER".equals(userTypeText.toUpperCase())) {
                    userType.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                } else if ("CLIENT".equals(userTypeText.toUpperCase())) {
                    userType.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    userType.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                }
            } else {
                userType.setText("USER");
                userType.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            }

            // Load profile image
            if (userProfileImage != null) {
                String imageUrl = chatModel.getOtherUserImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_person_ai)
                            .error(R.drawable.ic_person_ai)
                            .circleCrop()
                            .into(userProfileImage);
                } else {
                    userProfileImage.setImageResource(R.drawable.ic_person_ai);
                }
            }
        }
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        Date messageDate = new Date(timestamp);
        Date currentDate = new Date();

        if (isSameDay(messageDate, currentDate)) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(messageDate);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            return sdf.format(messageDate);
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(date1).equals(sdf.format(date2));
    }
}