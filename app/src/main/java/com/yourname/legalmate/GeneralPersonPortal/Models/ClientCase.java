package com.yourname.legalmate.GeneralPersonPortal.Models;

public class ClientCase {
    private String caseId;
    private String layerId;
    private String caseTitle;
    private String caseType;
    private String caseStatus;
    private String caseDescription;
    private String clientName;
    private String courtName;
    private String caseNumber;
    private String filingDate;
    private String judgeName;

    // Court Date & Reminder
    private String courtDate;
    private String courtTime;
    private String hearingType;

    // Status & Tracking
    private String caseStage;
    private String expectedOutcome;

    // Lawyer Information
    private String lawyerName;
    private String lawyerPhone;
    private String lawyerEmail;
    private String lawyerImageUrl;
    private String lawyerSpecialization;
    private String lawyerExperience;

    // Empty constructor required for Firestore
    public ClientCase() {}

    public ClientCase(String caseId, String layerId, String caseTitle, String caseType,
                      String caseStatus, String clientName, String lawyerName) {
        this.caseId = caseId;
        this.layerId = layerId;
        this.caseTitle = caseTitle;
        this.caseType = caseType;
        this.caseStatus = caseStatus;
        this.clientName = clientName;
        this.lawyerName = lawyerName;
    }

    // Getters and Setters
    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getCaseDescription() {
        return caseDescription;
    }

    public void setCaseDescription(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCourtName() {
        return courtName;
    }

    public void setCourtName(String courtName) {
        this.courtName = courtName;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getFilingDate() {
        return filingDate;
    }

    public void setFilingDate(String filingDate) {
        this.filingDate = filingDate;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
    }

    public String getCourtDate() {
        return courtDate;
    }

    public void setCourtDate(String courtDate) {
        this.courtDate = courtDate;
    }

    public String getCourtTime() {
        return courtTime;
    }

    public void setCourtTime(String courtTime) {
        this.courtTime = courtTime;
    }

    public String getHearingType() {
        return hearingType;
    }

    public void setHearingType(String hearingType) {
        this.hearingType = hearingType;
    }

    public String getCaseStage() {
        return caseStage;
    }

    public void setCaseStage(String caseStage) {
        this.caseStage = caseStage;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public String getLawyerPhone() {
        return lawyerPhone;
    }

    public void setLawyerPhone(String lawyerPhone) {
        this.lawyerPhone = lawyerPhone;
    }

    public String getLawyerEmail() {
        return lawyerEmail;
    }

    public void setLawyerEmail(String lawyerEmail) {
        this.lawyerEmail = lawyerEmail;
    }

    public String getLawyerImageUrl() {
        return lawyerImageUrl;
    }

    public void setLawyerImageUrl(String lawyerImageUrl) {
        this.lawyerImageUrl = lawyerImageUrl;
    }

    public String getLawyerSpecialization() {
        return lawyerSpecialization;
    }

    public void setLawyerSpecialization(String lawyerSpecialization) {
        this.lawyerSpecialization = lawyerSpecialization;
    }

    public String getLawyerExperience() {
        return lawyerExperience;
    }

    public void setLawyerExperience(String lawyerExperience) {
        this.lawyerExperience = lawyerExperience;
    }
}