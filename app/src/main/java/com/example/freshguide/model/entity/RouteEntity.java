package com.example.freshguide.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "routes",
    foreignKeys = {
        @ForeignKey(entity = OriginEntity.class,
            parentColumns = "id", childColumns = "origin_id", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = RoomEntity.class,
            parentColumns = "id", childColumns = "destination_room_id", onDelete = ForeignKey.CASCADE)
    },
    indices = {@Index("origin_id"), @Index("destination_room_id")}
)
public class RouteEntity {

    @PrimaryKey
    public int id;

    @ColumnInfo(name = "origin_id")
    public int originId;

    @ColumnInfo(name = "destination_room_id")
    public int destinationRoomId;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "instruction")
    public String instruction;

    public RouteEntity(int id, int originId, int destinationRoomId, String description, String instruction) {
        this.id = id;
        this.originId = originId;
        this.destinationRoomId = destinationRoomId;
        this.description = description;
        this.instruction = instruction;
    }
}
