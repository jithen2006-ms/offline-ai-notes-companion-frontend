package com.lecturecap.model;

import java.io.Serializable;

public class TimetableItem implements Serializable {
    private long id;
    private String courseName;
    private String dayOfWeek;
    private String startTime; // e.g. "09:00"
    private String endTime;   // e.g. "10:00"

    public TimetableItem() {
    }

    public TimetableItem(String courseName, String dayOfWeek, String startTime, String endTime) {
        this.courseName = courseName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimetableItem(long id, String courseName, String dayOfWeek, String startTime, String endTime) {
        this.id = id;
        this.courseName = courseName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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

    public String getTimeRangeFormatted() {
        return startTime + " – " + endTime;
    }
}
