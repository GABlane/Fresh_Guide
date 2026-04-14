package com.example.freshguide.receiver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.freshguide.MainActivity;
import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScheduleReminderReceiver extends BroadcastReceiver {

    private static final Executor REMINDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        int scheduleId = intent != null
                ? intent.getIntExtra(ScheduleReminderHelper.EXTRA_SCHEDULE_ID, -1)
                : -1;
        if (scheduleId <= 0) {
            return;
        }

        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        REMINDER_EXECUTOR.execute(() -> {
            try {
                if (!SessionManager.getInstance(appContext).isScheduleNotificationsEnabled()) {
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(appContext);
                ScheduleEntryEntity entry = db.scheduleDao().getByIdSync(scheduleId);
                if (entry == null || entry.reminderMinutes <= 0) {
                    return;
                }

                ScheduleReminderHelper.ensureNotificationChannel(appContext);
                ScheduleReminderHelper.ReminderOccurrenceWindow occurrenceWindow =
                        ScheduleReminderHelper.computeRelevantOccurrenceWindow(
                                entry,
                                System.currentTimeMillis()
                        );
                if (occurrenceWindow.endAtMillis > System.currentTimeMillis()) {
                    showNotification(appContext, entry, occurrenceWindow);
                }
                ScheduleReminderHelper.scheduleReminder(appContext, entry);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void showNotification(Context context,
                                  ScheduleEntryEntity entry,
                                  ScheduleReminderHelper.ReminderOccurrenceWindow occurrenceWindow) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.putExtra("open_tab", "schedule");
        openAppIntent.putExtra(ScheduleReminderHelper.EXTRA_SCHEDULE_ID, entry.id);
        openAppIntent.putExtra(
                ScheduleReminderHelper.EXTRA_OCCURRENCE_START_MILLIS,
                occurrenceWindow.startAtMillis
        );
        openAppIntent.putExtra(
                ScheduleReminderHelper.EXTRA_OCCURRENCE_END_MILLIS,
                occurrenceWindow.endAtMillis
        );
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                entry.id,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String startTime = formatMinutes(entry.startMinutes);
        String title = entry.title != null && !entry.title.isBlank()
                ? entry.title
                : "Your class";
        String content = String.format(
                Locale.getDefault(),
                "%s starts at %s. Tap to update your class status.",
                title,
                startTime
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ScheduleReminderHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_my_schedule)
                .setContentTitle("Class Reminder")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(ScheduleReminderHelper.buildNotificationId(entry.id), builder.build());
    }

    private String formatMinutes(int minutes) {
        int hour24 = Math.max(0, Math.min(23, minutes / 60));
        int minute = Math.max(0, Math.min(59, minutes % 60));
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        String period = hour24 >= 12 ? "PM" : "AM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, period);
    }
}
