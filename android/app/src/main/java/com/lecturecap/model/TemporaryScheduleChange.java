package com.lecturecap.model;

import java.io.Serializable;

public class TemporaryScheduleChange implements Serializable {

    public static final String TYPE_CLASS_CANCELLED = "CLASS_CANCELLED";
    public static final String TYPE_EXTRA_CLASS = "EXTRA_CLASS";
    public static final String TYPE_CLASS_RESCHEDULED = "CLASS_RESCHEDULED";
    public static final String TYPE_DATE_HOLIDAY = "DATE_HOLIDAY";
    public static final String TYPE_SPECIAL_WORKING_DAY = "SPECIAL_WORKING_DAY";

    private long id;
    private String date; // YYYY-MM-DD
    private String changeType; // CLASS_CANCELLED / EXTRA_CLASS / CLASS_RESCHEDULED / DATE_HOLIDAY / SPECIAL_WORKING_DAY
    private String courseName; // e.g. "DBMS" or null/empty if affects whole day
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private String reason; // e.g. "Professor on medical leave", "Extra project discussion"
    private String substituteDay; // e.g. "Monday" for special working days

    public TemporaryScheduleChange() {
    }

    public TemporaryScheduleChange(String date, String changeType, String courseName,
                                   String startTime, String endTime, String reason, String substituteDay) {
        this.date = date;
        this.changeType = changeType;
        this.courseName = courseName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.substituteDay = substituteDay;
    }

    public TemporaryScheduleChange(long id, String date, String changeType, String courseName,
                                   String startTime, String endTime, String reason, String substituteDay) {
        this.id = id;
        this.date = date;
        this.changeType = changeType;
        this.courseName = courseName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.substituteDay = substituteDay;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSubstituteDay() {
        return substituteDay;
    }

    public void setSubstituteDay(String substituteDay) {
        this.substituteDay = substituteDay;
    }

    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime + " – " + endTime;
        }
        return "All Day";
    }
}
