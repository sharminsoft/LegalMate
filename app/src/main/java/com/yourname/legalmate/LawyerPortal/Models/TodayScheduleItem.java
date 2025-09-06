package com.yourname.legalmate.LawyerPortal.Models;

public class TodayScheduleItem {

    // Schedule types
    public static final String TYPE_COURT_CASE = "court_case";
    public static final String TYPE_APPOINTMENT = "appointment";

    private String type;
    private String title;
    private String subtitle;
    private String time;
    private String date;
    private String clientId;
    private String caseId;

    // Constructors
    public TodayScheduleItem() {}

    public TodayScheduleItem(String type, String title, String subtitle, String time, String date) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.time = time;
        this.date = date;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    // Utility methods
    public boolean isCourtCase() {
        return TYPE_COURT_CASE.equals(type);
    }

    public boolean isAppointment() {
        return TYPE_APPOINTMENT.equals(type);
    }

    @Override
    public String toString() {
        return "TodayScheduleItem{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", time='" + time + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}