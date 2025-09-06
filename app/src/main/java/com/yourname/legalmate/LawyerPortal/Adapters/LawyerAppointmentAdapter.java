package com.yourname.legalmate.LawyerPortal.Adapters;

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
import java.util.Map;

public class LawyerAppointmentAdapter extends RecyclerView.Adapter<LawyerAppointmentAdapter.LawyerAppointmentViewHolder> {

    private Context context;
    private List<AppointmentModel> appointmentList;
    private OnLawyerAppointmentClickListener clickListener;
    private Map<String, String> clientNamesCache;

    public interface OnLawyerAppointmentClickListener {
        void onAppointmentClick(AppointmentModel appointment);
        void onApproveClick(AppointmentModel appointment);
        void onRejectClick(AppointmentModel appointment);
        void onClientDetailsClick(AppointmentModel appointment);
    }

    public LawyerAppointmentAdapter(Context context, List<AppointmentModel> appointmentList, Map<String, String> clientNamesCache) {
        this.context = context;
        this.appointmentList = appointmentList;
        this.clientNamesCache = clientNamesCache;
    }

    public void setOnLawyerAppointmentClickListener(OnLawyerAppointmentClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public LawyerAppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lawyer_appointment, parent, false);
        return new LawyerAppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LawyerAppointmentViewHolder holder, int position) {
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

    public class LawyerAppointmentViewHolder extends RecyclerView.ViewHolder {
        private View statusIndicator;
        private TextView tvCaseTitle;
        private TextView tvClientName;
        private TextView tvAppointmentDate;
        private TextView tvAppointmentTime;
        private TextView tvDescription;
        private Chip chipStatus;
        private LinearLayout layoutActions;
        private MaterialButton btnApprove;
        private MaterialButton btnReject;
        private MaterialButton btnClientDetails;

        public LawyerAppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
            setupClickListeners();
        }

        private void initViews() {
            statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            tvCaseTitle = itemView.findViewById(R.id.tvCaseTitle);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnClientDetails = itemView.findViewById(R.id.btnClientDetails);
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

            btnApprove.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onApproveClick(appointmentList.get(position));
                    }
                }
            });

            btnReject.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onRejectClick(appointmentList.get(position));
                    }
                }
            });

            btnClientDetails.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onClientDetailsClick(appointmentList.get(position));
                    }
                }
            });
        }

        public void bind(AppointmentModel appointment) {
            // Set case title
            tvCaseTitle.setText(appointment.getCaseTitle() != null ? appointment.getCaseTitle() : "No Title");

            // Set client name from cache
            String clientName = getClientNameFromCache(appointment.getClientId());
            tvClientName.setText(clientName);

            // Set formatted date
            tvAppointmentDate.setText(formatDate(appointment.getDate()));

            // Set time
            tvAppointmentTime.setText(appointment.getTime() != null ? appointment.getTime() : "N/A");

            // Set description
            if (appointment.getDescription() != null && !appointment.getDescription().isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(appointment.getDescription());
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Set status chip
            setupStatusChip(appointment.getStatus());

            // Set status indicator color
            setupStatusIndicator(appointment.getStatus());

            // Show/hide action buttons based on status
            setupActionButtons(appointment.getStatus());
        }

        private String getClientNameFromCache(String clientId) {
            if (clientId != null && clientNamesCache != null && clientNamesCache.containsKey(clientId)) {
                return clientNamesCache.get(clientId);
            }
            return "Loading...";
        }

        private void setupStatusChip(String status) {
            String displayStatus = status != null ? capitalizeFirst(status) : "Unknown";
            chipStatus.setText(displayStatus);

            // Set chip colors based on status
            int backgroundColor, textColor;
            String statusLower = status != null ? status.toLowerCase() : "";

            switch (statusLower) {
                case "approved":
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
            String statusLower = status != null ? status.toLowerCase() : "";

            switch (statusLower) {
                case "approved":
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
            // Show approve/reject buttons only for pending appointments
            String statusLower = status != null ? status.toLowerCase() : "";

            if ("pending".equals(statusLower)) {
                layoutActions.setVisibility(View.VISIBLE);
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
            } else {
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
            }

            // Client details button is always visible
            btnClientDetails.setVisibility(View.VISIBLE);

            // If no action buttons are visible, hide the entire layout
            if (btnApprove.getVisibility() == View.GONE &&
                    btnReject.getVisibility() == View.GONE &&
                    btnClientDetails.getVisibility() == View.GONE) {
                layoutActions.setVisibility(View.GONE);
            } else {
                layoutActions.setVisibility(View.VISIBLE);
            }
        }

        private String formatDate(String dateString) {
            if (dateString == null || dateString.isEmpty()) {
                return "N/A";
            }

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
            if (text == null || text.isEmpty()) return "Unknown";
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }
}