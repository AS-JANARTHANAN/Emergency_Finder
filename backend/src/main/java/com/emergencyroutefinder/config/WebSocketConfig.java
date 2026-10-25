package com.emergencyroutefinder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket / STOMP configuration for real-time status updates.
 *
 * Clients subscribe to topics:
 *   /topic/request/{requestId}   — request status updates
 *   /topic/worker/{workerId}     — incoming emergency alerts
 *   /topic/user/{userId}         — user-specific updates
 *
 * Demonstrates: Real-time communication, Spring WebSocket
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic destinations
        registry.enableSimpleBroker("/topic", "/queue");
        // Client messages to /app/... are routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint — Android client connects here
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS fallback for non-WebSocket clients
    }
}
