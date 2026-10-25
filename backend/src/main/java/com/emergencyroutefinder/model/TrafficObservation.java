package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/** A single normalized traffic observation from one source for one segment. */
public class TrafficObservation {

    public enum TrafficSource { CAMERA, GOOGLE, CROWD }
    public enum TrafficLevel  { LOW, MODERATE, HIGH, SEVERE }

    private Long id;
    private Long routeId;
    private Long segmentId;
    private TrafficSource trafficSource;
    private TrafficLevel trafficLevel;
    private double confidence;    // 0.0 → 1.0
    private double delayFactor;   // 1.0 = no delay, 2.0 = double travel time
    private String rawData;       // JSON string from source
    private LocalDateTime observedAt;

    public TrafficObservation() {}

    public TrafficObservation(TrafficSource source, TrafficLevel level, double confidence, double delayFactor) {
        this.trafficSource = source; this.trafficLevel = level;
        this.confidence = confidence; this.delayFactor = delayFactor;
        this.observedAt = LocalDateTime.now();
    }

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }
    public Long getRouteId()                            { return routeId; }
    public void setRouteId(Long routeId)                { this.routeId = routeId; }
    public Long getSegmentId()                          { return segmentId; }
    public void setSegmentId(Long segmentId)            { this.segmentId = segmentId; }
    public TrafficSource getTrafficSource()             { return trafficSource; }
    public void setTrafficSource(TrafficSource s)       { this.trafficSource = s; }
    public TrafficLevel getTrafficLevel()               { return trafficLevel; }
    public void setTrafficLevel(TrafficLevel l)         { this.trafficLevel = l; }
    public double getConfidence()                       { return confidence; }
    public void setConfidence(double confidence)        { this.confidence = confidence; }
    public double getDelayFactor()                      { return delayFactor; }
    public void setDelayFactor(double delayFactor)      { this.delayFactor = delayFactor; }
    public String getRawData()                          { return rawData; }
    public void setRawData(String rawData)              { this.rawData = rawData; }
    public LocalDateTime getObservedAt()                { return observedAt; }
    public void setObservedAt(LocalDateTime observedAt) { this.observedAt = observedAt; }
}
