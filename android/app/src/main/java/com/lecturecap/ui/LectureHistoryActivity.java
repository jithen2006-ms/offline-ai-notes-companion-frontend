package com.lecturecap.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lecturecap.R;
import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.LectureSession;
import com.lecturecap.network.BackendUploadManager;
import com.lecturecap.ui.adapter.LectureHistoryAdapter;

import java.util.List;

public class LectureHistoryActivity extends AppCompatActivity implements LectureHistoryAdapter.OnLectureClickListener {

    private static final String TAG = "LECTURE_HISTORY";

    private RecyclerView rvLectureHistory;
    private LinearLayout layoutEmptyHistory;
    private LectureHistoryAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecture_history);

        dbHelper = DatabaseHelper.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.lecture_history);
        }

        rvLectureHistory = findViewById(R.id.rvLectureHistory);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);

        rvLectureHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LectureHistoryAdapter(this);
        rvLectureHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        List<LectureSession> sessions = dbHelper.getAllLectureSessions();
        Log.d(TAG, "[DATABASE_READ] Loaded " + sessions.size() + " lecture sessions from SQLite.");
        adapter.setSessions(sessions);

        if (sessions.isEmpty()) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            rvLectureHistory.setVisibility(View.GONE);
        } else {
            layoutEmptyHistory.setVisibility(View.GONE);
            rvLectureHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLectureClick(LectureSession session) {
        showTranscriptDetailDialog(session);
    }

    private void showTranscriptDetailDialog(LectureSession session) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transcript_detail, null);

        TextView tvDetailCourseName = dialogView.findViewById(R.id.tvDetailCourseName);
        TextView tvDetailUploadBadge = dialogView.findViewById(R.id.tvDetailUploadBadge);
        TextView tvDetailSlotAndDate = dialogView.findViewById(R.id.tvDetailSlotAndDate);
        TextView tvDetailStatusTag = dialogView.findViewById(R.id.tvDetailStatusTag);
        TextView tvDetailTranscript = dialogView.findViewById(R.id.tvDetailTranscript);
        Button btnDetailUploadNow = dialogView.findViewById(R.id.btnDetailUploadNow);

        tvDetailCourseName.setText(session.getCourseName());
        tvDetailSlotAndDate.setText(session.getDate() + " • " + session.getTimeRange());

        String transcript = session.getTranscript();
        if (transcript != null && !transcript.trim().isEmpty()) {
            tvDetailTranscript.setText(transcript);
        } else {
            tvDetailTranscript.setText("No transcript available.");
        }

        if (LectureSession.UPLOAD_STATUS_UPLOADED.equals(session.getUploadStatus())) {
            tvDetailUploadBadge.setText("Uploaded ✓");
            tvDetailUploadBadge.setTextColor(getColor(R.color.status_active));
            btnDetailUploadNow.setText("Re-upload to Backend");
        } else if (LectureSession.UPLOAD_STATUS_FAILED.equals(session.getUploadStatus())) {
            tvDetailUploadBadge.setText("Upload Failed ⚠️");
            tvDetailUploadBadge.setTextColor(getColor(R.color.status_recording));
            btnDetailUploadNow.setText("Retry Upload");
        } else {
            tvDetailUploadBadge.setText("Upload Pending ⏳");
            tvDetailUploadBadge.setTextColor(getColor(R.color.accent));
            btnDetailUploadNow.setText("Upload to Backend Now");
        }

        if (LectureSession.STATUS_NO_SPEECH.equals(session.getSessionStatus())) {
            tvDetailStatusTag.setText("Status: Possible No Class / No Speech Detected");
            tvDetailStatusTag.setTextColor(getColor(R.color.status_warning));
        } else if (LectureSession.UPLOAD_STATUS_FAILED.equals(session.getUploadStatus())) {
            tvDetailStatusTag.setText("Status: Transcription Completed (Upload Failed)");
            tvDetailStatusTag.setTextColor(getColor(R.color.status_recording));
        } else {
            tvDetailStatusTag.setText("Status: Completed");
            tvDetailStatusTag.setTextColor(getColor(R.color.status_active));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        btnDetailUploadNow.setOnClickListener(v -> {
            btnDetailUploadNow.setEnabled(false);
            btnDetailUploadNow.setText("Uploading...");

            Log.d(TAG, "[LECTURE_UPLOAD_RETRY] Retrying upload for session ID=" + session.getId() + " without re-transcribing...");

            BackendUploadManager.uploadLectureSession(this, session, new BackendUploadManager.UploadCallback() {
                @Override
                public void onSuccess(String responseMessage) {
                    Log.d(TAG, "[LECTURE_UPLOAD_RETRY_SUCCESS] Session ID=" + session.getId() + " upload succeeded.");
                    Toast.makeText(LectureHistoryActivity.this, "Uploaded successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadHistory();
                }

                @Override
                public void onError(String errorMessage) {
                    Log.w(TAG, "[LECTURE_UPLOAD_RETRY_FAILED] Session ID=" + session.getId() + " retry failed: " + errorMessage);
                    Toast.makeText(LectureHistoryActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    btnDetailUploadNow.setEnabled(true);
                    btnDetailUploadNow.setText("Retry Upload");
                    loadHistory();
                }
            });
        });

        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lecture_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_clear_history) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all recorded lecture history?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        dbHelper.clearAllLectureHistory();
                        loadHistory();
                        Toast.makeText(this, "Lecture history cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BackendUploadManager.syncPendingUploads(this, null);
        loadHistory();
    }
}
