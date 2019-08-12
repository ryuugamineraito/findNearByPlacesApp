package com.google.ar.sceneform.samples.hellosceneform.Remote;

import com.google.ar.sceneform.samples.hellosceneform.Model.MyPlaces;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface GoogleAPIService {
    @GET
    Call<MyPlaces> getNearByPlaces(@Url String url);
}
