package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository — JDBC-based data access for the users table.
 *
 * Demonstrates: JDBC, PreparedStatement, DAO Pattern, Exception Handling
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ─────────────────────────────────────────────────────────────

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setMobile(rs.getString("mobile"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("is_active"));
        double lat = rs.getDouble("location_lat"); if (!rs.wasNull()) u.setLocationLat(lat);
        double lng = rs.getDouble("location_lng"); if (!rs.wasNull()) u.setLocationLng(lng);
        Timestamp locTs = rs.getTimestamp("location_updated_at");
        if (locTs != null) u.setLocationUpdatedAt(locTs.toLocalDateTime());
        u.setAllowCrowdTraffic(rs.getBoolean("allow_crowd_traffic"));
        u.setFcmToken(rs.getString("fcm_token"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) u.setUpdatedAt(updated.toLocalDateTime());
        return u;
    };

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public User save(User user) {
        String sql = """
            INSERT INTO users (full_name, email, mobile, password_hash, role, is_active, allow_crowd_traffic)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMobile());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole() != null ? user.getRole() : "USER");
            ps.setBoolean(6, user.isActive());
            ps.setBoolean(7, user.isAllowCrowdTraffic());
            return ps;
        }, keyHolder);
        user.setId(keyHolder.getKey().longValue());
        return user;
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> results = jdbc.query(sql, USER_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> results = jdbc.query(sql, USER_MAPPER, email);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users", USER_MAPPER);
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public boolean existsByMobile(String mobile) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE mobile = ?", Integer.class, mobile);
        return count != null && count > 0;
    }

    public void updateLocation(Long userId, double lat, double lng) {
        jdbc.update("""
            UPDATE users SET location_lat = ?, location_lng = ?, location_updated_at = NOW()
            WHERE id = ?
            """, lat, lng, userId);
    }

    public void updateFcmToken(Long userId, String token) {
        jdbc.update("UPDATE users SET fcm_token = ? WHERE id = ?", token, userId);
    }

    public void updateCrowdConsent(Long userId, boolean allow) {
        jdbc.update("UPDATE users SET allow_crowd_traffic = ? WHERE id = ?", allow, userId);
    }

    public void updateProfile(User user) {
        jdbc.update("""
            UPDATE users SET full_name = ?, mobile = ? WHERE id = ?
            """, user.getFullName(), user.getMobile(), user.getId());
    }

    public List<User> findCrowdConsentingUsers() {
        return jdbc.query("SELECT * FROM users WHERE allow_crowd_traffic = TRUE AND is_active = TRUE", USER_MAPPER);
    }
}
