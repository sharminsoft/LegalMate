package com.yourname.legalmate.LawyerPortal.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.legalmate.LawyerPortal.Models.AppointmentSchedule;
import com.yourname.legalmate.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpcomingAppointmentsAdapter extends RecyclerView.Adapter<UpcomingAppointmentsAdapter.ViewHolder> {

    private List<AppointmentSchedule> appointmentSchedules;
    private OnAppointmentClickListener clickListener;
    private Map<String, String> clientNamesCache;

    // Date formatters
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    private SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public interface OnAppointmentClickListener {
        void onAppointmentClick(AppointmentSchedule appointment);
    }

    public UpcomingAppointmentsAdapter(List<AppointmentSchedule> appointmentSchedules,
                                       OnAppointmentClickListener clickListener,
                                       Map<String, String> clientNamesCache) {
        this.appointmentSchedules = appointmentSchedules;
        this.clickListener = clickListener;
        this.clientNamesCache = clientNamesCache;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppointmentSchedule appointment = appointmentSchedules.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointmentSchedules != null ? appointmentSchedules.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Date circle views
        private TextView tvAppointmentDay;
        private TextView tvAppointmentMonth;

        // Header section views
        private TextView tvCaseTitle;
        private TextView tvAppointmentTime;

        // Bottom section views
        private TextView tvClientName;
        private TextView tvAppointmentType;
        private TextView tvDescription;
        private TextView tvAppointmentStatus;

        // Status views
        private View statusIndicator;
        private TextView tvStatusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize date circle views
            tvAppointmentDay = itemView.findViewById(R.id.tvAppointmentDay);
            tvAppointmentMonth = itemView.findViewById(R.id.tvAppointmentMonth);

            // Initialize header views
            tvCaseTitle = itemView.findViewById(R.id.tvCaseTitle);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);

            // Initialize bottom section views
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvAppointmentType = itemView.findViewById(R.id.tvAppointmentType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAppointmentStatus = itemView.findViewById(R.id.tvAppointmentStatus);

            // Initialize status views
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onAppointmentClick(appointmentSchedules.get(getAdapterPosition()));
                }
            });
        }

        public void bind(AppointmentSchedule appointment) {
            // Set case title (or consultation type)
            tvCaseTitle.setText(appointment.getCaseTitle());

            // Set client name from cache or appointment object
            String clientName = getClientDisplayName(appointment);
            tvClientName.setText("Client: " + clientName);

            // Set appointment type
            tvAppointmentType.setText("Consultation Meeting");

            // Format and set appointment date for the circle
            if (appointment.getAppointmentDate() != null && !appointment.getAppointmentDate().isEmpty()) {
                try {
                    Date date = inputDateFormat.parse(appointment.getAppointmentDate());
                    if (date != null) {
                        tvAppointmentDay.setText(dayFormat.format(date));
                        tvAppointmentMonth.setText(monthFormat.format(date).toUpperCase());
                    } else {
                        tvAppointmentDay.setText("--");
                        tvAppointmentMonth.setText("---");
                    }
                } catch (ParseException e) {
                    tvAppointmentDay.setText("--");
                    tvAppointmentMonth.setText("---");
                }
            } else {
                tvAppointmentDay.setText("--");
                tvAppointmentMonth.setText("---");
            }

            // Format and set appointment time
            if (appointment.getAppointmentTime() != null && !appointment.getAppointmentTime().isEmpty()) {
                try {
                    Date time = inputTimeFormat.parse(appointment.getAppointmentTime());
                    if (time != null) {
                        tvAppointmentTime.setText(outputTimeFormat.format(time));
                    } else {
                        tvAppointmentTime.setText(appointment.getAppointmentTime());
                    }
                } catch (ParseException e) {
                    tvAppointmentTime.setText(appointment.getAppointmentTime());
                }
            } else {
                tvAppointmentTime.setText("No time set");
            }

            // Set description
            String description = appointment.getDescription();
            if (description != null && !description.isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(description.length() > 100 ?
                        description.substring(0, 100) + "..." : description);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Set appointment status
            tvAppointmentStatus.setText("Status: " + appointment.getStatus().toUpperCase());

            // Set status indicator based on date
            setStatusBasedOnDate(appointment);
        }

        private String getClientDisplayName(AppointmentSchedule appointment) {
            // First try to get from the appointment object itself
            if (appointment.getClientName() != null &&
                    !appointment.getClientName().equals("Unknown Client")) {
                return appointment.getClientName();
            }

            // Then try to get from cache
            if (clientNamesCache != null && appointment.getClientId() != null &&
                    clientNamesCache.containsKey(appointment.getClientId())) {
                return clientNamesCache.get(appointment.getClientId());
            }

            // Fallback
            return "Unknown Client";
        }

        private void setStatusBasedOnDate(AppointmentSchedule appointment) {
            if (appointment.getAppointmentDate() != null && !appointment.getAppointmentDate().isEmpty()) {
                try {
                    Date appointmentDate = inputDateFormat.parse(appointment.getAppointmentDate());
                    Date currentDate = new Date();

                    if (appointmentDate != null) {
                        // Remove time component for date comparison
                        SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String appointmentDateStr = dateOnly.format(appointmentDate);
                        String currentDateStr = dateOnly.format(currentDate);

                        Date appointmentDateOnly = dateOnly.parse(appointmentDateStr);
                        Date currentDateOnly = dateOnly.parse(currentDateStr);

                        if (appointmentDateOnly.equals(currentDateOnly)) {
                            // Today
                            tvStatusText.setText("Today");
                            statusIndicator.setBackgroundResource(R.drawable.circle_orange);
                        } else if (appointmentDateOnly.after(currentDateOnly)) {
                            // Upcoming
                            tvStatusText.setText("Upcoming");
                            statusIndicator.setBackgroundResource(R.drawable.circle_green);
                        } else {
                            // Past
                            tvStatusText.setText("Past");
                            statusIndicator.setBackgroundResource(R.drawable.circle_red);
                        }
                    } else {
                        tvStatusText.setText("Pending");
                        statusIndicator.setBackgroundResource(R.drawable.circle_green);
                    }
                } catch (ParseException e) {
                    tvStatusText.setText("Unknown");
                    statusIndicator.setBackgroundResource(R.drawable.circle_green);
                }
            } else {
                tvStatusText.setText("No Date");
                statusIndicator.setBackgroundResource(R.drawable.circle_red);
            }
        }
    }

    // Method to update the data
    public void updateData(List<AppointmentSchedule> newAppointments) {
        this.appointmentSchedules = newAppointments;
        notifyDataSetChanged();
    }

    // Method to update client names cache
    public void updateClientNamesCache(Map<String, String> newCache) {
        if (newCache != null) {
            this.clientNamesCache = newCache;
            notifyDataSetChanged();
        }
    }
}