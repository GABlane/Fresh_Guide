package com.example.freshguide.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.database.dao.FacilityDao;
import com.example.freshguide.database.dao.OriginDao;
import com.example.freshguide.database.dao.RoomDao;
import com.example.freshguide.database.dao.RouteDao;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.model.entity.RouteStepEntity;
import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomRepository {

    public interface RoomDetailCallback {
        void onLoaded(RoomEntity room, List<FacilityEntity> facilities);
        void onError(String message);
    }

    public interface RouteCallback {
        void onLoaded(RouteDto route);
        void onError(String message);
    }

    public interface RouteTitleCallback {
        void onLoaded(String title);
    }

    private final RoomDao roomDao;
    private final FacilityDao facilityDao;
    private final OriginDao originDao;
    private final RouteDao routeDao;
    private final ApiService apiService;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RoomRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        roomDao = db.roomDao();
        facilityDao = db.facilityDao();
        originDao = db.originDao();
        routeDao = db.routeDao();
        apiService = ApiClient.getInstance(context).getApiService();
    }

    public LiveData<List<RoomEntity>> getAllRooms() {
        return roomDao.getAllRooms();
    }

    public LiveData<List<RoomEntity>> searchRooms(String query) {
        return roomDao.search(query);
    }

    public LiveData<List<RoomEntity>> searchRoomsByBuilding(String buildingCode, String query) {
        return roomDao.searchByBuilding(buildingCode, query);
    }

    public LiveData<List<RoomSearchResult>> getAllSearchResults() {
        return roomDao.getAllSearchResults();
    }

    public LiveData<List<RoomSearchResult>> searchRoomResults(String query) {
        return roomDao.searchResults(query);
    }

    public LiveData<List<RoomSearchResult>> searchRoomResultsByBuilding(String buildingCode, String query) {
        return roomDao.searchResultsByBuilding(buildingCode, query);
    }

    public void getRoomDetail(int roomId, RoomDetailCallback callback) {
        executor.execute(() -> {
            RoomEntity room = roomDao.getByIdSync(roomId);
            List<FacilityEntity> facilities = facilityDao.getFacilitiesForRoom(roomId);
            mainHandler.post(() -> {
                if (room != null) {
                    callback.onLoaded(room, facilities);
                } else {
                    callback.onError("Room not found");
                }
            });
        });
    }

    public void getRoute(int roomId, int originId, RouteCallback callback) {
        apiService.getRoute(roomId, originId).enqueue(new Callback<ApiResponse<RouteDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RouteDto>> call,
                                   Response<ApiResponse<RouteDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {
                    callback.onLoaded(response.body().getData());
                } else {
                    loadRouteFromLocal(roomId, originId, callback, getApiErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RouteDto>> call, Throwable t) {
                String message = (t != null && t.getMessage() != null && !t.getMessage().trim().isEmpty())
                        ? "Network error: " + t.getMessage().trim()
                        : "Network error";
                loadRouteFromLocal(roomId, originId, callback, message);
            }
        });
    }

    private void loadRouteFromLocal(int roomId, int originId, RouteCallback callback, String fallbackMessage) {
        executor.execute(() -> {
            RouteEntity localRoute = routeDao.getRouteSync(roomId, originId);
            if (localRoute == null) {
                String message = fallbackMessage != null && !fallbackMessage.trim().isEmpty()
                        ? fallbackMessage
                        : "Route not found";
                mainHandler.post(() -> callback.onError(message));
                return;
            }

            List<RouteStepEntity> localSteps = routeDao.getStepsForRoute(localRoute.id);
            RouteDto dto = new RouteDto();
            dto.id = localRoute.id;
            dto.originId = localRoute.originId;
            dto.destinationRoomId = localRoute.destinationRoomId;
            dto.description = localRoute.description;
            dto.instruction = localRoute.instruction;

            List<RouteStepDto> stepDtos = new java.util.ArrayList<>();
            if (localSteps != null) {
                for (RouteStepEntity step : localSteps) {
                    if (step == null) {
                        continue;
                    }
                    RouteStepDto stepDto = new RouteStepDto();
                    stepDto.id = step.id;
                    stepDto.routeId = step.routeId;
                    stepDto.orderNum = step.orderNum;
                    stepDto.instruction = step.instruction;
                    stepDto.direction = step.direction;
                    stepDto.landmark = step.landmark;
                    stepDtos.add(stepDto);
                }
            }
            dto.steps = stepDtos;

            mainHandler.post(() -> callback.onLoaded(dto));
        });
    }

    private String getApiErrorMessage(Response<ApiResponse<RouteDto>> response) {
        if (response != null && response.body() != null) {
            ApiResponse<RouteDto> body = response.body();
            if (body.getError() != null && !body.getError().trim().isEmpty()) {
                return body.getError().trim();
            }
            if (body.getMessage() != null && !body.getMessage().trim().isEmpty()) {
                return body.getMessage().trim();
            }
        }
        return "Route not found";
    }

    public void getRouteTitle(int roomId, int originId, RouteTitleCallback callback) {
        executor.execute(() -> {
            OriginEntity origin = originDao.getByIdSync(originId);
            RoomEntity room = roomDao.getByIdSync(roomId);
            String originName = (origin != null && origin.name != null && !origin.name.trim().isEmpty())
                    ? origin.name.trim()
                    : "Origin " + originId;
            String roomName = (room != null && room.name != null && !room.name.trim().isEmpty())
                    ? room.name.trim()
                    : "Room " + roomId;
            String title = originName + " → " + roomName;
            mainHandler.post(() -> callback.onLoaded(title));
        });
    }
}
