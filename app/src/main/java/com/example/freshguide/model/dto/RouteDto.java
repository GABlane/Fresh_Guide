package com.example.freshguide.model.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RouteDto {
    @SerializedName("id") public int id;
    @SerializedName("origin_id") public int originId;
    @SerializedName("destination_room_id") public int destinationRoomId;
    @SerializedName("description") public String description;
    @SerializedName("instruction") public String instruction;
    @SerializedName("origin") public OriginDto origin;
    @SerializedName("destination_room") public RoomDto destinationRoom;
    @SerializedName("steps") public List<RouteStepDto> steps;
}
