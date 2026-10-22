package com.lecturecap.model;

import java.io.Serializable;

public class ScheduleDecisionResult implements Serializable {

    public static final String STATUS_RECORD_NORMAL = "RECORD_NORMAL_CLASS";
    public static final String STATUS_RECORD_EXTRA = "RECORD_EXTRA_CLASS";
    public static final String STATUS_BLOCKED_HOLIDAY = "BLOCKED_HOLIDAY";
    public static final String STATUS_BLOCKED_EXAM = "BLOCKED_EXAM";
    public static final String STATUS_BLOCKED_CANCELLED = "BLOCKED_CANCELLED";
    public static final String STATUS_NO_CLASS = "NO_CLASS_SCHEDULED";

    private boolean shouldRecord;
    private String decisionStatus;
    private String courseName;
    private String startTime;
    private String endTime;
    private String explanation;
    private int priorityTier; // 1 = Temp Change, 2 = Exam, 3 = Academic Cal, 4 = Timetable

    public ScheduleDecisionResult() {
    }

    public ScheduleDecisionResult(boolean shouldRecord, String decisionStatus, String courseName,
                                  String startTime, String endTime, String explanation, int priorityTier) {
        this.shouldRecord = shouldRecord;
        this.decisionStatus = decisionStatus;
        this.courseName = courseName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.explanation = explanation;
        this.priorityTier = priorityTier;
    }

    public boolean isShouldRecord() {
        return shouldRecord;
    }

    public void setShouldRecord(boolean shouldRecord) {
        this.shouldRecord = shouldRecord;
    }

    public String getDecisionStatus() {
        return decisionStatus;
    }

    public void setDecisionStatus(String decisionStatus) {
        this.decisionStatus = decisionStatus;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
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

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getPriorityTier() {
        return priorityTier;
    }

    public void setPriorityTier(int priorityTier) {
        this.priorityTier = priorityTier;
    }

    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime + " – " + endTime;
        }
        return "";
    }
}
