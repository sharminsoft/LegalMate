package com.yourname.legalmate.LawyerPortal.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.legalmate.LawyerPortal.Models.Case;
import com.yourname.legalmate.R;

import java.util.List;

public class CasesAdapter extends RecyclerView.Adapter<CasesAdapter.CaseViewHolder> {

    private Context context;
    private List<Case> caseList;
    private OnCaseClickListener listener;

    // Fixed interface - removed duplicate "Case" keyword
    public interface OnCaseClickListener {
        void onCaseClick(Case currentCase);
    }

    public CasesAdapter(Context context, List<Case> caseList) {
        this.context = context;
        this.caseList = caseList;
    }

    public void setOnCaseClickListener(OnCaseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_case, parent, false);
        return new CaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaseViewHolder holder, int position) {
        Case currentCase = caseList.get(position);

        // Null safety checks
        String clientName = currentCase.getClientName() != null ?
                currentCase.getClientName() : "Unknown Client";
        String caseStatus = currentCase.getCaseStatus() != null ?
                currentCase.getCaseStatus() : "Unknown Status";

        holder.tvClientName.setText("Client: " + clientName);
        holder.tvCaseStatus.setText(caseStatus);

        // Set case title if available
        if (currentCase.getCaseTitle() != null && !currentCase.getCaseTitle().isEmpty()) {
            // If you have a case title TextView in your layout, uncomment below:
            // holder.tvCaseTitle.setText(currentCase.getCaseTitle());
        }

        // Set status color based on case status - using ContextCompat for better compatibility
        setStatusColor(holder.tvCaseStatus, caseStatus.toLowerCase());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCaseClick(currentCase);
            }
        });
    }

    private void setStatusColor(TextView statusTextView, String status) {
        int colorResId;

        switch (status) {
            case "ongoing":
            case "active":
                colorResId = android.R.color.holo_blue_dark;
                break;
            case "completed":
            case "closed":
                colorResId = android.R.color.holo_green_dark;
                break;
            case "pending":
            case "waiting":
                colorResId = android.R.color.holo_orange_dark;
                break;
            case "cancelled":
            case "canceled":
            case "rejected":
                colorResId = android.R.color.holo_red_dark;
                break;
            case "on hold":
            case "paused":
                colorResId = android.R.color.darker_gray;
                break;
            default:
                colorResId = android.R.color.primary_text_light;
                break;
        }

        // Use ContextCompat for better compatibility across Android versions
        int color = ContextCompat.getColor(context, colorResId);
        statusTextView.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return caseList != null ? caseList.size() : 0;
    }

    // Method to update the entire case list
    public void updateCases(List<Case> newCases) {
        if (newCases != null) {
            this.caseList = newCases;
            notifyDataSetChanged();
        }
    }

    // Method to filter cases (this should be used with filtered list)
    public void filterCases(List<Case> filteredCases) {
        if (filteredCases != null) {
            this.caseList = filteredCases;
            notifyDataSetChanged();
        }
    }

    // Method to add a single case
    public void addCase(Case newCase) {
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
    public void updateCase(int position, Case updatedCase) {
        if (caseList != null && position >= 0 && position < caseList.size() && updatedCase != null) {
            caseList.set(position, updatedCase);
            notifyItemChanged(position);
        }
    }

    // Method to get case at specific position
    public Case getCaseAt(int position) {
        if (caseList != null && position >= 0 && position < caseList.size()) {
            return caseList.get(position);
        }
        return null;
    }

    public static class CaseViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvCaseStatus;
        ImageView ivArrow;
        // Add more views as needed
        // TextView tvCaseTitle, tvDateCreated;

        public CaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvCaseStatus = itemView.findViewById(R.id.tvCaseStatus);
            ivArrow = itemView.findViewById(R.id.ivArrow);

            // Initialize other views if they exist in your layout
            // tvCaseTitle = itemView.findViewById(R.id.tvCaseTitle);
            // tvDateCreated = itemView.findViewById(R.id.tvDateCreated);
        }
    }
}