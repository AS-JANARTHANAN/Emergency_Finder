package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.EmergencyRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * EmergencyRequestRepository — JDBC data access for emergency_requests table.
 * Uses transactions for status updates to ensure consistency.
 */
@Repository
public class EmergencyRequestRepository {

    private final JdbcTemplate jdbc;

    public EmergencyRequestRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<EmergencyRequest> REQUEST_MAPPER = (rs, rowNum) -> {
        EmergencyRequest r = new EmergencyRequest();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getLong("user_id"));
        r.setEmergencyType(EmergencyRequest.EmergencyType.valueOf(rs.getString("emergency_type")));
        r.setDescription(rs.getString("description"));
        r.setPriority(EmergencyRequest.Priority.valueOf(rs.getString("priority")));
        r.setSourceLat(rs.getDouble("source_lat"));
        r.setSourceLng(rs.getDouble("source_lng"));
        double dLat = rs.getDouble("dest_lat"); if (!rs.wasNull()) r.setDestLat(dLat);
        double dLng = rs.getDouble("dest_lng"); if (!rs.wasNull()) r.setDestLng(dLng);
        r.setPreferredVehicle(EmergencyRequest.VehiclePreference.valueOf(rs.getString("preferred_vehicle")));
        r.setStatus(EmergencyRequest.Status.valueOf(rs.getString("status")));
        long wId = rs.getLong("assigned_worker_id");  if (!rs.wasNull()) r.setAssignedWorkerId(wId);
        long vId = rs.getLong("assigned_vehicle_id"); if (!rs.wasNull()) r.setAssignedVehicleId(vId);
        long rId = rs.getLong("recommended_route_id"); if (!rs.wasNull()) r.setRecommendedRouteId(rId);
        long hId = rs.getLong("selected_hospital_id"); if (!rs.wasNull()) r.setSelectedHospitalId(hId);
        Timestamp ts;
        ts = rs.getTimestamp("created_at");   if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("accepted_at");  if (ts != null) r.setAcceptedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("responded_at"); if (ts != null) r.setRespondedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("arrived_at");   if (ts != null) r.setArrivedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("completed_at"); if (ts != null) r.setCompletedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("cancelled_at"); if (ts != null) r.setCancelledAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("updated_at");   if (ts != null) r.setUpdatedAt(ts.toLocalDateTime());
        return r;
    };

    public EmergencyRequest save(EmergencyRequest req) {
        String sql = """
            INSERT INTO emergency_requests
                (user_id, emergency_type, description, priority, source_lat, source_lng,
                 dest_lat, dest_lng, preferred_vehicle, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, req.getUserId());
            ps.setString(2, req.getEmergencyType().name());
            ps.setString(3, req.getDescription());
            ps.setString(4, req.getPriority().name());
            ps.setDouble(5, req.getSourceLat());
            ps.setDouble(6, req.getSourceLng());
            if (req.getDestLat() != null) ps.setDouble(7, req.getDestLat()); else ps.setNull(7, Types.DOUBLE);
            if (req.getDestLng() != null) ps.setDouble(8, req.getDestLng()); else ps.setNull(8, Types.DOUBLE);
            ps.setString(9, req.getPreferredVehicle() != null ? req.getPreferredVehicle().name() : "ANY");
            ps.setString(10, req.getStatus().name());
            return ps;
        }, keyHolder);
        req.setId(keyHolder.getKey().longValue());
        return req;
    }

    public Optional<EmergencyRequest> findById(Long id) {
        List<EmergencyRequest> r = jdbc.query("SELECT * FROM emergency_requests WHERE id = ?", REQUEST_MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<EmergencyRequest> findByUserId(Long userId) {
        return jdbc.query("SELECT * FROM emergency_requests WHERE user_id = ? ORDER BY created_at DESC",
                REQUEST_MAPPER, userId);
    }

    public List<EmergencyRequest> findByWorkerId(Long workerId) {
        return jdbc.query("SELECT * FROM emergency_requests WHERE assigned_worker_id = ? ORDER BY created_at DESC",
                REQUEST_MAPPER, workerId);
    }

    public void updateStatus(Long requestId, String newStatus) {
        jdbc.update("UPDATE emergency_requests SET status = ? WHERE id = ?", newStatus, requestId);
    }

    public void assignWorker(Long requestId, Long workerId, Long vehicleId) {
        jdbc.update("""
            UPDATE emergency_requests
            SET assigned_worker_id = ?, assigned_vehicle_id = ?, status = 'ASSIGNED', accepted_at = NULL
            WHERE id = ?
            """, workerId, vehicleId, requestId);
    }

    public void markAccepted(Long requestId) {
        jdbc.update("""
            UPDATE emergency_requests SET status = 'ACCEPTED', accepted_at = NOW() WHERE id = ?
            """, requestId);
    }

    public void markResponding(Long requestId) {
        jdbc.update("""
            UPDATE emergency_requests SET status = 'RESPONDING', responded_at = NOW() WHERE id = ?
            """, requestId);
    }

    public void markArrived(Long requestId) {
        jdbc.update("""
            UPDATE emergency_requests SET status = 'ARRIVED', arrived_at = NOW() WHERE id = ?
            """, requestId);
    }

    public void markCompleted(Long requestId) {
        jdbc.update("""
            UPDATE emergency_requests SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?
            """, requestId);
    }

    public void setRecommendedRoute(Long requestId, Long routeId) {
        jdbc.update("UPDATE emergency_requests SET recommended_route_id = ? WHERE id = ?", routeId, requestId);
    }

    public void setSelectedHospital(Long requestId, Long hospitalId) {
        jdbc.update("UPDATE emergency_requests SET selected_hospital_id = ? WHERE id = ?", hospitalId, requestId);
    }

    public List<EmergencyRequest> findByStatus(String status) {
        return jdbc.query("SELECT * FROM emergency_requests WHERE status = ?", REQUEST_MAPPER, status);
    }
}
