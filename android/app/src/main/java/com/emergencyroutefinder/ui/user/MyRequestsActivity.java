package com.emergencyroutefinder.ui.user;
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
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyRequestsActivity extends BaseActivity {
    private RecyclerView rvRequests;
    private TextView tvEmptyState;
    @Override 
    protected void onCreate(Bundle s) { 
        super.onCreate(s); 
        setContentView(R.layout.activity_my_requests); 
        
        rvRequests = findViewById(R.id.rv_requests);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        
        // Ensure there is a TextView for empty state in XML, if not, we handle it gracefully.
        tvEmptyState = findViewById(R.id.tv_empty_state); // Might be null as we didn't add it in MyRequests, but we can just use Toast or ignore.
        
        SessionManager session = SessionManager.getInstance(this);
        loadRequests(session.getToken());
    }

    private void loadRequests(String userId) {
        FirebaseManager.getInstance().getRequestsByUser(userId)
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
                        Toast.makeText(this, "No requests found.", Toast.LENGTH_SHORT).show();
                    } else {
                        RequestSummaryAdapter adapter = new RequestSummaryAdapter(requests);
                        rvRequests.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(MyRequestsActivity.this, "Could not load history.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
