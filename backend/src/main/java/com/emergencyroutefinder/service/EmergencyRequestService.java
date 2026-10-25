package com.emergencyroutefinder.service;

import com.emergencyroutefinder.dto.EmergencyRequestDto;
import com.emergencyroutefinder.exception.BusinessException;
import com.emergencyroutefinder.exception.ResourceNotFoundException;
import com.emergencyroutefinder.model.*;
import com.emergencyroutefinder.repository.*;
import com.emergencyroutefinder.route.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core emergency request service.
 *
 * Demonstrates: Service layer, Transactions, Async processing (multithreading),
 *               Business rule enforcement, OOP
 */
@Service
public class EmergencyRequestService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRequestService.class);

    private final EmergencyRequestRepository requestRepository;
    private final ResponseHistoryRepository historyRepository;
    private final VehicleAllocationService allocationService;
    private final RouteService routeService;
    private final WebSocketNotificationService wsService;

    public EmergencyRequestService(EmergencyRequestRepository requestRepository,
                                   ResponseHistoryRepository historyRepository,
                                   @Lazy VehicleAllocationService allocationService,
                                   @Lazy RouteService routeService,
                                   WebSocketNotificationService wsService) {
        this.requestRepository  = requestRepository;
        this.historyRepository  = historyRepository;
        this.allocationService  = allocationService;
        this.routeService       = routeService;
        this.wsService          = wsService;
    }

    // ── Create Request ────────────────────────────────────────────────────────

    @Transactional
    public EmergencyRequest createRequest(EmergencyRequestDto dto) {
        EmergencyRequest req = new EmergencyRequest();
        req.setUserId(dto.getUserId());
        req.setEmergencyType(EmergencyRequest.EmergencyType.valueOf(dto.getEmergencyType()));
        req.setDescription(dto.getDescription());
        req.setPriority(EmergencyRequest.Priority.valueOf(dto.getPriority()));
        req.setSourceLat(dto.getSourceLat());
        req.setSourceLng(dto.getSourceLng());
        req.setDestLat(dto.getDestLat());
        req.setDestLng(dto.getDestLng());
        req.setPreferredVehicle(dto.getPreferredVehicle() != null
                ? EmergencyRequest.VehiclePreference.valueOf(dto.getPreferredVehicle())
                : EmergencyRequest.VehiclePreference.ANY);
        req.setStatus(EmergencyRequest.Status.PENDING);
        req = requestRepository.save(req);

        // Record status history
        historyRepository.save(new ResponseStatusHistory(
                req.getId(), null, "PENDING",
                ResponseStatusHistory.ChangedByType.SYSTEM, null));

        // Trigger asynchronous worker search (runs on background thread pool)
        final Long requestId = req.getId();
        final double srcLat   = req.getSourceLat();
        final double srcLng   = req.getSourceLng();
        final Double dstLat   = req.getDestLat();
        final Double dstLng   = req.getDestLng();
        findAndAlertWorkerAsync(requestId);

        // Trigger async route computation if we have a destination
        if (dstLat != null && dstLng != null) {
            routeService.computeRoutesAsync(requestId, srcLat, srcLng, dstLat, dstLng);
        }

        return req;
    }

    // ── Async Worker Search (Multithreading) ──────────────────────────────────

    @Async("taskExecutor")
    public void findAndAlertWorkerAsync(Long requestId) {
        log.info("[Thread: {}] Starting worker search for request {}", Thread.currentThread().getName(), requestId);
        try {
            updateStatus(requestId, "SEARCHING", ResponseStatusHistory.ChangedByType.SYSTEM, null);
            boolean found = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                found = allocationService.findAndAlertNearestWorker(requestId);
                if (found) {
                    log.info("Worker allocated for request {} on attempt {}", requestId, attempt);
                    break;
                }
                if (attempt < 3) {
                    Thread.sleep(2500);
                }
            }
            if (!found) {
                log.warn("No available worker found for request {} after retries", requestId);
                wsService.notifyUser(requestId, "SEARCHING",
                        "No emergency workers currently available nearby. Searching...");
            }
        } catch (Exception e) {
            log.error("Error during worker search for request {}: {}", requestId, e.getMessage());
        }
    }

    // ── Status Update ─────────────────────────────────────────────────────────

    @Transactional
    public EmergencyRequest updateStatus(Long requestId, String newStatusStr,
                                          ResponseStatusHistory.ChangedByType changedByType,
                                          Long changedById) {
        EmergencyRequest req = getById(requestId);
        EmergencyRequest.Status oldStatus = req.getStatus();
        EmergencyRequest.Status newStatus = EmergencyRequest.Status.valueOf(newStatusStr);

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new BusinessException("Invalid status transition: " + oldStatus + " → " + newStatus);
        }

        requestRepository.updateStatus(requestId, newStatusStr);
        req.setStatus(newStatus);

        // Record audit trail
        historyRepository.save(new ResponseStatusHistory(
                requestId, oldStatus.name(), newStatusStr, changedByType, changedById));

        // Notify via WebSocket
        wsService.notifyRequestStatusChange(requestId, newStatusStr);

        return req;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public EmergencyRequest getById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found: " + id));
    }

    public List<EmergencyRequest> getByUserId(Long userId) {
        return requestRepository.findByUserId(userId);
    }

    public List<EmergencyRequest> getByWorkerId(Long workerId) {
        return requestRepository.findByWorkerId(workerId);
    }

    public List<ResponseStatusHistory> getStatusHistory(Long requestId) {
        return historyRepository.findByRequestId(requestId);
    }

    // ── Hospital selection ────────────────────────────────────────────────────

    @Transactional
    public void selectHospital(Long requestId, Long hospitalId) {
        requestRepository.setSelectedHospital(requestId, hospitalId);
    }
}
