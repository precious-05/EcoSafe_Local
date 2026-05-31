package com.example.eco_front;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 9900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            Intent nextIntent;
            if (PrivacyPrefs.isAccepted(SplashActivity.this)) {
                nextIntent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                nextIntent = new Intent(SplashActivity.this, PrivacyPolicyActivity.class);
                nextIntent.putExtra(PrivacyPolicyActivity.EXTRA_REQUIRE_ACCEPTANCE, true);
            }
            startActivity(nextIntent);
            finish();
        }, SPLASH_DURATION);
    }
}
