package com.lecturecap.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.lecturecap.R;
import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.LectureSession;
import com.lecturecap.network.BackendUploadManager;
import com.lecturecap.stt.SpeechToTextManager;
import com.lecturecap.ui.RecordingActivity;
import com.lecturecap.utils.PermissionHelper;
import com.lecturecap.utils.SessionManager;
import com.lecturecap.utils.TimeUtils;

import java.io.File;
import java.io.IOException;

public class LectureRecordingService extends Service {

    private static final String TAG = "LectureRecordingService";
    public static final String CHANNEL_ID = "lecture_capture_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START_RECORDING = "com.lecturecap.action.START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "com.lecturecap.action.STOP_RECORDING";

    public static final String EXTRA_COURSE_NAME = "extra_course_name";
    public static final String EXTRA_START_TIME = "extra_start_time";
    public static final String EXTRA_END_TIME = "extra_end_time";
    public static final String EXTRA_DATE = "extra_date";

    public static final String BROADCAST_RECORDING_UPDATE = "com.lecturecap.broadcast.RECORDING_UPDATE";
    public static final String EXTRA_ELAPSED_SECONDS = "extra_elapsed_seconds";
    public static final String EXTRA_IS_RECORDING = "extra_is_recording";
    public static final String EXTRA_LIVE_TRANSCRIPT = "extra_live_transcript";

    private static boolean isServiceRunning = false;
    private static String activeCourseName = "";
    private static long elapsedSeconds = 0;
    private static String activeLiveTranscript = "";

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private SpeechToTextManager speechToTextManager;

    private String currentCourse = "General Lecture";
    private String currentStartTime = "09:00";
    private String currentEndTime = "10:00";
    private String currentDate = "";
    private long currentSessionDbId = -1;

    public static boolean isRunning() {
        return isServiceRunning;
    }

    public static String getActiveCourseName() {
        return activeCourseName;
    }

    public static long getElapsedSeconds() {
        return elapsedSeconds;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        timerHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_RECORDING.equals(action)) {
                currentCourse = intent.getStringExtra(EXTRA_COURSE_NAME);
                if (currentCourse == null || currentCourse.isEmpty()) currentCourse = "General Lecture";

                currentStartTime = intent.getStringExtra(EXTRA_START_TIME);
                if (currentStartTime == null || currentStartTime.isEmpty()) currentStartTime = TimeUtils.getCurrentTime24();

                currentEndTime = intent.getStringExtra(EXTRA_END_TIME);
                if (currentEndTime == null || currentEndTime.isEmpty()) currentEndTime = "10:00";

                currentDate = intent.getStringExtra(EXTRA_DATE);
                if (currentDate == null || currentDate.isEmpty()) currentDate = TimeUtils.getCurrentDateIso();

                startForegroundRecording();
            } else if (ACTION_STOP_RECORDING.equals(action)) {
                stopForegroundRecording();
            }
        }
        return START_NOT_STICKY;
    }

    private void startForegroundRecording() {
        if (isServiceRunning) {
            return;
        }

        isServiceRunning = true;
        activeCourseName = currentCourse;
        elapsedSeconds = 0;

        Notification notification = buildNotification(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Check RECORD_AUDIO permission before starting STT engine
        if (!PermissionHelper.hasAudioRecordingPermission(this)) {
            Log.e(TAG, "Cannot start automatic Speech-to-Text: RECORD_AUDIO permission is NOT granted!");
            isServiceRunning = false;
            stopForeground(true);
            stopSelf();
            return;
        }

        // Initialize local database session entry
        SessionManager sessionManager = new SessionManager(this);
        LectureSession session = new LectureSession(
                sessionManager.getUserEmail(),
                currentCourse,
                currentDate,
                currentStartTime,
                currentEndTime,
                "",
                LectureSession.STATUS_RECORDING,
                LectureSession.UPLOAD_STATUS_PENDING,
                0,
                System.currentTimeMillis()
        );
        currentSessionDbId = DatabaseHelper.getInstance(this).addLectureSession(session);

        // Start continuous Speech-to-Text engine (16kHz PCM Whisper ASR)
        initSpeechToText();

        // Start duration timer
        startDurationTimer();
    }

    private void initSpeechToText() {
        speechToTextManager = new SpeechToTextManager(this);
        speechToTextManager.setUpdateListener(new SpeechToTextManager.OnTranscriptUpdateListener() {
            @Override
            public void onPartialTranscript(String liveText) {
                Log.d("LectureCap_STT", "[PARTIAL_RESULT_RECEIVED] Live partial speech: " + liveText);
                String fullLive = activeLiveTranscript;
                if (!fullLive.isEmpty()) {
                    fullLive += " " + liveText;
                } else {
                    fullLive = liveText;
                }
                sendUpdateBroadcast(elapsedSeconds, true, fullLive);
            }

            @Override
            public void onTranscriptUpdated(String fullAccumulatedTranscript) {
                Log.d("LectureCap_STT", "[LIVE_TRANSCRIPT_UPDATED] Accumulated: " + fullAccumulatedTranscript);
                activeLiveTranscript = fullAccumulatedTranscript;
                sendUpdateBroadcast(elapsedSeconds, true, activeLiveTranscript);
            }

            @Override
            public void onError(String errorMsg) {
                Log.w("LectureCap_STT", "Whisper ASR error: " + errorMsg);
            }
        });
        speechToTextManager.startListening();
    }

    private void startDurationTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    elapsedSeconds++;

                    // Update notification periodically
                    if (elapsedSeconds % 5 == 0) {
                        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (nm != null) {
                            nm.notify(NOTIFICATION_ID, buildNotification(elapsedSeconds));
                        }
                    }

                    // Broadcast update to active UI
                    sendUpdateBroadcast(elapsedSeconds, true);

                    // Check if class end time is reached
                    if (TimeUtils.getCurrentTime24().equals(currentEndTime)) {
                        Log.d(TAG, "Scheduled class end time reached (" + currentEndTime + "). Stopping recording automatically.");
                        stopForegroundRecording();
                        return;
                    }

                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopForegroundRecording() {
        if (!isServiceRunning) return;

        isServiceRunning = false;
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // Stop and harvest Whisper ASR transcript & timestamped segments
        String transcript = "";
        String segmentsJsonStr = null;
        boolean hasSpeech = false;
        if (speechToTextManager != null) {
            transcript = speechToTextManager.getFinalTranscript();
            hasSpeech = speechToTextManager.hasMeaningfulSpeech();
            if (speechToTextManager.getTranscriptPayloadJson() != null) {
                segmentsJsonStr = speechToTextManager.getTranscriptPayloadJson().toString();
            }
            speechToTextManager.stopListening();
            speechToTextManager = null;
        }

        // Finalize lecture session in Database & automatically trigger backend upload
        finalizeLectureSession(transcript, segmentsJsonStr, hasSpeech);

        sendUpdateBroadcast(elapsedSeconds, false);
        stopForeground(true);
        stopSelf();
    }

    private void finalizeLectureSession(String transcript, String segmentsJsonStr, boolean hasSpeech) {
        if (currentSessionDbId != -1) {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            LectureSession session = db.getLectureSessionById(currentSessionDbId);
            if (session != null) {
                session.setDurationSeconds(elapsedSeconds);
                session.setSegmentsJsonStr(segmentsJsonStr);

                if (!hasSpeech || transcript == null || transcript.trim().isEmpty()) {
                    session.setSessionStatus(LectureSession.STATUS_NO_SPEECH);
                    session.setTranscript("[Possible No Class / No Speech Detected]");
                } else {
                    session.setSessionStatus(LectureSession.STATUS_COMPLETED);
                    session.setTranscript(transcript);
                }
                session.setUploadStatus(LectureSession.UPLOAD_STATUS_PENDING);
                db.updateLectureSession(session);

                // Automatic upload of transcript & timestamped segments to backend REST API
                BackendUploadManager.uploadLectureSession(getApplicationContext(), session, new BackendUploadManager.UploadCallback() {
                    @Override
                    public void onSuccess(String responseMessage) {
                        Log.d(TAG, "Automatic upload success: " + responseMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.w(TAG, "Automatic upload pending: " + errorMessage);
                    }
                });
            }
        }
    }

    private void sendUpdateBroadcast(long seconds, boolean recording) {
        sendUpdateBroadcast(seconds, recording, null);
    }

    private void sendUpdateBroadcast(long seconds, boolean recording, String liveTranscript) {
        Intent intent = new Intent(BROADCAST_RECORDING_UPDATE);
        intent.putExtra(EXTRA_ELAPSED_SECONDS, seconds);
        intent.putExtra(EXTRA_IS_RECORDING, recording);
        if (liveTranscript != null) {
            activeLiveTranscript = liveTranscript;
            intent.putExtra(EXTRA_LIVE_TRANSCRIPT, liveTranscript);
        }
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private Notification buildNotification(long seconds) {
        Intent openActivityIntent = new Intent(this, RecordingActivity.class);
        openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, LectureRecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String durationStr = TimeUtils.formatDuration(seconds);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording: " + currentCourse)
                .setContentText("Duration: " + durationStr + " • Class Slot: " + currentStartTime + "–" + currentEndTime)
                .setSmallIcon(R.drawable.ic_mic)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_mic, "Stop Recording", stopPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Classroom Lecture Capture Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active lecture audio capture status");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (isServiceRunning) {
            stopForegroundRecording();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
