-- ============================================================
-- EMERGENCY ROUTE FINDER — DEMO DATA
-- Run AFTER schema.sql
-- ============================================================

USE emergency_route_finder;

-- ============================================================
-- DEMO USERS (passwords: "Demo@1234" -> BCrypt)
-- ============================================================
INSERT INTO users (full_name, email, mobile, password_hash, role, is_active, location_lat, location_lng, allow_crowd_traffic) VALUES
('Alice Johnson',  'alice@demo.com',  '+1-555-0101', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'USER', TRUE, 12.9716, 77.5946, TRUE),
('Bob Williams',   'bob@demo.com',    '+1-555-0102', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'USER', TRUE, 12.9352, 77.6245, FALSE),
('Carol Davis',    'carol@demo.com',  '+1-555-0103', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'USER', TRUE, 12.9082, 77.6476, TRUE);

-- ============================================================
-- DEMO WORKERS (passwords: "Demo@1234" -> BCrypt)
-- ============================================================
INSERT INTO emergency_workers (full_name, email, mobile, password_hash, worker_type, availability, location_lat, location_lng, license_id) VALUES
('David Kumar',   'david@demo.com',   '+1-555-0201', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'AMBULANCE_DRIVER',   'AVAILABLE', 12.9755, 77.5890, 'AMB-LIC-001'),
('Emma Singh',    'emma@demo.com',    '+1-555-0202', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'AMBULANCE_DRIVER',   'AVAILABLE', 12.9400, 77.6100, 'AMB-LIC-002'),
('Frank Lopez',   'frank@demo.com',   '+1-555-0203', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'FIRE_ENGINE_DRIVER', 'AVAILABLE', 12.9600, 77.5780, 'FIR-LIC-001'),
('Grace Chen',    'grace@demo.com',   '+1-555-0204', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'FIRE_ENGINE_DRIVER', 'OFFLINE',   12.9300, 77.6300, 'FIR-LIC-002');

-- ============================================================
-- DEMO VEHICLES
-- ============================================================
INSERT INTO vehicles (vehicle_number, vehicle_type, worker_id, current_lat, current_lng, availability) VALUES
('AMB-KA-01-1001', 'AMBULANCE',   1, 12.9755, 77.5890, 'AVAILABLE'),
('AMB-KA-01-1002', 'AMBULANCE',   2, 12.9400, 77.6100, 'AVAILABLE'),
('FIRE-KA-01-201', 'FIRE_ENGINE', 3, 12.9600, 77.5780, 'AVAILABLE'),
('FIRE-KA-01-202', 'FIRE_ENGINE', 4, 12.9300, 77.6300, 'AVAILABLE');

-- ============================================================
-- DEMO HOSPITALS (Bengaluru area coordinates)
-- ============================================================
INSERT INTO hospitals (name, address, contact_number, lat, lng, emergency_available) VALUES
('City General Hospital',       '1 Main Road, Central Bengaluru',       '+91-80-2222-1111', 12.9716, 77.5946, TRUE),
('Apollo Emergency Centre',     '22 Ring Road, Bengaluru',              '+91-80-2333-4444', 12.9592, 77.6481, TRUE),
('St Mary Medical Centre',      '5 Church Street, Bengaluru',           '+91-80-2444-5555', 12.9756, 77.6052, TRUE),
('Narayana Health City',        '258/A Bommasandra, Bengaluru',         '+91-80-7178-6789', 12.8390, 77.6756, TRUE),
('Manipal Hospital Whitefield', 'ITPL Main Road, Whitefield, Bengaluru','+91-80-2222-8888', 12.9872, 77.7473, TRUE),
('Fortis Hospital Rajajinagar', '14 Cunningham Road, Bengaluru',        '+91-80-6699-1111', 12.9878, 77.5546, TRUE);

-- ============================================================
-- DEMO TRAFFIC CAMERAS
-- ============================================================
INSERT INTO traffic_cameras (camera_name, road_name, lat, lng, video_file_path, traffic_level, traffic_score) VALUES
('CAM-001', 'MG Road Corridor',       12.9740, 77.6010, 'assets/videos/camera_a.mp4', 'HIGH',     0.80),
('CAM-002', 'Outer Ring Road Seg-1',  12.9600, 77.6200, 'assets/videos/camera_b.mp4', 'MODERATE', 0.55),
('CAM-003', 'Hosur Road Corridor',    12.9350, 77.6100, 'assets/videos/camera_c.mp4', 'LOW',      0.25),
('CAM-004', 'Airport Road Seg-1',     12.9900, 77.5900, 'assets/videos/camera_d.mp4', 'MODERATE', 0.50),
('CAM-005', 'Whitefield Main Road',   12.9800, 77.7400, 'assets/videos/camera_e.mp4', 'SEVERE',   0.90);

-- ============================================================
-- DEMO CROWD LOCATION POINTS (recent simulated movement)
-- ============================================================
INSERT INTO crowd_location_points (segment_id, lat, lng, speed_kmh, recorded_at) VALUES
(NULL, 12.9730, 77.6000, 11.5, NOW()),
(NULL, 12.9725, 77.6005, 12.0, NOW()),
(NULL, 12.9718, 77.6012, 10.8, NOW()),
(NULL, 12.9700, 77.6020,  9.5, NOW()),
(NULL, 12.9710, 77.6015, 13.2, NOW()),
(NULL, 12.9705, 77.6018, 11.0, NOW()),
(NULL, 12.9712, 77.6022, 10.5, NOW()),
(NULL, 12.9608, 77.6205, 35.2, NOW()),
(NULL, 12.9595, 77.6210, 33.8, NOW()),
(NULL, 12.9580, 77.6215, 36.5, NOW()),
(NULL, 12.9570, 77.6220, 34.0, NOW()),
(NULL, 12.9560, 77.6225, 37.1, NOW()),
(NULL, 12.9555, 77.6228, 35.9, NOW()),
(NULL, 12.9550, 77.6230, 34.5, NOW());

-- ============================================================
-- DEMO TRAFFIC OBSERVATIONS (camera source)
-- ============================================================
INSERT INTO traffic_observations (traffic_source, traffic_level, confidence, delay_factor, raw_data, observed_at) VALUES
('CAMERA', 'HIGH',     0.80, 2.0, '{"camera_id":1,"road":"MG Road"}',          NOW()),
('CAMERA', 'MODERATE', 0.75, 1.5, '{"camera_id":2,"road":"Outer Ring Road"}',   NOW()),
('CAMERA', 'LOW',      0.85, 1.1, '{"camera_id":3,"road":"Hosur Road"}',        NOW()),
('GOOGLE', 'HIGH',     0.90, 1.8, '{"source":"google_routes","eta_sec":900}',   NOW()),
('CROWD',  'HIGH',     0.65, 1.7, '{"device_count":7,"avg_speed_kmh":11.5}',    NOW()),
('CROWD',  'LOW',      0.70, 1.2, '{"device_count":6,"avg_speed_kmh":35.2}',    NOW());
