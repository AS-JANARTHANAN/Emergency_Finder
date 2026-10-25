package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/**
 * Represents a registered emergency user.
 *
 * Demonstrates: Classes & Objects, Encapsulation (private fields + getters/setters),
 *               Constructors
 */
public class User {

    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String passwordHash;        // BCrypt hashed — never plaintext
    private String role;                // "USER" or "ADMIN"
    private boolean active;
    private Double locationLat;
    private Double locationLng;
    private LocalDateTime locationUpdatedAt;
    private boolean allowCrowdTraffic;  // Explicit consent for traffic contribution
    private String fcmToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public User() {}

    public User(String fullName, String email, String mobile, String passwordHash) {
        this.fullName     = fullName;
        this.email        = email;
        this.mobile       = mobile;
        this.passwordHash = passwordHash;
        this.role         = "USER";
        this.active       = true;
        this.allowCrowdTraffic = false;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getFullName()                { return fullName; }
    public void setFullName(String fullName)   { this.fullName = fullName; }

    public String getEmail()                   { return email; }
    public void setEmail(String email)         { this.email = email; }

    public String getMobile()                  { return mobile; }
    public void setMobile(String mobile)       { this.mobile = mobile; }

    public String getPasswordHash()            { return passwordHash; }
    public void setPasswordHash(String hash)   { this.passwordHash = hash; }

    public String getRole()                    { return role; }
    public void setRole(String role)           { this.role = role; }

    public boolean isActive()                  { return active; }
    public void setActive(boolean active)      { this.active = active; }

    public Double getLocationLat()             { return locationLat; }
    public void setLocationLat(Double lat)     { this.locationLat = lat; }

    public Double getLocationLng()             { return locationLng; }
    public void setLocationLng(Double lng)     { this.locationLng = lng; }

    public LocalDateTime getLocationUpdatedAt()                    { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public boolean isAllowCrowdTraffic()                           { return allowCrowdTraffic; }
    public void setAllowCrowdTraffic(boolean allowCrowdTraffic)    { this.allowCrowdTraffic = allowCrowdTraffic; }

    public String getFcmToken()                { return fcmToken; }
    public void setFcmToken(String fcmToken)   { this.fcmToken = fcmToken; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)  { this.updatedAt = t; }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', role='" + role + "'}";
    }
}
