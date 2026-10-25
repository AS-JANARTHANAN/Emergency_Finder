package com.emergencyroutefinder.security;

import com.emergencyroutefinder.repository.EmergencyWorkerRepository;
import com.emergencyroutefinder.repository.UserRepository;
import com.emergencyroutefinder.model.User;
import com.emergencyroutefinder.model.EmergencyWorker;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * Loads user/worker details for Spring Security authentication.
 * Searches both user and worker tables by email.
 */
@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmergencyWorkerRepository workerRepository;

    public JwtUserDetailsService(UserRepository userRepository,
                                  EmergencyWorkerRepository workerRepository) {
        this.userRepository = userRepository;
        this.workerRepository = workerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try user table first
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                    user.get().getEmail(),
                    user.get().getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }

        // Try worker table
        Optional<EmergencyWorker> worker = workerRepository.findByEmail(email);
        if (worker.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                    worker.get().getEmail(),
                    worker.get().getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_WORKER"))
            );
        }

        throw new UsernameNotFoundException("No account found for email: " + email);
    }
}
