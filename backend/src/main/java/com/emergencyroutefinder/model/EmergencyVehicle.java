package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/**
 * Abstract base class for all emergency vehicles.
 *
 * Demonstrates: Inheritance (abstract superclass), OOP hierarchy,
 *               Encapsulation, Constructors
 *
 * Concrete subclasses: Ambulance, FireEngine
 */
public abstract class EmergencyVehicle {

    public enum VehicleType   { AMBULANCE, FIRE_ENGINE }
    public enum VehicleStatus { AVAILABLE, ASSIGNED, MAINTENANCE }

    private Long id;
    private String vehicleNumber;
    private VehicleType vehicleType;         // Overridden by subclass
    private Long workerId;
    private Double currentLat;
    private Double currentLng;
    private VehicleStatus availability;
    private Long activeRequestId;
    private LocalDateTime lastLocationUpdate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    protected EmergencyVehicle() {}

    protected EmergencyVehicle(String vehicleNumber, VehicleType vehicleType, Long workerId) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType   = vehicleType;
        this.workerId      = workerId;
        this.availability  = VehicleStatus.AVAILABLE;
        this.active        = true;
    }

    // ── Abstract methods — subclasses must implement ──────────────────────────

    /** Returns a human-readable vehicle category description */
    public abstract String getVehicleCategory();

    /** Returns the default emergency type this vehicle handles */
    public abstract String getPrimaryEmergencyType();

    // ── Business methods ─────────────────────────────────────────────────────

    public boolean isAvailable() {
        return availability == VehicleStatus.AVAILABLE;
    }

    public void assign(Long requestId) {
        this.availability    = VehicleStatus.ASSIGNED;
        this.activeRequestId = requestId;
    }

    public void release() {
        this.availability    = VehicleStatus.AVAILABLE;
        this.activeRequestId = null;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public String getVehicleNumber()                { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber){ this.vehicleNumber = vehicleNumber; }

    public VehicleType getVehicleType()             { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType){ this.vehicleType = vehicleType; }

    public Long getWorkerId()                       { return workerId; }
    public void setWorkerId(Long workerId)          { this.workerId = workerId; }

    public Double getCurrentLat()                   { return currentLat; }
    public void setCurrentLat(Double currentLat)    { this.currentLat = currentLat; }

    public Double getCurrentLng()                   { return currentLng; }
    public void setCurrentLng(Double currentLng)    { this.currentLng = currentLng; }

    public VehicleStatus getAvailability()          { return availability; }
    public void setAvailability(VehicleStatus availability){ this.availability = availability; }

    public Long getActiveRequestId()                { return activeRequestId; }
    public void setActiveRequestId(Long activeRequestId){ this.activeRequestId = activeRequestId; }

    public LocalDateTime getLastLocationUpdate()    { return lastLocationUpdate; }
    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate){ this.lastLocationUpdate = lastLocationUpdate; }

    public boolean isActive()                       { return active; }
    public void setActive(boolean active)           { this.active = active; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt){ this.updatedAt = updatedAt; }
}
