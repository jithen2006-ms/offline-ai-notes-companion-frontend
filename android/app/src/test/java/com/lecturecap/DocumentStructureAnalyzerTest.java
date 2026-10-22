package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.OcrElement;
import com.lecturecap.utils.DocumentStructureAnalyzer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DocumentStructureAnalyzerTest {

    @Test
    public void testWeeklyTimetableClassification() {
        List<OcrElement> elements = new ArrayList<>();
        elements.add(new OcrElement("DEPARTMENT TIMETABLE", 100, 10, 400, 30));
        elements.add(new OcrElement("Monday", 10, 50, 80, 80));
        elements.add(new OcrElement("Tuesday", 10, 90, 80, 120));
        elements.add(new OcrElement("Wednesday", 10, 130, 80, 160));
        elements.add(new OcrElement("09:00 - 10:00", 100, 50, 200, 80));
        elements.add(new OcrElement("10:00 - 11:00", 210, 50, 310, 80));

        List<String> lines = new ArrayList<>();
        lines.add("DEPARTMENT TIMETABLE");
        lines.add("Monday");
        lines.add("09:00 - 10:00 Operating Systems");

        DocumentStructureAnalyzer.AnalysisResult result = DocumentStructureAnalyzer.analyzeDocument(elements, lines);

        assertEquals(DocumentStructureAnalyzer.DocumentType.WEEKLY_TIMETABLE, result.documentType);
        assertFalse(result.isAmbiguous);
    }

    @Test
    public void testWeeklyTimetableWithExamKeywordsStillClassifiesAsWeeklyTimetable() {
        List<OcrElement> elements = new ArrayList<>();
        // Title
        elements.add(new OcrElement("DEPARTMENT OF COMPUTER SCIENCE CLASS TIMETABLE", 100, 10, 500, 30));
        // Days
        elements.add(new OcrElement("Monday", 10, 50, 80, 80));
        elements.add(new OcrElement("Tuesday", 10, 90, 80, 120));
        elements.add(new OcrElement("Wednesday", 10, 130, 80, 160));
        elements.add(new OcrElement("Thursday", 10, 170, 80, 200));
        // Times
        elements.add(new OcrElement("09:00 - 10:00", 100, 50, 200, 80));
        elements.add(new OcrElement("10:00 - 11:00", 210, 50, 310, 80));

        // Body text contains exam noise (IA-1, SEE, CIE, Exam Prep)
        elements.add(new OcrElement("IA-1 Review & DBMS", 110, 50, 190, 75));
        elements.add(new OcrElement("SEE Exam Prep", 220, 50, 300, 75));
        elements.add(new com.lecturecap.model.OcrElement("CIE Assessment Session", 110, 90, 190, 115));

        List<String> lines = new ArrayList<>();
        lines.add("DEPARTMENT OF COMPUTER SCIENCE CLASS TIMETABLE");
        lines.add("Monday");
        lines.add("09:00 - 10:00 IA-1 Review & DBMS");
        lines.add("10:00 - 11:00 SEE Exam Prep");
        lines.add("Tuesday");
        lines.add("09:00 - 10:00 CIE Assessment Session");

        DocumentStructureAnalyzer.AnalysisResult result = DocumentStructureAnalyzer.analyzeDocument(elements, lines);

        // MUST classify as WEEKLY_TIMETABLE due to strong grid structure
        assertEquals(DocumentStructureAnalyzer.DocumentType.WEEKLY_TIMETABLE, result.documentType);
        assertFalse(result.isAmbiguous);
    }

    @Test
    public void testAcademicCalendarClassification() {
        List<OcrElement> elements = new ArrayList<>();
        elements.add(new OcrElement("COLLEGE ACADEMIC CALENDAR OF EVENTS", 100, 10, 400, 30));
        elements.add(new OcrElement("2026-08-15 Independence Day Holiday", 50, 60, 450, 80));
        elements.add(new OcrElement("2026-08-29 Saturday working as Monday", 50, 90, 450, 110));
        elements.add(new OcrElement("2026-10-02 Gandhi Jayanti Holiday", 50, 120, 450, 140));

        List<String> lines = new ArrayList<>();
        lines.add("COLLEGE ACADEMIC CALENDAR OF EVENTS 2026");
        lines.add("2026-08-15 Independence Day Holiday");
        lines.add("2026-08-29 Saturday working as Monday schedule");

        DocumentStructureAnalyzer.AnalysisResult result = DocumentStructureAnalyzer.analyzeDocument(elements, lines);

        assertEquals(DocumentStructureAnalyzer.DocumentType.ACADEMIC_CALENDAR, result.documentType);
        assertFalse(result.isAmbiguous);
    }

    @Test
    public void testExamTimetableClassification() {
        List<OcrElement> elements = new ArrayList<>();
        elements.add(new OcrElement("END-TERM EXAMINATION TIMETABLE", 100, 10, 400, 30));
        elements.add(new OcrElement("2026-09-15 10:00 - 12:00 Data Structures Exam", 50, 60, 450, 80));
        elements.add(new OcrElement("2026-09-18 14:00 - 16:00 Algorithms Exam", 50, 90, 450, 110));

        List<String> lines = new ArrayList<>();
        lines.add("END-TERM EXAMINATION TIMETABLE");
        lines.add("2026-09-15 10:00 - 12:00 Data Structures Exam Hall 1");

        DocumentStructureAnalyzer.AnalysisResult result = DocumentStructureAnalyzer.analyzeDocument(elements, lines);

        assertEquals(DocumentStructureAnalyzer.DocumentType.EXAM_TIMETABLE, result.documentType);
        assertFalse(result.isAmbiguous);
    }
}
