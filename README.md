# Emergency Route Finder

A full-stack emergency response system built as an academic project. The system connects people in distress with the nearest available emergency vehicle in real time — ambulances, fire engines — and guides them along the least-congested route using a three-source traffic fusion engine.

---

## What this project does

When a user submits an SOS request, the backend:

1. Finds all available workers within range using the Haversine formula (async thread)
2. Sends a push notification to the nearest worker via Firebase Cloud Messaging
3. Computes the optimal route by fusing traffic data from three sources — Google Routes API, simulated camera feeds, and crowd GPS data
4. Streams live worker location updates to the user's map over WebSocket (STOMP)
5. Re-evaluates routes every 5 minutes via a scheduled background task

The entire request follows a strict status lifecycle: `PENDING ? SEARCHING ? ASSIGNED ? ACCEPTED ? RESPONDING ? ARRIVED ? COMPLETED`

---

## My contribution (Backend)

I designed and built the backend in full — this was my primary responsibility in the team.

**Core backend work:**
- Spring Boot 3.2 REST API with JWT authentication (login, register, role-based access)
- WebSocket server using STOMP protocol for real-time location broadcasting
- Traffic Fusion Engine — aggregates Google Routes API, traffic camera scores, and crowd GPS into a single weighted score (50/25/25 split with confidence tagging)
- Vehicle allocation logic — Haversine-based nearest-worker search running asynchronously in a dedicated thread pool (`@Async`)
- Route scoring and re-evaluation every 5 minutes via `@Scheduled`
- All database access written in plain JDBC with `PreparedStatement` — no ORM abstraction
- JWT security filter chain integrated with Spring Security
- Firebase Admin SDK integration for server-side FCM push notifications
- Deployed backend to Railway using Docker

**Packages I own:**
```
controller/     — 7 REST controllers (auth, emergency, assignment, worker, user, route, hospital)
service/        — auth, emergency request lifecycle, vehicle allocation, notification, WebSocket push
traffic/        — TrafficFusionService, GoogleTrafficService, TrafficCameraService, CrowdTrafficService
route/          — RouteService, RouteScoringService
security/       — JWT token provider, auth filter, UserDetails integration
repository/     — 13 JDBC repositories (PreparedStatement, no ORM)
config/         — Async thread pools, WebSocket, Firebase, Security config
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | MySQL 8.0 (raw JDBC / PreparedStatement) |
| Real-time | WebSocket — STOMP over SockJS |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Routing | Google Routes API |
| Auth | JWT (JSON Web Token) + Spring Security |
| Async | `@Async` thread pools + `@Scheduled` |
| Build | Apache Maven |
| Deployment | Docker + Railway |
| Mobile (teammate) | Android (Java), Retrofit2, OkHttp, OSMDroid |

---

## Architecture overview

```
Android client  (Retrofit REST + STOMP WebSocket)
        |
Spring Boot 3.2 backend
        |
   +----+---------------------+
   |                          |
REST controllers    WebSocket broker
   |                          |
Service layer       Location broadcaster
   |
 +-+---------------------------+
 |                             |
Traffic Fusion Engine    JDBC Repositories
 (Google + Camera +       (MySQL 8.0)
  Crowd GPS weighted)
```

---

## Running locally

### 1. Database

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p emergency_route_finder < database/demo_data.sql
```

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/emergency_route_finder?useSSL=false&serverTimezone=UTC
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### 2. API keys

**Google Maps** — enable Maps SDK for Android, Routes API, Directions API in Google Cloud Console, then:

```properties
# application.properties
google.maps.api-key=YOUR_KEY_HERE
```

Without a key the backend returns simulated traffic data. The app stays fully demonstrable.

**Firebase** (optional) — download Service Account JSON from Firebase console, rename to `firebase-service-account.json`, place in `backend/src/main/resources/`. To skip FCM entirely:

```properties
firebase.enabled=false
```

The app falls back to 5-second HTTP polling.

### 3. Start backend

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

Health check:
```bash
curl http://localhost:8080/actuator/health
```

### 4. Android app

Open the `android/` folder in Android Studio, wait for Gradle sync, then run on an emulator (API 26+) or a physical device.

- **Emulator**: backend URL is pre-set to `10.0.2.2:8080`
- **Physical device**: update `BACKEND_URL` in `app/build.gradle` to your local IP (`ipconfig` on Windows)

---

## Demo accounts

| Role | Email | Password |
|---|---|---|
| User | alice@example.com | password123 |
| User | bob@example.com | password123 |
| Ambulance driver | driver1@example.com | password123 |
| Fire engine driver | driver2@example.com | password123 |

---

## End-to-end test flow

1. Login as `alice@example.com` on Device 1
2. Tap SOS ? fill emergency form ? submit
3. Backend searches nearby workers async (Haversine) and sends FCM alert
4. Login as `driver1@example.com` on Device 2 ? set status to Available
5. Worker receives push notification with 30-second countdown
6. Worker taps Accept
7. Device 1 sees worker location updating live on map (WebSocket)
8. Worker progresses: Responding ? Arrived ? Complete
9. Both sides see status changes instantly

---

## Traffic fusion details

The `TrafficFusionService` pulls from three sources and blends them into a single congestion score:

| Source | Default weight | Fallback |
|---|---|---|
| Google Routes API | 50% | Simulated data |
| Traffic camera scores | 25% | Excluded; weight redistributed |
| Crowd GPS data | 25% | Excluded if no opted-in users |

**Confidence levels** — HIGH when all three sources respond, MEDIUM for two, LOW for one. Routes with a HIGH-confidence score are preferred by the route-scoring algorithm. The entire fusion result is cached and refreshed every 5 minutes via `@Scheduled(fixedRate = 300_000)`.

---

## Multithreading

| Operation | Mechanism | Thread pool |
|---|---|---|
| Nearest-worker search | `@Async("taskExecutor")` | ERF-Task-N |
| FCM dispatch | `@Async("taskExecutor")` | ERF-Task-N |
| Route computation | `@Async("taskExecutor")` | ERF-Task-N |
| Location update processing | `@Async("taskExecutor")` | ERF-Task-N |
| Route re-evaluation | `@Scheduled(fixedRate=300000)` | Scheduler thread |
| Traffic data fetch | `@Async("trafficExecutor")` | ERF-Traffic-N |
| Android GPS (teammate) | `LocationForegroundService` | OS service |

---

## Common issues

**"Connection refused" on Android**
- Emulator: use `10.0.2.2:8080`, not `localhost`
- Physical device: use your machine's local IP from `ipconfig`

**MySQL fails to start**
- Confirm MySQL is running and the database `emergency_route_finder` exists (created by `schema.sql`)
- Double-check credentials in `application.properties`

**FCM not delivering**
- Set `firebase.enabled=false` to fall back to polling — app works fine without it

---

## Project layout

```
ch/
+-- android/                    Android project (teammate)
¦   +-- app/src/main/java/com/emergencyroutefinder/
¦       +-- ui/                 Activities for user and worker flows
¦       +-- network/            Retrofit client + API interface (35+ endpoints)
¦       +-- websocket/          STOMP WebSocket client
¦       +-- service/            Location foreground service
¦       +-- notification/       FCM message handler
+-- backend/                    Spring Boot project (my responsibility)
¦   +-- src/main/java/com/emergencyroutefinder/
¦       +-- controller/         7 REST controllers
¦       +-- service/            Business logic layer
¦       +-- traffic/            Three-source traffic fusion engine
¦       +-- route/              Route computation and scoring
¦       +-- repository/         13 JDBC repositories
¦       +-- security/           JWT filter + Spring Security config
¦       +-- config/             Async, WebSocket, Firebase, Security setup
+-- database/
    +-- schema.sql              Full MySQL DDL with indexes
    +-- demo_data.sql           4 users, 5 hospitals, 3 cameras
```
