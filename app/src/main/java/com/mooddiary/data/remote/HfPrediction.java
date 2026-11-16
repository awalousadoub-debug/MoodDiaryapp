package com.mooddiary.data.remote;

import com.google.gson.annotations.SerializedName;

public final class HfPrediction {
    @SerializedName("label") public String label;
    @SerializedName("score") public Float score;
}
