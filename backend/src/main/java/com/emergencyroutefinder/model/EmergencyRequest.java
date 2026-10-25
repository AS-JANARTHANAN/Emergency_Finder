package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/**
 * Represents a user-submitted emergency request and its full lifecycle.
 *
 * Status transitions (valid):
 *   PENDING → SEARCHING → ASSIGNED → ACCEPTED → RESPONDING → ARRIVED → COMPLETED
 *   Any state → CANCELLED
 *
 * Demonstrates: Classes & Objects, Encapsulation, Constructors, Enum types
 */
public class EmergencyRequest {

    public enum EmergencyType   { MEDICAL, ACCIDENT, FIRE, OTHER }
    public enum Priority        { LOW, MEDIUM, HIGH, CRITICAL }
    public enum VehiclePreference { AMBULANCE, FIRE_ENGINE, ANY }

    public enum Status {
        PENDING, SEARCHING, ASSIGNED, ACCEPTED, RESPONDING, ARRIVED, COMPLETED, CANCELLED;

        /**
         * Validates that a status transition is allowed.
         * Demonstrates: Exception Handling + business rule enforcement
         */
        public boolean canTransitionTo(Status newStatus) {
            return switch (this) {
                case PENDING    -> newStatus == SEARCHING  || newStatus == CANCELLED;
                case SEARCHING  -> newStatus == ASSIGNED   || newStatus == CANCELLED;
                case ASSIGNED   -> newStatus == ACCEPTED   || newStatus == SEARCHING || newStatus == CANCELLED;
                case ACCEPTED   -> newStatus == RESPONDING || newStatus == CANCELLED;
                case RESPONDING -> newStatus == ARRIVED    || newStatus == CANCELLED;
                case ARRIVED    -> newStatus == COMPLETED  || newStatus == CANCELLED;
                case COMPLETED, CANCELLED -> false;
            };
        }
    }

    private Long id;
    private Long userId;
    private EmergencyType emergencyType;
    private String description;
    private Priority priority;
    private double sourceLat;
    private double sourceLng;
    private Double destLat;
    private Double destLng;
    private VehiclePreference preferredVehicle;
    private Status status;
    private Long assignedWorkerId;
    private Long assignedVehicleId;
    private Long recommendedRouteId;
    private Long selectedHospitalId;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public EmergencyRequest() {}

    public EmergencyRequest(Long userId, EmergencyType emergencyType, Priority priority,
                            double sourceLat, double sourceLng) {
        this.userId        = userId;
        this.emergencyType = emergencyType;
        this.priority      = priority;
        this.sourceLat     = sourceLat;
        this.sourceLng     = sourceLng;
        this.status        = Status.PENDING;
        this.preferredVehicle = VehiclePreference.ANY;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public Long getUserId()                         { return userId; }
    public void setUserId(Long userId)              { this.userId = userId; }

    public EmergencyType getEmergencyType()         { return emergencyType; }
    public void setEmergencyType(EmergencyType t)   { this.emergencyType = t; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public Priority getPriority()                   { return priority; }
    public void setPriority(Priority priority)      { this.priority = priority; }

    public double getSourceLat()                    { return sourceLat; }
    public void setSourceLat(double sourceLat)      { this.sourceLat = sourceLat; }

    public double getSourceLng()                    { return sourceLng; }
    public void setSourceLng(double sourceLng)      { this.sourceLng = sourceLng; }

    public Double getDestLat()                      { return destLat; }
    public void setDestLat(Double destLat)          { this.destLat = destLat; }

    public Double getDestLng()                      { return destLng; }
    public void setDestLng(Double destLng)          { this.destLng = destLng; }

    public VehiclePreference getPreferredVehicle()              { return preferredVehicle; }
    public void setPreferredVehicle(VehiclePreference pv)       { this.preferredVehicle = pv; }

    public Status getStatus()                       { return status; }
    public void setStatus(Status status)            { this.status = status; }

    public Long getAssignedWorkerId()               { return assignedWorkerId; }
    public void setAssignedWorkerId(Long id)        { this.assignedWorkerId = id; }

    public Long getAssignedVehicleId()              { return assignedVehicleId; }
    public void setAssignedVehicleId(Long id)       { this.assignedVehicleId = id; }

    public Long getRecommendedRouteId()             { return recommendedRouteId; }
    public void setRecommendedRouteId(Long id)      { this.recommendedRouteId = id; }

    public Long getSelectedHospitalId()             { return selectedHospitalId; }
    public void setSelectedHospitalId(Long id)      { this.selectedHospitalId = id; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }

    public LocalDateTime getAcceptedAt()            { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime t)      { this.acceptedAt = t; }

    public LocalDateTime getRespondedAt()           { return respondedAt; }
    public void setRespondedAt(LocalDateTime t)     { this.respondedAt = t; }

    public LocalDateTime getArrivedAt()             { return arrivedAt; }
    public void setArrivedAt(LocalDateTime t)       { this.arrivedAt = t; }

    public LocalDateTime getCompletedAt()           { return completedAt; }
    public void setCompletedAt(LocalDateTime t)     { this.completedAt = t; }

    public LocalDateTime getCancelledAt()           { return cancelledAt; }
    public void setCancelledAt(LocalDateTime t)     { this.cancelledAt = t; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)       { this.updatedAt = t; }
}
