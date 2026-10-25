package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.location.WorkerLocationService;
import com.emergencyroutefinder.repository.NotificationRepository;
import com.emergencyroutefinder.repository.UserRepository;
import com.emergencyroutefinder.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User Controller — profile, location, crowd traffic consent, FCM token, notifications.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final WorkerLocationService locationService;
    private final NotificationRepository notificationRepository;

    public UserController(UserRepository userRepository,
                           WorkerLocationService locationService,
                           NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.locationService = locationService;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> {
                    // Never expose password hash in API response
                    u.setPasswordHash("[PROTECTED]");
                    return ResponseEntity.ok(u);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body) {
        locationService.updateUserLocation(id, body.get("lat"), body.get("lng"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/crowd-consent")
    public ResponseEntity<Map<String, Object>> setCrowdConsent(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean allow = body.getOrDefault("allow", false);
        userRepository.updateCrowdConsent(id, allow);
        return ResponseEntity.ok(Map.of("userId", id, "allowCrowdTraffic", allow));
    }

    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        userRepository.updateFcmToken(id, body.get("token"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/crowd-location")
    public ResponseEntity<Void> submitCrowdLocation(@RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(body.get("userId").toString());
        double lat  = Double.parseDouble(body.get("lat").toString());
        double lng  = Double.parseDouble(body.get("lng").toString());
        Double speed = body.containsKey("speedKmh") ? Double.parseDouble(body.get("speedKmh").toString()) : null;

        locationService.submitCrowdLocation(userId, lat, lng, speed);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/notifications")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long id) {
        return ResponseEntity.ok(notificationRepository.findByRecipient("USER", id));
    }

    @PutMapping("/notifications/{notifId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long notifId) {
        notificationRepository.markRead(notifId);
        return ResponseEntity.ok().build();
    }
}
