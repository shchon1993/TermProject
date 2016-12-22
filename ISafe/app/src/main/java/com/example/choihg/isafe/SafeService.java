package com.example.choihg.isafe;

import android.*;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import noman.googleplaces.PlacesListener;

/**
 * Created by ChoiHG on 2016-12-21.
 */

public class SafeService extends Service{
    double latitude, longitude;
    Timer mTimer = new Timer();
    TimerTask mTimerTask;
    private  static final String TAG = "@@@";
    LocationManager locationManager;
    boolean setGPS = false;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient = null;
    private GoogleMap googleMap;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();

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

    @Override
    public void onCreate() {
        Log.d("MusicService", "onCreate() ★★★★★");

        //locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);


        // 1. Notification 객체 생성
        // 1-1. Intent 객체 생성 - MainActivity 클래스를 실행하기 위한 Intent 객체
        Intent in = new Intent(this, MainActivity.class);

        // 1-2. Intent 객체를 이용하여 PendingIntent 객체를 생성 - Activity를 실행하기 위한 PendingIntent
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);

        // 1-3. Notification 객체 생성
        Notification noti = new Notification.Builder(this)
                .setContentTitle("iSafe가 작동 중 입니다.")
                .setContentText("위치 송수신 중...")
                .setSmallIcon(R.mipmap.icon)
                .setContentIntent(pIntent)
                .build();

        startForeground(123, noti);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MusicService", "onStartCommand() ★★★★★");
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        //startTimerTask();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
