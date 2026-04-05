package com.example.freshguide.model.ui;

public class RoomSearchResult {

    public int roomId;
    public String roomName;
    public String roomCode;
    public String roomType;
    public int floorNumber;
    public String buildingName;
    public String buildingCode;

    public String getDisplayName() {
        if (roomName != null && !roomName.trim().isEmpty()) {
            return roomName.trim();
        }
        if (roomCode != null && !roomCode.trim().isEmpty()) {
            return roomCode.trim();
        }
        return "Room";
    }

    public String getSubtitle() {
        String floorLabel = getFloorLabel(floorNumber);
        String buildingLabel = buildingName != null ? buildingName.trim() : "";

        if (buildingLabel.isEmpty()) {
            return floorLabel;
        }
        if (floorLabel.isEmpty()) {
            return buildingLabel;
        }
        return floorLabel + " " + buildingLabel;
    }

    private String getFloorLabel(int floor) {
        if (floor <= 0) {
            return "";
        }
        if (floor % 100 >= 11 && floor % 100 <= 13) {
            return floor + "th Floor";
        }
        switch (floor % 10) {
            case 1:
                return floor + "st Floor";
            case 2:
                return floor + "nd Floor";
            case 3:
                return floor + "rd Floor";
            default:
                return floor + "th Floor";
        }
    }
}
