package com.lecturecap.model;

import org.json.JSONObject;
import java.io.Serializable;

public class LectureSession implements Serializable {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_NO_SPEECH = "POSSIBLE_NO_CLASS_OR_NO_SPEECH";
    public static final String STATUS_RECORDING = "RECORDING";

    public static final String UPLOAD_STATUS_UPLOADED = "UPLOADED";
    public static final String UPLOAD_STATUS_PENDING = "PENDING";
    public static final String UPLOAD_STATUS_FAILED = "FAILED";

    private long id;
    private String userId;
    private String courseName;
    private String date;        // e.g. "2026-08-21"
    private String startTime;   // e.g. "09:00"
    private String endTime;     // e.g. "10:00"
    private String transcript;
    private String segmentsJsonStr; // JSON string of {"segments": [...]}
    private String sessionStatus; // COMPLETED / POSSIBLE_NO_CLASS_OR_NO_SPEECH
    private String uploadStatus;  // UPLOADED / PENDING / FAILED
    private long durationSeconds;
    private long createdAt;

    public LectureSession() {
    }

    public LectureSession(String userId, String courseName, String date, String startTime,
                          String endTime, String transcript, String sessionStatus,
                          String uploadStatus, long durationSeconds, long createdAt) {
        this.userId = userId;
        this.courseName = courseName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.transcript = transcript;
        this.sessionStatus = sessionStatus;
        this.uploadStatus = uploadStatus;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSegmentsJsonStr() {
        return segmentsJsonStr;
    }

    public void setSegmentsJsonStr(String segmentsJsonStr) {
        this.segmentsJsonStr = segmentsJsonStr;
    }

    public boolean hasSegmentsJson() {
        return segmentsJsonStr != null && !segmentsJsonStr.trim().isEmpty();
    }

    public JSONObject getSegmentsJsonObject() {
        if (!hasSegmentsJson()) return null;
        try {
            return new JSONObject(segmentsJsonStr);
        } catch (Exception e) {
            return null;
        }
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimeRange() {
        return startTime + " – " + endTime;
    }
}
