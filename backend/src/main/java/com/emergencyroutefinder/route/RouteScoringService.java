package com.emergencyroutefinder.route;

import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.traffic.FusedTrafficResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Route Scoring Engine — assigns a numerical score to each candidate route.
 *
 * Score Formula (lower = better):
 *   Score = (Base Travel Time * Traffic Duration Ratio) + Traffic Penalty + Camera Penalty + Crowd Penalty
 *
 * This score is transparent and explainable.
 *
 * Demonstrates: Business logic, mathematical scoring
 */
@Service
public class RouteScoringService {

    private static final Logger log = LoggerFactory.getLogger(RouteScoringService.class);

    /**
     * Score a route based on fused traffic information.
     * Lower score = more efficient route recommendation.
     */
    public double scoreRoute(RouteRecord route, FusedTrafficResult fusion) {
        double baseDuration = route.getBaseDurationSec() != null ? route.getBaseDurationSec() : 600.0;
        double trafficDuration = route.getTrafficDurationSec() != null ? route.getTrafficDurationSec() : baseDuration;

        // Base score: traffic-aware duration (seconds)
        double score = trafficDuration;

        // Camera traffic penalty (0-240 seconds max)
        if (fusion.isCameraAvailable()) {
            double cameraPenalty = getTrafficLevelPenaltySec(fusion.getCameraLevel());
            score += cameraPenalty;
        }

        // Crowd traffic penalty (0-240 seconds max)
        if (fusion.isCrowdAvailable()) {
            double crowdPenalty = getTrafficLevelPenaltySec(fusion.getCrowdLevel());
            score += crowdPenalty;
        }

        // Apply fused delay factor for remaining weighting
        score *= fusion.getFusedDelayFactor();

        // Confidence adjustment — low confidence slightly penalises score
        if (fusion.getConfidence() == RouteRecord.Confidence.LOW) {
            score *= 1.05;
        }

        log.debug("Route '{}': score={:.1f} (base={:.0f}s, trafficFactor={:.2f})",
                route.getRouteLabel(), score, baseDuration, fusion.getFusedDelayFactor());

        return Math.round(score);
    }

    private double getTrafficLevelPenaltySec(String level) {
        if (level == null) return 0;
        return switch (level) {
            case "LOW"      -> 30;
            case "MODERATE" -> 90;
            case "HIGH"     -> 180;
            case "SEVERE"   -> 300;
            default         -> 0;
        };
    }

    /**
     * Generate human-readable recommendation reason for the UI.
     */
    public String generateReason(RouteRecord route) {
        if (route.getGoogleTrafficState() != null) {
            return String.format(
                    "Recommended because combined traffic analysis indicates lower expected delay. " +
                    "Google traffic: %s. " +
                    "Confidence: %s. " +
                    "Sources used: %s.",
                    route.getGoogleTrafficState(),
                    route.getConfidence() != null ? route.getConfidence().name() : "MEDIUM",
                    route.getSourcesUsed() != null ? route.getSourcesUsed() : "Available sources"
            );
        }
        return "Recommended based on available traffic and route information.";
    }
}
