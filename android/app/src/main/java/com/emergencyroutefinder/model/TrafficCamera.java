package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class TrafficCamera {
    @SerializedName("id")            private long id;
    @SerializedName("cameraName")    private String cameraName;
    @SerializedName("roadName")      private String roadName;
    @SerializedName("lat")           private double lat;
    @SerializedName("lng")           private double lng;
    @SerializedName("videoFilePath") private String videoFilePath;
    @SerializedName("trafficLevel")  private String trafficLevel;
    @SerializedName("trafficScore")  private double trafficScore;

    public long getId()              { return id; }
    public String getCameraName()    { return cameraName; }
    public String getRoadName()      { return roadName; }
    public double getLat()           { return lat; }
    public double getLng()           { return lng; }
    public String getVideoFilePath() { return videoFilePath; }
    public String getTrafficLevel()  { return trafficLevel; }
    public double getTrafficScore()  { return trafficScore; }
}
