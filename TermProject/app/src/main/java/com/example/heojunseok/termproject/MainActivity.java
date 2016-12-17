package com.example.heojunseok.termproject;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import noman.googleplaces.NRPlaces;
import noman.googleplaces.PlaceType;
import noman.googleplaces.PlacesException;
import noman.googleplaces.PlacesListener;

public class MainActivity extends Activity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, PlacesListener{
    private  static final String TAG = "@@@";
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest;
    private static final int REQUEST_CODE_LOCATION = 2000;
    private static final int REQUEST_CODE_GPS = 2001;
    LocationManager locationManager;
    MapFragment mapFragment;
    boolean setGPS = false;
    boolean trace = false;
    LatLng SEOUL = new LatLng(37.56, 126.97);
    double latitude, longitude;
    private GoogleMap googleMap;
    LatLng currentPosition;
    Marker current_marker = null;
    List<Marker> previous_marker = null;

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    //GPS 활성화를 위한 대화상자
    private  void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS가 비활성화 되어있습니다. 활성화 할까요?")
                .setCancelable(false)
                .setPositiveButton("설정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(callGPSSettingIntent, REQUEST_CODE_GPS);
                    }
                });
        alertDialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
    //대화상자 결과처리
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case REQUEST_CODE_GPS:
                if (locationManager == null)
                    locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    setGPS = true;
                    mapFragment.getMapAsync(MainActivity.this);
                }
                break;
        }
    }
    protected  void onCreate(Bundle savedInstancState){
        super.onCreate(savedInstancState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        previous_marker = new ArrayList<Marker>();

        Button button = (Button)findViewById(R.id.Search);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                googleMap.clear();

                if(previous_marker != null)
                    previous_marker.clear();

                new NRPlaces.Builder()
                        .listener(MainActivity.this)
                        .key("AIzaSyDmWNCgElWM2VCRIZOPdUdam4TqiZE9QW8")
                        .latlng(currentPosition.latitude, currentPosition.longitude)
                        .radius(10000)
                        .type(PlaceType.POLICE)
                        .build()
                        .execute();
            }
        });
    }
    public boolean checkLocationPermission(){
        Log.d(TAG, "checkLocationPermission");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                }
                return false;
            }
            else{
                Log.d(TAG, "checkLocationPermission" + "이미 퍼미션 획득한 경우");

                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS){
                    Log.d(TAG, "checkLocationPermission Version >= M");
                    showGPSDisabledAlertToUser();
                }
                if (mGoogleApiClient == null){
                    Log.d(TAG, "checkLocationPermission " + "mGoogleApiClient == NULL");
                    buildGoogleApiClient();
                }
                else
                    Log.d(TAG, "checkLocationPermission " + "mGoogleApiCliecnt != NULL");

                if (mGoogleApiClient.isConnected())
                    Log.d(TAG, "checkLocationPermission " + "mGoogleApiClient 연결되어 있음");
                else
                    Log.d(TAG, "checkLocationPermission " + "mGoogleApiClient 끊어져 있음");

                mGoogleApiClient.reconnect();
                googleMap.setMyLocationEnabled(true);
            }
        }
        else{
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS){
                Log.d(TAG, "checkLocationPermission Version < M");
                showGPSDisabledAlertToUser();
            }
            if (mGoogleApiClient == null)
                buildGoogleApiClient();
            googleMap.setMyLocationEnabled(true);
        }
        return true;
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_CODE_LOCATION:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                            Log.d(TAG, "onRequestPermissionsResult");
                            showGPSDisabledAlertToUser();
                        }

                        if (mGoogleApiClient == null)
                            buildGoogleApiClient();
                        googleMap.setMyLocationEnabled(true);
                    }
                }
                else
                    Toast.makeText(this, "Permission cancle", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }
    public void onMapReady(final GoogleMap map){
        googleMap = map;

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback(){
            @Override
            public void onMapLoaded(){
                Log.d(TAG, "onMapLoaded");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    checkLocationPermission();
                else{
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS){
                        Log.d(TAG, "onMapLoaded");
                        showGPSDisabledAlertToUser();
                    }
                    if (mGoogleApiClient == null)
                        buildGoogleApiClient();
                    googleMap.setMyLocationEnabled(true);
                }
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                buildGoogleApiClient();
                googleMap.setMyLocationEnabled(true);
            }
            else{
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        }
        else{
            buildGoogleApiClient();
            googleMap.setMyLocationEnabled(true);
        }
    }
    public void onConnected(@Nullable Bundle bundle){
        Log.d(TAG, "onConnected");

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            setGPS = true;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            Log.d(TAG, "onConnected " + "getLocationAvailability mGoogleApiClient.isConnected() = " + mGoogleApiClient.isConnected());
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();

            if (setGPS && mGoogleApiClient.isConnected()){
                Log.d(TAG, "onConnected " + "requestLocationUpdateds");
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location == null)
                    return;

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("현재위치");
                googleMap.addMarker(markerOptions);

                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        }
    }
    public void onConnectionFailed(ConnectionResult result){
        Log.d(TAG, "Connection failed : ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
    public void onConnectionSuspended(int cause){
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
    protected void onStart(){
        super.onStart();

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }
    public void onResume(){
        super.onResume();

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }
    protected void onStop(){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
    public void onPause(){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }
    protected void onDestroy(){
        Log.d(TAG, "OnDestroy");

        if (mGoogleApiClient != null){
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        super.onDestroy();
    }
    public void onLocationChanged(final Location location){
        String errorMessage = "";
        currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (current_marker != null)
            current_marker.remove();

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("현재위치");
        current_marker = googleMap.addMarker(markerOptions);

        Button button2 = (Button)findViewById(R.id.Trace);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (trace == false)
                    trace = true;
                else
                    trace = false;
            }
        });
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        if (trace == true) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;

            try{
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),location.getLongitude(), 1);
            }catch (IOException ioException){
                errorMessage = "지오코더 서비스 사용불가";
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }catch (IllegalArgumentException illegalArgumentException){
                errorMessage = "잘못된 GPS 좌표";
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            googleMap.getUiSettings().setCompassEnabled(true);
            if (addresses == null || addresses.size() == 0){
                if (errorMessage.isEmpty()){
                    errorMessage = "주소 미발견";
                    Log.e(TAG, errorMessage);
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }else{
                Address address = addresses.get(0);
                Toast.makeText(this, address.getAddressLine(0).toString() + ", " + Double.toString(latitude) + ", " + Double.toString(longitude), Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void onPlacesFailure(PlacesException e){
        Log.i("PlacesAPI", "onPlacesFailure()");
    }
    public void onPlacesStart(){
        Log.i("PlacesAPI", "onPlacesStart()");
    }
    public void onPlacesSuccess(final  List<noman.googleplaces.Place> places){
        Log.i("PlacesAPI", "onPlacesSuccess()");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (noman.googleplaces.Place place : places){
                    LatLng latLng = new LatLng(place.getLatitude(), place.getLongitude());

                    MarkerOptions markerOptions =  new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(place.getName());
                    markerOptions.snippet(place.getVicinity());
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.police_department_icon2));
                    Marker item = googleMap.addMarker(markerOptions);
                    previous_marker.add(item);
                }

                HashSet<Marker> hashSet = new HashSet<Marker>();
                hashSet.addAll(previous_marker);
                previous_marker.clear();
                previous_marker.addAll(hashSet);
            }
        });
    }
    public void onPlacesFinished(){
        Log.i("PlacesAPI", "onPlacesFinished()");
    }
}
