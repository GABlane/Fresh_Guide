package com.example.freshguide.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.repository.SavedRoomRepository;

import java.util.List;

public class SavedRoomsViewModel extends AndroidViewModel {

    private final SavedRoomRepository savedRoomRepository;
    private final LiveData<List<RoomSearchResult>> savedRooms;

    public SavedRoomsViewModel(@NonNull Application application) {
        super(application);
        savedRoomRepository = new SavedRoomRepository(application);
        savedRooms = savedRoomRepository.observeSavedRooms();
        savedRoomRepository.syncNow();
    }

    public LiveData<List<RoomSearchResult>> getSavedRooms() {
        return savedRooms;
    }

    public void toggleSaved(int roomId, @NonNull SavedRoomRepository.ToggleCallback callback) {
        savedRoomRepository.toggleSaved(roomId, callback);
    }
}
