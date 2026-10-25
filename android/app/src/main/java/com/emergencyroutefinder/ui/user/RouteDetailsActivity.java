package com.emergencyroutefinder.ui.user;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.ui.BaseActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.preference.PreferenceManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

public class RouteDetailsActivity extends BaseActivity {

    private MapView map;
    private long requestId;
    private double sourceLat, sourceLng;

    private TextView tvRecommendedLabel, tvRecommendedReason, tvTrafficSummary;
    private RecyclerView rvRoutes;
    private ProgressBar progressBar;

    private View llLoadingSimulation, llResultCard, llExistingContent;
    private TextView tvSimulationStatus;
    private Button btnViewRouteFromAi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_route_details);

        requestId = getIntent().getLongExtra("requestId", -1);
        sourceLat = getIntent().getDoubleExtra("sourceLat", 0);
        sourceLng = getIntent().getDoubleExtra("sourceLng", 0);

        llLoadingSimulation = findViewById(R.id.ll_loading_simulation);
        llResultCard = findViewById(R.id.ll_result_card);
        llExistingContent = findViewById(R.id.ll_existing_content);
        tvSimulationStatus = findViewById(R.id.tv_simulation_status);
        btnViewRouteFromAi = findViewById(R.id.btn_view_route_from_ai);

        tvRecommendedLabel  = findViewById(R.id.tv_recommended_label);
        tvRecommendedReason = findViewById(R.id.tv_recommended_reason);
        tvTrafficSummary    = findViewById(R.id.tv_traffic_summary);
        rvRoutes            = findViewById(R.id.rv_routes);
        progressBar         = findViewById(R.id.progress_bar);

        rvRoutes.setLayoutManager(new LinearLayoutManager(this));

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        GeoPoint pos = new GeoPoint(sourceLat, sourceLng);
        Marker m = new Marker(map);
        m.setPosition(pos);
        m.setTitle("Emergency Location");
        map.getOverlays().add(m);
        map.getController().setCenter(pos);
        map.invalidate();

        btnViewRouteFromAi.setOnClickListener(v -> {
            llResultCard.setVisibility(View.GONE);
            llExistingContent.setVisibility(View.VISIBLE);
        });

        runSimulationSequence();
        loadRoutes();
    }

    private void runSimulationSequence() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Analyzing available routes...");
        }, 1000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Checking traffic conditions...");
        }, 2000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Comparing route options...");
        }, 3000);
        handler.postDelayed(() -> {
            if(tvSimulationStatus != null) tvSimulationStatus.setText("Selecting the best route...");
        }, 4000);
        handler.postDelayed(() -> {
            if(llLoadingSimulation != null) llLoadingSimulation.setVisibility(View.GONE);
            if(llResultCard != null) llResultCard.setVisibility(View.VISIBLE);
        }, 5000);
    }

    private void loadRoutes() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.createService(ApiService.class).getRoutes(requestId)
                .enqueue(new Callback<List<RouteRecord>>() {
                    @Override
                    public void onResponse(Call<List<RouteRecord>> call,
                                           Response<List<RouteRecord>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            List<RouteRecord> routes = response.body();
                            displayRoutes(routes);
                        } else {
                            // Trigger computation if no routes yet
                            computeRoutes();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<RouteRecord>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RouteDetailsActivity.this,
                                "Could not load routes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void computeRoutes() {
        progressBar.setVisibility(View.VISIBLE);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("requestId", requestId);
        body.put("originLat", sourceLat);
        body.put("originLng", sourceLng);
        body.put("destLat", sourceLat + 0.05); // Demo destination
        body.put("destLng", sourceLng + 0.05);

        ApiClient.createService(ApiService.class).computeRoutes(body)
                .enqueue(new Callback<java.util.Map>() {
                    @Override
                    public void onResponse(Call<java.util.Map> call, Response<java.util.Map> r) {
                        // Re-load after 2 seconds
                        new Handler().postDelayed(() -> loadRoutes(), 2000);
                    }
                    @Override
                    public void onFailure(Call<java.util.Map> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void displayRoutes(List<RouteRecord> routes) {
        if (routes.isEmpty()) {
            tvRecommendedLabel.setText("No routes available yet.");
            return;
        }

        RouteRecord recommended = null;
        for (RouteRecord r : routes) {
            if (r.isRecommended()) { recommended = r; break; }
        }
        if (recommended == null) recommended = routes.get(0);

        final RouteRecord rec = recommended;
        tvRecommendedLabel.setText("✅ Recommended: " + rec.getRouteLabel());
        tvRecommendedReason.setText(rec.getRecommendationReason() != null
                ? rec.getRecommendationReason()
                : "Best route based on traffic analysis.");

        tvTrafficSummary.setText(buildTrafficSummary(rec));

        RouteListAdapter adapter = new RouteListAdapter(routes, recommendedId ->
                Toast.makeText(this, "Route " + recommendedId + " selected", Toast.LENGTH_SHORT).show());
        rvRoutes.setAdapter(adapter);
    }

    private String buildTrafficSummary(RouteRecord rec) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Traffic Analysis:\n");
        sb.append("• Google Traffic: ").append(rec.getGoogleTrafficState() != null ? rec.getGoogleTrafficState() : "?").append("\n");
        sb.append("• Camera Score: ").append(rec.getCameraTrafficScore() != null ? String.format("%.2f", rec.getCameraTrafficScore()) : "?").append("\n");
        sb.append("• Crowd Score: ").append(rec.getCrowdTrafficScore() != null ? String.format("%.2f", rec.getCrowdTrafficScore()) : "?").append("\n");
        sb.append("• Fused Score: ").append(rec.getFusedTrafficScore() != null ? String.format("%.2f", rec.getFusedTrafficScore()) : "?").append("\n");
        sb.append("• Confidence: ").append(rec.getConfidence() != null ? rec.getConfidence() : "MEDIUM").append("\n");
        sb.append("• Sources: ").append(rec.getSourcesUsed() != null ? rec.getSourcesUsed() : "Available sources");
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
