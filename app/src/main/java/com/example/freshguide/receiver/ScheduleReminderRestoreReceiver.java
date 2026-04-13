package com.example.freshguide.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.freshguide.util.ScheduleReminderHelper;

public class ScheduleReminderRestoreReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ScheduleReminderHelper.syncAllReminders(context);
    }
}
