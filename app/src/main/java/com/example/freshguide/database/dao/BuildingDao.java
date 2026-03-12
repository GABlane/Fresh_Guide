package com.example.freshguide.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.freshguide.model.entity.BuildingEntity;

import java.util.List;

@Dao
public interface BuildingDao {

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    LiveData<List<BuildingEntity>> getAllBuildings();

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    List<BuildingEntity> getAllBuildingsSync();

    @Query("SELECT * FROM buildings WHERE id = :id")
    BuildingEntity getByIdSync(int id);

    @Query("SELECT * FROM buildings WHERE code = :code LIMIT 1")
    BuildingEntity getByCodeSync(String code);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BuildingEntity building);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BuildingEntity> buildings);

    @Update
    void update(BuildingEntity building);

    @Delete
    void delete(BuildingEntity building);

    @Query("DELETE FROM buildings")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM buildings")
    int count();
}
