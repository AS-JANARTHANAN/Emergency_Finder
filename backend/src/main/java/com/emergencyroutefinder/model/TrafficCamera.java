package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/** Prerecorded traffic camera associated with a road segment. */
public class TrafficCamera {

    public enum TrafficLevel { LOW, MODERATE, HIGH, SEVERE }

    private Long id;
    private String cameraName;
    private String roadName;
    private double lat;
    private double lng;
    private Long routeSegmentId;
    private String videoFilePath;
    private TrafficLevel trafficLevel;
    private double trafficScore;      // 0.0 (free) → 1.0 (severe)
    private boolean active;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;

    public TrafficCamera() {}

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }
    public String getCameraName()                       { return cameraName; }
    public void setCameraName(String cameraName)        { this.cameraName = cameraName; }
    public String getRoadName()                         { return roadName; }
    public void setRoadName(String roadName)            { this.roadName = roadName; }
    public double getLat()                              { return lat; }
    public void setLat(double lat)                      { this.lat = lat; }
    public double getLng()                              { return lng; }
    public void setLng(double lng)                      { this.lng = lng; }
    public Long getRouteSegmentId()                     { return routeSegmentId; }
    public void setRouteSegmentId(Long routeSegmentId)  { this.routeSegmentId = routeSegmentId; }
    public String getVideoFilePath()                    { return videoFilePath; }
    public void setVideoFilePath(String videoFilePath)  { this.videoFilePath = videoFilePath; }
    public TrafficLevel getTrafficLevel()               { return trafficLevel; }
    public void setTrafficLevel(TrafficLevel trafficLevel){ this.trafficLevel = trafficLevel; }
    public double getTrafficScore()                     { return trafficScore; }
    public void setTrafficScore(double trafficScore)    { this.trafficScore = trafficScore; }
    public boolean isActive()                           { return active; }
    public void setActive(boolean active)               { this.active = active; }
    public LocalDateTime getLastUpdated()               { return lastUpdated; }
    public void setLastUpdated(LocalDateTime t)         { this.lastUpdated = t; }
    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }
}
