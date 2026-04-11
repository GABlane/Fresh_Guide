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
                @Index(value = {"owner_student_id", "saved_at"}),
                @Index("sync_state"),
                @Index(value = {"owner_student_id", "sync_state"})
        }
)
public class SavedRoomEntity {

    public static final int SYNC_STATE_DIRTY = 0;
    public static final int SYNC_STATE_CLEAN = 1;

    @NonNull
    @ColumnInfo(name = "owner_student_id")
    public String ownerStudentId;

    @ColumnInfo(name = "room_id")
    public int roomId;

    @ColumnInfo(name = "saved_at")
    public long savedAt;

    @ColumnInfo(name = "sync_state", defaultValue = "0")
    public int syncState;

    @ColumnInfo(name = "pending_delete", defaultValue = "0")
    public int pendingDelete;

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    public long updatedAt;
}
