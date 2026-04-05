package com.example.freshguide.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.ui.RoomSearchResult;

import java.util.List;

@Dao
public interface RoomDao {

    // Satisfies checklist 6.4: SELECT — Read all
    @Query("SELECT * FROM rooms ORDER BY name ASC")
    LiveData<List<RoomEntity>> getAllRooms();

    @Query("SELECT * FROM rooms ORDER BY name ASC")
    List<RoomEntity> getAllRoomsSync();

    @Query("SELECT * FROM rooms WHERE id = :id")
    RoomEntity getByIdSync(int id);

    @Query("SELECT * FROM rooms WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    RoomEntity getByCodeSync(String code);

    @Query("SELECT * FROM rooms WHERE floor_id = :floorId ORDER BY name ASC")
    List<RoomEntity> getByFloorSync(int floorId);

    @Query("SELECT * FROM rooms WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY name ASC")
    LiveData<List<RoomEntity>> search(String query);

    @Query("SELECT r.* FROM rooms r " +
           "JOIN floors f ON r.floor_id = f.id " +
           "JOIN buildings b ON f.building_id = b.id " +
           "WHERE b.code = :buildingCode " +
           "AND (:query = '' OR r.name LIKE '%' || :query || '%' OR r.code LIKE '%' || :query || '%') " +
           "ORDER BY r.name ASC")
    LiveData<List<RoomEntity>> searchByBuilding(String buildingCode, String query);

    @Query("SELECT r.id AS roomId, " +
           "COALESCE(NULLIF(TRIM(r.name), ''), NULLIF(TRIM(r.code), ''), 'Room') AS roomName, " +
           "r.code AS roomCode, " +
           "r.type AS roomType, " +
           "f.number AS floorNumber, " +
           "COALESCE(NULLIF(TRIM(b.name), ''), NULLIF(TRIM(b.code), ''), '') AS buildingName, " +
           "b.code AS buildingCode " +
           "FROM rooms r " +
           "JOIN floors f ON r.floor_id = f.id " +
           "JOIN buildings b ON f.building_id = b.id " +
           "ORDER BY roomName ASC")
    LiveData<List<RoomSearchResult>> getAllSearchResults();

    @Query("SELECT r.id AS roomId, " +
           "COALESCE(NULLIF(TRIM(r.name), ''), NULLIF(TRIM(r.code), ''), 'Room') AS roomName, " +
           "r.code AS roomCode, " +
           "r.type AS roomType, " +
           "f.number AS floorNumber, " +
           "COALESCE(NULLIF(TRIM(b.name), ''), NULLIF(TRIM(b.code), ''), '') AS buildingName, " +
           "b.code AS buildingCode " +
           "FROM rooms r " +
           "JOIN floors f ON r.floor_id = f.id " +
           "JOIN buildings b ON f.building_id = b.id " +
           "WHERE r.name LIKE '%' || :query || '%' OR r.code LIKE '%' || :query || '%' " +
           "ORDER BY roomName ASC")
    LiveData<List<RoomSearchResult>> searchResults(String query);

    @Query("SELECT r.id AS roomId, " +
           "COALESCE(NULLIF(TRIM(r.name), ''), NULLIF(TRIM(r.code), ''), 'Room') AS roomName, " +
           "r.code AS roomCode, " +
           "r.type AS roomType, " +
           "f.number AS floorNumber, " +
           "COALESCE(NULLIF(TRIM(b.name), ''), NULLIF(TRIM(b.code), ''), '') AS buildingName, " +
           "b.code AS buildingCode " +
           "FROM rooms r " +
           "JOIN floors f ON r.floor_id = f.id " +
           "JOIN buildings b ON f.building_id = b.id " +
           "WHERE b.code = :buildingCode " +
           "AND (:query = '' OR r.name LIKE '%' || :query || '%' OR r.code LIKE '%' || :query || '%') " +
           "ORDER BY roomName ASC")
    LiveData<List<RoomSearchResult>> searchResultsByBuilding(String buildingCode, String query);

    // Satisfies checklist 6.3: INSERT — Create
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RoomEntity room);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RoomEntity> rooms);

    // Satisfies checklist 6.5: UPDATE
    @Update
    void update(RoomEntity room);

    // Satisfies checklist 6.6: DELETE
    @Delete
    void delete(RoomEntity room);

    @Query("DELETE FROM rooms")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM rooms")
    int count();
}
