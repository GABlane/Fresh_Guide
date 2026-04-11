package com.example.freshguide.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.receiver.ScheduleReminderReceiver;

import java.util.Calendar;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ScheduleReminderHelper {

    public static final String CHANNEL_ID = "schedule_reminders";
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    private static final String TAG = "ScheduleReminderHelper";
    private static final Executor REMINDER_EXECUTOR = Executors.newSingleThreadExecutor();

    private ScheduleReminderHelper() {
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Class Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Reminders before your classes start");
        manager.createNotificationChannel(channel);
    }

    public static void scheduleReminder(Context context, ScheduleEntryEntity entry) {
        Context appContext = context.getApplicationContext();
        if (entry == null || entry.id <= 0 || entry.reminderMinutes <= 0) {
            return;
        }
        if (!SessionManager.getInstance(appContext).isScheduleNotificationsEnabled()) {
            return;
        }

        long triggerAtMillis = computeNextReminderMillis(entry.dayOfWeek, entry.startMinutes, entry.reminderMinutes);
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        ensureNotificationChannel(appContext);
        PendingIntent pendingIntent = buildReminderPendingIntent(appContext, entry.id);
        alarmManager.cancel(pendingIntent);

        boolean usingExactAlarm = canUseExactAlarmScheduling(alarmManager);
        try {
            if (usingExactAlarm) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                scheduleInexactReminder(alarmManager, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException securityException) {
            usingExactAlarm = false;
            scheduleInexactReminder(alarmManager, triggerAtMillis, pendingIntent);
            Log.w(TAG, "Exact alarm access unavailable; falling back to inexact reminder", securityException);
        }

        Log.d(TAG, "Scheduled " + (usingExactAlarm ? "exact" : "inexact")
                + " reminder: " + buildReminderScheduledMessage(entry));
    }

    public static boolean canScheduleExactReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }
        return canUseExactAlarmScheduling(alarmManager);
    }

    public static void openExactAlarmSettings(Context context) {
        Context appContext = context.getApplicationContext();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + appContext.getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + appContext.getPackageName()));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            appContext.startActivity(intent);
        } catch (Exception exception) {
            Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallbackIntent.setData(Uri.parse("package:" + appContext.getPackageName()));
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(fallbackIntent);
        }
    }

    public static void cancelReminder(Context context, int scheduleId) {
        if (scheduleId <= 0) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildReminderPendingIntent(context, scheduleId);
        alarmManager.cancel(pendingIntent);
    }

    public static void syncAllReminders(Context context) {
        Context appContext = context.getApplicationContext();
        REMINDER_EXECUTOR.execute(() -> {
            SessionManager sessionManager = SessionManager.getInstance(appContext);
            String ownerKey = sessionManager.getStudentId();
            if (ownerKey == null || ownerKey.isBlank()) {
                return;
            }

            List<ScheduleEntryEntity> schedules = AppDatabase.getInstance(appContext)
                    .scheduleDao()
                    .getVisibleByOwnerSync(ownerKey);

            for (ScheduleEntryEntity entry : schedules) {
                cancelReminder(appContext, entry.id);
                if (sessionManager.isScheduleNotificationsEnabled()) {
                    scheduleReminder(appContext, entry);
                }
            }
        });
    }

    public static String buildReminderScheduledMessage(ScheduleEntryEntity entry) {
        if (entry == null || entry.reminderMinutes <= 0) {
            return "Reminder is off";
        }

        long triggerAtMillis = computeNextReminderMillis(
                entry.dayOfWeek, entry.startMinutes, entry.reminderMinutes);
        Calendar trigger = Calendar.getInstance();
        trigger.setTimeInMillis(triggerAtMillis);

        return String.format(
                Locale.getDefault(),
                "Reminder set for %tA %tI:%tM %Tp",
                trigger, trigger, trigger, trigger
        );
    }

    private static PendingIntent buildReminderPendingIntent(Context context, int scheduleId) {
        Intent intent = new Intent(context, ScheduleReminderReceiver.class);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        return PendingIntent.getBroadcast(
                context,
                scheduleId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static boolean canUseExactAlarmScheduling(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }

    private static void scheduleInexactReminder(AlarmManager alarmManager,
                                                long triggerAtMillis,
                                                PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
            return;
        }
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private static long computeNextReminderMillis(int dayOfWeek, int startMinutes, int reminderMinutes) {
        Calendar now = Calendar.getInstance();
        Calendar trigger = Calendar.getInstance();
        trigger.set(Calendar.DAY_OF_WEEK, toCalendarDay(dayOfWeek));
        trigger.set(Calendar.HOUR_OF_DAY, startMinutes / 60);
        trigger.set(Calendar.MINUTE, startMinutes % 60);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        trigger.add(Calendar.MINUTE, -reminderMinutes);

        if (!trigger.after(now)) {
            trigger.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return trigger.getTimeInMillis();
    }

    private static int toCalendarDay(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return Calendar.MONDAY;
            case 2:
                return Calendar.TUESDAY;
            case 3:
                return Calendar.WEDNESDAY;
            case 4:
                return Calendar.THURSDAY;
            case 5:
                return Calendar.FRIDAY;
            case 6:
                return Calendar.SATURDAY;
            case 7:
                return Calendar.SUNDAY;
            default:
                return Calendar.MONDAY;
        }
    }
}
