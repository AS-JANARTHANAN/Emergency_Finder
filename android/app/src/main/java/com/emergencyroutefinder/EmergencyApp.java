package com.emergencyroutefinder;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.util.SessionManager;

/**
 * Application class — initializes global singletons.
 *
 * Demonstrates: Android Application lifecycle, Singleton pattern
 */
public class EmergencyApp extends Application {

    public static final String CHANNEL_ID_EMERGENCY = "emergency_alerts";
    public static final String CHANNEL_ID_STATUS    = "status_updates";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enforce consistent light theme across all Android versions/devices
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize ApiClient with backend URL from BuildConfig
        ApiClient.init(BuildConfig.BACKEND_URL);

        // Initialize notification channels
        createNotificationChannels();

        // Ensure screenshots are permitted across all activities
        enableScreenshotsGlobally();
    }

    private void enableScreenshotsGlobally() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {
                clearSecureFlag(activity);
            }

            @Override
            public void onActivityStarted(android.app.Activity activity) {
                clearSecureFlag(activity);
            }

            @Override
            public void onActivityResumed(android.app.Activity activity) {
                clearSecureFlag(activity);
            }

            @Override
            public void onActivityPaused(android.app.Activity activity) {
                clearSecureFlag(activity);
            }

            @Override
            public void onActivityStopped(android.app.Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {}

            @Override
            public void onActivityDestroyed(android.app.Activity activity) {}

            private void clearSecureFlag(android.app.Activity activity) {
                if (activity != null && activity.getWindow() != null) {
                    activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity.setRecentsScreenshotEnabled(true);
                    }
                    android.view.View decor = activity.getWindow().peekDecorView();
                    if (decor != null) {
                        decor.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
                            if (activity.getWindow() != null) {
                                activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
                            }
                        });
                        decor.getViewTreeObserver().addOnPreDrawListener(() -> {
                            if (activity.getWindow() != null) {
                                activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
                            }
                            return true;
                        });
                    }
                }
            }
        });
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Emergency alert channel — high priority, custom sound
            NotificationChannel emergencyChannel = new NotificationChannel(
                    CHANNEL_ID_EMERGENCY,
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            emergencyChannel.setDescription("Incoming emergency request notifications");
            emergencyChannel.enableVibration(true);
            emergencyChannel.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 500});
            emergencyChannel.setShowBadge(true);
            manager.createNotificationChannel(emergencyChannel);

            // Status update channel — default priority
            NotificationChannel statusChannel = new NotificationChannel(
                    CHANNEL_ID_STATUS,
                    "Emergency Status",
                    NotificationManager.IMPORTANCE_DEFAULT);
            statusChannel.setDescription("Emergency request status updates");
            manager.createNotificationChannel(statusChannel);
        }
    }
}
