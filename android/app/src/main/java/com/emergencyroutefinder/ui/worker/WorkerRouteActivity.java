package com.emergencyroutefinder.ui.worker;

import android.os.Bundle;
import android.preference.PreferenceManager;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.network.FirebaseManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class WorkerRouteActivity extends BaseActivity {
    private MapView map;
    private long requestId;

    @Override 
    protected void onCreate(Bundle s) { 
        super.onCreate(s); 
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_worker_route); 
        
        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        requestId = getIntent().getLongExtra("requestId", -1);
        if (requestId != -1) {
            loadRoutePoints();
        }
    }

    private void loadRoutePoints() {
        FirebaseManager.getInstance().getEmergencyRequest(String.valueOf(requestId))
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        try {
                            EmergencyRequest req = doc.toObject(EmergencyRequest.class);
                            if (req != null) {
                                GeoPoint userPt = new GeoPoint(req.getSourceLat(), req.getSourceLng());
                                Marker userM = new Marker(map);
                                userM.setPosition(userPt);
                                userM.setTitle("Emergency Location");
                                map.getOverlays().add(userM);
                                map.getController().setCenter(userPt);
                                
                                String workerId = req.getAssignedWorkerId();
                                if (workerId != null) {
                                    FirebaseManager.getInstance().getDb().collection("Locations").document(workerId)
                                            .get().addOnSuccessListener(locDoc -> {
                                                if (locDoc != null && locDoc.exists()) {
                                                    Double lat = locDoc.getDouble("lat");
                                                    Double lng = locDoc.getDouble("lng");
                                                    if (lat != null && lng != null) {
                                                        GeoPoint workerPt = new GeoPoint(lat, lng);
                                                        Marker workerM = new Marker(map);
                                                        workerM.setPosition(workerPt);
                                                        workerM.setTitle("My Vehicle");
                                                        map.getOverlays().add(workerM);
                                                        
                                                        // Draw Route
                                                        drawRoute(workerPt, userPt);
                                                        
                                                        map.invalidate();
                                                    }
                                                }
                                            });
                                }
                                map.invalidate();
                            }
                        } catch (Exception ignored) {}
                    }
                });
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        new Thread(() -> {
            try {
                String url = String.format("https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                        start.getLongitude(), start.getLatitude(), end.getLongitude(), end.getLatitude());
                
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JSONObject json = new JSONObject(jsonStr);
                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject geometry = routes.getJSONObject(0).getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        
                        List<GeoPoint> geoPoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            geoPoints.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
                        }
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Polyline line = new Polyline();
                            line.setPoints(geoPoints);
                            line.getOutlinePaint().setColor(0xFF0000FF); // Blue
                            line.getOutlinePaint().setStrokeWidth(8.0f);
                            map.getOverlays().add(0, line); // Add behind markers
                            
                            // Zoom to fit route
                            map.zoomToBoundingBox(line.getBounds(), true, 100);
                            map.invalidate();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
