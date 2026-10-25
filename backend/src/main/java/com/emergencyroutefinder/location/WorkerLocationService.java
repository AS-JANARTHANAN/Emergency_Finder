package com.emergencyroutefinder.location;

import com.emergencyroutefinder.model.CrowdLocationPoint;
import com.emergencyroutefinder.repository.EmergencyWorkerRepository;
import com.emergencyroutefinder.repository.UserRepository;
import com.emergencyroutefinder.repository.VehicleRepository;
import com.emergencyroutefinder.service.WebSocketNotificationService;
import com.emergencyroutefinder.traffic.CrowdTrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Worker Location Service — receives GPS updates from the Android app
 * and broadcasts them to the map via WebSocket.
 *
 * Also handles crowd traffic data contribution from consenting users.
 * Demonstrates: Location processing, Async (multithreading)
 */
@Service
public class WorkerLocationService {

    private static final Logger log = LoggerFactory.getLogger(WorkerLocationService.class);

    private final EmergencyWorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final WebSocketNotificationService wsService;
    private final CrowdTrafficService crowdTrafficService;
    private final UserRepository userRepository;

    public WorkerLocationService(EmergencyWorkerRepository workerRepository,
                                  VehicleRepository vehicleRepository,
                                  WebSocketNotificationService wsService,
                                  CrowdTrafficService crowdTrafficService,
                                  UserRepository userRepository) {
        this.workerRepository = workerRepository;
        this.vehicleRepository = vehicleRepository;
        this.wsService = wsService;
        this.crowdTrafficService = crowdTrafficService;
        this.userRepository = userRepository;
    }

    /**
     * Update worker GPS location and broadcast to active request map.
     */
    @Async("taskExecutor")
    public void updateWorkerLocation(Long workerId, double lat, double lng, Long activeRequestId) {
        try {
            workerRepository.updateLocation(workerId, lat, lng);
            vehicleRepository.findByWorkerId(workerId).ifPresent(v -> {
                vehicleRepository.updateLocation(v.getId(), lat, lng);
            });

            // Broadcast to user's map via WebSocket
            if (activeRequestId != null) {
                wsService.notifyWorkerLocation(activeRequestId, lat, lng);
            }

            log.debug("Worker {} location updated: {}, {}", workerId, lat, lng);
        } catch (Exception e) {
            log.error("Location update error for worker {}: {}", workerId, e.getMessage());
        }
    }

    /**
     * Accept and store crowd traffic data from a consenting participating user.
     * Privacy: Only called if user has allow_crowd_traffic = TRUE.
     */
    @Async("taskExecutor")
    public void submitCrowdLocation(Long userId, double lat, double lng, Double speedKmh) {
        try {
            // Verify user consent before storing
            userRepository.findById(userId).ifPresent(user -> {
                if (user.isAllowCrowdTraffic()) {
                    CrowdLocationPoint point = new CrowdLocationPoint(userId, lat, lng, speedKmh);
                    crowdTrafficService.recordCrowdPoint(point);
                    log.debug("Crowd location stored for user {} (consented)", userId);
                } else {
                    log.warn("Rejected crowd location from user {} — consent not given", userId);
                }
            });
        } catch (Exception e) {
            log.error("Crowd location error: {}", e.getMessage());
        }
    }

    /**
     * Update user emergency location.
     */
    @Async("taskExecutor")
    public void updateUserLocation(Long userId, double lat, double lng) {
        try {
            userRepository.updateLocation(userId, lat, lng);
        } catch (Exception e) {
            log.error("User location update error: {}", e.getMessage());
        }
    }
}
