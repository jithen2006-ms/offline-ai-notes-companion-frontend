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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.lecturecap.R;
import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.OcrElement;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.utils.AiVisionParser;
import com.lecturecap.utils.DocumentStructureAnalyzer;
import com.lecturecap.ui.adapter.TimetableAdapter;
import android.net.Uri;
import android.util.Log;
import android.app.ProgressDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.button.MaterialButton;
import com.lecturecap.utils.OcrHelper;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.ScheduleOcrParser;
import com.lecturecap.utils.TextImportParser;
import com.lecturecap.utils.TimeUtils;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TimetableActivity extends AppCompatActivity implements TimetableAdapter.OnTimetableActionListener {

    private TabLayout tabDays;
    private RecyclerView rvTimetable;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabAddCourse;
    private MaterialButton btnImportTimetableOcr;
    private MaterialButton btnImportTimetableText;
    private TimetableAdapter adapter;
    private DatabaseHelper dbHelper;

    private String selectedDay = "Monday";

    private final ActivityResultLauncher<String[]> ocrFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    processTimetableOcr(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        dbHelper = DatabaseHelper.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.timetable_title);
        }

        tabDays = findViewById(R.id.tabDays);
        rvTimetable = findViewById(R.id.rvTimetable);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        fabAddCourse = findViewById(R.id.fabAddCourse);
        btnImportTimetableOcr = findViewById(R.id.btnImportTimetableOcr);
        btnImportTimetableText = findViewById(R.id.btnImportTimetableText);

        MaterialButton btnClearAllTimetable = findViewById(R.id.btnClearAllTimetable);

        setupRecyclerView();
        setupDayTabs();

        fabAddCourse.setOnClickListener(v -> showAddOrEditDialog(null));
        if (btnImportTimetableOcr != null) {
            btnImportTimetableOcr.setOnClickListener(v -> ocrFilePickerLauncher.launch(new String[]{"image/*", "application/pdf"}));
        }
        if (btnImportTimetableText != null) {
            btnImportTimetableText.setOnClickListener(v -> showTextImportDialog());
        }
        if (btnClearAllTimetable != null) {
            btnClearAllTimetable.setOnClickListener(v -> showClearAllConfirmationDialog());
        }
    }

    private void showClearAllConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Timetable Classes")
                .setMessage("Are you sure you want to clear all timetable classes?")
                .setPositiveButton("Yes, Clear All", (dialog, which) -> {
                    dbHelper.clearAllTimetableItems();
                    ScheduleManager.rescheduleAllAlarms(this);
                    loadTimetableForDay(selectedDay);
                    Toast.makeText(this, "Cleared all timetable classes", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI Prompt", aiPrompt);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClearText.setOnClickListener(v -> etPastedText.setText(""));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .create();

        btnImportTextSubmit.setOnClickListener(v -> {
            String text = etPastedText.getText() != null ? etPastedText.getText().toString().trim() : "";
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Please paste schedule text", Toast.LENGTH_SHORT).show();
                return;
            }
            List<TimetableItem> items = TextImportParser.parseWeeklyTimetableText(text);
            if (items.isEmpty()) {
                Toast.makeText(this, "No class schedules detected in pasted text.", Toast.LENGTH_LONG).show();
            } else {
                dialog.dismiss();
                showWeeklyTimetableReviewDialog(items);
            }
        });

        dialog.show();
    }

    private void setupRecyclerView() {
        rvTimetable.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableAdapter(this);
        rvTimetable.setAdapter(adapter);
    }

    private void setupDayTabs() {
        tabDays.removeAllTabs();
        String currentDay = TimeUtils.getCurrentDayOfWeek();
        int initialSelectIndex = 0;

        for (int i = 0; i < TimeUtils.DAYS_OF_WEEK.length; i++) {
            String day = TimeUtils.DAYS_OF_WEEK[i];
            TabLayout.Tab tab = tabDays.newTab().setText(day);
            tabDays.addTab(tab);
            if (day.equalsIgnoreCase(currentDay)) {
                initialSelectIndex = i;
            }
        }

        tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() != null) {
                    selectedDay = tab.getText().toString();
                    loadTimetableForDay(selectedDay);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        TabLayout.Tab targetTab = tabDays.getTabAt(initialSelectIndex);
        if (targetTab != null) {
            targetTab.select();
            selectedDay = TimeUtils.DAYS_OF_WEEK[initialSelectIndex];
        }
        loadTimetableForDay(selectedDay);
    }

    private void loadTimetableForDay(String day) {
        List<TimetableItem> items = dbHelper.getTimetableByDay(day);
        adapter.setItems(items);

        if (items.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTimetable.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTimetable.setVisibility(View.VISIBLE);
        }
    }

    private void showAddOrEditDialog(TimetableItem itemToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_timetable, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        Spinner spinnerDay = dialogView.findViewById(R.id.spinnerDay);
        Button btnStartTime = dialogView.findViewById(R.id.btnStartTime);
        Button btnEndTime = dialogView.findViewById(R.id.btnEndTime);

        final String[] startTimeHolder = {"09:00"};
        final String[] endTimeHolder = {"10:00"};

        boolean isEdit = (itemToEdit != null);

        if (isEdit) {
            tvDialogTitle.setText(R.string.edit);
            etCourseName.setText(itemToEdit.getCourseName());
            startTimeHolder[0] = itemToEdit.getStartTime();
            endTimeHolder[0] = itemToEdit.getEndTime();
            btnStartTime.setText(itemToEdit.getStartTime());
            btnEndTime.setText(itemToEdit.getEndTime());

            for (int i = 0; i < TimeUtils.DAYS_OF_WEEK.length; i++) {
                if (TimeUtils.DAYS_OF_WEEK[i].equalsIgnoreCase(itemToEdit.getDayOfWeek())) {
                    spinnerDay.setSelection(i);
                    break;
                }
            }
        } else {
            tvDialogTitle.setText(R.string.add_class);
            btnStartTime.setText(startTimeHolder[0]);
            btnEndTime.setText(endTimeHolder[0]);
            for (int i = 0; i < TimeUtils.DAYS_OF_WEEK.length; i++) {
                if (TimeUtils.DAYS_OF_WEEK[i].equalsIgnoreCase(selectedDay)) {
                    spinnerDay.setSelection(i);
                    break;
                }
            }
        }

        btnStartTime.setOnClickListener(v -> showTimePicker((hourOfDay, minute) -> {
            String formatted = TimeUtils.formatTime(hourOfDay, minute);
            startTimeHolder[0] = formatted;
            btnStartTime.setText(formatted);
        }, startTimeHolder[0]));

        btnEndTime.setOnClickListener(v -> showTimePicker((hourOfDay, minute) -> {
            String formatted = TimeUtils.formatTime(hourOfDay, minute);
            endTimeHolder[0] = formatted;
            btnEndTime.setText(formatted);
        }, endTimeHolder[0]));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String courseName = etCourseName.getText().toString().trim();
                    if (TextUtils.isEmpty(courseName)) {
                        Toast.makeText(TimetableActivity.this, "Please enter a course name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String day = spinnerDay.getSelectedItem().toString();
                    String start = startTimeHolder[0];
                    String end = endTimeHolder[0];

                    if (isEdit) {
                        itemToEdit.setCourseName(courseName);
                        itemToEdit.setDayOfWeek(day);
                        itemToEdit.setStartTime(start);
                        itemToEdit.setEndTime(end);
                        dbHelper.updateTimetableItem(itemToEdit);
                        Toast.makeText(TimetableActivity.this, "Course updated", Toast.LENGTH_SHORT).show();
                    } else {
                        TimetableItem newItem = new TimetableItem(courseName, day, start, end);
                        dbHelper.addTimetableItem(newItem);
                        Toast.makeText(TimetableActivity.this, "Course added", Toast.LENGTH_SHORT).show();
                    }

                    loadTimetableForDay(selectedDay);
                    ScheduleManager.rescheduleAllAlarms(TimetableActivity.this);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    interface OnTimeSelectedListener {
        void onTimeSelected(int hourOfDay, int minute);
    }

    private void showTimePicker(OnTimeSelectedListener listener, String defaultTime) {
        int hour = 9;
        int minute = 0;
        try {
            String[] parts = defaultTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}

        new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            if (listener != null) {
                listener.onTimeSelected(hourOfDay, minuteOfHour);
            }
        }, hour, minute, true).show();
    }

    @Override
    public void onEdit(TimetableItem item) {
        showAddOrEditDialog(item);
    }

    @Override
    public void onDelete(TimetableItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete \"" + item.getCourseName() + "\" from your timetable?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTimetableItem(item.getId());
                    loadTimetableForDay(selectedDay);
                    ScheduleManager.rescheduleAllAlarms(TimetableActivity.this);
                    Toast.makeText(TimetableActivity.this, "Course removed", Toast.LENGTH_SHORT).show();
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

    private void processTimetableOcr(Uri uri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Analyzing document structure & extracting schedule with OCR...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        OcrHelper.processUri(this, uri, new OcrHelper.OcrCallback() {
            @Override
            public void onSuccess(String rawText, List<String> lines, List<OcrElement> spatialElements) {
                progressDialog.dismiss();

                DocumentStructureAnalyzer.AnalysisResult analysis = DocumentStructureAnalyzer.analyzeDocument(spatialElements, lines);
                Log.d("TimetableActivity", "Document Analysis: " + analysis.toString());

                if (analysis.isAmbiguous) {
                    showAmbiguousDocumentTypeDialog(spatialElements, lines);
                    return;
                }

                if (analysis.documentType == DocumentStructureAnalyzer.DocumentType.ACADEMIC_CALENDAR) {
                    List<AcademicCalendarItem> calItems = AiVisionParser.parseAcademicCalendarStructured(lines);
                    showAcademicCalendarImportPrompt(calItems);
                    return;
                } else if (analysis.documentType == DocumentStructureAnalyzer.DocumentType.EXAM_TIMETABLE) {
                    List<ExamScheduleItem> examItems = AiVisionParser.parseExamTimetableStructured(lines);
                    showExamImportPrompt(examItems);
                    return;
                }

                List<TimetableItem> extracted = AiVisionParser.parseWeeklyTimetableStructured(spatialElements, lines);
                if (extracted.isEmpty()) {
                    Toast.makeText(TimetableActivity.this, "OCR complete. No timetable patterns found. You can add classes manually.", Toast.LENGTH_LONG).show();
                    showAddOrEditDialog(null);
                } else {
                    showWeeklyTimetableReviewDialog(extracted);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(TimetableActivity.this, "OCR Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAmbiguousDocumentTypeDialog(List<OcrElement> spatialElements, List<String> lines) {
        String[] options = {
                "Weekly Timetable (Recommended)",
                "Academic Calendar & Holidays",
                "Examination Schedule"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Document Type Uncertain")
                .setMessage("Please select the correct format for this document:")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        List<TimetableItem> extracted = AiVisionParser.parseWeeklyTimetableStructured(spatialElements, lines);
                        showWeeklyTimetableReviewDialog(extracted);
                    } else if (which == 1) {
                        List<AcademicCalendarItem> calItems = AiVisionParser.parseAcademicCalendarStructured(lines);
                        showAcademicCalendarImportPrompt(calItems);
                    } else if (which == 2) {
                        List<ExamScheduleItem> examItems = AiVisionParser.parseExamTimetableStructured(lines);
                        showExamImportPrompt(examItems);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAcademicCalendarImportPrompt(List<AcademicCalendarItem> items) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Academic Calendar Detected")
                .setMessage("This image appears to be an Academic Calendar with " + items.size() + " date event(s). Import them into your College Academic Calendar?")
                .setPositiveButton("Import Calendar", (dialog, which) -> {
                    for (AcademicCalendarItem item : items) {
                        dbHelper.addAcademicCalendarItem(item);
                    }
                    ScheduleManager.rescheduleAllAlarms(TimetableActivity.this);
                    Toast.makeText(TimetableActivity.this, "Imported " + items.size() + " calendar events!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showExamImportPrompt(List<ExamScheduleItem> items) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Exam Timetable Detected")
                .setMessage("This image appears to be an Examination Schedule with " + items.size() + " exam(s). Import them into your Exam Schedule?")
                .setPositiveButton("Import Exams", (dialog, which) -> {
                    for (ExamScheduleItem item : items) {
                        dbHelper.addExamScheduleItem(item);
                    }
                    ScheduleManager.rescheduleAllAlarms(TimetableActivity.this);
                    Toast.makeText(TimetableActivity.this, "Imported " + items.size() + " exams!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showWeeklyTimetableReviewDialog(List<TimetableItem> items) {
        StringBuilder summary = new StringBuilder("OCR Extracted " + items.size() + " lecture slot(s):\n\n");
        for (int i = 0; i < items.size(); i++) {
            TimetableItem item = items.get(i);
            summary.append(i + 1).append(". ")
                    .append(item.getCourseName()).append(" (")
                    .append(item.getDayOfWeek()).append(" ")
                    .append(item.getStartTime()).append("–").append(item.getEndTime()).append(")\n");
        }
        summary.append("\nTap 'Confirm & Save' to add these to your weekly schedule.");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Review OCR Extracted Schedule")
                .setMessage(summary.toString())
                .setPositiveButton("Confirm & Save", (dialog, which) -> {
                    for (TimetableItem item : items) {
                        dbHelper.addTimetableItem(item);
                    }
                    com.lecturecap.utils.ScheduleManager.rescheduleAllAlarms(TimetableActivity.this);
                    loadTimetableForDay(selectedDay);
                    Toast.makeText(TimetableActivity.this, "Successfully imported " + items.size() + " classes!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("+ Add Manual Class", (dialog, which) -> showAddOrEditDialog(null))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTimetableForDay(selectedDay);
    }
}
