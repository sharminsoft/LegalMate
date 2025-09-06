package com.yourname.legalmate.LawyerPortal;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CustomCalendarView extends View {

    private static final String TAG = "CustomCalendarView";

    // Date formats
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    // Calendar data
    private Calendar calendar;
    private Calendar currentMonth;
    private String selectedDate;

    // Marked dates
    private Set<String> upcomingDates = new HashSet<>();
    private Set<String> pastDates = new HashSet<>();

    // Paint objects
    private Paint headerPaint;
    private Paint datePaint;
    private Paint selectedDatePaint;
    private Paint upcomingDatePaint;
    private Paint pastDatePaint;
    private Paint todayPaint;

    // Dimensions
    private int cellWidth;
    private int cellHeight;
    private int headerHeight;

    // Colors
    private int primaryColor;
    private int textColor;
    private int backgroundColor;
    private int upcomingColor;
    private int pastColor;
    private int todayColor;

    // Interface for date selection
    public interface OnDateSelectedListener {
        void onDateSelected(String date, int year, int month, int dayOfMonth);
    }

    private OnDateSelectedListener dateSelectedListener;

    public CustomCalendarView(Context context) {
        super(context);
        init();
    }

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize calendar
        calendar = Calendar.getInstance();
        currentMonth = (Calendar) calendar.clone();

        // Initialize colors
        primaryColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        textColor = Color.BLACK;
        backgroundColor = Color.WHITE;
        upcomingColor = Color.parseColor("#4CAF50"); // Green
        pastColor = Color.parseColor("#F44336"); // Red
        todayColor = Color.parseColor("#FF9800"); // Orange

        // Initialize paints
        setupPaints();
    }

    private void setupPaints() {
        // Header paint (Month/Year)
        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(primaryColor);
        headerPaint.setTextSize(60f);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
        headerPaint.setTextAlign(Paint.Align.CENTER);

        // Regular date paint
        datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(textColor);
        datePaint.setTextSize(40f);
        datePaint.setTextAlign(Paint.Align.CENTER);

        // Selected date paint
        selectedDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedDatePaint.setColor(Color.WHITE);
        selectedDatePaint.setTextSize(40f);
        selectedDatePaint.setTextAlign(Paint.Align.CENTER);
        selectedDatePaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Upcoming date paint
        upcomingDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        upcomingDatePaint.setColor(upcomingColor);
        upcomingDatePaint.setStyle(Paint.Style.FILL);

        // Past date paint
        pastDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pastDatePaint.setColor(pastColor);
        pastDatePaint.setStyle(Paint.Style.FILL);

        // Today paint
        todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayPaint.setColor(todayColor);
        todayPaint.setStyle(Paint.Style.STROKE);
        todayPaint.setStrokeWidth(6f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        cellWidth = width / 7; // 7 days in a week
        headerHeight = 120;
        cellHeight = (height - headerHeight - 100) / 6; // 6 rows max
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        canvas.drawColor(backgroundColor);

        // Draw header (Month Year)
        drawHeader(canvas);

        // Draw day labels (Sun, Mon, Tue, etc.)
        drawDayLabels(canvas);

        // Draw calendar dates
        drawCalendarDates(canvas);
    }

    private void drawHeader(Canvas canvas) {
        String monthYear = monthYearFormat.format(currentMonth.getTime());
        float centerX = getWidth() / 2f;
        float centerY = headerHeight / 2f + 20;

        canvas.drawText(monthYear, centerX, centerY, headerPaint);
    }

    private void drawDayLabels(Canvas canvas) {
        String[] dayLabels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        Paint dayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayLabelPaint.setColor(primaryColor);
        dayLabelPaint.setTextSize(30f);
        dayLabelPaint.setTextAlign(Paint.Align.CENTER);
        dayLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        float y = headerHeight + 50;

        for (int i = 0; i < dayLabels.length; i++) {
            float x = (i * cellWidth) + (cellWidth / 2f);
            canvas.drawText(dayLabels[i], x, y, dayLabelPaint);
        }
    }

    private void drawCalendarDates(Canvas canvas) {
        Calendar tempCalendar = (Calendar) currentMonth.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        String today = dateFormat.format(new Date());

        int day = 1;
        for (int week = 0; week < 6; week++) {
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                if (week == 0 && dayOfWeek < firstDayOfWeek) {
                    continue; // Empty cells before first day of month
                }

                if (day > daysInMonth) {
                    break; // No more days in this month
                }

                // Calculate position
                float x = (dayOfWeek * cellWidth) + (cellWidth / 2f);
                float y = headerHeight + 100 + (week * cellHeight) + (cellHeight / 2f) + 15;

                // Create date string for this day
                tempCalendar.set(Calendar.DAY_OF_MONTH, day);
                String currentDate = dateFormat.format(tempCalendar.getTime());

                // Draw date background if it has court schedules
                drawDateBackground(canvas, x, y - 15, currentDate, today);

                // Choose paint based on date status
                Paint paintToUse = datePaint;
                if (currentDate.equals(selectedDate)) {
                    paintToUse = selectedDatePaint;
                } else if (currentDate.equals(today)) {
                    paintToUse = new Paint(datePaint);
                    paintToUse.setTypeface(Typeface.DEFAULT_BOLD);
                    paintToUse.setColor(todayColor);
                }

                // Draw day number
                canvas.drawText(String.valueOf(day), x, y, paintToUse);

                day++;
            }
        }
    }

    private void drawDateBackground(Canvas canvas, float centerX, float centerY, String date, String today) {
        float radius = Math.min(cellWidth, cellHeight) * 0.35f;

        if (upcomingDates.contains(date)) {
            // Draw green circle for upcoming dates
            canvas.drawCircle(centerX, centerY, radius, upcomingDatePaint);
        } else if (pastDates.contains(date)) {
            // Draw red circle for past dates
            canvas.drawCircle(centerX, centerY, radius, pastDatePaint);
        }

        if (date.equals(selectedDate)) {
            // Draw selection background
            Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectionPaint.setColor(primaryColor);
            selectionPaint.setAlpha(200);
            canvas.drawCircle(centerX, centerY, radius + 5, selectionPaint);
        }

        if (date.equals(today)) {
            // Draw today border
            canvas.drawCircle(centerX, centerY, radius + 8, todayPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // Check if touch is in calendar area
            if (y > headerHeight + 80) {
                int dayOfWeek = (int) (x / cellWidth);
                int week = (int) ((y - headerHeight - 100) / cellHeight);

                // Calculate the actual date
                Calendar tempCalendar = (Calendar) currentMonth.clone();
                tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
                int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;

                int day = (week * 7) + dayOfWeek - firstDayOfWeek + 1;
                int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                if (day > 0 && day <= daysInMonth) {
                    tempCalendar.set(Calendar.DAY_OF_MONTH, day);
                    selectedDate = dateFormat.format(tempCalendar.getTime());

                    if (dateSelectedListener != null) {
                        dateSelectedListener.onDateSelected(selectedDate,
                                tempCalendar.get(Calendar.YEAR),
                                tempCalendar.get(Calendar.MONTH),
                                day);
                    }

                    invalidate(); // Redraw the view
                }
            }
            return true;
        }

        return super.onTouchEvent(event);
    }

    // Public methods for updating calendar
    public void setUpcomingDates(Set<String> dates) {
        this.upcomingDates.clear();
        if (dates != null) {
            this.upcomingDates.addAll(dates);
        }
        invalidate();
    }

    public void setPastDates(Set<String> dates) {
        this.pastDates.clear();
        if (dates != null) {
            this.pastDates.addAll(dates);
        }
        invalidate();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.dateSelectedListener = listener;
    }

    public void goToNextMonth() {
        currentMonth.add(Calendar.MONTH, 1);
        invalidate();
    }

    public void goToPreviousMonth() {
        currentMonth.add(Calendar.MONTH, -1);
        invalidate();
    }

    public void goToCurrentMonth() {
        currentMonth = (Calendar) Calendar.getInstance().clone();
        invalidate();
    }

    public void setDate(long timeInMillis) {
        calendar.setTimeInMillis(timeInMillis);
        currentMonth.setTimeInMillis(timeInMillis);
        selectedDate = dateFormat.format(calendar.getTime());
        invalidate();
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public Calendar getCurrentMonth() {
        return (Calendar) currentMonth.clone();
    }
}