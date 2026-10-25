package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.RouteRecord;
import com.emergencyroutefinder.model.RouteSegment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class RouteRepository {

    private final JdbcTemplate jdbc;
    public RouteRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<RouteRecord> ROUTE_MAPPER = (rs, rowNum) -> {
        RouteRecord r = new RouteRecord();
        r.setId(rs.getLong("id"));
        r.setRequestId(rs.getLong("request_id"));
        r.setRouteLabel(rs.getString("route_label"));
        r.setOriginLat(rs.getDouble("origin_lat"));
        r.setOriginLng(rs.getDouble("origin_lng"));
        r.setDestLat(rs.getDouble("dest_lat"));
        r.setDestLng(rs.getDouble("dest_lng"));
        int dm = rs.getInt("distance_meters"); if (!rs.wasNull()) r.setDistanceMeters(dm);
        int bd = rs.getInt("base_duration_sec"); if (!rs.wasNull()) r.setBaseDurationSec(bd);
        int td = rs.getInt("traffic_duration_sec"); if (!rs.wasNull()) r.setTrafficDurationSec(td);
        r.setEncodedPolyline(rs.getString("encoded_polyline"));
        r.setGoogleTrafficState(rs.getString("google_traffic_state"));
        double cts = rs.getDouble("camera_traffic_score"); if (!rs.wasNull()) r.setCameraTrafficScore(cts);
        double crs = rs.getDouble("crowd_traffic_score");  if (!rs.wasNull()) r.setCrowdTrafficScore(crs);
        double fts = rs.getDouble("fused_traffic_score");  if (!rs.wasNull()) r.setFusedTrafficScore(fts);
        double frs = rs.getDouble("final_route_score");    if (!rs.wasNull()) r.setFinalRouteScore(frs);
        String conf = rs.getString("confidence");
        if (conf != null) r.setConfidence(RouteRecord.Confidence.valueOf(conf));
        r.setRecommended(rs.getBoolean("is_recommended"));
        r.setSourcesUsed(rs.getString("sources_used"));
        r.setRecommendationReason(rs.getString("recommendation_reason"));
        Timestamp ts = rs.getTimestamp("created_at"); if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("updated_at"); if (ts != null) r.setUpdatedAt(ts.toLocalDateTime());
        return r;
    };

    private static final RowMapper<RouteSegment> SEG_MAPPER = (rs, rowNum) -> {
        RouteSegment s = new RouteSegment();
        s.setId(rs.getLong("id"));
        s.setRouteId(rs.getLong("route_id"));
        s.setSegmentIndex(rs.getInt("segment_index"));
        s.setStartLat(rs.getDouble("start_lat")); s.setStartLng(rs.getDouble("start_lng"));
        s.setEndLat(rs.getDouble("end_lat"));   s.setEndLng(rs.getDouble("end_lng"));
        s.setRoadName(rs.getString("road_name"));
        int dm = rs.getInt("distance_meters"); if (!rs.wasNull()) s.setDistanceMeters(dm);
        int ds = rs.getInt("duration_sec");    if (!rs.wasNull()) s.setDurationSec(ds);
        long cId = rs.getLong("camera_id");    if (!rs.wasNull()) s.setCameraId(cId);
        s.setGoogleTrafficState(rs.getString("google_traffic_state"));
        s.setCrowdTrafficState(rs.getString("crowd_traffic_state"));
        s.setFusedTrafficState(rs.getString("fused_traffic_state"));
        int delay = rs.getInt("estimated_delay_sec"); if (!rs.wasNull()) s.setEstimatedDelaySec(delay);
        return s;
    };

    public RouteRecord save(RouteRecord route) {
        String sql = """
            INSERT INTO route_records
                (request_id, route_label, origin_lat, origin_lng, dest_lat, dest_lng,
                 distance_meters, base_duration_sec, traffic_duration_sec, encoded_polyline,
                 google_traffic_state, camera_traffic_score, crowd_traffic_score,
                 fused_traffic_score, final_route_score, confidence, is_recommended,
                 sources_used, recommendation_reason)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, route.getRequestId());
            ps.setString(2, route.getRouteLabel());
            ps.setDouble(3, route.getOriginLat()); ps.setDouble(4, route.getOriginLng());
            ps.setDouble(5, route.getDestLat());   ps.setDouble(6, route.getDestLng());
            if (route.getDistanceMeters() != null) ps.setInt(7, route.getDistanceMeters()); else ps.setNull(7, Types.INTEGER);
            if (route.getBaseDurationSec() != null) ps.setInt(8, route.getBaseDurationSec()); else ps.setNull(8, Types.INTEGER);
            if (route.getTrafficDurationSec() != null) ps.setInt(9, route.getTrafficDurationSec()); else ps.setNull(9, Types.INTEGER);
            ps.setString(10, route.getEncodedPolyline());
            ps.setString(11, route.getGoogleTrafficState());
            if (route.getCameraTrafficScore() != null) ps.setDouble(12, route.getCameraTrafficScore()); else ps.setNull(12, Types.DOUBLE);
            if (route.getCrowdTrafficScore() != null) ps.setDouble(13, route.getCrowdTrafficScore()); else ps.setNull(13, Types.DOUBLE);
            if (route.getFusedTrafficScore() != null) ps.setDouble(14, route.getFusedTrafficScore()); else ps.setNull(14, Types.DOUBLE);
            if (route.getFinalRouteScore() != null) ps.setDouble(15, route.getFinalRouteScore()); else ps.setNull(15, Types.DOUBLE);
            ps.setString(16, route.getConfidence() != null ? route.getConfidence().name() : "MEDIUM");
            ps.setBoolean(17, route.isRecommended());
            ps.setString(18, route.getSourcesUsed());
            ps.setString(19, route.getRecommendationReason());
            return ps;
        }, kh);
        route.setId(kh.getKey().longValue());
        return route;
    }

    public Optional<RouteRecord> findById(Long id) {
        List<RouteRecord> r = jdbc.query("SELECT * FROM route_records WHERE id = ?", ROUTE_MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<RouteRecord> findByRequestId(Long requestId) {
        return jdbc.query("SELECT * FROM route_records WHERE request_id = ? ORDER BY final_route_score ASC",
                ROUTE_MAPPER, requestId);
    }

    public Optional<RouteRecord> findRecommendedByRequestId(Long requestId) {
        List<RouteRecord> r = jdbc.query(
                "SELECT * FROM route_records WHERE request_id = ? AND is_recommended = TRUE LIMIT 1",
                ROUTE_MAPPER, requestId);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public void clearRecommended(Long requestId) {
        jdbc.update("UPDATE route_records SET is_recommended = FALSE WHERE request_id = ?", requestId);
    }

    public void markRecommended(Long routeId) {
        jdbc.update("UPDATE route_records SET is_recommended = TRUE WHERE id = ?", routeId);
    }

    public void updateScores(RouteRecord route) {
        jdbc.update("""
            UPDATE route_records SET camera_traffic_score=?, crowd_traffic_score=?,
            fused_traffic_score=?, final_route_score=?, confidence=?, is_recommended=?,
            sources_used=?, recommendation_reason=? WHERE id=?
            """,
            route.getCameraTrafficScore(), route.getCrowdTrafficScore(),
            route.getFusedTrafficScore(), route.getFinalRouteScore(),
            route.getConfidence() != null ? route.getConfidence().name() : "MEDIUM",
            route.isRecommended(), route.getSourcesUsed(), route.getRecommendationReason(),
            route.getId());
    }

    // ── Segments ─────────────────────────────────────────────────────────────

    public RouteSegment saveSegment(RouteSegment seg) {
        String sql = """
            INSERT INTO route_segments
                (route_id, segment_index, start_lat, start_lng, end_lat, end_lng,
                 road_name, distance_meters, duration_sec, camera_id,
                 google_traffic_state, crowd_traffic_state, fused_traffic_state, estimated_delay_sec)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, seg.getRouteId()); ps.setInt(2, seg.getSegmentIndex());
            ps.setDouble(3, seg.getStartLat()); ps.setDouble(4, seg.getStartLng());
            ps.setDouble(5, seg.getEndLat());   ps.setDouble(6, seg.getEndLng());
            ps.setString(7, seg.getRoadName());
            if (seg.getDistanceMeters() != null) ps.setInt(8, seg.getDistanceMeters()); else ps.setNull(8, Types.INTEGER);
            if (seg.getDurationSec() != null) ps.setInt(9, seg.getDurationSec()); else ps.setNull(9, Types.INTEGER);
            if (seg.getCameraId() != null) ps.setLong(10, seg.getCameraId()); else ps.setNull(10, Types.BIGINT);
            ps.setString(11, seg.getGoogleTrafficState()); ps.setString(12, seg.getCrowdTrafficState());
            ps.setString(13, seg.getFusedTrafficState());
            if (seg.getEstimatedDelaySec() != null) ps.setInt(14, seg.getEstimatedDelaySec()); else ps.setNull(14, Types.INTEGER);
            return ps;
        }, kh);
        seg.setId(kh.getKey().longValue());
        return seg;
    }

    public List<RouteSegment> findSegmentsByRouteId(Long routeId) {
        return jdbc.query("SELECT * FROM route_segments WHERE route_id = ? ORDER BY segment_index ASC",
                SEG_MAPPER, routeId);
    }

}
