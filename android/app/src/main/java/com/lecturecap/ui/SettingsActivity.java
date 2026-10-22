package com.lecturecap.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.lecturecap.R;
import com.lecturecap.network.BackendUploadManager;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private static final int PERMISSION_MIC_REQUEST = 301;

    private TextView tvSettingsEmail;
    private MaterialButton btnLogout;
    private TextInputEditText etBackendUrl;
    private MaterialButton btnSaveBackendUrl;
    private MaterialButton btnTestConnection;
    private SwitchMaterial switchAutoRecord;
    private TextView tvMicPermissionStatus;
    private MaterialButton btnGrantMicPermission;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        initViews();
        loadSettingsData();
        setupListeners();
    }

    private void initViews() {
        tvSettingsEmail = findViewById(R.id.tvSettingsEmail);
        btnLogout = findViewById(R.id.btnLogout);
        etBackendUrl = findViewById(R.id.etBackendUrl);
        btnSaveBackendUrl = findViewById(R.id.btnSaveBackendUrl);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        switchAutoRecord = findViewById(R.id.switchAutoRecord);
        tvMicPermissionStatus = findViewById(R.id.tvMicPermissionStatus);
        btnGrantMicPermission = findViewById(R.id.btnGrantMicPermission);
    }

    private void loadSettingsData() {
        tvSettingsEmail.setText(sessionManager.getUserEmail());
        etBackendUrl.setText(sessionManager.getBackendUrl());
        switchAutoRecord.setChecked(sessionManager.isAutoRecordEnabled());
        updateMicPermissionStatus();
    }

    private void updateMicPermissionStatus() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            tvMicPermissionStatus.setText("Status: Granted ✓");
            tvMicPermissionStatus.setTextColor(getColor(R.color.status_active));
            btnGrantMicPermission.setEnabled(false);
            btnGrantMicPermission.setText("Granted");
        } else {
            tvMicPermissionStatus.setText("Status: Not Granted ⚠️");
            tvMicPermissionStatus.setTextColor(getColor(R.color.status_recording));
            btnGrantMicPermission.setEnabled(true);
            btnGrantMicPermission.setText("Grant");
        }
    }

    private void setupListeners() {
        btnSaveBackendUrl.setOnClickListener(v -> {
            String url = etBackendUrl.getText() != null ? etBackendUrl.getText().toString().trim() : "";
            if (TextUtils.isEmpty(url)) {
                etBackendUrl.setError("URL cannot be empty");
                return;
            }
            sessionManager.setBackendUrl(url);
            Toast.makeText(this, "Backend URL saved", Toast.LENGTH_SHORT).show();
        });

        btnTestConnection.setOnClickListener(v -> {
            String url = etBackendUrl.getText() != null ? etBackendUrl.getText().toString().trim() : "";
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(this, "Please enter a valid URL first", Toast.LENGTH_SHORT).show();
                return;
            }
            btnTestConnection.setEnabled(false);
            btnTestConnection.setText("Testing...");
            BackendUploadManager.testConnection(url, new BackendUploadManager.UploadCallback() {
                @Override
                public void onSuccess(String responseMessage) {
                    btnTestConnection.setEnabled(true);
                    btnTestConnection.setText("Test Ping");
                    Toast.makeText(SettingsActivity.this, "✓ " + responseMessage, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    btnTestConnection.setEnabled(true);
                    btnTestConnection.setText("Test Ping");
                    Toast.makeText(SettingsActivity.this, "⚠️ " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        switchAutoRecord.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setAutoRecordEnabled(isChecked);
            if (isChecked) {
                ScheduleManager.rescheduleAllAlarms(this);
            }
            Toast.makeText(this, isChecked ? "Automatic timetable recording enabled" : "Automatic recording disabled", Toast.LENGTH_SHORT).show();
        });

        btnGrantMicPermission.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_MIC_REQUEST);
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_MIC_REQUEST) {
            updateMicPermissionStatus();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMicPermissionStatus();
    }
}
