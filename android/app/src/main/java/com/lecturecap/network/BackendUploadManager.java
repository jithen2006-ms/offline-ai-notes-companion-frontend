package com.lecturecap.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.LectureSession;
import com.lecturecap.utils.SessionManager;
import com.lecturecap.utils.TimeUtils;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendUploadManager {

    private static final String TAG = "BackendUploadManager";

    public interface UploadCallback {
        void onSuccess(String responseMessage);
        void onError(String errorMessage);
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Uploads a lecture session transcript & timestamped segments asynchronously to the configured backend REST API.
     */
    public static void uploadLectureSession(Context context, LectureSession session, UploadCallback callback) {
        SessionManager sessionManager = new SessionManager(context);
        String targetUrl = sessionManager.getBackendUrl();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Build JSON payload
                JSONObject payload = new JSONObject();
                payload.put("userId", session.getUserId());
                payload.put("course_name", session.getCourseName());
                payload.put("course", session.getCourseName());
                payload.put("date", session.getDate());
                payload.put("day", TimeUtils.getDayOfWeekForDate(session.getDate()));
                payload.put("start_time", session.getStartTime());
                payload.put("startTime", session.getStartTime());
                payload.put("end_time", session.getEndTime());
                payload.put("endTime", session.getEndTime());

                if (session.hasSegmentsJson()) {
                    payload.put("transcript", session.getSegmentsJsonObject());
                } else {
                    payload.put("transcript", session.getTranscript());
                }

                payload.put("session_status", session.getSessionStatus());
                payload.put("duration_seconds", session.getDurationSeconds());

                Log.d(TAG, "Uploading payload to " + targetUrl + ": " + payload.toString());

                URL url = new URL(targetUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                // Large timestamped transcripts can take longer than 15 seconds to persist.
                connection.setReadTimeout(120000);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Backend responded with HTTP " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    // Success
                    dbHelper.updateUploadStatus(session.getId(), LectureSession.UPLOAD_STATUS_UPLOADED);
                    session.setUploadStatus(LectureSession.UPLOAD_STATUS_UPLOADED);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess("Lecture uploaded successfully (HTTP " + responseCode + ")"));
                    }
                } else {
                    // Server error response
                    String serverMessage = "";
                    if (connection.getErrorStream() != null) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                            StringBuilder errorBody = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                errorBody.append(line);
                            }
                            serverMessage = errorBody.toString();
                        }
                    }
                    dbHelper.updateUploadStatus(session.getId(), LectureSession.UPLOAD_STATUS_FAILED);
                    session.setUploadStatus(LectureSession.UPLOAD_STATUS_FAILED);

                    if (callback != null) {
                        String failureMessage = "Server returned HTTP " + responseCode
                                + (serverMessage.isEmpty() ? "" : ": " + serverMessage);
                        mainHandler.post(() -> callback.onError(failureMessage));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to upload lecture transcript", e);
                dbHelper.updateUploadStatus(session.getId(), LectureSession.UPLOAD_STATUS_FAILED);
                session.setUploadStatus(LectureSession.UPLOAD_STATUS_FAILED);

                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Connection failed: " + e.getMessage()));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Batch uploads all un-uploaded lecture sessions (PENDING or FAILED) to the backend.
     */
    public static void syncPendingUploads(Context context, UploadCallback callback) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        List<LectureSession> sessions = db.getAllLectureSessions();
        int pendingCount = 0;
        for (LectureSession session : sessions) {
            if (!LectureSession.UPLOAD_STATUS_UPLOADED.equals(session.getUploadStatus())) {
                pendingCount++;
                uploadLectureSession(context, session, null);
            }
        }
        if (callback != null) {
            final int count = pendingCount;
            mainHandler.post(() -> callback.onSuccess("Sync triggered for " + count + " pending lecture(s)."));
        }
    }

    /**
     * Tests connectivity to the backend by issuing GET /api/health
     */
    public static void testConnection(String targetUrl, UploadCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String pingUrl = targetUrl;
                if (pingUrl != null && pingUrl.contains("/api/lectures/upload")) {
                    pingUrl = pingUrl.replace("/api/lectures/upload", "/api/health");
                } else if (pingUrl != null && pingUrl.contains("/api/lectures")) {
                    pingUrl = pingUrl.replace("/api/lectures", "/api/health");
                }

                URL url = new URL(pingUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess("Server reachable (HTTP " + responseCode + ")"));
                    }
                } else {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("Server returned HTTP " + responseCode));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Could not connect: " + e.getMessage()));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}
