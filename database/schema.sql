-- ============================================================
-- EMERGENCY ROUTE FINDER — DATABASE SCHEMA
-- Database: emergency_route_finder
-- MySQL 8.x
-- ============================================================

CREATE DATABASE IF NOT EXISTS emergency_route_finder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE emergency_route_finder;

-- ============================================================
-- TABLE 1: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(150)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    mobile          VARCHAR(20)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    location_lat    DOUBLE          NULL,
    location_lng    DOUBLE          NULL,
    location_updated_at DATETIME    NULL,
    allow_crowd_traffic BOOLEAN     NOT NULL DEFAULT FALSE,
    fcm_token       VARCHAR(512)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_email CHECK (email REGEXP '^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$')
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 2: emergency_workers
-- ============================================================
CREATE TABLE IF NOT EXISTS emergency_workers (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(150)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    mobile          VARCHAR(20)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    worker_type     ENUM('AMBULANCE_DRIVER','FIRE_ENGINE_DRIVER') NOT NULL,
    availability    ENUM('AVAILABLE','BUSY','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
    location_lat    DOUBLE          NULL,
    location_lng    DOUBLE          NULL,
    location_updated_at DATETIME    NULL,
    license_id      VARCHAR(100)    NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    fcm_token       VARCHAR(512)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 3: vehicles
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicles (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    vehicle_number  VARCHAR(50)     NOT NULL UNIQUE,
    vehicle_type    ENUM('AMBULANCE','FIRE_ENGINE') NOT NULL,
    worker_id       BIGINT          NOT NULL,
    current_lat     DOUBLE          NULL,
    current_lng     DOUBLE          NULL,
    availability    ENUM('AVAILABLE','ASSIGNED','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    active_request_id BIGINT        NULL,
    last_location_update DATETIME   NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_worker FOREIGN KEY (worker_id) REFERENCES emergency_workers(id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 4: emergency_requests
-- ============================================================
CREATE TABLE IF NOT EXISTS emergency_requests (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT      NOT NULL,
    emergency_type      ENUM('MEDICAL','ACCIDENT','FIRE','OTHER') NOT NULL,
    description         TEXT        NULL,
    priority            ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'HIGH',
    source_lat          DOUBLE      NOT NULL,
    source_lng          DOUBLE      NOT NULL,
    dest_lat            DOUBLE      NULL,
    dest_lng            DOUBLE      NULL,
    preferred_vehicle   ENUM('AMBULANCE','FIRE_ENGINE','ANY') NOT NULL DEFAULT 'ANY',
    status              ENUM('PENDING','SEARCHING','ASSIGNED','ACCEPTED','RESPONDING','ARRIVED','COMPLETED','CANCELLED')
                        NOT NULL DEFAULT 'PENDING',
    assigned_worker_id  BIGINT      NULL,
    assigned_vehicle_id BIGINT      NULL,
    recommended_route_id BIGINT     NULL,
    selected_hospital_id BIGINT     NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at         DATETIME    NULL,
    responded_at        DATETIME    NULL,
    arrived_at          DATETIME    NULL,
    completed_at        DATETIME    NULL,
    cancelled_at        DATETIME    NULL,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_user    FOREIGN KEY (user_id)            REFERENCES users(id),
    CONSTRAINT fk_request_worker  FOREIGN KEY (assigned_worker_id) REFERENCES emergency_workers(id),
    CONSTRAINT fk_request_vehicle FOREIGN KEY (assigned_vehicle_id) REFERENCES vehicles(id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 5: vehicle_assignments
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicle_assignments (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    request_id      BIGINT          NOT NULL,
    worker_id       BIGINT          NOT NULL,
    vehicle_id      BIGINT          NOT NULL,
    status          ENUM('PENDING','ACCEPTED','DECLINED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    assigned_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at     DATETIME        NULL,
    declined_at     DATETIME        NULL,
    completed_at    DATETIME        NULL,
    decline_reason  VARCHAR(255)    NULL,
    CONSTRAINT fk_assign_request FOREIGN KEY (request_id) REFERENCES emergency_requests(id),
    CONSTRAINT fk_assign_worker  FOREIGN KEY (worker_id)  REFERENCES emergency_workers(id),
    CONSTRAINT fk_assign_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 6: hospitals
-- ============================================================
CREATE TABLE IF NOT EXISTS hospitals (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    address             TEXT        NULL,
    contact_number      VARCHAR(30) NULL,
    lat                 DOUBLE      NOT NULL,
    lng                 DOUBLE      NOT NULL,
    emergency_available BOOLEAN     NOT NULL DEFAULT TRUE,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 7: route_records
-- ============================================================
CREATE TABLE IF NOT EXISTS route_records (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    request_id          BIGINT      NOT NULL,
    route_label         VARCHAR(20) NOT NULL DEFAULT 'Route A',
    origin_lat          DOUBLE      NOT NULL,
    origin_lng          DOUBLE      NOT NULL,
    dest_lat            DOUBLE      NOT NULL,
    dest_lng            DOUBLE      NOT NULL,
    distance_meters     INT         NULL,
    base_duration_sec   INT         NULL,
    traffic_duration_sec INT        NULL,
    encoded_polyline    MEDIUMTEXT  NULL,
    google_traffic_state VARCHAR(50) NULL,
    camera_traffic_score DOUBLE     NULL,
    crowd_traffic_score  DOUBLE     NULL,
    fused_traffic_score  DOUBLE     NULL,
    final_route_score   DOUBLE      NULL,
    confidence          ENUM('HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
    is_recommended      BOOLEAN     NOT NULL DEFAULT FALSE,
    sources_used        VARCHAR(255) NULL,
    recommendation_reason TEXT      NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_request FOREIGN KEY (request_id) REFERENCES emergency_requests(id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 8: route_segments
-- ============================================================
CREATE TABLE IF NOT EXISTS route_segments (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    route_id            BIGINT      NOT NULL,
    segment_index       INT         NOT NULL DEFAULT 0,
    start_lat           DOUBLE      NOT NULL,
    start_lng           DOUBLE      NOT NULL,
    end_lat             DOUBLE      NOT NULL,
    end_lng             DOUBLE      NOT NULL,
    road_name           VARCHAR(200) NULL,
    distance_meters     INT         NULL,
    duration_sec        INT         NULL,
    camera_id           BIGINT      NULL,
    google_traffic_state VARCHAR(50) NULL,
    crowd_traffic_state  VARCHAR(50) NULL,
    fused_traffic_state  VARCHAR(50) NULL,
    estimated_delay_sec INT         NULL,
    CONSTRAINT fk_segment_route FOREIGN KEY (route_id) REFERENCES route_records(id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 9: traffic_cameras
-- ============================================================
CREATE TABLE IF NOT EXISTS traffic_cameras (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    camera_name     VARCHAR(100)    NOT NULL,
    road_name       VARCHAR(200)    NOT NULL,
    lat             DOUBLE          NOT NULL,
    lng             DOUBLE          NOT NULL,
    route_segment_id BIGINT         NULL,
    video_file_path VARCHAR(500)    NOT NULL,
    traffic_level   ENUM('LOW','MODERATE','HIGH','SEVERE') NOT NULL DEFAULT 'MODERATE',
    traffic_score   DOUBLE          NOT NULL DEFAULT 0.5,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_updated    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 10: crowd_location_points
-- ============================================================
CREATE TABLE IF NOT EXISTS crowd_location_points (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NULL,
    segment_id      BIGINT          NULL,
    lat             DOUBLE          NOT NULL,
    lng             DOUBLE          NOT NULL,
    speed_kmh       DOUBLE          NULL,
    direction_deg   DOUBLE          NULL,
    recorded_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_crowd_segment (segment_id),
    INDEX idx_crowd_recorded (recorded_at)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 11: traffic_observations
-- ============================================================
CREATE TABLE IF NOT EXISTS traffic_observations (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    route_id        BIGINT          NULL,
    segment_id      BIGINT          NULL,
    traffic_source  ENUM('CAMERA','GOOGLE','CROWD') NOT NULL,
    traffic_level   ENUM('LOW','MODERATE','HIGH','SEVERE') NOT NULL,
    confidence      DOUBLE          NOT NULL DEFAULT 0.5,
    delay_factor    DOUBLE          NOT NULL DEFAULT 1.0,
    raw_data        JSON            NULL,
    observed_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_obs_segment (segment_id),
    INDEX idx_obs_source  (traffic_source),
    INDEX idx_obs_time    (observed_at)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 12: notifications
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    recipient_type  ENUM('USER','WORKER') NOT NULL,
    recipient_id    BIGINT          NOT NULL,
    request_id      BIGINT          NULL,
    title           VARCHAR(255)    NOT NULL,
    body            TEXT            NOT NULL,
    notification_type VARCHAR(100)  NOT NULL,
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    fcm_message_id  VARCHAR(255)    NULL,
    sent_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         DATETIME        NULL,
    INDEX idx_notif_recipient (recipient_type, recipient_id),
    INDEX idx_notif_request   (request_id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 13: response_status_history
-- ============================================================
CREATE TABLE IF NOT EXISTS response_status_history (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    request_id      BIGINT          NOT NULL,
    old_status      VARCHAR(50)     NULL,
    new_status      VARCHAR(50)     NOT NULL,
    changed_by_type ENUM('USER','WORKER','SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    changed_by_id   BIGINT          NULL,
    note            VARCHAR(500)    NULL,
    changed_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_request FOREIGN KEY (request_id) REFERENCES emergency_requests(id),
    INDEX idx_history_request (request_id),
    INDEX idx_history_time    (changed_at)
) ENGINE=InnoDB;

-- ============================================================
-- ADD DEFERRED FK (circular refs avoided by doing them here)
-- ============================================================
ALTER TABLE emergency_requests
    ADD CONSTRAINT fk_request_hospital
    FOREIGN KEY (selected_hospital_id) REFERENCES hospitals(id);

ALTER TABLE emergency_requests
    ADD CONSTRAINT fk_request_route
    FOREIGN KEY (recommended_route_id) REFERENCES route_records(id);

ALTER TABLE route_segments
    ADD CONSTRAINT fk_segment_camera
    FOREIGN KEY (camera_id) REFERENCES traffic_cameras(id);

ALTER TABLE vehicles
    ADD CONSTRAINT fk_vehicle_active_request
    FOREIGN KEY (active_request_id) REFERENCES emergency_requests(id);
