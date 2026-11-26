package com.mooddiary.domain.sentiment;

public final class SentimentResult {
    private final SentimentCategory category;
    private final float confidence;

    public SentimentResult(SentimentCategory category, float confidence) {
        if (category == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire.");
        }
        if (Float.isNaN(confidence) || Float.isInfinite(confidence)
                || confidence < 0.0f || confidence > 1.0f) {
            throw new IllegalArgumentException("Le score doit être compris entre 0 et 1.");
        }
        this.category = category;
        this.confidence = confidence;
    }

    public SentimentCategory getCategory() {
        return category;
    }

    public float getConfidence() {
        return confidence;
    }
}
