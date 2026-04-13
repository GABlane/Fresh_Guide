package com.example.freshguide.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.repository.RoomRepository;

public class DirectionsViewModel extends AndroidViewModel {

    private final MutableLiveData<RouteDto> route = new MutableLiveData<>();
    private final MutableLiveData<String> routeTitle = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final RoomRepository repository;

    public DirectionsViewModel(@NonNull Application application) {
        super(application);
        repository = new RoomRepository(application);
    }

    public LiveData<RouteDto> getRoute() { return route; }
    public LiveData<String> getRouteTitle() { return routeTitle; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadRoute(int roomId, int originId) {
        route.setValue(null);
        routeTitle.setValue(null);
        error.setValue(null);
        loading.setValue(true);
        repository.getRouteTitle(roomId, originId, routeTitle::setValue);
        repository.getRoute(roomId, originId, new RoomRepository.RouteCallback() {
            @Override
            public void onLoaded(RouteDto r) {
                route.setValue(r);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadRoomRoute(int roomId, int originRoomId) {
        route.setValue(null);
        routeTitle.setValue(null);
        error.setValue(null);
        loading.setValue(true);
        repository.getRoomToRoomRouteTitle(roomId, originRoomId, routeTitle::setValue);
        repository.getRoomToRoomRoute(roomId, originRoomId, new RoomRepository.RouteCallback() {
            @Override
            public void onLoaded(RouteDto r) {
                route.setValue(r);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }


}
