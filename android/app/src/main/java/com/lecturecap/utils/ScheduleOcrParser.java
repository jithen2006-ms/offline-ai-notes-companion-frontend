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

import com.lecturecap.model.OcrElement;

import java.util.Collections;

public class ScheduleOcrParser {

    private static final String TAG = "ScheduleOcrParser";

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final List<String> EXCLUDED_KEYWORDS = Arrays.asList("break", "lunch", "recess", "interval", "tea break", "free period", "vacant");

    // Regex patterns for time parsing
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?)\\s*(?:-|to|–|—|\\\\)\\s*(\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERIOD_TIME_PATTERN = Pattern.compile("(\\d{1,2}[:.]\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(?:,\\s*\\d{4})?)", Pattern.CASE_INSENSITIVE);

    /**
     * 2D Spatial Grid OCR Parser: maps OCR bounding boxes to 2D table rows and columns.
     */
    public static List<TimetableItem> parseWeeklyTimetableSpatial(List<OcrElement> elements, List<String> fallbackLines) {
        if (elements == null || elements.isEmpty()) {
            return parseWeeklyTimetable(fallbackLines);
        }

        // 1. Identify Day Headers
        class DayHeader {
            final String dayName;
            final OcrElement element;
            DayHeader(String dayName, OcrElement element) {
                this.dayName = dayName;
                this.element = element;
            }
        }

        List<DayHeader> dayHeaders = new ArrayList<>();
        for (OcrElement elem : elements) {
            String day = detectDayOfWeek(elem.getText());
            if (day != null) {
                boolean exists = false;
                for (DayHeader dh : dayHeaders) {
                    if (dh.dayName.equalsIgnoreCase(day) && Math.abs(dh.element.getCenterY() - elem.getCenterY()) < 30) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    dayHeaders.add(new DayHeader(day, elem));
                }
            }
        }

        // 2. Identify Time Slot Headers
        class TimeSlotHeader {
            final String startTime;
            String endTime;
            final OcrElement element;
            TimeSlotHeader(String startTime, String endTime, OcrElement element) {
                this.startTime = startTime;
                this.endTime = endTime;
                this.element = element;
            }
        }

        List<TimeSlotHeader> timeHeaders = new ArrayList<>();
        for (OcrElement elem : elements) {
            Matcher matcher = TIME_RANGE_PATTERN.matcher(elem.getText());
            if (matcher.find()) {
                String start = convertTo24Hour(matcher.group(1));
                String end = convertTo24Hour(matcher.group(2));
                timeHeaders.add(new TimeSlotHeader(start, end, elem));
            } else {
                Matcher periodMatcher = PERIOD_TIME_PATTERN.matcher(elem.getText());
                if (periodMatcher.find()) {
                    String timeStr = convertTo24Hour(periodMatcher.group(1));
                    timeHeaders.add(new TimeSlotHeader(timeStr, null, elem));
                }
            }
        }

        // Sort time headers horizontally by X coordinate
        Collections.sort(timeHeaders, (t1, t2) -> Integer.compare(t1.element.getCenterX(), t2.element.getCenterX()));

        // Fill missing end times for consecutive time headers
        for (int i = 0; i < timeHeaders.size(); i++) {
            TimeSlotHeader curr = timeHeaders.get(i);
            if (curr.endTime == null) {
                if (i + 1 < timeHeaders.size()) {
                    curr.endTime = timeHeaders.get(i + 1).startTime;
                } else {
                    curr.endTime = addOneHour(curr.startTime);
                }
            }
        }

        // Sort day headers vertically by Y coordinate (Days as Rows)
        Collections.sort(dayHeaders, (d1, d2) -> Integer.compare(d1.element.getCenterY(), d2.element.getCenterY()));

        Log.d(TAG, "Spatial OCR found " + dayHeaders.size() + " Day Headers and " + timeHeaders.size() + " Time Headers.");

        if (dayHeaders.size() < 2 || timeHeaders.isEmpty()) {
            Log.w(TAG, "Insufficient spatial grid headers detected. Falling back to linear parser.");
            return parseWeeklyTimetable(fallbackLines);
        }

        // 3. Map Subject Cells to (Day Row i, Time Slot Column j)
        List<TimetableItem> results = new ArrayList<>();

        for (int i = 0; i < dayHeaders.size(); i++) {
            DayHeader dayHeader = dayHeaders.get(i);
            int topY = (i == 0) ? dayHeader.element.getTop() - 20 : (dayHeaders.get(i - 1).element.getCenterY() + dayHeader.element.getCenterY()) / 2;
            int bottomY = (i == dayHeaders.size() - 1) ? dayHeader.element.getBottom() + 150 : (dayHeader.element.getCenterY() + dayHeaders.get(i + 1).element.getCenterY()) / 2;

            for (int j = 0; j < timeHeaders.size(); j++) {
                TimeSlotHeader timeHeader = timeHeaders.get(j);
                int leftX = (j == 0) ? timeHeader.element.getLeft() - 30 : (timeHeaders.get(j - 1).element.getCenterX() + timeHeader.element.getCenterX()) / 2;
                int rightX = (j == timeHeaders.size() - 1) ? timeHeader.element.getRight() + 50 : (timeHeader.element.getCenterX() + timeHeaders.get(j + 1).element.getCenterX()) / 2;

                StringBuilder cellSubject = new StringBuilder();
                int maxSpanRightX = rightX;
                boolean isBreak = false;

                for (OcrElement elem : elements) {
                    if (detectDayOfWeek(elem.getText()) != null || TIME_RANGE_PATTERN.matcher(elem.getText()).find()) {
                        continue;
                    }

                    int cX = elem.getCenterX();
                    int cY = elem.getCenterY();

                    if (cY >= topY && cY <= bottomY && cX >= leftX && cX <= rightX) {
                        String txt = elem.getText();
                        if (isNonLectureKeyword(txt)) {
                            isBreak = true;
                            break;
                        }
                        if (txt.length() > 1 && !txt.matches("(?i).*(time|day|period|room|sem).*")) {
                            if (cellSubject.length() > 0) cellSubject.append(" ");
                            cellSubject.append(txt);

                            if (elem.getRight() > maxSpanRightX + 30) {
                                maxSpanRightX = elem.getRight();
                            }
                        }
                    }
                }

                if (!isBreak && cellSubject.length() > 0) {
                    String subjectName = cellSubject.toString().trim();
                    int endColIndex = j;
                    for (int k = j + 1; k < timeHeaders.size(); k++) {
                        TimeSlotHeader nextTh = timeHeaders.get(k);
                        if (nextTh.element.getCenterX() <= maxSpanRightX) {
                            endColIndex = k;
                        }
                    }

                    String startTime = timeHeader.startTime;
                    String endTime = timeHeaders.get(endColIndex).endTime;

                    boolean duplicate = false;
                    for (TimetableItem existing : results) {
                        if (existing.getDayOfWeek().equals(dayHeader.dayName) && existing.getStartTime().equals(startTime) && existing.getCourseName().equals(subjectName)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        results.add(new TimetableItem(subjectName, dayHeader.dayName, startTime, endTime));
                    }
                }
            }
        }

        if (results.isEmpty()) {
            Log.w(TAG, "Spatial parsing yielded 0 results. Falling back to linear line parser.");
            return parseWeeklyTimetable(fallbackLines);
        }

        return results;
    }

    private static String addOneHour(String startTime) {
        try {
            String[] parts = startTime.split(":");
            int h = (Integer.parseInt(parts[0]) + 1) % 24;
            int m = Integer.parseInt(parts[1]);
            return String.format("%02d:%02d", h, m);
        } catch (Exception e) {
            return "10:00";
        }
    }

    /**
     * Generic parser: converts raw OCR text lines from ANY college timetable image into structured TimetableItem entries.
     */
    public static List<TimetableItem> parseWeeklyTimetable(List<String> lines) {
        List<TimetableItem> items = new ArrayList<>();
        String currentDay = "Monday";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            // Check if line specifies day of week
            String detectedDay = detectDayOfWeek(line);
            if (detectedDay != null) {
                currentDay = detectedDay;
                // If line only contains day name, move to next line
                if (line.equalsIgnoreCase(detectedDay) || line.length() < 12) {
                    continue;
                }
            }

            // Check if line is a break or non-lecture period
            if (isNonLectureKeyword(line)) {
                Log.d(TAG, "Skipping non-lecture slot: " + line);
                continue;
            }

            // Look for time ranges (e.g. 09:00 - 10:00 or 11:00 AM to 01:00 PM for merged labs)
            Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(line);
            if (timeMatcher.find()) {
                String rawStart = timeMatcher.group(1);
                String rawEnd = timeMatcher.group(2);

                String startTime = convertTo24Hour(rawStart);
                String endTime = convertTo24Hour(rawEnd);

                String courseName = line.replaceAll(TIME_RANGE_PATTERN.pattern(), "").trim();
                courseName = courseName.replaceAll("(?i)^(mon|tue|wed|thu|fri|sat|sun|monday|tuesday|wednesday|thursday|friday|saturday|sunday)", "").trim();
                courseName = courseName.replaceAll("^[\\s:-]+|[\\s:-]+$", "");

                if (courseName.isEmpty() && i > 0) {
                    // Check adjacent line for course title
                    String prevLine = lines.get(i - 1).trim();
                    if (detectDayOfWeek(prevLine) == null && !TIME_RANGE_PATTERN.matcher(prevLine).find() && !isNonLectureKeyword(prevLine)) {
                        courseName = prevLine;
                    }
                }
                if (courseName.isEmpty() && i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1).trim();
                    if (detectDayOfWeek(nextLine) == null && !TIME_RANGE_PATTERN.matcher(nextLine).find() && !isNonLectureKeyword(nextLine)) {
                        courseName = nextLine;
                    }
                }
                if (courseName.isEmpty()) {
                    courseName = "Lecture / Class " + (items.size() + 1);
                }

                items.add(new TimetableItem(courseName, currentDay, startTime, endTime));
            }
        }

        // Fallback: If no explicit time range found per line, generate slots with detected time headers
        if (items.isEmpty()) {
            items.addAll(fallbackGridParse(lines));
        }

        return items;
    }

    private static List<TimetableItem> fallbackGridParse(List<String> lines) {
        List<TimetableItem> items = new ArrayList<>();
        String currentDay = "Monday";
        String defaultStart = "09:00";
        int hourOffset = 9;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String day = detectDayOfWeek(trimmed);
            if (day != null) {
                currentDay = day;
                hourOffset = 9;
                continue;
            }

            if (isNonLectureKeyword(trimmed)) continue;

            if (trimmed.length() > 2 && !trimmed.matches("(?i).*?(time|day|period|semester|room).*?")) {
                String start = String.format("%02d:00", hourOffset);
                String end = String.format("%02d:00", Math.min(hourOffset + 1, 23));
                hourOffset++;
                items.add(new TimetableItem(trimmed, currentDay, start, end));
            }
        }
        return items;
    }

    /**
     * Generic parser: converts raw OCR text lines into Academic Calendar items.
     */
    public static List<AcademicCalendarItem> parseAcademicCalendar(List<String> lines) {
        List<AcademicCalendarItem> items = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher dateMatcher = DATE_PATTERN.matcher(trimmed);
            if (dateMatcher.find()) {
                String rawDate = dateMatcher.group(1);
                String isoDate = normalizeDate(rawDate);

                String description = trimmed.replace(rawDate, "").replaceAll("^[\\s:-]+|[\\s:-]+$", "").trim();
                if (description.isEmpty()) description = "Academic Event";

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
     * Generic parser: converts raw OCR text lines into Exam Schedule items.
     */
    public static List<ExamScheduleItem> parseExamTimetable(List<String> lines) {
        List<ExamScheduleItem> items = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            Matcher dateMatcher = DATE_PATTERN.matcher(line);
            if (dateMatcher.find()) {
                String rawDate = dateMatcher.group(1);
                String isoDate = normalizeDate(rawDate);

                Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(line);
                String start = "10:00";
                String end = "12:00";
                if (timeMatcher.find()) {
                    start = convertTo24Hour(timeMatcher.group(1));
                    end = convertTo24Hour(timeMatcher.group(2));
                }

                String room = "Exam Hall";
                Pattern roomPattern = Pattern.compile("(?i)(hall|room|lab|center|centre)\\s*[a-z0-9-]+");
                Matcher roomMatcher = roomPattern.matcher(line);
                if (roomMatcher.find()) {
                    room = roomMatcher.group(0);
                }

                String name = line.replace(rawDate, "");
                if (timeMatcher.find()) {
                    name = name.replace(timeMatcher.group(0), "");
                }
                if (roomMatcher.find()) {
                    name = name.replace(roomMatcher.group(0), "");
                }
                name = name.replaceAll("^[\\s:-]+|[\\s:-]+$", "").trim();
                if (name.isEmpty()) name = "Examination " + (items.size() + 1);

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

            if (isPm && h < 12) h += 12;
            if (isAm && h == 12) h = 0;

            return String.format("%02d:%02d", h, m);
        } catch (Exception e) {
            return "09:00";
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
        return TimeUtils.getCurrentDateIso();
    }
}

