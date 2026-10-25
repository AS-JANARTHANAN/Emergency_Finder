package com.emergencyroutefinder.websocket;

import android.util.Log;
import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

/**
 * STOMP WebSocket client wrapper.
 * Manages subscription to real-time topics.
 *
 * Demonstrates: Real-time communication, STOMP over WebSocket
 */
public class StompWebSocketClient {

    private static final String TAG = "StompWS";

    private final String baseUrl;
    private StompClient stompClient;
    private Disposable statusSub, locationSub;

    public interface MessageHandler {
        void onMessage(String topic, String message);
    }

    public StompWebSocketClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void connect(String jwtToken,
                        String statusTopic, MessageHandler statusHandler,
                        String locationTopic, MessageHandler locationHandler) {
        try {
            // Build WebSocket URL: convert http:// to ws://
            String wsUrl = baseUrl.replace("http://", "ws://")
                                  .replace("https://", "wss://")
                           + "/ws/websocket";

            stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
            stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

            stompClient.connect(
                    java.util.Collections.singletonList(
                            new ua.naiksoftware.stomp.dto.StompHeader("Authorization", "Bearer " + jwtToken)));

            // Subscribe to status topic
            statusSub = stompClient.topic(statusTopic).subscribe(
                    frame -> statusHandler.onMessage(statusTopic, frame.getPayload()),
                    err -> Log.e(TAG, "Status topic error: " + err.getMessage()));

            // Subscribe to location topic
            locationSub = stompClient.topic(locationTopic).subscribe(
                    frame -> locationHandler.onMessage(locationTopic, frame.getPayload()),
                    err -> Log.e(TAG, "Location topic error: " + err.getMessage()));

            Log.i(TAG, "STOMP connected to " + wsUrl);

        } catch (Exception e) {
            Log.e(TAG, "STOMP connection error: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (statusSub   != null && !statusSub.isDisposed())   statusSub.dispose();
            if (locationSub != null && !locationSub.isDisposed()) locationSub.dispose();
            if (stompClient != null) stompClient.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Disconnect error: " + e.getMessage());
        }
    }
}
