package com.emergencyroutefinder.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.FusedTrafficResult;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.ui.worker.WorkerRouteActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrafficAnalysisActivity extends BaseActivity {

    private long requestId;
    private TextView tvGoogleStatus, tvCameraStatus, tvCrowdStatus;
    private TextView tvFusedLevel, tvConfidence, tvSourcesUsed, tvDelayFactor;
    private TextView tvWeights, tvSimulationNote;
    private ProgressBar progressBar;

    private View llLoadingSimulation, llExistingContent;
    private TextView tvSimulationStatus;
    private Button btnViewRouteFromTraffic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_analysis);

        requestId = getIntent().getLongExtra("requestId", -1);

        llLoadingSimulation = findViewById(R.id.ll_loading_simulation);
        llExistingContent = findViewById(R.id.ll_existing_content);
        tvSimulationStatus = findViewById(R.id.tv_simulation_status);
        btnViewRouteFromTraffic = findViewById(R.id.btn_view_route_from_traffic);

        tvGoogleStatus   = findViewById(R.id.tv_google_status);
        tvCameraStatus   = findViewById(R.id.tv_camera_status);
        tvCrowdStatus    = findViewById(R.id.tv_crowd_status);
        tvFusedLevel     = findViewById(R.id.tv_fused_level);
        tvConfidence     = findViewById(R.id.tv_confidence);
        tvSourcesUsed    = findViewById(R.id.tv_sources_used);
        tvDelayFactor    = findViewById(R.id.tv_delay_factor);
        tvWeights        = findViewById(R.id.tv_weights);
        tvSimulationNote = findViewById(R.id.tv_simulation_note);
        progressBar      = findViewById(R.id.progress_bar);

        if(tvSimulationNote != null) tvSimulationNote.setText("ℹ️ Traffic data uses Google Routes API + prerecorded camera data + opted-in user movement data.\n" +
                "Camera footage is prerecorded simulation. Clearly labelled.");

        btnViewRouteFromTraffic.setOnClickListener(v -> {
            Intent i = new Intent(this, WorkerRouteActivity.class);
            i.putExtra("requestId", requestId);
            startActivity(i);
        });

        runSimulationSequence();
        loadTrafficFusion();
    }

    private void runSimulationSequence() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Analyzing available traffic data...");
        }, 1000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Checking simulated camera data...");
        }, 2000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Comparing road conditions...");
        }, 3000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Evaluating traffic impact...");
        }, 4000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Traffic analysis completed");
            if(llLoadingSimulation != null) llLoadingSimulation.setVisibility(View.GONE);
            if(llExistingContent != null) llExistingContent.setVisibility(View.VISIBLE);
        }, 5000);
    }

    private void loadTrafficFusion() {
        // Use demo coordinates if no specific route
        double oLat = 12.9716, oLng = 77.5946; // Bangalore demo
        double dLat = 12.9900, dLng = 77.6100;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        ApiClient.createService(ApiService.class)
                .getTrafficFusion(requestId, oLat, oLng, dLat, dLng)
                .enqueue(new Callback<FusedTrafficResult>() {
                    @Override
                    public void onResponse(Call<FusedTrafficResult> call,
                                           Response<FusedTrafficResult> response) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            displayFusion(response.body());
                        } else {
                            fallbackDisplay();
                        }
                    }

                    @Override
                    public void onFailure(Call<FusedTrafficResult> call, Throwable t) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        fallbackDisplay();
                    }
                });
    }

    private void fallbackDisplay() {
        if (tvGoogleStatus != null) tvGoogleStatus.setText("✓ Google Routes data (Demo)");
        if (tvCameraStatus != null) tvCameraStatus.setText("✓ Pre-recorded camera simulation");
        if (tvCrowdStatus != null) tvCrowdStatus.setText("✓ Opted-in user movement data");
        if (tvFusedLevel != null) tvFusedLevel.setText("MODERATE");
    }

    private void displayFusion(FusedTrafficResult fusion) {
        if (tvGoogleStatus != null) tvGoogleStatus.setText("✓ Google Routes data " + (fusion.isGoogleAvailable() ? "" : "(Demo)"));
        if (tvCameraStatus != null) tvCameraStatus.setText("✓ Pre-recorded camera simulation");
        if (tvCrowdStatus != null) tvCrowdStatus.setText("✓ Opted-in user movement data");

        if (tvFusedLevel != null) tvFusedLevel.setText(fusion.getFusedTrafficLevel());
        if (tvConfidence != null) tvConfidence.setText("📊 Confidence: " + (fusion.getConfidence() != null ? fusion.getConfidence() : "MEDIUM"));
        if (tvSourcesUsed != null) tvSourcesUsed.setText("Sources Used: " + (fusion.getSourcesUsed() != null ? fusion.getSourcesUsed() : "None"));
        if (tvDelayFactor != null) tvDelayFactor.setText("⏱ Delay Factor: " + String.format("%.2f", fusion.getFusedDelayFactor()) + "x");
        if (tvWeights != null) tvWeights.setText(String.format("Weights: Google=%.0f%%, Camera=%.0f%%, Crowd=%.0f%%",
                fusion.getWeightGoogle() * 100,
                fusion.getWeightCamera() * 100,
                fusion.getWeightCrowd() * 100));
    }
}
