package com.mooddiary.domain.sentiment;

import java.util.Locale;

public enum SentimentCategory {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    public static SentimentCategory fromModelLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Le modèle n'a pas retourné de label.");
        }

        switch (label.trim().toLowerCase(Locale.ROOT)) {
            case "positive":
            case "positif":
                return POSITIVE;
            case "neutral":
            case "neutre":
                return NEUTRAL;
            case "negative":
            case "négatif":
            case "negatif":
                return NEGATIVE;
            default:
                throw new IllegalArgumentException("Label de sentiment non reconnu.");
        }
    }
}
