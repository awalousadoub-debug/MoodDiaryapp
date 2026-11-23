package com.mooddiary.data.remote;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SentimentApi {
    @POST("hf-inference/models/{modelId}")
    <HfRequest>
    Call<List<HfPrediction>> analyze(
            @Path(value = "modelId", encoded = true) String modelId,
            @Header("Authorization") String authorization,
            @Body HfRequest request
    );
}
