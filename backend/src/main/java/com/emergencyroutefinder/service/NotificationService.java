package com.emergencyroutefinder.service;

import com.emergencyroutefinder.model.*;
import com.emergencyroutefinder.repository.NotificationRepository;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Notification Service — Firebase Cloud Messaging (FCM) push notifications.
 *
 * Demonstrates: Async/Multithreading, FCM integration, Exception Handling,
 *               Graceful fallback when Firebase is unavailable
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final com.emergencyroutefinder.config.FirebaseConfig firebaseConfig;

    public NotificationService(NotificationRepository notificationRepository,
                                com.emergencyroutefinder.config.FirebaseConfig firebaseConfig) {
        this.notificationRepository = notificationRepository;
        this.firebaseConfig = firebaseConfig;
    }

    /**
     * Send emergency alert to a worker via FCM (runs asynchronously on background thread).
     */
    @Async("taskExecutor")
    public void sendWorkerEmergencyAlert(EmergencyWorker worker, EmergencyRequest request,
                                          EmergencyVehicle vehicle, double distKm) {
        String title = "🚨 EMERGENCY REQUEST";
        String body  = String.format("%s Emergency | Distance: %.1f km | Tap to view",
                request.getEmergencyType(), distKm);

        // Store notification in DB regardless of FCM availability
        com.emergencyroutefinder.model.Notification dbNotif = new com.emergencyroutefinder.model.Notification(
                com.emergencyroutefinder.model.Notification.RecipientType.WORKER, worker.getId(),
                request.getId(), title, body, "EMERGENCY_ALERT");

        if (worker.getFcmToken() != null && firebaseConfig.isFirebaseEnabled()) {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("requestId",      String.valueOf(request.getId()));
                data.put("emergencyType",  request.getEmergencyType().name());
                data.put("priority",       request.getPriority().name());
                data.put("distanceKm",     String.format("%.2f", distKm));
                data.put("sourceLat",      String.valueOf(request.getSourceLat()));
                data.put("sourceLng",      String.valueOf(request.getSourceLng()));
                data.put("type",           "EMERGENCY_ALERT");

                Message fcmMessage = Message.builder()
                        .setToken(worker.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setSound("emergency_alert")
                                        .setVibrateTimingsInMillis(new long[]{0, 500, 250, 500})
                                        .setPriority(AndroidNotification.Priority.MAX)
                                        .setChannelId("emergency_alerts")
                                        .build())
                                .build())
                        .build();

                String messageId = FirebaseMessaging.getInstance().send(fcmMessage);
                dbNotif.setFcmMessageId(messageId);
                log.info("FCM emergency alert sent to worker {} — messageId: {}", worker.getId(), messageId);

            } catch (FirebaseMessagingException e) {
                log.error("FCM send failed for worker {}: {}", worker.getId(), e.getMessage());
                // Continue — notification still saved to DB for in-app polling
            }
        } else {
            log.info("FCM not available — notification stored in DB for in-app polling (worker {})", worker.getId());
        }

        notificationRepository.save(dbNotif);
    }

    /**
     * Notify user that their request was assigned / status changed.
     */
    @Async("taskExecutor")
    public void sendUserStatusUpdate(Long userId, String fcmToken, Long requestId,
                                     String title, String body) {
        com.emergencyroutefinder.model.Notification dbNotif = new com.emergencyroutefinder.model.Notification(
                com.emergencyroutefinder.model.Notification.RecipientType.USER, userId, requestId,
                title, body, "STATUS_UPDATE");

        if (fcmToken != null && firebaseConfig.isFirebaseEnabled()) {
            try {
                Message msg = Message.builder()
                        .setToken(fcmToken)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title).setBody(body).build())
                        .putData("requestId", String.valueOf(requestId))
                        .putData("type", "STATUS_UPDATE")
                        .build();
                FirebaseMessaging.getInstance().send(msg);
            } catch (FirebaseMessagingException e) {
                log.error("FCM user notification failed: {}", e.getMessage());
            }
        }

        notificationRepository.save(dbNotif);
    }
}
