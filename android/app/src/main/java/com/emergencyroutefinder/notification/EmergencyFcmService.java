package com.emergencyroutefinder.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.emergencyroutefinder.EmergencyApp;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.ui.worker.IncomingEmergencyActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.util.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Map;
import com.emergencyroutefinder.network.FirebaseManager;

/**
 * Firebase Cloud Messaging service.
 * Handles incoming FCM push notifications for emergency alerts.
 *
 * Demonstrates: FCM, Background processing, Android notification APIs
 */
public class EmergencyFcmService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        String type      = data.getOrDefault("type", "");
        String requestId = data.getOrDefault("requestId", "");

        if ("EMERGENCY_ALERT".equals(type) && !requestId.isEmpty()) {
            // Worker received emergency alert Ã¢â‚¬â€ launch IncomingEmergencyActivity
            showEmergencyNotification(data, Long.parseLong(requestId));
        } else if ("STATUS_UPDATE".equals(type) && !requestId.isEmpty()) {
            // User received status update
            showStatusNotification(data, Long.parseLong(requestId));
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // FCM token refreshed Ã¢â‚¬â€ update backend
        super.onNewToken(token);
        updateTokenOnBackend(token);
    }

    private void showEmergencyNotification(Map<String, String> data, long requestId) {
        FirebaseManager.getInstance().getEmergencyRequest(String.valueOf(requestId))
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    String status = task.getResult().getString("status");
                    if ("ASSIGNED".equals(status) || "PENDING".equals(status)) {
                        triggerNotification(data, requestId);
                    }
                }
            });
    }

    private void triggerNotification(Map<String, String> data, long requestId) {
        String emergencyType = data.getOrDefault("emergencyType", "EMERGENCY");
        String distance      = data.getOrDefault("distanceKm", "?");
        String priority      = data.getOrDefault("priority", "HIGH");

        Intent intent = new Intent(this, IncomingEmergencyActivity.class);
        intent.putExtra("requestId", requestId);
        intent.putExtra("emergencyType", emergencyType);
        intent.putExtra("priority", priority);
        intent.putExtra("distanceKm", distance);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) requestId,
                intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EmergencyApp.CHANNEL_ID_EMERGENCY)
                .setSmallIcon(R.drawable.ic_emergency_alert)
                .setContentTitle("ðŸš¨ EMERGENCY REQUEST")
                .setContentText(emergencyType + " | Distance: " + distance + " km")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Emergency type: " + emergencyType +
                                "\nDistance: " + distance + " km" +
                                "\nPriority: " + priority +
                                "\nTap to view and respond"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 500, 250, 500, 250, 500})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) requestId, builder.build());
    }

    private void showStatusNotification(Map<String, String> data, long requestId) {
        String title = data.getOrDefault("title", "Emergency Update");
        String body  = data.getOrDefault("body",  "Your emergency request status has changed.");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EmergencyApp.CHANNEL_ID_STATUS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) requestId + 10000, builder.build());
    }

    private void updateTokenOnBackend(String token) {
        try {
            SessionManager session = SessionManager.getInstance(this);
            if (!session.isLoggedIn()) return;

            long id = session.getPrincipalId();
            ApiClient.setAuthToken(session.getToken());
            ApiService api = ApiClient.createService(ApiService.class);

            Map<String, String> body = Map.of("token", token);
            Call<Void> call;
            if (session.isWorker()) {
                call = api.updateWorkerFcmToken(id, body);
            } else {
                call = api.updateUserFcmToken(id, body);
            }
            call.enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "FCM token update error: " + e.getMessage());
        }
    }
}
