package com.emergencyroutefinder.ui.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.RouteRecord;

import java.util.List;

/**
 * RecyclerView adapter for route options list.
 */
public class RouteListAdapter extends RecyclerView.Adapter<RouteListAdapter.ViewHolder> {

    public interface RouteSelectListener { void onRouteSelected(long routeId); }

    private final List<RouteRecord> routes;
    private final RouteSelectListener listener;

    public RouteListAdapter(List<RouteRecord> routes, RouteSelectListener listener) {
        this.routes   = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        RouteRecord r = routes.get(position);

        h.tvLabel.setText(r.isRecommended() ? "⭐ " + r.getRouteLabel() : r.getRouteLabel());
        h.tvDistance.setText(r.getDistanceMeters() != null
                ? String.format("%.1f km", r.getDistanceMeters() / 1000.0) : "? km");
        h.tvDuration.setText(r.getTrafficDurationSec() != null
                ? formatDuration(r.getTrafficDurationSec()) : "? min");
        h.tvTraffic.setText("Traffic: " + (r.getGoogleTrafficState() != null ? r.getGoogleTrafficState() : "?"));
        h.tvScore.setText("Score: " + (r.getFinalRouteScore() != null ? String.format("%.0f", r.getFinalRouteScore()) : "?"));
        h.tvSources.setText("Sources: " + (r.getSourcesUsed() != null ? r.getSourcesUsed() : "?"));

        if (r.isRecommended()) {
            h.itemView.setBackgroundColor(0xFFE8F5E9); // Light green for recommended
        }

        h.itemView.setOnClickListener(v -> listener.onRouteSelected(r.getId()));
    }

    private String formatDuration(int seconds) {
        int mins = seconds / 60;
        return mins + " min";
    }

    @Override
    public int getItemCount() { return routes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel, tvDistance, tvDuration, tvTraffic, tvScore, tvSources;
        ViewHolder(View v) {
            super(v);
            tvLabel    = v.findViewById(R.id.tv_route_label);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvDuration = v.findViewById(R.id.tv_duration);
            tvTraffic  = v.findViewById(R.id.tv_traffic_state);
            tvScore    = v.findViewById(R.id.tv_score);
            tvSources  = v.findViewById(R.id.tv_sources);
        }
    }
}
