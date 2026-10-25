package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class Hospital {
    @SerializedName("id")                  private long id;
    @SerializedName("name")                private String name;
    @SerializedName("address")             private String address;
    @SerializedName("contactNumber")       private String contactNumber;
    @SerializedName("lat")                 private double lat;
    @SerializedName("lng")                 private double lng;
    @SerializedName("emergencyAvailable")  private boolean emergencyAvailable;
    @SerializedName("distanceKm")          private double distanceKm;

    public long getId()                         { return id; }
    public String getName()                     { return name; }
    public String getAddress()                  { return address; }
    public String getContactNumber()            { return contactNumber; }
    public double getLat()                      { return lat; }
    public double getLng()                      { return lng; }
    public boolean isEmergencyAvailable()       { return emergencyAvailable; }
    public double getDistanceKm()               { return distanceKm; }
}
