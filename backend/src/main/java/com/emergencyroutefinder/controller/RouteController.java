package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.route.RouteService;
import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.traffic.TrafficFusionService;
import com.emergencyroutefinder.traffic.FusedTrafficResult;
import com.emergencyroutefinder.traffic.TrafficCameraService;
import com.emergencyroutefinder.model.TrafficCamera;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Route and Traffic Controller — route options, recommendations, and traffic analysis.
 */
@RestController
@RequestMapping("/api")
public class RouteController {

    private final RouteService routeService;
    private final TrafficFusionService fusionService;
    private final TrafficCameraService cameraService;

    public RouteController(RouteService routeService,
                            TrafficFusionService fusionService,
                            TrafficCameraService cameraService) {
        this.routeService  = routeService;
        this.fusionService = fusionService;
        this.cameraService = cameraService;
    }

    /** Get all computed route options for a request. */
    @GetMapping("/routes/{requestId}")
    public ResponseEntity<List<RouteRecord>> getRoutes(@PathVariable Long requestId) {
        return ResponseEntity.ok(routeService.getRoutesForRequest(requestId));
    }

    /** Get the recommended route for a request. */
    @GetMapping("/routes/{requestId}/recommended")
    public ResponseEntity<RouteRecord> getRecommended(@PathVariable Long requestId) {
        RouteRecord rec = routeService.getRecommended(requestId);
        if (rec == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(rec);
    }

    /** Compute fresh routes (trigger computation). */
    @PostMapping("/routes/compute")
    public ResponseEntity<Map<String, Object>> computeRoutes(@RequestBody Map<String, Object> body) {
        Long requestId = Long.parseLong(body.get("requestId").toString());
        double oLat    = Double.parseDouble(body.get("originLat").toString());
        double oLng    = Double.parseDouble(body.get("originLng").toString());
        double dLat    = Double.parseDouble(body.get("destLat").toString());
        double dLng    = Double.parseDouble(body.get("destLng").toString());

        routeService.computeRoutesAsync(requestId, oLat, oLng, dLat, dLng);
        return ResponseEntity.accepted().body(Map.of(
                "message", "Route computation started",
                "requestId", requestId));
    }

    /** Get fused traffic result for a route. */
    @GetMapping("/traffic/{requestId}/fusion")
    public ResponseEntity<FusedTrafficResult> getTrafficFusion(
            @PathVariable Long requestId,
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng) {
        FusedTrafficResult result = fusionService.fuseForRoute(originLat, originLng, destLat, destLng);
        return ResponseEntity.ok(result);
    }

    /** Get all active cameras (for map display). */
    @GetMapping("/traffic/cameras")
    public ResponseEntity<List<TrafficCamera>> getAllCameras() {
        return ResponseEntity.ok(cameraService.getAllActiveCameras());
    }

    /** Get camera for a specific route segment. */
    @GetMapping("/traffic/cameras/segment/{segmentId}")
    public ResponseEntity<TrafficCamera> getCameraForSegment(@PathVariable Long segmentId) {
        TrafficCamera cam = cameraService.getCameraForSegment(segmentId);
        if (cam == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(cam);
    }
}
