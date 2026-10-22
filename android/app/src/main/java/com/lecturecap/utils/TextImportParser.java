package com.lecturecap.utils;

import android.util.Log;

import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.TimetableItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextImportParser {

    private static final String TAG = "TextImportParser";

    public static final String NOT_DETECTED = "Not detected";

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final List<String> EXCLUDED_KEYWORDS = Arrays.asList("break", "lunch", "recess", "interval", "tea break", "free period", "vacant");

    // Flexible time patterns
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}(?:[:.]\\d{2})?\\s*(?:am|pm)?)\\s*(?:-|to|–|—|\\\\|\\|)\\s*(\\d{1,2}(?:[:.]\\d{2})?\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_TIME_PATTERN = Pattern.compile("(\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(?:,\\s*\\d{4})?)", Pattern.CASE_INSENSITIVE);
    private static final String DAY_CLEAN_REGEX = "(?i)^(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)";

    /**
     * Parses pasted natural/AI text into Weekly Timetable items with strict validation.
     */
    public static List<TimetableItem> parseWeeklyTimetableText(String rawText) {
        List<TimetableItem> items = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) return items;

        String[] rawLines = rawText.split("\\r?\\n");
        String currentDay = NOT_DETECTED;

        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i].trim();
            if (line.isEmpty() || line.startsWith("|-") || line.startsWith("#") || line.startsWith("==")) continue;

            // Handle Markdown / Pipe delimited table rows (e.g. | Mon | 09:00 - 10:00 | DBMS |)
            if (line.contains("|")) {
                List<TimetableItem> pipeItems = parsePipeDelimitedRow(line);
                if (!pipeItems.isEmpty()) {
                    items.addAll(pipeItems);
                    continue;
                }
            }

            // Check if line sets current day
            String detectedDay = detectDayOfWeek(line);
            if (detectedDay != null) {
                currentDay = detectedDay;
                if (line.equalsIgnoreCase(detectedDay)) {
                    continue;
                }
            }

            // Skip break / lunch lines
            if (isNonLectureKeyword(line)) continue;

            // Check time range
            Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(line);
            if (timeMatcher.find()) {
                String rawStart = timeMatcher.group(1);
                String rawEnd = timeMatcher.group(2);

                String startTime = convertTo24Hour(rawStart);
                String endTime = convertTo24Hour(rawEnd);

                String courseName = line.replaceAll(TIME_RANGE_PATTERN.pattern(), "").trim();
                courseName = courseName.replaceAll(DAY_CLEAN_REGEX, "").trim();
                courseName = courseName.replaceAll("^[\\s|:-]+|[\\s|:-]+$", "");

                if (courseName.isEmpty() && i + 1 < rawLines.length) {
                    String nextLine = rawLines[i + 1].trim();
                    if (detectDayOfWeek(nextLine) == null && !TIME_RANGE_PATTERN.matcher(nextLine).find()) {
                        courseName = nextLine;
                    }
                }
                if (courseName.isEmpty()) {
                    courseName = NOT_DETECTED;
                }

                items.add(new TimetableItem(courseName, currentDay, startTime, endTime));
            } else if (detectedDay != null) {
                // Line specifies subject but no time
                String courseName = line.replaceAll(DAY_CLEAN_REGEX, "").trim();
                courseName = courseName.replaceAll("^[\\s|:-]+|[\\s|:-]+$", "");
                if (!courseName.isEmpty() && !isNonLectureKeyword(courseName)) {
                    items.add(new TimetableItem(courseName, currentDay, NOT_DETECTED, NOT_DETECTED));
                }
            }
        }
        return items;
    }

    private static List<TimetableItem> parsePipeDelimitedRow(String line) {
        List<TimetableItem> items = new ArrayList<>();
        String[] parts = line.split("\\|");
        List<String> cleanParts = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) cleanParts.add(trimmed);
        }

        if (cleanParts.size() < 2) return items;

        String day = NOT_DETECTED;
        String start = NOT_DETECTED;
        String end = NOT_DETECTED;
        String subject = NOT_DETECTED;

        for (String part : cleanParts) {
            String detectedDay = detectDayOfWeek(part);
            if (detectedDay != null) {
                day = detectedDay;
                continue;
            }
            Matcher rangeMatcher = TIME_RANGE_PATTERN.matcher(part);
            if (rangeMatcher.find()) {
                start = convertTo24Hour(rangeMatcher.group(1));
                end = convertTo24Hour(rangeMatcher.group(2));
                continue;
            }
            Matcher singleMatcher = SINGLE_TIME_PATTERN.matcher(part);
            if (singleMatcher.find()) {
                if (start.equals(NOT_DETECTED)) {
                    start = convertTo24Hour(singleMatcher.group(1));
                } else if (end.equals(NOT_DETECTED)) {
                    end = convertTo24Hour(singleMatcher.group(1));
                }
                continue;
            }
            if (!isNonLectureKeyword(part) && !part.equalsIgnoreCase("day") && !part.equalsIgnoreCase("time") && !part.equalsIgnoreCase("subject") && !part.equalsIgnoreCase("start time") && !part.equalsIgnoreCase("end time") && !part.equalsIgnoreCase("class/subject")) {
                subject = part;
            }
        }

        if (!subject.equals(NOT_DETECTED) || !start.equals(NOT_DETECTED)) {
            items.add(new TimetableItem(subject, day, start, end));
        }

        return items;
    }

    /**
     * Parses pasted text into Academic Calendar items.
     */
    public static List<AcademicCalendarItem> parseAcademicCalendarText(String rawText) {
        List<AcademicCalendarItem> items = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) return items;

        String[] rawLines = rawText.split("\\r?\\n");
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("==")) continue;

            Matcher dateMatcher = DATE_PATTERN.matcher(trimmed);
            if (dateMatcher.find()) {
                String rawDate = dateMatcher.group(1);
                String isoDate = normalizeDate(rawDate);

                String description = trimmed.replace(rawDate, "").replaceAll("^[\\s|:-]+|[\\s|:-]+$", "").trim();
                if (description.isEmpty()) description = NOT_DETECTED;

                String type = AcademicCalendarItem.TYPE_HOLIDAY;
                String effectiveDay = null;

                String lower = description.toLowerCase();
                if (lower.contains("working") || lower.contains("timetable") || lower.contains("substitute") || lower.contains("swap")) {
                    type = AcademicCalendarItem.TYPE_SPECIAL_WORKING_DAY;
                    for (String d : DAYS) {
                        if (lower.contains(d.toLowerCase())) {
                            effectiveDay = d;
                            break;
                        }
                    }
                    if (effectiveDay == null) effectiveDay = "Monday";
                }

                items.add(new AcademicCalendarItem(isoDate, type, description, effectiveDay));
            }
        }
        return items;
    }

    /**
     * Parses pasted text into Exam Schedule items.
     */
    public static List<ExamScheduleItem> parseExamTimetableText(String rawText) {
        List<ExamScheduleItem> items = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) return items;

        String[] rawLines = rawText.split("\\r?\\n");
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("==")) continue;

            Matcher dateMatcher = DATE_PATTERN.matcher(trimmed);
            if (dateMatcher.find()) {
                String rawDate = dateMatcher.group(1);
                String isoDate = normalizeDate(rawDate);

                String lineWithoutDate = trimmed.replace(rawDate, "").trim();

                Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(lineWithoutDate);
                String start = NOT_DETECTED;
                String end = NOT_DETECTED;
                String matchedTimeStr = null;
                if (timeMatcher.find()) {
                    matchedTimeStr = timeMatcher.group(0);
                    start = convertTo24Hour(timeMatcher.group(1));
                    end = convertTo24Hour(timeMatcher.group(2));
                }

                String room = NOT_DETECTED;
                Pattern roomPattern = Pattern.compile("(?i)(hall|room|lab|center|centre)\\s*[a-z0-9-]+");
                Matcher roomMatcher = roomPattern.matcher(lineWithoutDate);
                String matchedRoomStr = null;
                if (roomMatcher.find()) {
                    matchedRoomStr = roomMatcher.group(0);
                    room = matchedRoomStr;
                }

                String name = lineWithoutDate;
                if (matchedTimeStr != null) {
                    name = name.replace(matchedTimeStr, "");
                }
                if (matchedRoomStr != null) {
                    name = name.replace(matchedRoomStr, "");
                }
                name = name.replaceAll("(?i)exam", "").replaceAll("^[\\s|:-]+|[\\s|:-]+$", "").trim();
                if (name.isEmpty()) name = NOT_DETECTED;

                items.add(new ExamScheduleItem(name, isoDate, start, end, room));
            }
        }
        return items;
    }

    private static boolean isNonLectureKeyword(String text) {
        String lower = text.toLowerCase();
        for (String kw : EXCLUDED_KEYWORDS) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private static String detectDayOfWeek(String text) {
        String lower = text.toLowerCase();
        for (String day : DAYS) {
            if (lower.contains(day.toLowerCase())) return day;
        }
        if (lower.contains("mon")) return "Monday";
        if (lower.contains("tue")) return "Tuesday";
        if (lower.contains("wed")) return "Wednesday";
        if (lower.contains("thu")) return "Thursday";
        if (lower.contains("fri")) return "Friday";
        if (lower.contains("sat")) return "Saturday";
        if (lower.contains("sun")) return "Sunday";
        return null;
    }

    private static String convertTo24Hour(String rawTimeStr) {
        try {
            rawTimeStr = rawTimeStr.trim();
            boolean isPm = rawTimeStr.toLowerCase().contains("pm");
            boolean isAm = rawTimeStr.toLowerCase().contains("am");

            String clean = rawTimeStr.replaceAll("(?i)[a-z\\s]", "").replace(".", ":");
            String[] parts = clean.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            if (!rawTimeStr.contains(":") && h < 8 && !isAm) {
                isPm = true;
            }

            if (isPm && h < 12) h += 12;
            if (isAm && h == 12) h = 0;

            return String.format("%02d:%02d", h, m);
        } catch (Exception e) {
            return NOT_DETECTED;
        }
    }

    private static String normalizeDate(String rawDate) {
        try {
            rawDate = rawDate.replaceAll("[./]", "-").trim();
            String[] parts = rawDate.split("-");
            if (parts.length == 3) {
                if (parts[0].length() == 4) {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int d = Integer.parseInt(parts[2]);
                    return String.format("%04d-%02d-%02d", y, m, d);
                } else if (parts[2].length() == 4) {
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    return String.format("%04d-%02d-%02d", y, m, d);
                }
            }
        } catch (Exception ignored) {}
        return NOT_DETECTED;
    }
}
