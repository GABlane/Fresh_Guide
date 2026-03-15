package com.example.freshguide.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "rooms",
    foreignKeys = @ForeignKey(
        entity = FloorEntity.class,
        parentColumns = "id",
        childColumns = "floor_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("floor_id")
)
public class RoomEntity {

    @PrimaryKey
    public int id;

    @ColumnInfo(name = "floor_id")
    public int floorId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "code")
    public String code;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    @ColumnInfo(name = "location")
    public String location;

    public RoomEntity(int id, int floorId, String name, String code, String type, String description, String imageUrl, String location) {
        this.id = id;
        this.floorId = floorId;
        this.name = name;
        this.code = code;
        this.type = type;
        this.description = description;
        this.imageUrl = imageUrl;
        this.location = location;
    }
}
