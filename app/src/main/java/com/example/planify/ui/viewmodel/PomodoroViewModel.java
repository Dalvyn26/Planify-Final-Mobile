package com.example.planify.ui.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.planify.service.PomodoroTimerService;
import com.example.planify.utils.PreferenceManager;

public class PomodoroViewModel extends AndroidViewModel {
    public enum TimerState { IDLE, RUNNING, PAUSED }
    public enum SessionType { WORK, SHORT_BREAK, LONG_BREAK }

    public final MutableLiveData<Long> timeRemainingMs = new MutableLiveData<>();
    public final MutableLiveData<TimerState> timerState = new MutableLiveData<>(TimerState.IDLE);
    public final MutableLiveData<SessionType> sessionType = new MutableLiveData<>(SessionType.WORK);
    public final MutableLiveData<Integer> completedSessions = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> selectedTaskId = new MutableLiveData<>(-1);

    private final PreferenceManager prefs;
    private final Application app;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OnTimerFinishedListener finishedListener;

    // Flag to block stale service callbacks from overwriting manually-set values
    // (e.g., after resetTimer() or stopTimer() sets the correct duration)
    private boolean blockServiceTimeUpdates = false;

    public interface OnTimerFinishedListener {
        void onFinished();
    }

    public PomodoroViewModel(@NonNull Application application) {
        super(application);
        this.app = application;
        prefs = PreferenceManager.getInstance(application);
        resetTimer();
        registerServiceCallback();
    }

    private void registerServiceCallback() {
        PomodoroTimerService.setTimerCallback(new PomodoroTimerService.TimerCallback() {
            @Override
            public void onTick(long remainingMs) {
                if (!blockServiceTimeUpdates) {
                    mainHandler.post(() -> timeRemainingMs.setValue(remainingMs));
                }
            }

            @Override
            public void onStateChanged(int state, long remainingMs) {
                mainHandler.post(() -> {
                    // Never let service push time when we've manually set it locally
                    if (!blockServiceTimeUpdates) {
                        timeRemainingMs.setValue(remainingMs);
                    }
                    switch (state) {
                        case PomodoroTimerService.STATE_RUNNING:
                            // Unblock updates once the timer is truly running
                            blockServiceTimeUpdates = false;
                            timerState.setValue(TimerState.RUNNING);
                            break;
                        case PomodoroTimerService.STATE_PAUSED:
                            timerState.setValue(TimerState.PAUSED);
                            break;
                        default: // IDLE (service stopped)
                            timerState.setValue(TimerState.IDLE);
                            break;
                    }
                });
            }

            @Override
            public void onFinished() {
                mainHandler.post(() -> {
                    timerState.setValue(TimerState.IDLE);
                    // Don't reset time here — advanceToNextSession() will call resetTimer()
                    if (finishedListener != null) finishedListener.onFinished();
                });
            }
        });
    }

    public void setOnTimerFinishedListener(OnTimerFinishedListener listener) {
        this.finishedListener = listener;
    }

    public void startTimer() {
        if (timerState.getValue() == TimerState.RUNNING) return;

        // Unblock service time updates now that we're starting
        blockServiceTimeUpdates = false;

        TimerState currentTimerState = timerState.getValue();

        if (currentTimerState == TimerState.PAUSED) {
            // Resume with current remaining time
            Long currentRemaining = timeRemainingMs.getValue();
            if (currentRemaining == null || currentRemaining <= 0) {
                currentRemaining = getDurationMs(sessionType.getValue());
            }
            Intent intent = new Intent(app, PomodoroTimerService.class);
            intent.setAction(PomodoroTimerService.ACTION_START); // Start with remaining
            intent.putExtra(PomodoroTimerService.EXTRA_DURATION, currentRemaining);
            intent.putExtra(PomodoroTimerService.EXTRA_SESSION_LABEL, getSessionLabel());
            app.startForegroundService(intent);
        } else {
            // Fresh start — use the currently displayed time (set by resetTimer)
            Long currentRemaining = timeRemainingMs.getValue();
            if (currentRemaining == null || currentRemaining <= 0) {
                currentRemaining = getDurationMs(sessionType.getValue());
            }

            Intent intent = new Intent(app, PomodoroTimerService.class);
            intent.setAction(PomodoroTimerService.ACTION_START);
            intent.putExtra(PomodoroTimerService.EXTRA_DURATION, currentRemaining);
            intent.putExtra(PomodoroTimerService.EXTRA_SESSION_LABEL, getSessionLabel());
            app.startForegroundService(intent);
        }

        timerState.setValue(TimerState.RUNNING);
    }

    public void pauseTimer() {
        // Block the PAUSED broadcast from overwriting the time the UI already shows
        blockServiceTimeUpdates = true;

        Intent intent = new Intent(app, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_PAUSE);
        app.startService(intent);
        timerState.setValue(TimerState.PAUSED);
    }

    public void stopTimer() {
        // Block service's IDLE broadcast (it sends remainingMs=0)
        blockServiceTimeUpdates = true;

        Intent intent = new Intent(app, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_STOP);
        app.startService(intent);

        timerState.setValue(TimerState.IDLE);
        sessionType.setValue(SessionType.WORK);
        // Reset to WORK duration — must happen AFTER block is set
        timeRemainingMs.setValue(getDurationMs(SessionType.WORK));
        completedSessions.setValue(0);
    }

    /**
     * Stops the timer service WITHOUT resetting session/completedSessions.
     * Used when skipping a session — advanceToNextSession() will handle the transition.
     */
    public void stopAndAdvance() {
        blockServiceTimeUpdates = true;

        Intent intent = new Intent(app, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_STOP);
        app.startService(intent);

        timerState.setValue(TimerState.IDLE);
        // Time will be set by the subsequent resetTimer() call in advanceToNextSession()
    }

    public void resetTimer() {
        // Block service's stale broadcast from overwriting the correct reset value
        blockServiceTimeUpdates = true;

        SessionType current = sessionType.getValue();
        long durationMs = getDurationMs(current != null ? current : SessionType.WORK);
        timeRemainingMs.setValue(durationMs);
        timerState.setValue(TimerState.IDLE);

        // Auto-unblock after a short delay (enough for any pending service broadcast to arrive)
        mainHandler.postDelayed(() -> blockServiceTimeUpdates = false, 500);
    }

    public long getDurationMs(SessionType type) {
        if (type == null) type = SessionType.WORK;
        switch (type) {
            case SHORT_BREAK: return (long) prefs.getPomoShortBreak() * 60 * 1000;
            case LONG_BREAK: return (long) prefs.getPomoLongBreak() * 60 * 1000;
            default: return (long) prefs.getPomoDuration() * 60 * 1000;
        }
    }

    public int getInterval() { return prefs.getPomoInterval(); }

    private String getSessionLabel() {
        SessionType type = sessionType.getValue();
        if (type == null) return "DEEP WORK";
        switch (type) {
            case SHORT_BREAK: return "SHORT BREAK";
            case LONG_BREAK: return "LONG BREAK";
            default: return "DEEP WORK";
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        PomodoroTimerService.setTimerCallback(null);
        mainHandler.removeCallbacksAndMessages(null);
    }
}
