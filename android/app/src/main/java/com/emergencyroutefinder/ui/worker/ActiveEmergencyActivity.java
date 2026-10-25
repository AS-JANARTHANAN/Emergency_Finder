package com.emergencyroutefinder.ui.worker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import com.emergencyroutefinder.ui.BaseActivity;
import android.preference.PreferenceManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.emergencyroutefinder.service.LocationForegroundService;
import com.emergencyroutefinder.util.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Active Emergency Activity — Worker's screen during live response.
 *
 * Shows:
 * - Emergency location on map
 * - Status action buttons (Responding, Arrived, Complete)
 * - Route and hospital navigation buttons
 *
 * Starts location foreground service for GPS tracking.
 */
public class ActiveEmergencyActivity extends BaseActivity {

    private MapView map;
    private long requestId, assignmentId;
    private EmergencyRequest currentRequest;

    private TextView tvStatus, tvEmergencyType, tvPriority;
    private Button btnResponding, btnArrived, btnComplete;
    private Button btnViewRoute, btnViewRoutes, btnViewTraffic;

    private Handler pollingHandler;
    private static final long POLL_INTERVAL = 10000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_active_emergency);

        requestId    = getIntent().getLongExtra("requestId", -1);
        assignmentId = getIntent().getLongExtra("assignmentId", -1);

        tvStatus       = findViewById(R.id.tv_status);
        tvEmergencyType = findViewById(R.id.tv_emergency_type);
        tvPriority     = findViewById(R.id.tv_priority);
        btnResponding  = findViewById(R.id.btn_responding);
        btnArrived     = findViewById(R.id.btn_arrived);
        btnComplete    = findViewById(R.id.btn_complete);
        btnViewRoute   = findViewById(R.id.btn_view_route);
        btnViewRoutes  = findViewById(R.id.btn_view_routes);
        btnViewTraffic = findViewById(R.id.btn_view_traffic);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        // Start GPS tracking service
        Intent locationIntent = new Intent(this, LocationForegroundService.class);
        locationIntent.putExtra("requestId", requestId);
        startForegroundService(locationIntent);

        loadRequestDetails();
        setupButtons();
        startPolling();
    }

    private void loadRequestDetails() {
        FirebaseManager.getInstance().getEmergencyRequest(String.valueOf(requestId))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        try {
                            currentRequest = task.getResult().toObject(EmergencyRequest.class);
                            if (currentRequest != null) {
                                runOnUiThread(() -> {
                                    tvEmergencyType.setText(currentRequest.getEmergencyType() != null
                                            ? currentRequest.getEmergencyType().name() : "EMERGENCY");
                                    tvPriority.setText("Priority: " +
                                            (currentRequest.getPriority() != null
                                                    ? currentRequest.getPriority().name() : "HIGH"));
                                    tvStatus.setText("Status: " +
                                            (currentRequest.getStatus() != null
                                                    ? currentRequest.getStatus().name() : "ACCEPTED"));

                                    if (map != null) {
                                        GeoPoint emergency = new GeoPoint(
                                                currentRequest.getSourceLat(), currentRequest.getSourceLng());
                                        Marker marker = new Marker(map);
                                        marker.setPosition(emergency);
                                        marker.setTitle("Emergency Location");
                                        map.getOverlays().add(marker);
                                        map.getController().setCenter(emergency);
                                        map.invalidate();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // Location tracking updates for OSM
    private void updateWorkerLocationOnMap() {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
                .getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null && map != null) {
                        GeoPoint workerLoc = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                        Marker wMarker = new Marker(map);
                        wMarker.setPosition(workerLoc);
                        wMarker.setTitle("My Vehicle");
                        map.getOverlays().add(wMarker);
                        map.invalidate();
                        if (currentRequest != null) {
                            map.getController().setCenter(workerLoc);
                        }
                    }
                });
    }

    private void setupButtons() {
        String workerId = SessionManager.getInstance(this).getToken();

        btnResponding.setOnClickListener(v -> {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "RESPONDING");
            FirebaseManager.getInstance().updateEmergencyRequest(String.valueOf(requestId), body)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            runOnUiThread(() -> tvStatus.setText("Status: RESPONDING"));
                        }
                    });
        });

        btnArrived.setOnClickListener(v -> {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "ARRIVED");
            FirebaseManager.getInstance().updateEmergencyRequest(String.valueOf(requestId), body)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            runOnUiThread(() -> tvStatus.setText("Status: ARRIVED"));
                        }
                    });
        });

        btnComplete.setOnClickListener(v -> {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "COMPLETED");
            FirebaseManager.getInstance().updateEmergencyRequest(String.valueOf(requestId), body)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            stopLocationService();
                            Intent intent = new Intent(ActiveEmergencyActivity.this,
                                    WorkerDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    });
        });

        btnViewRoute.setOnClickListener(v -> {
            Intent i = new Intent(this, WorkerRouteActivity.class);
            i.putExtra("requestId", requestId);
            startActivity(i);
        });

        btnViewRoutes.setOnClickListener(v -> {
            if (currentRequest == null) {
                Toast.makeText(this, "Loading request details...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, com.emergencyroutefinder.ui.user.RouteDetailsActivity.class);
            i.putExtra("requestId", requestId);
            i.putExtra("sourceLat", currentRequest.getSourceLat());
            i.putExtra("sourceLng", currentRequest.getSourceLng());
            startActivity(i);
        });

        btnViewTraffic.setOnClickListener(v -> {
            Intent i = new Intent(this, com.emergencyroutefinder.ui.user.TrafficAnalysisActivity.class);
            i.putExtra("requestId", requestId);
            startActivity(i);
        });
    }

    private void startPolling() {
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadRequestDetails();
                pollingHandler.postDelayed(this, POLL_INTERVAL);
            }
        }, POLL_INTERVAL);
    }

    private void stopLocationService() {
        stopService(new Intent(this, LocationForegroundService.class));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingHandler != null) pollingHandler.removeCallbacksAndMessages(null);
    }
}
