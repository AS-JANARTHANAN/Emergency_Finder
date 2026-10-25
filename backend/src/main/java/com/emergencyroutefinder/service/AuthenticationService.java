package com.emergencyroutefinder.service;

import com.emergencyroutefinder.dto.*;
import com.emergencyroutefinder.exception.BusinessException;
import com.emergencyroutefinder.model.EmergencyWorker;
import com.emergencyroutefinder.model.User;
import com.emergencyroutefinder.repository.EmergencyWorkerRepository;
import com.emergencyroutefinder.repository.UserRepository;
import com.emergencyroutefinder.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service — registration and login for both roles.
 *
 * Demonstrates: Service layer, Security, Exception Handling, Encapsulation
 */
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final EmergencyWorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository userRepository,
                                  EmergencyWorkerRepository workerRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider,
                                  AuthenticationManager authenticationManager) {
        this.userRepository       = userRepository;
        this.workerRepository     = workerRepository;
        this.passwordEncoder      = passwordEncoder;
        this.jwtTokenProvider     = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    // ── User Registration ─────────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerUser(UserRegistrationRequest req) {
        validateUserRegistration(req);

        User user = new User(req.getFullName(), req.getEmail(), req.getMobile(),
                passwordEncoder.encode(req.getPassword()));  // BCrypt hash — never plaintext
        user.setAllowCrowdTraffic(req.isAllowCrowdTraffic());
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail(), "USER", user.getId());
        return new AuthResponse(token, "USER", user.getId(), user.getFullName(), user.getEmail());
    }

    private void validateUserRegistration(UserRegistrationRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email address is already registered.");
        }
        if (userRepository.existsByMobile(req.getMobile())) {
            throw new BusinessException("Mobile number is already registered.");
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }
        if (req.getPassword().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
    }

    // ── Worker Registration ───────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerWorker(WorkerRegistrationRequest req) {
        validateWorkerRegistration(req);

        EmergencyWorker worker = new EmergencyWorker(
                req.getFullName(), req.getEmail(), req.getMobile(),
                passwordEncoder.encode(req.getPassword()),
                EmergencyWorker.WorkerType.valueOf(req.getWorkerType()));
        worker.setLicenseId(req.getLicenseId());
        worker = workerRepository.save(worker);

        String token = jwtTokenProvider.generateToken(worker.getEmail(), "WORKER", worker.getId());
        return new AuthResponse(token, "WORKER", worker.getId(), worker.getFullName(), worker.getEmail());
    }

    private void validateWorkerRegistration(WorkerRegistrationRequest req) {
        if (workerRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email address is already registered.");
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }
        if (req.getPassword().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        // Determine role and ID by checking which table the email belongs to
        var userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) throw new BusinessException("Account is deactivated.");
            if (req.getRole() != null && !req.getRole().trim().isEmpty() && !"USER".equalsIgnoreCase(req.getRole().trim())) {
                throw new BusinessException("Account is registered as Emergency User. Please select Emergency User role.");
            }
            String token = jwtTokenProvider.generateToken(user.getEmail(), "USER", user.getId());
            return new AuthResponse(token, "USER", user.getId(), user.getFullName(), user.getEmail());
        }

        var workerOpt = workerRepository.findByEmail(req.getEmail());
        if (workerOpt.isPresent()) {
            EmergencyWorker worker = workerOpt.get();
            if (!worker.isActive()) throw new BusinessException("Account is deactivated.");
            if (req.getRole() != null && !req.getRole().trim().isEmpty() && !"WORKER".equalsIgnoreCase(req.getRole().trim())) {
                throw new BusinessException("Account is registered as Worker / Driver. Please select Worker / Driver role.");
            }
            String token = jwtTokenProvider.generateToken(worker.getEmail(), "WORKER", worker.getId());
            return new AuthResponse(token, "WORKER", worker.getId(), worker.getFullName(), worker.getEmail());
        }

        throw new BusinessException("No account found for email: " + req.getEmail());
    }
}
