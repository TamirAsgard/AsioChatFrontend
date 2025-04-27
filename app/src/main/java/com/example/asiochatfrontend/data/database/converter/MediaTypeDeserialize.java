package com.example.asiochatfrontend.data.database.converter;

import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

public class MediaTypeDeserialize implements JsonDeserializer<MediaType> {
    @Override
    public MediaType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            if (json.isJsonPrimitive()) {
                JsonPrimitive primitive = json.getAsJsonPrimitive();

                if (primitive.isString()) {
                    return MediaType.valueOf(primitive.getAsString().toUpperCase());
                }

                // If the backend sends 0, 1, 2...
                if (primitive.isNumber()) {
                    int index = primitive.getAsInt();
                    MediaType[] values = MediaType.values();
                    return (index >= 0 && index < values.length) ? values[index] : MediaType.DOCUMENT;
                }
            }
        } catch (Exception e) {
            // You can log the issue here if you want
        }

        return MediaType.DOCUMENT;
    }
}
