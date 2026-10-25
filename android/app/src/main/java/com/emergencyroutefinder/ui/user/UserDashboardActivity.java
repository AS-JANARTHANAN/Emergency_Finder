package com.emergencyroutefinder.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.*;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.ui.auth.LoginActivity;
import com.emergencyroutefinder.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * User Dashboard — main hub for emergency user.
 *
 * Shows:
 * - Welcome banner with user name
 * - SOS button → RequestEmergencyActivity
 * - Recent requests list
 * - Quick access to notifications, profile
 */
public class UserDashboardActivity extends BaseActivity {

    private TextView tvWelcome, tvActiveStatus;
    private Button btnSos, btnMyRequests, btnProfile, btnLogout;
    private RecyclerView rvRecentRequests;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        SessionManager session = SessionManager.getInstance(this);

        tvWelcome      = findViewById(R.id.tv_welcome);
        tvActiveStatus = findViewById(R.id.tv_active_status);
        btnSos         = findViewById(R.id.btn_sos);
        btnMyRequests  = findViewById(R.id.btn_my_requests);
        btnProfile     = findViewById(R.id.btn_profile);
        btnLogout      = findViewById(R.id.btn_logout);
        rvRecentRequests = findViewById(R.id.rv_recent_requests);
        progressBar    = findViewById(R.id.progress_bar);

        tvWelcome.setText("Hello, " + session.getFullName() + "!");

        rvRecentRequests.setLayoutManager(new LinearLayoutManager(this));

        btnSos.setOnClickListener(v ->
                startActivity(new Intent(this, RequestEmergencyActivity.class)));

        btnMyRequests.setOnClickListener(v ->
                startActivity(new Intent(this, MyRequestsActivity.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadRecentRequests(session.getToken());
    }

    private void loadRecentRequests(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        
        FirebaseManager.getInstance().getRequestsByUser(userId)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<EmergencyRequest> requests = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            try {
                                EmergencyRequest r = doc.toObject(EmergencyRequest.class);
                                if (r != null) requests.add(r);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        // Check for active request
                        for (EmergencyRequest r : requests) {
                            String status = r.getStatus() != null ? r.getStatus().name() : "";
                            if (!"COMPLETED".equals(status) && !"CANCELLED".equals(status)) {
                                tvActiveStatus.setText("Active request: " + status);
                                tvActiveStatus.setVisibility(View.VISIBLE);
                                // Show button to open live tracking
                                tvActiveStatus.setOnClickListener(v -> {
                                    Intent i = new Intent(UserDashboardActivity.this,
                                            LiveResponseMapActivity.class);
                                    i.putExtra("requestId", r.getId());
                                    i.putExtra("sourceLat", r.getSourceLat());
                                    i.putExtra("sourceLng", r.getSourceLng());
                                    startActivity(i);
                                });
                                break;
                            }
                        }
                        
                        // Sort logic can be added here if needed
                        
                        // Show last 3 requests
                        List<EmergencyRequest> recent = requests.subList(0, Math.min(3, requests.size()));
                        RequestSummaryAdapter adapter = new RequestSummaryAdapter(recent);
                        rvRecentRequests.setAdapter(adapter);
                    } else {
                        Toast.makeText(UserDashboardActivity.this,
                                "Could not load requests.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload recent requests when returning to dashboard
        loadRecentRequests(SessionManager.getInstance(this).getToken());
    }
}
