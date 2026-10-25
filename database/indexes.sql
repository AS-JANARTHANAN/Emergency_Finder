-- ============================================================
-- EMERGENCY ROUTE FINDER — INDEXES
-- Run AFTER schema.sql and demo_data.sql
-- ============================================================

USE emergency_route_finder;

-- Users
CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_mobile   ON users(mobile);

-- Workers
CREATE INDEX idx_workers_email  ON emergency_workers(email);
CREATE INDEX idx_workers_avail  ON emergency_workers(availability);
CREATE INDEX idx_workers_type   ON emergency_workers(worker_type);

-- Vehicles
CREATE INDEX idx_vehicles_type  ON vehicles(vehicle_type);
CREATE INDEX idx_vehicles_avail ON vehicles(availability);
CREATE INDEX idx_vehicles_worker ON vehicles(worker_id);

-- Emergency requests
CREATE INDEX idx_requests_user   ON emergency_requests(user_id);
CREATE INDEX idx_requests_status ON emergency_requests(status);
CREATE INDEX idx_requests_worker ON emergency_requests(assigned_worker_id);
CREATE INDEX idx_requests_created ON emergency_requests(created_at);

-- Assignments
CREATE INDEX idx_assign_request ON vehicle_assignments(request_id);
CREATE INDEX idx_assign_worker  ON vehicle_assignments(worker_id);
CREATE INDEX idx_assign_status  ON vehicle_assignments(status);

-- Route records
CREATE INDEX idx_route_request ON route_records(request_id);
CREATE INDEX idx_route_recommended ON route_records(is_recommended);

-- Route segments
CREATE INDEX idx_seg_route ON route_segments(route_id);

-- Traffic cameras
CREATE INDEX idx_cam_active ON traffic_cameras(is_active);

-- Notifications
CREATE INDEX idx_notif_read ON notifications(is_read);
