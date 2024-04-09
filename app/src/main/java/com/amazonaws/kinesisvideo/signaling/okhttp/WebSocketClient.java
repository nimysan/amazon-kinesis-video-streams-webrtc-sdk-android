package com.amazonaws.kinesisvideo.signaling.okhttp;

import static org.awaitility.Awaitility.await;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import android.util.Log;
import androidx.annotation.NonNull;

import com.amazonaws.kinesisvideo.signaling.SignalingListener;
import com.amazonaws.kinesisvideo.utils.Constants;

import java.util.concurrent.TimeUnit;

/**
 * An OkHttp based WebSocket client.
 */
class WebSocketClient {

    private static final String TAG = "WebSocketClient";

    private final WebSocket webSocket;
    private volatile boolean isOpen = false;

    WebSocketClient(final String uri, final SignalingListener signalingListener) {

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        OkHttpClient client = clientBuilder.build();

        Request request = new Request.Builder()
                .url(uri)
                .addHeader("User-Agent", Constants.APP_NAME + "/" + Constants.VERSION + " " + System.getProperty("http.agent"))
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket connection opened");
                isOpen = true;
                // Register message handler
                signalingListener.getWebsocketListener().onOpen(webSocket, response);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "Received message: " + text);
                signalingListener.getWebsocketListener().onMessage(webSocket, text);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket connection closed: " + reason);
                isOpen = false;
                signalingListener.getWebsocketListener().onClosed(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket connection failed", t);
                isOpen = false;
                // Handle failure
                signalingListener.onException((Exception) t);
            }
        });

        // Await WebSocket connection
        await().atMost(10, TimeUnit.SECONDS).until(WebSocketClient.this::isOpen);
    }

    void send(String message) {
        Log.d(TAG, "Sending message: " + message);
        webSocket.send(message);
    }

    void disconnect() {
        webSocket.close(1000, "Disconnect requested");
    }

    boolean isOpen() {
        return isOpen;
    }
}
