package com.emergencyroutefinder.network;

import com.emergencyroutefinder.model.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

/**
 * Retrofit API interface — all backend REST endpoints.
 * Demonstrates: Interfaces, Network abstraction
 */
public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    @POST("api/auth/register/user")
    Call<AuthResponse> registerUser(@Body Map<String, Object> body);

    @POST("api/auth/register/worker")
    Call<AuthResponse> registerWorker(@Body Map<String, Object> body);

    // ── Emergency Requests ────────────────────────────────────────────────────
    @POST("api/emergency/request")
    Call<EmergencyRequest> createRequest(@Body Map<String, Object> body);

    @GET("api/emergency/{id}")
    Call<EmergencyRequest> getRequest(@Path("id") long id);

    @GET("api/emergency/user/{userId}")
    Call<List<EmergencyRequest>> getRequestsByUser(@Path("userId") long userId);

    @GET("api/emergency/worker/{workerId}")
    Call<List<EmergencyRequest>> getRequestsByWorker(@Path("workerId") long workerId);

    @GET("api/emergency/{id}/history")
    Call<List<StatusHistory>> getStatusHistory(@Path("id") long id);

    @PUT("api/emergency/{id}/status")
    Call<EmergencyRequest> updateStatus(@Path("id") long id, @Body Map<String, String> body);

    @PUT("api/emergency/{id}/hospital")
    Call<Void> selectHospital(@Path("id") long id, @Body Map<String, Long> body);

    // ── Assignments ───────────────────────────────────────────────────────────
    @POST("api/assignments/{id}/accept")
    Call<Map> acceptAssignment(@Path("id") long id, @Body Map<String, Long> body);

    @POST("api/assignments/{id}/decline")
    Call<Map> declineAssignment(@Path("id") long id, @Body Map<String, Object> body);

    @POST("api/assignments/{id}/arrived")
    Call<Map> markArrived(@Path("id") long id, @Body Map<String, Long> body);

    @POST("api/assignments/{id}/complete")
    Call<Map> completeAssignment(@Path("id") long id, @Body Map<String, Long> body);

    @POST("api/assignments/{id}/responding")
    Call<Map> markResponding(@Path("id") long id, @Body Map<String, Long> body);

    // ── Workers ───────────────────────────────────────────────────────────────
    @GET("api/workers/{id}")
    Call<EmergencyWorker> getWorker(@Path("id") long id);

    @PUT("api/workers/{id}/availability")
    Call<Map> setAvailability(@Path("id") long id, @Body Map<String, String> body);

    @PUT("api/workers/{id}/location")
    Call<Map> updateWorkerLocation(@Path("id") long id, @Body Map<String, Object> body);

    @PUT("api/workers/{id}/fcm-token")
    Call<Void> updateWorkerFcmToken(@Path("id") long id, @Body Map<String, String> body);

    // ── Users ─────────────────────────────────────────────────────────────────
    @GET("api/users/{id}")
    Call<User> getUser(@Path("id") long id);

    @PUT("api/users/{id}/location")
    Call<Void> updateUserLocation(@Path("id") long id, @Body Map<String, Double> body);

    @PUT("api/users/{id}/crowd-consent")
    Call<Map> setCrowdConsent(@Path("id") long id, @Body Map<String, Boolean> body);

    @PUT("api/users/{id}/fcm-token")
    Call<Void> updateUserFcmToken(@Path("id") long id, @Body Map<String, String> body);

    @POST("api/users/crowd-location")
    Call<Void> submitCrowdLocation(@Body Map<String, Object> body);

    @GET("api/users/{id}/notifications")
    Call<List<NotificationItem>> getNotifications(@Path("id") long id);

    // ── Routes & Traffic ──────────────────────────────────────────────────────
    @GET("api/routes/{requestId}")
    Call<List<RouteRecord>> getRoutes(@Path("requestId") long requestId);

    @GET("api/routes/{requestId}/recommended")
    Call<RouteRecord> getRecommendedRoute(@Path("requestId") long requestId);

    @POST("api/routes/compute")
    Call<Map> computeRoutes(@Body Map<String, Object> body);

    @GET("api/traffic/{requestId}/fusion")
    Call<FusedTrafficResult> getTrafficFusion(
            @Path("requestId") long requestId,
            @Query("originLat") double originLat,
            @Query("originLng") double originLng,
            @Query("destLat") double destLat,
            @Query("destLng") double destLng);

    @GET("api/traffic/cameras")
    Call<List<TrafficCamera>> getCameras();

    @GET("api/traffic/cameras/segment/{segmentId}")
    Call<TrafficCamera> getCameraForSegment(@Path("segmentId") long segmentId);

    // ── Hospitals ─────────────────────────────────────────────────────────────
    @GET("api/hospitals/nearby")
    Call<List<Hospital>> getNearbyHospitals(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("maxKm") double maxKm);

    @GET("api/hospitals/{id}")
    Call<Hospital> getHospital(@Path("id") long id);
}
