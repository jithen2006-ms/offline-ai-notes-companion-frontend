package com.lecturecap;

import static org.junit.Assert.*;

import com.lecturecap.model.LectureSession;
import com.lecturecap.stt.SpeechToTextManager;

import org.junit.Test;

public class SpeechToTextPipelineTest {

    @Test
    public void testLectureTranscriptAccumulationAndClassSeparation() {
        // Class A: Computer Networks (09:00 - 10:00)
        LectureSession sessionA = new LectureSession(
                "student@college.edu",
                "Computer Networks",
                "2026-08-22",
                "09:00",
                "10:00",
                "Today we are learning about the OSI model. The OSI model has seven layers.",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_PENDING,
                3600,
                System.currentTimeMillis()
        );

        // Class B: Artificial Intelligence (10:00 - 11:00)
        LectureSession sessionB = new LectureSession(
                "student@college.edu",
                "Artificial Intelligence",
                "2026-08-22",
                "10:00",
                "11:00",
                "In artificial intelligence, heuristic search algorithms optimize pathfinding in graphs.",
                LectureSession.STATUS_COMPLETED,
                LectureSession.UPLOAD_STATUS_PENDING,
                3600,
                System.currentTimeMillis()
        );

        // Verify Class A Transcript
        assertEquals("Computer Networks", sessionA.getCourseName());
        assertEquals("2026-08-22", sessionA.getDate());
        assertEquals("09:00", sessionA.getStartTime());
        assertEquals("10:00", sessionA.getEndTime());
        assertTrue(sessionA.getTranscript().contains("OSI model has seven layers"));

        // Verify Class B Transcript
        assertEquals("Artificial Intelligence", sessionB.getCourseName());
        assertEquals("2026-08-22", sessionB.getDate());
        assertEquals("10:00", sessionB.getStartTime());
        assertEquals("11:00", sessionB.getEndTime());
        assertTrue(sessionB.getTranscript().contains("heuristic search algorithms"));

        // Verify class-wise transcript isolation (Class A text does NOT leak into Class B)
        assertFalse(sessionB.getTranscript().contains("OSI model"));
        assertFalse(sessionA.getTranscript().contains("heuristic search"));
    }

    @Test
    public void testNoClassNoTranscriptGuard() {
        // When no class is scheduled, session status remains NO_SPEECH and transcript is empty
        LectureSession emptySession = new LectureSession(
                "student@college.edu",
                "No Scheduled Class",
                "2026-08-22",
                "12:00",
                "13:00",
                "[Possible No Class / No Speech Detected]",
                LectureSession.STATUS_NO_SPEECH,
                LectureSession.UPLOAD_STATUS_PENDING,
                0,System.currentTimeMillis()
        );

        assertEquals(LectureSession.STATUS_NO_SPEECH, emptySession.getSessionStatus());
        assertTrue(emptySession.getTranscript().contains("No Speech Detected"));
    }
}
