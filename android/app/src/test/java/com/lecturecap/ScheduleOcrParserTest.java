package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.utils.ScheduleOcrParser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ScheduleOcrParserTest {

    @Test
    public void testGenericWeeklyTimetableOcrParsing() {
        List<String> rawOcrLines = new ArrayList<>();
        rawOcrLines.add("DEPARTMENT OF COMPUTER SCIENCE TIMETABLE");
        rawOcrLines.add("MONDAY");
        rawOcrLines.add("09:00 AM - 10:00 AM Database Systems");
        rawOcrLines.add("10:00 AM - 11:00 AM Computer Networks");
        rawOcrLines.add("11:00 AM - 01:00 PM Advanced Systems Lab"); // Merged 2-hour lab
        rawOcrLines.add("01:00 PM - 02:00 PM LUNCH BREAK"); // Non-lecture break
        rawOcrLines.add("02:00 PM - 03:00 PM Cloud Computing");
        rawOcrLines.add("TUESDAY");
        rawOcrLines.add("09:30 - 11:00 Machine Learning");
        rawOcrLines.add("11:00 - 12:30 Software Engineering");

        List<TimetableItem> parsedItems = ScheduleOcrParser.parseWeeklyTimetable(rawOcrLines);

        assertNotNull(parsedItems);
        assertFalse(parsedItems.isEmpty());

        // Verify Monday Database Systems
        TimetableItem item1 = parsedItems.get(0);
        assertEquals("Monday", item1.getDayOfWeek());
        assertEquals("09:00", item1.getStartTime());
        assertEquals("10:00", item1.getEndTime());
        assertTrue(item1.getCourseName().contains("Database Systems"));

        // Verify Monday 2-hour Merged Lab
        TimetableItem labItem = parsedItems.get(2);
        assertEquals("Monday", labItem.getDayOfWeek());
        assertEquals("11:00", labItem.getStartTime());
        assertEquals("13:00", labItem.getEndTime()); // Merged lab ends at 13:00
        assertTrue(labItem.getCourseName().contains("Advanced Systems Lab"));

        // Verify Lunch Break was excluded
        for (TimetableItem item : parsedItems) {
            assertFalse(item.getCourseName().toLowerCase().contains("lunch"));
            assertFalse(item.getCourseName().toLowerCase().contains("break"));
        }

        // Verify Tuesday class
        TimetableItem tuesdayItem = parsedItems.get(4);
        assertEquals("Tuesday", tuesdayItem.getDayOfWeek());
        assertEquals("09:30", tuesdayItem.getStartTime());
        assertEquals("11:00", tuesdayItem.getEndTime());
    }

    @Test
    public void testSpatial2DGridOcrParsing() {
        List<com.lecturecap.model.OcrElement> spatialElements = new ArrayList<>();
        // Time Slot Column Headers (Top Row Y=50)
        spatialElements.add(new com.lecturecap.model.OcrElement("09:00 - 10:00", 100, 40, 200, 60));
        spatialElements.add(new com.lecturecap.model.OcrElement("10:00 - 11:00", 210, 40, 310, 60));
        spatialElements.add(new com.lecturecap.model.OcrElement("11:00 - 13:00", 320, 40, 520, 60)); // 2-hour merged header

        // Day Row Headers (Left Column X=30)
        spatialElements.add(new com.lecturecap.model.OcrElement("Monday", 10, 100, 80, 140));
        spatialElements.add(new com.lecturecap.model.OcrElement("Tuesday", 10, 160, 80, 200));

        // Body Cells (Monday)
        spatialElements.add(new com.lecturecap.model.OcrElement("Operating Systems", 110, 100, 190, 130));
        spatialElements.add(new com.lecturecap.model.OcrElement("Theory of Computation", 220, 100, 300, 130));
        spatialElements.add(new com.lecturecap.model.OcrElement("Networks Lab", 330, 100, 500, 130)); // Spans merged lab

        // Body Cells (Tuesday)
        spatialElements.add(new com.lecturecap.model.OcrElement("Database Systems", 110, 160, 190, 190));

        List<TimetableItem> results = ScheduleOcrParser.parseWeeklyTimetableSpatial(spatialElements, new ArrayList<>());

        System.out.println("DEBUG SPATIAL RESULTS COUNT: " + results.size());
        for (TimetableItem item : results) {
            System.out.println(" -> " + item.getDayOfWeek() + ": " + item.getCourseName() + " (" + item.getStartTime() + " - " + item.getEndTime() + ")");
        }

        assertNotNull(results);
        assertEquals(4, results.size());

        // Verify Monday Operating Systems
        TimetableItem item1 = results.get(0);
        assertEquals("Monday", item1.getDayOfWeek());
        assertEquals("Operating Systems", item1.getCourseName());
        assertEquals("09:00", item1.getStartTime());
        assertEquals("10:00", item1.getEndTime());

        // Verify Monday Merged Networks Lab (11:00 - 13:00)
        TimetableItem labItem = results.get(2);
        assertEquals("Monday", labItem.getDayOfWeek());
        assertEquals("Networks Lab", labItem.getCourseName());
        assertEquals("11:00", labItem.getStartTime());
        assertEquals("13:00", labItem.getEndTime());

        // Verify Tuesday Database Systems
        TimetableItem tuesItem = results.get(3);
        assertEquals("Tuesday", tuesItem.getDayOfWeek());
        assertEquals("Database Systems", tuesItem.getCourseName());
        assertEquals("09:00", tuesItem.getStartTime());
    }

    @Test
    public void testAcademicCalendarOcrParsing() {
        List<String> calendarLines = new ArrayList<>();
        calendarLines.add("COLLEGE ACADEMIC CALENDAR 2026");
        calendarLines.add("2026-08-15 Independence Day Holiday");
        calendarLines.add("2026-08-29 Saturday working as Monday schedule");
        calendarLines.add("2026-10-02 Gandhi Jayanti Holiday");

        List<AcademicCalendarItem> items = ScheduleOcrParser.parseAcademicCalendar(calendarLines);

        assertEquals(3, items.size());

        // Holiday verification
        AcademicCalendarItem item1 = items.get(0);
        assertEquals("2026-08-15", item1.getDate());
        assertTrue(item1.isHoliday());
        assertTrue(item1.getDescription().contains("Independence Day"));

        // Special working day & day swap verification
        AcademicCalendarItem item2 = items.get(1);
        assertEquals("2026-08-29", item2.getDate());
        assertTrue(item2.isSpecialWorkingDay());
        assertEquals("Monday", item2.getEffectiveDay());
    }

    @Test
    public void testExamTimetableOcrParsing() {
        List<String> examLines = new ArrayList<>();
        examLines.add("MID-TERM EXAMINATION SCHEDULE");
        examLines.add("2026-09-15 10:00 - 12:00 Data Structures Exam Hall 101");
        examLines.add("2026-09-18 14:00 - 16:00 Algorithms Lab 2");

        List<ExamScheduleItem> exams = ScheduleOcrParser.parseExamTimetable(examLines);

        assertEquals(2, exams.size());

        ExamScheduleItem exam1 = exams.get(0);
        assertEquals("2026-09-15", exam1.getDate());
        assertEquals("10:00", exam1.getStartTime());
        assertEquals("12:00", exam1.getEndTime());
        assertTrue(exam1.getExamName().contains("Data Structures"));

        ExamScheduleItem exam2 = exams.get(1);
        assertEquals("2026-09-18", exam2.getDate());
        assertEquals("14:00", exam2.getStartTime());
        assertEquals("16:00", exam2.getEndTime());
    }
}
