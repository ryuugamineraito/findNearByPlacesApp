/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;
import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.hellosceneform.Model.MyPlaces;
import com.google.ar.sceneform.samples.hellosceneform.Model.Results;
import com.google.ar.sceneform.samples.hellosceneform.Remote.GoogleAPIService;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;
    private static final int MY_PERMISSION_CODE = 1000;
    private GoogleMap mMap;
  private GoogleApiClient mGoogleApiClient;
  private ArFragment arFragment;
  private ModelRenderable andyRenderable;
  private double latitude,longitude;
  private Location mLocation;
  private Marker mMarker;
  private LocationRequest mLocationRequest;
  GoogleAPIService mService;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    // When you build a Renderable, Sceneform loads its resources in the background while returning
    // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
    ModelRenderable.builder()
        .setSource(this, R.raw.andy)
        .build()
        .thenAccept(renderable -> andyRenderable = renderable)
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (andyRenderable == null) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable andy and add it to the anchor.
          TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
          andy.setParent(anchorNode);
          andy.setRenderable(andyRenderable);
          andy.select();
        });
      SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.mapFragment);
      mapFragment.getMapAsync(this);
      //init service
      mService= Common.getGoogleAPIService();
      //request runtime permission
      if(Build.VERSION.SDK_INT >= VERSION_CODES.M)
      {
          checkLocationPermission();
      }
        //need to find the place to set up nearByPlace("cafe"); -> ask someone :))

  }


    private void nearByPlace(String placeType) {
      //if(mMap!=null)
        mMap.clear();

      String url=getUrl(latitude,longitude,placeType);

      mService.getNearByPlaces(url)
              .enqueue(new Callback<MyPlaces>() {
                  @Override
                  public void onResponse(Call<MyPlaces> call, Response<MyPlaces> response) {
                      if(response.isSuccessful())
                      {
                          for(int i=0;i<response.body().getResults().length;++i)
                          {
                              MarkerOptions markerOptions=new MarkerOptions();
                              Results googlePlace = response.body().getResults()[i];
                              double lat =Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                              double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());
                              Log.d("test LatLng","Lat:"+lat+",Lng:"+lng);
                              String placeName =googlePlace.getName();
                              String vicinity=googlePlace.getVicinity();
                              LatLng latLng=new LatLng(lat,lng);
                              markerOptions.position(latLng);
                              markerOptions.title(placeName);
                              markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                              mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                              mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                          }
                      }
                  }

                  @Override
                  public void onFailure(Call<MyPlaces> call, Throwable t) {

                  }
              });
    }

    private String getUrl(double latitude, double longitude, String placeType) {
      StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
      googlePlaceUrl.append("location="+latitude+","+longitude);
      googlePlaceUrl.append("&radius="+1000);
      googlePlaceUrl.append("&type"+placeType);
      googlePlaceUrl.append("&sensor=true");
      googlePlaceUrl.append("&key="+getResources().getString(R.string.browser_key));
      Log.d("getUrl",googlePlaceUrl.toString());
      return googlePlaceUrl.toString();
    }

    private boolean checkLocationPermission() {
      if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
      {
          if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
              ActivityCompat.requestPermissions(this,new String[]{
                      Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_CODE);
          else
              ActivityCompat.requestPermissions(this,new String[]{
                      Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_CODE);
          return false;
      }
      else
          return true;


    }

    /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        //Init Google Play Services
        if(Build.VERSION.SDK_INT >= VERSION_CODES.M)
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
            {
                buildGoogleAPIClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else
        {
            buildGoogleAPIClient();
            mMap.setMyLocationEnabled(true);
        }

    }

    private synchronized void buildGoogleAPIClient() {
      mGoogleApiClient=new GoogleApiClient.Builder(this)
              .addConnectionCallbacks(this)
              .addOnConnectionFailedListener(this)
              .addApi(LocationServices.API).build();
      mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation=location;
        if(mMarker!=null)
            mMarker.remove();

        latitude=location.getLatitude();
        longitude=location.getLongitude();
        Log.d("test LatLng","Lat:"+latitude+",Lng:"+longitude);
        LatLng latLng=new LatLng(latitude,longitude);
        MarkerOptions markerOptions=new MarkerOptions()
                .position(latLng)
                .title("Your position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mMarker=mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        if(mGoogleApiClient!=null)
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_CODE:
            {
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                    {
                        if(mGoogleApiClient==null)
                            buildGoogleAPIClient();
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else
                    Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
                break;
            }
        }

    }


}
