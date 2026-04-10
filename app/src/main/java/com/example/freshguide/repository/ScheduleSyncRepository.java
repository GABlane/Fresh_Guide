package com.example.freshguide.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.ScheduleEntryDto;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.util.NetworkUtils;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Response;

public class ScheduleSyncRepository {

    private static final String TAG = "ScheduleSyncRepository";

    public interface RoomsCallback {
        void onResult(List<RoomEntity> rooms);
    }

    public interface OperationCallback {
        void onSuccess(ScheduleEntryEntity savedEntry);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    private final Context appContext;
    private final AppDatabase db;
    private final ApiService api;
    private final SessionManager session;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    public ScheduleSyncRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(appContext);
        api = ApiClient.getInstance(appContext).getApiService();
        session = SessionManager.getInstance(appContext);
    }

    public LiveData<List<ScheduleEntryEntity>> observeAllSchedules() {
        return db.scheduleDao().observeAllForOwner(getOwnerKey());
    }

    public LiveData<List<ScheduleEntryEntity>> observeSchedulesByDay(int dayOfWeek) {
        return db.scheduleDao().observeByDayForOwner(getOwnerKey(), dayOfWeek);
    }

    public LiveData<Integer> observeScheduleCount() {
        return db.scheduleDao().observeCountForOwner(getOwnerKey());
    }

    public void loadRooms(RoomsCallback callback) {
        ioExecutor.execute(() -> {
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();
            mainHandler.post(() -> callback.onResult(rooms));
        });
    }

    public void saveSchedule(ScheduleEntryEntity entry, OperationCallback callback) {
        ioExecutor.execute(() -> {
            String ownerKey = requireOwnerKey();
            if (ownerKey == null) {
                mainHandler.post(() -> callback.onError("No logged-in student account"));
                return;
            }

            try {
                long now = System.currentTimeMillis();
                if (entry.createdAt <= 0) {
                    entry.createdAt = now;
                }
                entry.updatedAt = now;
                entry.ownerStudentId = ownerKey;
                entry.pendingDelete = 0;
                entry.syncState = ScheduleEntryEntity.SYNC_STATE_DIRTY;
                if (entry.clientUuid == null || entry.clientUuid.isBlank()) {
                    entry.clientUuid = UUID.randomUUID().toString();
                }

                if (entry.id <= 0) {
                    long id = db.scheduleDao().insert(entry);
                    entry.id = (int) id;
                } else {
                    db.scheduleDao().update(entry);
                }

                ScheduleReminderHelper.cancelReminder(appContext, entry.id);
                ScheduleReminderHelper.scheduleReminder(appContext, entry);

                mainHandler.post(() -> callback.onSuccess(entry));
                syncNow();
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Failed to save schedule";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    public void deleteSchedule(ScheduleEntryEntity entry, SimpleCallback callback) {
        ioExecutor.execute(() -> {
            String ownerKey = requireOwnerKey();
            if (ownerKey == null) {
                mainHandler.post(() -> callback.onError("No logged-in student account"));
                return;
            }

            try {
                if (entry.remoteId != null) {
                    entry.ownerStudentId = ownerKey;
                    entry.pendingDelete = 1;
                    entry.syncState = ScheduleEntryEntity.SYNC_STATE_DIRTY;
                    entry.updatedAt = System.currentTimeMillis();
                    db.scheduleDao().update(entry);
                } else {
                    db.scheduleDao().delete(entry);
                }

                ScheduleReminderHelper.cancelReminder(appContext, entry.id);
                mainHandler.post(callback::onSuccess);
                syncNow();
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Failed to delete schedule";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    public void syncNow() {
        ioExecutor.execute(() -> syncNowInternal(requireOwnerKey()));
    }

    private void syncNowInternal(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            return;
        }
        if (!NetworkUtils.isConnected(appContext)) {
            return;
        }
        if (!syncRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            pushPending(ownerKey);
            pullRemote(ownerKey);
            refreshReminders(ownerKey);
        } catch (Exception ignored) {
            // Keep local changes pending for retry on next sync opportunity.
        } finally {
            syncRunning.set(false);
        }
    }

    private void pushPending(String ownerKey) {
        List<ScheduleEntryEntity> pending = db.scheduleDao().getPendingForSync(
                ownerKey,
                ScheduleEntryEntity.SYNC_STATE_CLEAN
        );

        for (ScheduleEntryEntity entry : pending) {
            if (entry.ownerStudentId == null || entry.ownerStudentId.isBlank()) {
                entry.ownerStudentId = ownerKey;
            }
            if (entry.clientUuid == null || entry.clientUuid.isBlank()) {
                entry.clientUuid = UUID.randomUUID().toString();
            }

            if (entry.pendingDelete == 1) {
                if (entry.remoteId != null) {
                    try {
                        Response<ApiResponse<Void>> response = api.deleteSchedule(entry.remoteId).execute();
                        if (isSuccess(response) || response.code() == 404) {
                            db.scheduleDao().delete(entry);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to delete schedule remotely: " + entry.id, e);
                        // keep pending for retry
                    }
                } else {
                    db.scheduleDao().delete(entry);
                }
                continue;
            }

            db.scheduleDao().update(entry);

            Map<String, Object> payload = toPayload(entry);
            try {
                if (entry.remoteId == null) {
                    Response<ApiResponse<ScheduleEntryDto>> response = api.createSchedule(payload).execute();
                    if (isSuccess(response) && response.body() != null && response.body().getData() != null) {
                        upsertFromRemote(ownerKey, response.body().getData(), true);
                    }
                } else {
                    Response<ApiResponse<ScheduleEntryDto>> response = api.updateSchedule(entry.remoteId, payload).execute();
                    if (isSuccess(response) && response.body() != null && response.body().getData() != null) {
                        upsertFromRemote(ownerKey, response.body().getData(), true);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync schedule entry: " + entry.id, e);
                // keep dirty and retry later
            }
        }
    }

    private void pullRemote(String ownerKey) {
        Response<ApiResponse<List<ScheduleEntryDto>>> response;
        try {
            response = api.getSchedules().execute();
        } catch (Exception e) {
            return;
        }

        if (!isSuccess(response) || response.body() == null) {
            return;
        }

        List<ScheduleEntryDto> remote = response.body().getData();
        if (remote == null) {
            remote = new ArrayList<>();
        }

        Set<Integer> remoteIds = new HashSet<>();
        for (ScheduleEntryDto dto : remote) {
            remoteIds.add(dto.id);
            upsertFromRemote(ownerKey, dto, false);
        }

        List<ScheduleEntryEntity> localRows = db.scheduleDao().getAllByOwnerSync(ownerKey);
        for (ScheduleEntryEntity local : localRows) {
            if (local.remoteId == null) {
                continue;
            }
            if (local.pendingDelete == 1) {
                continue;
            }
            if (local.syncState != ScheduleEntryEntity.SYNC_STATE_CLEAN) {
                continue;
            }
            if (!remoteIds.contains(local.remoteId)) {
                db.scheduleDao().delete(local);
                ScheduleReminderHelper.cancelReminder(appContext, local.id);
            }
        }
    }

    private void upsertFromRemote(String ownerKey, ScheduleEntryDto dto, boolean fromPush) {
        ScheduleEntryEntity existing = db.scheduleDao().getByRemoteIdSync(ownerKey, dto.id);
        if (existing == null && dto.clientUuid != null && !dto.clientUuid.isBlank()) {
            existing = db.scheduleDao().getByClientUuidSync(ownerKey, dto.clientUuid);
        }

        if (!fromPush && existing != null && existing.syncState != ScheduleEntryEntity.SYNC_STATE_CLEAN && existing.pendingDelete == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        ScheduleEntryEntity target;
        if (existing == null) {
            target = new ScheduleEntryEntity(
                    dto.title,
                    dto.courseCode,
                    dto.instructor,
                    dto.notes,
                    dto.colorHex,
                    dto.dayOfWeek,
                    dto.startMinutes,
                    dto.endMinutes,
                    dto.isOnline ? 1 : 0,
                    dto.roomId,
                    dto.onlinePlatform,
                    dto.reminderMinutes,
                    now,
                    now
            );
            target.ownerStudentId = ownerKey;
            target.clientUuid = (dto.clientUuid != null && !dto.clientUuid.isBlank())
                    ? dto.clientUuid
                    : UUID.randomUUID().toString();
        } else {
            target = existing;
            target.title = dto.title;
            target.courseCode = dto.courseCode;
            target.instructor = dto.instructor;
            target.notes = dto.notes;
            target.colorHex = dto.colorHex;
            target.dayOfWeek = dto.dayOfWeek;
            target.startMinutes = dto.startMinutes;
            target.endMinutes = dto.endMinutes;
            target.isOnline = dto.isOnline ? 1 : 0;
            target.roomId = dto.roomId;
            target.onlinePlatform = dto.onlinePlatform;
            target.reminderMinutes = dto.reminderMinutes;
            target.updatedAt = now;
            if (dto.clientUuid != null && !dto.clientUuid.isBlank()) {
                target.clientUuid = dto.clientUuid;
            }
        }

        target.remoteId = dto.id;
        target.ownerStudentId = ownerKey;
        target.pendingDelete = 0;
        target.syncState = ScheduleEntryEntity.SYNC_STATE_CLEAN;

        if (target.id <= 0) {
            long newId = db.scheduleDao().insert(target);
            target.id = (int) newId;
        } else {
            db.scheduleDao().update(target);
        }
    }

    private void refreshReminders(String ownerKey) {
        List<ScheduleEntryEntity> visibleSchedules = db.scheduleDao().getVisibleByOwnerSync(ownerKey);
        for (ScheduleEntryEntity entry : visibleSchedules) {
            ScheduleReminderHelper.cancelReminder(appContext, entry.id);
            ScheduleReminderHelper.scheduleReminder(appContext, entry);
        }
    }

    private Map<String, Object> toPayload(ScheduleEntryEntity entry) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("client_uuid", entry.clientUuid);
        payload.put("title", entry.title);
        payload.put("course_code", entry.courseCode);
        payload.put("instructor", entry.instructor);
        payload.put("notes", entry.notes);
        payload.put("color_hex", entry.colorHex);
        payload.put("day_of_week", entry.dayOfWeek);
        payload.put("start_minutes", entry.startMinutes);
        payload.put("end_minutes", entry.endMinutes);
        payload.put("is_online", entry.isOnline == 1);
        payload.put("room_id", entry.roomId);
        payload.put("online_platform", entry.onlinePlatform);
        payload.put("reminder_minutes", entry.reminderMinutes);
        return payload;
    }

    private String getOwnerKey() {
        String owner = requireOwnerKey();
        return owner != null ? owner : "__no_owner__";
    }

    private String requireOwnerKey() {
        return session.getStudentId();
    }

    private <T> boolean isSuccess(Response<ApiResponse<T>> response) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess();
    }
}
