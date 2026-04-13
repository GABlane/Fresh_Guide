package com.example.freshguide.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.BootstrapResponse;
import com.example.freshguide.model.dto.BuildingDto;
import com.example.freshguide.model.dto.FacilityDto;
import com.example.freshguide.model.dto.FloorDto;
import com.example.freshguide.model.dto.OriginDto;
import com.example.freshguide.model.dto.RoomDto;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.dto.SyncVersionResponse;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.RoomFacilityCrossRef;
import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.model.entity.RouteStepEntity;
import com.example.freshguide.model.entity.SyncMetaEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.util.RoomImageCacheManager;
import com.example.freshguide.util.RoomImageUrlResolver;
import com.example.freshguide.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncRepository {

    public interface SyncCallback {
        void onSyncComplete();
        void onSyncSkipped(); // already up to date
        void onSyncError(String message);
    }

    private final ApiService apiService;
    private final AppDatabase db;
    private final SessionManager session;
    private final Context appContext;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SyncRepository(Context context) {
        appContext = context.getApplicationContext();
        apiService = ApiClient.getInstance(appContext).getApiService();
        db = AppDatabase.getInstance(appContext);
        session = SessionManager.getInstance(appContext);
    }

    public void syncIfNeeded(SyncCallback callback) {
        // /api/sync/version returns bare {version, published_at} — no ApiResponse wrapper
        apiService.getSyncVersion().enqueue(new Callback<SyncVersionResponse>() {
            @Override
            public void onResponse(Call<SyncVersionResponse> call, Response<SyncVersionResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onSyncError("Could not check sync version");
                    return;
                }
                int serverVersion = response.body().version;
                int localVersion = session.getSyncVersion();

                if (serverVersion <= localVersion) {
                    executor.execute(() -> {
                        boolean coreDataMissing = isCoreDataMissing();
                        mainHandler.post(() -> {
                            if (coreDataMissing) {
                                fetchAndStore(serverVersion, callback);
                            } else {
                                callback.onSyncSkipped();
                            }
                        });
                    });
                    return;
                }
                fetchAndStore(serverVersion, callback);
            }

            @Override
            public void onFailure(Call<SyncVersionResponse> call, Throwable t) {
                callback.onSyncError("Network error: " + t.getMessage());
            }
        });
    }

    private boolean isCoreDataMissing() {
        return db.buildingDao().count() <= 0
                || db.floorDao().count() <= 0
                || db.roomDao().count() <= 0;
    }

    /**
     * Force-download bootstrap and overwrite local map data even when server version
     * is unchanged. Useful after backend reseeds on the same version.
     */
    public void syncNow(SyncCallback callback) {
        apiService.getBootstrap().enqueue(new Callback<BootstrapResponse>() {
            @Override
            public void onResponse(Call<BootstrapResponse> call, Response<BootstrapResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    callback.onSyncError("Bootstrap download failed");
                    return;
                }
                BootstrapResponse body = response.body();
                int version = body.version > 0 ? body.version : Math.max(1, session.getSyncVersion());
                BootstrapResponse.BootstrapData data = body.data;
                executor.execute(() -> {
                    storeBootstrap(data, version);
                    mainHandler.post(callback::onSyncComplete);
                });
            }

            @Override
            public void onFailure(Call<BootstrapResponse> call, Throwable t) {
                callback.onSyncError("Network error: " + t.getMessage());
            }
        });
    }

    private void fetchAndStore(int serverVersion, SyncCallback callback) {
        // /api/sync/bootstrap returns bare {version, published_at, data: {...}} — no ApiResponse wrapper
        apiService.getBootstrap().enqueue(new Callback<BootstrapResponse>() {
            @Override
            public void onResponse(Call<BootstrapResponse> call, Response<BootstrapResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    callback.onSyncError("Bootstrap download failed");
                    return;
                }
                BootstrapResponse.BootstrapData data = response.body().data;
                executor.execute(() -> {
                    storeBootstrap(data, serverVersion);
                    mainHandler.post(callback::onSyncComplete);
                });
            }

            @Override
            public void onFailure(Call<BootstrapResponse> call, Throwable t) {
                callback.onSyncError("Network error: " + t.getMessage());
            }
        });
    }

    private void storeBootstrap(BootstrapResponse.BootstrapData data, int version) {
        RoomImageCacheManager.clearAllCachedRoomImages(appContext);

        // Clear old data
        db.routeDao().deleteAllSteps();
        db.routeDao().deleteAllRoutes();
        db.facilityDao().deleteAllCrossRefs();
        db.facilityDao().deleteAll();
        db.roomDao().deleteAll();
        db.floorDao().deleteAll();
        db.buildingDao().deleteAll();
        db.originDao().deleteAll();

        // Buildings — nested: building → floors → rooms → facilities
        // Flatten into separate lists for Room DB
        List<BuildingEntity> buildings = new ArrayList<>();
        List<FloorEntity> floors = new ArrayList<>();
        List<RoomEntity> rooms = new ArrayList<>();
        List<RoomFacilityCrossRef> crossRefs = new ArrayList<>();

        if (data.buildings != null) {
            for (BuildingDto b : data.buildings) {
                buildings.add(new BuildingEntity(b.id, b.name, b.code, b.description));

                if (b.floors != null) {
                    for (FloorDto f : b.floors) {
                        // building_id comes from parent — floor DTO may not carry it when nested
                        floors.add(new FloorEntity(f.id, b.id, f.number, f.name));

                        if (f.rooms != null) {
                            for (RoomDto r : f.rooms) {
                                String roomImage = RoomImageUrlResolver.resolveFromDto(appContext, r.imageFullUrl, r.imageUrl);

                                String cachedPath = RoomImageCacheManager.cacheRoomImage(appContext, r.id, roomImage);
                                rooms.add(new RoomEntity(
                                        r.id,
                                        f.id,
                                        r.name,
                                        r.code,
                                        r.type,
                                        r.description,
                                        roomImage,
                                        r.location,
                                        cachedPath
                                ));
                                if (r.facilities != null) {
                                    for (FacilityDto fac : r.facilities) {
                                        crossRefs.add(new RoomFacilityCrossRef(r.id, fac.id));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        db.buildingDao().insertAll(buildings);
        db.floorDao().insertAll(floors);
        db.roomDao().insertAll(rooms);

        // Facilities (flat list — all campus facilities)
        if (data.facilities != null) {
            List<FacilityEntity> facilities = new ArrayList<>();
            for (FacilityDto dto : data.facilities) {
                facilities.add(new FacilityEntity(dto.id, dto.name, dto.icon));
            }
            db.facilityDao().insertAll(facilities);
        }

        // Room-facility cross refs (inserted after both rooms and facilities are stored)
        db.facilityDao().insertCrossRefs(crossRefs);

        // Origins
        if (data.origins != null) {
            List<OriginEntity> origins = new ArrayList<>();
            for (OriginDto dto : data.origins) {
                origins.add(new OriginEntity(dto.id, dto.name, dto.code, dto.description));
            }
            db.originDao().insertAll(origins);
        }

        // Routes + steps
        if (data.routes != null) {
            List<RouteEntity> routeEntities = new ArrayList<>();
            List<RouteStepEntity> stepEntities = new ArrayList<>();
            for (RouteDto dto : data.routes) {
                routeEntities.add(new RouteEntity(dto.id, dto.originId, dto.destinationRoomId, dto.description, dto.instruction));
                if (dto.steps != null) {
                    for (RouteStepDto s : dto.steps) {
                        stepEntities.add(new RouteStepEntity(s.id, s.routeId, s.orderNum, s.instruction, s.direction, s.landmark));
                    }
                }
            }
            db.routeDao().insertRoutes(routeEntities);
            db.routeDao().insertSteps(stepEntities);
        }

        // Sync meta
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        db.syncMetaDao().upsert(new SyncMetaEntity(version, now));
        session.saveSyncVersion(version);
    }
}
