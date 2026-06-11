package com.example.planify.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class PreferenceManager {
    private static PreferenceManager instance;
    private final SharedPreferences prefs;

    private PreferenceManager(Context context) {
        prefs = context.getApplicationContext()
            .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    // Theme
    public void saveThemeMode(int mode) {
        prefs.edit().putInt(Constants.KEY_THEME, mode).apply();
    }

    public int getThemeMode() {
        return prefs.getInt(Constants.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // Pomodoro settings
    public void savePomoDuration(int minutes) {
        prefs.edit().putInt(Constants.KEY_POMO_WORK, minutes).apply();
    }

    public int getPomoDuration() {
        return prefs.getInt(Constants.KEY_POMO_WORK, Constants.DEFAULT_POMO_WORK);
    }

    public void savePomoShortBreak(int minutes) {
        prefs.edit().putInt(Constants.KEY_POMO_SHORT, minutes).apply();
    }

    public int getPomoShortBreak() {
        return prefs.getInt(Constants.KEY_POMO_SHORT, Constants.DEFAULT_POMO_SHORT);
    }

    public void savePomoLongBreak(int minutes) {
        prefs.edit().putInt(Constants.KEY_POMO_LONG, minutes).apply();
    }

    public int getPomoLongBreak() {
        return prefs.getInt(Constants.KEY_POMO_LONG, Constants.DEFAULT_POMO_LONG);
    }

    public void savePomoInterval(int n) {
        prefs.edit().putInt(Constants.KEY_POMO_INTERVAL, n).apply();
    }

    public int getPomoInterval() {
        return prefs.getInt(Constants.KEY_POMO_INTERVAL, Constants.DEFAULT_POMO_INTERVAL);
    }

    // Weather cache
    public void cacheWeather(String json) {
        prefs.edit().putString(Constants.KEY_CACHED_WEATHER, json).apply();
    }

    public String getCachedWeather() {
        return prefs.getString(Constants.KEY_CACHED_WEATHER, null);
    }

    public void setWeatherCacheTimestamp(long timestampMs) {
        prefs.edit().putLong("weather_cached_at", timestampMs).apply();
    }

    public long getWeatherCacheTimestamp() {
        return prefs.getLong("weather_cached_at", 0L);
    }

    // Events created directly from Calendar page — excluded from Task import
    public void addExcludedEventId(String eventId) {
        String existing = prefs.getString("excluded_event_ids", "");
        if (!existing.contains(eventId)) {
            String updated = existing.isEmpty() ? eventId : existing + "," + eventId;
            prefs.edit().putString("excluded_event_ids", updated).apply();
        }
    }

    public boolean isEventExcluded(String eventId) {
        String excluded = prefs.getString("excluded_event_ids", "");
        return excluded.contains(eventId);
    }

    public void removeExcludedEventId(String eventId) {
        String existing = prefs.getString("excluded_event_ids", "");
        String updated = existing.replace(eventId, "").replace(",,", ",");
        if (updated.startsWith(",")) updated = updated.substring(1);
        if (updated.endsWith(",")) updated = updated.substring(0, updated.length() - 1);
        prefs.edit().putString("excluded_event_ids", updated).apply();
    }

    // Calendar cache
    public void cacheCalendarEvents(String yearMonth, String json) {
        prefs.edit().putString("calendar_" + yearMonth, json).apply();
    }

    public String getCachedCalendarEvents(String yearMonth) {
        return prefs.getString("calendar_" + yearMonth, null);
    }

    // Google Calendar ↔ Task sync tracking
    public void setLastCalendarSyncTime(long timestampMs) {
        prefs.edit().putLong("last_calendar_sync", timestampMs).apply();
    }

    public long getLastCalendarSyncTime() {
        return prefs.getLong("last_calendar_sync", 0L);
    }

    /** Mark that the initial full import was done for the current user */
    public void setInitialSyncDone(int userId, boolean done) {
        prefs.edit().putBoolean("initial_sync_done_" + userId, done).apply();
    }

    public boolean isInitialSyncDone(int userId) {
        return prefs.getBoolean("initial_sync_done_" + userId, false);
    }

    // Calendar month cache timestamps (for TTL-based freshness checks)
    public void setCalendarCacheTimestamp(int year, int month, long timestampMs) {
        String key = "calendar_cached_at_" + year + "_" + String.format("%02d", month + 1);
        prefs.edit().putLong(key, timestampMs).apply();
    }

    public long getCalendarCacheTimestamp(int year, int month) {
        String key = "calendar_cached_at_" + year + "_" + String.format("%02d", month + 1);
        return prefs.getLong(key, 0L);
    }
}
