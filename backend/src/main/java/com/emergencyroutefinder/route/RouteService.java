package com.emergencyroutefinder.route;

import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.model.RouteSegment;
import com.emergencyroutefinder.model.TrafficCamera;
import com.emergencyroutefinder.repository.RouteRepository;
import com.emergencyroutefinder.repository.TrafficCameraRepository;
import com.emergencyroutefinder.traffic.FusedTrafficResult;
import com.emergencyroutefinder.traffic.TrafficFusionService;
import com.emergencyroutefinder.traffic.GoogleTrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Route Service — computes, scores, and recommends routes.
 *
 * Process:
 * 1. Request candidate routes from Google (or demo data)
 * 2. Run TrafficFusionService for each candidate
 * 3. Score each route using RouteScoringService
 * 4. Persist routes and segments to DB
 * 5. Mark the recommended route
 * 6. Periodically re-evaluate routes when traffic changes
 *
 * Demonstrates: Service layer, Async, Scheduled tasks (Multithreading)
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final TrafficFusionService fusionService;
    private final GoogleTrafficService googleService;
    private final RouteScoringService scoringService;
    private final TrafficCameraRepository cameraRepository;
    private final com.emergencyroutefinder.service.WebSocketNotificationService wsService;
    private final com.emergencyroutefinder.repository.EmergencyRequestRepository requestRepository;

    public RouteService(RouteRepository routeRepository,
                        TrafficFusionService fusionService,
                        GoogleTrafficService googleService,
                        RouteScoringService scoringService,
                        TrafficCameraRepository cameraRepository,
                        com.emergencyroutefinder.service.WebSocketNotificationService wsService,
                        com.emergencyroutefinder.repository.EmergencyRequestRepository requestRepository) {
        this.routeRepository = routeRepository;
        this.fusionService   = fusionService;
        this.googleService   = googleService;
        this.scoringService  = scoringService;
        this.cameraRepository = cameraRepository;
        this.wsService       = wsService;
        this.requestRepository = requestRepository;
    }

    /**
     * Compute and store route options for an emergency request.
     * Runs asynchronously after worker accepts.
     */
    @Async("taskExecutor")
    @Transactional
    public void computeRoutesAsync(Long requestId, double originLat, double originLng,
                                    double destLat, double destLng) {
        log.info("[Thread: {}] Computing routes for request {}", Thread.currentThread().getName(), requestId);
        try {
            computeAndSaveRoutes(requestId, originLat, originLng, destLat, destLng);
        } catch (Exception e) {
            log.error("Route computation failed for request {}: {}", requestId, e.getMessage());
        }
    }

    @Transactional
    public List<RouteRecord> computeAndSaveRoutes(Long requestId, double originLat, double originLng,
                                                   double destLat, double destLng) {
        // Get candidate routes from Google (or demo)
        List<RouteRecord> candidates = googleService.computeRoutes(
                originLat, originLng, destLat, destLng, requestId);

        // For each candidate, run traffic fusion and scoring
        for (RouteRecord route : candidates) {
            FusedTrafficResult fusion = fusionService.fuseForRoute(
                    route.getOriginLat(), route.getOriginLng(),
                    route.getDestLat(), route.getDestLng());

            route.setCameraTrafficScore(fusion.getFusedScore());
            route.setCrowdTrafficScore(fusion.getFusedScore());
            route.setFusedTrafficScore(fusion.getFusedScore());
            route.setConfidence(fusion.getConfidence());
            route.setSourcesUsed(fusion.getSourcesUsed());

            // Score route
            double score = scoringService.scoreRoute(route, fusion);
            route.setFinalRouteScore(score);
        }

        // Sort by score — lower is better
        candidates.sort(Comparator.comparingDouble(r -> r.getFinalRouteScore() != null ? r.getFinalRouteScore() : 999.0));

        // Mark best as recommended
        if (!candidates.isEmpty()) {
            candidates.get(0).setRecommended(true);
            candidates.get(0).setRecommendationReason(
                    scoringService.generateReason(candidates.get(0)));
        }

        // Save all candidates to DB
        List<RouteRecord> saved = new ArrayList<>();
        for (RouteRecord r : candidates) {
            routeRepository.save(r);
            // Save demo segments (one per route for simplicity; real integration would parse polyline)
            saveRouteSegments(r, originLat, originLng, destLat, destLng);
            saved.add(r);
        }

        // Update request with recommended route ID
        if (!saved.isEmpty() && saved.get(0).isRecommended()) {
            requestRepository.setRecommendedRoute(requestId, saved.get(0).getId());
        }

        return saved;
    }

    private void saveRouteSegments(RouteRecord route, double oLat, double oLng, double dLat, double dLng) {
        // For demo: create 3 simple segments dividing the route
        double[] lats = { oLat, oLat + (dLat - oLat) / 3, oLat + 2 * (dLat - oLat) / 3, dLat };
        double[] lngs = { oLng, oLng + (dLng - oLng) / 3, oLng + 2 * (dLng - oLng) / 3, dLng };
        String[] roads = { "Segment 1 Road", "Segment 2 Road", "Segment 3 Road" };

        // Get nearby cameras
        List<TrafficCamera> cameras = cameraRepository.findNearby(
                (oLat + dLat) / 2, (oLng + dLng) / 2, 0.15);

        for (int i = 0; i < 3; i++) {
            RouteSegment seg = new RouteSegment();
            seg.setRouteId(route.getId());
            seg.setSegmentIndex(i);
            seg.setStartLat(lats[i]); seg.setStartLng(lngs[i]);
            seg.setEndLat(lats[i + 1]); seg.setEndLng(lngs[i + 1]);
            seg.setRoadName(roads[i]);
            seg.setDistanceMeters(route.getDistanceMeters() != null ? route.getDistanceMeters() / 3 : 2000);
            seg.setFusedTrafficState(route.getGoogleTrafficState() != null ? route.getGoogleTrafficState() : "MODERATE");

            // Assign camera if available
            if (i < cameras.size()) {
                seg.setCameraId(cameras.get(i).getId());
            }

            routeRepository.saveSegment(seg);
        }
    }

    /** Get all routes for a request (with segments). */
    public List<RouteRecord> getRoutesForRequest(Long requestId) {
        List<RouteRecord> routes = routeRepository.findByRequestId(requestId);
        for (RouteRecord r : routes) {
            r.setSegments(routeRepository.findSegmentsByRouteId(r.getId()));
        }
        return routes;
    }

    public RouteRecord getRecommended(Long requestId) {
        return routeRepository.findRecommendedByRequestId(requestId)
                .orElse(null);
    }

    /**
     * Re-evaluate routes every 5 minutes for active emergencies.
     * Demonstrates: @Scheduled multithreading
     */
    @Scheduled(fixedRateString = "300000") // every 5 minutes
    public void reEvaluateActiveRoutes() {
        log.debug("[Scheduler] Checking for route re-evaluation...");
        // Get RESPONDING requests and re-score their routes
        try {
            List<com.emergencyroutefinder.model.EmergencyRequest> activeRequests =
                    requestRepository.findByStatus("RESPONDING");
            for (com.emergencyroutefinder.model.EmergencyRequest req : activeRequests) {
                reEvaluateRequest(req);
            }
        } catch (Exception e) {
            log.error("Route re-evaluation error: {}", e.getMessage());
        }
    }

    private void reEvaluateRequest(com.emergencyroutefinder.model.EmergencyRequest req) {
        List<RouteRecord> routes = routeRepository.findByRequestId(req.getId());
        if (routes.isEmpty()) return;

        // Re-score each route with fresh traffic data
        for (RouteRecord route : routes) {
            FusedTrafficResult fusion = fusionService.fuseForRoute(
                    route.getOriginLat(), route.getOriginLng(),
                    route.getDestLat(), route.getDestLng());
            double newScore = scoringService.scoreRoute(route, fusion);
            route.setFinalRouteScore(newScore);
            route.setFusedTrafficScore(fusion.getFusedScore());
            route.setConfidence(fusion.getConfidence());
            route.setSourcesUsed(fusion.getSourcesUsed());
        }

        // Find new best
        routes.sort(Comparator.comparingDouble(r -> r.getFinalRouteScore() != null ? r.getFinalRouteScore() : 999.0));
        RouteRecord bestOld = routes.stream().filter(RouteRecord::isRecommended).findFirst().orElse(null);
        RouteRecord bestNew = routes.get(0);

        if (bestOld == null || !bestOld.getId().equals(bestNew.getId())) {
            // Recommendation changed — update DB and notify
            routeRepository.clearRecommended(req.getId());
            bestNew.setRecommended(true);
            bestNew.setRecommendationReason("Route updated due to traffic conditions change.");
            routeRepository.updateScores(bestNew);
            requestRepository.setRecommendedRoute(req.getId(), bestNew.getId());

            wsService.notifyRouteUpdate(req.getId(), bestNew.getId(),
                    "Route updated due to traffic conditions.");
            log.info("Route recommendation updated for request {} → route {}", req.getId(), bestNew.getId());
        }
    }
}
