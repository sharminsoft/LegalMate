package com.yourname.legalmate.Chat;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private String senderName;
    private String senderType;
    private String senderImageUrl;

    // Default constructor for Firebase
    public MessageModel() {}

    // Constructor with all parameters
    public MessageModel(String messageId, String senderId, String receiverId,
                        String message, long timestamp, String senderName,
                        String senderType, String senderImageUrl) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.senderName = senderName;
        this.senderType = senderType;
        this.senderImageUrl = senderImageUrl;
    }

    // Constructor for backward compatibility
    public MessageModel(String messageId, String senderId, String receiverId,
                        String message, long timestamp, String senderName) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.senderName = senderName;
        this.senderType = "User";
        this.senderImageUrl = null;
    }

    // Getters
    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderType() {
        return senderType;
    }

    public String getSenderImageUrl() {
        return senderImageUrl;
    }

    // Setters
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public void setSenderImageUrl(String senderImageUrl) {
        this.senderImageUrl = senderImageUrl;
    }

    @Override
    public String toString() {
        return "MessageModel{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", senderName='" + senderName + '\'' +
                ", senderType='" + senderType + '\'' +
                ", senderImageUrl='" + senderImageUrl + '\'' +
                '}';
    }
}