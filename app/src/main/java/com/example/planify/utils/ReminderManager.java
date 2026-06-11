package com.example.planify.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.planify.data.local.entity.Task;
import com.example.planify.receiver.ReminderReceiver;

public class ReminderManager {
    private static final String TAG = "ReminderManager";

    public static void scheduleReminders(Context context, Task task) {
        if (task.dueDate == null || task.dueDate <= 0) return;

        long now = System.currentTimeMillis();

        // Only schedule if the due date is in the future
        if (task.dueDate <= now) {
            Log.d(TAG, "Due date is in the past, skipping reminders for: " + task.title);
            return;
        }

        // 1 hour before
        schedule(context, task, task.dueDate - 60 * 60 * 1000, "1 hour", 1000 + task.id);

        // 30 mins before
        schedule(context, task, task.dueDate - 30 * 60 * 1000, "30 minutes", 2000 + task.id);

        // 5 mins before
        schedule(context, task, task.dueDate - 5 * 60 * 1000, "5 minutes", 3000 + task.id);

        Log.d(TAG, "Reminders scheduled for task: " + task.title + " (id=" + task.id + ")");
    }

    /**
     * Cancel all scheduled reminders for a task (e.g., when task is completed or deleted).
     */
    public static void cancelReminders(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Cancel all 3 intervals
        int[] requestCodes = {1000 + taskId, 2000 + taskId, 3000 + taskId};
        for (int code : requestCodes) {
            Intent intent = new Intent(context, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, code, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }

        Log.d(TAG, "Reminders cancelled for taskId: " + taskId);
    }

    private static void schedule(Context context, Task task, long triggerTime, String interval, int requestCode) {
        long now = System.currentTimeMillis();

        // Skip if trigger time is in the past
        if (triggerTime <= now) {
            Log.d(TAG, "Skipping " + interval + " reminder (already passed) for: " + task.title);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Android 12+ (API 31): Check if we can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms — permission not granted. Using inexact alarm.");
                // Fall back to inexact alarm
                Intent intent = new Intent(context, ReminderReceiver.class);
                intent.putExtra("taskId", task.id);
                intent.putExtra("taskTitle", task.title);
                intent.putExtra("interval", interval);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                return;
            }
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("taskId", task.id);
        intent.putExtra("taskTitle", task.title);
        intent.putExtra("interval", interval);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, "Scheduled " + interval + " reminder at " + new java.util.Date(triggerTime) + " for: " + task.title);
        } catch (SecurityException e) {
            // Fallback for devices where exact alarm is restricted
            Log.w(TAG, "SecurityException scheduling exact alarm, using inexact: " + e.getMessage());
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}
