package com.lecturecap.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.lecturecap.R;
import com.lecturecap.service.LectureRecordingService;
import com.lecturecap.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class RecordingActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private TextView tvLiveBadge;
    private TextView tvRecordingCourse;
    private TextView tvRecordingSlot;
    private TextView tvDurationTimer;
    private TextView tvLiveTranscript;
    private TextView tvSttModeBadge;
    private MaterialButton btnStopRecording;

    private String courseName = "General Lecture";
    private String startTime = "09:00";
    private String endTime = "10:00";
    private String dateIso = "";

    private final BroadcastReceiver recordingUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LectureRecordingService.BROADCAST_RECORDING_UPDATE.equals(intent.getAction())) {
                long seconds = intent.getLongExtra(LectureRecordingService.EXTRA_ELAPSED_SECONDS, 0);
                boolean isRecording = intent.getBooleanExtra(LectureRecordingService.EXTRA_IS_RECORDING, true);
                String liveTranscript = intent.getStringExtra(LectureRecordingService.EXTRA_LIVE_TRANSCRIPT);

                tvDurationTimer.setText(TimeUtils.formatDuration(seconds));

                if (liveTranscript != null && tvLiveTranscript != null) {
                    tvLiveTranscript.setText(liveTranscript);
                }

                if (!isRecording) {
                    onRecordingStopped();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Live Lecture Capture");
        }

        initViews();
        extractIntentData();

        btnStopRecording.setOnClickListener(v -> stopRecordingService());

        checkAndRequestPermissions();
    }

    private void initViews() {
        tvLiveBadge = findViewById(R.id.tvLiveBadge);
        tvRecordingCourse = findViewById(R.id.tvRecordingCourse);
        tvRecordingSlot = findViewById(R.id.tvRecordingSlot);
        tvDurationTimer = findViewById(R.id.tvDurationTimer);
        tvLiveTranscript = findViewById(R.id.tvLiveTranscript);
        tvSttModeBadge = findViewById(R.id.tvSttModeBadge);
        btnStopRecording = findViewById(R.id.btnStopRecording);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(LectureRecordingService.EXTRA_COURSE_NAME)) {
                courseName = intent.getStringExtra(LectureRecordingService.EXTRA_COURSE_NAME);
            }
            if (intent.hasExtra(LectureRecordingService.EXTRA_START_TIME)) {
                startTime = intent.getStringExtra(LectureRecordingService.EXTRA_START_TIME);
            }
            if (intent.hasExtra(LectureRecordingService.EXTRA_END_TIME)) {
                endTime = intent.getStringExtra(LectureRecordingService.EXTRA_END_TIME);
            }
            if (intent.hasExtra(LectureRecordingService.EXTRA_DATE)) {
                dateIso = intent.getStringExtra(LectureRecordingService.EXTRA_DATE);
            }
        }

        if (dateIso.isEmpty()) {
            dateIso = TimeUtils.getCurrentDateIso();
        }

        tvRecordingCourse.setText(courseName);
        tvRecordingSlot.setText("Scheduled: " + startTime + " – " + endTime);

        if (LectureRecordingService.isRunning()) {
            tvDurationTimer.setText(TimeUtils.formatDuration(LectureRecordingService.getElapsedSeconds()));
        }
    }

    private void checkAndRequestPermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!requiredPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            startRecordingServiceIfNotRunning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startRecordingServiceIfNotRunning();
            } else {
                Toast.makeText(this, "Microphone permission is required to capture lecture audio.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startRecordingServiceIfNotRunning() {
        if (!LectureRecordingService.isRunning()) {
            Intent serviceIntent = new Intent(this, LectureRecordingService.class);
            serviceIntent.setAction(LectureRecordingService.ACTION_START_RECORDING);
            serviceIntent.putExtra(LectureRecordingService.EXTRA_COURSE_NAME, courseName);
            serviceIntent.putExtra(LectureRecordingService.EXTRA_START_TIME, startTime);
            serviceIntent.putExtra(LectureRecordingService.EXTRA_END_TIME, endTime);
            serviceIntent.putExtra(LectureRecordingService.EXTRA_DATE, dateIso);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private void stopRecordingService() {
        Intent serviceIntent = new Intent(this, LectureRecordingService.class);
        serviceIntent.setAction(LectureRecordingService.ACTION_STOP_RECORDING);
        startService(serviceIntent);
        onRecordingStopped();
    }

    private void onRecordingStopped() {
        Toast.makeText(this, "Lecture recording finished and saved to History.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(LectureRecordingService.BROADCAST_RECORDING_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recordingUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(recordingUpdateReceiver, filter);
        }

        if (!LectureRecordingService.isRunning() && tvDurationTimer.getText().equals("00:00:00")) {
            // Service not yet started, handled in permission result
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(recordingUpdateReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
