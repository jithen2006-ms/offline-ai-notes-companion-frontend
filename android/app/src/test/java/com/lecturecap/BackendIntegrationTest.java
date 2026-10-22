package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.LectureSession;

import org.json.JSONObject;
import org.junit.Test;

public class BackendIntegrationTest {

    @Test
    public void testLectureSessionJsonPayloadSchema() throws Exception {
        LectureSession session = new LectureSession(
                "student@college.edu",
                "Computer Networks",
                "2026-08-25",
                "09:00",
                "10:00",
                "Today we are learning about the transport layer and TCP handshakes.",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_PENDING,
                3600,
                System.currentTimeMillis()
        );

        JSONObject payload = new JSONObject();
        payload.put("userId", session.getUserId());
        payload.put("course_name", session.getCourseName());
        payload.put("course", session.getCourseName());
        payload.put("date", session.getDate());
        payload.put("day", "Tuesday");
        payload.put("start_time", session.getStartTime());
        payload.put("startTime", session.getStartTime());
        payload.put("end_time", session.getEndTime());
        payload.put("endTime", session.getEndTime());
        payload.put("transcript", session.getTranscript());
        payload.put("session_status", session.getSessionStatus());
        payload.put("duration_seconds", session.getDurationSeconds());

        assertEquals("Computer Networks", payload.getString("course_name"));
        assertEquals("2026-08-25", payload.getString("date"));
        assertEquals("09:00", payload.getString("start_time"));
        assertEquals("10:00", payload.getString("end_time"));
        assertTrue(payload.getString("transcript").contains("TCP handshakes"));
        assertEquals(LectureSession.UPLOAD_STATUS_PENDING, session.getUploadStatus());
    }
}
