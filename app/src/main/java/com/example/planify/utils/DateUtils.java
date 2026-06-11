package com.example.planify.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatFullDate(long timestamp) {
        return FULL_DATE_FORMAT.format(new Date(timestamp));
    }

    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public static boolean isToday(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar ts = Calendar.getInstance();
        ts.setTimeInMillis(timestamp);
        return now.get(Calendar.YEAR) == ts.get(Calendar.YEAR)
            && now.get(Calendar.DAY_OF_YEAR) == ts.get(Calendar.DAY_OF_YEAR);
    }

    public static int getDayOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static int getMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.MONTH); // 0-11
    }

    public static int getYear(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.YEAR);
    }

    public static String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning";
        else if (hour < 17) return "Good Afternoon";
        else return "Good Evening";
    }

    public static String formatMinutesSeconds(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public static String getMonthYearLabel(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
    }
}
