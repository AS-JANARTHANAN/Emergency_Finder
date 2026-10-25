package com.emergencyroutefinder.traffic;

import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.model.TrafficObservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Traffic Source 2 — Google Routes API / traffic-aware routing.
 *
 * Uses the official Google Routes API (not scraping).
 * Returns candidate routes with traffic-aware duration information.
 *
 * When the Google API key is not configured, falls back to demo data
 * so the application remains demonstrable.
 *
 * Implements the TrafficService interface.
 */
@Service("googleTrafficService")
public class GoogleTrafficService implements TrafficService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTrafficService.class);

    @Value("${google.maps.api-key:DEMO}")
    private String apiKey;

    @Value("${google.routes.api-url:https://routes.googleapis.com/directions/v2:computeRoutes}")
    private String routesApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<TrafficObservation> getObservationsForRoute(double originLat, double originLng,
                                                             double destLat, double destLng) {
        if ("DEMO".equals(apiKey) || apiKey == null || apiKey.startsWith("YOUR_")) {
            log.info("Google API key not configured — using demo traffic observation");
            return buildDemoObservation();
        }

        try {
            return callGoogleRoutesApi(originLat, originLng, destLat, destLng);
        } catch (Exception e) {
            log.error("Google Routes API error: {} — using demo fallback", e.getMessage());
            return buildDemoObservation();
        }
    }

    /**
     * Call Google Routes API with traffic awareness.
     */
    public List<RouteRecord> computeRoutes(double originLat, double originLng,
                                            double destLat, double destLng,
                                            Long requestId) {
        if ("DEMO".equals(apiKey) || apiKey == null || apiKey.startsWith("YOUR_")) {
            return buildDemoRoutes(originLat, originLng, destLat, destLng, requestId);
        }

        try {
            return callGoogleRoutesApiForRoutes(originLat, originLng, destLat, destLng, requestId);
        } catch (Exception e) {
            log.error("Google Routes API failed: {} — using demo routes", e.getMessage());
            return buildDemoRoutes(originLat, originLng, destLat, destLng, requestId);
        }
    }

    private List<RouteRecord> callGoogleRoutesApiForRoutes(double oLat, double oLng,
                                                            double dLat, double dLng,
                                                            Long requestId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("origin").putObject("location").putObject("latLng")
                .put("latitude", oLat).put("longitude", oLng);
        body.putObject("destination").putObject("location").putObject("latLng")
                .put("latitude", dLat).put("longitude", dLng);
        body.put("travelMode", "DRIVE");
        body.put("computeAlternativeRoutes", true);
        body.putObject("routingPreference").put("routingPreference", "TRAFFIC_AWARE");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(routesApiUrl))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask",
                        "routes.duration,routes.staticDuration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.travelAdvisory")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseRoutesResponse(response.body(), oLat, oLng, dLat, dLng, requestId);
    }

    private List<RouteRecord> parseRoutesResponse(String json, double oLat, double oLng,
                                                   double dLat, double dLng, Long requestId) throws Exception {
        List<RouteRecord> routes = new ArrayList<>();
        JsonNode root = objectMapper.readTree(json);
        JsonNode routeArray = root.get("routes");
        if (routeArray == null || routeArray.isEmpty()) return buildDemoRoutes(oLat, oLng, dLat, dLng, requestId);

        String[] labels = {"Route A", "Route B", "Route C"};
        int idx = 0;
        for (JsonNode r : routeArray) {
            if (idx >= 3) break;
            RouteRecord record = new RouteRecord(requestId, labels[idx], oLat, oLng, dLat, dLng);
            if (r.has("distanceMeters")) record.setDistanceMeters(r.get("distanceMeters").asInt());
            if (r.has("staticDuration")) {
                String dur = r.get("staticDuration").asText().replace("s", "");
                record.setBaseDurationSec(Integer.parseInt(dur));
            }
            if (r.has("duration")) {
                String dur = r.get("duration").asText().replace("s", "");
                record.setTrafficDurationSec(Integer.parseInt(dur));
            }
            if (r.has("polyline") && r.get("polyline").has("encodedPolyline")) {
                record.setEncodedPolyline(r.get("polyline").get("encodedPolyline").asText());
            }
            // Extract traffic advisory
            if (r.has("travelAdvisory") && r.get("travelAdvisory").has("trafficDensity")) {
                record.setGoogleTrafficState(r.get("travelAdvisory").get("trafficDensity").asText());
            }
            record.setSourcesUsed("GOOGLE");
            routes.add(record);
            idx++;
        }
        return routes.isEmpty() ? buildDemoRoutes(oLat, oLng, dLat, dLng, requestId) : routes;
    }

    private List<TrafficObservation> callGoogleRoutesApi(double oLat, double oLng,
                                                   double dLat, double dLng) throws Exception {
        return List.of(); // used only internally; actual call in callGoogleRoutesApiForRoutes
    }

    // ── Demo Fallback ─────────────────────────────────────────────────────────

    private List<TrafficObservation> buildDemoObservation() {
        TrafficObservation obs = new TrafficObservation(
                TrafficObservation.TrafficSource.GOOGLE,
                TrafficObservation.TrafficLevel.MODERATE,
                0.85, 1.5);
        obs.setRawData("{\"source\":\"google_routes_demo\",\"note\":\"API key not configured — demo data\"}");
        return List.of(obs);
    }

    private List<RouteRecord> buildDemoRoutes(double oLat, double oLng,
                                               double dLat, double dLng, Long requestId) {
        List<RouteRecord> routes = new ArrayList<>();

        // Route A — shortest but higher traffic
        RouteRecord routeA = new RouteRecord(requestId, "Route A", oLat, oLng, dLat, dLng);
        routeA.setDistanceMeters(5_400);
        routeA.setBaseDurationSec(660);
        routeA.setTrafficDurationSec(900);
        routeA.setGoogleTrafficState("HEAVY");
        routeA.setSourcesUsed("GOOGLE_DEMO");

        // Route B — longer but less traffic
        RouteRecord routeB = new RouteRecord(requestId, "Route B", oLat, oLng, dLat, dLng);
        routeB.setDistanceMeters(6_200);
        routeB.setBaseDurationSec(780);
        routeB.setTrafficDurationSec(850);
        routeB.setGoogleTrafficState("MEDIUM");
        routeB.setSourcesUsed("GOOGLE_DEMO");

        // Route C — alternative
        RouteRecord routeC = new RouteRecord(requestId, "Route C", oLat, oLng, dLat, dLng);
        routeC.setDistanceMeters(7_100);
        routeC.setBaseDurationSec(900);
        routeC.setTrafficDurationSec(920);
        routeC.setGoogleTrafficState("LOW");
        routeC.setSourcesUsed("GOOGLE_DEMO");

        routes.add(routeA);
        routes.add(routeB);
        routes.add(routeC);
        return routes;
    }

    @Override
    public boolean isAvailable() {
        return !"DEMO".equals(apiKey) && apiKey != null && !apiKey.startsWith("YOUR_");
    }

    @Override
    public String getSourceName() {
        return "GOOGLE";
    }
}
