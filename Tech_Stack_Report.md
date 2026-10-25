# Tech Stack — Emergency Route Finder

Quick reference for the technologies used across the three layers of this project.

---

## Android (teammate's responsibility)

The mobile app is a native Android application written in Java, targeting API 26 and above.

- **UI**: XML layouts with Material Design, custom animations
- **Maps**: OSMDroid (OpenStreetMap) for offline-capable map rendering
- **Networking**: Retrofit2 + OkHttp3 for REST calls to the backend
- **Real-time**: STOMP client over WebSocket for live location updates
- **Location**: `FusedLocationProviderClient` inside an Android Foreground Service — keeps tracking even when the app is in the background
- **Session**: Encrypted `SharedPreferences` storing JWT token and user role

---

## Backend (my responsibility)

Built with Spring Boot 3.2. Every database operation goes through raw JDBC (`PreparedStatement`) — no ORM.

- **REST API**: Spring Web — 7 controllers, 35+ endpoints covering auth, emergency lifecycle, worker management, routing, and hospitals
- **Auth**: JWT issued on login, validated by a custom Spring Security filter on every request
- **Real-time**: WebSocket server using STOMP — broadcasts worker location updates to subscribed users
- **Traffic Fusion**: Custom engine that blends three data sources (Google Routes API 50%, camera feeds 25%, crowd GPS 25%) into a single congestion score with confidence rating
- **Async**: `@Async` thread pools (`taskExecutor` for core operations, `trafficExecutor` for traffic fetching) + `@Scheduled` for periodic route re-evaluation every 5 minutes
- **Push**: Firebase Admin SDK — sends FCM notifications server-side when a worker is assigned
- **Database**: 13 JDBC repositories, all using `PreparedStatement` directly against MySQL 8.0
- **Deployment**: Dockerised and deployed to Railway

---

## Database

MySQL 8.0. Schema in `database/schema.sql`, demo data in `database/demo_data.sql`.

4 demo users, 5 hospitals, 3 simulated traffic cameras.

---

## Security

- JWT for all backend REST endpoints
- Spring Security filter chain with role-based access (`USER` vs `WORKER`)
- Android `FLAG_SECURE` managed dynamically — prevents screenshots during sensitive flows

---

## Key dependencies

| Dependency | Used for |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-websocket` | STOMP WebSocket |
| `spring-boot-starter-security` | Auth filter chain |
| `jjwt` | JWT sign / validate |
| `mysql-connector-j` | Raw JDBC |
| `firebase-admin` | Server-side FCM |
| `spring-boot-starter-actuator` | Health endpoint |
| `retrofit2` (Android) | REST client |
| `osmdroid` (Android) | Maps |
