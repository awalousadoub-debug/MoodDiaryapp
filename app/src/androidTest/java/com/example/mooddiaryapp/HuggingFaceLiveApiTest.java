package com.example.mooddiaryapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mooddiary.data.remote.HuggingFaceClient;
import com.mooddiary.data.repository.SentimentRepository;
import com.mooddiary.domain.sentiment.SentimentResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class HuggingFaceLiveApiTest {

    @Test
    public void sendsFrenchTextToHuggingFaceAndReceivesPrediction() throws Exception {
        assumeTrue(
                "Configure HF_TOKEN dans secrets.properties pour exécuter le test live.",
                BuildConfig.HF_TOKEN != null && !BuildConfig.HF_TOKEN.trim().isEmpty()
        );

        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<SentimentResult> result = new AtomicReference<>();
        AtomicReference<SentimentRepository.Failure> failure = new AtomicReference<>();

        HuggingFaceClient.createSentimentRepository().analyze(
                "Je suis très heureux, ma journée se passe bien.",
                new SentimentRepository.Callback() {
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
                }
        );

        assertTrue(
                "Hugging Face n'a pas répondu en 90 secondes.",
                completed.await(90, TimeUnit.SECONDS)
        );

        assertTrue(
                failure.get() == null
                        ? ""
                        : failure.get().getMessage(),
                failure.get() == null
        );

        assertNotNull("La réponse ne contient aucune prédiction.", result.get());
        assertTrue(result.get().getConfidence() >= 0.0f);
        assertTrue(result.get().getConfidence() <= 1.0f);
    }
}