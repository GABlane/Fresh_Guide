package com.example.freshguide.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.repository.SavedRoomRepository;

import java.util.List;

public class SavedRoomsViewModel extends AndroidViewModel {

    private final LiveData<List<RoomSearchResult>> savedRooms;

    public SavedRoomsViewModel(@NonNull Application application) {
        super(application);
        savedRooms = new SavedRoomRepository(application).observeSavedRooms();
    }

    public LiveData<List<RoomSearchResult>> getSavedRooms() {
        return savedRooms;
    }
}
