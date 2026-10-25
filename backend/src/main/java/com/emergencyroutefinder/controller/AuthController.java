package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.dto.*;
import com.emergencyroutefinder.service.AuthenticationService;
import com.emergencyroutefinder.repository.VehicleRepository;
import com.emergencyroutefinder.model.EmergencyVehicle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller — login, user registration, worker registration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authService;
    private final VehicleRepository vehicleRepository;

    public AuthController(AuthenticationService authService, VehicleRepository vehicleRepository) {
        this.authService = authService;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/user")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody UserRegistrationRequest request) {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/worker")
    public ResponseEntity<AuthResponse> registerWorker(@RequestBody WorkerRegistrationRequest request) {
        AuthResponse response = authService.registerWorker(request);

        // Auto-create vehicle for this worker
        if (request.getVehicleNumber() != null && !request.getVehicleNumber().isEmpty()) {
            try {
                EmergencyVehicle vehicle;
                if ("FIRE_ENGINE".equals(request.getVehicleType())) {
                    vehicle = new com.emergencyroutefinder.model.FireEngine(
                            request.getVehicleNumber(), response.getPrincipalId());
                } else {
                    vehicle = new com.emergencyroutefinder.model.Ambulance(
                            request.getVehicleNumber(), response.getPrincipalId());
                }
                vehicleRepository.save(vehicle);
            } catch (Exception e) {
                // Vehicle creation failure doesn't block registration
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
