package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/** Audit trail entry for every request status change. */
public class ResponseStatusHistory {

    public enum ChangedByType { USER, WORKER, SYSTEM }

    private Long id;
    private Long requestId;
    private String oldStatus;
    private String newStatus;
    private ChangedByType changedByType;
    private Long changedById;
    private String note;
    private LocalDateTime changedAt;

    public ResponseStatusHistory() {}

    public ResponseStatusHistory(Long requestId, String oldStatus, String newStatus,
                                 ChangedByType changedByType, Long changedById) {
        this.requestId = requestId; this.oldStatus = oldStatus; this.newStatus = newStatus;
        this.changedByType = changedByType; this.changedById = changedById;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId()                                     { return id; }
    public void setId(Long id)                              { this.id = id; }
    public Long getRequestId()                              { return requestId; }
    public void setRequestId(Long requestId)                { this.requestId = requestId; }
    public String getOldStatus()                            { return oldStatus; }
    public void setOldStatus(String oldStatus)              { this.oldStatus = oldStatus; }
    public String getNewStatus()                            { return newStatus; }
    public void setNewStatus(String newStatus)              { this.newStatus = newStatus; }
    public ChangedByType getChangedByType()                 { return changedByType; }
    public void setChangedByType(ChangedByType changedByType){ this.changedByType = changedByType; }
    public Long getChangedById()                            { return changedById; }
    public void setChangedById(Long changedById)            { this.changedById = changedById; }
    public String getNote()                                 { return note; }
    public void setNote(String note)                        { this.note = note; }
    public LocalDateTime getChangedAt()                     { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt)       { this.changedAt = changedAt; }
}
