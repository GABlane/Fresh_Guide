package com.example.freshguide.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "saved_rooms",
        primaryKeys = {"owner_student_id", "room_id"},
        foreignKeys = @ForeignKey(
                entity = RoomEntity.class,
                parentColumns = "id",
                childColumns = "room_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("owner_student_id"),
                @Index("room_id"),
                @Index(value = {"owner_student_id", "saved_at"})
        }
)
public class SavedRoomEntity {

    @NonNull
    @ColumnInfo(name = "owner_student_id")
    public String ownerStudentId;

    @ColumnInfo(name = "room_id")
    public int roomId;

    @ColumnInfo(name = "saved_at")
    public long savedAt;
}
