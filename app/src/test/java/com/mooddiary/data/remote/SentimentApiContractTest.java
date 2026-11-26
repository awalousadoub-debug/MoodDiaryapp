package com.mooddiary.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mooddiary.data.repository.SentimentRepository;
import com.mooddiary.domain.sentiment.SentimentCategory;
import com.mooddiary.domain.sentiment.SentimentResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class SentimentApiContractTest {
    private MockWebServer server;
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        Type listType = new TypeToken<List<HfPrediction>>() {}.getType();
        gson = new GsonBuilder()
                .registerTypeAdapter(listType, new HfPredictionListDeserializer())
                .create();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void sendsExpectedHuggingFaceRequestAndMapsTopPrediction() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("["
                        + "{\"label\":\"negative\",\"score\":0.05},"
                        + "{\"label\":\"neutral\",\"score\":0.10},"
                        + "{\"label\":\"positive\",\"score\":0.85}"
                        + "]"));

        SentimentApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SentimentApi.class);
        SentimentRepository repository = new SentimentRepository(
                api,
                () -> "unit-test-token",
                "clapAI/modernBERT-base-multilingual-sentiment"
        );
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<SentimentResult> result = new AtomicReference<>();
        AtomicReference<SentimentRepository.Failure> failure = new AtomicReference<>();

        repository.analyze("Je passe une très bonne journée.", new SentimentRepository.Callback() {
            @Override
            public void onSuccess(SentimentResult value) {
                result.set(value);
                completed.countDown();
            }

            @Override
            public void onError(SentimentRepository.Failure value) {
                failure.set(value);
                completed.countDown();
            }
        });

        assertTrue("La réponse HTTP de test n'a pas été reçue.",
                completed.await(5, TimeUnit.SECONDS));
        assertEquals(null, failure.get());
        assertNotNull(result.get());
        assertEquals(SentimentCategory.POSITIVE, result.get().getCategory());
        assertEquals(0.85f, result.get().getConfidence(), 0.0001f);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(
                "/hf-inference/models/clapAI/modernBERT-base-multilingual-sentiment",
                request.getPath()
        );
        assertEquals("Bearer unit-test-token", request.getHeader("Authorization"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"inputs\":\"Je passe une très bonne journée.\""));
        assertTrue(body.contains("\"top_k\":3"));
    }

    @Test
    public void handlesNested2DArrayHuggingFaceResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[["
                        + "{\"label\":\"negative\",\"score\":0.05},"
                        + "{\"label\":\"positive\",\"score\":0.95}"
                        + "]]"));

        SentimentApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SentimentApi.class);
        SentimentRepository repository = new SentimentRepository(
                api,
                () -> "unit-test-token",
                "clapAI/modernBERT-base-multilingual-sentiment"
        );
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<SentimentResult> result = new AtomicReference<>();
        AtomicReference<SentimentRepository.Failure> failure = new AtomicReference<>();

        repository.analyze("Je suis ravi.", new SentimentRepository.Callback() {
            @Override
            public void onSuccess(SentimentResult value) {
                result.set(value);
                completed.countDown();
            }

            @Override
            public void onError(SentimentRepository.Failure value) {
                failure.set(value);
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(null, failure.get());
        assertNotNull(result.get());
        assertEquals(SentimentCategory.POSITIVE, result.get().getCategory());
        assertEquals(0.95f, result.get().getConfidence(), 0.0001f);
    }

    @Test
    public void extractsErrorMessageOnHttpError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(402)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Purchase pre-paid credits to continue using Inference Providers.\"}"));

        SentimentApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SentimentApi.class);
        SentimentRepository repository = new SentimentRepository(
                api,
                () -> "unit-test-token",
                "clapAI/modernBERT-base-multilingual-sentiment"
        );
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<SentimentRepository.Failure> failure = new AtomicReference<>();

        repository.analyze("Test", new SentimentRepository.Callback() {
            @Override
            public void onSuccess(SentimentResult value) {
                completed.countDown();
            }

            @Override
            public void onError(SentimentRepository.Failure value) {
                failure.set(value);
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertNotNull(failure.get());
        assertEquals(SentimentRepository.FailureKind.RATE_LIMITED, failure.get().getKind());
        assertEquals(402, failure.get().getHttpStatus());
        assertTrue(failure.get().getMessage().contains("Purchase pre-paid credits"));
    }

    @Test
    public void missingTokenFailsWithoutMakingNetworkRequest() throws Exception {
        SentimentApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SentimentApi.class);
        SentimentRepository repository = new SentimentRepository(
                api,
                () -> "",
                "clapAI/modernBERT-base-multilingual-sentiment"
        );
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<SentimentRepository.Failure> failure = new AtomicReference<>();

        repository.analyze("Texte de test", new SentimentRepository.Callback() {
            @Override
            public void onSuccess(SentimentResult result) {
                completed.countDown();
            }

            @Override
            public void onError(SentimentRepository.Failure value) {
                failure.set(value);
                completed.countDown();
            }
        });

        assertTrue(completed.await(1, TimeUnit.SECONDS));
        assertEquals(SentimentRepository.FailureKind.TOKEN_MISSING, failure.get().getKind());
        assertEquals(0, server.getRequestCount());
    }
}
