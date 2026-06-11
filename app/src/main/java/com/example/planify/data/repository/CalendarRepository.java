package com.example.planify.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.planify.data.remote.CalendarApiService;
import com.example.planify.data.remote.model.CalendarEvent;
import com.example.planify.utils.PreferenceManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class CalendarRepository {
    private static final String TAG = "CalendarRepository";

    // Cache TTL: 5 minutes — prevents redundant API calls within this window
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    // In-memory calendar list cache (calendar IDs rarely change)
    private static final long CALENDAR_LIST_TTL_MS = 30 * 60 * 1000L; // 30 min
    private List<String> cachedCalendarIds = null;
    private long calendarListCachedAt = 0;

    private final Context context;
    private final PreferenceManager prefs;
    private final Gson gson;

    public CalendarRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getInstance(context);
        this.gson = new Gson();
    }

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
            return null;
        }
    }

    /**
     * Returns cached calendar IDs, or fetches from API if stale/empty.
     * This avoids calling calendarList().list() on every single operation.
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

    /** Invalidate the in-memory calendar list cache (e.g., after sign-in change). */
    public void invalidateCalendarListCache() {
        cachedCalendarIds = null;
        calendarListCachedAt = 0;
    }

    /**
     * Check if the cached month events are still fresh (within TTL).
     */
    public boolean isCacheFresh(int year, int month) {
        long cachedAt = prefs.getCalendarCacheTimestamp(year, month);
        if (cachedAt <= 0) return false;
        return (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MS;
    }

    public void getMonthEvents(int year, int month, ExecutorService executor,
                               Handler mainHandler, CalendarApiService.CalendarCallback callback) {
        executor.execute(() -> {
            try {
                Calendar service = getCalendarService();
                if (service == null) {
                    mainHandler.post(() -> callback.onError(new Exception("Not signed in")));
                    return;
                }

                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(year, month, 1, 0, 0, 0);
                long startMs = cal.getTimeInMillis();
                cal.set(year, month + 1, 1, 0, 0, 0);
                long endMs = cal.getTimeInMillis();

                com.google.api.client.util.DateTime timeMin = new com.google.api.client.util.DateTime(startMs);
                com.google.api.client.util.DateTime timeMax = new com.google.api.client.util.DateTime(endMs);

                List<CalendarEvent> result = new ArrayList<>();

                // Use cached calendar IDs instead of calling calendarList().list() every time
                List<String> calendarIds = getCalendarIds(service);

                for (String calId : calendarIds) {
                    try {
                        Events events = service.events().list(calId)
                            .setTimeMin(timeMin)
                            .setTimeMax(timeMax)
                            .setSingleEvents(true)
                            .execute();

                        if (events.getItems() != null) {
                            for (Event e : events.getItems()) {
                                CalendarEvent ce = new CalendarEvent();
                                ce.id = e.getId();
                                ce.title = e.getSummary() != null ? e.getSummary() : "";
                                ce.description = e.getDescription() != null ? e.getDescription() : "";
                                if (e.getStart() != null) {
                                    if (e.getStart().getDateTime() != null) {
                                        ce.startTime = e.getStart().getDateTime().getValue();
                                    } else if (e.getStart().getDate() != null) {
                                        ce.startTime = e.getStart().getDate().getValue();
                                    }
                                }
                                if (e.getEnd() != null) {
                                    if (e.getEnd().getDateTime() != null) {
                                        ce.endTime = e.getEnd().getDateTime().getValue();
                                    } else if (e.getEnd().getDate() != null) {
                                        ce.endTime = e.getEnd().getDate().getValue();
                                    }
                                }
                                ce.colorId = e.getColorId();
                                result.add(ce);
                            }
                        }
                    } catch (Exception ignored) {
                        // If one calendar fails, continue with the rest
                        Log.w(TAG, "Failed to fetch events from calendar: " + calId, ignored);
                    }
                }

                // Sort results by start time since merged from multiple sources
                Collections.sort(result, (a, b) -> Long.compare(a.startTime, b.startTime));

                // Cache events + timestamp
                String yearMonth = year + "_" + String.format("%02d", month + 1);
                prefs.cacheCalendarEvents(yearMonth, gson.toJson(result));
                prefs.setCalendarCacheTimestamp(year, month, System.currentTimeMillis());

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "getMonthEvents failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void createEvent(String title, String description, long startTime, long endTime,
                            ExecutorService executor, Handler mainHandler,
                            CalendarApiService.CreateEventCallback callback) {
        executor.execute(() -> {
            try {
                Calendar service = getCalendarService();
                if (service == null) {
                    mainHandler.post(() -> callback.onError(new Exception("Not signed in")));
                    return;
                }

                // Validate times
                if (endTime <= startTime) {
                    mainHandler.post(() -> callback.onError(new Exception("End time must be after start time")));
                    return;
                }

                Event event = new Event().setSummary(title).setDescription(description);
                EventDateTime start = new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(startTime));
                EventDateTime end = new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(endTime));
                event.setStart(start).setEnd(end);

                Event created = service.events().insert("primary", event).execute();
                Log.d(TAG, "Event created: " + created.getId());
                mainHandler.post(() -> callback.onSuccess(created.getId()));
            } catch (Exception e) {
                Log.e(TAG, "createEvent failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void deleteEvent(String eventId, ExecutorService executor, Handler mainHandler,
                            CalendarApiService.DeleteCallback callback) {
        executor.execute(() -> {
            try {
                Calendar service = getCalendarService();
                if (service == null) {
                    mainHandler.post(() -> callback.onError(new Exception("Not signed in")));
                    return;
                }
                service.events().delete("primary", eventId).execute();
                Log.d(TAG, "Event deleted: " + eventId);
                mainHandler.post(() -> callback.onSuccess());
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                // 404 = event already deleted, 410 = event gone — treat as success
                int statusCode = e.getStatusCode();
                if (statusCode == 404 || statusCode == 410) {
                    Log.w(TAG, "Event already gone (HTTP " + statusCode + "): " + eventId);
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    Log.e(TAG, "deleteEvent failed (HTTP " + statusCode + ")", e);
                    mainHandler.post(() -> callback.onError(e));
                }
            } catch (Exception e) {
                Log.e(TAG, "deleteEvent failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void updateEvent(String eventId, String title, String description,
                            long startTime, long endTime,
                            ExecutorService executor, Handler mainHandler,
                            CalendarApiService.UpdateEventCallback callback) {
        executor.execute(() -> {
            try {
                Calendar service = getCalendarService();
                if (service == null) {
                    mainHandler.post(() -> callback.onError(new Exception("Not signed in")));
                    return;
                }

                // Validate times
                if (endTime <= startTime) {
                    mainHandler.post(() -> callback.onError(new Exception("End time must be after start time")));
                    return;
                }

                // Fetch existing event first, then update its fields
                Event event = service.events().get("primary", eventId).execute();
                event.setSummary(title);
                event.setDescription(description);

                EventDateTime start = new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(startTime));
                EventDateTime end = new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(endTime));
                event.setStart(start).setEnd(end);

                service.events().update("primary", eventId, event).execute();
                Log.d(TAG, "Event updated: " + eventId);
                mainHandler.post(() -> callback.onSuccess());
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                int statusCode = e.getStatusCode();
                Log.e(TAG, "updateEvent failed (HTTP " + statusCode + ")", e);
                mainHandler.post(() -> callback.onError(e));
            } catch (Exception e) {
                Log.e(TAG, "updateEvent failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public List<CalendarEvent> getCachedMonthEvents(int year, int month) {
        String yearMonth = year + "_" + String.format("%02d", month + 1);
        String json = prefs.getCachedCalendarEvents(yearMonth);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<CalendarEvent>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
