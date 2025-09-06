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

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<MessageModel> messageList;
    private String currentUserId;

    public MessageAdapter(Context context, List<MessageModel> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_sent_message);
            timeText = itemView.findViewById(R.id.tv_sent_time);
        }

        public void bind(MessageModel message) {
            if (message.getMessage() != null) {
                messageText.setText(message.getMessage());
            } else {
                messageText.setText("");
            }
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    public class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView senderProfileImage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_received_message);
            timeText = itemView.findViewById(R.id.tv_received_time);
            senderProfileImage = itemView.findViewById(R.id.iv_sender_profile);
        }

        public void bind(MessageModel message) {
            if (message.getMessage() != null) {
                messageText.setText(message.getMessage());
            } else {
                messageText.setText("");
            }

            timeText.setText(formatTime(message.getTimestamp()));


            // Load sender profile image
            if (senderProfileImage != null) {
                String imageUrl = message.getSenderImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_person_ai)
                            .error(R.drawable.ic_person_ai)
                            .circleCrop()
                            .into(senderProfileImage);
                } else {
                    senderProfileImage.setImageResource(R.drawable.ic_person_ai);
                }
            }
        }
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}