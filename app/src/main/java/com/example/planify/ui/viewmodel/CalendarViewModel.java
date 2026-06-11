package com.example.planify.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.planify.data.remote.CalendarApiService;
import com.example.planify.data.remote.model.CalendarEvent;
import com.example.planify.data.repository.CalendarRepository;
import com.example.planify.utils.NetworkUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.List;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarViewModel extends AndroidViewModel {
    private static final String TAG = "CalendarViewModel";

    private final CalendarRepository repository;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public final MutableLiveData<List<CalendarEvent>> monthEvents = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);
    public final MutableLiveData<Exception> error = new MutableLiveData<>();
    private int currentYear, currentMonth;

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        repository = new CalendarRepository(application);
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        currentYear = cal.get(java.util.Calendar.YEAR);
        currentMonth = cal.get(java.util.Calendar.MONTH);
    }

    public void loadMonthEvents(int year, int month) {
        loadMonthEvents(year, month, false);
    }

    public void loadMonthEvents(int year, int month, boolean forceRefresh) {
        this.currentYear = year;
        this.currentMonth = month;

        // Always show cached data immediately (instant UI)
        List<CalendarEvent> cached = repository.getCachedMonthEvents(year, month);
        if (!cached.isEmpty()) {
            monthEvents.setValue(cached);
        }

        // Guard: if user hasn't connected Google Calendar, set offline quietly
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplication());
        if (account == null) {
            isOffline.setValue(true);
            return;
        }

        if (!NetworkUtils.isConnected(getApplication())) {
            isOffline.setValue(true);
            return;
        }

        // Skip API call if cache is still fresh (within 5 min TTL) and not forced
        if (!forceRefresh && repository.isCacheFresh(year, month) && !cached.isEmpty()) {
            Log.d(TAG, "Cache fresh for " + year + "/" + (month + 1) + ", skipping API call");
            isOffline.setValue(false);
            return;
        }

        // Only show loading spinner if we have no data or it's a forced refresh
        if (cached.isEmpty() || forceRefresh) {
            isLoading.setValue(true);
        }

        repository.getMonthEvents(year, month, executor, mainHandler, new CalendarApiService.CalendarCallback() {
            @Override
            public void onSuccess(List<CalendarEvent> events) {
                isLoading.postValue(false);
                isOffline.postValue(false);
                monthEvents.postValue(events);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                isOffline.postValue(true);
                // Only propagate real errors, not "not signed in"
                String msg = e.getMessage();
                if (msg != null && !msg.contains("Not signed in")) {
                    error.postValue(e);
                }
            }
        });
    }

    /**
     * Prefetch surrounding months. Only fetches months that are NOT already cached
     * to avoid unnecessary API calls.
     */
    public void prefetchRange() {
        // Guard: skip if user hasn't connected Google Calendar
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplication());
        if (account == null) return;

        if (!NetworkUtils.isConnected(getApplication())) return;

        Calendar cal = Calendar.getInstance();

        // Load -1 to +1 months only (reduced from -2 to +2)
        for (int i = -1; i <= 1; i++) {
            Calendar target = (Calendar) cal.clone();
            target.add(Calendar.MONTH, i);
            int y = target.get(Calendar.YEAR);
            int m = target.get(Calendar.MONTH);

            // Skip if cache is still fresh — no need to re-fetch
            if (repository.isCacheFresh(y, m)) {
                Log.d(TAG, "Prefetch skip (cache fresh): " + y + "/" + (m + 1));
                continue;
            }

            repository.getMonthEvents(y, m, executor, mainHandler, new CalendarApiService.CalendarCallback() {
                @Override public void onSuccess(List<CalendarEvent> events) {
                    Log.d(TAG, "Prefetched: " + y + "/" + (m + 1) + " (" + events.size() + " events)");
                }
                @Override public void onError(Exception e) {
                    Log.w(TAG, "Prefetch failed: " + y + "/" + (m + 1), e);
                }
            });
        }
    }

    public void createEvent(String title, String description, long startTime, long endTime,
                            CalendarApiService.CreateEventCallback callback) {
        // Validate sign-in before attempting API call
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplication());
        if (account == null) {
            callback.onError(new Exception("Not signed in to Google Calendar"));
            return;
        }
        repository.createEvent(title, description, startTime, endTime, executor, mainHandler, callback);
    }

    public void deleteEvent(String eventId, CalendarApiService.DeleteCallback callback) {
        // Validate sign-in before attempting API call
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplication());
        if (account == null) {
            callback.onError(new Exception("Not signed in to Google Calendar"));
            return;
        }
        repository.deleteEvent(eventId, executor, mainHandler, callback);
    }

    public void updateEvent(String eventId, String title, String description,
                            long startTime, long endTime,
                            CalendarApiService.UpdateEventCallback callback) {
        // Validate sign-in before attempting API call
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplication());
        if (account == null) {
            callback.onError(new Exception("Not signed in to Google Calendar"));
            return;
        }
        repository.updateEvent(eventId, title, description, startTime, endTime, executor, mainHandler, callback);
    }

    /**
     * Force-refresh the current month (e.g. after swipe-to-refresh).
     */
    public void refresh() {
        loadMonthEvents(currentYear, currentMonth, true);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
