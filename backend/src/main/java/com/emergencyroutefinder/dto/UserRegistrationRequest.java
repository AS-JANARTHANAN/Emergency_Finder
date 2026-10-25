package com.emergencyroutefinder.dto;

public class UserRegistrationRequest {
    private String fullName;
    private String email;
    private String mobile;
    private String password;
    private String confirmPassword;
    private boolean allowCrowdTraffic = false;

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
    public boolean isAllowCrowdTraffic()      { return allowCrowdTraffic; }
    public void setAllowCrowdTraffic(boolean b){ this.allowCrowdTraffic = b; }
}
