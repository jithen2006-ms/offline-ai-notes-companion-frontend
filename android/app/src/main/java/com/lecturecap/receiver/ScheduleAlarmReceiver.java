package com.lecturecap.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.lecturecap.model.ScheduleDecisionResult;
import com.lecturecap.service.LectureRecordingService;
import com.lecturecap.utils.ClassScheduleDetector;
import com.lecturecap.utils.ScheduleManager;
import com.lecturecap.utils.SessionManager;
import com.lecturecap.utils.TimeUtils;

public class ScheduleAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "ScheduleAlarmReceiver";
    public static final String ACTION_CHECK_AND_RECORD = "com.lecturecap.action.CHECK_AND_RECORD";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log.d(TAG, "onReceive action: " + action);

        // Handle Boot & System Package Updates
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.d(TAG, "Device boot completed or package replaced. Re-synchronizing all schedule alarms...");
            ScheduleManager.rescheduleAllAlarms(context);
            return;
        }

        SessionManager sessionManager = new SessionManager(context);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAutoRecordEnabled()) {
            Log.d(TAG, "Auto-recording skipped (Logged in: " + sessionManager.isLoggedIn() + ", Auto-record: " + sessionManager.isAutoRecordEnabled() + ")");
            return;
        }

        // Run strict 4-Tier priority evaluation
        ScheduleDecisionResult decision = ClassScheduleDetector.evaluateCurrentSchedule(context);
        Log.d(TAG, "Schedule Decision Result: " + decision.getDecisionStatus() + " -> " + decision.getExplanation());

        if (decision.isShouldRecord() && !LectureRecordingService.isRunning()) {
            Log.d(TAG, "Starting automatic recording for: " + decision.getCourseName());

            Intent serviceIntent = new Intent(context, LectureRecordingService.class);
            serviceIntent.setAction(LectureRecordingService.ACTION_START_RECORDING);
            serviceIntent.putExtra(LectureRecordingService.EXTRA_COURSE_NAME, decision.getCourseName());
            serviceIntent.putExtra(LectureRecordingService.EXTRA_START_TIME, decision.getStartTime());
            serviceIntent.putExtra(LectureRecordingService.EXTRA_END_TIME, decision.getEndTime());
            serviceIntent.putExtra(LectureRecordingService.EXTRA_DATE, TimeUtils.getCurrentDateIso());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            Log.d(TAG, "Automatic recording blocked or no active class: " + decision.getExplanation());
        }

        // Keep future recurring alarms continuously scheduled
        ScheduleManager.rescheduleAllAlarms(context);
    }
}
