package com.yourname.legalmate.LawyerPortal.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.legalmate.LawyerPortal.Models.CourtSchedule;
import com.yourname.legalmate.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingCasesAdapter extends RecyclerView.Adapter<UpcomingCasesAdapter.ViewHolder> {

    private List<CourtSchedule> courtSchedules;
    private OnScheduleClickListener clickListener;
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    private SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public interface OnScheduleClickListener {
        void onScheduleClick(CourtSchedule schedule);
    }

    public UpcomingCasesAdapter(List<CourtSchedule> courtSchedules, OnScheduleClickListener clickListener) {
        this.courtSchedules = courtSchedules;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_case, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourtSchedule schedule = courtSchedules.get(position);
        holder.bind(schedule);
    }

    @Override
    public int getItemCount() {
        return courtSchedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Date circle views
        private TextView tvCourtDay;
        private TextView tvCourtMonth;

        // Header section views
        private TextView tvCaseTitle;
        private TextView tvCourtTime;

        // Bottom section views
        private TextView tvClientName;
        private TextView tvCaseType;
        private TextView tvCourtName;
        private TextView tvHearingType;

        // Status views
        private View statusIndicator;
        private TextView tvStatusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize date circle views
            tvCourtDay = itemView.findViewById(R.id.tvCourtDay);
            tvCourtMonth = itemView.findViewById(R.id.tvCourtMonth);

            // Initialize header views
            tvCaseTitle = itemView.findViewById(R.id.tvCaseTitle);
            tvCourtTime = itemView.findViewById(R.id.tvCourtTime);

            // Initialize bottom section views
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvCaseType = itemView.findViewById(R.id.tvCaseType);
            tvCourtName = itemView.findViewById(R.id.tvCourtName);
            tvHearingType = itemView.findViewById(R.id.tvHearingType);

            // Initialize status views
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onScheduleClick(courtSchedules.get(getAdapterPosition()));
                }
            });
        }

        public void bind(CourtSchedule schedule) {
            // Set case title
            tvCaseTitle.setText(schedule.getCaseTitle() != null ? schedule.getCaseTitle() : "Untitled Case");

            // Set client name
            tvClientName.setText("Client: " + (schedule.getClientName() != null ? schedule.getClientName() : "Unknown"));

            // Set case type
            tvCaseType.setText(schedule.getCaseType() != null ? schedule.getCaseType() : "General");

            // Format and set court date for the circle
            if (schedule.getCourtDate() != null && !schedule.getCourtDate().isEmpty()) {
                try {
                    Date date = inputDateFormat.parse(schedule.getCourtDate());
                    if (date != null) {
                        tvCourtDay.setText(dayFormat.format(date));
                        tvCourtMonth.setText(monthFormat.format(date).toUpperCase());
                    } else {
                        tvCourtDay.setText("--");
                        tvCourtMonth.setText("---");
                    }
                } catch (ParseException e) {
                    tvCourtDay.setText("--");
                    tvCourtMonth.setText("---");
                }
            } else {
                tvCourtDay.setText("--");
                tvCourtMonth.setText("---");
            }

            // Format and set court time
            if (schedule.getCourtTime() != null && !schedule.getCourtTime().isEmpty()) {
                try {
                    Date time = inputTimeFormat.parse(schedule.getCourtTime());
                    if (time != null) {
                        tvCourtTime.setText(outputTimeFormat.format(time));
                    } else {
                        tvCourtTime.setText(schedule.getCourtTime());
                    }
                } catch (ParseException e) {
                    tvCourtTime.setText(schedule.getCourtTime());
                }
            } else {
                tvCourtTime.setText("No time set");
            }

            // Set court name
            String courtInfo = schedule.getDisplayCourtInfo();
            tvCourtName.setText(!courtInfo.isEmpty() ? courtInfo : "Court not specified");

            // Set hearing type
            tvHearingType.setText(schedule.getHearingType() != null && !schedule.getHearingType().isEmpty()
                    ? schedule.getHearingType() : "General Hearing");

            // Set status (you can customize this based on your business logic)
            setStatusBasedOnDate(schedule);
        }

        private void setStatusBasedOnDate(CourtSchedule schedule) {
            // Simple logic to determine status based on date
            // You can modify this according to your requirements

            if (schedule.getCourtDate() != null && !schedule.getCourtDate().isEmpty()) {
                try {
                    Date courtDate = inputDateFormat.parse(schedule.getCourtDate());
                    Date currentDate = new Date();

                    if (courtDate != null) {
                        // Remove time component for date comparison
                        SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String courtDateStr = dateOnly.format(courtDate);
                        String currentDateStr = dateOnly.format(currentDate);

                        Date courtDateOnly = dateOnly.parse(courtDateStr);
                        Date currentDateOnly = dateOnly.parse(currentDateStr);

                        if (courtDateOnly.equals(currentDateOnly)) {
                            // Today
                            tvStatusText.setText("Today");
                            statusIndicator.setBackgroundResource(R.drawable.circle_orange); // Assuming you have red circle
                        } else if (courtDateOnly.after(currentDateOnly)) {
                            // Upcoming
                            tvStatusText.setText("Upcoming");
                            statusIndicator.setBackgroundResource(R.drawable.circle_green);
                        } else {
                            // Past
                            tvStatusText.setText("Past");
                            statusIndicator.setBackgroundResource(R.drawable.circle_red); // Assuming you have gray circle
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
    public void updateData(List<CourtSchedule> newSchedules) {
        this.courtSchedules = newSchedules;
        notifyDataSetChanged();
    }
}