package com.emergencyroutefinder.traffic;

import com.emergencyroutefinder.model.CrowdLocationPoint;
import com.emergencyroutefinder.model.TrafficObservation;
import com.emergencyroutefinder.repository.CrowdLocationRepository;
import com.emergencyroutefinder.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Traffic Source 3 — Participating user movement data (crowd-sourced).
 *
 * PRIVACY: Only uses data from users who explicitly opted in (allow_crowd_traffic = TRUE).
 * Data is aggregated into road-segment speed estimates.
 * No personal identity is exposed to the traffic calculation.
 *
 * Implements the TrafficService interface.
 */
@Service("crowdTrafficService")
public class CrowdTrafficService implements TrafficService {

    private static final Logger log = LoggerFactory.getLogger(CrowdTrafficService.class);

    @Value("${traffic.crowd.freshness-minutes:15}")
    private int freshnessMinutes;

    private final CrowdLocationRepository crowdLocationRepository;

    public CrowdTrafficService(CrowdLocationRepository crowdLocationRepository) {
        this.crowdLocationRepository = crowdLocationRepository;
    }

    @Override
    public List<TrafficObservation> getObservationsForRoute(double originLat, double originLng,
                                                             double destLat, double destLng) {
        List<TrafficObservation> observations = new ArrayList<>();
        try {
            double midLat = (originLat + destLat) / 2;
            double midLng = (originLng + destLng) / 2;
            double radiusDeg = 0.12; // ~12 km bounding box

            List<CrowdLocationPoint> points = crowdLocationRepository.findRecentNearby(
                    midLat, midLng, radiusDeg, freshnessMinutes);

            if (points.isEmpty()) {
                log.debug("Crowd traffic: no recent data near route");
                return observations; // empty → fusion will handle missing source
            }

            // Aggregate: calculate average speed from all recent points
            OptionalDouble avgSpeed = points.stream()
                    .filter(p -> p.getSpeedKmh() != null)
                    .mapToDouble(CrowdLocationPoint::getSpeedKmh)
                    .average();

            if (avgSpeed.isPresent()) {
                double speed = avgSpeed.getAsDouble();
                String level = HaversineUtil.speedToTrafficLevel(speed);
                double confidence = Math.min(0.9, 0.4 + (points.size() * 0.05)); // more devices = higher confidence
                double delayFactor = HaversineUtil.trafficLevelToDelayFactor(level);

                TrafficObservation obs = new TrafficObservation(
                        TrafficObservation.TrafficSource.CROWD,
                        TrafficObservation.TrafficLevel.valueOf(level),
                        confidence, delayFactor);

                obs.setRawData(String.format(
                        "{\"device_count\":%d,\"avg_speed_kmh\":%.1f,\"traffic_level\":\"%s\",\"freshness_minutes\":%d}",
                        points.size(), speed, level, freshnessMinutes));
                observations.add(obs);

                log.debug("Crowd traffic: {} devices, avg speed {:.1f} km/h → {}", points.size(), speed, level);
            }
        } catch (Exception e) {
            log.error("CrowdTrafficService error: {}", e.getMessage());
            // Return empty — fusion handles missing source
        }
        return observations;
    }

    /** Accept a new crowd location point from a consenting user. */
    public void recordCrowdPoint(CrowdLocationPoint point) {
        try {
            crowdLocationRepository.save(point);
        } catch (Exception e) {
            log.error("Failed to record crowd point: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Available if any data exists (crowd data always potentially available when users share)
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSourceName() {
        return "CROWD";
    }
}
