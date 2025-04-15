package com.example.asiochatfrontend.data.database.converter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

public class MessageStateDeserializer implements JsonDeserializer<MessageState> {
    @Override
    public MessageState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            if (json.isJsonPrimitive()) {
                JsonPrimitive primitive = json.getAsJsonPrimitive();

                // If the backend sends "READ", "SENT", etc.
                if (primitive.isString()) {
                    return MessageState.valueOf(primitive.getAsString().toUpperCase());
                }

                // If the backend sends 0, 1, 2...
                if (primitive.isNumber()) {
                    int index = primitive.getAsInt();
                    MessageState[] values = MessageState.values();
                    return (index >= 0 && index < values.length) ? values[index] : MessageState.UNKNOWN;
                }
            }
        } catch (Exception e) {
            // You can log the issue here if you want
        }

        return MessageState.UNKNOWN;
    }
}