package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/**
 * A single GPS + speed observation from a consenting participating user.
 * Used as input for crowd-sourced traffic estimation.
 *
 * Privacy: user_id stored only if consented. segment_id used for aggregation.
 * No personal identity exposed to traffic calculation.
 */
public class CrowdLocationPoint {

    private Long id;
    private Long userId;       // May be null for anonymous contribution
    private Long segmentId;    // Road segment (null until matched)
    private double lat;
    private double lng;
    private Double speedKmh;
    private Double directionDeg;
    private LocalDateTime recordedAt;

    public CrowdLocationPoint() {}

    public CrowdLocationPoint(Long userId, double lat, double lng, Double speedKmh) {
        this.userId = userId; this.lat = lat; this.lng = lng; this.speedKmh = speedKmh;
    }

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }
    public Long getUserId()                         { return userId; }
    public void setUserId(Long userId)              { this.userId = userId; }
    public Long getSegmentId()                      { return segmentId; }
    public void setSegmentId(Long segmentId)        { this.segmentId = segmentId; }
    public double getLat()                          { return lat; }
    public void setLat(double lat)                  { this.lat = lat; }
    public double getLng()                          { return lng; }
    public void setLng(double lng)                  { this.lng = lng; }
    public Double getSpeedKmh()                     { return speedKmh; }
    public void setSpeedKmh(Double speedKmh)        { this.speedKmh = speedKmh; }
    public Double getDirectionDeg()                 { return directionDeg; }
    public void setDirectionDeg(Double directionDeg){ this.directionDeg = directionDeg; }
    public LocalDateTime getRecordedAt()            { return recordedAt; }
    public void setRecordedAt(LocalDateTime t)      { this.recordedAt = t; }
}
