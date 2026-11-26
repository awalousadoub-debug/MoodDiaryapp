package com.mooddiary.data.remote;

import com.example.mooddiaryapp.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mooddiary.data.repository.SentimentRepository;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class HuggingFaceClient {
    public static final String BASE_URL = "https://router.huggingface.co/";

    private HuggingFaceClient() {
    }

    public static SentimentRepository createSentimentRepository() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();

        Type listType = new TypeToken<List<HfPrediction>>() {}.getType();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(listType, new HfPredictionListDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        SentimentApi api = retrofit.create(SentimentApi.class);
        return new SentimentRepository(
                api,
                new BuildConfigHfTokenProvider(),
                BuildConfig.HF_MODEL_ID
        );
    }
}
