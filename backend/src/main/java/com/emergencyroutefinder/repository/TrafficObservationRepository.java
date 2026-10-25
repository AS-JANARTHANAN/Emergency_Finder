package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.TrafficObservation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class TrafficObservationRepository {

    private final JdbcTemplate jdbc;
    public TrafficObservationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<TrafficObservation> MAPPER = (rs, rowNum) -> {
        TrafficObservation t = new TrafficObservation();
        t.setId(rs.getLong("id"));
        long rId = rs.getLong("route_id");   if (!rs.wasNull()) t.setRouteId(rId);
        long sId = rs.getLong("segment_id"); if (!rs.wasNull()) t.setSegmentId(sId);
        t.setTrafficSource(TrafficObservation.TrafficSource.valueOf(rs.getString("traffic_source")));
        t.setTrafficLevel(TrafficObservation.TrafficLevel.valueOf(rs.getString("traffic_level")));
        t.setConfidence(rs.getDouble("confidence"));
        t.setDelayFactor(rs.getDouble("delay_factor"));
        t.setRawData(rs.getString("raw_data"));
        Timestamp ts = rs.getTimestamp("observed_at"); if (ts != null) t.setObservedAt(ts.toLocalDateTime());
        return t;
    };

    public TrafficObservation save(TrafficObservation obs) {
        String sql = """
            INSERT INTO traffic_observations
                (route_id, segment_id, traffic_source, traffic_level, confidence, delay_factor, raw_data)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (obs.getRouteId() != null) ps.setLong(1, obs.getRouteId()); else ps.setNull(1, Types.BIGINT);
            if (obs.getSegmentId() != null) ps.setLong(2, obs.getSegmentId()); else ps.setNull(2, Types.BIGINT);
            ps.setString(3, obs.getTrafficSource().name());
            ps.setString(4, obs.getTrafficLevel().name());
            ps.setDouble(5, obs.getConfidence());
            ps.setDouble(6, obs.getDelayFactor());
            ps.setString(7, obs.getRawData());
            return ps;
        }, kh);
        obs.setId(kh.getKey().longValue());
        return obs;
    }

    public List<TrafficObservation> findBySegmentId(Long segmentId) {
        return jdbc.query("SELECT * FROM traffic_observations WHERE segment_id = ? ORDER BY observed_at DESC LIMIT 10",
                MAPPER, segmentId);
    }
}
