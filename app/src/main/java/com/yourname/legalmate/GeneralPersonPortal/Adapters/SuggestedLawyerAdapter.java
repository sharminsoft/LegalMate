package com.yourname.legalmate.GeneralPersonPortal.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.yourname.legalmate.GeneralPersonPortal.Activities.LawyerProfileDetailActivity;
import com.yourname.legalmate.GeneralPersonPortal.Models.SuggestedLawyerModel;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.List;

public class SuggestedLawyerAdapter extends RecyclerView.Adapter<SuggestedLawyerAdapter.LawyerViewHolder> {

    private static final String TAG = "SuggestedLawyerAdapter";
    private Context context;
    private List<SuggestedLawyerModel> lawyerList;

    public SuggestedLawyerAdapter(Context context, List<SuggestedLawyerModel> lawyerList) {
        this.context = context;
        this.lawyerList = new ArrayList<>(lawyerList); // এখানে ডিপ কপি
    }


    @NonNull
    @Override
    public LawyerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_suggested_lawyer, parent, false);
        return new LawyerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LawyerViewHolder holder, int position) {
        SuggestedLawyerModel lawyer = lawyerList.get(position);

        Log.d(TAG, "Binding lawyer at position " + position + ": " + lawyer.getFullName());

        // Set lawyer name with safe method
        holder.tvLawyerName.setText(lawyer.getSafeFullName());

        // Set designation using the formatted method
        holder.tvDesignation.setText(lawyer.getFormattedDesignation());

        // Set rating with validation
        float rating = (float) lawyer.getRating();
        if (rating > 0) {
            holder.ratingBar.setRating(rating);
            holder.ratingBar.setVisibility(View.VISIBLE);
            holder.tvRating.setText(lawyer.getFormattedRating());
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.ratingBar.setVisibility(View.GONE);
            holder.tvRating.setVisibility(View.GONE);
        }

        // Set review count
        holder.tvReviewCount.setText(lawyer.getFormattedReviewText());

        // Set profile image with improved error handling
        if (lawyer.hasValidProfileImage()) {
            try {
                Glide.with(context)
                        .load(lawyer.getSafeProfileImageUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_user_profile)
                        .error(R.drawable.ic_user_profile)
                        .into(holder.ivLawyerProfile);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image for lawyer: " + lawyer.getLawyerId(), e);
                holder.ivLawyerProfile.setImageResource(R.drawable.ic_user_profile);
            }
        } else {
            holder.ivLawyerProfile.setImageResource(R.drawable.ic_user_profile);
        }

        // Set click listener for the card with null check
        holder.cardView.setOnClickListener(v -> {
            if (context != null && lawyer.getLawyerId() != null) {
                try {
                    Intent intent = new Intent(context, LawyerProfileDetailActivity.class);
                    intent.putExtra("lawyer_id", lawyer.getLawyerId());
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting LawyerProfileDetailActivity", e);
                }
            }
        });

        Log.d(TAG, "Successfully bound lawyer: " + lawyer.getFullName() +
                ", Rating: " + lawyer.getRating() +
                ", Reviews: " + lawyer.getReviewCount());
    }

    @Override
    public int getItemCount() {
        return lawyerList != null ? lawyerList.size() : 0;
    }

    public void updateData(List<SuggestedLawyerModel> newLawyerList) {
        Log.d(TAG, "Updating adapter data. New list size: " +
                (newLawyerList != null ? newLawyerList.size() : 0));

        if (newLawyerList != null) {
            this.lawyerList.clear();
            this.lawyerList.addAll(new ArrayList<>(newLawyerList)); // কপি নিয়ে অ্যাড করো
            notifyDataSetChanged();

            Log.d(TAG, "Adapter updated successfully with " + this.lawyerList.size() + " items");
        } else {
            Log.w(TAG, "Attempted to update with null lawyer list");
        }
    }


    public void addLawyer(SuggestedLawyerModel lawyer) {
        if (lawyer != null && lawyerList != null) {
            lawyerList.add(lawyer);
            notifyItemInserted(lawyerList.size() - 1);
        }
    }

    public void removeLawyer(int position) {
        if (lawyerList != null && position >= 0 && position < lawyerList.size()) {
            lawyerList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearData() {
        if (lawyerList != null) {
            lawyerList.clear();
            notifyDataSetChanged();
        }
    }

    static class LawyerViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivLawyerProfile;
        TextView tvLawyerName;
        TextView tvDesignation;
        RatingBar ratingBar;
        TextView tvRating;
        TextView tvReviewCount;

        public LawyerViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                cardView = itemView.findViewById(R.id.cardView);
                ivLawyerProfile = itemView.findViewById(R.id.ivLawyerProfile);
                tvLawyerName = itemView.findViewById(R.id.tvLawyerName);
                tvDesignation = itemView.findViewById(R.id.tvDesignation);
                ratingBar = itemView.findViewById(R.id.ratingBar);
                tvRating = itemView.findViewById(R.id.tvRating);
                tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            } catch (Exception e) {
                Log.e("LawyerViewHolder", "Error initializing views", e);
            }
        }
    }
}