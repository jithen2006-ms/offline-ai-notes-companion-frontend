package com.lecturecap.network;

public class ApiConfig {

    // Default Online AI Vision & OCR Endpoint
    public static final String DEFAULT_ONLINE_AI_VISION_URL = "http://10.24.99.3:8000/api/ai/vision-ocr";
    
    // Default Online Speech-to-Text Endpoint / Service
    public static final String DEFAULT_ONLINE_STT_URL = "http://10.24.99.3:8000/api/stt/transcribe";

    // Default Backend Lecture Upload Endpoint
    public static final String DEFAULT_BACKEND_UPLOAD_URL = "http://10.24.99.3:8000/api/lectures/upload";

    // Optional API Key header parameter
    public static final String API_KEY_HEADER = "X-AI-API-Key";
    public static final String DEFAULT_API_KEY = "demo_classroom_ai_key";

    public static boolean isOnlineRequired() {
        return true;
    }
}
