package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/**
 * Tracks assignment of a vehicle/worker to an emergency request.
 * One request may have multiple assignment attempts (if worker declines).
 */
public class VehicleAssignment {

    public enum AssignmentStatus { PENDING, ACCEPTED, DECLINED, COMPLETED, CANCELLED }

    private Long id;
    private Long requestId;
    private Long workerId;
    private Long vehicleId;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime declinedAt;
    private LocalDateTime completedAt;
    private String declineReason;

    public VehicleAssignment() {}

    public VehicleAssignment(Long requestId, Long workerId, Long vehicleId) {
        this.requestId  = requestId;
        this.workerId   = workerId;
        this.vehicleId  = vehicleId;
        this.status     = AssignmentStatus.PENDING;
    }

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }
    public Long getRequestId()                          { return requestId; }
    public void setRequestId(Long requestId)            { this.requestId = requestId; }
    public Long getWorkerId()                           { return workerId; }
    public void setWorkerId(Long workerId)              { this.workerId = workerId; }
    public Long getVehicleId()                          { return vehicleId; }
    public void setVehicleId(Long vehicleId)            { this.vehicleId = vehicleId; }
    public AssignmentStatus getStatus()                 { return status; }
    public void setStatus(AssignmentStatus status)      { this.status = status; }
    public LocalDateTime getAssignedAt()                { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getAcceptedAt()                { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getDeclinedAt()                { return declinedAt; }
    public void setDeclinedAt(LocalDateTime declinedAt) { this.declinedAt = declinedAt; }
    public LocalDateTime getCompletedAt()               { return completedAt; }
    public void setCompletedAt(LocalDateTime t)         { this.completedAt = t; }
    public String getDeclineReason()                    { return declineReason; }
    public void setDeclineReason(String reason)         { this.declineReason = reason; }
}
