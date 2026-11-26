package com.mooddiary.data.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mooddiary.data.remote.HfPrediction;
import com.mooddiary.data.remote.HfTokenProvider;
import com.mooddiary.data.remote.SentimentApi;
import com.mooddiary.data.remote.SentimentRequest;
import com.mooddiary.domain.sentiment.SentimentCategory;
import com.mooddiary.domain.sentiment.SentimentResult;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class SentimentRepository {
    public enum FailureKind {
        INVALID_INPUT,
        TOKEN_MISSING,
        AUTHENTICATION,
        RATE_LIMITED,
        MODEL_UNAVAILABLE,
        NETWORK,
        HTTP_ERROR,
        INVALID_RESPONSE
    }

    public static final class Failure {
        private final FailureKind kind;
        private final int httpStatus;
        private final String message;

        public Failure(FailureKind kind, int httpStatus, String message) {
            this.kind = kind;
            this.httpStatus = httpStatus;
            this.message = message;
        }

        public FailureKind getKind() {
            return kind;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public String getMessage() {
            return message;
        }
    }

    public interface Callback {
        void onSuccess(SentimentResult result);
        void onError(Failure failure);
    }

    private final SentimentApi api;
    private final HfTokenProvider tokenProvider;
    private final String modelId;

    public SentimentRepository(SentimentApi api, HfTokenProvider tokenProvider, String modelId) {
        if (api == null || tokenProvider == null || modelId == null || modelId.trim().isEmpty()) {
            throw new IllegalArgumentException("La configuration de l'analyse est incomplète.");
        }
        this.api = api;
        this.tokenProvider = tokenProvider;
        this.modelId = modelId;
    }

    /** Runs the HTTP request asynchronously; callers should not block the Android main thread. */
    public void analyze(String text, Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Un callback est obligatoire.");
        }
        if (text == null || text.trim().isEmpty()) {
            callback.onError(new Failure(
                    FailureKind.INVALID_INPUT, 0, "Le texte à analyser est vide."
            ));
            return;
        }

        String token = tokenProvider.getToken();
        if (token == null || token.trim().isEmpty()) {
            callback.onError(new Failure(
                    FailureKind.TOKEN_MISSING, 0,
                    "Le token Hugging Face de développement n'est pas configuré."
            ));
            return;
        }

        api.analyze(
                modelId,
                "Bearer " + token.trim(),
                new SentimentRequest(text.trim())
        ).enqueue(new retrofit2.Callback<List<HfPrediction>>() {
            @Override
            public void onResponse(
                    retrofit2.Call<List<HfPrediction>> call,
                    Response<List<HfPrediction>> response
            ) {
                if (!response.isSuccessful()) {
                    String detail = extractErrorDetail(response);
                    callback.onError(httpFailure(response.code(), detail));
                    return;
                }

                SentimentResult result = parsePredictions(response.body());
                if (result == null) {
                    callback.onError(new Failure(
                            FailureKind.INVALID_RESPONSE, response.code(),
                            "La réponse du modèle est vide ou invalide."
                    ));
                    return;
                }
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(retrofit2.Call<List<HfPrediction>> call, Throwable error) {
                FailureKind kind = error instanceof IOException
                        ? FailureKind.NETWORK : FailureKind.INVALID_RESPONSE;
                String message = kind == FailureKind.NETWORK
                        ? "Connexion à Hugging Face impossible."
                        : "La réponse de Hugging Face n'a pas pu être lue.";
                if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                    message += " (" + error.getMessage() + ")";
                }
                callback.onError(new Failure(kind, 0, message));
            }
        });
    }

    private static String extractErrorDetail(Response<?> response) {
        if (response == null) {
            return null;
        }
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return null;
        }
        try {
            String rawBody = errorBody.string();
            if (!rawBody.trim().isEmpty()) {
                JsonElement element = JsonParser.parseString(rawBody);
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    if (json.has("error")) {
                        JsonElement err = json.get("error");
                        if (err.isJsonPrimitive()) {
                            return err.getAsString();
                        } else if (err.isJsonArray() && !err.getAsJsonArray().isEmpty()) {
                            return err.getAsJsonArray().get(0).getAsString();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static SentimentResult parsePredictions(List<HfPrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return null;
        }

        HfPrediction best = null;
        for (HfPrediction prediction : predictions) {
            if (prediction == null || prediction.label == null || prediction.score == null) {
                continue;
            }
            float score = prediction.score;
            if (Float.isNaN(score) || Float.isInfinite(score) || score < 0.0f || score > 1.0f) {
                continue;
            }
            if (best == null || score > best.score) {
                best = prediction;
            }
        }

        if (best == null) {
            return null;
        }
        try {
            return new SentimentResult(
                    SentimentCategory.fromModelLabel(best.label),
                    best.score
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Failure httpFailure(int statusCode, String errorDetail) {
        String suffix = (errorDetail != null && !errorDetail.trim().isEmpty())
                ? " : " + errorDetail.trim()
                : ".";
        switch (statusCode) {
            case 401:
            case 403:
                return new Failure(
                        FailureKind.AUTHENTICATION, statusCode,
                        "Hugging Face a refusé l'authentification" + suffix
                );
            case 402:
                return new Failure(
                        FailureKind.RATE_LIMITED, statusCode,
                        "Crédits Hugging Face insuffisants ou abonnement requis (HTTP 402)" + suffix
                );
            case 429:
                return new Failure(
                        FailureKind.RATE_LIMITED, statusCode,
                        "Limite de requêtes Hugging Face atteinte. Réessaie plus tard" + suffix
                );
            case 503:
                return new Failure(
                        FailureKind.MODEL_UNAVAILABLE, statusCode,
                        "Le modèle est en cours de démarrage ou temporairement indisponible" + suffix
                );
            default:
                return new Failure(
                        FailureKind.HTTP_ERROR, statusCode,
                        "La requête Hugging Face a échoué (HTTP " + statusCode + ")" + suffix
                );
        }
    }
}
