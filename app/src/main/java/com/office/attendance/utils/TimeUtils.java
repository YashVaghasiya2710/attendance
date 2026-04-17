package com.office.attendance.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    public static String todayDate() {
        return formatDate(new Date());
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "--:--";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatTimeShort(long millis) {
        if (millis <= 0) return "--:--";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatHours(double hours) {
        if (hours < 0) return "--";
        int h = (int) hours;
        int m = (int) Math.round((hours - h) * 60);
        return String.format(Locale.getDefault(), "%dh %02dm", h, m);
    }

    public static String formatHoursDiff(double hours) {
        String sign = hours >= 0 ? "+" : "-";
        double absHours = Math.abs(hours);
        int h = (int) absHours;
        int m = (int) Math.round((absHours - h) * 60);
        return String.format(Locale.getDefault(), "%s%dh %02dm", sign, h, m);
    }

    public static String formatDate(String dateStr) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static String formatDateShort(String dateStr) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd MMM", Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static long parseTimeShort(String timeStr, String dateStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;
        try {
            // Support both 12h and 24h formats
            String[] formats = {"yyyy-MM-dd hh:mm a", "yyyy-MM-dd HH:mm", "yyyy-MM-dd h:mm a"};
            for (String format : formats) {
                try {
                    SimpleDateFormat fmt = new SimpleDateFormat(format, Locale.getDefault());
                    Date date = fmt.parse(dateStr + " " + timeStr);
                    if (date != null) return date.getTime();
                } catch (Exception ignored) {}
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String monthYearLabel(int year, int month) {
        // month is 0-based
        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month, 1);
        return fmt.format(cal.getTime());
    }
}