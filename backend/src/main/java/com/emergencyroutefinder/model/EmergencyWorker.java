package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/**
 * Represents an emergency worker (ambulance or fire-engine driver).
 *
 * Demonstrates: Classes & Objects, Encapsulation, Constructors
 */
public class EmergencyWorker {

    public enum WorkerType   { AMBULANCE_DRIVER, FIRE_ENGINE_DRIVER }
    public enum Availability { AVAILABLE, BUSY, OFFLINE }

    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String passwordHash;
    private WorkerType workerType;
    private Availability availability;
    private Double locationLat;
    private Double locationLng;
    private LocalDateTime locationUpdatedAt;
    private String licenseId;
    private boolean active;
    private String fcmToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public EmergencyWorker() {}

    public EmergencyWorker(String fullName, String email, String mobile,
                           String passwordHash, WorkerType workerType) {
        this.fullName     = fullName;
        this.email        = email;
        this.mobile       = mobile;
        this.passwordHash = passwordHash;
        this.workerType   = workerType;
        this.availability = Availability.OFFLINE;
        this.active       = true;
    }

    // ── Business methods ─────────────────────────────────────────────────────

    public boolean isAvailable() {
        return availability == Availability.AVAILABLE;
    }

    public boolean canHandleVehicleType(String vehicleType) {
        if ("AMBULANCE".equals(vehicleType))   return workerType == WorkerType.AMBULANCE_DRIVER;
        if ("FIRE_ENGINE".equals(vehicleType)) return workerType == WorkerType.FIRE_ENGINE_DRIVER;
        return true; // "ANY"
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public String getFullName()                     { return fullName; }
    public void setFullName(String fullName)        { this.fullName = fullName; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getMobile()                       { return mobile; }
    public void setMobile(String mobile)            { this.mobile = mobile; }

    public String getPasswordHash()                 { return passwordHash; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }

    public WorkerType getWorkerType()               { return workerType; }
    public void setWorkerType(WorkerType workerType){ this.workerType = workerType; }

    public Availability getAvailability()           { return availability; }
    public void setAvailability(Availability availability) { this.availability = availability; }

    public Double getLocationLat()                  { return locationLat; }
    public void setLocationLat(Double locationLat)  { this.locationLat = locationLat; }

    public Double getLocationLng()                  { return locationLng; }
    public void setLocationLng(Double locationLng)  { this.locationLng = locationLng; }

    public LocalDateTime getLocationUpdatedAt()                         { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt)   { this.locationUpdatedAt = locationUpdatedAt; }

    public String getLicenseId()                    { return licenseId; }
    public void setLicenseId(String licenseId)      { this.licenseId = licenseId; }

    public boolean isActive()                       { return active; }
    public void setActive(boolean active)           { this.active = active; }

    public String getFcmToken()                     { return fcmToken; }
    public void setFcmToken(String fcmToken)        { this.fcmToken = fcmToken; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt){ this.updatedAt = updatedAt; }
}
