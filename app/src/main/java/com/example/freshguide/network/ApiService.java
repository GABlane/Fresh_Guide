package com.example.freshguide.network;

import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.BootstrapResponse;
import com.example.freshguide.model.dto.BuildingDto;
import com.example.freshguide.model.dto.FacilityDto;
import com.example.freshguide.model.dto.FloorDto;
import com.example.freshguide.model.dto.LoginResponse;
import com.example.freshguide.model.dto.OriginDto;
import com.example.freshguide.model.dto.RoomDto;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.SyncVersionResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @POST("register")
    Call<ApiResponse<LoginResponse>> registerStudent(@Body Map<String, String> body);

    @POST("admin/login")
    Call<ApiResponse<LoginResponse>> adminLogin(@Body Map<String, String> body);

    @POST("logout")
    Call<ApiResponse<Void>> logout();

    // ── Sync ─────────────────────────────────────────────────────────────────

    @GET("sync/version")
    Call<SyncVersionResponse> getSyncVersion();

    @GET("sync/bootstrap")
    Call<BootstrapResponse> getBootstrap();

    // ── Schedule (student) ───────────────────────────────────────────────────

    // ── Rooms (user) ─────────────────────────────────────────────────────────

    @GET("rooms/{id}")
    Call<ApiResponse<RoomDto>> getRoom(@Path("id") int roomId);

    @GET("routes/{roomId}")
    Call<ApiResponse<RouteDto>> getRoute(@Path("roomId") int roomId, @Query("origin_id") int originId);

    // ── Admin — Buildings ─────────────────────────────────────────────────────

    @GET("admin/buildings")
    Call<ApiResponse<List<BuildingDto>>> adminGetBuildings();

    @POST("admin/buildings")
    Call<ApiResponse<BuildingDto>> adminCreateBuilding(@Body Map<String, String> body);

    @PUT("admin/buildings/{id}")
    Call<ApiResponse<BuildingDto>> adminUpdateBuilding(@Path("id") int id, @Body Map<String, String> body);

    @DELETE("admin/buildings/{id}")
    Call<ApiResponse<Void>> adminDeleteBuilding(@Path("id") int id);

    // ── Admin — Floors ────────────────────────────────────────────────────────

    @GET("admin/floors")
    Call<ApiResponse<List<FloorDto>>> adminGetFloors();

    @POST("admin/floors")
    Call<ApiResponse<FloorDto>> adminCreateFloor(@Body Map<String, Object> body);

    @PUT("admin/floors/{id}")
    Call<ApiResponse<FloorDto>> adminUpdateFloor(@Path("id") int id, @Body Map<String, Object> body);

    @DELETE("admin/floors/{id}")
    Call<ApiResponse<Void>> adminDeleteFloor(@Path("id") int id);

    // ── Admin — Rooms ─────────────────────────────────────────────────────────

    @GET("admin/rooms")
    Call<ApiResponse<List<RoomDto>>> adminGetRooms();

    @POST("admin/rooms")
    Call<ApiResponse<RoomDto>> adminCreateRoom(@Body Map<String, Object> body);

    @PUT("admin/rooms/{id}")
    Call<ApiResponse<RoomDto>> adminUpdateRoom(@Path("id") int id, @Body Map<String, Object> body);

    @DELETE("admin/rooms/{id}")
    Call<ApiResponse<Void>> adminDeleteRoom(@Path("id") int id);

    @Multipart
    @POST("admin/rooms/{id}/image")
    Call<ApiResponse<RoomDto>> adminUploadRoomImage(
            @Path("id") int id,
            @Part MultipartBody.Part image
    );

    @DELETE("admin/rooms/{id}/image")
    Call<ApiResponse<RoomDto>> adminDeleteRoomImage(@Path("id") int id);

    // ── Admin — Facilities ────────────────────────────────────────────────────

    @GET("admin/facilities")
    Call<ApiResponse<List<FacilityDto>>> adminGetFacilities();

    @POST("admin/facilities")
    Call<ApiResponse<FacilityDto>> adminCreateFacility(@Body Map<String, String> body);

    @PUT("admin/facilities/{id}")
    Call<ApiResponse<FacilityDto>> adminUpdateFacility(@Path("id") int id, @Body Map<String, String> body);

    @DELETE("admin/facilities/{id}")
    Call<ApiResponse<Void>> adminDeleteFacility(@Path("id") int id);

    // ── Admin — Origins ───────────────────────────────────────────────────────

    @GET("admin/origins")
    Call<ApiResponse<List<OriginDto>>> adminGetOrigins();

    @POST("admin/origins")
    Call<ApiResponse<OriginDto>> adminCreateOrigin(@Body Map<String, String> body);

    @PUT("admin/origins/{id}")
    Call<ApiResponse<OriginDto>> adminUpdateOrigin(@Path("id") int id, @Body Map<String, String> body);

    @DELETE("admin/origins/{id}")
    Call<ApiResponse<Void>> adminDeleteOrigin(@Path("id") int id);

    // ── Admin — Routes ────────────────────────────────────────────────────────

    @GET("admin/routes")
    Call<ApiResponse<List<RouteDto>>> adminGetRoutes();

    @POST("admin/routes")
    Call<ApiResponse<RouteDto>> adminCreateRoute(@Body Map<String, Object> body);

    @PUT("admin/routes/{id}")
    Call<ApiResponse<RouteDto>> adminUpdateRoute(@Path("id") int id, @Body Map<String, Object> body);

    @DELETE("admin/routes/{id}")
    Call<ApiResponse<Void>> adminDeleteRoute(@Path("id") int id);

    // ── Admin — Publish ───────────────────────────────────────────────────────

    @POST("admin/publish")
    Call<ApiResponse<Map<String, Integer>>> publish();
}