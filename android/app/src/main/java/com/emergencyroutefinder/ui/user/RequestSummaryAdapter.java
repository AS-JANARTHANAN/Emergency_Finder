package com.emergencyroutefinder.ui.user;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.EmergencyRequest;

import java.util.List;

/**
 * RecyclerView Adapter for request summaries.
 * Demonstrates: RecyclerView pattern, ViewHolder, Adapter
 */
public class RequestSummaryAdapter extends RecyclerView.Adapter<RequestSummaryAdapter.ViewHolder> {

    private final List<EmergencyRequest> requests;

    public RequestSummaryAdapter(List<EmergencyRequest> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyRequest req = requests.get(position);
        holder.tvType.setText(req.getEmergencyType() != null ? req.getEmergencyType().name() : "EMERGENCY");
        holder.tvStatus.setText(req.getStatus() != null ? req.getStatus().name() : "UNKNOWN");
        holder.tvPriority.setText("Priority: " + (req.getPriority() != null ? req.getPriority().name() : "?"));

        String statusColor = switch (req.getStatus() != null ? req.getStatus().name() : "PENDING") {
            case "COMPLETED" -> "#4CAF50";
            case "CANCELLED" -> "#F44336";
            case "RESPONDING", "ARRIVED" -> "#FF9800";
            default -> "#2196F3";
        };
        try {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor));
        } catch (Exception ignored) {}

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RequestDetailsActivity.class);
            intent.putExtra("requestId", req.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return requests.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvStatus, tvPriority;

        ViewHolder(View itemView) {
            super(itemView);
            tvType     = itemView.findViewById(R.id.tv_emergency_type);
            tvStatus   = itemView.findViewById(R.id.tv_status);
            tvPriority = itemView.findViewById(R.id.tv_priority);
        }
    }
}
