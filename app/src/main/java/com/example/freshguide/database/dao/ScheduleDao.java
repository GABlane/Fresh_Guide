package com.example.freshguide.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.freshguide.model.entity.ScheduleEntryEntity;

import java.util.List;

@Dao
public interface ScheduleDao {

    @Query("SELECT * FROM schedule_entries WHERE (owner_student_id = :ownerStudentId OR owner_student_id IS NULL) AND pending_delete = 0 AND day_of_week = :dayOfWeek ORDER BY start_minutes ASC")
    LiveData<List<ScheduleEntryEntity>> observeByDayForOwner(String ownerStudentId, int dayOfWeek);

    @Query("SELECT * FROM schedule_entries WHERE (owner_student_id = :ownerStudentId OR owner_student_id IS NULL) AND pending_delete = 0 ORDER BY day_of_week ASC, start_minutes ASC")
    LiveData<List<ScheduleEntryEntity>> observeAllForOwner(String ownerStudentId);

    @Query("SELECT * FROM schedule_entries WHERE (owner_student_id = :ownerStudentId OR owner_student_id IS NULL) ORDER BY day_of_week ASC, start_minutes ASC")
    List<ScheduleEntryEntity> getAllByOwnerSync(String ownerStudentId);

    @Query("SELECT * FROM schedule_entries WHERE (owner_student_id = :ownerStudentId OR owner_student_id IS NULL) AND pending_delete = 0 ORDER BY day_of_week ASC, start_minutes ASC")
    List<ScheduleEntryEntity> getVisibleByOwnerSync(String ownerStudentId);

    @Query("SELECT * FROM schedule_entries WHERE (owner_student_id = :ownerStudentId OR owner_student_id IS NULL) AND (sync_state != :cleanState OR pending_delete = 1) ORDER BY updated_at ASC")
    List<ScheduleEntryEntity> getPendingForSync(String ownerStudentId, int cleanState);

    @Query("SELECT * FROM schedule_entries WHERE id = :id LIMIT 1")
    ScheduleEntryEntity getByIdSync(int id);

    @Query("SELECT * FROM schedule_entries WHERE owner_student_id = :ownerStudentId AND remote_id = :remoteId LIMIT 1")
    ScheduleEntryEntity getByRemoteIdSync(String ownerStudentId, int remoteId);

    @Query("SELECT * FROM schedule_entries WHERE owner_student_id = :ownerStudentId AND client_uuid = :clientUuid LIMIT 1")
    ScheduleEntryEntity getByClientUuidSync(String ownerStudentId, String clientUuid);

    @Query("SELECT COUNT(*) FROM schedule_entries WHERE (owner_student_id = :ownerStudentId OR owner_student_id IS NULL) AND pending_delete = 0")
    LiveData<Integer> observeCountForOwner(String ownerStudentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ScheduleEntryEntity entry);

    @Update
    void update(ScheduleEntryEntity entry);

    @Delete
    void delete(ScheduleEntryEntity entry);

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM schedule_entries WHERE owner_student_id = :ownerStudentId")
    void deleteAllByOwner(String ownerStudentId);
}
