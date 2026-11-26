package com.mooddiary.data.remote;

/** Supplies an inference token without placing it in request logs or source control. */
public interface HfTokenProvider {
    String getToken();
}
