package com.yourname.legalmate.LawyerPortal.Models;

public class AttachedDocument {
    private String name;
    private String uri;
    private String status;
    private long fileSize;
    private String cloudinaryUrl;
    private String cloudinaryPublicId;
    private String uploadDate;
    private String fileExtension;
    private String mimeType;

    // Constructors
    public AttachedDocument() {
        // Default constructor required for Firebase
    }

    public AttachedDocument(String name, String uri, String status, long fileSize) {
        this.name = name;
        this.uri = uri;
        this.status = status;
        this.fileSize = fileSize;
        this.fileExtension = extractFileExtension(name);
    }

    // Helper method to extract file extension
    private String extractFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.fileExtension = extractFileExtension(name);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    // Add compatibility methods for backward compatibility
    public long getSize() {
        return fileSize;
    }

    public void setSize(long size) {
        this.fileSize = size;
    }

    public String getCloudinaryUrl() {
        return cloudinaryUrl;
    }

    public void setCloudinaryUrl(String cloudinaryUrl) {
        this.cloudinaryUrl = cloudinaryUrl;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public void setCloudinaryPublicId(String cloudinaryPublicId) {
        this.cloudinaryPublicId = cloudinaryPublicId;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    // Helper methods
    public boolean isPDF() {
        return "pdf".equalsIgnoreCase(fileExtension);
    }

    public boolean isImage() {
        return fileExtension != null && (
                fileExtension.equalsIgnoreCase("jpg") ||
                        fileExtension.equalsIgnoreCase("jpeg") ||
                        fileExtension.equalsIgnoreCase("png") ||
                        fileExtension.equalsIgnoreCase("gif") ||
                        fileExtension.equalsIgnoreCase("bmp") ||
                        fileExtension.equalsIgnoreCase("webp")
        );
    }

    public String getViewingUrl() {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return uri; // Fallback to original URI
        }

        if (isPDF()) {
            // For PDFs, return URL that forces inline display
            return cloudinaryUrl.replace("/upload/", "/upload/fl_attachment:inline/");
        }

        return cloudinaryUrl;
    }

    public String getDownloadUrl() {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return uri;
        }

        // Force download
        return cloudinaryUrl.replace("/upload/", "/upload/fl_attachment/");
    }

    public String getPreviewUrl(String cloudName) {
        if (cloudinaryPublicId == null || cloudinaryPublicId.isEmpty() || cloudName == null) {
            return cloudinaryUrl;
        }

        if (isPDF()) {
            // Generate preview of first page as image
            return "https://res.cloudinary.com/" + cloudName + "/image/upload/f_jpg,pg_1,w_300,h_400,c_fit/" + cloudinaryPublicId + ".jpg";
        } else if (isImage()) {
            // For images, return thumbnail
            return cloudinaryUrl.replace("/upload/", "/upload/w_300,h_400,c_fit/");
        }

        return cloudinaryUrl;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        return "AttachedDocument{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", fileSize=" + fileSize +
                ", fileExtension='" + fileExtension + '\'' +
                ", cloudinaryUrl='" + cloudinaryUrl + '\'' +
                '}';
    }
}