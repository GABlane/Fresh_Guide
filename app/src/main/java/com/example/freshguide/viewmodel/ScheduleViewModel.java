package com.example.freshguide.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.util.ScheduleReminderHelper;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScheduleViewModel extends AndroidViewModel {

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

    private final AppDatabase db;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();

    public ScheduleViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<List<ScheduleEntryEntity>> getSchedulesByDay(int dayOfWeek) {
        return db.scheduleDao().observeByDay(dayOfWeek);
    }

    public LiveData<List<ScheduleEntryEntity>> getAllSchedules() {
        return db.scheduleDao().observeAll();
    }

    public LiveData<Integer> getTotalScheduleCount() {
        return db.scheduleDao().observeCount();
    }

    public void loadRooms(RoomsCallback callback) {
        ioExecutor.execute(() -> {
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();
            callback.onResult(rooms);
        });
    }

    public void saveSchedule(ScheduleEntryEntity entry, OperationCallback callback) {
        ioExecutor.execute(() -> {
            try {
                if (entry.id <= 0) {
                    long id = db.scheduleDao().insert(entry);
                    entry.id = (int) id;
                } else {
                    db.scheduleDao().update(entry);
                }

                ScheduleReminderHelper.cancelReminder(getApplication(), entry.id);
                ScheduleReminderHelper.scheduleReminder(getApplication(), entry);
                callback.onSuccess(entry);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save schedule");
            }
        });
    }

    public void deleteSchedule(ScheduleEntryEntity entry, SimpleCallback callback) {
        ioExecutor.execute(() -> {
            try {
                db.scheduleDao().delete(entry);
                ScheduleReminderHelper.cancelReminder(getApplication(), entry.id);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete schedule");
            }
        });
    }
}
