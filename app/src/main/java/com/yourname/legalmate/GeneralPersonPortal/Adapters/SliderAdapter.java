package com.yourname.legalmate.GeneralPersonPortal.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.yourname.legalmate.GeneralPersonPortal.Models.SliderItem;
import com.yourname.legalmate.R;

import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

    private Context context;
    private List<SliderItem> sliderItems;
    private OnSliderClickListener clickListener;

    public interface OnSliderClickListener {
        void onSliderClick(SliderItem sliderItem, int position);
    }

    public SliderAdapter(Context context, List<SliderItem> sliderItems) {
        this.context = context;
        this.sliderItems = sliderItems;
    }

    public void setOnSliderClickListener(OnSliderClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        SliderItem item = sliderItems.get(position);

        holder.tvSliderTitle.setText(item.getSliderTitle());
        holder.tvSliderSubtitle.setText(item.getSliderSubtitle());

        // Load image using Glide
        if (item.getSliderImageUrl() != null && !item.getSliderImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getSliderImageUrl())
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(16))
                            .placeholder(R.drawable.ic_legal_advice)
                            .error(R.drawable.ic_legal_advice))
                    .into(holder.ivSliderImage);
        } else {
            holder.ivSliderImage.setImageResource(R.drawable.ic_legal_advice);
        }

        // Handle button click
        holder.btnSliderAction.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSliderClick(item, position);
            }
        });

        // Handle card click
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSliderClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sliderItems != null ? sliderItems.size() : 0;
    }

    public void updateData(List<SliderItem> newItems) {
        this.sliderItems = newItems;
        notifyDataSetChanged();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSliderTitle, tvSliderSubtitle;
        ImageView ivSliderImage;
        Button btnSliderAction;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSliderTitle = itemView.findViewById(R.id.tvSliderTitle);
            tvSliderSubtitle = itemView.findViewById(R.id.tvSliderSubtitle);
            ivSliderImage = itemView.findViewById(R.id.ivSliderImage);
            btnSliderAction = itemView.findViewById(R.id.btnSliderAction);
        }
    }
}