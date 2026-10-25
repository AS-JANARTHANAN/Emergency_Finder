package com.emergencyroutefinder.model;

import java.time.LocalDateTime;

/** Push/in-app notification record. */
public class Notification {

    public enum RecipientType { USER, WORKER }

    private Long id;
    private RecipientType recipientType;
    private Long recipientId;
    private Long requestId;
    private String title;
    private String body;
    private String notificationType;
    private boolean read;
    private String fcmMessageId;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public Notification() {}

    public Notification(RecipientType recipientType, Long recipientId, Long requestId,
                        String title, String body, String notificationType) {
        this.recipientType = recipientType; this.recipientId = recipientId;
        this.requestId = requestId; this.title = title; this.body = body;
        this.notificationType = notificationType; this.read = false;
    }

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }
    public RecipientType getRecipientType()             { return recipientType; }
    public void setRecipientType(RecipientType rt)      { this.recipientType = rt; }
    public Long getRecipientId()                        { return recipientId; }
    public void setRecipientId(Long recipientId)        { this.recipientId = recipientId; }
    public Long getRequestId()                          { return requestId; }
    public void setRequestId(Long requestId)            { this.requestId = requestId; }
    public String getTitle()                            { return title; }
    public void setTitle(String title)                  { this.title = title; }
    public String getBody()                             { return body; }
    public void setBody(String body)                    { this.body = body; }
    public String getNotificationType()                 { return notificationType; }
    public void setNotificationType(String t)           { this.notificationType = t; }
    public boolean isRead()                             { return read; }
    public void setRead(boolean read)                   { this.read = read; }
    public String getFcmMessageId()                     { return fcmMessageId; }
    public void setFcmMessageId(String fcmMessageId)    { this.fcmMessageId = fcmMessageId; }
    public LocalDateTime getSentAt()                    { return sentAt; }
    public void setSentAt(LocalDateTime sentAt)         { this.sentAt = sentAt; }
    public LocalDateTime getReadAt()                    { return readAt; }
    public void setReadAt(LocalDateTime readAt)         { this.readAt = readAt; }
}
