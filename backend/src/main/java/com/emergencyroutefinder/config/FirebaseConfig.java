package com.emergencyroutefinder.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK initialization for FCM push notifications.
 *
 * If the service account file is missing, Firebase is disabled gracefully
 * and in-app polling is used as fallback — project remains demonstrable.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service-account-path:classpath:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initializeFirebase() {
        if (!firebaseEnabled) {
            log.warn("Firebase is DISABLED via configuration. FCM notifications will use in-app polling fallback.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized.");
            return;
        }

        try {
            InputStream serviceAccount;
            if (serviceAccountPath.startsWith("classpath:")) {
                String path = serviceAccountPath.replace("classpath:", "");
                serviceAccount = new ClassPathResource(path).getInputStream();
            } else {
                serviceAccount = new java.io.FileInputStream(serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully.");

        } catch (IOException e) {
            log.warn("Firebase service account file not found at '{}'. FCM disabled — using fallback polling.", serviceAccountPath);
            log.warn("To enable FCM: add your firebase-service-account.json to src/main/resources/");
        }
    }

    public boolean isFirebaseEnabled() {
        return firebaseEnabled && !FirebaseApp.getApps().isEmpty();
    }
}
