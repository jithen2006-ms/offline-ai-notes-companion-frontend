package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.ScheduleDecisionResult;
import com.lecturecap.model.TemporaryScheduleChange;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class SchedulePriorityTest {

    @Test
    public void testAcademicCalendarModel() {
        AcademicCalendarItem holiday = new AcademicCalendarItem(
                "2026-08-15",
                AcademicCalendarItem.TYPE_HOLIDAY,
                "Independence Day",
                null
        );
        assertTrue(holiday.isHoliday());
        assertFalse(holiday.isSpecialWorkingDay());
        assertEquals("2026-08-15", holiday.getDate());

        AcademicCalendarItem specialDay = new AcademicCalendarItem(
                "2026-08-29",
                AcademicCalendarItem.TYPE_SPECIAL_WORKING_DAY,
                "Saturday working as Monday",
                "Monday"
        );
        assertFalse(specialDay.isHoliday());
        assertTrue(specialDay.isSpecialWorkingDay());
        assertEquals("Monday", specialDay.getEffectiveDay());
    }

    @Test
    public void testExamScheduleModel() {
        ExamScheduleItem exam = new ExamScheduleItem(
                "DBMS Mid-Term Exam",
                "2026-09-15",
                "10:00",
                "12:00",
                "Exam Hall A"
        );
        assertEquals("DBMS Mid-Term Exam", exam.getExamName());
        assertEquals("2026-09-15", exam.getDate());
        assertEquals("10:00 – 12:00", exam.getTimeRange());
        assertEquals("Exam Hall A", exam.getRoom());
    }

    @Test
    public void testTemporaryScheduleChangeModel() {
        TemporaryScheduleChange cancelNotice = new TemporaryScheduleChange(
                "2026-08-25",
                TemporaryScheduleChange.TYPE_CLASS_CANCELLED,
                "DBMS",
                "09:00",
                "10:00",
                "Faculty attending conference",
                null
        );
        assertEquals(TemporaryScheduleChange.TYPE_CLASS_CANCELLED, cancelNotice.getChangeType());
        assertEquals("DBMS", cancelNotice.getCourseName());
        assertEquals("09:00 – 10:00", cancelNotice.getTimeRange());

        TemporaryScheduleChange extraClass = new TemporaryScheduleChange(
                "2026-08-26",
                TemporaryScheduleChange.TYPE_EXTRA_CLASS,
                "Java Programming",
                "14:00",
                "15:30",
                "Revision class",
                null
        );
        assertEquals(TemporaryScheduleChange.TYPE_EXTRA_CLASS, extraClass.getChangeType());
        assertEquals("Java Programming", extraClass.getCourseName());
    }

    @Test
    public void testScheduleDecisionResult() {
        ScheduleDecisionResult result = new ScheduleDecisionResult(
                true,
                ScheduleDecisionResult.STATUS_RECORD_NORMAL,
                "DBMS",
                "09:00",
                "10:00",
                "Class Scheduled: DBMS",
                4
        );
        assertTrue(result.isShouldRecord());
        assertEquals("DBMS", result.getCourseName());
        assertEquals("09:00 – 10:00", result.getTimeRange());
        assertEquals(4, result.getPriorityTier());

        ScheduleDecisionResult examResult = new ScheduleDecisionResult(
                false,
                ScheduleDecisionResult.STATUS_BLOCKED_EXAM,
                "DBMS Mid-Term Exam",
                "10:00",
                "12:00",
                "No Recording: Exam in Progress",
                2
        );
        assertFalse(examResult.isShouldRecord());
        assertEquals(2, examResult.getPriorityTier());
    }

    @Test
    public void testJsonParsingStructure() throws Exception {
        String jsonNotices = "[\n" +
                "  {\"date\": \"2026-08-25\", \"changeType\": \"CLASS_CANCELLED\", \"courseName\": \"DBMS\", \"startTime\": \"09:00\", \"endTime\": \"10:00\", \"reason\": \"Faculty attending workshop\"}\n" +
                "]";

        JSONArray array = new JSONArray(jsonNotices);
        assertEquals(1, array.length());
        JSONObject obj = array.getJSONObject(0);
        assertEquals("CLASS_CANCELLED", obj.getString("changeType"));
        assertEquals("DBMS", obj.getString("courseName"));
    }
}
