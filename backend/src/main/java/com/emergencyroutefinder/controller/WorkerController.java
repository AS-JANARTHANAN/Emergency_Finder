package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.location.WorkerLocationService;
import com.emergencyroutefinder.model.Ambulance;
import com.emergencyroutefinder.model.EmergencyVehicle;
import com.emergencyroutefinder.model.EmergencyWorker;
import com.emergencyroutefinder.model.FireEngine;
import com.emergencyroutefinder.repository.EmergencyWorkerRepository;
import com.emergencyroutefinder.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Worker Controller — availability, location updates, profile, FCM token.
 */
@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final EmergencyWorkerRepository workerRepository;
    private final WorkerLocationService locationService;
    private final VehicleRepository vehicleRepository;
    private final com.emergencyroutefinder.repository.AssignmentRepository assignmentRepository;

    public WorkerController(EmergencyWorkerRepository workerRepository,
                              WorkerLocationService locationService,
                              VehicleRepository vehicleRepository,
                              com.emergencyroutefinder.repository.AssignmentRepository assignmentRepository) {
        this.workerRepository = workerRepository;
        this.locationService = locationService;
        this.vehicleRepository = vehicleRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyWorker> getWorker(@PathVariable Long id) {
        Optional<EmergencyWorker> w = workerRepository.findById(id);
        return w.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<Map<String, Object>> setAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String avail = body.get("availability"); // AVAILABLE, BUSY, OFFLINE
        workerRepository.updateAvailability(id, avail);

        if ("AVAILABLE".equalsIgnoreCase(avail)) {
            assignmentRepository.clearStalePendingAssignments(id);
            Optional<EmergencyVehicle> vOpt = vehicleRepository.findByWorkerId(id);
            if (vOpt.isPresent()) {
                vehicleRepository.updateAvailability(vOpt.get().getId(), "AVAILABLE", null);
            } else {
                workerRepository.findById(id).ifPresent(w -> {
                    boolean isFire = w.getWorkerType() == EmergencyWorker.WorkerType.FIRE_ENGINE_DRIVER;
                    String vNum = (isFire ? "FIRE-" : "AMB-") + "KA-" + String.format("%04d", id);
                    EmergencyVehicle newV = isFire ? new FireEngine(vNum, id) : new Ambulance(vNum, id);
                    newV.setAvailability(EmergencyVehicle.VehicleStatus.AVAILABLE);
                    vehicleRepository.save(newV);
                });
            }
        } else if ("OFFLINE".equalsIgnoreCase(avail)) {
            vehicleRepository.findByWorkerId(id).ifPresent(v ->
                vehicleRepository.updateAvailability(v.getId(), "MAINTENANCE", null));
        }

        return ResponseEntity.ok(Map.of("workerId", id, "availability", avail));
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        double lat = Double.parseDouble(body.get("lat").toString());
        double lng = Double.parseDouble(body.get("lng").toString());
        Long activeRequestId = body.containsKey("activeRequestId")
                ? Long.parseLong(body.get("activeRequestId").toString()) : null;

        locationService.updateWorkerLocation(id, lat, lng, activeRequestId);
        return ResponseEntity.ok(Map.of("workerId", id, "lat", lat, "lng", lng));
    }

    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        workerRepository.updateFcmToken(id, body.get("token"));
        return ResponseEntity.ok().build();
    }
}
