package com.yourname.legalmate.LawyerPortal.Models;

import java.io.Serializable;

public class CourtSchedule implements Serializable {
    private String caseId;
    private String caseTitle;
    private String clientName;
    private String caseType;
    private String courtDate;
    private String courtTime;
    private String courtName;
    private String courtLocation;
    private String judgeName;
    private String hearingType;
    private String reminderSettings;

    // Default constructor
    public CourtSchedule() {
    }

    // Constructor with all fields
    public CourtSchedule(String caseId, String caseTitle, String clientName, String caseType,
                         String courtDate, String courtTime, String courtName, String courtLocation,
                         String judgeName, String hearingType, String reminderSettings) {
        this.caseId = caseId;
        this.caseTitle = caseTitle;
        this.clientName = clientName;
        this.caseType = caseType;
        this.courtDate = courtDate;
        this.courtTime = courtTime;
        this.courtName = courtName;
        this.courtLocation = courtLocation;
        this.judgeName = judgeName;
        this.hearingType = hearingType;
        this.reminderSettings = reminderSettings;
    }

    // Getters
    public String getCaseId() {
        return caseId;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public String getClientName() {
        return clientName;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getCourtDate() {
        return courtDate;
    }

    public String getCourtTime() {
        return courtTime;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getCourtLocation() {
        return courtLocation;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public String getHearingType() {
        return hearingType;
    }

    public String getReminderSettings() {
        return reminderSettings;
    }

    // Setters
    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public void setCourtDate(String courtDate) {
        this.courtDate = courtDate;
    }

    public void setCourtTime(String courtTime) {
        this.courtTime = courtTime;
    }

    public void setCourtName(String courtName) {
        this.courtName = courtName;
    }

    public void setCourtLocation(String courtLocation) {
        this.courtLocation = courtLocation;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
    }

    public void setHearingType(String hearingType) {
        this.hearingType = hearingType;
    }

    public void setReminderSettings(String reminderSettings) {
        this.reminderSettings = reminderSettings;
    }

    // Helper method to get formatted display text
    public String getDisplayTitle() {
        return caseTitle + " - " + clientName;
    }

    public String getDisplayCourtInfo() {
        StringBuilder courtInfo = new StringBuilder();
        if (courtName != null && !courtName.isEmpty()) {
            courtInfo.append(courtName);
        }
        if (courtLocation != null && !courtLocation.isEmpty()) {
            if (courtInfo.length() > 0) {
                courtInfo.append(", ");
            }
            courtInfo.append(courtLocation);
        }
        return courtInfo.toString();
    }

    public String getDisplayJudgeInfo() {
        return judgeName != null && !judgeName.isEmpty() ? "Hon'ble " + judgeName : "";
    }

    @Override
    public String toString() {
        return "CourtSchedule{" +
                "caseId='" + caseId + '\'' +
                ", caseTitle='" + caseTitle + '\'' +
                ", clientName='" + clientName + '\'' +
                ", caseType='" + caseType + '\'' +
                ", courtDate='" + courtDate + '\'' +
                ", courtTime='" + courtTime + '\'' +
                ", courtName='" + courtName + '\'' +
                ", courtLocation='" + courtLocation + '\'' +
                ", judgeName='" + judgeName + '\'' +
                ", hearingType='" + hearingType + '\'' +
                ", reminderSettings='" + reminderSettings + '\'' +
                '}';
    }
}