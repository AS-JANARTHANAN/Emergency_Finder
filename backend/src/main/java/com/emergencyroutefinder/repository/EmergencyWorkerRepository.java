package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.EmergencyWorker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * EmergencyWorkerRepository — JDBC data access for emergency_workers table.
 */
@Repository
public class EmergencyWorkerRepository {

    private final JdbcTemplate jdbc;

    public EmergencyWorkerRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<EmergencyWorker> WORKER_MAPPER = (rs, rowNum) -> {
        EmergencyWorker w = new EmergencyWorker();
        w.setId(rs.getLong("id"));
        w.setFullName(rs.getString("full_name"));
        w.setEmail(rs.getString("email"));
        w.setMobile(rs.getString("mobile"));
        w.setPasswordHash(rs.getString("password_hash"));
        w.setWorkerType(EmergencyWorker.WorkerType.valueOf(rs.getString("worker_type")));
        w.setAvailability(EmergencyWorker.Availability.valueOf(rs.getString("availability")));
        double lat = rs.getDouble("location_lat"); if (!rs.wasNull()) w.setLocationLat(lat);
        double lng = rs.getDouble("location_lng"); if (!rs.wasNull()) w.setLocationLng(lng);
        Timestamp locTs = rs.getTimestamp("location_updated_at");
        if (locTs != null) w.setLocationUpdatedAt(locTs.toLocalDateTime());
        w.setLicenseId(rs.getString("license_id"));
        w.setActive(rs.getBoolean("is_active"));
        w.setFcmToken(rs.getString("fcm_token"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) w.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) w.setUpdatedAt(updated.toLocalDateTime());
        return w;
    };

    public EmergencyWorker save(EmergencyWorker worker) {
        String sql = """
            INSERT INTO emergency_workers
                (full_name, email, mobile, password_hash, worker_type, availability, license_id, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, worker.getFullName());
            ps.setString(2, worker.getEmail());
            ps.setString(3, worker.getMobile());
            ps.setString(4, worker.getPasswordHash());
            ps.setString(5, worker.getWorkerType().name());
            ps.setString(6, worker.getAvailability().name());
            ps.setString(7, worker.getLicenseId());
            ps.setBoolean(8, worker.isActive());
            return ps;
        }, keyHolder);
        worker.setId(keyHolder.getKey().longValue());
        return worker;
    }

    public Optional<EmergencyWorker> findById(Long id) {
        List<EmergencyWorker> r = jdbc.query("SELECT * FROM emergency_workers WHERE id = ?", WORKER_MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public Optional<EmergencyWorker> findByEmail(String email) {
        List<EmergencyWorker> r = jdbc.query("SELECT * FROM emergency_workers WHERE email = ?", WORKER_MAPPER, email);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<EmergencyWorker> findAll() {
        return jdbc.query("SELECT * FROM emergency_workers", WORKER_MAPPER);
    }

    public boolean existsByEmail(String email) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM emergency_workers WHERE email = ?", Integer.class, email);
        return c != null && c > 0;
    }

    /** Retrieve all AVAILABLE workers (with location) filtered by worker type where applicable. */
    public List<EmergencyWorker> findAvailableWorkers(String workerType) {
        if (workerType == null || "ANY".equals(workerType)) {
            return jdbc.query("""
                SELECT * FROM emergency_workers
                WHERE availability = 'AVAILABLE' AND is_active = TRUE
                  AND location_lat IS NOT NULL AND location_lng IS NOT NULL
                """, WORKER_MAPPER);
        }
        return jdbc.query("""
            SELECT * FROM emergency_workers
            WHERE availability = 'AVAILABLE' AND is_active = TRUE
              AND worker_type = ?
              AND location_lat IS NOT NULL AND location_lng IS NOT NULL
            """, WORKER_MAPPER, workerType);
    }

    public void updateAvailability(Long workerId, String availability) {
        jdbc.update("UPDATE emergency_workers SET availability = ? WHERE id = ?", availability, workerId);
    }

    public void updateLocation(Long workerId, double lat, double lng) {
        jdbc.update("""
            UPDATE emergency_workers SET location_lat = ?, location_lng = ?, location_updated_at = NOW()
            WHERE id = ?
            """, lat, lng, workerId);
    }

    public void updateFcmToken(Long workerId, String token) {
        jdbc.update("UPDATE emergency_workers SET fcm_token = ? WHERE id = ?", token, workerId);
    }
}
