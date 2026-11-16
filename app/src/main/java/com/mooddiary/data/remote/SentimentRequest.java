package com.mooddiary.data.remote;

import com.google.gson.annotations.SerializedName;

public final class SentimentRequest {
    @SerializedName("inputs") public final String inputs;
    @SerializedName("parameters") public final Parameters parameters;
    public SentimentRequest(String text) { inputs=text; parameters=new Parameters(3); }
    public static final class Parameters {
        @SerializedName("top_k") public final int topK;
        public Parameters(int topK) { this.topK=topK; }
    }
}
