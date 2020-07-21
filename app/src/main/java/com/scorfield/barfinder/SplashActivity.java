package com.scorfield.barfinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String BAR_SHARED_PREFS = "BarCache";
    private static final String BAR_SEARCH_RANGE = "SearchRange";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Setting loading effect
        new Thread() {
            public void run() {
                try {
                    // Thread will sleep for 3 seconds
                    sleep(3 * 1000);

                    SharedPreferences sharedPreferences = getSharedPreferences(BAR_SHARED_PREFS, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    int range = sharedPreferences.getInt(BAR_SEARCH_RANGE, 0);
                    if (range ==0) {
                        editor.putInt(BAR_SEARCH_RANGE, 5000);
                        editor.apply();
                    }

                    Intent intent = new Intent(SplashActivity.this, PermissionActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    finish();

                } catch (Exception e) {
                }
            }
        }.start();
    }
}
