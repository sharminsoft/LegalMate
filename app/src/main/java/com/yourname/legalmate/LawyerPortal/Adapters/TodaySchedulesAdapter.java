package com.yourname.legalmate.LawyerPortal.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.legalmate.LawyerPortal.Models.TodayScheduleItem;
import com.yourname.legalmate.R;

import java.util.List;

public class TodaySchedulesAdapter extends RecyclerView.Adapter<TodaySchedulesAdapter.ScheduleViewHolder> {

    public static String viewName = "";


    private List<TodayScheduleItem> schedules;
    private Context context;
    private OnScheduleClickListener onScheduleClickListener;

    public interface OnScheduleClickListener {
        void onScheduleClick(TodayScheduleItem schedule, int position);
    }

    public TodaySchedulesAdapter(List<TodayScheduleItem> schedules, Context context) {
        this.schedules = schedules;
        this.context = context;
    }

    public TodaySchedulesAdapter(List<TodayScheduleItem> schedules, Context context, OnScheduleClickListener listener) {
        this.schedules = schedules;
        this.context = context;
        this.onScheduleClickListener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewName.equals("bottomSheet")) {
            return new ScheduleViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_today_schedule_bottomsheet, parent, false));
        } else if (viewName.equals("home")) {
            return new ScheduleViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_today_schedule, parent, false));
        } else {
            // Default case - return home layout if viewName is not set or unknown
            return new ScheduleViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_today_schedule, parent, false));
        }

    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        TodayScheduleItem schedule = schedules.get(position);

        // Set title and subtitle
        holder.tvTitle.setText(schedule.getTitle() != null ? schedule.getTitle() : "No Title");
        holder.tvSubtitle.setText(schedule.getSubtitle() != null ? schedule.getSubtitle() : "No Details");

        // Set time
        if (schedule.getTime() != null && !schedule.getTime().isEmpty()) {
            holder.tvTime.setText(schedule.getTime());
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setText("No Time");
            holder.tvTime.setVisibility(View.VISIBLE);
        }

        // Set icon and colors based on type
        if (schedule.isCourtCase()) {
            setupCourtCaseUI(holder);
        } else if (schedule.isAppointment()) {
            setupAppointmentUI(holder);
        } else {
            setupDefaultUI(holder);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onScheduleClickListener != null) {
                onScheduleClickListener.onScheduleClick(schedule, position);
            }
        });

        // Add subtle animation
        animateItem(holder.itemView, position);
    }

    private void setupCourtCaseUI(ScheduleViewHolder holder) {
        // Court case styling
        holder.ivIcon.setImageResource(R.drawable.ic_clock);
        holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.white));

        holder.tvType.setText("Court");
        holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.white));
        holder.tvType.setBackground(ContextCompat.getDrawable(context, R.drawable.court_case_badge_background));
        holder.tvType.setVisibility(View.VISIBLE);
    }

    private void setupAppointmentUI(ScheduleViewHolder holder) {
        // Appointment styling
        holder.ivIcon.setImageResource(R.drawable.ic_clock);
        holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.white));

        holder.tvType.setText("Appointment");
        holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.white));
        holder.tvType.setBackground(ContextCompat.getDrawable(context, R.drawable.appointment_badge_background));
        holder.tvType.setVisibility(View.VISIBLE);
    }

    private void setupDefaultUI(ScheduleViewHolder holder) {
        // Default styling
        holder.ivIcon.setImageResource(R.drawable.ic_schedule);
        holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.white));

        holder.tvType.setText("Schedule");
        holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.white));
        holder.tvType.setBackground(ContextCompat.getDrawable(context, R.drawable.schedule_type_badge_background));
        holder.tvType.setVisibility(View.VISIBLE);
    }

    private void animateItem(View view, int position) {
        // Simple fade in animation
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(position * 50L)
                .start();
    }

    @Override
    public int getItemCount() {
        return schedules != null ? schedules.size() : 0;
    }

    public void updateSchedules(List<TodayScheduleItem> newSchedules) {
        this.schedules = newSchedules;
        notifyDataSetChanged();
    }

    public void addSchedule(TodayScheduleItem schedule) {
        if (schedules != null) {
            schedules.add(schedule);
            notifyItemInserted(schedules.size() - 1);
        }
    }

    public void removeSchedule(int position) {
        if (schedules != null && position >= 0 && position < schedules.size()) {
            schedules.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, schedules.size());
        }
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvTime, tvType;
        ImageView ivIcon;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvScheduleTitle);
            tvSubtitle = itemView.findViewById(R.id.tvScheduleSubtitle);
            tvTime = itemView.findViewById(R.id.tvScheduleTime);
            tvType = itemView.findViewById(R.id.tvScheduleType);
            ivIcon = itemView.findViewById(R.id.ivScheduleIcon);
        }
    }
}