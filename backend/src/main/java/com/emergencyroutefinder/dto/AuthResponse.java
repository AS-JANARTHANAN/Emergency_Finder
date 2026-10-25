package com.emergencyroutefinder.dto;

/** Authentication response — JWT token + principal metadata. */
public class AuthResponse {
    private String token;
    private String role;
    private Long principalId;
    private String fullName;
    private String email;

    public AuthResponse() {}

    public AuthResponse(String token, String role, Long principalId, String fullName, String email) {
        this.token = token; this.role = role; this.principalId = principalId;
        this.fullName = fullName; this.email = email;
    }

    public String getToken()                  { return token; }
    public void setToken(String token)        { this.token = token; }
    public String getRole()                   { return role; }
    public void setRole(String role)          { this.role = role; }
    public Long getPrincipalId()              { return principalId; }
    public void setPrincipalId(Long id)       { this.principalId = id; }
    public String getFullName()               { return fullName; }
    public void setFullName(String fullName)  { this.fullName = fullName; }
    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }
}
