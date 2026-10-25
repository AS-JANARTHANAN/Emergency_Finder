package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("id")               private long id;
    @SerializedName("title")            private String title;
    @SerializedName("body")             private String body;
    @SerializedName("notificationType") private String notificationType;
    @SerializedName("read")             private boolean read;
    @SerializedName("sentAt")           private String sentAt;

    public long getId()                  { return id; }
    public String getTitle()             { return title; }
    public String getBody()              { return body; }
    public String getNotificationType()  { return notificationType; }
    public boolean isRead()              { return read; }
    public String getSentAt()            { return sentAt; }
}
