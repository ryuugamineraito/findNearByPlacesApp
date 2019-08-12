package com.google.ar.sceneform.samples.hellosceneform;

import com.google.ar.sceneform.samples.hellosceneform.Remote.GoogleAPIService;
import com.google.ar.sceneform.samples.hellosceneform.Remote.RetrofitClient;

public class Common {
    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/";
    public static GoogleAPIService getGoogleAPIService ()
    {
        return RetrofitClient.getClient(GOOGLE_API_URL).create(GoogleAPIService.class);
    }
}
