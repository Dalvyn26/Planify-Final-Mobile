package com.example.planify.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.planify.data.local.AppDatabase;
import com.example.planify.data.local.TaskDao;
import com.example.planify.data.local.entity.Task;
import com.example.planify.data.remote.model.CalendarEvent;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages one-way sync from Google Calendar → Planify Task database.
 *
 * Two modes:
 *   1. Full import  — triggered on first Google Sign-In, fetches events from 90 days ago to 1 year ahead.
 *   2. Delta sync   — triggered periodically while the app is open, only fetches events
 *                     updated since the last successful sync (using Google's `updatedMin` filter).
 */
public class CalendarSyncManager {

    private static final String TAG = "CalendarSyncManager";

    // Sync every 30 minutes while app is in foreground (was 10 min — too aggressive)
    private static final long SYNC_INTERVAL_MS = 30 * 60 * 1000L;

    // Minimum time between onResume delta syncs (5 minutes cooldown)
    private static final long RESUME_SYNC_COOLDOWN_MS = 5 * 60 * 1000L;

    // Full import range: start of today → 365 days future
    // We intentionally skip past events so a new user doesn't get flooded with old tasks
    private static final long IMPORT_FUTURE_MS = 365L * 24 * 60 * 60 * 1000;

    // In-memory calendar list cache (shared across sync operations)
    private static final long CALENDAR_LIST_TTL_MS = 30 * 60 * 1000L;
    private List<String> cachedCalendarIds = null;
    private long calendarListCachedAt = 0;

    private final Context context;
    private final TaskDao taskDao;
    private final PreferenceManager prefs;
    private final int currentUserId;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Handler syncHandler;
    private Runnable syncRunnable;
    private boolean isSyncRunning = false;
    private long lastResumeSyncTime = 0;

    public interface SyncCallback {
        void onSyncComplete(int imported);
        void onSyncError(String message);
    }

    public CalendarSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.taskDao = AppDatabase.getInstance(context).taskDao();
        this.prefs = PreferenceManager.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.syncHandler = new Handler(Looper.getMainLooper());

        android.content.SharedPreferences sp =
                context.getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
        this.currentUserId = sp.getInt("user_id", 0);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Run the initial full import if it hasn't been done yet for this user.
     * Call this right after a successful Google Sign-In.
     */
    public void runInitialImportIfNeeded(SyncCallback callback) {
        // Guard: skip if not signed in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.d(TAG, "Not signed in, skipping sync");
            return;
        }

        if (prefs.isInitialSyncDone(currentUserId)) {
            Log.d(TAG, "Initial sync already done for user " + currentUserId);
            // Cooldown check: don't run delta sync too frequently on resume
            long now = System.currentTimeMillis();
            if ((now - lastResumeSyncTime) < RESUME_SYNC_COOLDOWN_MS) {
                Log.d(TAG, "Delta sync cooldown active, skipping");
                return;
            }
            lastResumeSyncTime = now;
            runDeltaSync(callback);
            return;
        }

        Log.d(TAG, "Starting initial full import for user " + currentUserId);
        runFullImport(callback);
    }

    /**
     * Start automatic periodic delta sync (every SYNC_INTERVAL_MS).
     * Call this from MainActivity.onResume() / onCreate().
     */
    public void startPeriodicSync() {
        stopPeriodicSync();
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Periodic sync triggered");
                runDeltaSync(null);
                syncHandler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        };
        syncHandler.postDelayed(syncRunnable, SYNC_INTERVAL_MS);
        Log.d(TAG, "Periodic sync started (interval: " + (SYNC_INTERVAL_MS / 60000) + " min)");
    }

    /**
     * Stop the periodic sync. Call from MainActivity.onPause() or onDestroy().
     */
    public void stopPeriodicSync() {
        if (syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
            syncRunnable = null;
        }
    }

    // =========================================================================
    // Core sync logic
    // =========================================================================

    /**
     * Full import: fetch all events from [now - 90d] to [now + 365d].
     * Marks each event with its Google Calendar ID so we don't duplicate.
     */
    private void runFullImport(SyncCallback callback) {
        if (isSyncRunning) return;
        isSyncRunning = true;

        executor.execute(() -> {
            try {
                Calendar service = getCalendarService();
                if (service == null) {
                    mainHandler.post(() -> {
                        isSyncRunning = false;
                        if (callback != null) callback.onSyncError("Not signed in to Google");
                    });
                    return;
                }

                long now = System.currentTimeMillis();
                // timeMin = start of today — skip all events that are already in the past
                java.util.Calendar todayCal = java.util.Calendar.getInstance();
                todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                todayCal.set(java.util.Calendar.MINUTE, 0);
                todayCal.set(java.util.Calendar.SECOND, 0);
                todayCal.set(java.util.Calendar.MILLISECOND, 0);
                DateTime timeMin = new DateTime(todayCal.getTimeInMillis());
                DateTime timeMax = new DateTime(now + IMPORT_FUTURE_MS);

                List<CalendarEvent> events = fetchEvents(service, timeMin, timeMax, null);
                int imported = importEventsToTasks(events);

                prefs.setInitialSyncDone(currentUserId, true);
                prefs.setLastCalendarSyncTime(now);

                Log.d(TAG, "Full import done: " + imported + " events imported");
                mainHandler.post(() -> {
                    isSyncRunning = false;
                    if (callback != null) callback.onSyncComplete(imported);
                });
            } catch (Exception e) {
                Log.e(TAG, "Full import failed", e);
                mainHandler.post(() -> {
                    isSyncRunning = false;
                    if (callback != null) callback.onSyncError(e.getMessage());
                });
            }
        });
    }

    /**
     * Delta sync: only fetch events that were created/updated since last sync.
     * Very lightweight — safe to run periodically.
     */
    private void runDeltaSync(SyncCallback callback) {
        if (isSyncRunning) return;
        isSyncRunning = true;

        executor.execute(() -> {
            try {
                Calendar service = getCalendarService();
                if (service == null) {
                    isSyncRunning = false;
                    return; // Silently skip if not signed in
                }

                long lastSync = prefs.getLastCalendarSyncTime();
                long now = System.currentTimeMillis();
                long future = now + IMPORT_FUTURE_MS;

                // updatedMin = last sync time (fetch only events modified after last sync)
                DateTime updatedMin = new DateTime(lastSync > 0 ? lastSync : now - 24 * 60 * 60 * 1000L);
                DateTime timeMax = new DateTime(future);

                List<CalendarEvent> events = fetchEvents(service, null, timeMax, updatedMin);
                int imported = importEventsToTasks(events);

                prefs.setLastCalendarSyncTime(now);

                if (imported > 0) {
                    Log.d(TAG, "Delta sync: " + imported + " new events imported");
                }

                mainHandler.post(() -> {
                    isSyncRunning = false;
                    if (callback != null) callback.onSyncComplete(imported);
                });
            } catch (Exception e) {
                Log.e(TAG, "Delta sync failed", e);
                mainHandler.post(() -> {
                    isSyncRunning = false;
                    if (callback != null) callback.onSyncError(e.getMessage());
                });
            }
        });
    }

    /**
     * Returns cached calendar IDs, or fetches from API if stale/empty.
     */
    private List<String> getCalendarIds(Calendar service) throws Exception {
        long now = System.currentTimeMillis();
        if (cachedCalendarIds != null && (now - calendarListCachedAt) < CALENDAR_LIST_TTL_MS) {
            return cachedCalendarIds;
        }

        com.google.api.services.calendar.model.CalendarList calendarList =
                service.calendarList().list().execute();

        List<String> ids = new ArrayList<>();
        if (calendarList.getItems() != null) {
            for (com.google.api.services.calendar.model.CalendarListEntry entry : calendarList.getItems()) {
                ids.add(entry.getId());
            }
        }

        cachedCalendarIds = ids;
        calendarListCachedAt = now;
        Log.d(TAG, "Calendar list refreshed: " + ids.size() + " calendars");
        return ids;
    }

    /**
     * Fetch events from all user calendars.
     * Uses cached calendar IDs to avoid redundant calendarList().list() calls.
     * @param updatedMin If non-null, only events updated after this time are returned (delta sync).
     */
    private List<CalendarEvent> fetchEvents(Calendar service, DateTime timeMin, DateTime timeMax,
                                             DateTime updatedMin) throws Exception {
        List<CalendarEvent> result = new ArrayList<>();

        // Use cached calendar IDs instead of fetching list every time
        List<String> calendarIds = getCalendarIds(service);

        for (String calId : calendarIds) {
            try {
                com.google.api.services.calendar.Calendar.Events.List request =
                        service.events().list(calId)
                                .setSingleEvents(true)
                                .setOrderBy("startTime");

                if (timeMin != null) request.setTimeMin(timeMin);
                if (timeMax != null) request.setTimeMax(timeMax);
                if (updatedMin != null) request.setUpdatedMin(updatedMin);

                Events events = request.execute();

                if (events.getItems() != null) {
                    for (Event e : events.getItems()) {
                        // Skip cancelled events
                        if ("cancelled".equals(e.getStatus())) continue;
                        // Skip all-day events with no specific time (no actionable deadline)
                        if (e.getStart() == null || e.getStart().getDateTime() == null) continue;

                        CalendarEvent ce = new CalendarEvent();
                        ce.id = e.getId();
                        ce.title = e.getSummary() != null ? e.getSummary() : "Untitled Event";
                        ce.description = e.getDescription() != null ? e.getDescription() : "";
                        ce.startTime = e.getStart().getDateTime().getValue();
                        ce.endTime = e.getEnd() != null && e.getEnd().getDateTime() != null
                                ? e.getEnd().getDateTime().getValue()
                                : ce.startTime + 3600000L; // Default 1 hour duration
                        result.add(ce);
                    }
                }
            } catch (Exception ignored) {
                // If one calendar fails, continue with the rest
                Log.w(TAG, "Failed to fetch from calendar: " + calId, ignored);
            }
        }

        return result;
    }

    /**
     * Insert events into the local Task database, skipping any that already exist.
     * @return number of new tasks actually inserted
     */
    private int importEventsToTasks(List<CalendarEvent> events) {
        int count = 0;
        long now = System.currentTimeMillis();
        for (CalendarEvent event : events) {
            if (event.id == null || event.id.isEmpty()) continue;

            // Skip events that have already ended — no point adding them as tasks
            if (event.endTime > 0 && event.endTime < now) continue;

            // Skip if this event was created directly from Calendar page (not a task)
            if (prefs.isEventExcluded(event.id)) continue;

            // Skip if already exists in DB (by calendarEventId)
            Task existing = taskDao.getTaskByCalendarEventId(event.id);
            if (existing != null) continue;

            Task task = new Task();
            task.title = event.title;
            task.description = event.description;
            task.dueDate = event.endTime; // Use event END as due date
            task.createdAt = System.currentTimeMillis();
            task.isCompleted = false;
            task.isArchived = false;
            task.priority = Constants.PRIORITY_MEDIUM;
            task.pomodoroCount = 0;
            task.userId = currentUserId;
            task.calendarEventId = event.id;

            long newId = taskDao.insertTask(task);
            task.id = (int) newId;

            // Automatically schedule reminders for the imported task if it's in the future
            if (task.dueDate != null && task.dueDate > System.currentTimeMillis()) {
                ReminderManager.scheduleReminders(context, task);
            }

            count++;
        }
        return count;
    }

    // =========================================================================
    // Google Calendar service factory
    // =========================================================================

    private Calendar getCalendarService() {
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
            if (account == null) return null;

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singletonList(CalendarScopes.CALENDAR));
            credential.setSelectedAccountName(account.getEmail());

            return new Calendar.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Planify")
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build Calendar service", e);
            return null;
        }
    }
}
