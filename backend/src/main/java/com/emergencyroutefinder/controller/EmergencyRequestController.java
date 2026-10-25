package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.dto.EmergencyRequestDto;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.model.ResponseStatusHistory;
import com.emergencyroutefinder.service.EmergencyRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Emergency Request Controller — CRUD and status management.
 */
@RestController
@RequestMapping("/api/emergency")
public class EmergencyRequestController {

    private final EmergencyRequestService requestService;

    public EmergencyRequestController(EmergencyRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/request")
    public ResponseEntity<EmergencyRequest> createRequest(@RequestBody EmergencyRequestDto dto) {
        EmergencyRequest req = requestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyRequest> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EmergencyRequest>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(requestService.getByUserId(userId));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<EmergencyRequest>> getByWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(requestService.getByWorkerId(workerId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ResponseStatusHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getStatusHistory(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<EmergencyRequest> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newStatus    = body.get("status");
        String changedByStr = body.getOrDefault("changedByType", "SYSTEM");
        Long changedById    = body.containsKey("changedById") ? Long.parseLong(body.get("changedById")) : null;

        ResponseStatusHistory.ChangedByType changedBy =
                ResponseStatusHistory.ChangedByType.valueOf(changedByStr);

        EmergencyRequest updated = requestService.updateStatus(id, newStatus, changedBy, changedById);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/hospital")
    public ResponseEntity<Void> selectHospital(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        requestService.selectHospital(id, body.get("hospitalId"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<EmergencyRequest> cancelRequest(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body) {
        Long cancelledById = body.containsKey("userId") ? Long.parseLong(body.get("userId")) : null;
        EmergencyRequest updated = requestService.updateStatus(id, "CANCELLED",
                ResponseStatusHistory.ChangedByType.USER, cancelledById);
        return ResponseEntity.ok(updated);
    }
}
