package com.emergencyroutefinder.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A calculated route between two points for an emergency request.
 * Multiple RouteRecords can exist per request (Route A, B, C alternatives).
 */
public class RouteRecord {

    public enum Confidence { HIGH, MEDIUM, LOW }

    private Long id;
    private Long requestId;
    private String routeLabel;         // "Route A", "Route B", ...
    private double originLat;
    private double originLng;
    private double destLat;
    private double destLng;
    private Integer distanceMeters;
    private Integer baseDurationSec;
    private Integer trafficDurationSec;
    private String encodedPolyline;
    private String googleTrafficState;
    private Double cameraTrafficScore;
    private Double crowdTrafficScore;
    private Double fusedTrafficScore;
    private Double finalRouteScore;
    private Confidence confidence;
    private boolean recommended;
    private String sourcesUsed;
    private String recommendationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient — populated from joins
    private List<RouteSegment> segments;

    public RouteRecord() {}

    public RouteRecord(Long requestId, String routeLabel, double originLat, double originLng,
                       double destLat, double destLng) {
        this.requestId = requestId; this.routeLabel = routeLabel;
        this.originLat = originLat; this.originLng = originLng;
        this.destLat = destLat; this.destLng = destLng;
        this.confidence = Confidence.MEDIUM;
    }

    public Long getId()                                     { return id; }
    public void setId(Long id)                              { this.id = id; }
    public Long getRequestId()                              { return requestId; }
    public void setRequestId(Long r)                        { this.requestId = r; }
    public String getRouteLabel()                           { return routeLabel; }
    public void setRouteLabel(String l)                     { this.routeLabel = l; }
    public double getOriginLat()                            { return originLat; }
    public void setOriginLat(double v)                      { this.originLat = v; }
    public double getOriginLng()                            { return originLng; }
    public void setOriginLng(double v)                      { this.originLng = v; }
    public double getDestLat()                              { return destLat; }
    public void setDestLat(double v)                        { this.destLat = v; }
    public double getDestLng()                              { return destLng; }
    public void setDestLng(double v)                        { this.destLng = v; }
    public Integer getDistanceMeters()                      { return distanceMeters; }
    public void setDistanceMeters(Integer v)                { this.distanceMeters = v; }
    public Integer getBaseDurationSec()                     { return baseDurationSec; }
    public void setBaseDurationSec(Integer v)               { this.baseDurationSec = v; }
    public Integer getTrafficDurationSec()                  { return trafficDurationSec; }
    public void setTrafficDurationSec(Integer v)            { this.trafficDurationSec = v; }
    public String getEncodedPolyline()                      { return encodedPolyline; }
    public void setEncodedPolyline(String v)                { this.encodedPolyline = v; }
    public String getGoogleTrafficState()                   { return googleTrafficState; }
    public void setGoogleTrafficState(String v)             { this.googleTrafficState = v; }
    public Double getCameraTrafficScore()                   { return cameraTrafficScore; }
    public void setCameraTrafficScore(Double v)             { this.cameraTrafficScore = v; }
    public Double getCrowdTrafficScore()                    { return crowdTrafficScore; }
    public void setCrowdTrafficScore(Double v)              { this.crowdTrafficScore = v; }
    public Double getFusedTrafficScore()                    { return fusedTrafficScore; }
    public void setFusedTrafficScore(Double v)              { this.fusedTrafficScore = v; }
    public Double getFinalRouteScore()                      { return finalRouteScore; }
    public void setFinalRouteScore(Double v)                { this.finalRouteScore = v; }
    public Confidence getConfidence()                       { return confidence; }
    public void setConfidence(Confidence v)                 { this.confidence = v; }
    public boolean isRecommended()                          { return recommended; }
    public void setRecommended(boolean v)                   { this.recommended = v; }
    public String getSourcesUsed()                          { return sourcesUsed; }
    public void setSourcesUsed(String v)                    { this.sourcesUsed = v; }
    public String getRecommendationReason()                 { return recommendationReason; }
    public void setRecommendationReason(String v)           { this.recommendationReason = v; }
    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime t)               { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)               { this.updatedAt = t; }
    public List<RouteSegment> getSegments()                 { return segments; }
    public void setSegments(List<RouteSegment> segments)    { this.segments = segments; }
}
