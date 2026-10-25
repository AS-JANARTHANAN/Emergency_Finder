package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.Hospital;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class HospitalRepository {

    private final JdbcTemplate jdbc;
    public HospitalRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Hospital> MAPPER = (rs, rowNum) -> {
        Hospital h = new Hospital();
        h.setId(rs.getLong("id"));
        h.setName(rs.getString("name"));
        h.setAddress(rs.getString("address"));
        h.setContactNumber(rs.getString("contact_number"));
        h.setLat(rs.getDouble("lat"));
        h.setLng(rs.getDouble("lng"));
        h.setEmergencyAvailable(rs.getBoolean("emergency_available"));
        h.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) h.setCreatedAt(ts.toLocalDateTime());
        return h;
    };

    public Optional<Hospital> findById(Long id) {
        List<Hospital> r = jdbc.query("SELECT * FROM hospitals WHERE id = ?", MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<Hospital> findAllActive() {
        return jdbc.query("SELECT * FROM hospitals WHERE is_active = TRUE", MAPPER);
    }

    public List<Hospital> findActiveWithEmergency() {
        return jdbc.query("SELECT * FROM hospitals WHERE is_active = TRUE AND emergency_available = TRUE", MAPPER);
    }
}
