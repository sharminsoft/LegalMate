package com.yourname.legalmate.GeneralPersonPortal.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.yourname.legalmate.GeneralPersonPortal.Models.AppointmentModel;
import com.yourname.legalmate.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private Context context;
    private List<AppointmentModel> appointmentList;
    private OnAppointmentClickListener clickListener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(AppointmentModel appointment);
        void onCancelClick(AppointmentModel appointment);
        void onRescheduleClick(AppointmentModel appointment);
    }

    public AppointmentAdapter(Context context, List<AppointmentModel> appointmentList) {
        this.context = context;
        this.appointmentList = appointmentList;
    }

    public void setOnAppointmentClickListener(OnAppointmentClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        AppointmentModel appointment = appointmentList.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    public void updateAppointments(List<AppointmentModel> newAppointments) {
        this.appointmentList = newAppointments;
        notifyDataSetChanged();
    }

    public class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private View statusIndicator;
        private TextView tvCaseTitle;
        private TextView tvLawyerName;
        private TextView tvAppointmentDate;
        private TextView tvAppointmentTime;
        private Chip chipStatus;
        private LinearLayout layoutActions;
        private MaterialButton btnCancel;
        private MaterialButton btnReschedule;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
            setupClickListeners();
        }

        private void initViews() {
            statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            tvCaseTitle = itemView.findViewById(R.id.tvCaseTitle);
            tvLawyerName = itemView.findViewById(R.id.tvLawyerName);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnReschedule = itemView.findViewById(R.id.btnReschedule);
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onAppointmentClick(appointmentList.get(position));
                    }
                }
            });

            btnCancel.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onCancelClick(appointmentList.get(position));
                    }
                }
            });

            btnReschedule.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onRescheduleClick(appointmentList.get(position));
                    }
                }
            });
        }

        public void bind(AppointmentModel appointment) {
            // Set case title
            tvCaseTitle.setText(appointment.getCaseTitle());

            // Set lawyer name (fetch from LawyerModel if needed)
            String lawyerName = appointment.getLawyerName();
            if (lawyerName == null || lawyerName.isEmpty()) {
                lawyerName = "Lawyer Name Loading...";
                // You can fetch lawyer name here using appointment.getLawyerId()
            }
            tvLawyerName.setText(lawyerName);

            // Set formatted date
            tvAppointmentDate.setText(formatDate(appointment.getDate()));

            // Set time
            tvAppointmentTime.setText(appointment.getTime());

            // Set status chip
            setupStatusChip(appointment.getStatus());

            // Set status indicator color
            setupStatusIndicator(appointment.getStatus());

            // Show/hide action buttons based on status
            setupActionButtons(appointment.getStatus());
        }

        private void setupStatusChip(String status) {
            chipStatus.setText(capitalizeFirst(status));

            // Set chip colors based on status
            int backgroundColor, textColor;
            switch (status.toLowerCase()) {
                case "confirmed":
                    backgroundColor = ContextCompat.getColor(context, R.color.success_container);
                    textColor = ContextCompat.getColor(context, R.color.on_success_container);
                    break;
                case "pending":
                    backgroundColor = ContextCompat.getColor(context, R.color.warning_container);
                    textColor = ContextCompat.getColor(context, R.color.on_warning_container);
                    break;
                case "cancelled":
                    backgroundColor = ContextCompat.getColor(context, R.color.error_container);
                    textColor = ContextCompat.getColor(context, R.color.on_error_container);
                    break;
                case "completed":
                    backgroundColor = ContextCompat.getColor(context, R.color.info_container);
                    textColor = ContextCompat.getColor(context, R.color.on_info_container);
                    break;
                default:
                    backgroundColor = ContextCompat.getColor(context, R.color.surface_variant);
                    textColor = ContextCompat.getColor(context, R.color.on_surface_variant);
                    break;
            }

            chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(backgroundColor));
            chipStatus.setTextColor(textColor);
        }

        private void setupStatusIndicator(String status) {
            int color;
            switch (status.toLowerCase()) {
                case "confirmed":
                    color = ContextCompat.getColor(context, R.color.success);
                    break;
                case "pending":
                    color = ContextCompat.getColor(context, R.color.warning);
                    break;
                case "cancelled":
                    color = ContextCompat.getColor(context, R.color.error);
                    break;
                case "completed":
                    color = ContextCompat.getColor(context, R.color.info);
                    break;
                default:
                    color = ContextCompat.getColor(context, R.color.primary);
                    break;
            }
            statusIndicator.setBackgroundColor(color);
        }

        private void setupActionButtons(String status) {
            // Show action buttons only for pending appointments
            if ("pending".equalsIgnoreCase(status)) {
                layoutActions.setVisibility(View.VISIBLE);
            } else {
                layoutActions.setVisibility(View.GONE);
            }
        }

        private String formatDate(String dateString) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return dateString; // Return original if parsing fails
            }
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }
}