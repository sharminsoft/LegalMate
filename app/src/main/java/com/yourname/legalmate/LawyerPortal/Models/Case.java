package com.yourname.legalmate.LawyerPortal.Models;

public class Case {
    private String caseId;
    private String clientName;
    private String caseStatus;
    private String caseTitle;
    private String caseDescription;
    private String dateCreated;

    // Empty constructor required for Firestore
    public Case() {}

    public Case(String caseId, String clientName, String caseStatus, String caseTitle, String caseDescription, String dateCreated) {
        this.caseId = caseId;
        this.clientName = clientName;
        this.caseStatus = caseStatus;
        this.caseTitle = caseTitle;
        this.caseDescription = caseDescription;
        this.dateCreated = dateCreated;
    }

    // Getters and Setters
    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseDescription() {
        return caseDescription;
    }

    public void setCaseDescription(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }
}