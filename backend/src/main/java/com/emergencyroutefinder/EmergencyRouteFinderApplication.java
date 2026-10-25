package com.emergencyroutefinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Emergency Route Finder — Spring Boot Application Entry Point.
 *
 * @EnableAsync    — enables async multithreading for worker search, notifications, route computation
 * @EnableScheduling — enables scheduled route re-evaluation every 5 minutes
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class EmergencyRouteFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergencyRouteFinderApplication.class, args);
    }
}
