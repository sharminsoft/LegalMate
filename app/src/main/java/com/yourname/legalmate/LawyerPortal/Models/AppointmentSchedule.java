package com.yourname.legalmate.LawyerPortal.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentSchedule {

    private String appointmentId;
    private String caseTitle;
    private String clientId;
    private String clientName;
    private String appointmentDate;
    private String appointmentTime;
    private String description;
    private String status;

    // Date formatters for display purposes
    private static final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // Constructors
    public AppointmentSchedule() {}

    public AppointmentSchedule(String appointmentId, String caseTitle, String clientId,
                               String appointmentDate, String appointmentTime) {
        this.appointmentId = appointmentId;
        this.caseTitle = caseTitle;
        this.clientId = clientId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
    }

    // Getters and Setters
    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getCaseTitle() {
        return caseTitle != null ? caseTitle : "Consultation";
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName != null ? clientName : "Unknown Client";
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime != null ? appointmentTime : "";
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status != null ? status : "approved";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Utility methods for display
    public String getDisplayDate() {
        if (appointmentDate != null && !appointmentDate.isEmpty()) {
            try {
                Date date = inputDateFormat.parse(appointmentDate);
                if (date != null) {
                    return displayDateFormat.format(date);
                }
            } catch (Exception e) {
                // Return original date if parsing fails
            }
        }
        return appointmentDate != null ? appointmentDate : "No date set";
    }

    public String getDisplayTime() {
        if (appointmentTime != null && !appointmentTime.isEmpty()) {
            try {
                Date time = inputTimeFormat.parse(appointmentTime);
                if (time != null) {
                    return displayTimeFormat.format(time);
                }
            } catch (Exception e) {
                // Return original time if parsing fails
            }
        }
        return appointmentTime != null ? appointmentTime : "No time set";
    }

    public String getDisplayDateTime() {
        return getDisplayDate() + " at " + getDisplayTime();
    }

    // Check if appointment is today
    public boolean isToday() {
        if (appointmentDate != null && !appointmentDate.isEmpty()) {
            try {
                Date appointmentDateObj = inputDateFormat.parse(appointmentDate);
                Date today = new Date();

                SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String appointmentDateStr = dateOnly.format(appointmentDateObj);
                String todayStr = dateOnly.format(today);

                return appointmentDateStr.equals(todayStr);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // Check if appointment is in the past
    public boolean isPast() {
        if (appointmentDate != null && !appointmentDate.isEmpty()) {
            try {
                Date appointmentDateObj = inputDateFormat.parse(appointmentDate);
                Date today = new Date();

                SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date appointmentDateOnly = dateOnly.parse(dateOnly.format(appointmentDateObj));
                Date todayOnly = dateOnly.parse(dateOnly.format(today));

                return appointmentDateOnly.before(todayOnly);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // Get status for display
    public String getDisplayStatus() {
        if (isToday()) {
            return "Today";
        } else if (isPast()) {
            return "Past";
        } else {
            return "Upcoming";
        }
    }

    // Get status color indicator
    public String getStatusColor() {
        if (isToday()) {
            return "orange"; // Today - Orange
        } else if (isPast()) {
            return "red"; // Past - Red
        } else {
            return "green"; // Upcoming - Green
        }
    }

    @Override
    public String toString() {
        return "AppointmentSchedule{" +
                "appointmentId='" + appointmentId + '\'' +
                ", caseTitle='" + caseTitle + '\'' +
                ", clientName='" + clientName + '\'' +
                ", appointmentDate='" + appointmentDate + '\'' +
                ", appointmentTime='" + appointmentTime + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}