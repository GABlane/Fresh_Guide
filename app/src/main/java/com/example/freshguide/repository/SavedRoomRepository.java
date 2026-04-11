package com.example.freshguide.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.database.dao.SavedRoomDao;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.FavoriteRoomDto;
import com.example.freshguide.model.entity.SavedRoomEntity;
import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.util.NetworkUtils;
import com.example.freshguide.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Response;

public class SavedRoomRepository {

    public interface ToggleCallback {
        void onComplete(boolean isSaved);
        void onError(String message);
    }

    private static final String TAG = "SavedRoomRepository";

    private final Context appContext;
    private final SavedRoomDao savedRoomDao;
    private final SessionManager sessionManager;
    private final ApiService apiService;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    public SavedRoomRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        savedRoomDao = db.savedRoomDao();
        sessionManager = SessionManager.getInstance(appContext);
        apiService = ApiClient.getInstance(appContext).getApiService();
    }

    public LiveData<List<RoomSearchResult>> observeSavedRooms() {
        String ownerStudentId = getOwnerStudentId();
        if (ownerStudentId == null) {
            MutableLiveData<List<RoomSearchResult>> empty = new MutableLiveData<>();
            empty.setValue(Collections.emptyList());
            return empty;
        }
        return savedRoomDao.observeSavedRooms(ownerStudentId);
    }

    public LiveData<Boolean> observeIsSaved(int roomId) {
        String ownerStudentId = getOwnerStudentId();
        if (ownerStudentId == null) {
            MutableLiveData<Boolean> empty = new MutableLiveData<>();
            empty.setValue(false);
            return empty;
        }
        return savedRoomDao.observeIsSaved(ownerStudentId, roomId);
    }

    public void toggleSaved(int roomId, @NonNull ToggleCallback callback) {
        String ownerStudentId = getOwnerStudentId();
        if (ownerStudentId == null) {
            mainHandler.post(() -> callback.onError("No logged-in student account"));
            return;
        }

        ioExecutor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                SavedRoomEntity existing = savedRoomDao.getByOwnerAndRoomSync(ownerStudentId, roomId);

                if (existing != null && existing.pendingDelete == 0) {
                    existing.pendingDelete = 1;
                    existing.syncState = SavedRoomEntity.SYNC_STATE_DIRTY;
                    existing.updatedAt = now;
                    savedRoomDao.upsert(existing);
                    mainHandler.post(() -> callback.onComplete(false));
                } else {
                    SavedRoomEntity entity = existing != null ? existing : new SavedRoomEntity();
                    entity.ownerStudentId = ownerStudentId;
                    entity.roomId = roomId;
                    entity.savedAt = now;
                    entity.updatedAt = now;
                    entity.pendingDelete = 0;
                    entity.syncState = SavedRoomEntity.SYNC_STATE_DIRTY;
                    savedRoomDao.upsert(entity);
                    mainHandler.post(() -> callback.onComplete(true));
                }

                syncNowInternal(ownerStudentId);
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unable to update saved locations";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    public void syncNow() {
        ioExecutor.execute(() -> syncNowInternal(getOwnerStudentId()));
    }

    private void syncNowInternal(@Nullable String ownerStudentId) {
        if (ownerStudentId == null || ownerStudentId.isBlank()) {
            return;
        }
        if (!NetworkUtils.isConnected(appContext)) {
            return;
        }
        if (!syncRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            pushPending(ownerStudentId);
            pullRemote(ownerStudentId);
        } catch (Exception e) {
            Log.w(TAG, "Saved room sync failed", e);
        } finally {
            syncRunning.set(false);
        }
    }

    private void pushPending(@NonNull String ownerStudentId) {
        List<SavedRoomEntity> pendingRows = savedRoomDao.getPendingForSync(
                ownerStudentId,
                SavedRoomEntity.SYNC_STATE_CLEAN
        );

        for (SavedRoomEntity pending : pendingRows) {
            if (pending.pendingDelete == 1) {
                try {
                    Response<ApiResponse<Void>> response = apiService.deleteFavorite(pending.roomId).execute();
                    if (isSuccess(response) || response.code() == 404) {
                        savedRoomDao.deleteByOwnerAndRoom(ownerStudentId, pending.roomId);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to push favorite delete: " + pending.roomId, e);
                }
                continue;
            }

            try {
                Response<ApiResponse<FavoriteRoomDto>> response = apiService.saveFavorite(pending.roomId).execute();
                if (isSuccess(response) && response.body() != null && response.body().getData() != null) {
                    upsertFromRemote(ownerStudentId, response.body().getData());
                    continue;
                }
                if (isSuccess(response)) {
                    pending.syncState = SavedRoomEntity.SYNC_STATE_CLEAN;
                    pending.pendingDelete = 0;
                    pending.updatedAt = System.currentTimeMillis();
                    savedRoomDao.upsert(pending);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to push favorite save: " + pending.roomId, e);
            }
        }
    }

    private void pullRemote(@NonNull String ownerStudentId) {
        Response<ApiResponse<List<FavoriteRoomDto>>> response;
        try {
            response = apiService.getFavorites().execute();
        } catch (Exception e) {
            return;
        }

        if (!isSuccess(response) || response.body() == null) {
            return;
        }

        List<FavoriteRoomDto> remoteFavorites = response.body().getData();
        if (remoteFavorites == null) {
            remoteFavorites = Collections.emptyList();
        }

        Set<Integer> remoteRoomIds = new HashSet<>();
        for (FavoriteRoomDto remote : remoteFavorites) {
            if (remote == null || remote.roomId <= 0) {
                continue;
            }
            remoteRoomIds.add(remote.roomId);

            SavedRoomEntity existing = savedRoomDao.getByOwnerAndRoomSync(ownerStudentId, remote.roomId);
            if (existing != null && existing.syncState != SavedRoomEntity.SYNC_STATE_CLEAN) {
                continue;
            }
            upsertFromRemote(ownerStudentId, remote);
        }

        List<SavedRoomEntity> localRows = savedRoomDao.getAllByOwnerSync(ownerStudentId);
        for (SavedRoomEntity local : localRows) {
            if (local.syncState != SavedRoomEntity.SYNC_STATE_CLEAN) {
                continue;
            }
            if (local.pendingDelete == 1) {
                savedRoomDao.deleteByOwnerAndRoom(ownerStudentId, local.roomId);
                continue;
            }
            if (!remoteRoomIds.contains(local.roomId)) {
                savedRoomDao.deleteByOwnerAndRoom(ownerStudentId, local.roomId);
            }
        }
    }

    private void upsertFromRemote(@NonNull String ownerStudentId, @NonNull FavoriteRoomDto remote) {
        if (remote.roomId <= 0) {
            return;
        }

        SavedRoomEntity entity = savedRoomDao.getByOwnerAndRoomSync(ownerStudentId, remote.roomId);
        if (entity == null) {
            entity = new SavedRoomEntity();
            entity.ownerStudentId = ownerStudentId;
            entity.roomId = remote.roomId;
        }

        long defaultSavedAt = entity.savedAt > 0 ? entity.savedAt : System.currentTimeMillis();
        long parsedSavedAt = parseServerTimestamp(remote.savedAt, defaultSavedAt);
        long parsedUpdatedAt = parseServerTimestamp(remote.updatedAt, parsedSavedAt);

        entity.savedAt = parsedSavedAt;
        entity.updatedAt = parsedUpdatedAt;
        entity.pendingDelete = 0;
        entity.syncState = SavedRoomEntity.SYNC_STATE_CLEAN;
        savedRoomDao.upsert(entity);
    }

    private <T> boolean isSuccess(@Nullable Response<ApiResponse<T>> response) {
        return response != null
                && response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess();
    }

    private long parseServerTimestamp(@Nullable String isoTime, long fallback) {
        if (isoTime == null || isoTime.isBlank()) {
            return fallback;
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date parsed = format.parse(isoTime.trim());
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (Exception ignored) {
                // Try next format.
            }
        }

        return fallback;
    }

    @Nullable
    private String getOwnerStudentId() {
        String studentId = sessionManager.getStudentId();
        if (studentId == null) {
            return null;
        }
        String trimmed = studentId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
