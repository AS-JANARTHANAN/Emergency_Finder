package com.emergencyroutefinder.repository;

import com.emergencyroutefinder.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbc;
    public NotificationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Notification> MAPPER = (rs, rowNum) -> {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setRecipientType(Notification.RecipientType.valueOf(rs.getString("recipient_type")));
        n.setRecipientId(rs.getLong("recipient_id"));
        long rId = rs.getLong("request_id"); if (!rs.wasNull()) n.setRequestId(rId);
        n.setTitle(rs.getString("title"));
        n.setBody(rs.getString("body"));
        n.setNotificationType(rs.getString("notification_type"));
        n.setRead(rs.getBoolean("is_read"));
        n.setFcmMessageId(rs.getString("fcm_message_id"));
        Timestamp ts = rs.getTimestamp("sent_at"); if (ts != null) n.setSentAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("read_at"); if (ts != null) n.setReadAt(ts.toLocalDateTime());
        return n;
    };

    public Notification save(Notification notification) {
        String sql = """
            INSERT INTO notifications
                (recipient_type, recipient_id, request_id, title, body, notification_type, fcm_message_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, notification.getRecipientType().name());
            ps.setLong(2, notification.getRecipientId());
            if (notification.getRequestId() != null) ps.setLong(3, notification.getRequestId()); else ps.setNull(3, Types.BIGINT);
            ps.setString(4, notification.getTitle());
            ps.setString(5, notification.getBody());
            ps.setString(6, notification.getNotificationType());
            ps.setString(7, notification.getFcmMessageId());
            return ps;
        }, kh);
        notification.setId(kh.getKey().longValue());
        return notification;
    }

    public List<Notification> findByRecipient(String recipientType, Long recipientId) {
        return jdbc.query("""
            SELECT * FROM notifications WHERE recipient_type = ? AND recipient_id = ?
            ORDER BY sent_at DESC LIMIT 50
            """, MAPPER, recipientType, recipientId);
    }

    public int countUnread(String recipientType, Long recipientId) {
        Integer c = jdbc.queryForObject("""
            SELECT COUNT(*) FROM notifications WHERE recipient_type = ? AND recipient_id = ? AND is_read = FALSE
            """, Integer.class, recipientType, recipientId);
        return c != null ? c : 0;
    }

    public void markRead(Long notificationId) {
        jdbc.update("UPDATE notifications SET is_read = TRUE, read_at = NOW() WHERE id = ?", notificationId);
    }
}
