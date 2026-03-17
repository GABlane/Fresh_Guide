package com.example.freshguide.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.model.entity.RouteStepEntity;

import java.util.List;

@Dao
public interface RouteDao {

    @Query("SELECT * FROM routes WHERE destination_room_id = :roomId AND origin_id = :originId LIMIT 1")
    RouteEntity getRouteSync(int roomId, int originId);

    @Query("SELECT * FROM route_steps WHERE route_id = :routeId ORDER BY order_num ASC")
    List<RouteStepEntity> getStepsForRoute(int routeId);

    @Query("SELECT * FROM routes ORDER BY id ASC")
    List<RouteEntity> getAllRoutesSync();

    @Query("SELECT COUNT(*) FROM routes")
    int count();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoute(RouteEntity route);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoutes(List<RouteEntity> routes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStep(RouteStepEntity step);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSteps(List<RouteStepEntity> steps);

    @Update
    void updateRoute(RouteEntity route);

    @Delete
    void deleteRoute(RouteEntity route);

    @Query("DELETE FROM routes")
    void deleteAllRoutes();

    @Query("DELETE FROM route_steps")
    void deleteAllSteps();
}
