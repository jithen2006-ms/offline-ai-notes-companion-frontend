package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ScheduleDecisionResult;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.utils.ClassScheduleDetector;

import org.junit.Test;

public class HolidayOverrideEndToEndTest {

    @Test
    public void testHolidayOverrideBlocksNormalClassRecording() {
        // Mock Timetable Class on Saturday 09:00 - 10:00
        TimetableItem classItem = new TimetableItem("Computer Networks", "Saturday", "09:00", "10:00");
        AcademicCalendarItem holidayItem = new AcademicCalendarItem("2026-08-22", AcademicCalendarItem.TYPE_HOLIDAY, "National Holiday", null);

        // Evaluate Holiday Override
        ScheduleDecisionResult result = evaluateScheduleMock(holidayItem, classItem, "2026-08-22", "09:15", "Saturday");

        // Expected Behavior: Recording MUST be blocked with STATUS_BLOCKED_HOLIDAY
        assertFalse(result.isShouldRecord());
        assertEquals(ScheduleDecisionResult.STATUS_BLOCKED_HOLIDAY, result.getDecisionStatus());
        assertTrue(result.getExplanation().contains("College Holiday"));
        assertEquals(3, result.getPriorityTier());
    }

    @Test
    public void testNormalClassDayWithoutHolidayStartsRecording() {
        // Mock Timetable Class on Saturday 09:00 - 10:00 with NO Holiday
        TimetableItem classItem = new TimetableItem("Computer Networks", "Saturday", "09:00", "10:00");

        // Evaluate Normal Class
        ScheduleDecisionResult result = evaluateScheduleMock(null, classItem, "2026-08-22", "09:15", "Saturday");

        // Expected Behavior: Recording MUST start with STATUS_RECORD_NORMAL
        assertTrue(result.isShouldRecord());
        assertEquals(ScheduleDecisionResult.STATUS_RECORD_NORMAL, result.getDecisionStatus());
        assertEquals("Computer Networks", result.getCourseName());
        assertEquals("09:00", result.getStartTime());
        assertEquals("10:00", result.getEndTime());
        assertEquals(4, result.getPriorityTier());
    }

    private ScheduleDecisionResult evaluateScheduleMock(AcademicCalendarItem holidayItem, TimetableItem classItem, String dateIso, String time24, String dayOfWeek) {
        if (holidayItem != null && holidayItem.isHoliday()) {
            return new ScheduleDecisionResult(
                    false,
                    ScheduleDecisionResult.STATUS_BLOCKED_HOLIDAY,
                    null,
                    null,
                    null,
                    "No Recording: College Holiday (" + holidayItem.getDescription() + ")",
                    3
            );
        }
        if (classItem != null && classItem.getDayOfWeek().equalsIgnoreCase(dayOfWeek)) {
            if (time24.compareTo(classItem.getStartTime()) >= 0 && time24.compareTo(classItem.getEndTime()) < 0) {
                return new ScheduleDecisionResult(
                        true,
                        ScheduleDecisionResult.STATUS_RECORD_NORMAL,
                        classItem.getCourseName(),
                        classItem.getStartTime(),
                        classItem.getEndTime(),
                        "Class Scheduled: " + classItem.getCourseName(),
                        4
                );
            }
        }
        return new ScheduleDecisionResult(false, ScheduleDecisionResult.STATUS_NO_CLASS, null, null, null, "No class scheduled right now", 4);
    }
}
