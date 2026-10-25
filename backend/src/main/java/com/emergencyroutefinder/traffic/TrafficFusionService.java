package com.emergencyroutefinder.traffic;

import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.model.TrafficObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Traffic Fusion Engine — combines all three traffic sources into a single estimate.
 *
 * Configurable weights:
 *   Google traffic:  50% (default)
 *   Camera traffic:  25% (default)
 *   Crowd traffic:   25% (default)
 *
 * When sources are unavailable, remaining weight is redistributed.
 * Confidence is lowered proportionally to missing sources.
 *
 * Demonstrates: Service aggregation, weighted combination, resilience
 */
@Service
public class TrafficFusionService {

    private static final Logger log = LoggerFactory.getLogger(TrafficFusionService.class);

    @Value("${traffic.fusion.weight.google:0.50}")
    private double weightGoogle;

    @Value("${traffic.fusion.weight.camera:0.25}")
    private double weightCamera;

    @Value("${traffic.fusion.weight.crowd:0.25}")
    private double weightCrowd;

    private final TrafficCameraService cameraService;
    private final GoogleTrafficService googleService;
    private final CrowdTrafficService crowdService;

    public TrafficFusionService(TrafficCameraService cameraService,
                                 GoogleTrafficService googleService,
                                 CrowdTrafficService crowdService) {
        this.cameraService  = cameraService;
        this.googleService  = googleService;
        this.crowdService   = crowdService;
    }

    /**
     * Compute a fused traffic score for a route.
     *
     * @return FusedTrafficResult containing weighted score, confidence, and source breakdown
     */
    public FusedTrafficResult fuseForRoute(double originLat, double originLng,
                                            double destLat, double destLng) {
        log.info("Running traffic fusion for route ({},{}) → ({},{})", originLat, originLng, destLat, destLng);

        // ── Collect from all three sources ───────────────────────────────────
        List<TrafficObservation> googleObs = new ArrayList<>();
        List<TrafficObservation> cameraObs = new ArrayList<>();
        List<TrafficObservation> crowdObs  = new ArrayList<>();

        boolean googleAvail = false, cameraAvail = false, crowdAvail = false;

        try {
            googleObs  = googleService.getObservationsForRoute(originLat, originLng, destLat, destLng);
            googleAvail = !googleObs.isEmpty();
        } catch (Exception e) { log.warn("Google traffic source unavailable: {}", e.getMessage()); }

        try {
            cameraObs  = cameraService.getObservationsForRoute(originLat, originLng, destLat, destLng);
            cameraAvail = !cameraObs.isEmpty();
        } catch (Exception e) { log.warn("Camera traffic source unavailable: {}", e.getMessage()); }

        try {
            crowdObs   = crowdService.getObservationsForRoute(originLat, originLng, destLat, destLng);
            crowdAvail = !crowdObs.isEmpty();
        } catch (Exception e) { log.warn("Crowd traffic source unavailable: {}", e.getMessage()); }

        // ── Build sources-used label ──────────────────────────────────────────
        List<String> usedSources = new ArrayList<>();
        if (googleAvail) usedSources.add("GOOGLE");
        if (cameraAvail) usedSources.add("CAMERA");
        if (crowdAvail)  usedSources.add("CROWD");

        // ── Compute weighted delay factor ─────────────────────────────────────
        double totalWeight = 0;
        double weightedDelaySum = 0;

        if (googleAvail) {
            double gDelay = avgDelayFactor(googleObs);
            weightedDelaySum += weightGoogle * gDelay;
            totalWeight += weightGoogle;
        }
        if (cameraAvail) {
            double cDelay = avgDelayFactor(cameraObs);
            weightedDelaySum += weightCamera * cDelay;
            totalWeight += weightCamera;
        }
        if (crowdAvail) {
            double crDelay = avgDelayFactor(crowdObs);
            weightedDelaySum += weightCrowd * crDelay;
            totalWeight += weightCrowd;
        }

        double fusedDelayFactor;
        if (totalWeight == 0) {
            fusedDelayFactor = 1.0; // No sources — use base time
            log.warn("All traffic sources unavailable — using baseline (delay factor 1.0)");
        } else {
            fusedDelayFactor = weightedDelaySum / totalWeight;
        }

        // ── Confidence ────────────────────────────────────────────────────────
        int availCount = (googleAvail ? 1 : 0) + (cameraAvail ? 1 : 0) + (crowdAvail ? 1 : 0);
        RouteRecord.Confidence confidence = switch (availCount) {
            case 3 -> RouteRecord.Confidence.HIGH;
            case 2 -> RouteRecord.Confidence.MEDIUM;
            default -> RouteRecord.Confidence.LOW;
        };

        // ── Fused traffic level ───────────────────────────────────────────────
        String fusedLevel = delayFactorToLevel(fusedDelayFactor);
        double fusedScore = delayFactorToScore(fusedDelayFactor);

        // ── Individual source summaries ───────────────────────────────────────
        String googleLevel  = googleAvail ? avgLevel(googleObs) : "UNAVAILABLE";
        String cameraLevel  = cameraAvail ? avgLevel(cameraObs) : "UNAVAILABLE";
        String crowdLevel   = crowdAvail  ? avgLevel(crowdObs)  : "UNAVAILABLE";

        FusedTrafficResult result = new FusedTrafficResult();
        result.setFusedTrafficLevel(fusedLevel);
        result.setFusedDelayFactor(fusedDelayFactor);
        result.setFusedScore(fusedScore);
        result.setConfidence(confidence);
        result.setSourcesUsed(String.join(", ", usedSources));
        result.setGoogleLevel(googleLevel);
        result.setCameraLevel(cameraLevel);
        result.setCrowdLevel(crowdLevel);
        result.setGoogleAvailable(googleAvail);
        result.setCameraAvailable(cameraAvail);
        result.setCrowdAvailable(crowdAvail);
        result.setWeightGoogle(weightGoogle);
        result.setWeightCamera(weightCamera);
        result.setWeightCrowd(weightCrowd);
        result.setGoogleObservations(googleObs);
        result.setCameraObservations(cameraObs);
        result.setCrowdObservations(crowdObs);

        log.info("Fusion complete: level={}, delay={:.2f}, confidence={}, sources={}",
                fusedLevel, fusedDelayFactor, confidence, usedSources);

        return result;
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private double avgDelayFactor(List<TrafficObservation> obs) {
        return obs.stream().mapToDouble(TrafficObservation::getDelayFactor).average().orElse(1.0);
    }

    private String avgLevel(List<TrafficObservation> obs) {
        if (obs.isEmpty()) return "UNKNOWN";
        double avgScore = obs.stream()
                .mapToDouble(o -> levelToScore(o.getTrafficLevel().name()))
                .average().orElse(0.5);
        return scoreToLevel(avgScore);
    }

    private double levelToScore(String level) {
        return switch (level) {
            case "LOW"      -> 0.2;
            case "MODERATE" -> 0.5;
            case "HIGH"     -> 0.75;
            case "SEVERE"   -> 1.0;
            default         -> 0.5;
        };
    }

    private String scoreToLevel(double score) {
        if (score < 0.35)      return "LOW";
        if (score < 0.60)      return "MODERATE";
        if (score < 0.85)      return "HIGH";
        return "SEVERE";
    }

    private String delayFactorToLevel(double factor) {
        if (factor < 1.2)      return "LOW";
        if (factor < 1.7)      return "MODERATE";
        if (factor < 2.5)      return "HIGH";
        return "SEVERE";
    }

    private double delayFactorToScore(double factor) {
        return Math.min(1.0, (factor - 1.0) / 2.0);
    }
}
