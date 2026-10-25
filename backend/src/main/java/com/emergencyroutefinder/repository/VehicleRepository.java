package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.EmergencyVehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class VehicleRepository {

    private final JdbcTemplate jdbc;
    public VehicleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<EmergencyVehicle> VEHICLE_MAPPER = (rs, rowNum) -> {
        String type = rs.getString("vehicle_type");
        EmergencyVehicle v;
        if ("AMBULANCE".equals(type)) {
            v = new com.emergencyroutefinder.model.Ambulance();
        } else {
            v = new com.emergencyroutefinder.model.FireEngine();
        }
        v.setId(rs.getLong("id"));
        v.setVehicleNumber(rs.getString("vehicle_number"));
        v.setVehicleType(EmergencyVehicle.VehicleType.valueOf(type));
        v.setWorkerId(rs.getLong("worker_id"));
        double lat = rs.getDouble("current_lat"); if (!rs.wasNull()) v.setCurrentLat(lat);
        double lng = rs.getDouble("current_lng"); if (!rs.wasNull()) v.setCurrentLng(lng);
        v.setAvailability(EmergencyVehicle.VehicleStatus.valueOf(rs.getString("availability")));
        long arId = rs.getLong("active_request_id"); if (!rs.wasNull()) v.setActiveRequestId(arId);
        v.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("last_location_update");
        if (ts != null) v.setLastLocationUpdate(ts.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) v.setCreatedAt(created.toLocalDateTime());
        return v;
    };

    public EmergencyVehicle save(EmergencyVehicle vehicle) {
        String sql = """
            INSERT INTO vehicles (vehicle_number, vehicle_type, worker_id, availability, is_active)
            VALUES (?, ?, ?, ?, ?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, vehicle.getVehicleNumber());
            ps.setString(2, vehicle.getVehicleType().name());
            ps.setLong(3, vehicle.getWorkerId());
            ps.setString(4, vehicle.getAvailability().name());
            ps.setBoolean(5, vehicle.isActive());
            return ps;
        }, kh);
        vehicle.setId(kh.getKey().longValue());
        return vehicle;
    }

    public Optional<EmergencyVehicle> findById(Long id) {
        List<EmergencyVehicle> r = jdbc.query("SELECT * FROM vehicles WHERE id = ?", VEHICLE_MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public Optional<EmergencyVehicle> findByWorkerId(Long workerId) {
        List<EmergencyVehicle> r = jdbc.query("SELECT * FROM vehicles WHERE worker_id = ? AND is_active = TRUE LIMIT 1",
                VEHICLE_MAPPER, workerId);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<EmergencyVehicle> findAvailableByType(String vehicleType) {
        if (vehicleType == null || "ANY".equals(vehicleType)) {
            return jdbc.query("SELECT * FROM vehicles WHERE availability = 'AVAILABLE' AND is_active = TRUE",
                    VEHICLE_MAPPER);
        }
        return jdbc.query("SELECT * FROM vehicles WHERE vehicle_type = ? AND availability = 'AVAILABLE' AND is_active = TRUE",
                VEHICLE_MAPPER, vehicleType);
    }

    public void updateAvailability(Long vehicleId, String status, Long activeRequestId) {
        if (activeRequestId != null) {
            jdbc.update("UPDATE vehicles SET availability = ?, active_request_id = ? WHERE id = ?",
                    status, activeRequestId, vehicleId);
        } else {
            jdbc.update("UPDATE vehicles SET availability = ?, active_request_id = NULL WHERE id = ?",
                    status, vehicleId);
        }
    }

    public void updateLocation(Long vehicleId, double lat, double lng) {
        jdbc.update("""
            UPDATE vehicles SET current_lat = ?, current_lng = ?, last_location_update = NOW() WHERE id = ?
            """, lat, lng, vehicleId);
    }
}
