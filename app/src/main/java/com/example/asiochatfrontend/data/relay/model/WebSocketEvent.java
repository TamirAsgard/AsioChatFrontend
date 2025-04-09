package com.example.asiochatfrontend.data.relay.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class WebSocketEvent {

    @SerializedName("type")
    private EventType type;

    @SerializedName("payload")
    private JsonElement payload;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("eventId")
    private String eventId;

    @SerializedName("senderId")
    private String senderId;

    public WebSocketEvent(EventType type, JsonElement payload, String eventId, String senderId) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.eventId = eventId;
        this.senderId = senderId;
    }

    public EventType getType() {
        return type;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSenderId() {
        return senderId;
    }

    public enum EventType {
        @SerializedName("MESSAGE")
        MESSAGE,

        @SerializedName("USER_PRESENCE")
        USER_PRESENCE,

        @SerializedName("MEDIA_UPLOAD")
        MEDIA_UPLOAD,

        @SerializedName("MEDIA_DOWNLOAD")
        MEDIA_DOWNLOAD,

        @SerializedName("CHAT_UPDATE")
        CHAT_UPDATE,

        @SerializedName("GROUP_UPDATE")
        GROUP_UPDATE,

        @SerializedName("DELIVERY_RECEIPT")
        DELIVERY_RECEIPT,

        @SerializedName("READ_RECEIPT")
        READ_RECEIPT,

        @SerializedName("TYPING_INDICATOR")
        TYPING_INDICATOR,

        @SerializedName("ERROR")
        ERROR,

        @SerializedName("CONNECT")
        CONNECT,

        @SerializedName("DISCONNECT")
        DISCONNECT
    }
}
