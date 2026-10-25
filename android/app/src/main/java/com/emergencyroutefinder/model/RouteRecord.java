package com.emergencyroutefinder.model;

import com.google.gson.annotations.SerializedName;

/** Android-side RouteRecord model. */
public class RouteRecord {
    @SerializedName("id")                  private long id;
    @SerializedName("requestId")           private long requestId;
    @SerializedName("routeLabel")          private String routeLabel;
    @SerializedName("originLat")           private double originLat;
    @SerializedName("originLng")           private double originLng;
    @SerializedName("destLat")             private double destLat;
    @SerializedName("destLng")             private double destLng;
    @SerializedName("distanceMeters")      private Integer distanceMeters;
    @SerializedName("baseDurationSec")     private Integer baseDurationSec;
    @SerializedName("trafficDurationSec")  private Integer trafficDurationSec;
    @SerializedName("encodedPolyline")     private String encodedPolyline;
    @SerializedName("googleTrafficState")  private String googleTrafficState;
    @SerializedName("cameraTrafficScore")  private Double cameraTrafficScore;
    @SerializedName("crowdTrafficScore")   private Double crowdTrafficScore;
    @SerializedName("fusedTrafficScore")   private Double fusedTrafficScore;
    @SerializedName("finalRouteScore")     private Double finalRouteScore;
    @SerializedName("confidence")          private String confidence;
    @SerializedName("isRecommended")       private boolean recommended;
    @SerializedName("sourcesUsed")         private String sourcesUsed;
    @SerializedName("recommendationReason")private String recommendationReason;

    public long getId()                         { return id; }
    public long getRequestId()                  { return requestId; }
    public String getRouteLabel()               { return routeLabel; }
    public double getOriginLat()                { return originLat; }
    public double getOriginLng()                { return originLng; }
    public double getDestLat()                  { return destLat; }
    public double getDestLng()                  { return destLng; }
    public Integer getDistanceMeters()          { return distanceMeters; }
    public Integer getBaseDurationSec()         { return baseDurationSec; }
    public Integer getTrafficDurationSec()      { return trafficDurationSec; }
    public String getEncodedPolyline()          { return encodedPolyline; }
    public String getGoogleTrafficState()       { return googleTrafficState; }
    public Double getCameraTrafficScore()       { return cameraTrafficScore; }
    public Double getCrowdTrafficScore()        { return crowdTrafficScore; }
    public Double getFusedTrafficScore()        { return fusedTrafficScore; }
    public Double getFinalRouteScore()          { return finalRouteScore; }
    public String getConfidence()               { return confidence; }
    public boolean isRecommended()              { return recommended; }
    public String getSourcesUsed()              { return sourcesUsed; }
    public String getRecommendationReason()     { return recommendationReason; }
}
