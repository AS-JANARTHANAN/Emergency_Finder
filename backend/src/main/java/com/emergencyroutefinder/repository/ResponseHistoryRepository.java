package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.ResponseStatusHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class ResponseHistoryRepository {

    private final JdbcTemplate jdbc;
    public ResponseHistoryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<ResponseStatusHistory> MAPPER = (rs, rowNum) -> {
        ResponseStatusHistory h = new ResponseStatusHistory();
        h.setId(rs.getLong("id"));
        h.setRequestId(rs.getLong("request_id"));
        h.setOldStatus(rs.getString("old_status"));
        h.setNewStatus(rs.getString("new_status"));
        h.setChangedByType(ResponseStatusHistory.ChangedByType.valueOf(rs.getString("changed_by_type")));
        long cId = rs.getLong("changed_by_id"); if (!rs.wasNull()) h.setChangedById(cId);
        h.setNote(rs.getString("note"));
        Timestamp ts = rs.getTimestamp("changed_at"); if (ts != null) h.setChangedAt(ts.toLocalDateTime());
        return h;
    };

    public ResponseStatusHistory save(ResponseStatusHistory history) {
        String sql = """
            INSERT INTO response_status_history
                (request_id, old_status, new_status, changed_by_type, changed_by_id, note)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, history.getRequestId());
            ps.setString(2, history.getOldStatus());
            ps.setString(3, history.getNewStatus());
            ps.setString(4, history.getChangedByType().name());
            if (history.getChangedById() != null) ps.setLong(5, history.getChangedById()); else ps.setNull(5, Types.BIGINT);
            ps.setString(6, history.getNote());
            return ps;
        }, kh);
        history.setId(kh.getKey().longValue());
        return history;
    }

    public List<ResponseStatusHistory> findByRequestId(Long requestId) {
        return jdbc.query("""
            SELECT * FROM response_status_history WHERE request_id = ? ORDER BY changed_at ASC
            """, MAPPER, requestId);
    }
}
