package com.example.freshguide.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.freshguide.model.entity.SavedRoomEntity;
import com.example.freshguide.model.ui.RoomSearchResult;

import java.util.List;

@Dao
public interface SavedRoomDao {

    @Query("SELECT EXISTS(SELECT 1 FROM saved_rooms WHERE owner_student_id = :ownerStudentId AND room_id = :roomId)")
    LiveData<Boolean> observeIsSaved(String ownerStudentId, int roomId);

    @Query("SELECT EXISTS(SELECT 1 FROM saved_rooms WHERE owner_student_id = :ownerStudentId AND room_id = :roomId)")
    boolean isSavedSync(String ownerStudentId, int roomId);

    @Query("SELECT r.id AS roomId, " +
            "COALESCE(NULLIF(TRIM(r.name), ''), NULLIF(TRIM(r.code), ''), 'Room') AS roomName, " +
            "r.code AS roomCode, " +
            "r.type AS roomType, " +
            "f.number AS floorNumber, " +
            "COALESCE(NULLIF(TRIM(b.name), ''), NULLIF(TRIM(b.code), ''), '') AS buildingName, " +
            "b.code AS buildingCode " +
            "FROM saved_rooms s " +
            "JOIN rooms r ON s.room_id = r.id " +
            "JOIN floors f ON r.floor_id = f.id " +
            "JOIN buildings b ON f.building_id = b.id " +
            "WHERE s.owner_student_id = :ownerStudentId " +
            "ORDER BY s.saved_at DESC, roomName ASC")
    LiveData<List<RoomSearchResult>> observeSavedRooms(String ownerStudentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SavedRoomEntity savedRoom);

    @Query("DELETE FROM saved_rooms WHERE owner_student_id = :ownerStudentId AND room_id = :roomId")
    void delete(String ownerStudentId, int roomId);
}
