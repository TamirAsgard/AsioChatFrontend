package com.example.asiochatfrontend.core.model.enums;

import com.google.gson.annotations.SerializedName;

public enum MediaType {
    @SerializedName("Image")
    IMAGE,
    @SerializedName("Video")
    VIDEO,
    @SerializedName("Voice")
    AUDIO,
    @SerializedName("Document")
    DOCUMENT
}