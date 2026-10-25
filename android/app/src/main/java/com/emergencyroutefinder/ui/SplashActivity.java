package com.emergencyroutefinder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.ui.auth.LoginActivity;
import com.emergencyroutefinder.ui.user.UserDashboardActivity;
import com.emergencyroutefinder.ui.worker.WorkerDashboardActivity;
import com.emergencyroutefinder.util.SessionManager;

/**
 * Splash Screen — shows branding for 2 seconds then routes to login or dashboard.
 */
public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager session = SessionManager.getInstance(this);
        session.restoreToken();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (session.isLoggedIn()) {
                // Route to correct dashboard based on role
                if (session.isWorker()) {
                    startActivity(new Intent(this, WorkerDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, UserDashboardActivity.class));
                }
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }
}
