package com.emergencyroutefinder.model;

import com.google.gson.annotations.SerializedName;

/** Android-side AuthResponse model. */
public class AuthResponse {
    @SerializedName("token")       private String token;
    @SerializedName("role")        private String role;
    @SerializedName("principalId") private long principalId;
    @SerializedName("fullName")    private String fullName;
    @SerializedName("email")       private String email;

    public String getToken()        { return token; }
    public String getRole()         { return role; }
    public long getPrincipalId()    { return principalId; }
    public String getFullName()     { return fullName; }
    public String getEmail()        { return email; }
}
