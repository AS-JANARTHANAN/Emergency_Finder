package com.emergencyroutefinder.network;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    // Auth helpers
    public Task<com.google.firebase.auth.AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<com.google.firebase.auth.AuthResult> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    // Firestore helpers for Users/Workers
    public Task<Void> saveUserRecord(String uid, String role, String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("name", name);
        data.put("email", email);
        return db.collection("Users").document(uid).set(data);
    }

    public Task<DocumentSnapshot> getUserRecord(String uid) {
        return db.collection("Users").document(uid).get();
    }

    public Task<Void> updateWorkerStatus(String uid, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("availability", status);
        return db.collection("Users").document(uid).update(data);
    }

    // Save location
    public Task<Void> updateLocation(String uid, double lat, double lng) {
        Map<String, Object> loc = new HashMap<>();
        loc.put("lat", lat);
        loc.put("lng", lng);
        loc.put("timestamp", System.currentTimeMillis());
        return db.collection("Locations").document(uid).set(loc);
    }
    
    public Task<DocumentSnapshot> getLocation(String uid) {
        return db.collection("Locations").document(uid).get();
    }
    
    public Task<QuerySnapshot> getAvailableWorkers() {
        return db.collection("Users")
                 .whereEqualTo("role", "WORKER")
                 .whereEqualTo("availability", "AVAILABLE")
                 .get();
    }

    // Emergency Request
    public Task<Void> createEmergencyRequest(String requestId, Map<String, Object> data) {
        return db.collection("Emergencies").document(requestId).set(data);
    }
    
    public Task<Void> updateEmergencyRequest(String requestId, Map<String, Object> data) {
        return db.collection("Emergencies").document(requestId).update(data);
    }
    
    public Task<DocumentSnapshot> getEmergencyRequest(String requestId) {
        return db.collection("Emergencies").document(requestId).get();
    }
    
    public Task<QuerySnapshot> getRequestsByUser(String uid) {
        return db.collection("Emergencies")
                 .whereEqualTo("userId", uid)
                 .get();
    }
    
    public Task<QuerySnapshot> getRequestsByWorker(String workerId) {
        return db.collection("Emergencies")
                 .whereEqualTo("assignedWorkerId", workerId)
                 .get();
    }
}
