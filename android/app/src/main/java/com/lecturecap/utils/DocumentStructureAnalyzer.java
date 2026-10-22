package com.lecturecap.utils;

import android.util.Log;

import com.lecturecap.model.OcrElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentStructureAnalyzer {

    private static final String TAG = "DocStructureAnalyzer";

    public enum DocumentType {
        WEEKLY_TIMETABLE,
        ACADEMIC_CALENDAR,
        EXAM_TIMETABLE,
        UNKNOWN
    }

    public enum LayoutStructure {
        GRID_MATRIX,
        SECTIONAL_LIST,
        CHRONOLOGICAL_TABLE,
        UNSTRUCTURED
    }

    public static class AnalysisResult {
        public final DocumentType documentType;
        public final LayoutStructure layoutStructure;
        public final float confidence;
        public final String rationale;
        public final boolean isAmbiguous;

        public AnalysisResult(DocumentType documentType, LayoutStructure layoutStructure, float confidence, String rationale, boolean isAmbiguous) {
            this.documentType = documentType;
            this.layoutStructure = layoutStructure;
            this.confidence = confidence;
            this.rationale = rationale;
            this.isAmbiguous = isAmbiguous;
        }

        @Override
        public String toString() {
            return "AnalysisResult{" +
                    "type=" + documentType +
                    ", layout=" + layoutStructure +
                    ", confidence=" + confidence +
                    ", ambiguous=" + isAmbiguous +
                    ", rationale='" + rationale + '\'' +
                    '}';
        }
    }

    private static final Pattern DAY_PATTERN = Pattern.compile("(?i)^(mon|tue|wed|thu|fri|sat|sun|monday|tuesday|wednesday|thursday|friday|saturday|sunday)$");
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?)\\s*(?:-|to|–|—|\\\\)\\s*(\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2})", Pattern.CASE_INSENSITIVE);

    // Title / Header specific patterns
    private static final Pattern EXAM_HEADER_PATTERN = Pattern.compile("(?i).*?(examination timetable|exam timetable|exam schedule|end term exam|mid term exam|ia timetable|cie timetable|see timetable).*?");
    private static final Pattern CALENDAR_HEADER_PATTERN = Pattern.compile("(?i).*?(academic calendar|calendar of events|list of holidays|semester calendar).*?");
    private static final Pattern WEEKLY_HEADER_PATTERN = Pattern.compile("(?i).*?(weekly timetable|class timetable|lecture schedule|time table|class routine).*?");

    /**
     * Structure-aware weighted confidence classifier.
     */
    public static AnalysisResult analyzeDocument(List<OcrElement> elements, List<String> lines) {
        if ((elements == null || elements.isEmpty()) && (lines == null || lines.isEmpty())) {
            return new AnalysisResult(DocumentType.UNKNOWN, LayoutStructure.UNSTRUCTURED, 0.0f, "Empty document input", true);
        }

        List<String> textLines = lines != null ? lines : new ArrayList<>();
        StringBuilder headerTextBuilder = new StringBuilder();
        int maxHeaderLines = Math.min(textLines.size(), 4);
        for (int i = 0; i < maxHeaderLines; i++) {
            headerTextBuilder.append(textLines.get(i)).append(" ");
        }
        String headerText = headerTextBuilder.toString().toLowerCase();

        Set<String> uniqueDaysFound = new HashSet<>();
        int timeSlotCount = 0;
        int datePatternCount = 0;

        if (elements != null) {
            for (OcrElement elem : elements) {
                String text = elem.getText().trim();
                Matcher dayMatcher = DAY_PATTERN.matcher(text);
                if (dayMatcher.find()) {
                    uniqueDaysFound.add(dayMatcher.group().toLowerCase());
                }
                if (TIME_RANGE_PATTERN.matcher(text).find()) {
                    timeSlotCount++;
                }
                if (DATE_PATTERN.matcher(text).find()) {
                    datePatternCount++;
                }
            }
        }

        int dayHeaderCount = uniqueDaysFound.size();

        // 1. Calculate Structural Scores
        int weeklyScore = 0;
        int examScore = 0;
        int calendarScore = 0;

        // Weekly Timetable Scoring
        if (dayHeaderCount >= 2) weeklyScore += 40;
        if (dayHeaderCount >= 4) weeklyScore += 20;
        if (timeSlotCount >= 2) weeklyScore += 35;
        if (timeSlotCount >= 4) weeklyScore += 15;
        if (WEEKLY_HEADER_PATTERN.matcher(headerText).find()) weeklyScore += 30;

        // Exam Timetable Scoring (Requires explicit exam header OR dates linked to exam sessions)
        if (EXAM_HEADER_PATTERN.matcher(headerText).find()) {
            examScore += 60;
        }
        if (datePatternCount >= 3 && (headerText.contains("exam") || headerText.contains("assessment"))) {
            examScore += 40;
        }
        // Body text keywords add minor points only
        if (lines != null) {
            int bodyExamMentions = 0;
            for (String l : lines) {
                if (l.toLowerCase().contains("exam") || l.toLowerCase().contains("hall")) {
                    bodyExamMentions++;
                }
            }
            examScore += Math.min(bodyExamMentions * 3, 10);
        }

        // Academic Calendar Scoring (Requires explicit calendar header OR date list with holiday keywords)
        if (CALENDAR_HEADER_PATTERN.matcher(headerText).find()) {
            calendarScore += 60;
        }
        if (datePatternCount >= 3 && (headerText.contains("calendar") || headerText.contains("holiday"))) {
            calendarScore += 40;
        }

        Log.d(TAG, String.format("Confidence Scores -> Weekly: %d, Exam: %d, Calendar: %d (Days=%d, Times=%d, Dates=%d)",
                weeklyScore, examScore, calendarScore, dayHeaderCount, timeSlotCount, datePatternCount));

        // 2. Select Winning Category & Evaluate Ambiguity
        DocumentType winner = DocumentType.WEEKLY_TIMETABLE;
        int maxScore = weeklyScore;
        String rationale = "Strong weekly day headers and repeating time-slot structure";

        if (examScore > maxScore) {
            winner = DocumentType.EXAM_TIMETABLE;
            maxScore = examScore;
            rationale = "Explicit examination timetable header and date-wise exam schedule";
        }
        if (calendarScore > maxScore) {
            winner = DocumentType.ACADEMIC_CALENDAR;
            maxScore = calendarScore;
            rationale = "Explicit academic calendar header and holiday date milestones";
        }

        // Check if top scores are ambiguous (close margin or low confidence)
        boolean isAmbiguous = false;
        int marginToNext = 100;
        if (winner == DocumentType.WEEKLY_TIMETABLE) {
            marginToNext = weeklyScore - Math.max(examScore, calendarScore);
        } else if (winner == DocumentType.EXAM_TIMETABLE) {
            marginToNext = examScore - Math.max(weeklyScore, calendarScore);
        } else if (winner == DocumentType.ACADEMIC_CALENDAR) {
            marginToNext = calendarScore - Math.max(weeklyScore, examScore);
        }

        if (maxScore < 50 || marginToNext < 15) {
            isAmbiguous = true;
        }

        float confidence = Math.min(maxScore / 100.0f, 1.0f);

        // 3. Layout Structure Determination
        LayoutStructure layout;
        if (dayHeaderCount >= 2 && timeSlotCount >= 2) {
            layout = LayoutStructure.GRID_MATRIX;
        } else if (dayHeaderCount >= 2) {
            layout = LayoutStructure.SECTIONAL_LIST;
        } else if (datePatternCount >= 2) {
            layout = LayoutStructure.CHRONOLOGICAL_TABLE;
        } else {
            layout = LayoutStructure.UNSTRUCTURED;
        }

        return new AnalysisResult(winner, layout, confidence, rationale, isAmbiguous);
    }
}
