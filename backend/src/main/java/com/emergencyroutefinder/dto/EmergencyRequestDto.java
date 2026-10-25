package com.emergencyroutefinder.dto;

/** Emergency request creation DTO from Android client. */
public class EmergencyRequestDto {
    private Long userId;
    private String emergencyType;   // MEDICAL, ACCIDENT, FIRE, OTHER
    private String description;
    private String priority;        // LOW, MEDIUM, HIGH, CRITICAL
    private double sourceLat;
    private double sourceLng;
    private Double destLat;
    private Double destLng;
    private String preferredVehicle; // AMBULANCE, FIRE_ENGINE, ANY

    public Long getUserId()                   { return userId; }
    public void setUserId(Long userId)        { this.userId = userId; }
    public String getEmergencyType()          { return emergencyType; }
    public void setEmergencyType(String et)   { this.emergencyType = et; }
    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }
    public String getPriority()               { return priority; }
    public void setPriority(String p)         { this.priority = p; }
    public double getSourceLat()              { return sourceLat; }
    public void setSourceLat(double sourceLat){ this.sourceLat = sourceLat; }
    public double getSourceLng()              { return sourceLng; }
    public void setSourceLng(double sourceLng){ this.sourceLng = sourceLng; }
    public Double getDestLat()                { return destLat; }
    public void setDestLat(Double destLat)    { this.destLat = destLat; }
    public Double getDestLng()                { return destLng; }
    public void setDestLng(Double destLng)    { this.destLng = destLng; }
    public String getPreferredVehicle()       { return preferredVehicle; }
    public void setPreferredVehicle(String pv){ this.preferredVehicle = pv; }
}
