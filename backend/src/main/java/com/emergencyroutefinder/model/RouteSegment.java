package com.emergencyroutefinder.model;

/** A segment of a calculated route — maps to a specific road section and camera. */
public class RouteSegment {

    private Long id;
    private Long routeId;
    private int segmentIndex;
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private String roadName;
    private Integer distanceMeters;
    private Integer durationSec;
    private Long cameraId;
    private String googleTrafficState;
    private String crowdTrafficState;
    private String fusedTrafficState;
    private Integer estimatedDelaySec;

    public RouteSegment() {}

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }
    public Long getRouteId()                            { return routeId; }
    public void setRouteId(Long routeId)                { this.routeId = routeId; }
    public int getSegmentIndex()                        { return segmentIndex; }
    public void setSegmentIndex(int segmentIndex)       { this.segmentIndex = segmentIndex; }
    public double getStartLat()                         { return startLat; }
    public void setStartLat(double startLat)            { this.startLat = startLat; }
    public double getStartLng()                         { return startLng; }
    public void setStartLng(double startLng)            { this.startLng = startLng; }
    public double getEndLat()                           { return endLat; }
    public void setEndLat(double endLat)                { this.endLat = endLat; }
    public double getEndLng()                           { return endLng; }
    public void setEndLng(double endLng)                { this.endLng = endLng; }
    public String getRoadName()                         { return roadName; }
    public void setRoadName(String roadName)            { this.roadName = roadName; }
    public Integer getDistanceMeters()                  { return distanceMeters; }
    public void setDistanceMeters(Integer d)            { this.distanceMeters = d; }
    public Integer getDurationSec()                     { return durationSec; }
    public void setDurationSec(Integer d)               { this.durationSec = d; }
    public Long getCameraId()                           { return cameraId; }
    public void setCameraId(Long cameraId)              { this.cameraId = cameraId; }
    public String getGoogleTrafficState()               { return googleTrafficState; }
    public void setGoogleTrafficState(String v)         { this.googleTrafficState = v; }
    public String getCrowdTrafficState()                { return crowdTrafficState; }
    public void setCrowdTrafficState(String v)          { this.crowdTrafficState = v; }
    public String getFusedTrafficState()                { return fusedTrafficState; }
    public void setFusedTrafficState(String v)          { this.fusedTrafficState = v; }
    public Integer getEstimatedDelaySec()               { return estimatedDelaySec; }
    public void setEstimatedDelaySec(Integer v)         { this.estimatedDelaySec = v; }
}
