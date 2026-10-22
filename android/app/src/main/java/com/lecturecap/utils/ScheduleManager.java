package com.lecturecap.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.TemporaryScheduleChange;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.receiver.ScheduleAlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleManager {

    private static final String TAG = "ScheduleManager";

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public static void rescheduleAllAlarms(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        if (!sessionManager.isAutoRecordEnabled()) {
            Log.d(TAG, "Auto-recording disabled in settings. Skipping alarm scheduling.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        DatabaseHelper db = DatabaseHelper.getInstance(context);

        // 1. Schedule weekly timetable alarms
        List<TimetableItem> allItems = db.getAllTimetableItems();
        for (TimetableItem item : allItems) {
            scheduleClassAlarm(context, alarmManager, item);
        }

        // 2. Schedule temporary extra / rescheduled class alarms
        List<TemporaryScheduleChange> tempChanges = db.getAllTemporaryScheduleChanges();
        for (TemporaryScheduleChange temp : tempChanges) {
            if (TemporaryScheduleChange.TYPE_EXTRA_CLASS.equalsIgnoreCase(temp.getChangeType())
                    || TemporaryScheduleChange.TYPE_CLASS_RESCHEDULED.equalsIgnoreCase(temp.getChangeType())) {
                scheduleTempAlarm(context, alarmManager, temp);
            }
        }
    }

    private static void scheduleClassAlarm(Context context, AlarmManager alarmManager, TimetableItem item) {
        try {
            if (item.getStartTime() == null || !item.getStartTime().contains(":")) return;

            String[] timeParts = item.getStartTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            int targetDayOfWeek = getCalendarDayOfWeek(item.getDayOfWeek());

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // Adjust day of week to next future occurrence
            while (calendar.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek || calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, ScheduleAlarmReceiver.class);
            intent.setAction(ScheduleAlarmReceiver.ACTION_CHECK_AND_RECORD);

            int requestCode = (int) (item.getId() * 100 + targetDayOfWeek);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            setExactAlarm(alarmManager, calendar.getTimeInMillis(), pendingIntent);
            Log.d(TAG, "Scheduled weekly alarm for " + item.getCourseName() + " on " + item.getDayOfWeek() + " at " + item.getStartTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm for " + item.getCourseName(), e);
        }
    }

    private static void scheduleTempAlarm(Context context, AlarmManager alarmManager, TemporaryScheduleChange temp) {
        try {
            if (temp.getDate() == null || temp.getStartTime() == null || !temp.getStartTime().contains(":")) return;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date targetDate = sdf.parse(temp.getDate() + " " + temp.getStartTime());
            if (targetDate == null || targetDate.getTime() <= System.currentTimeMillis()) {
                return; // Past event
            }

            Intent intent = new Intent(context, ScheduleAlarmReceiver.class);
            intent.setAction(ScheduleAlarmReceiver.ACTION_CHECK_AND_RECORD);

            int requestCode = (int) (temp.getId() * 1000 + 77);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            setExactAlarm(alarmManager, targetDate.getTime(), pendingIntent);
            Log.d(TAG, "Scheduled temp alarm for " + temp.getCourseName() + " on " + temp.getDate() + " at " + temp.getStartTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling temp alarm for " + temp.getCourseName(), e);
        }
    }

    private static void setExactAlarm(AlarmManager alarmManager, long triggerAtMillis, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private static int getCalendarDayOfWeek(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }
}

