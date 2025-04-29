package com.example.asiochatfrontend.data.relay.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class WebSocketEvent {

    @SerializedName("type")
    private EventType type;

    @SerializedName("payload")
    private JsonElement payload;

    @SerializedName("jid")
    private String jid;

    public WebSocketEvent(EventType type, JsonElement payload, String jid) {
        this.type = type;
        this.payload = payload;
        this.jid = jid;
    }

    public EventType getType() {
        return type;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public enum EventType {
        @SerializedName("CHAT")
        CHAT,

        @SerializedName("CONNECTION")
        CONNECTION,

        @SerializedName("incomingMessage")
        INCOMING,

        @SerializedName("MESSAGE_READ")
        MESSAGE_READ,

        @SerializedName("CREATE_CHAT")
        CREATE_CHAT,
    }

    @Override
    public String toString() {
        return "WebSocketEvent{" +
                "type=" + type +
                ", payload=" + payload +
                ", jid='" + jid + '\'' +
                '}';
    }
}
