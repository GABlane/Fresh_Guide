package com.example.freshguide.model.dto;

import com.google.gson.annotations.SerializedName;

public class FavoriteRoomDto {
    @SerializedName("room_id") public int roomId;
    @SerializedName("saved_at") public String savedAt;
    @SerializedName("updated_at") public String updatedAt;
}
