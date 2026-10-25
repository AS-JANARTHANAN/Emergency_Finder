package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")                private long id;
    @SerializedName("fullName")          private String fullName;
    @SerializedName("email")             private String email;
    @SerializedName("mobile")            private String mobile;
    @SerializedName("allowCrowdTraffic") private boolean allowCrowdTraffic;

    public long getId()                  { return id; }
    public String getFullName()          { return fullName; }
    public String getEmail()             { return email; }
    public String getMobile()            { return mobile; }
    public boolean isAllowCrowdTraffic() { return allowCrowdTraffic; }
}
