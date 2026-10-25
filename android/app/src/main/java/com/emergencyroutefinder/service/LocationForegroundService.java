package com.emergencyroutefinder.service;

import android.Manifest;
import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.network.FirebaseManager;
import com.emergencyroutefinder.util.SessionManager;
import com.google.android.gms.location.*;

import java.util.Map;

/**
 * Foreground Location Service — runs during active emergency to send GPS updates.
 *
 * Demonstrates: Android Services (foreground), GPS, Background multithreading
 */
public class LocationForegroundService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service";
    private static final int NOTIFICATION_ID = 999;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private long activeRequestId = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            activeRequestId = intent.getLongExtra("requestId", -1);
        }

        // Start as foreground service to comply with Android 12+ restrictions
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Emergency Response Active")
                .setContentText("Location tracking active for emergency response")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startLocationUpdates();
        return START_STICKY; // Restart if killed
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    sendLocationToBackend(location.getLatitude(), location.getLongitude());
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.i(TAG, "Location updates started for request " + activeRequestId);
    }

    private void sendLocationToBackend(double lat, double lng) {
        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) return;

        // We store string uid for Firebase
        String workerUid = session.getToken(); // using token as uid since we changed SessionManager token to user.getUid()
        
        FirebaseManager.getInstance().updateLocation(workerUid, lat, lng)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location sent to Firestore: " + lat + ", " + lng))
                .addOnFailureListener(e -> Log.w(TAG, "Location send failed: " + e.getMessage()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.i(TAG, "Location service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Used during active emergency response");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
