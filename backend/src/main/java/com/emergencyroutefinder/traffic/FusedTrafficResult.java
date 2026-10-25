package com.emergencyroutefinder.traffic;

import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.model.TrafficObservation;
import java.util.List;

/** Result of the traffic fusion engine for a route. */
public class FusedTrafficResult {

    private String fusedTrafficLevel;
    private double fusedDelayFactor;
    private double fusedScore;
    private RouteRecord.Confidence confidence;
    private String sourcesUsed;

    // Per-source breakdown
    private String googleLevel;
    private String cameraLevel;
    private String crowdLevel;
    private boolean googleAvailable;
    private boolean cameraAvailable;
    private boolean crowdAvailable;

    // Configured weights
    private double weightGoogle;
    private double weightCamera;
    private double weightCrowd;

    // Raw observations per source
    private List<TrafficObservation> googleObservations;
    private List<TrafficObservation> cameraObservations;
    private List<TrafficObservation> crowdObservations;

    public FusedTrafficResult() {}

    public String getFusedTrafficLevel()                        { return fusedTrafficLevel; }
    public void setFusedTrafficLevel(String v)                  { this.fusedTrafficLevel = v; }
    public double getFusedDelayFactor()                         { return fusedDelayFactor; }
    public void setFusedDelayFactor(double v)                   { this.fusedDelayFactor = v; }
    public double getFusedScore()                               { return fusedScore; }
    public void setFusedScore(double v)                         { this.fusedScore = v; }
    public RouteRecord.Confidence getConfidence()               { return confidence; }
    public void setConfidence(RouteRecord.Confidence c)         { this.confidence = c; }
    public String getSourcesUsed()                              { return sourcesUsed; }
    public void setSourcesUsed(String v)                        { this.sourcesUsed = v; }
    public String getGoogleLevel()                              { return googleLevel; }
    public void setGoogleLevel(String v)                        { this.googleLevel = v; }
    public String getCameraLevel()                              { return cameraLevel; }
    public void setCameraLevel(String v)                        { this.cameraLevel = v; }
    public String getCrowdLevel()                               { return crowdLevel; }
    public void setCrowdLevel(String v)                         { this.crowdLevel = v; }
    public boolean isGoogleAvailable()                          { return googleAvailable; }
    public void setGoogleAvailable(boolean v)                   { this.googleAvailable = v; }
    public boolean isCameraAvailable()                          { return cameraAvailable; }
    public void setCameraAvailable(boolean v)                   { this.cameraAvailable = v; }
    public boolean isCrowdAvailable()                           { return crowdAvailable; }
    public void setCrowdAvailable(boolean v)                    { this.crowdAvailable = v; }
    public double getWeightGoogle()                             { return weightGoogle; }
    public void setWeightGoogle(double v)                       { this.weightGoogle = v; }
    public double getWeightCamera()                             { return weightCamera; }
    public void setWeightCamera(double v)                       { this.weightCamera = v; }
    public double getWeightCrowd()                              { return weightCrowd; }
    public void setWeightCrowd(double v)                        { this.weightCrowd = v; }
    public List<TrafficObservation> getGoogleObservations()     { return googleObservations; }
    public void setGoogleObservations(List<TrafficObservation> v){ this.googleObservations = v; }
    public List<TrafficObservation> getCameraObservations()     { return cameraObservations; }
    public void setCameraObservations(List<TrafficObservation> v){ this.cameraObservations = v; }
    public List<TrafficObservation> getCrowdObservations()      { return crowdObservations; }
    public void setCrowdObservations(List<TrafficObservation> v){ this.crowdObservations = v; }
}
