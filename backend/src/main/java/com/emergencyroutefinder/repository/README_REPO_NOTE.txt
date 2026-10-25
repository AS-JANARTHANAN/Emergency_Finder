// Add this method to EmergencyRequestRepository.java
// findByStatus for the scheduler in RouteService

// In the existing EmergencyRequestRepository.java, add:
// public List<EmergencyRequest> findByStatus(String status) {
//     return jdbc.query("SELECT * FROM emergency_requests WHERE status = ?", REQUEST_MAPPER, status);
// }

// This note ensures the developer adds this to the repository.
// The actual addition goes into EmergencyRequestRepository.java directly:
