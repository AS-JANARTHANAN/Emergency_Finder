package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.TrafficCamera;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class TrafficCameraRepository {

    private final JdbcTemplate jdbc;
    public TrafficCameraRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<TrafficCamera> MAPPER = (rs, rowNum) -> {
        TrafficCamera c = new TrafficCamera();
        c.setId(rs.getLong("id"));
        c.setCameraName(rs.getString("camera_name"));
        c.setRoadName(rs.getString("road_name"));
        c.setLat(rs.getDouble("lat"));
        c.setLng(rs.getDouble("lng"));
        long segId = rs.getLong("route_segment_id"); if (!rs.wasNull()) c.setRouteSegmentId(segId);
        c.setVideoFilePath(rs.getString("video_file_path"));
        c.setTrafficLevel(TrafficCamera.TrafficLevel.valueOf(rs.getString("traffic_level")));
        c.setTrafficScore(rs.getDouble("traffic_score"));
        c.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("last_updated");
        if (ts != null) c.setLastUpdated(ts.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) c.setCreatedAt(ct.toLocalDateTime());
        return c;
    };

    public List<TrafficCamera> findAll() {
        return jdbc.query("SELECT * FROM traffic_cameras WHERE is_active = TRUE", MAPPER);
    }

    public Optional<TrafficCamera> findById(Long id) {
        List<TrafficCamera> r = jdbc.query("SELECT * FROM traffic_cameras WHERE id = ?", MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /** Find cameras near a point (bounding box approximation for performance). */
    public List<TrafficCamera> findNearby(double lat, double lng, double radiusDegrees) {
        return jdbc.query("""
            SELECT * FROM traffic_cameras
            WHERE is_active = TRUE
              AND lat BETWEEN ? AND ?
              AND lng BETWEEN ? AND ?
            """, MAPPER,
            lat - radiusDegrees, lat + radiusDegrees,
            lng - radiusDegrees, lng + radiusDegrees);
    }

    public Optional<TrafficCamera> findBySegmentId(Long segmentId) {
        List<TrafficCamera> r = jdbc.query("SELECT * FROM traffic_cameras WHERE route_segment_id = ? LIMIT 1",
                MAPPER, segmentId);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }
}
