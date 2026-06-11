package com.example.planify.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.planify.R;
import com.example.planify.ui.activity.MainActivity;

public class PomodoroTimerService extends Service {

    // Notification
    private static final String CHANNEL_ID = "planify_pomodoro_timer";
    private static final int NOTIFICATION_ID = 2001;

    // Actions
    public static final String ACTION_START = "com.example.planify.POMO_START";
    public static final String ACTION_PAUSE = "com.example.planify.POMO_PAUSE";
    public static final String ACTION_RESUME = "com.example.planify.POMO_RESUME";
    public static final String ACTION_STOP = "com.example.planify.POMO_STOP";
    public static final String ACTION_TOGGLE = "com.example.planify.POMO_TOGGLE";

    // Extras
    public static final String EXTRA_TIME_REMAINING = "time_remaining";
    public static final String EXTRA_TIMER_STATE = "timer_state";
    public static final String EXTRA_SESSION_LABEL = "session_label";
    public static final String EXTRA_DURATION = "duration";

    // Timer states
    public static final int STATE_IDLE = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED = 2;

    private CountDownTimer countDownTimer;
    private long timeRemainingMs = 0;
    private int currentState = STATE_IDLE;
    private String sessionLabel = "DEEP WORK";

    // Static listener for ViewModel communication (no extra dependency needed)
    public interface TimerCallback {
        void onTick(long remainingMs);
        void onStateChanged(int state, long remainingMs);
        void onFinished();
    }

    private static TimerCallback callback;

    public static void setTimerCallback(TimerCallback cb) {
        callback = cb;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case ACTION_START:
                long duration = intent.getLongExtra(EXTRA_DURATION, 25 * 60 * 1000L);
                String label = intent.getStringExtra(EXTRA_SESSION_LABEL);
                if (label != null) sessionLabel = label;
                timeRemainingMs = duration;
                startCountdown();
                break;

            case ACTION_PAUSE:
                pauseCountdown();
                break;

            case ACTION_RESUME:
                startCountdown();
                break;

            case ACTION_TOGGLE:
                if (currentState == STATE_RUNNING) {
                    pauseCountdown();
                } else if (currentState == STATE_PAUSED) {
                    startCountdown();
                }
                break;

            case ACTION_STOP:
                stopCountdown();
                break;
        }

        return START_STICKY;
    }

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();

        currentState = STATE_RUNNING;
        notifyStateChanged();

        countDownTimer = new CountDownTimer(timeRemainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingMs = millisUntilFinished;
                updateNotification();
                if (callback != null) callback.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeRemainingMs = 0;
                currentState = STATE_IDLE;
                if (callback != null) callback.onFinished();
                notifyStateChanged();
                showFinishedNotification();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        }.start();

        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private void pauseCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        currentState = STATE_PAUSED;
        notifyStateChanged();
        updateNotification();
    }

    private void stopCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        currentState = STATE_IDLE;
        timeRemainingMs = 0;
        notifyStateChanged();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void notifyStateChanged() {
        if (callback != null) callback.onStateChanged(currentState, timeRemainingMs);
    }

    // ==================== NOTIFICATION ====================

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "PLANIFY FOCUS TIMER",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Persistent timer notification for Pomodoro focus sessions");
        channel.setShowBadge(true);
        channel.setSound(null, null);
        channel.enableVibration(false);

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        int minutes = (int) (timeRemainingMs / 1000) / 60;
        int seconds = (int) (timeRemainingMs / 1000) % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        // Tap to open app
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.putExtra("navigate_to", "pomodoro");
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Toggle action (Play/Pause)
        Intent toggleIntent = new Intent(this, PomodoroTimerService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getService(this, 1, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String toggleLabel = (currentState == STATE_RUNNING) ? "▐▐  PAUSE" : "►  START";
        int toggleIcon = (currentState == STATE_RUNNING) ? R.drawable.ic_pause : R.drawable.ic_play;
        String statePrefix = (currentState == STATE_PAUSED) ? "⏸ PAUSED" : "● FOCUS";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pomodoro)
                .setContentTitle("[PLANIFY] " + sessionLabel + "  " + timeStr)
                .setContentText(statePrefix + " — Tap to return to mission control")
                .setContentIntent(openPi)
                .setOngoing(true)
                .setShowWhen(false)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(toggleIcon, toggleLabel, togglePi)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(statePrefix + " — " + sessionLabel + "\n⏱ " + timeStr + " REMAINING"))
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void showFinishedNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.putExtra("navigate_to", "pomodoro");
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pomodoro)
                .setContentTitle("[PLANIFY] SESSION COMPLETE")
                .setContentText("✓ " + sessionLabel + " finished! Tap to continue.")
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID + 1, notification);
    }

    // ==================== GETTERS ====================

    public long getTimeRemainingMs() { return timeRemainingMs; }
    public int getCurrentState() { return currentState; }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        currentState = STATE_IDLE;
        notifyStateChanged();
        super.onDestroy();
    }
}
