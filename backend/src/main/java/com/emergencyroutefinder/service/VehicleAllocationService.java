package com.emergencyroutefinder.service;

import com.emergencyroutefinder.exception.BusinessException;
import com.emergencyroutefinder.exception.ResourceNotFoundException;
import com.emergencyroutefinder.model.*;
import com.emergencyroutefinder.repository.*;
import com.emergencyroutefinder.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Vehicle Allocation Service — nearest-worker detection + assignment.
 *
 * CORE FEATURE: Uses Haversine formula to find closest available worker.
 * Uses database transactions to prevent duplicate assignments.
 *
 * Demonstrates: Haversine, Sorting, Transactions, Exception Handling, Multithreading (called async)
 */
@Service
public class VehicleAllocationService {

    private static final Logger log = LoggerFactory.getLogger(VehicleAllocationService.class);

    private final EmergencyRequestRepository requestRepository;
    private final EmergencyWorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final ResponseHistoryRepository historyRepository;
    private final WebSocketNotificationService wsService;

    public VehicleAllocationService(EmergencyRequestRepository requestRepository,
                                     EmergencyWorkerRepository workerRepository,
                                     VehicleRepository vehicleRepository,
                                     AssignmentRepository assignmentRepository,
                                     NotificationService notificationService,
                                     ResponseHistoryRepository historyRepository,
                                     WebSocketNotificationService wsService) {
        this.requestRepository  = requestRepository;
        this.workerRepository   = workerRepository;
        this.vehicleRepository  = vehicleRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.historyRepository  = historyRepository;
        this.wsService          = wsService;
    }

    /**
     * Find the nearest available worker and send them an emergency alert.
     *
     * @param requestId the emergency request to respond to
     * @return true if a worker was found and alerted
     */
    public boolean findAndAlertNearestWorker(Long requestId) {
        EmergencyRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        String requiredWorkerType = resolveWorkerType(req.getPreferredVehicle(),
                req.getEmergencyType());

        // Get all available workers (filtered by type)
        List<EmergencyWorker> candidates = workerRepository.findAvailableWorkers(requiredWorkerType);

        if (candidates.isEmpty()) {
            log.warn("No available workers for request {}", requestId);
            return false;
        }

        // Sort by Haversine distance from emergency location
        candidates.sort(Comparator.comparingDouble(w ->
                HaversineUtil.distanceKm(req.getSourceLat(), req.getSourceLng(),
                        w.getLocationLat(), w.getLocationLng())));

        // Try to assign workers in distance order (skip workers already assigned elsewhere)
        for (EmergencyWorker worker : candidates) {
            if (tryAssign(req, worker)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Atomic assignment — uses transaction to prevent duplicate assignments.
     */
    @Transactional
    public synchronized boolean tryAssign(EmergencyRequest req, EmergencyWorker worker) {
        // Re-check worker availability inside transaction (prevent race condition)
        Optional<EmergencyWorker> freshWorker = workerRepository.findById(worker.getId());
        if (freshWorker.isEmpty() || !freshWorker.get().isAvailable()) {
            return false;
        }

        // Re-check request is still searchable
        Optional<EmergencyRequest> freshReq = requestRepository.findById(req.getId());
        if (freshReq.isEmpty()) return false;
        EmergencyRequest.Status status = freshReq.get().getStatus();
        if (status != EmergencyRequest.Status.PENDING && status != EmergencyRequest.Status.SEARCHING) {
            return false;
        }

        // Check worker not already assigned elsewhere
        if (assignmentRepository.hasActiveAssignment(worker.getId())) {
            return false;
        }

        // Get worker's vehicle (or auto-provision if missing)
        Optional<EmergencyVehicle> vehicleOpt = vehicleRepository.findByWorkerId(worker.getId());
        if (vehicleOpt.isEmpty()) {
            boolean isFire = worker.getWorkerType() == EmergencyWorker.WorkerType.FIRE_ENGINE_DRIVER;
            String vNum = (isFire ? "FIRE-" : "AMB-") + "KA-" + String.format("%04d", worker.getId());
            EmergencyVehicle newV = isFire ? new FireEngine(vNum, worker.getId()) : new Ambulance(vNum, worker.getId());
            newV.setAvailability(EmergencyVehicle.VehicleStatus.AVAILABLE);
            vehicleRepository.save(newV);
            vehicleOpt = Optional.of(newV);
        }
        EmergencyVehicle vehicle = vehicleOpt.get();
        if (!vehicle.isAvailable()) {
            if (!assignmentRepository.hasActiveAssignment(worker.getId())) {
                vehicleRepository.updateAvailability(vehicle.getId(), "AVAILABLE", null);
                vehicle.setAvailability(EmergencyVehicle.VehicleStatus.AVAILABLE);
            } else {
                return false;
            }
        }

        // Create assignment record
        VehicleAssignment assignment = new VehicleAssignment(req.getId(), worker.getId(), vehicle.getId());
        assignmentRepository.save(assignment);

        // Update request
        requestRepository.assignWorker(req.getId(), worker.getId(), vehicle.getId());

        // Mark worker and vehicle BUSY/ASSIGNED
        workerRepository.updateAvailability(worker.getId(), "BUSY");
        vehicleRepository.updateAvailability(vehicle.getId(), "ASSIGNED", req.getId());

        // Record history
        historyRepository.save(new ResponseStatusHistory(
                req.getId(), "SEARCHING", "ASSIGNED",
                ResponseStatusHistory.ChangedByType.SYSTEM, null));

        double distKm = HaversineUtil.distanceKm(req.getSourceLat(), req.getSourceLng(),
                worker.getLocationLat(), worker.getLocationLng());

        // Send FCM notification to worker
        notificationService.sendWorkerEmergencyAlert(worker, req, vehicle, distKm);

        // Notify user via WebSocket that a worker was found
        wsService.notifyRequestStatusChange(req.getId(), "ASSIGNED");

        log.info("Worker {} assigned to request {} (distance: {:.2f} km)", worker.getId(), req.getId(), distKm);
        return true;
    }

    // ── Accept / Decline ──────────────────────────────────────────────────────

    @Transactional
    public void acceptAssignment(Long assignmentId, Long workerId) {
        VehicleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (!assignment.getWorkerId().equals(workerId)) {
            throw new BusinessException("Assignment does not belong to this worker.");
        }
        if (assignment.getStatus() != VehicleAssignment.AssignmentStatus.PENDING) {
            throw new BusinessException("Assignment is no longer pending (status: " + assignment.getStatus() + ")");
        }

        assignmentRepository.accept(assignmentId);
        requestRepository.markAccepted(assignment.getRequestId());

        historyRepository.save(new ResponseStatusHistory(
                assignment.getRequestId(), "ASSIGNED", "ACCEPTED",
                ResponseStatusHistory.ChangedByType.WORKER, workerId));

        wsService.notifyRequestStatusChange(assignment.getRequestId(), "ACCEPTED");
        log.info("Worker {} accepted assignment {} for request {}", workerId, assignmentId, assignment.getRequestId());
    }

    @Transactional
    public void declineAssignment(Long assignmentId, Long workerId, String reason) {
        VehicleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (!assignment.getWorkerId().equals(workerId)) {
            throw new BusinessException("Assignment does not belong to this worker.");
        }

        assignmentRepository.decline(assignmentId, reason);

        // Free worker and vehicle
        workerRepository.updateAvailability(workerId, "AVAILABLE");
        vehicleRepository.updateAvailability(assignment.getVehicleId(), "AVAILABLE", null);

        // Reset request to SEARCHING and find next worker
        requestRepository.updateStatus(assignment.getRequestId(), "SEARCHING");

        historyRepository.save(new ResponseStatusHistory(
                assignment.getRequestId(), "ASSIGNED", "SEARCHING",
                ResponseStatusHistory.ChangedByType.WORKER, workerId));

        // Async retry
        findAndAlertNearestWorker(assignment.getRequestId());

        log.info("Worker {} declined assignment {} — retrying worker search", workerId, assignmentId);
    }

    @Transactional
    public void completeResponse(Long assignmentId, Long workerId) {
        VehicleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        assignmentRepository.complete(assignmentId);
        requestRepository.markCompleted(assignment.getRequestId());
        workerRepository.updateAvailability(workerId, "AVAILABLE");
        vehicleRepository.updateAvailability(assignment.getVehicleId(), "AVAILABLE", null);

        historyRepository.save(new ResponseStatusHistory(
                assignment.getRequestId(), "ARRIVED", "COMPLETED",
                ResponseStatusHistory.ChangedByType.WORKER, workerId));

        wsService.notifyRequestStatusChange(assignment.getRequestId(), "COMPLETED");
        log.info("Response completed for assignment {}", assignmentId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String resolveWorkerType(EmergencyRequest.VehiclePreference pref,
                                     EmergencyRequest.EmergencyType type) {
        if (pref == EmergencyRequest.VehiclePreference.AMBULANCE) return "AMBULANCE_DRIVER";
        if (pref == EmergencyRequest.VehiclePreference.FIRE_ENGINE) return "FIRE_ENGINE_DRIVER";
        // Auto-select by emergency type
        if (type == EmergencyRequest.EmergencyType.MEDICAL ||
            type == EmergencyRequest.EmergencyType.ACCIDENT) return "AMBULANCE_DRIVER";
        if (type == EmergencyRequest.EmergencyType.FIRE) return "FIRE_ENGINE_DRIVER";
        return null; // ANY — return all types
    }
}
