package com.yourname.legalmate.GeneralPersonPortal.Models;

import com.google.firebase.Timestamp;

public class AppointmentModel {
    private String appointmentId;
    private String clientId;
    private String lawyerId;
    private String lawyerName;
    private String date;
    private String time;
    private String caseTitle;
    private String description;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default constructor required for Firebase
    public AppointmentModel() {
    }

    // Constructor with parameters
    public AppointmentModel(String appointmentId, String clientId, String lawyerId,
                            String lawyerName, String date, String time, String caseTitle,
                            String description, String status, Timestamp createdAt) {
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.lawyerId = lawyerId;
        this.lawyerName = lawyerName;
        this.date = date;
        this.time = time;
        this.caseTitle = caseTitle;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(String lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper method to get status color
    public int getStatusColor() {
        switch (status.toLowerCase()) {
            case "confirmed":
                return android.R.color.holo_green_dark;
            case "pending":
                return android.R.color.holo_orange_dark;
            case "cancelled":
                return android.R.color.holo_red_dark;
            case "completed":
                return android.R.color.holo_blue_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    // Helper method to get status background color
    public int getStatusBackgroundColor() {
        switch (status.toLowerCase()) {
            case "confirmed":
                return android.R.color.holo_green_light;
            case "pending":
                return android.R.color.holo_orange_light;
            case "cancelled":
                return android.R.color.holo_red_light;
            case "completed":
                return android.R.color.holo_blue_light;
            default:
                return android.R.color.background_light;
        }
    }

    @Override
    public String toString() {
        return "AppointmentModel{" +
                "appointmentId='" + appointmentId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", lawyerId='" + lawyerId + '\'' +
                ", lawyerName='" + lawyerName + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", caseTitle='" + caseTitle + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}