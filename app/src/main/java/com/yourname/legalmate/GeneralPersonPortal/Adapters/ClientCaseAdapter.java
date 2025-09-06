package com.yourname.legalmate.GeneralPersonPortal.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yourname.legalmate.GeneralPersonPortal.Models.ClientCase;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ClientCaseAdapter extends RecyclerView.Adapter<ClientCaseAdapter.ClientCaseViewHolder> {

    private Context context;
    private List<ClientCase> caseList;
    private OnCaseClickListener listener;

    public interface OnCaseClickListener {
        void onCaseClick(ClientCase clientCase);
    }

    public ClientCaseAdapter(Context context, List<ClientCase> caseList) {
        this.context = context;
        this.caseList = caseList;
    }

    public void setOnCaseClickListener(OnCaseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClientCaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_client_case, parent, false);
        return new ClientCaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientCaseViewHolder holder, int position) {
        ClientCase currentCase = caseList.get(position);

        // Null safety checks and populate data
        String caseTitle = currentCase.getCaseTitle() != null ?
                currentCase.getCaseTitle() : "Unknown Case";
        String caseType = currentCase.getCaseType() != null ?
                currentCase.getCaseType() : "General";
        String caseStatus = currentCase.getCaseStatus() != null ?
                currentCase.getCaseStatus() : "Unknown";
        String courtName = currentCase.getCourtName() != null ?
                currentCase.getCourtName() : "Court not assigned";
        String caseNumber = currentCase.getCaseNumber() != null ?
                "Case No: " + currentCase.getCaseNumber() : "Case No: Not assigned";
        String lawyerName = currentCase.getLawyerName() != null ?
                "Advocate " + currentCase.getLawyerName() : "Lawyer not assigned";
        String lawyerSpecialization = currentCase.getLawyerSpecialization() != null ?
                currentCase.getLawyerSpecialization() : "Legal Specialist";

        // Set basic case information
        holder.tvCaseTitle.setText(caseTitle);
        holder.tvCaseType.setText(caseType);
        holder.tvCaseStatus.setText(caseStatus);
        holder.tvCaseNumber.setText(caseNumber);
        holder.tvCourtName.setText(courtName);

        // Set lawyer information
        holder.tvLawyerName.setText(lawyerName);
        holder.tvLawyerSpecialization.setText(lawyerSpecialization);

        // Load lawyer profile image
        if (currentCase.getLawyerImageUrl() != null && !currentCase.getLawyerImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(currentCase.getLawyerImageUrl())
                    .placeholder(R.drawable.ic_person_ai)
                    .error(R.drawable.ic_person_ai)
                    .circleCrop()
                    .into(holder.ivLawyerImage);
        } else {
            holder.ivLawyerImage.setImageResource(R.drawable.ic_person_ai);
        }

        // Set next court date
        if (currentCase.getCourtDate() != null && !currentCase.getCourtDate().isEmpty()) {
            holder.tvNextCourtDate.setText(formatDate(currentCase.getCourtDate()));
            holder.layoutNextDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvNextCourtDate.setText("Not scheduled");
            holder.layoutNextDate.setVisibility(View.VISIBLE);
        }

        // Set status color based on case status
        setStatusColor(holder.tvCaseStatus, caseStatus.toLowerCase());

        // Set case type background
        setCaseTypeBackground(holder.tvCaseType, caseType.toLowerCase());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCaseClick(currentCase);
            }
        });
    }

    private void setStatusColor(TextView statusTextView, String status) {
        int colorResId;
        int backgroundResId;

        switch (status) {
            case "ongoing":
            case "active":
                colorResId = android.R.color.white;
                backgroundResId = R.drawable.status_badge_active;
                break;
            case "completed":
            case "closed":
                colorResId = android.R.color.white;
                backgroundResId = R.drawable.status_badge_completed;
                break;
            case "pending":
            case "waiting":
                colorResId = android.R.color.white;
                backgroundResId = R.drawable.status_badge_pending;
                break;
            case "cancelled":
            case "canceled":
            case "rejected":
                colorResId = android.R.color.white;
                backgroundResId = R.drawable.status_badge_cancelled;
                break;
            case "on hold":
            case "paused":
                colorResId = android.R.color.white;
                backgroundResId = R.drawable.status_badge_on_hold;
                break;
            default:
                colorResId = android.R.color.white;
                backgroundResId = R.drawable.status_badge_background_case_tracking;
                break;
        }

        statusTextView.setTextColor(ContextCompat.getColor(context, colorResId));
        statusTextView.setBackgroundResource(backgroundResId);
    }

    private void setCaseTypeBackground(TextView typeTextView, String caseType) {
        int backgroundResId;

        switch (caseType) {
            case "civil":
                backgroundResId = R.drawable.type_badge_civil;
                break;
            case "criminal":
                backgroundResId = R.drawable.type_badge_criminal;
                break;
            case "family":
                backgroundResId = R.drawable.type_badge_family;
                break;
            case "commercial":
            case "business":
                backgroundResId = R.drawable.type_badge_commercial;
                break;
            case "property":
                backgroundResId = R.drawable.type_badge_property;
                break;
            default:
                backgroundResId = R.drawable.type_badge_background;
                break;
        }

        typeTextView.setBackgroundResource(backgroundResId);
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString; // Return original if parsing fails
        }
    }

    @Override
    public int getItemCount() {
        return caseList != null ? caseList.size() : 0;
    }

    // Method to update the entire case list
    public void updateCases(List<ClientCase> newCases) {
        if (newCases != null) {
            this.caseList = newCases;
            notifyDataSetChanged();
        }
    }

    // Method to add a single case
    public void addCase(ClientCase newCase) {
        if (newCase != null && caseList != null) {
            caseList.add(newCase);
            notifyItemInserted(caseList.size() - 1);
        }
    }

    // Method to remove a case
    public void removeCase(int position) {
        if (caseList != null && position >= 0 && position < caseList.size()) {
            caseList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Method to update a specific case
    public void updateCase(int position, ClientCase updatedCase) {
        if (caseList != null && position >= 0 && position < caseList.size() && updatedCase != null) {
            caseList.set(position, updatedCase);
            notifyItemChanged(position);
        }
    }

    // Method to get case at specific position
    public ClientCase getCaseAt(int position) {
        if (caseList != null && position >= 0 && position < caseList.size()) {
            return caseList.get(position);
        }
        return null;
    }

    public static class ClientCaseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCaseTitle, tvCaseType, tvCaseStatus, tvCaseNumber, tvCourtName;
        TextView tvLawyerName, tvLawyerSpecialization, tvNextCourtDate;
        CircleImageView ivLawyerImage;
        LinearLayout layoutNextDate;
        ImageView ivArrow;

        public ClientCaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCaseTitle = itemView.findViewById(R.id.tvCaseTitle);
            tvCaseType = itemView.findViewById(R.id.tvCaseType);
            tvCaseStatus = itemView.findViewById(R.id.tvCaseStatus);
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvCourtName = itemView.findViewById(R.id.tvCourtName);
            tvLawyerName = itemView.findViewById(R.id.tvLawyerName);
            tvLawyerSpecialization = itemView.findViewById(R.id.tvLawyerSpecialization);
            tvNextCourtDate = itemView.findViewById(R.id.tvNextCourtDate);
            ivLawyerImage = itemView.findViewById(R.id.ivLawyerImage);
            layoutNextDate = itemView.findViewById(R.id.layoutNextDate);
            ivArrow = itemView.findViewById(R.id.ivArrow);
        }
    }
}