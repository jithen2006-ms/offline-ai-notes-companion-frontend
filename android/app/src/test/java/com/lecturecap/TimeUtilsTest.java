package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.LectureSession;
import com.lecturecap.model.TimetableItem;
import com.lecturecap.utils.TimeUtils;

import org.junit.Test;

public class TimeUtilsTest {

    @Test
    public void testFormatTime() {
        assertEquals("09:05", TimeUtils.formatTime(9, 5));
        assertEquals("14:30", TimeUtils.formatTime(14, 30));
        assertEquals("00:00", TimeUtils.formatTime(0, 0));
    }

    @Test
    public void testFormatDuration() {
        assertEquals("00:45", TimeUtils.formatDuration(45));
        assertEquals("05:30", TimeUtils.formatDuration(330));
        assertEquals("01:00:00", TimeUtils.formatDuration(3600));
        assertEquals("02:15:30", TimeUtils.formatDuration(8130));
    }

    @Test
    public void testTimetableItemModel() {
        TimetableItem item = new TimetableItem(1, "DBMS", "Monday", "09:00", "10:00");
        assertEquals("DBMS", item.getCourseName());
        assertEquals("Monday", item.getDayOfWeek());
        assertEquals("09:00", item.getStartTime());
        assertEquals("10:00", item.getEndTime());
        assertEquals("09:00 – 10:00", item.getTimeRangeFormatted());
    }

    @Test
    public void testLectureSessionModel() {
        LectureSession session = new LectureSession(
                "student@college.edu",
                "Computer Networks",
                "2026-08-21",
                "10:00",
                "11:00",
                "Today we learned OSI layers.",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_UPLOADED,
                3600,
                System.currentTimeMillis()
        );

        assertEquals("student@college.edu", session.getUserId());
        assertEquals("Computer Networks", session.getCourseName());
        assertEquals(LectureSession.STATUS_COMPLETED, session.getSessionStatus());
        assertEquals(LectureSession.UPLOAD_STATUS_UPLOADED, session.getUploadStatus());
        assertEquals("10:00 – 11:00", session.getTimeRange());
    }
}
