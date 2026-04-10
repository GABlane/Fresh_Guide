package com.example.freshguide.receiver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.freshguide.BuildConfig;
import com.example.freshguide.MainActivity;
import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;

import java.util.Locale;
import java.util.concurrent.Executors;

public class ScheduleReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int scheduleId = intent != null
                ? intent.getIntExtra(ScheduleReminderHelper.EXTRA_SCHEDULE_ID, -1)
                : -1;
        if (scheduleId <= 0) {
            return;
        }
        if (!SessionManager.getInstance(context).isScheduleNotificationsEnabled()) {
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            ScheduleEntryEntity entry = db.scheduleDao().getByIdSync(scheduleId);
            if (entry == null) {
                return;
            }

            ScheduleReminderHelper.ensureNotificationChannel(context);
            showNotification(context, entry);
            showDebugToast(context, entry);
            ScheduleReminderHelper.scheduleReminder(context, entry);
        });
    }

    private void showNotification(Context context, ScheduleEntryEntity entry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.putExtra("open_tab", "schedule");
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                entry.id,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String startTime = formatMinutes(entry.startMinutes);
        String content = String.format(Locale.getDefault(), "%s starts at %s", entry.title, startTime);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ScheduleReminderHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_my_schedule)
                .setContentTitle("Class Reminder")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(10000 + entry.id, builder.build());
    }

    private void showDebugToast(Context context, ScheduleEntryEntity entry) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        String message = "Reminder fired: " + entry.title + " at " + formatMinutes(entry.startMinutes);
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show());
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
