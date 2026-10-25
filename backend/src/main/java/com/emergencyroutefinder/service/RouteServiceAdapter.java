package com.emergencyroutefinder.service;

import com.emergencyroutefinder.route.RouteService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Adapter allowing EmergencyRequestService to reference RouteService
 * without circular dependency issues.
 */
@Service
public class RouteServiceAdapter {

    private final RouteService routeService;

    public RouteServiceAdapter(@Lazy RouteService routeService) {
        this.routeService = routeService;
    }

    public void computeRoutesForRequest(Long requestId, double originLat, double originLng,
                                         double destLat, double destLng) {
        routeService.computeRoutesAsync(requestId, originLat, originLng, destLat, destLng);
    }
}
