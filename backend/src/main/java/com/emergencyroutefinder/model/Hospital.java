package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/** Hospital record for nearby hospital search. */
public class Hospital {
    private Long id;
    private String name;
    private String address;
    private String contactNumber;
    private double lat;
    private double lng;
    private boolean emergencyAvailable;
    private boolean active;
    private LocalDateTime createdAt;
    // Transient — set during distance search
    private Double distanceKm;

    public Hospital() {}

    public Hospital(String name, String address, String contactNumber, double lat, double lng) {
        this.name = name; this.address = address; this.contactNumber = contactNumber;
        this.lat = lat; this.lng = lng; this.emergencyAvailable = true; this.active = true;
    }

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }
    public String getName()                             { return name; }
    public void setName(String name)                    { this.name = name; }
    public String getAddress()                          { return address; }
    public void setAddress(String address)              { this.address = address; }
    public String getContactNumber()                    { return contactNumber; }
    public void setContactNumber(String c)              { this.contactNumber = c; }
    public double getLat()                              { return lat; }
    public void setLat(double lat)                      { this.lat = lat; }
    public double getLng()                              { return lng; }
    public void setLng(double lng)                      { this.lng = lng; }
    public boolean isEmergencyAvailable()               { return emergencyAvailable; }
    public void setEmergencyAvailable(boolean b)        { this.emergencyAvailable = b; }
    public boolean isActive()                           { return active; }
    public void setActive(boolean active)               { this.active = active; }
    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }
    public Double getDistanceKm()                       { return distanceKm; }
    public void setDistanceKm(Double distanceKm)        { this.distanceKm = distanceKm; }
}
