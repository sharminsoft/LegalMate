package com.yourname.legalmate.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.yourname.legalmate.R;

import java.util.Calendar;

public class YearPickerDialog {

    public interface OnYearSelectedListener {
        void onYearSelected(int year);
    }

    public static void showYearPicker(Context context, OnYearSelectedListener listener) {
        showYearPicker(context, Calendar.getInstance().get(Calendar.YEAR), listener);
    }

    public static void showYearPicker(Context context, int defaultYear, OnYearSelectedListener listener) {
        // Create NumberPicker programmatically
        NumberPicker numberPicker = new NumberPicker(context);

        // Set year range (1950 to current year + 5)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = 1950;
        int maxYear = currentYear + 5;

        numberPicker.setMinValue(minYear);
        numberPicker.setMaxValue(maxYear);
        numberPicker.setValue(defaultYear);
        numberPicker.setWrapSelectorWheel(false);

        // Create AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Year");
        builder.setView(numberPicker);

        builder.setPositiveButton("OK", (dialog, which) -> {
            if (listener != null) {
                listener.onYearSelected(numberPicker.getValue());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Overloaded method for enrollment years (limited range)
    public static void showEnrollmentYearPicker(Context context, int defaultYear, OnYearSelectedListener listener) {
        NumberPicker numberPicker = new NumberPicker(context);

        // Set limited range for enrollment (last 50 years to current year)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = currentYear - 50;
        int maxYear = currentYear;

        numberPicker.setMinValue(minYear);
        numberPicker.setMaxValue(maxYear);
        numberPicker.setValue(defaultYear > maxYear ? maxYear : (defaultYear < minYear ? minYear : defaultYear));
        numberPicker.setWrapSelectorWheel(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Enrollment Year");
        builder.setView(numberPicker);

        builder.setPositiveButton("OK", (dialog, which) -> {
            if (listener != null) {
                listener.onYearSelected(numberPicker.getValue());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method for graduation years
    public static void showGraduationYearPicker(Context context, int defaultYear, OnYearSelectedListener listener) {
        NumberPicker numberPicker = new NumberPicker(context);

        // Set range for graduation (1970 to current year + 2)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = 1970;
        int maxYear = currentYear + 2;

        numberPicker.setMinValue(minYear);
        numberPicker.setMaxValue(maxYear);
        numberPicker.setValue(defaultYear > maxYear ? maxYear : (defaultYear < minYear ? minYear : defaultYear));
        numberPicker.setWrapSelectorWheel(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Graduation Year");
        builder.setView(numberPicker);

        builder.setPositiveButton("OK", (dialog, which) -> {
            if (listener != null) {
                listener.onYearSelected(numberPicker.getValue());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}