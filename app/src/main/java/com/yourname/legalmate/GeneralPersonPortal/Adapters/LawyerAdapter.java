package com.yourname.legalmate.GeneralPersonPortal.Adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.yourname.legalmate.GeneralPersonPortal.Models.LawyerModel;
import com.yourname.legalmate.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LawyerAdapter extends RecyclerView.Adapter<LawyerAdapter.LawyerViewHolder> {

    private Context context;
    private List<LawyerModel> lawyers;
    private OnLawyerClickListener listener;

    public interface OnLawyerClickListener {
        void onLawyerClick(LawyerModel lawyer);
        void onMessageClick(LawyerModel lawyer);
        void onFavoriteClick(LawyerModel lawyer, int position);
    }

    public LawyerAdapter(Context context, List<LawyerModel> lawyers, OnLawyerClickListener listener) {
        this.context = context;
        this.lawyers = lawyers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LawyerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lawyer_card, parent, false);
        return new LawyerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LawyerViewHolder holder, int position) {
        LawyerModel lawyer = lawyers.get(position);
        holder.bind(lawyer, position);
    }

    @Override
    public int getItemCount() {
        return lawyers.size();
    }

    public class LawyerViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView ivLawyerPhoto;
        private View onlineIndicator;
        private TextView tvLawyerName;
        private TextView tvSpecialization;
        private TextView tvExperience;
        private TextView tvLocation;
        private ImageButton btnFavorite;
        private TextView tvRating;
        private TextView tvReviewCount;
        private TextView tvConsultationFee;
        private TextView tvAvailableStatus;
        private MaterialButton btnViewProfile;
        private MaterialButton btnMessage;

        public LawyerViewHolder(@NonNull View itemView) {
            super(itemView);
            initializeViews();
        }

        private void initializeViews() {
            ivLawyerPhoto = itemView.findViewById(R.id.ivLawyerPhoto);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            tvLawyerName = itemView.findViewById(R.id.tvLawyerName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvConsultationFee = itemView.findViewById(R.id.tvConsultationFee);
            tvAvailableStatus = itemView.findViewById(R.id.tvAvailableStatus);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnMessage = itemView.findViewById(R.id.btnMessage);
        }

        public void bind(LawyerModel lawyer, int position) {
            // Set lawyer name
            tvLawyerName.setText(lawyer.getDisplayName());

            // Set specialization
            tvSpecialization.setText(lawyer.getPracticeAreasString());

            // Set experience
            tvExperience.setText(lawyer.getExperienceString());

            // Set location
            tvLocation.setText(lawyer.getLocationString());

            // Set rating
            tvRating.setText(lawyer.getRatingString());

            // Set review count
            tvReviewCount.setText(lawyer.getReviewCountString());

            // Set consultation fee
            tvConsultationFee.setText(lawyer.getConsultationFeeString());

            // Set availability status
            tvAvailableStatus.setText(lawyer.getAvailabilityStatus());
            setAvailabilityStatusStyle(lawyer.isAvailable());

            // Set online indicator
            setOnlineIndicatorVisibility(lawyer.isAvailable());

            // Load profile image
            loadProfileImage(lawyer.getProfileImageUrl());

            // Set favorite button
            setFavoriteButton(lawyer.isFavorite());

            // Set click listeners
            setClickListeners(lawyer, position);
        }

        private void setAvailabilityStatusStyle(boolean isAvailable) {
            if (isAvailable) {
                tvAvailableStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                tvAvailableStatus.setBackgroundResource(R.drawable.available_status_background);
            } else {
                tvAvailableStatus.setText("Busy");
                tvAvailableStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_orange));
                tvAvailableStatus.setBackgroundResource(R.drawable.busy_status_background);
            }
        }

        private void setOnlineIndicatorVisibility(boolean isAvailable) {
            onlineIndicator.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
        }

        private void loadProfileImage(String imageUrl) {
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.lowyer_placeholder)
                    .error(R.drawable.lowyer_placeholder)
                    .centerCrop();

            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(context)
                        .load(imageUrl)
                        .apply(requestOptions)
                        .into(ivLawyerPhoto);
            } else {
                Glide.with(context)
                        .load(R.drawable.lowyer_placeholder)
                        .apply(requestOptions)
                        .into(ivLawyerPhoto);
            }
        }

        private void setFavoriteButton(boolean isFavorite) {
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                btnFavorite.setColorFilter(ContextCompat.getColor(context, R.color.error_red));
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                btnFavorite.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        private void setClickListeners(LawyerModel lawyer, int position) {
            // Card click - show lawyer profile
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLawyerClick(lawyer);
                }
            });

            // View Profile button click
            btnViewProfile.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLawyerClick(lawyer);
                }
            });

            // Message button click
            btnMessage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(lawyer);
                }
            });

            // Favorite button click
            btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(lawyer, position);
                }
            });
        }
    }

    // Method to update a specific item
    public void updateItem(int position, LawyerModel lawyer) {
        if (position >= 0 && position < lawyers.size()) {
            lawyers.set(position, lawyer);
            notifyItemChanged(position);
        }
    }

    // Method to add items
    public void addItems(List<LawyerModel> newLawyers) {
        int startPosition = lawyers.size();
        lawyers.addAll(newLawyers);
        notifyItemRangeInserted(startPosition, newLawyers.size());
    }

    // Method to clear all items
    public void clearItems() {
        lawyers.clear();
        notifyDataSetChanged();
    }

    // Method to remove item
    public void removeItem(int position) {
        if (position >= 0 && position < lawyers.size()) {
            lawyers.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, lawyers.size());
        }
    }

    // Method to get item at position
    public LawyerModel getItem(int position) {
        if (position >= 0 && position < lawyers.size()) {
            return lawyers.get(position);
        }
        return null;
    }

    // Method to update the entire list
    public void updateList(List<LawyerModel> newLawyers) {
        this.lawyers = newLawyers;
        notifyDataSetChanged();
    }
}