package com.lecturecap.model;

import java.io.Serializable;

public class ExamScheduleItem implements Serializable {

    private long id;
    private String examName;  // e.g. "DBMS Mid-Term Examination"
    private String date;      // YYYY-MM-DD
    private String startTime; // HH:mm
    private String endTime;   // HH:mm
    private String room;      // e.g. "Hall 301"

    public ExamScheduleItem() {
    }

    public ExamScheduleItem(String examName, String date, String startTime, String endTime, String room) {
        this.examName = examName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    public ExamScheduleItem(long id, String examName, String date, String startTime, String endTime, String room) {
        this.id = id;
        this.examName = examName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
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

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTimeRange() {
        return startTime + " – " + endTime;
    }
}
