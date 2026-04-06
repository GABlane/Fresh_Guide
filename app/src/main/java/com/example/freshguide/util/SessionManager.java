package com.example.freshguide.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {

    private static final String PREFS_FILE = "fresh_guide_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_STUDENT_ID = "student_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_PROFILE_COURSE_SECTION = "profile_course_section";
    private static final String KEY_PROFILE_PHOTO_URI = "profile_photo_uri";
    private static final String KEY_SYNC_VERSION = "sync_version";
    private static final String KEY_SCHEDULE_VIEW_MODE = "schedule_view_mode";
    private static final String KEY_SCHEDULE_SELECTED_DAY = "schedule_selected_day";
    private static final String KEY_DEFAULT_REMINDER_MINUTES = "default_reminder_minutes";
    private static final String KEY_USE_24_HOUR_TIME = "use_24_hour_time";
    private static final String KEY_START_WEEK_ON_SUNDAY = "start_week_on_sunday";
    private static final String KEY_SCHEDULE_NOTIFICATIONS_ENABLED = "schedule_notifications_enabled";
    private static final String KEY_SYNC_ALERTS_ENABLED = "sync_alerts_enabled";
    private static final String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    private static final String KEY_WIFI_ONLY_SYNC = "wifi_only_sync";

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ADMIN = "admin";
    public static final String VIEW_MODE_WEEKLY = "weekly";
    public static final String VIEW_MODE_DAILY = "daily";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        SharedPreferences temp;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            temp = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to plain prefs if encryption fails (should not happen on normal devices)
            temp = context.getSharedPreferences(PREFS_FILE + "_plain", Context.MODE_PRIVATE);
        }
        prefs = temp;
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveSession(String token, String role, String studentId, String userName) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_ROLE, role)
                .putString(KEY_STUDENT_ID, studentId)
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getStudentId() {
        return prefs.getString(KEY_STUDENT_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public void setUserName(String userName) {
        prefs.edit().putString(KEY_USER_NAME, userName).apply();
    }

    public void setProfileCourseSection(String courseSection) {
        prefs.edit().putString(KEY_PROFILE_COURSE_SECTION, courseSection).apply();
    }

    public String getProfileCourseSection() {
        return prefs.getString(KEY_PROFILE_COURSE_SECTION, null);
    }

    public void setProfilePhotoUri(String photoUri) {
        prefs.edit().putString(KEY_PROFILE_PHOTO_URI, photoUri).apply();
    }

    public String getProfilePhotoUri() {
        return prefs.getString(KEY_PROFILE_PHOTO_URI, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(getRole());
    }

    public void saveSyncVersion(int version) {
        prefs.edit().putInt(KEY_SYNC_VERSION, version).apply();
    }

    public int getSyncVersion() {
        return prefs.getInt(KEY_SYNC_VERSION, -1);
    }

    public void setScheduleViewMode(String mode) {
        prefs.edit().putString(KEY_SCHEDULE_VIEW_MODE, mode).apply();
    }

    public String getScheduleViewMode() {
        return prefs.getString(KEY_SCHEDULE_VIEW_MODE, VIEW_MODE_WEEKLY);
    }

    public void setSelectedScheduleDay(int day) {
        prefs.edit().putInt(KEY_SCHEDULE_SELECTED_DAY, day).apply();
    }

    public int getSelectedScheduleDay(int fallbackDay) {
        return prefs.getInt(KEY_SCHEDULE_SELECTED_DAY, fallbackDay);
    }

    public void setDefaultReminderMinutes(int reminderMinutes) {
        prefs.edit().putInt(KEY_DEFAULT_REMINDER_MINUTES, reminderMinutes).apply();
    }

    public int getDefaultReminderMinutes() {
        return prefs.getInt(KEY_DEFAULT_REMINDER_MINUTES, 10);
    }

    public void setUse24HourTime(boolean value) {
        prefs.edit().putBoolean(KEY_USE_24_HOUR_TIME, value).apply();
    }

    public boolean shouldUse24HourTime() {
        return prefs.getBoolean(KEY_USE_24_HOUR_TIME, false);
    }

    public void setStartWeekOnSunday(boolean value) {
        prefs.edit().putBoolean(KEY_START_WEEK_ON_SUNDAY, value).apply();
    }

    public boolean shouldStartWeekOnSunday() {
        return prefs.getBoolean(KEY_START_WEEK_ON_SUNDAY, false);
    }

    public void setScheduleNotificationsEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_SCHEDULE_NOTIFICATIONS_ENABLED, value).apply();
    }

    public boolean isScheduleNotificationsEnabled() {
        return prefs.getBoolean(KEY_SCHEDULE_NOTIFICATIONS_ENABLED, true);
    }

    public void setSyncAlertsEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_SYNC_ALERTS_ENABLED, value).apply();
    }

    public boolean isSyncAlertsEnabled() {
        return prefs.getBoolean(KEY_SYNC_ALERTS_ENABLED, true);
    }

    public void setAutoSyncEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply();
    }

    public boolean isAutoSyncEnabled() {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true);
    }

    public void setWifiOnlySync(boolean value) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY_SYNC, value).apply();
    }

    public boolean isWifiOnlySync() {
        return prefs.getBoolean(KEY_WIFI_ONLY_SYNC, false);
    }

    public void resetAppPreferences() {
        prefs.edit()
                .remove(KEY_SCHEDULE_VIEW_MODE)
                .remove(KEY_SCHEDULE_SELECTED_DAY)
                .remove(KEY_DEFAULT_REMINDER_MINUTES)
                .remove(KEY_USE_24_HOUR_TIME)
                .remove(KEY_START_WEEK_ON_SUNDAY)
                .remove(KEY_SCHEDULE_NOTIFICATIONS_ENABLED)
                .remove(KEY_SYNC_ALERTS_ENABLED)
                .remove(KEY_AUTO_SYNC_ENABLED)
                .remove(KEY_WIFI_ONLY_SYNC)
                .apply();
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_ROLE)
                .remove(KEY_STUDENT_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_PROFILE_COURSE_SECTION)
                .remove(KEY_PROFILE_PHOTO_URI)
                .apply();
    }
}
