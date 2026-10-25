package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.CrowdLocationPoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class CrowdLocationRepository {

    private final JdbcTemplate jdbc;
    public CrowdLocationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<CrowdLocationPoint> MAPPER = (rs, rowNum) -> {
        CrowdLocationPoint p = new CrowdLocationPoint();
        p.setId(rs.getLong("id"));
        long uId = rs.getLong("user_id"); if (!rs.wasNull()) p.setUserId(uId);
        long sId = rs.getLong("segment_id"); if (!rs.wasNull()) p.setSegmentId(sId);
        p.setLat(rs.getDouble("lat"));
        p.setLng(rs.getDouble("lng"));
        double speed = rs.getDouble("speed_kmh"); if (!rs.wasNull()) p.setSpeedKmh(speed);
        Timestamp ts = rs.getTimestamp("recorded_at");
        if (ts != null) p.setRecordedAt(ts.toLocalDateTime());
        return p;
    };

    public CrowdLocationPoint save(CrowdLocationPoint point) {
        String sql = """
            INSERT INTO crowd_location_points (user_id, segment_id, lat, lng, speed_kmh, direction_deg)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (point.getUserId() != null) ps.setLong(1, point.getUserId()); else ps.setNull(1, Types.BIGINT);
            if (point.getSegmentId() != null) ps.setLong(2, point.getSegmentId()); else ps.setNull(2, Types.BIGINT);
            ps.setDouble(3, point.getLat());
            ps.setDouble(4, point.getLng());
            if (point.getSpeedKmh() != null) ps.setDouble(5, point.getSpeedKmh()); else ps.setNull(5, Types.DOUBLE);
            if (point.getDirectionDeg() != null) ps.setDouble(6, point.getDirectionDeg()); else ps.setNull(6, Types.DOUBLE);
            return ps;
        }, kh);
        point.setId(kh.getKey().longValue());
        return point;
    }

    /**
     * Get recent crowd observations near a bounding box within a freshness window.
     * @param freshnessMinutes only data within this window (e.g. 15 minutes)
     */
    public List<CrowdLocationPoint> findRecentNearby(double lat, double lng,
                                                      double radiusDeg, int freshnessMinutes) {
        return jdbc.query("""
            SELECT * FROM crowd_location_points
            WHERE lat BETWEEN ? AND ?
              AND lng BETWEEN ? AND ?
              AND recorded_at >= NOW() - INTERVAL ? MINUTE
            """, MAPPER,
            lat - radiusDeg, lat + radiusDeg,
            lng - radiusDeg, lng + radiusDeg,
            freshnessMinutes);
    }

    /** Purge stale crowd data older than retentionMinutes. */
    public int purgeStale(int retentionMinutes) {
        return jdbc.update("DELETE FROM crowd_location_points WHERE recorded_at < NOW() - INTERVAL ? MINUTE",
                retentionMinutes);
    }
}
