package com.example.freshguide.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.database.dao.OriginDao;
import com.example.freshguide.database.dao.RoomDao;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteRepository {

    public interface RouteCallback {
        void onSuccess(RouteDto route);
        void onError(String message);
    }

    public interface RoutesCallback {
        void onSuccess(List<RouteDto> routes);
        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ValidationCallback {
        void onValid(String displayName);
        void onInvalid(String message);
    }

    public interface FormOptionsCallback {
        void onSuccess(List<OriginEntity> origins, List<RoomEntity> rooms);
        void onError(String message);
    }

    private final ApiService apiService;
    private final OriginDao originDao;
    private final RoomDao roomDao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RouteRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.apiService = ApiClient.getInstance(appContext).getApiService();
        this.originDao = db.originDao();
        this.roomDao = db.roomDao();
    }

    public void getRoutes(@NonNull RoutesCallback callback) {
        apiService.adminGetRoutes().enqueue(new Callback<ApiResponse<List<RouteDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RouteDto>>> call,
                                   Response<ApiResponse<List<RouteDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getApiErrorMessage(response, "Failed to load routes"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RouteDto>>> call, Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void getRoute(int id, @NonNull RouteCallback callback) {
        apiService.adminGetRoute(id).enqueue(new Callback<ApiResponse<RouteDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RouteDto>> call,
                                   Response<ApiResponse<RouteDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getApiErrorMessage(response, "Failed to load route"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RouteDto>> call, Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void createRoute(@NonNull RouteDto route, @NonNull RouteCallback callback) {
        apiService.adminCreateRoute(toRouteBody(route)).enqueue(new Callback<ApiResponse<RouteDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RouteDto>> call,
                                   Response<ApiResponse<RouteDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getApiErrorMessage(response, "Failed to create route"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RouteDto>> call, Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void updateRoute(int id, @NonNull RouteDto route, @NonNull RouteCallback callback) {
        apiService.adminUpdateRoute(id, toRouteBody(route)).enqueue(new Callback<ApiResponse<RouteDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RouteDto>> call,
                                   Response<ApiResponse<RouteDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getApiErrorMessage(response, "Failed to update route"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RouteDto>> call, Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void deleteRoute(int id, @NonNull DeleteCallback callback) {
        apiService.adminDeleteRoute(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess();
                } else {
                    callback.onError(getApiErrorMessage(response, "Delete failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void loadFormOptions(@NonNull FormOptionsCallback callback) {
        executor.execute(() -> {
            try {
                List<OriginEntity> origins = originDao.getAllSync();
                List<RoomEntity> rooms = roomDao.getAllRoomsSync();
                mainHandler.post(() -> callback.onSuccess(origins, rooms));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Failed to load local origin/room data"));
            }
        });
    }

    public void validateOrigin(int originId, @NonNull ValidationCallback callback) {
        executor.execute(() -> {
            OriginEntity origin = originDao.getByIdSync(originId);
            mainHandler.post(() -> {
                if (origin != null) {
                    callback.onValid(origin.name != null ? origin.name : ("Origin #" + originId));
                } else {
                    callback.onInvalid("Selected origin is not available locally");
                }
            });
        });
    }

    public void validateRoom(int roomId, @NonNull ValidationCallback callback) {
        executor.execute(() -> {
            RoomEntity room = roomDao.getByIdSync(roomId);
            mainHandler.post(() -> {
                if (room != null) {
                    String display = room.name != null ? room.name : ("Room #" + roomId);
                    callback.onValid(display);
                } else {
                    callback.onInvalid("Selected destination room is not available locally");
                }
            });
        });
    }

    @NonNull
    private Map<String, Object> toRouteBody(@NonNull RouteDto route) {
        Map<String, Object> body = new HashMap<>();
        body.put("origin_id", route.originId);
        body.put("destination_room_id", route.destinationRoomId);
        body.put("description", route.description != null ? route.description.trim() : null);
        body.put("instruction", route.instruction != null ? route.instruction.trim() : null);

        List<Map<String, Object>> stepsBody = new ArrayList<>();
        if (route.steps != null) {
            for (int i = 0; i < route.steps.size(); i++) {
                RouteStepDto step = route.steps.get(i);
                if (step == null) {
                    continue;
                }

                String instruction = step.instruction != null ? step.instruction.trim() : "";
                if (instruction.isEmpty()) {
                    continue;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("order", i + 1);
                map.put("instruction", instruction);
                map.put("direction", normalizeDirection(step.direction));
                map.put("landmark", step.landmark != null && !step.landmark.trim().isEmpty()
                        ? step.landmark.trim()
                        : null);
                stepsBody.add(map);
            }
        }
        body.put("steps", stepsBody);
        return body;
    }

    private String normalizeDirection(String direction) {
        if (direction == null) {
            return null;
        }
        String normalized = direction.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        switch (normalized) {
            case "straight":
            case "left":
            case "right":
            case "up":
            case "down":
                return normalized;
            default:
                return null;
        }
    }

    private <T> String getApiErrorMessage(Response<ApiResponse<T>> response, String fallback) {
        if (response == null || response.body() == null) {
            return fallback;
        }

        ApiResponse<T> body = response.body();
        if (body.getError() != null && !body.getError().trim().isEmpty()) {
            return body.getError().trim();
        }
        if (body.getMessage() != null && !body.getMessage().trim().isEmpty()) {
            return body.getMessage().trim();
        }
        return fallback;
    }

    private String safeMessage(Throwable t) {
        if (t == null || t.getMessage() == null || t.getMessage().trim().isEmpty()) {
            return "Unknown error";
        }
        return t.getMessage().trim();
    }
}
