package com.example.asiochatfrontend.core.model.enums;

import com.google.gson.annotations.SerializedName;

public enum MessageState {
    @SerializedName("SENT")
    SENT,

    @SerializedName("READ")
    READ,

    @SerializedName("UNKNOWN")
    UNKNOWN,

    @SerializedName("PENDING")
    PENDING
}