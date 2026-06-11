package com.example.planify.utils;

import androidx.appcompat.app.AppCompatDelegate;

public class Constants {
    public static final String WEATHER_API_KEY = com.example.planify.BuildConfig.WEATHER_API_KEY;
    public static final String DEFAULT_CITY = "Makassar";
    public static final String DB_NAME = "planify_db";
    public static final String PREF_NAME = "planify_prefs";
    public static final String KEY_THEME = "theme_mode";
    public static final String KEY_POMO_WORK = "pomo_work_duration";
    public static final String KEY_POMO_SHORT = "pomo_short_break";
    public static final String KEY_POMO_LONG = "pomo_long_break";
    public static final String KEY_POMO_INTERVAL = "pomo_long_break_interval";
    public static final String KEY_CACHED_WEATHER = "cached_weather";
    public static final String NOTIFICATION_CHANNEL_ID = "planify_pomodoro";
    public static final String REMINDER_CHANNEL_ID = "planify_reminders";
    public static final int DEFAULT_POMO_WORK = 25;
    public static final int DEFAULT_POMO_SHORT = 5;
    public static final int DEFAULT_POMO_LONG = 15;
    public static final int DEFAULT_POMO_INTERVAL = 4;
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int DEFAULT_THEME = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
}
