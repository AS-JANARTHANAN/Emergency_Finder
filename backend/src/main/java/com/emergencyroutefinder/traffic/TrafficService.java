package com.emergencyroutefinder.traffic;

import com.emergencyroutefinder.model.TrafficObservation;
import java.util.List;

/**
 * TrafficService interface — contract for all three traffic sources.
 *
 * Demonstrates: Java Interfaces — all three sources implement this interface,
 *               allowing the TrafficFusionService to treat them uniformly.
 */
public interface TrafficService {

    /**
     * Get traffic observations for a route segment between two points.
     *
     * @return list of normalized traffic observations (may be empty if unavailable)
     */
    List<TrafficObservation> getObservationsForRoute(double originLat, double originLng,
                                                      double destLat, double destLng);

    /** Check if this traffic source is currently available. */
    boolean isAvailable();

    /** Return a short name for this source (CAMERA, GOOGLE, CROWD). */
    String getSourceName();
}
