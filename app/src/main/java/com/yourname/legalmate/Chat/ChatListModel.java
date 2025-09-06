package com.yourname.legalmate.Chat;

public class ChatListModel {
    private String otherUserId;
    private String otherUserName;
    private String userType;
    private String lastMessage;
    private long lastMessageTime;
    private String otherUserImageUrl;

    public ChatListModel() {
        // Empty constructor for Firebase
    }

    public ChatListModel(String otherUserId, String otherUserName, String userType,
                         String lastMessage, long lastMessageTime) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.userType = userType;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public ChatListModel(String otherUserId, String otherUserName, String userType,
                         String lastMessage, long lastMessageTime, String otherUserImageUrl) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.userType = userType;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.otherUserImageUrl = otherUserImageUrl;
    }

    // Getters
    public String getOtherUserId() {
        return otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public String getUserType() {
        return userType;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public String getOtherUserImageUrl() {
        return otherUserImageUrl;
    }

    // Setters
    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public void setOtherUserImageUrl(String otherUserImageUrl) {
        this.otherUserImageUrl = otherUserImageUrl;
    }

    @Override
    public String toString() {
        return "ChatListModel{" +
                "otherUserId='" + otherUserId + '\'' +
                ", otherUserName='" + otherUserName + '\'' +
                ", userType='" + userType + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", lastMessageTime=" + lastMessageTime +
                ", otherUserImageUrl='" + otherUserImageUrl + '\'' +
                '}';
    }
}