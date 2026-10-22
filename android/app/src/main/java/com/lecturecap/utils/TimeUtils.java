package com.lecturecap.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static final String[] DAYS_OF_WEEK = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    /**
     * Returns current Day of Week string (e.g., "Monday")
     */
    public static String getCurrentDayOfWeek() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
        return dayFormat.format(new Date());
    }

    public static String getDayOfWeekForDate(String dateIso) {
        try {
            SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", Locale.US);
            Date date = sdfIso.parse(dateIso);
            if (date != null) {
                return sdfDay.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return getCurrentDayOfWeek();
    }

    /**
     * Returns current time formatted as "HH:mm" (24-hour format)
     */
    public static String getCurrentTime24() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        return timeFormat.format(new Date());
    }

    /**
     * Returns current date formatted as "dd MMM yyyy" (e.g., "21 Aug 2026")
     */
    public static String getCurrentDateFormatted() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return dateFormat.format(new Date());
    }

    /**
     * Returns ISO date formatted as "yyyy-MM-dd" (e.g., "2026-08-21")
     */
    public static String getCurrentDateIso() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return dateFormat.format(new Date());
    }

    /**
     * Checks if current time is within [startTime, endTime] (format "HH:mm")
     */
    public static boolean isCurrentTimeInRange(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            String currentStr = sdf.format(new Date());
            Date current = sdf.parse(currentStr);
            Date start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);

            if (current != null && start != null && end != null) {
                return !current.before(start) && current.before(end);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Formats hour and minute into "HH:mm"
     */
    public static String formatTime(int hourOfDay, int minute) {
        return String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
    }

    public static String addSecondsToTime(String time24, long seconds) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
            Date parsedTime = timeFormat.parse(time24);
            if (parsedTime != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsedTime);
                calendar.add(Calendar.SECOND, (int) Math.max(0, seconds));
                return timeFormat.format(calendar.getTime());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time24;
    }

    /**
     * Converts "09:00" to "9:00 AM" for display
     */
    public static String formatTo12Hour(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.US);
            Date date = sdf24.parse(time24);
            if (date != null) {
                return sdf12.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time24;
    }

    /**
     * Formats duration in seconds into "MM:SS" or "HH:MM:SS"
     */
    public static String formatDuration(long seconds) {
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hrs > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs);
        } else {
            return String.format(Locale.US, "%02d:%02d", mins, secs);
        }
    }
}
