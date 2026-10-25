package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class FusedTrafficResult {
    @SerializedName("fusedTrafficLevel") private String fusedTrafficLevel;
    @SerializedName("fusedDelayFactor")  private double fusedDelayFactor;
    @SerializedName("fusedScore")        private double fusedScore;
    @SerializedName("confidence")        private String confidence;
    @SerializedName("sourcesUsed")       private String sourcesUsed;
    @SerializedName("googleLevel")       private String googleLevel;
    @SerializedName("cameraLevel")       private String cameraLevel;
    @SerializedName("crowdLevel")        private String crowdLevel;
    @SerializedName("googleAvailable")   private boolean googleAvailable;
    @SerializedName("cameraAvailable")   private boolean cameraAvailable;
    @SerializedName("crowdAvailable")    private boolean crowdAvailable;
    @SerializedName("weightGoogle")      private double weightGoogle;
    @SerializedName("weightCamera")      private double weightCamera;
    @SerializedName("weightCrowd")       private double weightCrowd;

    public String getFusedTrafficLevel()   { return fusedTrafficLevel; }
    public double getFusedDelayFactor()    { return fusedDelayFactor; }
    public double getFusedScore()          { return fusedScore; }
    public String getConfidence()          { return confidence; }
    public String getSourcesUsed()         { return sourcesUsed; }
    public String getGoogleLevel()         { return googleLevel; }
    public String getCameraLevel()         { return cameraLevel; }
    public String getCrowdLevel()          { return crowdLevel; }
    public boolean isGoogleAvailable()     { return googleAvailable; }
    public boolean isCameraAvailable()     { return cameraAvailable; }
    public boolean isCrowdAvailable()      { return crowdAvailable; }
    public double getWeightGoogle()        { return weightGoogle; }
    public double getWeightCamera()        { return weightCamera; }
    public double getWeightCrowd()         { return weightCrowd; }
}
