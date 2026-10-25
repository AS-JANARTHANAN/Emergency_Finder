package com.emergencyroutefinder.model;
import com.google.gson.annotations.SerializedName;

public class StatusHistory {
    @SerializedName("id")             private long id;
    @SerializedName("requestId")      private long requestId;
    @SerializedName("oldStatus")      private String oldStatus;
    @SerializedName("newStatus")      private String newStatus;
    @SerializedName("changedByType")  private String changedByType;
    @SerializedName("note")           private String note;
    @SerializedName("changedAt")      private String changedAt;

    public long getId()              { return id; }
    public long getRequestId()       { return requestId; }
    public String getOldStatus()     { return oldStatus; }
    public String getNewStatus()     { return newStatus; }
    public String getChangedByType() { return changedByType; }
    public String getNote()          { return note; }
    public String getChangedAt()     { return changedAt; }
}
