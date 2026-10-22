package com.lecturecap.ui;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lecturecap.R;
import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.ui.adapter.AcademicCalendarAdapter;
import android.content.Context;
import android.net.Uri;
import android.app.ProgressDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.lecturecap.utils.OcrHelper;
import com.lecturecap.utils.ScheduleImporter;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.ScheduleOcrParser;

import java.util.List;

public class AcademicCalendarActivity extends AppCompatActivity implements AcademicCalendarAdapter.OnDeleteClickListener {

    private RecyclerView rvAcademicCalendar;
    private LinearLayout layoutEmptyAcad;
    private FloatingActionButton fabAddAcadDate;
    private MaterialButton btnImportCalendar;
    private MaterialButton btnImportCalendarOcr;
    private AcademicCalendarAdapter adapter;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<String[]> ocrFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    processCalendarOcr(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academic_calendar);

        dbHelper = DatabaseHelper.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("College Academic Calendar");
        }

        rvAcademicCalendar = findViewById(R.id.rvAcademicCalendar);
        layoutEmptyAcad = findViewById(R.id.layoutEmptyAcad);
        fabAddAcadDate = findViewById(R.id.fabAddAcadDate);
        btnImportCalendar = findViewById(R.id.btnImportCalendar);
        btnImportCalendarOcr = findViewById(R.id.btnImportCalendarOcr);
        MaterialButton btnImportCalendarText = findViewById(R.id.btnImportCalendarText);

        rvAcademicCalendar.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AcademicCalendarAdapter(this);
        rvAcademicCalendar.setAdapter(adapter);

        fabAddAcadDate.setOnClickListener(v -> showAddCalendarDialog());
        btnImportCalendar.setOnClickListener(v -> showImportDialog());
        if (btnImportCalendarOcr != null) {
            btnImportCalendarOcr.setOnClickListener(v -> ocrFilePickerLauncher.launch(new String[]{"image/*", "application/pdf"}));
        }
        if (btnImportCalendarText != null) {
            btnImportCalendarText.setOnClickListener(v -> showTextImportDialog());
        }

        loadCalendarData();
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
                Toast.makeText(this, "Please paste calendar text", Toast.LENGTH_SHORT).show();
                return;
            }
            List<AcademicCalendarItem> items = com.lecturecap.utils.TextImportParser.parseAcademicCalendarText(text);
            if (items.isEmpty()) {
                Toast.makeText(this, "No academic calendar dates detected in pasted text.", Toast.LENGTH_LONG).show();
            } else {
                dialog.dismiss();
                showAcademicCalendarReviewDialog(items);
            }
        });

        dialog.show();
    }

    private void loadCalendarData() {
        List<AcademicCalendarItem> list = dbHelper.getAllAcademicCalendarItems();
        adapter.setItems(list);

        if (list.isEmpty()) {
            layoutEmptyAcad.setVisibility(View.VISIBLE);
            rvAcademicCalendar.setVisibility(View.GONE);
        } else {
            layoutEmptyAcad.setVisibility(View.GONE);
            rvAcademicCalendar.setVisibility(View.VISIBLE);
        }
    }

    private void showAddCalendarDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_academic_calendar, null);
        EditText etAcadDesc = dialogView.findViewById(R.id.etAcadDesc);
        EditText etAcadDate = dialogView.findViewById(R.id.etAcadDate);
        Spinner spinnerAcadType = dialogView.findViewById(R.id.spinnerAcadType);
        Spinner spinnerEffectiveDay = dialogView.findViewById(R.id.spinnerEffectiveDay);

        String[] types = {
                AcademicCalendarItem.TYPE_HOLIDAY,
                AcademicCalendarItem.TYPE_WORKING_DAY,
                AcademicCalendarItem.TYPE_SPECIAL_WORKING_DAY
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerAcadType.setAdapter(typeAdapter);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String desc = etAcadDesc.getText().toString().trim();
                    String date = etAcadDate.getText().toString().trim();
                    String type = spinnerAcadType.getSelectedItem().toString();
                    String effDay = AcademicCalendarItem.TYPE_SPECIAL_WORKING_DAY.equals(type)
                            ? spinnerEffectiveDay.getSelectedItem().toString() : null;

                    if (TextUtils.isEmpty(desc) || TextUtils.isEmpty(date)) {
                        Toast.makeText(this, "Please provide description and date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AcademicCalendarItem item = new AcademicCalendarItem(date, type, desc, effDay);
                    dbHelper.addAcademicCalendarItem(item);
                    Toast.makeText(this, "Calendar event added", Toast.LENGTH_SHORT).show();
                    loadCalendarData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showImportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_schedule, null);
        EditText etJsonContent = dialogView.findViewById(R.id.etJsonContent);
        Button btnPasteSampleTemplate = dialogView.findViewById(R.id.btnPasteSampleTemplate);

        String sampleTemplate = "[\n" +
                "  {\"date\": \"2026-09-05\", \"type\": \"HOLIDAY\", \"description\": \"Teachers' Day Holiday\"},\n" +
                "  {\"date\": \"2026-10-02\", \"type\": \"HOLIDAY\", \"description\": \"Gandhi Jayanti\"},\n" +
                "  {\"date\": \"2026-09-12\", \"type\": \"SPECIAL_WORKING_DAY\", \"description\": \"Saturday working as Friday\", \"effectiveDay\": \"Friday\"}\n" +
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
                        int count = ScheduleImporter.importAcademicCalendarJson(this, json);
                        Toast.makeText(this, "Successfully imported " + count + " academic dates!", Toast.LENGTH_SHORT).show();
                        loadCalendarData();
                    } catch (Exception e) {
                        Toast.makeText(this, "Import error: Invalid JSON format", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDeleteClick(AcademicCalendarItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Event")
                .setMessage("Remove \"" + item.getDescription() + "\" (" + item.getDate() + ")?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteAcademicCalendarItem(item.getId());
                    loadCalendarData();
                    ScheduleManager.rescheduleAllAlarms(AcademicCalendarActivity.this);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void processCalendarOcr(Uri uri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Analyzing academic calendar image/PDF with OCR...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        OcrHelper.processUri(this, uri, new OcrHelper.OcrCallback() {
            @Override
            public void onSuccess(String rawText, List<String> lines, List<com.lecturecap.model.OcrElement> spatialElements) {
                progressDialog.dismiss();
                List<AcademicCalendarItem> extracted = ScheduleOcrParser.parseAcademicCalendar(lines);
                if (extracted.isEmpty()) {
                    Toast.makeText(AcademicCalendarActivity.this, "OCR complete. No calendar dates automatically parsed. Add date manually.", Toast.LENGTH_LONG).show();
                    showAddCalendarDialog();
                } else {
                    showAcademicCalendarReviewDialog(extracted);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(AcademicCalendarActivity.this, "OCR Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAcademicCalendarReviewDialog(List<AcademicCalendarItem> items) {
        StringBuilder summary = new StringBuilder("OCR Extracted " + items.size() + " academic event(s):\n\n");
        for (int i = 0; i < items.size(); i++) {
            AcademicCalendarItem item = items.get(i);
            summary.append(i + 1).append(". ")
                    .append(item.getDate()).append(" [").append(item.getType()).append("]: ")
                    .append(item.getDescription());
            if (item.getEffectiveDay() != null) {
                summary.append(" (Effective: ").append(item.getEffectiveDay()).append(")");
            }
            summary.append("\n");
        }
        summary.append("\nTap 'Confirm & Save' to add these dates to the Academic Calendar.");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Review OCR Calendar Events")
                .setMessage(summary.toString())
                .setPositiveButton("Confirm & Save", (dialog, which) -> {
                    for (AcademicCalendarItem item : items) {
                        dbHelper.addAcademicCalendarItem(item);
                    }
                    ScheduleManager.rescheduleAllAlarms(AcademicCalendarActivity.this);
                    loadCalendarData();
                    Toast.makeText(AcademicCalendarActivity.this, "Successfully imported " + items.size() + " calendar events!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("+ Add Event", (dialog, which) -> showAddCalendarDialog())
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
