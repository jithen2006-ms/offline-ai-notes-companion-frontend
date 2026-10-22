package com.lecturecap.network;

import static org.junit.Assert.*;

import com.lecturecap.utils.DocumentStructureAnalyzer;

import org.junit.Test;

public class OnlineAiOcrServiceTest {

    @Test
    public void testApiConfigSettings() {
        assertTrue(ApiConfig.isOnlineRequired());
        assertEquals("http://10.24.99.3:8000/api/ai/vision-ocr", ApiConfig.DEFAULT_ONLINE_AI_VISION_URL);
        assertEquals("http://10.24.99.3:8000/api/stt/transcribe", ApiConfig.DEFAULT_ONLINE_STT_URL);
        assertEquals("http://10.24.99.3:8000/api/lectures/upload", ApiConfig.DEFAULT_BACKEND_UPLOAD_URL);
        assertEquals("X-AI-API-Key", ApiConfig.API_KEY_HEADER);
    }

    @Test
    public void testDocumentTypesForOnlineAiOcr() {
        assertEquals("WEEKLY_TIMETABLE", DocumentStructureAnalyzer.DocumentType.WEEKLY_TIMETABLE.name());
        assertEquals("ACADEMIC_CALENDAR", DocumentStructureAnalyzer.DocumentType.ACADEMIC_CALENDAR.name());
        assertEquals("EXAM_TIMETABLE", DocumentStructureAnalyzer.DocumentType.EXAM_TIMETABLE.name());
    }
}
