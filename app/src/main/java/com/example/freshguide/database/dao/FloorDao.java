package com.example.freshguide.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.freshguide.model.entity.FloorEntity;

import java.util.List;

@Dao
public interface FloorDao {

    @Query("SELECT * FROM floors WHERE building_id = :buildingId ORDER BY number ASC")
    List<FloorEntity> getByBuildingSync(int buildingId);

    @Query("SELECT * FROM floors ORDER BY building_id ASC, number ASC")
    List<FloorEntity> getAllSync();

    @Query("SELECT * FROM floors WHERE id = :id")
    FloorEntity getByIdSync(int id);

    @Query("SELECT COUNT(*) FROM floors")
    int count();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FloorEntity floor);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FloorEntity> floors);

    @Update
    void update(FloorEntity floor);

    @Delete
    void delete(FloorEntity floor);

    @Query("DELETE FROM floors")
    void deleteAll();
}
