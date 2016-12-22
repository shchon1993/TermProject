package com.example.choihg.isafe;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


/**
 * Created by Administrator on 2016-12-21.
 */

public class SplashScreenActivity extends Activity{


    @Override
    protected  void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreenActivity.this,
                            MainActivity.class);
                    startActivity(i);
                    finish();

            }

        }, 1000);
    }
}
