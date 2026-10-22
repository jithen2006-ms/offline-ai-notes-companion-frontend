package com.lecturecap.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lecturecap.R;
import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.TemporaryScheduleChange;
import com.lecturecap.ui.adapter.TemporaryChangesAdapter;
import com.lecturecap.utils.ScheduleImporter;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.TimeUtils;

import java.util.List;

public class TemporaryChangesActivity extends AppCompatActivity implements TemporaryChangesAdapter.OnDeleteClickListener {

    private RecyclerView rvTempChanges;
    private LinearLayout layoutEmptyTemp;
    private FloatingActionButton fabAddTempNotice;
    private MaterialButton btnImportNotices;
    private TemporaryChangesAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temporary_changes);

        dbHelper = DatabaseHelper.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Schedule Changes & Notices");
        }

        rvTempChanges = findViewById(R.id.rvTempChanges);
        layoutEmptyTemp = findViewById(R.id.layoutEmptyTemp);
        fabAddTempNotice = findViewById(R.id.fabAddTempNotice);
        btnImportNotices = findViewById(R.id.btnImportNotices);

        rvTempChanges.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TemporaryChangesAdapter(this);
        rvTempChanges.setAdapter(adapter);

        fabAddTempNotice.setOnClickListener(v -> showAddNoticeDialog());
        btnImportNotices.setOnClickListener(v -> showImportDialog());

        loadNotices();
    }

    private void loadNotices() {
        List<TemporaryScheduleChange> list = dbHelper.getAllTemporaryScheduleChanges();
        adapter.setItems(list);

        if (list.isEmpty()) {
            layoutEmptyTemp.setVisibility(View.VISIBLE);
            rvTempChanges.setVisibility(View.GONE);
        } else {
            layoutEmptyTemp.setVisibility(View.GONE);
            rvTempChanges.setVisibility(View.VISIBLE);
        }
    }

    private void showAddNoticeDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_temporary_change, null);
        Spinner spinnerTempType = dialogView.findViewById(R.id.spinnerTempType);
        EditText etTempCourse = dialogView.findViewById(R.id.etTempCourse);
        EditText etTempDate = dialogView.findViewById(R.id.etTempDate);
        EditText etTempReason = dialogView.findViewById(R.id.etTempReason);
        Button btnTempStartTime = dialogView.findViewById(R.id.btnTempStartTime);
        Button btnTempEndTime = dialogView.findViewById(R.id.btnTempEndTime);

        String[] types = {
                TemporaryScheduleChange.TYPE_CLASS_CANCELLED,
                TemporaryScheduleChange.TYPE_EXTRA_CLASS,
                TemporaryScheduleChange.TYPE_CLASS_RESCHEDULED,
                TemporaryScheduleChange.TYPE_DATE_HOLIDAY,
                TemporaryScheduleChange.TYPE_SPECIAL_WORKING_DAY
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerTempType.setAdapter(typeAdapter);

        final String[] startTime = {"09:00"};
        final String[] endTime = {"10:00"};

        btnTempStartTime.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, min) -> {
            startTime[0] = TimeUtils.formatTime(hour, min);
            btnTempStartTime.setText(startTime[0]);
        }, 9, 0, true).show());

        btnTempEndTime.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, min) -> {
            endTime[0] = TimeUtils.formatTime(hour, min);
            btnTempEndTime.setText(endTime[0]);
        }, 10, 0, true).show());

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String type = spinnerTempType.getSelectedItem().toString();
                    String course = etTempCourse.getText().toString().trim();
                    String date = etTempDate.getText().toString().trim();
                    String reason = etTempReason.getText().toString().trim();

                    if (TextUtils.isEmpty(date)) {
                        Toast.makeText(this, "Please enter a valid date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    TemporaryScheduleChange item = new TemporaryScheduleChange(
                            date, type, course, startTime[0], endTime[0], reason, null
                    );
                    dbHelper.addTemporaryScheduleChange(item);
                    Toast.makeText(this, "Notice saved", Toast.LENGTH_SHORT).show();
                    loadNotices();
                    ScheduleManager.rescheduleAllAlarms(this);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showImportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_schedule, null);
        EditText etJsonContent = dialogView.findViewById(R.id.etJsonContent);
        Button btnPasteSampleTemplate = dialogView.findViewById(R.id.btnPasteSampleTemplate);

        String sampleTemplate = "[\n" +
                "  {\"date\": \"2026-08-25\", \"changeType\": \"CLASS_CANCELLED\", \"courseName\": \"DBMS\", \"startTime\": \"09:00\", \"endTime\": \"10:00\", \"reason\": \"Faculty attending workshop\"},\n" +
                "  {\"date\": \"2026-08-26\", \"changeType\": \"EXTRA_CLASS\", \"courseName\": \"Java Programming\", \"startTime\": \"14:00\", \"endTime\": \"15:30\", \"reason\": \"Extra practical session\"}\n" +
                "]";

        btnPasteSampleTemplate.setOnClickListener(v -> etJsonContent.setText(sampleTemplate));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Import", (dialog, which) -> {
                    String json = etJsonContent.getText().toString().trim();
                    if (TextUtils.isEmpty(json)) {
                        Toast.makeText(this, "JSON content cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int count = ScheduleImporter.importTemporaryChangesJson(this, json);
                        Toast.makeText(this, "Successfully imported " + count + " notices!", Toast.LENGTH_SHORT).show();
                        loadNotices();
                        ScheduleManager.rescheduleAllAlarms(this);
                    } catch (Exception e) {
                        Toast.makeText(this, "Import error: Invalid JSON format", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDeleteClick(TemporaryScheduleChange item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Notice")
                .setMessage("Remove this schedule notice?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTemporaryScheduleChange(item.getId());
                    loadNotices();
                    ScheduleManager.rescheduleAllAlarms(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
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
