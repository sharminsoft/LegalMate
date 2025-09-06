package com.yourname.legalmate.LawyerPortal.Models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DocumentTemplate {
    private String id;
    private String type;
    private String title;
    private String titleBangla;
    private Date createdAt;
    private Date updatedAt;
    private Map<String, Object> formData;
    private String language;
    private String status;
    private String createdBy; // User ID who created this document
    private String lastModifiedBy; // User ID who last modified this document
    private int version; // Document version for tracking changes

    public DocumentTemplate() {
        this.formData = new HashMap<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.language = "en";
        this.status = "draft";
        this.version = 1;
    }

    public DocumentTemplate(String type, String title, String titleBangla) {
        this();
        this.type = type;
        this.title = title;
        this.titleBangla = titleBangla;
        this.id = type + "_" + System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getTitleBangla() {
        return titleBangla;
    }

    public void setTitleBangla(String titleBangla) {
        this.titleBangla = titleBangla;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getFormData() {
        if (formData == null) {
            formData = new HashMap<>();
        }
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status != null ? status : "draft";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    // Helper methods
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isDraft() {
        return "draft".equalsIgnoreCase(status);
    }

    public void markAsCompleted() {
        this.status = "completed";
        this.updatedAt = new Date();
    }

    public void markAsDraft() {
        this.status = "draft";
        this.updatedAt = new Date();
    }

    public String getDisplayTitle(String language) {
        if ("bn".equals(language) && titleBangla != null && !titleBangla.trim().isEmpty()) {
            return titleBangla;
        }
        return title != null ? title : "Untitled Document";
    }

    public void addFormField(String key, Object value) {
        if (formData == null) {
            formData = new HashMap<>();
        }
        formData.put(key, value);
        this.updatedAt = new Date();
    }

    public Object getFormField(String key) {
        if (formData == null) {
            return null;
        }
        return formData.get(key);
    }

    public String getFormFieldAsString(String key) {
        Object value = getFormField(key);
        return value != null ? value.toString() : "";
    }

    public boolean hasFormField(String key) {
        return formData != null && formData.containsKey(key) && formData.get(key) != null;
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = new Date();
    }

    @Override
    public String toString() {
        return "DocumentTemplate{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}