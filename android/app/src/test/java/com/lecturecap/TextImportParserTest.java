package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.utils.TextImportParser;

import org.junit.Test;

import java.util.List;

public class TextImportParserTest {

    @Test
    public void testNormalTimetableTextParsing() {
        String input = "Monday 9:00 AM - 10:00 AM Database Systems\n" +
                "Tuesday 10:00 AM - 11:00 AM Computer Networks\n" +
                "Wednesday 11:00 AM - 01:00 PM Advanced Systems Lab";

        List<TimetableItem> items = TextImportParser.parseWeeklyTimetableText(input);

        assertNotNull(items);
        assertEquals(3, items.size());

        TimetableItem item1 = items.get(0);
        assertEquals("Monday", item1.getDayOfWeek());
        assertEquals("09:00", item1.getStartTime());
        assertEquals("10:00", item1.getEndTime());
        assertTrue(item1.getCourseName().contains("Database Systems"));

        // Multi-hour lab verification
        TimetableItem labItem = items.get(2);
        assertEquals("Wednesday", labItem.getDayOfWeek());
        assertEquals("11:00", labItem.getStartTime());
        assertEquals("13:00", labItem.getEndTime());
    }

    @Test
    public void testPipeDelimitedFormatWithAmPm() {
        String input = "Monday | 09:00 AM | 10:00 AM | Database Systems\n" +
                "Tuesday | 02:00 PM | 04:00 PM | Web Development Lab";

        List<TimetableItem> items = TextImportParser.parseWeeklyTimetableText(input);

        assertNotNull(items);
        assertEquals(2, items.size());

        TimetableItem item1 = items.get(0);
        assertEquals("Monday", item1.getDayOfWeek());
        assertEquals("09:00", item1.getStartTime());
        assertEquals("10:00", item1.getEndTime());
        assertEquals("Database Systems", item1.getCourseName());

        TimetableItem item2 = items.get(1);
        assertEquals("Tuesday", item2.getDayOfWeek());
        assertEquals("14:00", item2.getStartTime());
        assertEquals("16:00", item2.getEndTime());
        assertEquals("Web Development Lab", item2.getCourseName());
    }

    @Test
    public void testChatGptMarkdownTableTextParsing() {
        String markdownInput = "| Day | Time | Subject |\n" +
                "|---|---|---|\n" +
                "| Mon | 09:00 - 10:00 | Operating Systems |\n" +
                "| Tue | 11:00 - 12:30 | Software Engineering |\n" +
                "| Wed | 14:00 - 16:00 | Networks Lab |";

        List<TimetableItem> items = TextImportParser.parseWeeklyTimetableText(markdownInput);

        assertNotNull(items);
        assertEquals(3, items.size());

        TimetableItem item1 = items.get(0);
        assertEquals("Monday", item1.getDayOfWeek());
        assertEquals("09:00", item1.getStartTime());
        assertEquals("10:00", item1.getEndTime());
        assertEquals("Operating Systems", item1.getCourseName());
    }

    @Test
    public void testMissingFieldsValidationNotDetected() {
        String incompleteText = "Monday DBMS";

        List<TimetableItem> items = TextImportParser.parseWeeklyTimetableText(incompleteText);

        assertNotNull(items);
        assertEquals(1, items.size());

        TimetableItem item = items.get(0);
        assertEquals("Monday", item.getDayOfWeek());
        assertEquals("DBMS", item.getCourseName());
        assertEquals(TextImportParser.NOT_DETECTED, item.getStartTime());
        assertEquals(TextImportParser.NOT_DETECTED, item.getEndTime());
    }

    @Test
    public void testAcademicCalendarTextParsing() {
        String input = "2026-08-15 Independence Day Holiday\n" +
                "2026-08-29 Saturday working as Monday schedule\n" +
                "2026-10-02 Gandhi Jayanti Holiday";

        List<AcademicCalendarItem> items = TextImportParser.parseAcademicCalendarText(input);

        assertNotNull(items);
        assertEquals(3, items.size());

        AcademicCalendarItem item1 = items.get(0);
        assertEquals("2026-08-15", item1.getDate());
        assertTrue(item1.isHoliday());
        assertTrue(item1.getDescription().contains("Independence Day"));

        AcademicCalendarItem item2 = items.get(1);
        assertEquals("2026-08-29", item2.getDate());
        assertTrue(item2.isSpecialWorkingDay());
        assertEquals("Monday", item2.getEffectiveDay());
    }

    @Test
    public void testExamTimetableTextParsing() {
        String input = "2026-09-15 10:00 - 12:00 Data Structures Exam Hall 101\n" +
                "2026-09-18 14:00 - 16:00 Algorithms Exam Lab 2";

        List<ExamScheduleItem> exams = TextImportParser.parseExamTimetableText(input);

        assertNotNull(exams);
        assertEquals(2, exams.size());

        ExamScheduleItem exam1 = exams.get(0);
        assertEquals("2026-09-15", exam1.getDate());
        assertEquals("10:00", exam1.getStartTime());
        assertEquals("12:00", exam1.getEndTime());
        assertTrue(exam1.getExamName().contains("Data Structures"));
        assertTrue(exam1.getRoom().contains("Hall 101"));
    }
}
