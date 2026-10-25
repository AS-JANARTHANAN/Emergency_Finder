package com.emergencyroutefinder.dto;

public class WorkerRegistrationRequest {
    private String fullName;
    private String email;
    private String mobile;
    private String password;
    private String confirmPassword;
    private String workerType;     // AMBULANCE_DRIVER or FIRE_ENGINE_DRIVER
    private String vehicleType;    // AMBULANCE or FIRE_ENGINE
    private String vehicleNumber;
    private String licenseId;

    public String getFullName()               { return fullName; }
    public void setFullName(String fullName)  { this.fullName = fullName; }
    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }
    public String getMobile()                 { return mobile; }
    public void setMobile(String mobile)      { this.mobile = mobile; }
    public String getPassword()               { return password; }
    public void setPassword(String p)         { this.password = p; }
    public String getConfirmPassword()        { return confirmPassword; }
    public void setConfirmPassword(String cp) { this.confirmPassword = cp; }
    public String getWorkerType()             { return workerType; }
    public void setWorkerType(String wt)      { this.workerType = wt; }
    public String getVehicleType()            { return vehicleType; }
    public void setVehicleType(String vt)     { this.vehicleType = vt; }
    public String getVehicleNumber()          { return vehicleNumber; }
    public void setVehicleNumber(String vn)   { this.vehicleNumber = vn; }
    public String getLicenseId()              { return licenseId; }
    public void setLicenseId(String l)        { this.licenseId = l; }
}
