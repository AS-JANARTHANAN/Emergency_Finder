package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.VehicleAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class AssignmentRepository {

    private final JdbcTemplate jdbc;
    public AssignmentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<VehicleAssignment> MAPPER = (rs, rowNum) -> {
        VehicleAssignment a = new VehicleAssignment();
        a.setId(rs.getLong("id"));
        a.setRequestId(rs.getLong("request_id"));
        a.setWorkerId(rs.getLong("worker_id"));
        a.setVehicleId(rs.getLong("vehicle_id"));
        a.setStatus(VehicleAssignment.AssignmentStatus.valueOf(rs.getString("status")));
        Timestamp ts;
        ts = rs.getTimestamp("assigned_at");  if (ts != null) a.setAssignedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("accepted_at");  if (ts != null) a.setAcceptedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("declined_at");  if (ts != null) a.setDeclinedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("completed_at"); if (ts != null) a.setCompletedAt(ts.toLocalDateTime());
        a.setDeclineReason(rs.getString("decline_reason"));
        return a;
    };

    public VehicleAssignment save(VehicleAssignment assignment) {
        String sql = """
            INSERT INTO vehicle_assignments (request_id, worker_id, vehicle_id, status)
            VALUES (?, ?, ?, ?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, assignment.getRequestId());
            ps.setLong(2, assignment.getWorkerId());
            ps.setLong(3, assignment.getVehicleId());
            ps.setString(4, assignment.getStatus().name());
            return ps;
        }, kh);
        assignment.setId(kh.getKey().longValue());
        return assignment;
    }

    public Optional<VehicleAssignment> findById(Long id) {
        List<VehicleAssignment> r = jdbc.query("SELECT * FROM vehicle_assignments WHERE id = ?", MAPPER, id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /** Find active (PENDING/ACCEPTED) assignment for a request. */
    public Optional<VehicleAssignment> findActiveByRequestId(Long requestId) {
        List<VehicleAssignment> r = jdbc.query("""
            SELECT * FROM vehicle_assignments
            WHERE request_id = ? AND status IN ('PENDING','ACCEPTED')
            ORDER BY assigned_at DESC LIMIT 1
            """, MAPPER, requestId);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /** Check if a worker has any current active assignment with an active request. */
    public boolean hasActiveAssignment(Long workerId) {
        Integer c = jdbc.queryForObject("""
            SELECT COUNT(*) FROM vehicle_assignments va
            JOIN emergency_requests er ON va.request_id = er.id
            WHERE va.worker_id = ? AND va.status IN ('PENDING','ACCEPTED')
              AND er.status IN ('ASSIGNED','ACCEPTED','RESPONDING','ARRIVED')
            """, Integer.class, workerId);
        return c != null && c > 0;
    }

    public void clearStalePendingAssignments(Long workerId) {
        jdbc.update("""
            UPDATE vehicle_assignments SET status = 'DECLINED', decline_reason = 'Stale / Worker reset'
            WHERE worker_id = ? AND status = 'PENDING'
            """, workerId);
    }

    public void accept(Long assignmentId) {
        jdbc.update("""
            UPDATE vehicle_assignments SET status = 'ACCEPTED', accepted_at = NOW() WHERE id = ?
            """, assignmentId);
    }

    public void decline(Long assignmentId, String reason) {
        jdbc.update("""
            UPDATE vehicle_assignments SET status = 'DECLINED', declined_at = NOW(), decline_reason = ? WHERE id = ?
            """, reason, assignmentId);
    }

    public void complete(Long assignmentId) {
        jdbc.update("""
            UPDATE vehicle_assignments SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?
            """, assignmentId);
    }

    public List<VehicleAssignment> findByWorkerId(Long workerId) {
        return jdbc.query("SELECT * FROM vehicle_assignments WHERE worker_id = ? ORDER BY assigned_at DESC",
                MAPPER, workerId);
    }
}
