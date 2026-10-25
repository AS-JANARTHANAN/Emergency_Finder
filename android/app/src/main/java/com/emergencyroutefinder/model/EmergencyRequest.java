package com.emergencyroutefinder.model;

import com.google.gson.annotations.SerializedName;

/** Android-side model for emergency request — mirrors backend EmergencyRequest. */
public class EmergencyRequest {

    public EmergencyRequest() {} // Needed for Firestore


    @SerializedName("id")              private long id;
    @SerializedName("userId")          private String userId;
    @SerializedName("emergencyType")   private EmergencyType emergencyType;
    @SerializedName("description")     private String description;
    @SerializedName("priority")        private Priority priority;
    @SerializedName("sourceLat")       private double sourceLat;
    @SerializedName("sourceLng")       private double sourceLng;
    @SerializedName("destLat")         private Double destLat;
    @SerializedName("destLng")         private Double destLng;
    @SerializedName("preferredVehicle")private VehiclePreference preferredVehicle;
    @SerializedName("status")          private Status status;
    @SerializedName("assignedWorkerId")private String assignedWorkerId;
    @SerializedName("createdAt")       private String createdAt;
    @SerializedName("completedAt")     private String completedAt;

    public enum EmergencyType { MEDICAL, ACCIDENT, FIRE, OTHER }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum VehiclePreference { AMBULANCE, FIRE_ENGINE, ANY }
    public enum Status { PENDING, SEARCHING, ASSIGNED, ACCEPTED, RESPONDING, ARRIVED, COMPLETED, CANCELLED }

    public long getId()                         { return id; }
    public String getUserId()                     { return userId; }
    public EmergencyType getEmergencyType()     { return emergencyType; }
    public String getDescription()             { return description; }
    public Priority getPriority()              { return priority; }
    public double getSourceLat()               { return sourceLat; }
    public double getSourceLng()               { return sourceLng; }
    public Double getDestLat()                 { return destLat; }
    public Double getDestLng()                 { return destLng; }
    public VehiclePreference getPreferredVehicle() { return preferredVehicle; }
    public Status getStatus()                  { return status; }
    public String getAssignedWorkerId()          { return assignedWorkerId; }
    public String getCreatedAt()               { return createdAt; }
    public String getCompletedAt()             { return completedAt; }

    // Setters for Firestore
    public void setId(long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setEmergencyType(EmergencyType type) { this.emergencyType = type; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setSourceLat(double lat) { this.sourceLat = lat; }
    public void setSourceLng(double lng) { this.sourceLng = lng; }
    public void setDestLat(Double lat) { this.destLat = lat; }
    public void setDestLng(Double lng) { this.destLng = lng; }
    public void setPreferredVehicle(VehiclePreference pref) { this.preferredVehicle = pref; }
    public void setStatus(Status status) { this.status = status; }
    public void setAssignedWorkerId(String id) { this.assignedWorkerId = id; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
