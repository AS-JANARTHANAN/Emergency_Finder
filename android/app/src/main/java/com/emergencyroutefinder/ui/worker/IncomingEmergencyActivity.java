package com.emergencyroutefinder.ui.worker;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.*;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.network.FirebaseManager;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.util.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class IncomingEmergencyActivity extends BaseActivity {

    private static final int AUTO_DECLINE_SECONDS = 30;

    private TextView tvEmergencyType, tvPriority, tvDistance, tvCountdown;
    private Button btnAccept, btnDecline;
    private ProgressBar progressTimer;
    private CountDownTimer countDownTimer;
    private Ringtone ringtone;

    private long requestId;
    private String emergencyType;
    private String priority;
    private String distanceKm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_emergency);

        requestId     = getIntent().getLongExtra("requestId", -1);
        emergencyType = getIntent().getStringExtra("emergencyType");
        priority      = getIntent().getStringExtra("priority");
        distanceKm    = getIntent().getStringExtra("distanceKm");

        tvEmergencyType = findViewById(R.id.tv_emergency_type);
        tvPriority      = findViewById(R.id.tv_priority);
        tvDistance      = findViewById(R.id.tv_distance);
        tvCountdown     = findViewById(R.id.tv_countdown);
        btnAccept       = findViewById(R.id.btn_accept);
        btnDecline      = findViewById(R.id.btn_decline);
        progressTimer   = findViewById(R.id.progress_timer);

        tvEmergencyType.setText("🚨 " + (emergencyType != null ? emergencyType : "EMERGENCY"));
        tvPriority.setText("Priority: " + (priority != null ? priority : "HIGH"));
        tvDistance.setText("Distance: " + (distanceKm != null ? distanceKm + " km" : "Unknown"));

        btnAccept.setOnClickListener(v -> acceptEmergency());
        btnDecline.setOnClickListener(v -> declineEmergency());
        
        // Start playing the alarm sound
        playAlarmSound();

        // Verify that the request is still actionable. If the FCM arrived late and we already accepted, close this.
        verifyRequestStatus();

        startCountdown();
    }
    
    private void playAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void verifyRequestStatus() {
        FirebaseManager.getInstance().getEmergencyRequest(String.valueOf(requestId))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        EmergencyRequest req = task.getResult().toObject(EmergencyRequest.class);
                        if (req != null) {
                            String status = req.getStatus() != null ? req.getStatus().name() : "";
                            // If we already accepted it or someone else did, close the screen
                            if (!"ASSIGNED".equals(status) && !"PENDING".equals(status)) {
                                stopAlarmSound();
                                cancelCountdown();
                                finish();
                            }
                        }
                    }
                });
    }

    private void startCountdown() {
        progressTimer.setMax(AUTO_DECLINE_SECONDS * 1000);
        countDownTimer = new CountDownTimer(AUTO_DECLINE_SECONDS * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                if(tvCountdown != null) tvCountdown.setText("Auto-decline in " + seconds + "s");
                if(progressTimer != null) progressTimer.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                if(tvCountdown != null) tvCountdown.setText("Auto-declined");
                declineEmergency();
            }
        }.start();
    }

    private void acceptEmergency() {
        stopAlarmSound();
        cancelCountdown();
        if(btnAccept != null) {
            btnAccept.setEnabled(false);
            btnAccept.setText("Accepting...");
        }
        String workerId = SessionManager.getInstance(this).getToken();
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ACCEPTED");
        body.put("acceptedWorkerId", workerId);

        FirebaseManager.getInstance().updateEmergencyRequest(String.valueOf(requestId), body)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(IncomingEmergencyActivity.this, ActiveEmergencyActivity.class);
                        intent.putExtra("requestId", requestId);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        if(btnAccept != null) {
                            btnAccept.setEnabled(true);
                            btnAccept.setText("ACCEPT");
                        }
                        Toast.makeText(IncomingEmergencyActivity.this,
                                "Accept failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineEmergency() {
        stopAlarmSound();
        cancelCountdown();
        String workerId = SessionManager.getInstance(this).getToken();
        Map<String, Object> body = new HashMap<>();
        body.put("status", "PENDING"); 
        body.put("assignedWorkerId", null);

        FirebaseManager.getInstance().updateEmergencyRequest(String.valueOf(requestId), body)
                .addOnCompleteListener(task -> {}); // Ignore result

        finish();
    }

    private void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
        cancelCountdown();
    }
}
