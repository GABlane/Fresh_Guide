package com.example.freshguide.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.BuildingDto;
import com.example.freshguide.model.dto.FacilityDto;
import com.example.freshguide.model.dto.FloorDto;
import com.example.freshguide.model.dto.OriginDto;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RoomDto;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.repository.BuildingRepository;
import com.example.freshguide.repository.RouteRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminViewModel extends AndroidViewModel {

    private static final Set<String> EXCLUDED_DASHBOARD_ROOM_CODES = new HashSet<>(Arrays.asList(
            "COURT",
            "REG",
            "LIB",
            "ENT",
            "EXIT",
            "MAIN-3-STUDENT-AFFAIRS",
            "MAIN-4-STUDENT-LOUNGE",
            "MAIN-5-AUDIT"
    ));

    private final MutableLiveData<Integer> roomCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> buildingCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> floorCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> routeCount = new MutableLiveData<>(0);
    private final MutableLiveData<List<BuildingDto>> buildings = new MutableLiveData<>();
    private final MutableLiveData<List<FloorDto>> floors = new MutableLiveData<>();
    private final MutableLiveData<List<FacilityDto>> facilities = new MutableLiveData<>();
    private final MutableLiveData<List<OriginDto>> origins = new MutableLiveData<>();
    private final MutableLiveData<List<RouteDto>> routes = new MutableLiveData<>();
    private final MutableLiveData<RouteDto> currentRoute = new MutableLiveData<>();
    private final MutableLiveData<List<OriginEntity>> routeFormOrigins = new MutableLiveData<>();
    private final MutableLiveData<List<RoomEntity>> routeFormRooms = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    private final AppDatabase db;
    private final BuildingRepository buildingRepository;
    private final RouteRepository routeRepository;
    private final ApiService apiService;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AdminViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        buildingRepository = new BuildingRepository(application);
        routeRepository = new RouteRepository(application);
        apiService = ApiClient.getInstance(application).getApiService();
    }

    public LiveData<Integer> getRoomCount() { return roomCount; }
    public LiveData<Integer> getBuildingCount() { return buildingCount; }
    public LiveData<Integer> getFloorCount() { return floorCount; }
    public LiveData<Integer> getRouteCount() { return routeCount; }
    public LiveData<List<BuildingDto>> getBuildings() { return buildings; }
    public LiveData<List<FloorDto>> getFloors() { return floors; }
    public LiveData<List<FacilityDto>> getFacilities() { return facilities; }
    public LiveData<List<OriginDto>> getOrigins() { return origins; }
    public LiveData<List<RouteDto>> getRoutes() { return routes; }
    public LiveData<RouteDto> getCurrentRoute() { return currentRoute; }
    public LiveData<List<OriginEntity>> getRouteFormOrigins() { return routeFormOrigins; }
    public LiveData<List<RoomEntity>> getRouteFormRooms() { return routeFormRooms; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getSuccessMessage() { return successMessage; }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public void loadDashboardCounts() {
        executor.execute(() -> {
            int rooms = countVisibleRooms(db.roomDao().getAllRoomsSync());
            int bldgs = db.buildingDao().count();
            int floorsLocal = db.floorDao().count();
            int routesLocal = db.routeDao().count();
            mainHandler.post(() -> {
                roomCount.setValue(rooms);
                buildingCount.setValue(bldgs);
                floorCount.setValue(floorsLocal);
                routeCount.setValue(routesLocal);
            });
        });

        // Refresh with live admin API counts to avoid stale local cache values.
        apiService.adminGetRooms().enqueue(new Callback<ApiResponse<List<RoomDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RoomDto>>> call,
                                   Response<ApiResponse<List<RoomDto>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }
                roomCount.setValue(countVisibleRoomDtos(response.body().getData()));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RoomDto>>> call, Throwable t) {
                // Keep local fallback count when network call fails.
            }
        });

        apiService.adminGetBuildings().enqueue(new Callback<ApiResponse<List<BuildingDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BuildingDto>>> call,
                                   Response<ApiResponse<List<BuildingDto>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }
                List<BuildingDto> data = response.body().getData();
                buildingCount.setValue(data != null ? data.size() : 0);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BuildingDto>>> call, Throwable t) {
                // Keep local fallback count when network call fails.
            }
        });

        apiService.adminGetFloors().enqueue(new Callback<ApiResponse<List<FloorDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FloorDto>>> call,
                                   Response<ApiResponse<List<FloorDto>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }
                List<FloorDto> data = response.body().getData();
                floorCount.setValue(data != null ? data.size() : 0);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FloorDto>>> call, Throwable t) {
                // Keep local fallback count when network call fails.
            }
        });

        apiService.adminGetRoutes().enqueue(new Callback<ApiResponse<List<RouteDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RouteDto>>> call,
                                   Response<ApiResponse<List<RouteDto>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }
                List<RouteDto> data = response.body().getData();
                routeCount.setValue(data != null ? data.size() : 0);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RouteDto>>> call, Throwable t) {
                // Keep local fallback count when network call fails.
            }
        });
    }

    private int countVisibleRooms(List<RoomEntity> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (RoomEntity room : rooms) {
            if (room == null || isExcludedFromDashboardRoomCount(room.code)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private int countVisibleRoomDtos(List<RoomDto> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (RoomDto room : rooms) {
            if (room == null || isExcludedFromDashboardRoomCount(room.code)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean isExcludedFromDashboardRoomCount(String code) {
        if (code == null) {
            return false;
        }
        return EXCLUDED_DASHBOARD_ROOM_CODES.contains(code.trim().toUpperCase());
    }

    // ── Buildings ─────────────────────────────────────────────────────────────

    public void loadBuildings() {
        loading.setValue(true);
        buildingRepository.adminGetBuildings(new BuildingRepository.RepoCallback<List<BuildingDto>>() {
            @Override
            public void onSuccess(List<BuildingDto> result) {
                buildings.setValue(result);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void createBuilding(String name, String code, String description) {
        loading.setValue(true);
        buildingRepository.adminCreateBuilding(name, code, description,
                new BuildingRepository.RepoCallback<BuildingDto>() {
                    @Override
                    public void onSuccess(BuildingDto result) {
                        successMessage.setValue("Building created");
                        loading.setValue(false);
                        loadBuildings();
                    }

                    @Override
                    public void onError(String message) {
                        error.setValue(message);
                        loading.setValue(false);
                    }
                });
    }

    public void updateBuilding(int id, String name, String code, String description) {
        loading.setValue(true);
        buildingRepository.adminUpdateBuilding(id, name, code, description,
                new BuildingRepository.RepoCallback<BuildingDto>() {
                    @Override
                    public void onSuccess(BuildingDto result) {
                        successMessage.setValue("Building updated");
                        loading.setValue(false);
                        loadBuildings();
                    }

                    @Override
                    public void onError(String message) {
                        error.setValue(message);
                        loading.setValue(false);
                    }
                });
    }

    public void deleteBuilding(int id) {
        loading.setValue(true);
        buildingRepository.adminDeleteBuilding(id, new BuildingRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                successMessage.setValue("Building deleted");
                loading.setValue(false);
                loadBuildings();
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    // ── Floors ────────────────────────────────────────────────────────────────

    public void loadFloors() {
        loading.setValue(true);
        apiService.adminGetFloors().enqueue(new Callback<ApiResponse<List<FloorDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FloorDto>>> call,
                                   Response<ApiResponse<List<FloorDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<FloorDto> data = response.body().getData();
                    floors.setValue(data != null ? data : new java.util.ArrayList<>());
                    loading.setValue(false);
                    return;
                }
                loadFloorsFromLocal("Failed to load floors");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FloorDto>>> call, Throwable t) {
                loadFloorsFromLocal("Network error: " + t.getMessage());
            }
        });
    }

    private void loadFloorsFromLocal(@NonNull String fallbackMessage) {
        executor.execute(() -> {
            List<FloorEntity> localFloors = db.floorDao().getAllSync();
            List<FloorDto> fallbackFloors = new java.util.ArrayList<>();
            if (localFloors != null) {
                for (FloorEntity floor : localFloors) {
                    if (floor == null) {
                        continue;
                    }
                    FloorDto dto = new FloorDto();
                    dto.id = floor.id;
                    dto.buildingId = floor.buildingId;
                    dto.number = floor.number;
                    dto.name = floor.name;
                    fallbackFloors.add(dto);
                }
            }
            mainHandler.post(() -> {
                floors.setValue(fallbackFloors);
                loading.setValue(false);
                if (fallbackFloors.isEmpty()) {
                    error.setValue(fallbackMessage);
                }
            });
        });
    }

    public void createFloor(int buildingId, int number, String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("building_id", buildingId);
        body.put("number", number);
        body.put("name", name);
        apiService.adminCreateFloor(body).enqueue(new Callback<ApiResponse<FloorDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<FloorDto>> call,
                                   Response<ApiResponse<FloorDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    successMessage.setValue("Floor created");
                    loadFloors();
                } else {
                    error.setValue("Failed to save floor");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FloorDto>> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void updateFloor(int id, int buildingId, int number, String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("building_id", buildingId);
        body.put("number", number);
        body.put("name", name);
        apiService.adminUpdateFloor(id, body).enqueue(new Callback<ApiResponse<FloorDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<FloorDto>> call,
                                   Response<ApiResponse<FloorDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    successMessage.setValue("Floor updated");
                    loadFloors();
                } else {
                    error.setValue("Failed to update floor");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FloorDto>> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteFloor(int id) {
        apiService.adminDeleteFloor(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    successMessage.setValue("Floor deleted");
                    loadFloors();
                } else {
                    error.setValue("Delete failed");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    // ── Facilities ────────────────────────────────────────────────────────────

    public void loadFacilities() {
        loading.setValue(true);
        apiService.adminGetFacilities().enqueue(new Callback<ApiResponse<List<FacilityDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FacilityDto>>> call,
                                   Response<ApiResponse<List<FacilityDto>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    facilities.setValue(response.body().getData());
                } else {
                    error.setValue("Failed to load facilities");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FacilityDto>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteFacility(int id) {
        apiService.adminDeleteFacility(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    successMessage.setValue("Facility deleted");
                    loadFacilities();
                } else {
                    error.setValue("Delete failed");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    // ── Origins ───────────────────────────────────────────────────────────────

    public void loadOrigins() {
        loading.setValue(true);
        apiService.adminGetOrigins().enqueue(new Callback<ApiResponse<List<OriginDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OriginDto>>> call,
                                   Response<ApiResponse<List<OriginDto>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    origins.setValue(response.body().getData());
                } else {
                    error.setValue("Failed to load origins");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OriginDto>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteOrigin(int id) {
        apiService.adminDeleteOrigin(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    successMessage.setValue("Origin deleted");
                    loadOrigins();
                } else {
                    error.setValue("Delete failed");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    // ── Routes ────────────────────────────────────────────────────────────────

    public void loadRoutes() {
        loading.setValue(true);
        routeRepository.getRoutes(new RouteRepository.RoutesCallback() {
            @Override
            public void onSuccess(List<RouteDto> routeList) {
                loading.setValue(false);
                routes.setValue(routeList);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void loadSingleRoute(int id) {
        loading.setValue(true);
        routeRepository.getRoute(id, new RouteRepository.RouteCallback() {
            @Override
            public void onSuccess(RouteDto route) {
                loading.setValue(false);
                currentRoute.setValue(route);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void createRoute(RouteDto route) {
        loading.setValue(true);
        routeRepository.createRoute(route, new RouteRepository.RouteCallback() {
            @Override
            public void onSuccess(RouteDto savedRoute) {
                loading.setValue(false);
                successMessage.setValue("Route created");
                loadRoutes();
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void updateRoute(int id, RouteDto route) {
        loading.setValue(true);
        routeRepository.updateRoute(id, route, new RouteRepository.RouteCallback() {
            @Override
            public void onSuccess(RouteDto savedRoute) {
                loading.setValue(false);
                successMessage.setValue("Route updated");
                loadRoutes();
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void deleteRoute(int id) {
        loading.setValue(true);
        routeRepository.deleteRoute(id, new RouteRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
                successMessage.setValue("Route deleted");
                loadRoutes();
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void loadRouteFormOptions() {
        routeRepository.loadFormOptions(new RouteRepository.FormOptionsCallback() {
            @Override
            public void onSuccess(List<OriginEntity> originList, List<RoomEntity> roomList) {
                routeFormOrigins.setValue(originList);
                routeFormRooms.setValue(roomList);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void validateOriginLocal(int originId, RouteRepository.ValidationCallback callback) {
        routeRepository.validateOrigin(originId, callback);
    }

    public void validateRoomLocal(int roomId, RouteRepository.ValidationCallback callback) {
        routeRepository.validateRoom(roomId, callback);
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    public void publish() {
        loading.setValue(true);
        apiService.publish().enqueue(new Callback<ApiResponse<Map<String, Integer>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Integer>>> call,
                                   Response<ApiResponse<Map<String, Integer>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Integer> data = response.body().getData();
                    int version = data != null && data.containsKey("version") ? data.get("version") : 0;
                    successMessage.setValue("Published! Version: " + version);
                } else {
                    error.setValue("Publish failed");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Integer>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
