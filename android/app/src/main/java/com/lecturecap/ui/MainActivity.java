package com.lecturecap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.lecturecap.R;
import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.ScheduleDecisionResult;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.service.LectureRecordingService;
import com.lecturecap.stt.OfflineSttModelManager;
import com.lecturecap.utils.ClassScheduleDetector;
import com.lecturecap.utils.PermissionHelper;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.SessionManager;
import com.lecturecap.utils.TimeUtils;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcomeUser;
    private TextView tvStudentEmail;
    private TextView tvCurrentDay;
    private TextView tvCurrentDate;
    private TextView tvStatusBadge;
    private TextView tvCurrentCourseName;
    private TextView tvCurrentClassTime;
    private TextView tvDecisionRationale;
    private MaterialButton btnQuickRecord;

    private MaterialCardView cardTimetable;
    private MaterialCardView cardAcademicCalendar;
    private MaterialCardView cardExamTimetable;
    private MaterialCardView cardTempChanges;
    private MaterialCardView cardHistory;
    private MaterialCardView cardSettings;

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private ScheduleDecisionResult currentDecision = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupListeners();
        updateDashboardData();
        PermissionHelper.requestRequiredPermissions(this);
    }

    private void initViews() {
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvStudentEmail = findViewById(R.id.tvStudentEmail);
        tvCurrentDay = findViewById(R.id.tvCurrentDay);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvCurrentCourseName = findViewById(R.id.tvCurrentCourseName);
        tvCurrentClassTime = findViewById(R.id.tvCurrentClassTime);
        tvDecisionRationale = findViewById(R.id.tvDecisionRationale);
        btnQuickRecord = findViewById(R.id.btnQuickRecord);

        cardTimetable = findViewById(R.id.cardTimetable);
        cardAcademicCalendar = findViewById(R.id.cardAcademicCalendar);
        cardExamTimetable = findViewById(R.id.cardExamTimetable);
        cardTempChanges = findViewById(R.id.cardTempChanges);
        cardHistory = findViewById(R.id.cardHistory);
        cardSettings = findViewById(R.id.cardSettings);
    }

    private void setupListeners() {
        cardTimetable.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TimetableActivity.class));
        });

        cardAcademicCalendar.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AcademicCalendarActivity.class));
        });

        cardExamTimetable.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ExamTimetableActivity.class));
        });

        cardTempChanges.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TemporaryChangesActivity.class));
        });

        cardHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LectureHistoryActivity.class));
        });

        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        btnQuickRecord.setOnClickListener(v -> {
            launchRecordingScreen();
        });
    }

    private void launchRecordingScreen() {
        Intent intent = new Intent(MainActivity.this, RecordingActivity.class);
        if (currentDecision != null && currentDecision.isShouldRecord() && currentDecision.getCourseName() != null) {
            intent.putExtra(LectureRecordingService.EXTRA_COURSE_NAME, currentDecision.getCourseName());
            intent.putExtra(LectureRecordingService.EXTRA_START_TIME, currentDecision.getStartTime());
            intent.putExtra(LectureRecordingService.EXTRA_END_TIME, currentDecision.getEndTime());
        } else {
            intent.putExtra(LectureRecordingService.EXTRA_COURSE_NAME, "General Lecture");
            intent.putExtra(LectureRecordingService.EXTRA_START_TIME, TimeUtils.getCurrentTime24());
            intent.putExtra(LectureRecordingService.EXTRA_END_TIME, "23:59");
        }
        intent.putExtra(LectureRecordingService.EXTRA_DATE, TimeUtils.getCurrentDateIso());
        startActivity(intent);
    }

    private void updateDashboardData() {
        String studentName = sessionManager.getUserName();
        String studentEmail = sessionManager.getUserEmail();

        tvWelcomeUser.setText("Welcome back, " + studentName);
        tvStudentEmail.setText(studentEmail);

        String currentDay = TimeUtils.getCurrentDayOfWeek();
        String currentDate = TimeUtils.getCurrentDateFormatted();

        tvCurrentDay.setText(currentDay);
        tvCurrentDate.setText(currentDate);

        // Check active recording state
        if (LectureRecordingService.isRunning()) {
            tvStatusBadge.setText("● RECORDING ACTIVE");
            tvStatusBadge.setTextColor(getColor(R.color.status_recording));
            tvCurrentCourseName.setText(LectureRecordingService.getActiveCourseName());
            tvCurrentClassTime.setText("Duration: " + TimeUtils.formatDuration(LectureRecordingService.getElapsedSeconds()));
            tvDecisionRationale.setText("Foreground Recording active with ongoing notification");
            btnQuickRecord.setText("View Live Recording");
            return;
        }

        // Run 4-tier schedule priority evaluation
        currentDecision = ClassScheduleDetector.evaluateCurrentSchedule(this);
        tvDecisionRationale.setText("Priority Result: " + currentDecision.getExplanation());

        if (currentDecision.isShouldRecord()) {
            if (ScheduleDecisionResult.STATUS_RECORD_EXTRA.equals(currentDecision.getDecisionStatus())) {
                tvStatusBadge.setText("EXTRA CLASS");
            } else {
                tvStatusBadge.setText("CLASS SCHEDULED");
            }
            tvStatusBadge.setTextColor(getColor(R.color.status_active));
            tvCurrentCourseName.setText(currentDecision.getCourseName());
            tvCurrentClassTime.setText(currentDecision.getTimeRange());
            btnQuickRecord.setText("Record " + currentDecision.getCourseName() + " Lecture");
        } else {
            // Recording is not scheduled or is blocked
            if (ScheduleDecisionResult.STATUS_BLOCKED_HOLIDAY.equals(currentDecision.getDecisionStatus())) {
                tvStatusBadge.setText("HOLIDAY (NO REC)");
                tvStatusBadge.setTextColor(getColor(R.color.status_warning));
                tvCurrentCourseName.setText(currentDecision.getExplanation());
                tvCurrentClassTime.setText("Class recording suppressed for holiday");
                btnQuickRecord.setText("Holiday Active");
            } else if (ScheduleDecisionResult.STATUS_BLOCKED_EXAM.equals(currentDecision.getDecisionStatus())) {
                tvStatusBadge.setText("EXAM (NO REC)");
                tvStatusBadge.setTextColor(getColor(R.color.status_recording));
                tvCurrentCourseName.setText(currentDecision.getCourseName());
                tvCurrentClassTime.setText("Exam in progress: " + currentDecision.getTimeRange());
                btnQuickRecord.setText("Exam in Progress");
            } else if (ScheduleDecisionResult.STATUS_BLOCKED_CANCELLED.equals(currentDecision.getDecisionStatus())) {
                tvStatusBadge.setText("CANCELLED (NO REC)");
                tvStatusBadge.setTextColor(getColor(R.color.status_warning));
                tvCurrentCourseName.setText(currentDecision.getCourseName() + " (Cancelled)");
                tvCurrentClassTime.setText(currentDecision.getExplanation());
                btnQuickRecord.setText("Class Cancelled");
            } else {
                // Default: No class right now
                TimetableItem nextClass = ClassScheduleDetector.getNextUpcomingClass(this);
                tvStatusBadge.setText("NO ACTIVE CLASS");
                tvStatusBadge.setTextColor(getColor(R.color.status_idle));
                tvCurrentCourseName.setText(R.string.no_class_scheduled);
                if (nextClass != null) {
                    tvCurrentClassTime.setText("Next: " + nextClass.getCourseName() + " at " + nextClass.getStartTime());
                } else {
                    tvCurrentClassTime.setText("No more lectures scheduled today");
                }
                btnQuickRecord.setText(R.string.start_manual_record);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionHelper.requestRequiredPermissions(this);
        if (sessionManager != null && sessionManager.isAutoRecordEnabled()) {
            ScheduleManager.rescheduleAllAlarms(this);
        }
        updateDashboardData();
    }
}
