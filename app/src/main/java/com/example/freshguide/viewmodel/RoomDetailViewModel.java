package com.example.freshguide.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.repository.RoomRepository;
import com.example.freshguide.repository.SavedRoomRepository;

import java.util.List;

public class RoomDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<RoomEntity> room = new MutableLiveData<>();
    private final MutableLiveData<List<FacilityEntity>> facilities = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final RoomRepository repository;
    private final SavedRoomRepository savedRoomRepository;
    private final MutableLiveData<Integer> observedRoomId = new MutableLiveData<>(-1);
    private final LiveData<Boolean> isSaved;

    public RoomDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new RoomRepository(application);
        savedRoomRepository = new SavedRoomRepository(application);
        isSaved = Transformations.switchMap(observedRoomId, roomId -> {
            if (roomId == null || roomId <= 0) {
                MutableLiveData<Boolean> empty = new MutableLiveData<>();
                empty.setValue(false);
                return empty;
            }
            return savedRoomRepository.observeIsSaved(roomId);
        });
    }

    public LiveData<RoomEntity> getRoom() { return room; }
    public LiveData<List<FacilityEntity>> getFacilities() { return facilities; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsSaved() { return isSaved; }

    public void loadRoom(int roomId) {
        observedRoomId.setValue(roomId);
        repository.getRoomDetail(roomId, new RoomRepository.RoomDetailCallback() {
            @Override
            public void onLoaded(RoomEntity r, List<FacilityEntity> f) {
                room.setValue(r);
                facilities.setValue(f);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void toggleSaved(@NonNull SavedRoomRepository.ToggleCallback callback) {
        Integer currentRoomId = observedRoomId.getValue();
        if (currentRoomId == null || currentRoomId <= 0) {
            callback.onError("Invalid room");
            return;
        }
        savedRoomRepository.toggleSaved(currentRoomId, callback);
    }
}
