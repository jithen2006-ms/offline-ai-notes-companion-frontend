package com.lecturecap.model;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class LectureSessionStateMachineTest {

    @Test
    public void testLectureSessionInitialStateAndSegments() throws Exception {
        JSONObject segmentsObj = new JSONObject();
        JSONArray segs = new JSONArray();
        JSONObject seg1 = new JSONObject();
        seg1.put("id", 1);
        seg1.put("start", 0.0);
        seg1.put("end", 4.16);
        seg1.put("text", "Can you speak English for one minute");
        segs.put(seg1);
        segmentsObj.put("segments", segs);

        LectureSession session = new LectureSession(
                "student@college.edu",
                "English Communication",
                "2026-08-26",
                "10:00",
                "10:05",
                "Can you speak English for one minute",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_PENDING,
                260,
                System.currentTimeMillis()
        );
        session.setSegmentsJsonStr(segmentsObj.toString());

        // 1. Verify transcription completion
        assertEquals(LectureSession.STATUS_COMPLETED, session.getSessionStatus());
        assertEquals(LectureSession.UPLOAD_STATUS_PENDING, session.getUploadStatus());
        assertEquals("Can you speak English for one minute", session.getTranscript());
        assertTrue(session.hasSegmentsJson());
        assertNotNull(session.getSegmentsJsonObject());
        assertEquals(1, session.getSegmentsJsonObject().getJSONArray("segments").length());
    }

    @Test
    public void testUploadFailureDoesNotDiscardTranscriptOrSegments() throws Exception {
        LectureSession session = new LectureSession(
                "student@college.edu",
                "DBMS",
                "2026-08-26",
                "09:00",
                "10:00",
                "Relational database management systems organize data into tables.",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_PENDING,
                3600,
                System.currentTimeMillis()
        );
        session.setSegmentsJsonStr("{\"segments\":[{\"id\":1,\"start\":0.0,\"end\":10.0,\"text\":\"Relational database management systems organize data into tables.\"}]}");

        // Simulate network upload failure
        session.setUploadStatus(LectureSession.UPLOAD_STATUS_FAILED);

        // TRANSCRIPTION MUST REMAIN COMPLETED & TRANSCRIPT MUST BE PRESERVED
        assertEquals(LectureSession.STATUS_COMPLETED, session.getSessionStatus());
        assertEquals(LectureSession.UPLOAD_STATUS_FAILED, session.getUploadStatus());
        assertEquals("Relational database management systems organize data into tables.", session.getTranscript());
        assertTrue(session.hasSegmentsJson());
        assertNotNull(session.getSegmentsJsonObject());
    }

    @Test
    public void testSuccessfulUploadUpdatesOnlyUploadStatus() {
        LectureSession session = new LectureSession(
                "student@college.edu",
                "Computer Networks",
                "2026-08-26",
                "11:00",
                "12:00",
                "TCP IP protocol suite enables reliable communication.",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_FAILED, // Was failed previously
                3600,
                System.currentTimeMillis()
        );

        // Simulate retry upload success
        session.setUploadStatus(LectureSession.UPLOAD_STATUS_UPLOADED);

        // Upload status becomes UPLOADED while session status remains COMPLETED
        assertEquals(LectureSession.STATUS_COMPLETED, session.getSessionStatus());
        assertEquals(LectureSession.UPLOAD_STATUS_UPLOADED, session.getUploadStatus());
        assertEquals("TCP IP protocol suite enables reliable communication.", session.getTranscript());
    }
}
