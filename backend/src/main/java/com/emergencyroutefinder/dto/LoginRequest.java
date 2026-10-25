package com.emergencyroutefinder.dto;

/** Login request DTO — email + password. */
public class LoginRequest {
    private String email;
    private String password;
    private String role;

    public String getEmail()              { return email; }
    public void setEmail(String email)    { this.email = email; }
    public String getPassword()           { return password; }
    public void setPassword(String p)     { this.password = p; }
    public String getRole()               { return role; }
    public void setRole(String role)      { this.role = role; }
}
