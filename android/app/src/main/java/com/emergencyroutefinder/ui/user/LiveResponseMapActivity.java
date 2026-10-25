package com.emergencyroutefinder.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import com.emergencyroutefinder.BuildConfig;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.model.EmergencyWorker;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.util.SessionManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import android.preference.PreferenceManager;

import java.util.Locale;

/**
 * Live Response Map Activity — displays real-time worker dispatch & location on map.
 *
 * Demonstrates:
 * - Google Maps SDK marker plotting and bounds animation
 * - WebSocket real-time updates + resilient HTTP polling fallback
 * - Explicit status synchronization between User (Phone 1) and Worker (Phone 2)
 */
public class LiveResponseMapActivity extends BaseActivity {

    private MapView map;
    private Marker workerMarker, userMarker;
    private long requestId;
    private double sourceLat, sourceLng;

    private TextView tvStatus, tvWorkerName, tvEta;
    private Button btnViewHospitals;

    private ListenerRegistration requestListener;
    private ListenerRegistration locationListener;

    private String currentStatus = "PENDING";
    private String assignedWorkerId = null;
    private long pollCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_live_response_map);

        requestId = getIntent().getLongExtra("requestId", -1);
        sourceLat = getIntent().getDoubleExtra("sourceLat", 0);
        sourceLng = getIntent().getDoubleExtra("sourceLng", 0);

        tvStatus     = findViewById(R.id.tv_status);
        tvWorkerName = findViewById(R.id.tv_worker_name);
        tvEta        = findViewById(R.id.tv_eta);
        btnViewHospitals = findViewById(R.id.btn_view_hospitals);

        tvWorkerName.setText("Connecting to dispatch system...");
        tvEta.setText("GPS Coords: " + String.format(Locale.US, "%.5f, %.5f", sourceLat, sourceLng));

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        
        setupInitialMap();

        startFirebaseListeners();

        btnViewHospitals.setOnClickListener(v -> {
            Intent i = new Intent(this, NearbyHospitalsActivity.class);
            i.putExtra("requestId", requestId);
            i.putExtra("lat", sourceLat);
            i.putExtra("lng", sourceLng);
            startActivity(i);
        });
    }

    private void setupInitialMap() {
        if (sourceLat != 0 && sourceLng != 0) {
            GeoPoint userLocation = new GeoPoint(sourceLat, sourceLng);
            userMarker = new Marker(map);
            userMarker.setPosition(userLocation);
            userMarker.setTitle("Emergency Location");
            map.getOverlays().add(userMarker);
            map.getController().setCenter(userLocation);
        }
        map.invalidate();
    }

    private void startFirebaseListeners() {
        requestListener = FirebaseManager.getInstance().getDb()
                .collection("Emergencies").document(String.valueOf(requestId))
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    
                    try {
                        EmergencyRequest req = doc.toObject(EmergencyRequest.class);
                        if (req != null) {
                            String newStatus = req.getStatus() != null ? req.getStatus().name() : currentStatus;
                            if (!newStatus.equals(currentStatus)) {
                                currentStatus = newStatus;
                                runOnUiThread(() -> updateStatusDisplay(currentStatus));
                            }
                            
                            // Check for assigned worker
                            String workerId = req.getAssignedWorkerId();
                            if (workerId != null && !workerId.equals(assignedWorkerId)) {
                                assignedWorkerId = workerId;
                                fetchWorkerDetailsAndListenLocation(workerId);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
    }

    private void fetchWorkerDetailsAndListenLocation(String workerId) {
        // Fetch worker details (name)
        FirebaseManager.getInstance().getUserRecord(workerId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String name = task.getResult().getString("name");
                        runOnUiThread(() -> tvWorkerName.setText("🚨 Responder: " + (name != null ? name : "Emergency Worker")));
                    }
                });

        // Listen for location changes
        locationListener = FirebaseManager.getInstance().getDb()
                .collection("Locations").document(workerId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");
                    if (lat != null && lng != null) {
                        runOnUiThread(() -> updateWorkerMarker(lat, lng));
                    }
                });
    }

    private void updateStatusDisplay(String status) {
        String label;
        int color;

        switch (status) {
            case "PENDING":
            case "SEARCHING":
                label = "🔍 Locating nearest available emergency responder...";
                color = getColor(android.R.color.holo_orange_dark);
                break;
            case "ASSIGNED":
                label = "📤 Responder found! Awaiting confirmation...";
                color = getColor(android.R.color.holo_blue_dark);
                break;
            case "ACCEPTED":
                label = "✅ Responder accepted! En route to your location.";
                color = getColor(android.R.color.holo_green_dark);
                break;
            case "RESPONDING":
                label = "🚨 Responder is currently en route!";
                color = getColor(android.R.color.holo_green_dark);
                break;
            case "ARRIVED":
                label = "🏁 Responder has arrived at your location!";
                color = getColor(android.R.color.holo_green_dark);
                break;
            case "COMPLETED":
                label = "✔️ Emergency response completed.";
                color = getColor(android.R.color.darker_gray);
                break;
            case "CANCELLED":
                label = "❌ Request cancelled.";
                color = getColor(android.R.color.holo_red_dark);
                break;
            default:
                label = "Status: " + status;
                color = getColor(android.R.color.black);
                break;
        }

        tvStatus.setText(label);
        tvStatus.setTextColor(color);
    }

    private void updateWorkerMarker(double lat, double lng) {
        if (map == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);

        if (workerMarker == null) {
            workerMarker = new Marker(map);
            workerMarker.setPosition(pos);
            workerMarker.setTitle("Emergency Responder");
            map.getOverlays().add(workerMarker);
        } else {
            workerMarker.setPosition(pos);
        }

        // Compute distance and update ETA
        if (sourceLat != 0 && sourceLng != 0) {
            double distKm = computeDistanceKm(sourceLat, sourceLng, lat, lng);
            tvEta.setText(String.format(Locale.US, "Estimated Distance: %.2f km", distKm));
            map.getController().setCenter(pos);
        }
        map.invalidate();
    }

    private double computeDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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
        if (requestListener != null) requestListener.remove();
        if (locationListener != null) locationListener.remove();
    }
}
