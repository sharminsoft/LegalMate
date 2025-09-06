package com.yourname.legalmate.GeneralPersonPortal.Models;

import java.util.List;

public class LawyerModel {
    // Basic Profile
    private String lawyerId;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String shortBio;
    private String preferredLanguage;

    // Contact & Location
    private String mobileNumber;
    private String emailAddress;
    private String officeAddress;
    private List<String> workingDays;
    private String workingStartTime;
    private String workingEndTime;

    // Professional Information
    private List<String> practiceAreas;
    private String experience;
    private String chamberName;
    private String barRegistrationNumber;
    private String enrollmentYear;
    private String llbInstitution;
    private String llbYear;

    // Consultation & Fees
    private String feeType;
    private String fixedFee;
    private String minFee;
    private String maxFee;
    private boolean isAvailable;
    private List<String> consultationTypes;

    // Documents
    private String profileImageUrl;
    private String idCardUrl;
    private String barCertificateUrl;

    // Profile Status
    private String profileStatus;
    private boolean isVerified;
    private boolean isActive;

    // Appointment Settings
    private boolean appointmentBookingEnabled;

    // Additional Fields for UI
    private double rating;
    private int reviewCount;
    private boolean isFavorite;

    // Constructors
    public LawyerModel() {
        // Default constructor required for Firebase
    }


    // Basic Profile Getters and Setters
    public String getLawyerId() { return lawyerId; }
    public void setLawyerId(String lawyerId) { this.lawyerId = lawyerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getShortBio() { return shortBio; }
    public void setShortBio(String shortBio) { this.shortBio = shortBio; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    // Contact & Location Getters and Setters
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getOfficeAddress() { return officeAddress; }
    public void setOfficeAddress(String officeAddress) { this.officeAddress = officeAddress; }

    public List<String> getWorkingDays() { return workingDays; }
    public void setWorkingDays(List<String> workingDays) { this.workingDays = workingDays; }

    public String getWorkingStartTime() { return workingStartTime; }
    public void setWorkingStartTime(String workingStartTime) { this.workingStartTime = workingStartTime; }

    public String getWorkingEndTime() { return workingEndTime; }
    public void setWorkingEndTime(String workingEndTime) { this.workingEndTime = workingEndTime; }

    // Professional Information Getters and Setters
    public List<String> getPracticeAreas() { return practiceAreas; }
    public void setPracticeAreas(List<String> practiceAreas) { this.practiceAreas = practiceAreas; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getChamberName() { return chamberName; }
    public void setChamberName(String chamberName) { this.chamberName = chamberName; }

    public String getBarRegistrationNumber() { return barRegistrationNumber; }
    public void setBarRegistrationNumber(String barRegistrationNumber) { this.barRegistrationNumber = barRegistrationNumber; }

    public String getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(String enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public String getLlbInstitution() { return llbInstitution; }
    public void setLlbInstitution(String llbInstitution) { this.llbInstitution = llbInstitution; }

    public String getLlbYear() { return llbYear; }
    public void setLlbYear(String llbYear) { this.llbYear = llbYear; }

    // Consultation & Fees Getters and Setters
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }

    public String getFixedFee() { return fixedFee; }
    public void setFixedFee(String fixedFee) { this.fixedFee = fixedFee; }

    public String getMinFee() { return minFee; }
    public void setMinFee(String minFee) { this.minFee = minFee; }

    public String getMaxFee() { return maxFee; }
    public void setMaxFee(String maxFee) { this.maxFee = maxFee; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public List<String> getConsultationTypes() { return consultationTypes; }
    public void setConsultationTypes(List<String> consultationTypes) { this.consultationTypes = consultationTypes; }

    // Documents Getters and Setters
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getIdCardUrl() { return idCardUrl; }
    public void setIdCardUrl(String idCardUrl) { this.idCardUrl = idCardUrl; }

    public String getBarCertificateUrl() { return barCertificateUrl; }
    public void setBarCertificateUrl(String barCertificateUrl) { this.barCertificateUrl = barCertificateUrl; }

    // Profile Status Getters and Setters
    public String getProfileStatus() { return profileStatus; }
    public void setProfileStatus(String profileStatus) { this.profileStatus = profileStatus; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Appointment Settings Getters and Setters
    public boolean isAppointmentBookingEnabled() { return appointmentBookingEnabled; }
    public void setAppointmentBookingEnabled(boolean appointmentBookingEnabled) { this.appointmentBookingEnabled = appointmentBookingEnabled; }

    // Additional Fields Getters and Setters
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    // Utility Methods
    public boolean isProfileComplete() {
        return fullName != null && !fullName.isEmpty() &&
                profileImageUrl != null && !profileImageUrl.isEmpty() &&
                practiceAreas != null && !practiceAreas.isEmpty() &&
                experience != null && !experience.isEmpty();
    }

    public String getDisplayName() {
        return fullName != null ? "Advocate " + fullName : "Unknown Lawyer";
    }

    public String getPracticeAreasString() {
        if (practiceAreas == null || practiceAreas.isEmpty()) {
            return "General Practice";
        }
        return String.join(" • ", practiceAreas);
    }

    public String getExperienceString() {
        if (experience == null || experience.isEmpty()) {
            return "Experience not specified";
        }
        return experience + " years exp";
    }

    public String getLocationString() {
        if (officeAddress == null || officeAddress.isEmpty()) {
            return "Location not specified";
        }
        return officeAddress;
    }

    public String getConsultationFeeString() {
        if ("fixed".equals(feeType) && fixedFee != null) {
            return "৳" + fixedFee + "/consultation";
        } else if ("ranged".equals(feeType) && minFee != null && maxFee != null) {
            return "৳" + minFee + "-" + maxFee + "/consultation";
        }
        return "Fee not specified";
    }

    public String getAvailabilityStatus() {
        return isAvailable ? "Available Now" : "Not Available";
    }

    public String getRatingString() {
        return String.format("%.1f", rating);
    }

    public String getReviewCountString() {
        return "(" + reviewCount + " reviews)";
    }

    @Override
    public String toString() {
        return "LawyerModel{" +
                "lawyerId='" + lawyerId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", practiceAreas=" + practiceAreas +
                ", experience='" + experience + '\'' +
                ", rating=" + rating +
                '}';
    }
}