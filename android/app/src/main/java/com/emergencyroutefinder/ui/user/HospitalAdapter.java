package com.emergencyroutefinder.ui.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.Hospital;

import java.util.List;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {

    public interface SelectListener { void onSelect(long hospitalId); }

    private final List<Hospital> hospitals;
    private final SelectListener listener;

    public HospitalAdapter(List<Hospital> hospitals, SelectListener listener) {
        this.hospitals = hospitals;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Hospital hospital = hospitals.get(position);
        h.tvName.setText(hospital.getName());
        h.tvAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : "");
        h.tvDistance.setText(String.format("%.1f km away", hospital.getDistanceKm()));
        h.tvEmergency.setText(hospital.isEmergencyAvailable() ? "✅ Emergency available" : "❌ No emergency service");
        h.tvPhone.setText(hospital.getContactNumber() != null ? "📞 " + hospital.getContactNumber() : "");
        h.btnSelect.setOnClickListener(v -> listener.onSelect(hospital.getId()));
    }

    @Override
    public int getItemCount() { return hospitals.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvDistance, tvEmergency, tvPhone;
        Button btnSelect;
        ViewHolder(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tv_hospital_name);
            tvAddress   = v.findViewById(R.id.tv_address);
            tvDistance  = v.findViewById(R.id.tv_distance);
            tvEmergency = v.findViewById(R.id.tv_emergency);
            tvPhone     = v.findViewById(R.id.tv_phone);
            btnSelect   = v.findViewById(R.id.btn_select_hospital);
        }
    }
}
