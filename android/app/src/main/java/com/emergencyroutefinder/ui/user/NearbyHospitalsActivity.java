package com.emergencyroutefinder.ui.user;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.Hospital;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.ui.BaseActivity;
import android.preference.PreferenceManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Nearby Hospitals Activity — shows hospitals on map, sorted by distance.
 */
public class NearbyHospitalsActivity extends BaseActivity {

    private MapView map;
    private double lat, lng;
    private long requestId;
    private RecyclerView rvHospitals;
    private ProgressBar progressBar;
    private List<Hospital> hospitals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_nearby_hospitals);

        lat        = getIntent().getDoubleExtra("lat", 0);
        lng        = getIntent().getDoubleExtra("lng", 0);
        requestId  = getIntent().getLongExtra("requestId", -1);

        rvHospitals = findViewById(R.id.rv_hospitals);
        progressBar = findViewById(R.id.progress_bar);

        rvHospitals.setLayoutManager(new LinearLayoutManager(this));

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getController().setZoom(13.0);
        
        if (lat != 0) {
            GeoPoint pos = new GeoPoint(lat, lng);
            Marker m = new Marker(map);
            m.setPosition(pos);
            m.setTitle("Emergency Location");
            map.getOverlays().add(m);
            map.getController().setCenter(pos);
        }

        loadHospitals();
    }



    private void loadHospitals() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.createService(ApiService.class).getNearbyHospitals(lat, lng, 20.0)
                .enqueue(new Callback<List<Hospital>>() {
                    @Override
                    public void onResponse(Call<List<Hospital>> call, Response<List<Hospital>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            hospitals = response.body();
                            plotHospitals();
                            HospitalAdapter adapter = new HospitalAdapter(hospitals, hospitalId -> {
                                // Select hospital for this request
                                java.util.Map<String, Long> body = new java.util.HashMap<>();
                                body.put("hospitalId", hospitalId);
                                ApiClient.createService(ApiService.class)
                                        .selectHospital(requestId, body)
                                        .enqueue(new Callback<Void>() {
                                            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                                                Toast.makeText(NearbyHospitalsActivity.this,
                                                        "Hospital selected", Toast.LENGTH_SHORT).show();
                                            }
                                            @Override public void onFailure(Call<Void> c, Throwable t) {}
                                        });
                            });
                            rvHospitals.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Hospital>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NearbyHospitalsActivity.this,
                                "Could not load hospitals", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void plotHospitals() {
        if (map == null || hospitals.isEmpty()) return;
        for (Hospital h : hospitals) {
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(h.getLat(), h.getLng()));
            m.setTitle(h.getName());
            m.setSnippet(String.format("%.1f km | %s", h.getDistanceKm(),
                    h.isEmergencyAvailable() ? "Emergency ✓" : "No Emergency"));
            map.getOverlays().add(m);
        }
        map.invalidate();
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
