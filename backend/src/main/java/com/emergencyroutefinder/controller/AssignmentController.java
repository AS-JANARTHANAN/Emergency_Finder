package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.service.VehicleAllocationService;
import com.emergencyroutefinder.service.EmergencyRequestService;
import com.emergencyroutefinder.repository.AssignmentRepository;
import com.emergencyroutefinder.model.ResponseStatusHistory;
import com.emergencyroutefinder.model.VehicleAssignment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Assignment Controller — worker accept/decline/arrive/complete actions.
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final VehicleAllocationService allocationService;
    private final EmergencyRequestService requestService;
    private final AssignmentRepository assignmentRepository;

    public AssignmentController(VehicleAllocationService allocationService,
                                  EmergencyRequestService requestService,
                                  AssignmentRepository assignmentRepository) {
        this.allocationService = allocationService;
        this.requestService = requestService;
        this.assignmentRepository = assignmentRepository;
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<Map<String, Object>> accept(
            @PathVariable Long requestId,
            @RequestBody Map<String, Long> body) {
        Long workerId = body.get("workerId");
        VehicleAssignment assignment = assignmentRepository.findActiveByRequestId(requestId)
                .orElseThrow(() -> new com.emergencyroutefinder.exception.BusinessException("No active assignment for request " + requestId));
        allocationService.acceptAssignment(assignment.getId(), workerId);
        return ResponseEntity.ok(Map.of("status", "ACCEPTED", "assignmentId", assignment.getId()));
    }

    @PostMapping("/{requestId}/decline")
    public ResponseEntity<Map<String, Object>> decline(
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> body) {
        Long workerId = Long.parseLong(body.get("workerId").toString());
        String reason = (String) body.getOrDefault("reason", "Worker declined");
        VehicleAssignment assignment = assignmentRepository.findActiveByRequestId(requestId)
                .orElseThrow(() -> new com.emergencyroutefinder.exception.BusinessException("No active assignment for request " + requestId));
        allocationService.declineAssignment(assignment.getId(), workerId, reason);
        return ResponseEntity.ok(Map.of("status", "DECLINED", "assignmentId", assignment.getId()));
    }

    @PostMapping("/{requestId}/arrived")
    public ResponseEntity<Map<String, Object>> arrived(
            @PathVariable Long requestId,
            @RequestBody Map<String, Long> body) {
        Long workerId  = body.get("workerId");
        requestService.updateStatus(requestId, "ARRIVED",
                ResponseStatusHistory.ChangedByType.WORKER, workerId);
        return ResponseEntity.ok(Map.of("status", "ARRIVED", "requestId", requestId));
    }

    @PostMapping("/{requestId}/complete")
    public ResponseEntity<Map<String, Object>> complete(
            @PathVariable Long requestId,
            @RequestBody Map<String, Long> body) {
        Long workerId = body.get("workerId");
        VehicleAssignment assignment = assignmentRepository.findActiveByRequestId(requestId)
                .orElseThrow(() -> new com.emergencyroutefinder.exception.BusinessException("No active assignment for request " + requestId));
        allocationService.completeResponse(assignment.getId(), workerId);
        return ResponseEntity.ok(Map.of("status", "COMPLETED", "assignmentId", assignment.getId()));
    }

    @PostMapping("/{requestId}/responding")
    public ResponseEntity<Map<String, Object>> responding(
            @PathVariable Long requestId,
            @RequestBody Map<String, Long> body) {
        Long workerId  = body.get("workerId");
        requestService.updateStatus(requestId, "RESPONDING",
                ResponseStatusHistory.ChangedByType.WORKER, workerId);
        return ResponseEntity.ok(Map.of("status", "RESPONDING", "requestId", requestId));
    }

    @GetMapping("/active/{workerId}")
    public ResponseEntity<?> getActiveAssignment(@PathVariable Long workerId) {
        return ResponseEntity.ok(assignmentRepository.findByWorkerId(workerId));
    }
}
