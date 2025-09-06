package com.yourname.legalmate.GeneralPersonPortal.Models;

import java.util.List;

public class SuggestedLawyerModel {
    private String lawyerId;
    private String fullName;
    private String profileImageUrl;
    private List<String> practiceAreas;
    private double rating;
    private int reviewCount;
    private String experience;
    private String chamberName;
    private boolean isVerified;
    private boolean isActive;
    private boolean isAvailable;
    private String profileStatus;

    // Default constructor required for Firestore
    public SuggestedLawyerModel() {
        this.rating = 0.0;
        this.reviewCount = 0;
        this.isVerified = false;
        this.isActive = false;
        this.isAvailable = false;
    }

    // Constructor with basic parameters
    public SuggestedLawyerModel(String lawyerId, String fullName) {
        this();
        this.lawyerId = lawyerId;
        this.fullName = fullName;
    }

    // Getters and Setters
    public String getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(String lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public List<String> getPracticeAreas() {
        return practiceAreas;
    }

    public void setPracticeAreas(List<String> practiceAreas) {
        this.practiceAreas = practiceAreas;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getChamberName() {
        return chamberName;
    }

    public void setChamberName(String chamberName) {
        this.chamberName = chamberName;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getProfileStatus() {
        return profileStatus;
    }

    public void setProfileStatus(String profileStatus) {
        this.profileStatus = profileStatus;
    }

    // Helper methods
    public boolean isProfileComplete() {
        return fullName != null && !fullName.trim().isEmpty() &&
                practiceAreas != null && !practiceAreas.isEmpty();
    }

    public String getFormattedDesignation() {
        if (practiceAreas != null && !practiceAreas.isEmpty()) {
            String designation = practiceAreas.get(0);
            if (designation != null && !designation.trim().isEmpty()) {
                if (practiceAreas.size() > 1) {
                    designation += " & More";
                }
                return designation;
            }
        }
        return "Legal Practitioner";
    }

    public String getFormattedRating() {
        if (rating <= 0) {
            return "0.0";
        }
        return String.format("%.1f", rating);
    }

    public String getFormattedReviewText() {
        return "(" + reviewCount + " review" + (reviewCount != 1 ? "s" : "") + ")";
    }

    public String getSafeFullName() {
        return (fullName != null && !fullName.trim().isEmpty()) ? fullName : "Unknown Lawyer";
    }

    public String getSafeProfileImageUrl() {
        return (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) ? profileImageUrl : "";
    }

    public boolean hasValidProfileImage() {
        return profileImageUrl != null &&
                !profileImageUrl.trim().isEmpty() &&
                (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://"));
    }

    public boolean isQualifiedForSuggestion() {
        return isProfileComplete() &&
                isVerified &&
                isActive &&
                rating > 0.0;
    }

    @Override
    public String toString() {
        return "SuggestedLawyerModel{" +
                "lawyerId='" + lawyerId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", rating=" + rating +
                ", reviewCount=" + reviewCount +
                ", isVerified=" + isVerified +
                ", isActive=" + isActive +
                '}';
    }
}