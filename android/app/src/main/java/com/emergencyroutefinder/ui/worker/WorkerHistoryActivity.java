package com.emergencyroutefinder.ui.worker;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.network.FirebaseManager;
import com.emergencyroutefinder.util.SessionManager;
import com.emergencyroutefinder.model.EmergencyRequest;
import com.emergencyroutefinder.ui.user.RequestSummaryAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class WorkerHistoryActivity extends BaseActivity {
    private RecyclerView rvHistory;
    private TextView tvEmptyState;
    @Override 
    protected void onCreate(Bundle s) { 
        super.onCreate(s); 
        setContentView(R.layout.activity_worker_history); 
        
        rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        
        tvEmptyState = findViewById(R.id.tv_empty_state);
        
        SessionManager session = SessionManager.getInstance(this);
        loadHistory(session.getToken());
    }

    private void loadHistory(String workerId) {
        FirebaseManager.getInstance().getRequestsByWorker(workerId)
            .addOnCompleteListener(task -> {
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
                    if(requests.isEmpty()) {
                        if(tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvHistory.setVisibility(View.GONE);
                        }
                    } else {
                        if(tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                        RequestSummaryAdapter adapter = new RequestSummaryAdapter(requests);
                        rvHistory.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(WorkerHistoryActivity.this, "Could not load history.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
