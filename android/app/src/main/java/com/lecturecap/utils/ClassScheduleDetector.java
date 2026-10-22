package com.lecturecap.utils;

import android.content.Context;

import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.ScheduleDecisionResult;
import com.lecturecap.model.TemporaryScheduleChange;
import com.lecturecap.model.TimetableItem;

import java.util.List;

public class ClassScheduleDetector {

    /**
     * Evaluates whether a lecture should be recorded right now using the strict 4-Tier Priority Engine:
     * Tier 1: Temporary Schedule Changes / Notices
     * Tier 2: Exam Timetable
     * Tier 3: College Academic Calendar
     * Tier 4: Normal Weekly Timetable
     */
    public static ScheduleDecisionResult evaluateCurrentSchedule(Context context) {
        String currentDate = TimeUtils.getCurrentDateIso();
        String currentTime = TimeUtils.getCurrentTime24();
        String currentDay = TimeUtils.getCurrentDayOfWeek();

        return evaluateScheduleFor(context, currentDate, currentTime, currentDay);
    }

    /**
     * Parameterized evaluation for testing and custom date/time simulation.
     */
    public static ScheduleDecisionResult evaluateScheduleFor(Context context, String dateIso, String time24, String dayOfWeek) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        String effectiveDayOfWeek = dayOfWeek;

        // ================= TIER 1: TEMPORARY SCHEDULE CHANGES / NOTICES =================
        List<TemporaryScheduleChange> tempChanges = db.getTemporaryScheduleChangesForDate(dateIso);
        for (TemporaryScheduleChange change : tempChanges) {
            // Check full-day holiday notice
            if (TemporaryScheduleChange.TYPE_DATE_HOLIDAY.equalsIgnoreCase(change.getChangeType())) {
                return new ScheduleDecisionResult(
                        false,
                        ScheduleDecisionResult.STATUS_BLOCKED_HOLIDAY,
                        null,
                        null,
                        null,
                        "No Recording: Temporary Holiday Declared (" + change.getReason() + ")",
                        1
                );
            }

            // Check special working day substitution
            if (TemporaryScheduleChange.TYPE_SPECIAL_WORKING_DAY.equalsIgnoreCase(change.getChangeType())) {
                if (change.getSubstituteDay() != null && !change.getSubstituteDay().isEmpty()) {
                    effectiveDayOfWeek = change.getSubstituteDay();
                }
            }

            // Check time-specific notices
            if (change.getStartTime() != null && change.getEndTime() != null) {
                if (isTimeInRange(time24, change.getStartTime(), change.getEndTime())) {
                    if (TemporaryScheduleChange.TYPE_CLASS_CANCELLED.equalsIgnoreCase(change.getChangeType())) {
                        return new ScheduleDecisionResult(
                                false,
                                ScheduleDecisionResult.STATUS_BLOCKED_CANCELLED,
                                change.getCourseName(),
                                change.getStartTime(),
                                change.getEndTime(),
                                "No Recording: " + change.getCourseName() + " Cancelled (" + change.getReason() + ")",
                                1
                        );
                    } else if (TemporaryScheduleChange.TYPE_EXTRA_CLASS.equalsIgnoreCase(change.getChangeType())
                            || TemporaryScheduleChange.TYPE_CLASS_RESCHEDULED.equalsIgnoreCase(change.getChangeType())) {
                        return new ScheduleDecisionResult(
                                true,
                                ScheduleDecisionResult.STATUS_RECORD_EXTRA,
                                change.getCourseName(),
                                change.getStartTime(),
                                change.getEndTime(),
                                "Recording Extra Class: " + change.getCourseName() + " (" + change.getReason() + ")",
                                1
                        );
                    }
                }
            }
        }

        // ================= TIER 2: EXAM TIMETABLE =================
        List<ExamScheduleItem> exams = db.getExamScheduleItemsForDate(dateIso);
        for (ExamScheduleItem exam : exams) {
            if (isTimeInRange(time24, exam.getStartTime(), exam.getEndTime())) {
                return new ScheduleDecisionResult(
                        false,
                        ScheduleDecisionResult.STATUS_BLOCKED_EXAM,
                        exam.getExamName(),
                        exam.getStartTime(),
                        exam.getEndTime(),
                        "No Recording: " + exam.getExamName() + " in Progress (" + exam.getRoom() + ")",
                        2
                );
            }
        }

        // ================= TIER 3: COLLEGE ACADEMIC CALENDAR =================
        AcademicCalendarItem acadItem = db.getAcademicCalendarItemForDate(dateIso);
        if (acadItem != null) {
            if (acadItem.isHoliday()) {
                return new ScheduleDecisionResult(
                        false,
                        ScheduleDecisionResult.STATUS_BLOCKED_HOLIDAY,
                        null,
                        null,
                        null,
                        "No Recording: College Holiday (" + acadItem.getDescription() + ")",
                        3
                );
            }
            if (acadItem.isSpecialWorkingDay() && acadItem.getEffectiveDay() != null) {
                effectiveDayOfWeek = acadItem.getEffectiveDay();
            }
        }

        // ================= TIER 4: NORMAL WEEKLY TIMETABLE =================
        List<TimetableItem> scheduledClasses = db.getTimetableByDay(effectiveDayOfWeek);
        for (TimetableItem item : scheduledClasses) {
            if (isTimeInRange(time24, item.getStartTime(), item.getEndTime())) {
                // Double-check if this specific class was cancelled by a notice without exact time match
                boolean isCancelled = false;
                for (TemporaryScheduleChange change : tempChanges) {
                    if (TemporaryScheduleChange.TYPE_CLASS_CANCELLED.equalsIgnoreCase(change.getChangeType())
                            && item.getCourseName().equalsIgnoreCase(change.getCourseName())) {
                        isCancelled = true;
                        break;
                    }
                }

                if (isCancelled) {
                    return new ScheduleDecisionResult(
                            false,
                            ScheduleDecisionResult.STATUS_BLOCKED_CANCELLED,
                            item.getCourseName(),
                            item.getStartTime(),
                            item.getEndTime(),
                            "No Recording: " + item.getCourseName() + " Cancelled by Notice",
                            1
                    );
                }

                return new ScheduleDecisionResult(
                        true,
                        ScheduleDecisionResult.STATUS_RECORD_NORMAL,
                        item.getCourseName(),
                        item.getStartTime(),
                        item.getEndTime(),
                        "Class Scheduled: " + item.getCourseName(),
                        4
                );
            }
        }

        // Default: No class active
        return new ScheduleDecisionResult(
                false,
                ScheduleDecisionResult.STATUS_NO_CLASS,
                null,
                null,
                null,
                "No class scheduled right now",
                4
        );
    }

    public static TimetableItem getActiveClass(Context context) {
        ScheduleDecisionResult result = evaluateCurrentSchedule(context);
        if (result.isShouldRecord() && result.getCourseName() != null) {
            return new TimetableItem(
                    result.getCourseName(),
                    TimeUtils.getCurrentDayOfWeek(),
                    result.getStartTime(),
                    result.getEndTime()
            );
        }
        return null;
    }

    public static TimetableItem getNextUpcomingClass(Context context) {
        String currentDay = TimeUtils.getCurrentDayOfWeek();
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        List<TimetableItem> classes = db.getTimetableByDay(currentDay);
        String currentTime = TimeUtils.getCurrentTime24();

        for (TimetableItem item : classes) {
            if (item.getStartTime().compareTo(currentTime) > 0) {
                return item;
            }
        }
        return null;
    }

    private static boolean isTimeInRange(String current, String start, String end) {
        return current.compareTo(start) >= 0 && current.compareTo(end) < 0;
    }
}
