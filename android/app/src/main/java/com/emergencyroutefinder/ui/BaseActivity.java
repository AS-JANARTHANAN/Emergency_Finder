package com.emergencyroutefinder.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BaseActivity for all activities in the Emergency Route Finder application.
 *
 * Guarantees that screenshots and screen recordings are ALWAYS allowed
 * across the entire app lifecycle, preventing FLAG_SECURE from being applied
 * or reapplied by vendor ROMs, libraries, or system events.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        stripSecureFlag();
        return true;
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        stripSecureFlag();
        super.onCreate(savedInstanceState);
        stripSecureFlag();
        setupScreenshotEnforcement();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        stripSecureFlag();
        setupScreenshotEnforcement();
    }

    @Override
    protected void onStart() {
        super.onStart();
        stripSecureFlag();
        setupScreenshotEnforcement();
    }

    @Override
    protected void onResume() {
        super.onResume();
        stripSecureFlag();
        setupScreenshotEnforcement();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        stripSecureFlag();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        stripSecureFlag();
        setupScreenshotEnforcement();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        stripSecureFlag();
    }

    @Override
    protected void onDestroy() {
        try {
            if (getWindow() != null && getWindow().peekDecorView() != null) {
                View decor = getWindow().peekDecorView();
                decor.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    /**
     * Actively strips WindowManager.LayoutParams.FLAG_SECURE and ensures
     * recent task snapshots and screen captures are explicitly enabled.
     */
    public void stripSecureFlag() {
        if (getWindow() != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setRecentsScreenshotEnabled(true);
            }
        }
    }

    private void setupScreenshotEnforcement() {
        try {
            if (getWindow() != null) {
                View decor = getWindow().peekDecorView();
                if (decor != null) {
                    decor.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                    decor.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
                    decor.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> stripSecureFlag());
                }
            }
        } catch (Exception ignored) {}
    }
}
