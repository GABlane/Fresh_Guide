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

    @Query("SELECT * FROM schedule_entries WHERE day_of_week = :dayOfWeek ORDER BY start_minutes ASC")
    LiveData<List<ScheduleEntryEntity>> observeByDay(int dayOfWeek);

    @Query("SELECT * FROM schedule_entries ORDER BY day_of_week ASC, start_minutes ASC")
    LiveData<List<ScheduleEntryEntity>> observeAll();

    @Query("SELECT * FROM schedule_entries ORDER BY day_of_week ASC, start_minutes ASC")
    List<ScheduleEntryEntity> getAllSync();

    @Query("SELECT * FROM schedule_entries WHERE id = :id LIMIT 1")
    ScheduleEntryEntity getByIdSync(int id);

    @Query("SELECT COUNT(*) FROM schedule_entries")
    LiveData<Integer> observeCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ScheduleEntryEntity entry);

    @Update
    void update(ScheduleEntryEntity entry);

    @Delete
    void delete(ScheduleEntryEntity entry);
}
