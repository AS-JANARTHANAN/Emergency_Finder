package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class EmergencyWorker {
    @SerializedName("id")           private long id;
    @SerializedName("fullName")     private String fullName;
    @SerializedName("email")        private String email;
    @SerializedName("mobile")       private String mobile;
    @SerializedName("workerType")   private String workerType;
    @SerializedName("availability") private String availability;
    @SerializedName("locationLat")  private Double locationLat;
    @SerializedName("locationLng")  private Double locationLng;

    public long getId()             { return id; }
    public String getFullName()     { return fullName; }
    public String getEmail()        { return email; }
    public String getMobile()       { return mobile; }
    public String getWorkerType()   { return workerType; }
    public String getAvailability() { return availability; }
    public Double getLocationLat()  { return locationLat; }
    public Double getLocationLng()  { return locationLng; }
}
