package com.mooddiary.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class HfPredictionListDeserializer implements JsonDeserializer<List<HfPrediction>> {
    @Override
    public List<HfPrediction> deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
    ) throws JsonParseException {
        List<HfPrediction> list = new ArrayList<>();
        if (json == null || json.isJsonNull()) {
            return list;
        }

        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            for (JsonElement element : array) {
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                if (element.isJsonArray()) {
                    // 2D Array: [[{"label":"positive","score":0.85}, ...]]
                    for (JsonElement inner : element.getAsJsonArray()) {
                        if (inner != null && inner.isJsonObject()) {
                            HfPrediction pred = context.deserialize(inner, HfPrediction.class);
                            if (pred != null) {
                                list.add(pred);
                            }
                        }
                    }
                } else if (element.isJsonObject()) {
                    // 1D Array: [{"label":"positive","score":0.85}, ...]
                    HfPrediction pred = context.deserialize(element, HfPrediction.class);
                    if (pred != null) {
                        list.add(pred);
                    }
                }
            }
        } else if (json.isJsonObject()) {
            // Single Object fallback
            HfPrediction pred = context.deserialize(json, HfPrediction.class);
            if (pred != null) {
                list.add(pred);
            }
        }
        return list;
    }
}
