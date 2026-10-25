package com.emergencyroutefinder.ui.user;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.util.SessionManager;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Request Emergency Activity — emergency submission form.
 * Automatically captures GPS, validates fields, submits request.
 *
 * Demonstrates: GUI, GPS location, background threading, exception handling
 */
public class RequestEmergencyActivity extends BaseActivity {

    private static final String TAG = "RequestEmergencyAct";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int LOCATION_SETTINGS_REQUEST   = 101;
    /** Max time to wait for a fresh GPS fix before giving up (ms). */
    private static final long LOCATION_TIMEOUT_MS = 15_000;

    private Spinner spinnerType, spinnerVehicle, spinnerPriority;
    private EditText etDescription, etDestLat, etDestLng;
    private RadioGroup rgLocationSource;
    private View cvGpsLocation, cvManualLocation;
    private EditText etManualAddress;
    private Button btnResolveAddress;
    private TextView tvManualLocationStatus;
    private TextView tvCurrentLocation, tvLocationStatus;
    private Button btnSubmit, btnRefreshLocation;
    private ProgressBar progressBar;
    private TextView tvError;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private CancellationTokenSource cancellationTokenSource;

    private double currentLat = 0, currentLng = 0;
    private boolean locationCaptured = false;
    private boolean locationRequestInFlight = false;
    private String manualAddressLabel = null;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeoutRunnable = () -> {
        Log.w(TAG, "Location request timed out after " + LOCATION_TIMEOUT_MS + "ms");
        stopLocationUpdates();
        // Try last-known as final fallback
        tryLastKnownLocation(true /* fromTimeout */);
    };

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_emergency);

        spinnerType       = findViewById(R.id.spinner_emergency_type);
        spinnerVehicle    = findViewById(R.id.spinner_vehicle);
        spinnerPriority   = findViewById(R.id.spinner_priority);
        etDescription     = findViewById(R.id.et_description);
        etDestLat         = findViewById(R.id.et_dest_lat);
        etDestLng         = findViewById(R.id.et_dest_lng);
        rgLocationSource  = findViewById(R.id.rg_location_source);
        cvGpsLocation     = findViewById(R.id.cv_gps_location);
        cvManualLocation  = findViewById(R.id.cv_manual_location);
        etManualAddress   = findViewById(R.id.et_manual_address);
        btnResolveAddress = findViewById(R.id.btn_resolve_address);
        tvManualLocationStatus = findViewById(R.id.tv_manual_location_status);
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        tvLocationStatus  = findViewById(R.id.tv_location_status);
        btnSubmit         = findViewById(R.id.btn_submit_emergency);
        btnRefreshLocation= findViewById(R.id.btn_refresh_location);
        progressBar       = findViewById(R.id.progress_bar);
        tvError           = findViewById(R.id.tv_error);

        setupSpinners();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rgLocationSource.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_gps) {
                cvGpsLocation.setVisibility(View.VISIBLE);
                cvManualLocation.setVisibility(View.GONE);
                manualAddressLabel = null;
                if (!locationCaptured) startLocationFlow();
            } else if (checkedId == R.id.rb_manual) {
                cvGpsLocation.setVisibility(View.GONE);
                cvManualLocation.setVisibility(View.VISIBLE);
                stopLocationUpdates();
            }
        });

        btnResolveAddress.setOnClickListener(v -> resolveManualAddress());

        btnSubmit.setOnClickListener(v -> submitEmergency());
        btnRefreshLocation.setOnClickListener(v -> {
            hideError();
            startLocationFlow();
        });

        // Kick off location flow immediately
        startLocationFlow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spinners
    // ──────────────────────────────────────────────────────────────────────────
    private void setupSpinners() {
        String[] types = {"MEDICAL", "ACCIDENT", "FIRE", "OTHER"};
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types));

        String[] vehicles = {"ANY", "AMBULANCE", "FIRE_ENGINE"};
        spinnerVehicle.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicles));

        String[] priorities = {"HIGH", "CRITICAL", "MEDIUM", "LOW"};
        spinnerPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Location flow: permission → settings check → fetch
    // ──────────────────────────────────────────────────────────────────────────

    /** Entry point — checks permission, then proceeds to fetch. */
    private void startLocationFlow() {
        if (locationRequestInFlight) {
            tvLocationStatus.setText("Already capturing location…");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            checkLocationSettingsThenFetch();
        }
    }

    private void requestLocationPermission() {
        tvLocationStatus.setText("Requesting location permission…");
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettingsThenFetch();
            } else {
                tvLocationStatus.setText("❌ Location permission denied.");
                showError("Location permission is required to send an emergency request. "
                        + "Grant it in Settings and tap Refresh Location.");
            }
        }
    }

    /**
     * Use LocationSettingsRequest to ensure GPS / network provider is ON.
     * If not, prompt the user via the system dialog (no need to go to Settings manually).
     */
    private void checkLocationSettingsThenFetch() {
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(req)
                .setAlwaysShow(true)
                .build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(response -> {
                    Log.d(TAG, "Location settings satisfied, fetching location…");
                    fetchFreshLocation();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        // Show the system dialog to turn on GPS
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(
                                    RequestEmergencyActivity.this,
                                    LOCATION_SETTINGS_REQUEST);
                        } catch (IntentSender.SendIntentException ex) {
                            Log.e(TAG, "Could not resolve location settings", ex);
                            showLocationServicesError();
                        }
                    } else {
                        showLocationServicesError();
                    }
                });
    }

    private void showLocationServicesError() {
        tvLocationStatus.setText("❌ Location services are off.");
        showError("Please enable GPS/Location Services on your device and tap Refresh Location.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "User enabled location settings");
                fetchFreshLocation();
            } else {
                showLocationServicesError();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Core location fetch — uses requestLocationUpdates for reliability
    // ──────────────────────────────────────────────────────────────────────────

    private void fetchFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        stopLocationUpdates(); // Cancel any in-flight request

        tvLocationStatus.setText("📡 Acquiring GPS signal…");
        tvCurrentLocation.setText("");
        locationCaptured = false;
        locationRequestInFlight = true;
        btnRefreshLocation.setEnabled(false);

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .setWaitForAccurateLocation(false) // Don't block; accept first fix
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                android.location.Location loc = result.getLastLocation();
                if (loc != null) {
                    Log.i(TAG, "Got fresh location: " + loc.getLatitude()
                            + ", " + loc.getLongitude()
                            + " acc=" + loc.getAccuracy() + "m");
                    onLocationSuccess(loc);
                }
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability availability) {
                if (!availability.isLocationAvailable()) {
                    Log.w(TAG, "Location not available (provider off?)");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper());

        // Schedule timeout
        timeoutHandler.removeCallbacks(locationTimeoutRunnable);
        timeoutHandler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT_MS);

        Log.d(TAG, "Location updates requested, timeout in " + LOCATION_TIMEOUT_MS + "ms");
    }

    private void onLocationSuccess(android.location.Location location) {
        timeoutHandler.removeCallbacks(locationTimeoutRunnable);
        stopLocationUpdates();

        currentLat = location.getLatitude();
        currentLng = location.getLongitude();
        locationCaptured = true;
        manualAddressLabel = null;

        runOnUiThread(() -> {
            tvCurrentLocation.setText(String.format(
                    "Lat: %.6f, Lng: %.6f  (±%.0fm)",
                    currentLat, currentLng, location.getAccuracy()));
            tvLocationStatus.setText("✅ Location captured");
            btnRefreshLocation.setEnabled(true);
            hideError();
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Manual Address Resolution
    // ──────────────────────────────────────────────────────────────────────────

    private void resolveManualAddress() {
        String addressString = etManualAddress.getText().toString().trim();
        if (addressString.isEmpty()) {
            showError("Please enter an address to resolve.");
            return;
        }

        hideError();
        tvManualLocationStatus.setText("Resolving address...");
        tvManualLocationStatus.setTextColor(0xFF757575); // Grey
        
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(RequestEmergencyActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(addressString, 1);
                
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        currentLat = address.getLatitude();
                        currentLng = address.getLongitude();
                        locationCaptured = true;
                        
                        // Construct a readable address label
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i)).append(", ");
                        }
                        if (sb.length() > 2) sb.setLength(sb.length() - 2); // Remove trailing comma
                        manualAddressLabel = sb.toString();

                        tvManualLocationStatus.setText(String.format(
                                "✅ Location resolved:\n%s\nLat: %.6f, Lng: %.6f",
                                manualAddressLabel, currentLat, currentLng));
                        tvManualLocationStatus.setTextColor(0xFF2E7D32); // Green
                    } else {
                        tvManualLocationStatus.setText("❌ Could not resolve address. Try a different query.");
                        tvManualLocationStatus.setTextColor(0xFFC62828); // Red
                        locationCaptured = false;
                        manualAddressLabel = null;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Geocoding failed", e);
                runOnUiThread(() -> {
                    tvManualLocationStatus.setText("❌ Error resolving address. Check internet connection.");
                    tvManualLocationStatus.setTextColor(0xFFC62828); // Red
                    locationCaptured = false;
                    manualAddressLabel = null;
                });
            }
        }).start();
    }

    /**
     * Called on timeout or as a secondary attempt after getCurrentLocation() returns null.
     * Tries getLastLocation(); if still null, shows an actionable error.
     */
    private void tryLastKnownLocation(boolean fromTimeout) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            tvLocationStatus.setText("❌ Location permission denied.");
            btnRefreshLocation.setEnabled(true);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                long ageMs = System.currentTimeMillis() - loc.getTime();
                Log.i(TAG, "Using last-known location (age=" + ageMs + "ms): "
                        + loc.getLatitude() + ", " + loc.getLongitude());

                // Accept last-known if ≤ 5 minutes old
                if (ageMs < 5 * 60 * 1000L) {
                    currentLat = loc.getLatitude();
                    currentLng = loc.getLongitude();
                    locationCaptured = true;
                    manualAddressLabel = null;
                    tvCurrentLocation.setText(String.format(
                            "Lat: %.6f, Lng: %.6f  (last known, %.0fs ago)",
                            currentLat, currentLng, ageMs / 1000.0));
                    tvLocationStatus.setText("⚠️ Using recent last-known location");
                } else {
                    // Stale — don't silently accept
                    tvLocationStatus.setText("⚠️ Last-known location is stale (>"
                            + (ageMs / 60000) + " min old). Move outdoors and tap Refresh.");
                    showError("GPS fix timed out. Move to an area with better GPS signal and tap Refresh Location.");
                }
            } else {
                if (fromTimeout) {
                    tvLocationStatus.setText("❌ Could not get location.");
                    showError("GPS could not obtain a fix. Ensure GPS is enabled and move outdoors, then tap Refresh Location.");
                }
            }
            btnRefreshLocation.setEnabled(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "getLastLocation failed: " + e.getMessage());
            tvLocationStatus.setText("❌ Location error: " + e.getMessage());
            btnRefreshLocation.setEnabled(true);
        });

        locationRequestInFlight = false;
    }

    /** Stops any ongoing location updates and cancels the timeout. */
    private void stopLocationUpdates() {
        timeoutHandler.removeCallbacks(locationTimeoutRunnable);
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            Log.d(TAG, "Location updates stopped");
        }
        locationRequestInFlight = false;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Submit
    // ──────────────────────────────────────────────────────────────────────────

    private void submitEmergency() {
        hideError();

        if (!locationCaptured) {
            showError("Current location not captured yet. Tap 'Refresh Location'.");
            return;
        }

        String type     = spinnerType.getSelectedItem().toString();
        String vehicle  = spinnerVehicle.getSelectedItem().toString();
        String priority = spinnerPriority.getSelectedItem().toString();
        String desc     = etDescription.getText().toString().trim();

        SessionManager session = SessionManager.getInstance(this);
        String userId = session.getToken();
        
        long requestId = System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("id", requestId);
        body.put("userId", userId);
        body.put("emergencyType", type);
        
        // Append manual address to description if used
        String finalDesc = desc.isEmpty() ? type + " emergency" : desc;
        if (manualAddressLabel != null) {
            finalDesc += " | Location Source: Manual Address (" + manualAddressLabel + ")";
        }
        body.put("description", finalDesc);
        
        body.put("priority", priority);
        body.put("sourceLat", currentLat);
        body.put("sourceLng", currentLng);
        body.put("preferredVehicle", vehicle);
        body.put("status", "ASSIGNED"); // Directly assigning

        // Optional destination
        String destLatStr = etDestLat.getText().toString().trim();
        String destLngStr = etDestLng.getText().toString().trim();
        if (!destLatStr.isEmpty() && !destLngStr.isEmpty()) {
            try {
                body.put("destLat", Double.parseDouble(destLatStr));
                body.put("destLng", Double.parseDouble(destLngStr));
            } catch (NumberFormatException ignored) {}
        }

        setLoading(true);
        
        // Find an available worker
        FirebaseManager.getInstance().getAvailableWorkers().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                // For simplicity in a small app (5-8 devices), pick the first available worker.
                // A production app would fetch all their locations and compare distances.
                DocumentSnapshot workerDoc = task.getResult().getDocuments().get(0);
                String workerId = workerDoc.getId();
                
                body.put("assignedWorkerId", workerId);
                
                FirebaseManager.getInstance().createEmergencyRequest(String.valueOf(requestId), body)
                        .addOnCompleteListener(saveTask -> {
                            setLoading(false);
                            if (saveTask.isSuccessful()) {
                                Toast.makeText(RequestEmergencyActivity.this,
                                        "Emergency request submitted & assigned!", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(RequestEmergencyActivity.this, LiveResponseMapActivity.class);
                                intent.putExtra("requestId", requestId);
                                intent.putExtra("sourceLat", currentLat);
                                intent.putExtra("sourceLng", currentLng);
                                startActivity(intent);
                                finish();
                            } else {
                                showError("Failed to save request. Try again.");
                            }
                        });
            } else {
                setLoading(false);
                showError("No emergency workers available at the moment! Please try again later.");
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(loading ? "Submitting..." : "SUBMIT EMERGENCY REQUEST");
    }
}
