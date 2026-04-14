package com.example.freshguide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

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
    private static final String KEY_PROFILE_MIGRATED_PREFIX = "profile_migrated_";
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
    private static final String KEY_SCHEDULE_REMINDER_RESPONSE_STATUS = "schedule_reminder_response_status";
    private static final String KEY_SCHEDULE_REMINDER_RESPONSE_ID = "schedule_reminder_response_id";
    private static final String KEY_SCHEDULE_REMINDER_RESPONSE_START_AT = "schedule_reminder_response_start_at";
    private static final String KEY_SCHEDULE_REMINDER_RESPONSE_END_AT = "schedule_reminder_response_end_at";

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ADMIN = "admin";
    public static final String VIEW_MODE_WEEKLY = "weekly";
    public static final String VIEW_MODE_DAILY = "daily";
    public static final String SCHEDULE_REMINDER_RESPONSE_GOING = "going";
    public static final String SCHEDULE_REMINDER_RESPONSE_SKIP = "skip";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private String pendingProfilePhotoRef;

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
            Log.e("SessionManager", "EncryptedSharedPreferences initialization failed. Secure storage unavailable.", e);
            // Clear any existing plaintext preferences for security
            SharedPreferences plainPrefs = context.getSharedPreferences(PREFS_FILE + "_plain", Context.MODE_PRIVATE);
            plainPrefs.edit().clear().apply();

            throw new SecurityException("Secure storage unavailable. Please reinstall the app or contact support.", e);
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
        String sanitized = sanitizePersistentProfilePhotoRef(photoUri);
        if (sanitized == null) {
            prefs.edit().remove(KEY_PROFILE_PHOTO_URI).apply();
            return;
        }
        prefs.edit().putString(KEY_PROFILE_PHOTO_URI, sanitized).apply();
    }

    public String getProfilePhotoUri() {
        String stored = prefs.getString(KEY_PROFILE_PHOTO_URI, null);
        String sanitized = sanitizePersistentProfilePhotoRef(stored);
        if (!TextUtils.equals(stored, sanitized)) {
            if (sanitized == null) {
                prefs.edit().remove(KEY_PROFILE_PHOTO_URI).apply();
            } else {
                prefs.edit().putString(KEY_PROFILE_PHOTO_URI, sanitized).apply();
            }
        }
        return sanitized;
    }

    public void setPendingProfilePhotoRef(String photoRef) {
        pendingProfilePhotoRef = photoRef;
    }

    public String getPendingProfilePhotoRef() {
        return pendingProfilePhotoRef;
    }

    public void clearPendingProfilePhotoRef() {
        pendingProfilePhotoRef = null;
    }

    public boolean isProfileMigrated(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        return prefs.getBoolean(KEY_PROFILE_MIGRATED_PREFIX + studentId.trim(), false);
    }

    public void setProfileMigrated(String studentId, boolean migrated) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return;
        }
        prefs.edit().putBoolean(KEY_PROFILE_MIGRATED_PREFIX + studentId.trim(), migrated).apply();
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

    public void setScheduleReminderResponse(int scheduleId,
                                            String status,
                                            long occurrenceStartAtMillis,
                                            long occurrenceEndAtMillis) {
        if (scheduleId <= 0
                || TextUtils.isEmpty(status)
                || occurrenceStartAtMillis <= 0L
                || occurrenceEndAtMillis <= occurrenceStartAtMillis) {
            clearScheduleReminderResponse();
            return;
        }

        prefs.edit()
                .putString(KEY_SCHEDULE_REMINDER_RESPONSE_STATUS, status)
                .putInt(KEY_SCHEDULE_REMINDER_RESPONSE_ID, scheduleId)
                .putLong(KEY_SCHEDULE_REMINDER_RESPONSE_START_AT, occurrenceStartAtMillis)
                .putLong(KEY_SCHEDULE_REMINDER_RESPONSE_END_AT, occurrenceEndAtMillis)
                .apply();
    }

    public void clearScheduleReminderResponse() {
        prefs.edit()
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_STATUS)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_ID)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_START_AT)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_END_AT)
                .apply();
    }

    public void clearScheduleReminderResponseIfMatches(int scheduleId) {
        if (prefs.getInt(KEY_SCHEDULE_REMINDER_RESPONSE_ID, -1) == scheduleId) {
            clearScheduleReminderResponse();
        }
    }

    public ScheduleReminderResponse getActiveScheduleReminderResponse(long nowMillis) {
        boolean hasStoredResponse = prefs.contains(KEY_SCHEDULE_REMINDER_RESPONSE_STATUS)
                || prefs.contains(KEY_SCHEDULE_REMINDER_RESPONSE_ID)
                || prefs.contains(KEY_SCHEDULE_REMINDER_RESPONSE_START_AT)
                || prefs.contains(KEY_SCHEDULE_REMINDER_RESPONSE_END_AT);
        int scheduleId = prefs.getInt(KEY_SCHEDULE_REMINDER_RESPONSE_ID, -1);
        String status = prefs.getString(KEY_SCHEDULE_REMINDER_RESPONSE_STATUS, null);
        long occurrenceStartAtMillis = prefs.getLong(KEY_SCHEDULE_REMINDER_RESPONSE_START_AT, -1L);
        long occurrenceEndAtMillis = prefs.getLong(KEY_SCHEDULE_REMINDER_RESPONSE_END_AT, -1L);

        if (scheduleId <= 0
                || TextUtils.isEmpty(status)
                || occurrenceStartAtMillis <= 0L
                || occurrenceEndAtMillis <= occurrenceStartAtMillis
                || occurrenceEndAtMillis <= nowMillis) {
            if (hasStoredResponse) {
                clearScheduleReminderResponse();
            }
            return null;
        }

        return new ScheduleReminderResponse(
                scheduleId,
                status,
                occurrenceStartAtMillis,
                occurrenceEndAtMillis
        );
    }

    public boolean isScheduleReminderResponsePreferenceKey(String key) {
        return KEY_SCHEDULE_REMINDER_RESPONSE_STATUS.equals(key)
                || KEY_SCHEDULE_REMINDER_RESPONSE_ID.equals(key)
                || KEY_SCHEDULE_REMINDER_RESPONSE_START_AT.equals(key)
                || KEY_SCHEDULE_REMINDER_RESPONSE_END_AT.equals(key);
    }

    public void registerPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener != null) {
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    public void unregisterPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(listener);
        }
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
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_STATUS)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_ID)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_START_AT)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_END_AT)
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
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_STATUS)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_ID)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_START_AT)
                .remove(KEY_SCHEDULE_REMINDER_RESPONSE_END_AT)
                .apply();
        pendingProfilePhotoRef = null;
    }

    public static final class ScheduleReminderResponse {
        public final int scheduleId;
        public final String status;
        public final long occurrenceStartAtMillis;
        public final long occurrenceEndAtMillis;

        private ScheduleReminderResponse(int scheduleId,
                                         String status,
                                         long occurrenceStartAtMillis,
                                         long occurrenceEndAtMillis) {
            this.scheduleId = scheduleId;
            this.status = status;
            this.occurrenceStartAtMillis = occurrenceStartAtMillis;
            this.occurrenceEndAtMillis = occurrenceEndAtMillis;
        }

        public boolean isGoing() {
            return SCHEDULE_REMINDER_RESPONSE_GOING.equals(status);
        }

        public boolean isSkip() {
            return SCHEDULE_REMINDER_RESPONSE_SKIP.equals(status);
        }
    }

    private String sanitizePersistentProfilePhotoRef(String photoRef) {
        if (photoRef == null) {
            return null;
        }

        String trimmed = photoRef.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Temporary picker URIs are not safe to restore after the app restarts.
        if (trimmed.startsWith("content://")) {
            return null;
        }

        return trimmed;
    }
}
