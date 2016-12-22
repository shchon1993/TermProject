package com.example.choihg.isafe;


import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import noman.googleplaces.NRPlaces;
import noman.googleplaces.PlaceType;
import noman.googleplaces.PlacesException;
import noman.googleplaces.PlacesListener;

public class MainActivity extends Activity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, PlacesListener, NavigationView.OnNavigationItemSelectedListener{
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
    Marker child_marker = null;
    List<Marker> previous_marker = null;
    SafeService mService = new SafeService(); // 서비스 객체
    boolean isSend = true;
    boolean running = true;

    boolean parent = false;
    String radius = "10000";

    int second = 10;

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();

    Timer mTimer;
    TimerTask mTimerTask = null;

    public static class PosData {
        private double lat;
        private double lon;

        public PosData() { }

        public PosData(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }
    }

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
        if (requestCode == 1) { //메인이 요청한 코드가 1이고
            if (resultCode == 1) { //결과코드가 1일 때 메모쓰기액티비티에서 온 삽입 코드이므로
                String result = data.getStringExtra("parent"); //ListView에 파일 이름을 삽입한다.
                if (result.equals("1")){
                    parent = true;
                }
                else {
                    parent = false;
                }
            }
            else if (resultCode == 2) { //결과코드가 2일 때 메모읽기액티비티에서 온 삭제 코드이므로
                radius = data.getStringExtra("radius"); //ListView에 파일 이름을 삭제한다.
            }
            else if (resultCode == 3) {
                second = Integer.parseInt(data.getStringExtra("second"));
            }
        }
    }


    protected  void onCreate(Bundle savedInstancState){
        super.onCreate(savedInstancState);
        setContentView(R.layout.activity_main);

        Log.i("zzzzzzzzzzzzzzzzzz", "onCreate()");

        //bindService(new Intent(this, SafeService.class), mConnection, Context.BIND_AUTO_CREATE);
        startService(new Intent(this, SafeService.class));

        mTimer = new Timer();

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        previous_marker = new ArrayList<Marker>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }


    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_service) {
            if(isSend == true) {
                stopService(new Intent(this, SafeService.class));
                isSend = false;
            }
            else {
                startService(new Intent(this, SafeService.class));
                isSend = true;
            }
        }
        else if (id == R.id.nav_call) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
            if(intent != null) {
                if(intent.resolveActivity(getPackageManager()) != null) { startActivity(intent); }}
        }
        else if (id == R.id.nav_police) {
            Toast.makeText(getApplicationContext(), "반경 "+radius+"m 내의 경찰서들을 검색합니다.", Toast.LENGTH_SHORT).show();
            googleMap.clear();

            if(previous_marker != null)
                previous_marker.clear();

            new NRPlaces.Builder()
                    .listener(MainActivity.this)
                    .key("AIzaSyDmWNCgElWM2VCRIZOPdUdam4TqiZE9QW8")
                    .latlng(currentPosition.latitude, currentPosition.longitude)
                    .radius(Integer.parseInt(radius))
                    .type(PlaceType.POLICE)
                    .build()
                    .execute();

        }
        else if (id == R.id.nav_parent) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("부모 설정")
                    .setMessage("부모 : Yes, 아이 : No")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            parent = true;
                            Toast.makeText(getApplicationContext(), "부모로 설정하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    parent = false;
                    Toast.makeText(getApplicationContext(), "아이로 설정하였습니다.", Toast.LENGTH_SHORT).show();
                }
            });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
            mTimer.cancel();
            mTimer = new Timer();
        }
        else if (id == R.id.nav_distance) {
            final EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("검색 반경 설정")
                    .setMessage("값을 입력해주세요. (최대 : 50000)")
                    .setView(et)
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            if (et.getText().length() == 0 || Integer.parseInt(et.getText().toString()) > 50000)
                                Toast.makeText(getApplicationContext(), "값을 다시 입력해주세요.", Toast.LENGTH_SHORT).show();
                            else {
                                radius = et.getText().toString();
                                Toast.makeText(getApplicationContext(), "반경 : "+radius+"m", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                /*MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("현재위치");
                googleMap.addMarker(markerOptions);*/

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

        Log.i("zzzzzzzzzzzzzzzzzz", "onStart()");

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }


    public void onResume(){
        super.onResume();
        Log.i("zzzzzzzzzzzzzzzzzz", "onResume()");

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();

        mTimer = new Timer();
    }


    protected void onStop(){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        Log.i("zzzzzzzzzzzzzzzzzz", "onStop()");
        super.onStop();

        mTimer.cancel();
    }


    public void onPause(){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        Log.i("zzzzzzzzzzzzzzzzzz", "onPause()");
        super.onPause();
    }


    protected void onDestroy(){
        Log.i("zzzzzzzzzzzzzzzzzz", "onDestroy()");

        if (mGoogleApiClient != null){
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        super.onDestroy();

        running = false;
    }


    public void onLocationChanged(final Location location){
        Log.i("zzzzzzzzzzzzzzzz", "onLocationChanged()");
        String errorMessage = "";
        currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (current_marker != null)
            current_marker.remove();

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("내 위치");
        if(parent == true)
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.parent));
        else
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.child));
        current_marker = googleMap.addMarker(markerOptions);

        //latitude = location.getLatitude();
        //longitude = location.getLongitude();

        if(parent == false)
            startTimerTask1(location.getLatitude(), location.getLongitude());
        else
            startTimerTask2();
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


    private void startTimerTask1(final double x, final double y) {
        // 1. TimerTask 실행 중이라면 중단한다
        stopTimerTask();

        // 2. 새로운 TimerTask를 생성한다
        // 2-1. 100 밀리초마다 카운팅 되는 태스크를 정의
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Map<String, Object> hopperUpdates = new HashMap<String, Object>();
                hopperUpdates.put("position", new PosData(x, y));
                databaseReference.updateChildren(hopperUpdates);
                String st = Double.toString(x);
                Log.i("zzzzzzzzzzzzzzz", "(" + st + ")위치를 전송하였습니다.");
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        String st = Double.toString(x);
                        if (child_marker != null)
                            child_marker.remove();
                        Toast.makeText(getApplicationContext(), "("+x+", "+y+") 위치를 전송하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        // 3. TimerTask를 Timer를 통해 실행시킨다
        // 3-1. 즉시 타이머를 구동하고 100 밀리초 단위로 반복하라
        mTimer.schedule(mTimerTask, 0, 1000 * second);
    }

    private void startTimerTask2() {
        // 1. TimerTask 실행 중이라면 중단한다
        stopTimerTask();

        // 2. 새로운 TimerTask를 생성한다
        // 2-1. 100 밀리초마다 카운팅 되는 태스크를 정의
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                databaseReference.child("position").addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Get user value
                                MainActivity.PosData posData = dataSnapshot.getValue(MainActivity.PosData.class);
                                latitude = posData.getLat();
                                longitude = posData.getLon();
                                //String st = Double.toString(posData.getLat());
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                                // ...
                            }
                        });

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        //Toast.makeText(getApplicationContext(), second + "위치를 전송하였습니다.", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), "("+latitude+", "+longitude+") 위치를 수신하였습니다.", Toast.LENGTH_SHORT).show();

                        if (child_marker != null)
                            child_marker.remove();

                        LatLng latLng = new LatLng(latitude, longitude);
                        final MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.title("아이위치");
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.child));
                        child_marker = googleMap.addMarker(markerOptions);
                    }
                });
            }
        };

        // 3. TimerTask를 Timer를 통해 실행시킨다
        // 3-1. 즉시 타이머를 구동하고 100 밀리초 단위로 반복하라
        mTimer.schedule(mTimerTask, 0, 1000 * second);
    }


    private void stopTimerTask() {
        // 1. 모든 태스크를 중단한다
        if(mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }
}
