package com.emergencyroutefinder.ui.worker;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.emergencyroutefinder.EmergencyApp;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.model.EmergencyWorker;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.ui.auth.LoginActivity;
import com.emergencyroutefinder.util.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Worker Dashboard â€” main hub for emergency workers.
 *
 * Shows:
 * - Availability toggle (AVAILABLE / OFFLINE)
 * - GPS location streaming when available
 * - Real-time incoming emergency polling and audible alert
 * - Active emergency card if currently assigned
 * - History of past assignments and profile access
 */
public class WorkerDashboardActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQ = 200;
    private static final long POLL_INTERVAL_MS = 3000L;

    private TextView tvWelcome, tvAvailabilityStatus;
    private Button btnToggleAvailability, btnHistory, btnProfile, btnLogout;
    private View cardActiveEmergency;
    private String currentAvailability = "OFFLINE";

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private boolean isPollingActive = false;
    private long lastHandledRequestId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SessionManager session = SessionManager.getInstance(this);

        tvWelcome              = findViewById(R.id.tv_welcome);
        tvAvailabilityStatus   = findViewById(R.id.tv_availability_status);
        btnToggleAvailability  = findViewById(R.id.btn_toggle_availability);
        btnHistory             = findViewById(R.id.btn_history);
        btnProfile             = findViewById(R.id.btn_profile);
        btnLogout              = findViewById(R.id.btn_logout);
        cardActiveEmergency    = findViewById(R.id.card_active_emergency);

        tvWelcome.setText("Hello, " + session.getFullName() + "!");
        updateAvailabilityDisplay(currentAvailability);

        btnToggleAvailability.setOnClickListener(v -> toggleAvailability(session.getToken()));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, WorkerHistoryActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, WorkerProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            stopAssignmentPolling();
            setAvailability(session.getToken(), "OFFLINE", null);
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Request location permissions if needed
        checkLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncBackendWorkerStatus(SessionManager.getInstance(this).getToken());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep polling if online so background alerts work while app is open
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAssignmentPolling();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQ && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateCurrentGpsLocation(SessionManager.getInstance(this).getToken(), null);
        }
    }

    private void syncBackendWorkerStatus(String workerId) {
        FirebaseManager.getInstance().getUserRecord(workerId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String avail = task.getResult().getString("availability");
                        currentAvailability = avail != null ? avail : "OFFLINE";
                        runOnUiThread(() -> {
                            updateAvailabilityDisplay(currentAvailability);
                            if ("AVAILABLE".equals(currentAvailability)) {
                                startAssignmentPolling(workerId);
                            } else {
                                stopAssignmentPolling();
                            }
                        });
                    }
                    checkActiveEmergency(workerId);
                });
    }

    private void toggleAvailability(String workerId) {
        String newAvail = "AVAILABLE".equals(currentAvailability) ? "OFFLINE" : "AVAILABLE";
        if ("AVAILABLE".equals(newAvail)) {
            btnToggleAvailability.setEnabled(false);
            btnToggleAvailability.setText("Going Online...");
            updateCurrentGpsLocation(workerId, () -> {
                setAvailability(workerId, newAvail, () -> {
                    btnToggleAvailability.setEnabled(true);
                    startAssignmentPolling(workerId);
                });
            });
        } else {
            stopAssignmentPolling();
            setAvailability(workerId, newAvail, null);
        }
    }

    private void updateCurrentGpsLocation(String workerId, Runnable onComplete) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (onComplete != null) onComplete.run();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                currentLat = loc.getLatitude();
                currentLng = loc.getLongitude();

                FirebaseManager.getInstance().updateLocation(workerId, currentLat, currentLng)
                        .addOnCompleteListener(task -> {
                            if (onComplete != null) onComplete.run();
                        });
            } else {
                if (onComplete != null) onComplete.run();
            }
        }).addOnFailureListener(e -> {
            if (onComplete != null) onComplete.run();
        });
    }

    private void setAvailability(String workerId, String availability, Runnable onComplete) {
        FirebaseManager.getInstance().updateWorkerStatus(workerId, availability)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentAvailability = availability;
                        runOnUiThread(() -> updateAvailabilityDisplay(availability));
                        if (onComplete != null) onComplete.run();
                    } else {
                        runOnUiThread(() -> {
                            btnToggleAvailability.setEnabled(true);
                            Toast.makeText(WorkerDashboardActivity.this,
                                    "Failed to update status", Toast.LENGTH_SHORT).show();
                        });
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    private void updateAvailabilityDisplay(String avail) {
        boolean available = "AVAILABLE".equals(avail);
        tvAvailabilityStatus.setText("Status: " + avail);
        tvAvailabilityStatus.setTextColor(available
                ? getColor(android.R.color.holo_green_dark)
                : getColor(android.R.color.darker_gray));
        btnToggleAvailability.setText(available ? "Go Offline" : "Go Available");
    }

    private com.google.firebase.firestore.ListenerRegistration assignmentListener;

    private synchronized void startAssignmentPolling(String workerId) {
        if (isPollingActive) return;
        isPollingActive = true;

        assignmentListener = FirebaseManager.getInstance().getDb().collection("Emergencies")
                .whereEqualTo("assignedWorkerId", workerId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || !isPollingActive || !"AVAILABLE".equals(currentAvailability)) return;
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            try {
                                EmergencyRequest req = doc.toObject(EmergencyRequest.class);
                                if (req != null) {
                                    String status = req.getStatus() != null ? req.getStatus().name() : "";
                                    // Notice: IDs are Long in the model but Firestore has String. We'll use doc.getId()
                                    // if we changed ID to String. But we can't change ID easily.
                                    if ("ASSIGNED".equals(status) && req.getId() != lastHandledRequestId) {
                                        lastHandledRequestId = req.getId();
                                        stopAssignmentPolling();
                                        notifyIncomingEmergency(req);
                                        return;
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
    }

    private synchronized void stopAssignmentPolling() {
        isPollingActive = false;
        if (assignmentListener != null) {
            assignmentListener.remove();
            assignmentListener = null;
        }
    }

    private void notifyIncomingEmergency(EmergencyRequest req) {
        // Ringtone playback is now handled directly by IncomingEmergencyActivity

        String typeStr = req.getEmergencyType() != null ? req.getEmergencyType().name() : "EMERGENCY";
        String priorityStr = req.getPriority() != null ? req.getPriority().name() : "HIGH";

        double distKm = 0.0;
        if (currentLat != 0 && currentLng != 0 && req.getSourceLat() != 0) {
            distKm = computeDistanceKm(currentLat, currentLng, req.getSourceLat(), req.getSourceLng());
        }
        String distanceStr = distKm > 0 ? String.format(Locale.US, "%.1f", distKm) : "Near";

        // Create Intent to open IncomingEmergencyActivity
        Intent intent = new Intent(this, IncomingEmergencyActivity.class);
        intent.putExtra("requestId", req.getId());
        intent.putExtra("emergencyType", typeStr);
        intent.putExtra("priority", priorityStr);
        intent.putExtra("distanceKm", distanceStr);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) req.getId(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // System notification with heads-up / full-screen intent
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EmergencyApp.CHANNEL_ID_EMERGENCY)
                .setSmallIcon(R.drawable.ic_emergency_alert)
                .setContentTitle("ðŸš¨ EMERGENCY REQUEST ASSIGNED")
                .setContentText(typeStr + " | Distance: " + distanceStr + " km")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("New emergency request nearby!\nType: " + typeStr
                                + "\nPriority: " + priorityStr
                                + "\nDistance: " + distanceStr + " km\nTap to accept or decline."))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVibrate(new long[]{0, 500, 250, 500, 250, 500})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) req.getId(), builder.build());
        }

        // Immediately launch IncomingEmergencyActivity on screen
        startActivity(intent);
    }

    private double computeDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void checkActiveEmergency(String workerId) {
        FirebaseManager.getInstance().getRequestsByWorker(workerId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean hasActive = false;
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            try {
                                EmergencyRequest req = doc.toObject(EmergencyRequest.class);
                                if (req != null) {
                                    String status = req.getStatus() != null ? req.getStatus().name() : "";
                                    if ("ACCEPTED".equals(status) || "RESPONDING".equals(status) || "ARRIVED".equals(status)) {
                                        hasActive = true;
                                        runOnUiThread(() -> {
                                            cardActiveEmergency.setVisibility(View.VISIBLE);
                                            cardActiveEmergency.setOnClickListener(v -> {
                                                Intent i = new Intent(WorkerDashboardActivity.this, ActiveEmergencyActivity.class);
                                                i.putExtra("requestId", req.getId());
                                                startActivity(i);
                                            });
                                        });
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (!hasActive) {
                            runOnUiThread(() -> cardActiveEmergency.setVisibility(View.GONE));
                        }
                    }
                });
    }
}
