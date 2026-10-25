package com.emergencyroutefinder.traffic;

import com.emergencyroutefinder.model.TrafficCamera;
import com.emergencyroutefinder.model.TrafficObservation;
import com.emergencyroutefinder.repository.TrafficCameraRepository;
import com.emergencyroutefinder.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Traffic Source 1 — Prerecorded traffic camera footage.
 *
 * Retrieves camera data near a route and converts it to TrafficObservation objects.
 * All footage is clearly labelled as prerecorded simulation data.
 *
 * Implements the TrafficService interface.
 * Demonstrates: Interfaces, Dependency Inversion, Exception Handling
 */
@Service("trafficCameraService")
public class TrafficCameraService implements TrafficService {

    private static final Logger log = LoggerFactory.getLogger(TrafficCameraService.class);
    private static final String SOURCE_LABEL = "Prerecorded simulation (NOT live CCTV)";

    private final TrafficCameraRepository cameraRepository;

    public TrafficCameraService(TrafficCameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    @Override
    public List<TrafficObservation> getObservationsForRoute(double originLat, double originLng,
                                                             double destLat, double destLng) {
        List<TrafficObservation> observations = new ArrayList<>();
        try {
            // Get cameras near the route midpoint (simple bounding box)
            double midLat = (originLat + destLat) / 2;
            double midLng = (originLng + destLng) / 2;
            double radiusDeg = 0.15; // ~15 km

            List<TrafficCamera> cameras = cameraRepository.findNearby(midLat, midLng, radiusDeg);

            for (TrafficCamera cam : cameras) {
                TrafficObservation obs = new TrafficObservation(
                        TrafficObservation.TrafficSource.CAMERA,
                        TrafficObservation.TrafficLevel.valueOf(cam.getTrafficLevel().name()),
                        0.80,  // Camera has medium-high confidence (prerecorded)
                        HaversineUtil.trafficLevelToDelayFactor(cam.getTrafficLevel().name())
                );
                obs.setRawData(String.format(
                        "{\"camera_id\":%d,\"camera_name\":\"%s\",\"road\":\"%s\",\"source\":\"%s\",\"traffic_score\":%.2f}",
                        cam.getId(), cam.getCameraName(), cam.getRoadName(), SOURCE_LABEL, cam.getTrafficScore()));
                observations.add(obs);
            }

            log.debug("Camera source returned {} observations near ({}, {})", observations.size(), midLat, midLng);
        } catch (Exception e) {
            log.error("TrafficCameraService error: {}", e.getMessage());
            // Return empty list — TrafficFusionService handles missing sources
        }
        return observations;
    }

    @Override
    public boolean isAvailable() {
        try {
            return !cameraRepository.findAll().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSourceName() {
        return "CAMERA";
    }

    /** Get specific camera for a route segment. */
    public TrafficCamera getCameraForSegment(Long segmentId) {
        return cameraRepository.findBySegmentId(segmentId).orElse(null);
    }

    /** Get all active cameras for UI display. */
    public List<TrafficCamera> getAllActiveCameras() {
        return cameraRepository.findAll();
    }
}
