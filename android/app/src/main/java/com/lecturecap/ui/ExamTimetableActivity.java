package com.lecturecap.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.ui.adapter.ExamTimetableAdapter;
import com.lecturecap.utils.ScheduleImporter;
import android.content.Context;
import android.net.Uri;
import android.app.ProgressDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.lecturecap.utils.OcrHelper;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.ScheduleOcrParser;
import com.lecturecap.utils.TimeUtils;

import java.util.List;

public class ExamTimetableActivity extends AppCompatActivity implements ExamTimetableAdapter.OnDeleteClickListener {

    private RecyclerView rvExams;
    private LinearLayout layoutEmptyExams;
    private FloatingActionButton fabAddExam;
    private MaterialButton btnImportExams;
    private MaterialButton btnImportExamsOcr;
    private ExamTimetableAdapter adapter;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<String[]> ocrFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    processExamsOcr(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_timetable);

        dbHelper = DatabaseHelper.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("College Examination Schedule");
        }

        rvExams = findViewById(R.id.rvExams);
        layoutEmptyExams = findViewById(R.id.layoutEmptyExams);
        fabAddExam = findViewById(R.id.fabAddExam);
        btnImportExams = findViewById(R.id.btnImportExams);
        btnImportExamsOcr = findViewById(R.id.btnImportExamsOcr);
        MaterialButton btnImportExamsText = findViewById(R.id.btnImportExamsText);

        rvExams.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExamTimetableAdapter(this);
        rvExams.setAdapter(adapter);

        fabAddExam.setOnClickListener(v -> showAddExamDialog());
        btnImportExams.setOnClickListener(v -> showImportDialog());
        if (btnImportExamsOcr != null) {
            btnImportExamsOcr.setOnClickListener(v -> ocrFilePickerLauncher.launch(new String[]{"image/*", "application/pdf"}));
        }
        if (btnImportExamsText != null) {
            btnImportExamsText.setOnClickListener(v -> showTextImportDialog());
        }

        loadExams();
    }

    private void showTextImportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_text_import, null);
        Button btnCopyAiPrompt = dialogView.findViewById(R.id.btnCopyAiPrompt);
        EditText etPastedText = dialogView.findViewById(R.id.etPastedText);
        Button btnClearText = dialogView.findViewById(R.id.btnClearText);
        Button btnImportTextSubmit = dialogView.findViewById(R.id.btnImportTextSubmit);

        String aiPrompt = "Read this document carefully and extract ALL relevant information exactly as shown.\n\n" +
                "Do not guess, invent, summarize, or omit information.\n\n" +
                "For a weekly timetable, output every actual class in this format:\n" +
                "Day | Start Time | End Time | Class/Subject\n\n" +
                "Use AM/PM explicitly and preserve the exact times from the document.\n\n" +
                "Do NOT include BREAK, LUNCH, RECESS, FREE PERIOD, or similar non-class periods.\n\n" +
                "Include labs, tutorials and projects as classes.\n\n" +
                "If a class spans multiple timetable slots, use its actual full start and end time.\n\n" +
                "For an academic calendar, output:\n" +
                "Date/Date Range | Event/Holiday\n\n" +
                "For an exam timetable, output:\n" +
                "Date | Start Time | End Time | Subject/Exam\n\n" +
                "Extract ALL entries visible in the document.\n" +
                "Do not add any information that is not present in the document.";

        btnCopyAiPrompt.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("AI Prompt", aiPrompt);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClearText.setOnClickListener(v -> etPastedText.setText(""));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .create();

        btnImportTextSubmit.setOnClickListener(v -> {
            String text = etPastedText.getText() != null ? etPastedText.getText().toString().trim() : "";
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Please paste exam schedule text", Toast.LENGTH_SHORT).show();
                return;
            }
            List<ExamScheduleItem> items = com.lecturecap.utils.TextImportParser.parseExamTimetableText(text);
            if (items.isEmpty()) {
                Toast.makeText(this, "No examination schedules detected in pasted text.", Toast.LENGTH_LONG).show();
            } else {
                dialog.dismiss();
                showExamsReviewDialog(items);
            }
        });

        dialog.show();
    }

    private void loadExams() {
        List<ExamScheduleItem> list = dbHelper.getAllExamScheduleItems();
        adapter.setItems(list);

        if (list.isEmpty()) {
            layoutEmptyExams.setVisibility(View.VISIBLE);
            rvExams.setVisibility(View.GONE);
        } else {
            layoutEmptyExams.setVisibility(View.GONE);
            rvExams.setVisibility(View.VISIBLE);
        }
    }

    private void showAddExamDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_exam, null);
        EditText etExamName = dialogView.findViewById(R.id.etExamName);
        EditText etExamDate = dialogView.findViewById(R.id.etExamDate);
        EditText etExamRoom = dialogView.findViewById(R.id.etExamRoom);
        Button btnExamStartTime = dialogView.findViewById(R.id.btnExamStartTime);
        Button btnExamEndTime = dialogView.findViewById(R.id.btnExamEndTime);

        final String[] startTime = {"10:00"};
        final String[] endTime = {"12:00"};

        btnExamStartTime.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, min) -> {
            startTime[0] = TimeUtils.formatTime(hour, min);
            btnExamStartTime.setText(startTime[0]);
        }, 10, 0, true).show());

        btnExamEndTime.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, min) -> {
            endTime[0] = TimeUtils.formatTime(hour, min);
            btnExamEndTime.setText(endTime[0]);
        }, 12, 0, true).show());

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etExamName.getText().toString().trim();
                    String date = etExamDate.getText().toString().trim();
                    String room = etExamRoom.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(date)) {
                        Toast.makeText(this, "Please enter exam name and date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ExamScheduleItem item = new ExamScheduleItem(name, date, startTime[0], endTime[0], room);
                    dbHelper.addExamScheduleItem(item);
                    Toast.makeText(this, "Exam added", Toast.LENGTH_SHORT).show();
                    loadExams();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showImportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_schedule, null);
        EditText etJsonContent = dialogView.findViewById(R.id.etJsonContent);
        Button btnPasteSampleTemplate = dialogView.findViewById(R.id.btnPasteSampleTemplate);

        String sampleTemplate = "[\n" +
                "  {\"examName\": \"Operating Systems End-Term\", \"date\": \"2026-09-25\", \"startTime\": \"09:30\", \"endTime\": \"12:30\", \"room\": \"Hall 101\"},\n" +
                "  {\"examName\": \"Software Engineering Exam\", \"date\": \"2026-09-28\", \"startTime\": \"14:00\", \"endTime\": \"17:00\", \"room\": \"Hall 102\"}\n" +
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
                        int count = ScheduleImporter.importExamScheduleJson(this, json);
                        Toast.makeText(this, "Successfully imported " + count + " exams!", Toast.LENGTH_SHORT).show();
                        loadExams();
                    } catch (Exception e) {
                        Toast.makeText(this, "Import error: Invalid JSON format", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDeleteClick(ExamScheduleItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Exam")
                .setMessage("Remove \"" + item.getExamName() + "\" from schedule?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteExamScheduleItem(item.getId());
                    loadExams();
                    ScheduleManager.rescheduleAllAlarms(ExamTimetableActivity.this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processExamsOcr(Uri uri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Analyzing exam schedule image/PDF with OCR...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        OcrHelper.processUri(this, uri, new OcrHelper.OcrCallback() {
            @Override
            public void onSuccess(String rawText, List<String> lines, List<com.lecturecap.model.OcrElement> spatialElements) {
                progressDialog.dismiss();
                List<ExamScheduleItem> extracted = ScheduleOcrParser.parseExamTimetable(lines);
                if (extracted.isEmpty()) {
                    Toast.makeText(ExamTimetableActivity.this, "OCR complete. No exam schedules automatically parsed. Add exam manually.", Toast.LENGTH_LONG).show();
                    showAddExamDialog();
                } else {
                    showExamsReviewDialog(extracted);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(ExamTimetableActivity.this, "OCR Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showExamsReviewDialog(List<ExamScheduleItem> items) {
        StringBuilder summary = new StringBuilder("OCR Extracted " + items.size() + " exam(s):\n\n");
        for (int i = 0; i < items.size(); i++) {
            ExamScheduleItem item = items.get(i);
            summary.append(i + 1).append(". ")
                    .append(item.getExamName()).append(" (")
                    .append(item.getDate()).append(" ").append(item.getStartTime()).append("–").append(item.getEndTime()).append(")")
                    .append(" [Room: ").append(item.getRoom()).append("]\n");
        }
        summary.append("\nTap 'Confirm & Save' to add these exams to your Exam Schedule.");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Review OCR Exam Schedule")
                .setMessage(summary.toString())
                .setPositiveButton("Confirm & Save", (dialog, which) -> {
                    for (ExamScheduleItem item : items) {
                        dbHelper.addExamScheduleItem(item);
                    }
                    ScheduleManager.rescheduleAllAlarms(ExamTimetableActivity.this);
                    loadExams();
                    Toast.makeText(ExamTimetableActivity.this, "Successfully imported " + items.size() + " exams!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("+ Add Exam", (dialog, which) -> showAddExamDialog())
                .setNegativeButton(R.string.cancel, null)
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
