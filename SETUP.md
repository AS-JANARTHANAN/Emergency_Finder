# Setup Guide — Emergency Route Finder

This guide covers everything needed to run the project locally from scratch. Read through once before starting — a few things (API keys, Firebase) are optional and the app works without them.

---

## What you need installed

| Tool | Version | Why |
|---|---|---|
| JDK | 17+ | Spring Boot backend |
| Android Studio | 2023.1+ (Hedgehog) | Android app |
| MySQL | 8.0+ | Database |
| Maven | 3.8+ | Backend build |
| Google Maps API key | — | Maps + Routes (optional) |
| Firebase project | — | FCM push notifications (optional) |

---

## Step 1 — Set up the database

Run these two files against your local MySQL instance:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p emergency_route_finder < database/demo_data.sql
```

Then open `backend/src/main/resources/application.properties` and fill in your credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/emergency_route_finder?useSSL=false&serverTimezone=UTC
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

---

## Step 2 — Google Maps API key (optional)

1. Go to console.cloud.google.com
2. Enable: Maps SDK for Android, Routes API, Directions API
3. Create an API key (restrict it to package `com.emergencyroutefinder`)

Add it in two places:

**Backend** — `application.properties`:
```properties
google.maps.api-key=YOUR_KEY_HERE
```

**Android** — `android/app/build.gradle`:
```groovy
buildConfigField "String", "MAPS_API_KEY", '"YOUR_KEY_HERE"'
```

Without a key the backend generates simulated traffic data and the app remains fully demonstrable — maps just show grey tiles.

---

## Step 3 — Firebase setup (optional)

1. Go to console.firebase.google.com ? create a project ? add Android app with package `com.emergencyroutefinder`
2. Download `google-services.json` ? place it in `android/app/`
3. Download the Service Account JSON ? rename it to `firebase-service-account.json` ? place it in `backend/src/main/resources/`

To disable Firebase entirely and use polling instead:

```properties
firebase.enabled=false
```

With this flag set, the Android app polls the backend every 5 seconds for updates. Everything works, just without push notifications.

---

## Step 4 — Start the backend

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

The server starts at **http://localhost:8080**.

Verify it's up:
```bash
curl http://localhost:8080/actuator/health
```

Test login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"alice@example.com\",\"password\":\"password123\"}"
```

---

## Step 5 — Run the Android app

**Emulator** — backend URL is already configured as `10.0.2.2:8080`, nothing to change.

**Physical device on the same WiFi** — update `BACKEND_URL` in `android/app/build.gradle`:
```groovy
buildConfigField "String", "BACKEND_URL", '"http://192.168.X.X:8080"'
```
Find your IP with `ipconfig` on Windows.

In Android Studio: File ? Open ? select the `android/` folder ? wait for Gradle sync (first run takes a few minutes) ? run on emulator or device (API 26+).

---

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Emergency user | alice@example.com | password123 |
| Emergency user | bob@example.com | password123 |
| Ambulance driver | driver1@example.com | password123 |
| Fire engine driver | driver2@example.com | password123 |

---

## Testing the full flow

1. **Device 1** — log in as `alice@example.com`
2. Tap SOS ? fill the emergency form ? submit
3. Backend finds the nearest available worker (Haversine, async) and sends an FCM alert
4. **Device 2** — log in as `driver1@example.com`, set status to Available
5. Worker receives the push notification with a 30-second acceptance countdown
6. Worker taps Accept
7. Device 1 sees the worker's location updating on the map in real time (WebSocket)
8. Worker progresses through: Responding ? Arrived ? Complete
9. Both sides see status changes as they happen

---

## Architecture

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
 (Google 50% + Camera         (MySQL 8.0,
  25% + Crowd GPS 25%)         PreparedStatement)
```

---

## Traffic fusion weights

| Source | Weight | Fallback when unavailable |
|---|---|---|
| Google Routes API | 50% | Simulated data |
| Traffic camera scores | 25% | Excluded; weight redistributed |
| Crowd GPS data | 25% | Excluded if no opted-in users |

Confidence: HIGH (all 3 sources) ? MEDIUM (2) ? LOW (1). Routes re-evaluated every 5 minutes via `@Scheduled`.

---

## Multithreading

| Operation | Mechanism | Thread name |
|---|---|---|
| Worker search | `@Async("taskExecutor")` | ERF-Task-N |
| FCM dispatch | `@Async("taskExecutor")` | ERF-Task-N |
| Route computation | `@Async("taskExecutor")` | ERF-Task-N |
| Location update | `@Async("taskExecutor")` | ERF-Task-N |
| Route re-evaluation | `@Scheduled(fixedRate=300000)` | Scheduler |
| Traffic fetch | `@Async("trafficExecutor")` | ERF-Traffic-N |
| Android GPS | `LocationForegroundService` | OS service |

---

## Common issues

**"Connection refused" on Android**
- Emulator ? use `10.0.2.2:8080`
- Physical device ? use your computer's local IP
- `usesCleartextTraffic="true"` is already set in the manifest

**MySQL startup error**
- Check that MySQL is running
- Verify the database `emergency_route_finder` exists (created by `schema.sql`)
- Confirm username/password in `application.properties`

**FCM not working**
- Set `firebase.enabled=false` to switch to polling — everything else continues normally

**Maps show grey tiles**
- Add a valid API key — all routing logic works without it

---

## Project layout

```
ch/
+-- README.md
+-- SETUP.md                        This file
+-- android/                        Android project
¦   +-- app/src/main/
¦       +-- AndroidManifest.xml
¦       +-- java/com/emergencyroutefinder/
¦           +-- ui/auth/            Login, register screens
¦           +-- ui/user/            10 user-facing activities
¦           +-- ui/worker/          8 worker-facing activities
¦           +-- network/            Retrofit client + API interface (35+ endpoints)
¦           +-- websocket/          STOMP WebSocket client
¦           +-- service/            Location foreground service
¦           +-- notification/       FCM receiver
¦           +-- util/               SessionManager
+-- backend/
¦   +-- src/main/java/com/emergencyroutefinder/
¦       +-- controller/             7 REST controllers
¦       +-- service/                Business logic (auth, emergency, allocation, notification)
¦       +-- traffic/                Three-source fusion engine
¦       +-- route/                  Route computation and scoring
¦       +-- repository/             13 JDBC repositories (PreparedStatement)
¦       +-- security/               JWT filter + Spring Security
¦       +-- config/                 Async, WebSocket, Firebase, Security setup
+-- database/
    +-- schema.sql                  MySQL DDL with indexes
    +-- demo_data.sql               Seed data
```
