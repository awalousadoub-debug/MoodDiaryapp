package com.mooddiary.data.remote;

import com.example.mooddiaryapp.BuildConfig;

public final class BuildConfigHfTokenProvider implements HfTokenProvider {
    @Override
    public String getToken() {
        return BuildConfig.HF_TOKEN;
    }
}
