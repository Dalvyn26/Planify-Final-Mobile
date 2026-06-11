package com.example.planify.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.planify.R;
import com.example.planify.ui.activity.MainActivity;
import com.example.planify.utils.Constants;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("taskTitle");
        String interval = intent.getStringExtra("interval");
        int taskId = intent.getIntExtra("taskId", 0);

        if (title == null) title = "Unknown Mission";
        if (interval == null) interval = "soon";

        showNotification(context, title, interval, taskId);
    }

    private void showNotification(Context context, String title, String interval, int taskId) {
        // Tap to open the app
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, taskId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification text based on interval
        String urgencyTag;
        switch (interval) {
            case "5 minutes":
                urgencyTag = "Critical";
                break;
            case "30 minutes":
                urgencyTag = "Warning";
                break;
            default:
                urgencyTag = "Head Up";
                break;
        }

        String contentTitle = "[PLANIFY] " + urgencyTag;
        String contentText = "Mission \"" + title.toUpperCase() + "\" is due in " + interval + "!";
        String bigText = urgencyTag + " — DEADLINE APPROACHING\n\n"
                + "► MISSION: " + title.toUpperCase() + "\n"
                + "► DUE IN: " + interval.toUpperCase() + "\n\n"
                + "Tap to open Planify and take action.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tasks)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(taskId + interval.hashCode(), builder.build());
        }
    }
}
