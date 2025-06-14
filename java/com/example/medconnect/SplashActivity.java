package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the new entry screen (SymptomBrowseActivity) */
                Intent mainIntent = new Intent(SplashActivity.this, SymptomBrowseActivity.class);
                startActivity(mainIntent);
                finish(); // Close the splash activity
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}