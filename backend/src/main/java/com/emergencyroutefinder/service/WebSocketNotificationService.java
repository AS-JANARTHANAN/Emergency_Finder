package com.emergencyroutefinder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket notification service — broadcasts real-time updates to connected clients.
 *
 * Topics:
 *   /topic/request/{requestId}  — request status updates (user + worker)
 *   /topic/worker/{workerId}    — incoming emergency for a specific worker
 *   /topic/location/{requestId} — worker location updates
 *
 * Demonstrates: Real-time communication, Spring STOMP
 */
@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Notify all subscribers to a request topic of a status change. */
    public void notifyRequestStatusChange(Long requestId, String newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("status", newStatus);
        payload.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/request/" + requestId;
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("WS → {} : status={}", destination, newStatus);
    }

    /** Send worker location update to user's map. */
    public void notifyWorkerLocation(Long requestId, double lat, double lng) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("lat", lat);
        payload.put("lng", lng);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/location/" + requestId, payload);
    }

    /** Notify user of a message (e.g. worker searching, hospital selected). */
    public void notifyUser(Long requestId, String status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("status", status);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/request/" + requestId, payload);
    }

    /** Push route update to both user and worker. */
    public void notifyRouteUpdate(Long requestId, Long recommendedRouteId, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("event", "ROUTE_UPDATED");
        payload.put("recommendedRouteId", recommendedRouteId);
        payload.put("reason", reason);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/request/" + requestId, payload);
    }
}
