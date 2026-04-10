package com.example.freshguide.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.database.dao.SavedRoomDao;
import com.example.freshguide.model.entity.SavedRoomEntity;
import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.util.SessionManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SavedRoomRepository {

    public interface ToggleCallback {
        void onComplete(boolean isSaved);
        void onError(String message);
    }

    private final SavedRoomDao savedRoomDao;
    private final SessionManager sessionManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SavedRoomRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        savedRoomDao = db.savedRoomDao();
        sessionManager = SessionManager.getInstance(context.getApplicationContext());
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

        executor.execute(() -> {
            try {
                boolean isSaved = savedRoomDao.isSavedSync(ownerStudentId, roomId);
                if (isSaved) {
                    savedRoomDao.delete(ownerStudentId, roomId);
                    mainHandler.post(() -> callback.onComplete(false));
                    return;
                }

                SavedRoomEntity entity = new SavedRoomEntity();
                entity.ownerStudentId = ownerStudentId;
                entity.roomId = roomId;
                entity.savedAt = System.currentTimeMillis();
                savedRoomDao.upsert(entity);
                mainHandler.post(() -> callback.onComplete(true));
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unable to update saved locations";
                mainHandler.post(() -> callback.onError(message));
            }
        });
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
