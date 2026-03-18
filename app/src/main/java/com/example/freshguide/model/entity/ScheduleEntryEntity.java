package com.example.freshguide.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "schedule_entries",
        foreignKeys = @ForeignKey(
                entity = RoomEntity.class,
                parentColumns = "id",
                childColumns = "room_id",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index("day_of_week"),
                @Index("room_id"),
                @Index("client_uuid"),
                @Index("remote_id"),
                @Index("owner_student_id")
        }
)
public class ScheduleEntryEntity {

    public static final int SYNC_STATE_DIRTY = 0;
    public static final int SYNC_STATE_CLEAN = 1;

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "course_code")
    public String courseCode;

    @ColumnInfo(name = "instructor")
    public String instructor;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "color_hex")
    public String colorHex;

    @ColumnInfo(name = "day_of_week")
    public int dayOfWeek;

    @ColumnInfo(name = "start_minutes")
    public int startMinutes;

    @ColumnInfo(name = "end_minutes")
    public int endMinutes;

    @ColumnInfo(name = "is_online")
    public int isOnline;

    @ColumnInfo(name = "room_id")
    public Integer roomId;

    @ColumnInfo(name = "online_platform")
    public String onlinePlatform;

    @ColumnInfo(name = "reminder_minutes")
    public int reminderMinutes;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "remote_id")
    public Integer remoteId;

    @ColumnInfo(name = "client_uuid")
    public String clientUuid;

    @ColumnInfo(name = "owner_student_id")
    public String ownerStudentId;

    @ColumnInfo(name = "sync_state", defaultValue = "0")
    public int syncState;

    @ColumnInfo(name = "pending_delete", defaultValue = "0")
    public int pendingDelete;

    public ScheduleEntryEntity(String title,
                               String courseCode,
                               String instructor,
                               String notes,
                               String colorHex,
                               int dayOfWeek,
                               int startMinutes,
                               int endMinutes,
                               int isOnline,
                               Integer roomId,
                               String onlinePlatform,
                               int reminderMinutes,
                               long createdAt,
                               long updatedAt) {
        this.title = title;
        this.courseCode = courseCode;
        this.instructor = instructor;
        this.notes = notes;
        this.colorHex = colorHex;
        this.dayOfWeek = dayOfWeek;
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
        this.isOnline = isOnline;
        this.roomId = roomId;
        this.onlinePlatform = onlinePlatform;
        this.reminderMinutes = reminderMinutes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.remoteId = null;
        this.clientUuid = null;
        this.ownerStudentId = null;
        this.syncState = SYNC_STATE_DIRTY;
        this.pendingDelete = 0;
    }
}
