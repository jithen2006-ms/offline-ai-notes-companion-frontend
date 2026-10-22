package com.lecturecap.model;

import java.io.Serializable;

public class AcademicCalendarItem implements Serializable {

    public static final String TYPE_HOLIDAY = "HOLIDAY";
    public static final String TYPE_WORKING_DAY = "WORKING_DAY";
    public static final String TYPE_SPECIAL_WORKING_DAY = "SPECIAL_WORKING_DAY";

    private long id;
    private String date; // YYYY-MM-DD
    private String type; // HOLIDAY / WORKING_DAY / SPECIAL_WORKING_DAY
    private String description;
    private String effectiveDay; // e.g. "Monday" for special working days

    public AcademicCalendarItem() {
    }

    public AcademicCalendarItem(String date, String type, String description, String effectiveDay) {
        this.date = date;
        this.type = type;
        this.description = description;
        this.effectiveDay = effectiveDay;
    }

    public AcademicCalendarItem(long id, String date, String type, String description, String effectiveDay) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.description = description;
        this.effectiveDay = effectiveDay;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEffectiveDay() {
        return effectiveDay;
    }

    public void setEffectiveDay(String effectiveDay) {
        this.effectiveDay = effectiveDay;
    }

    public boolean isHoliday() {
        return TYPE_HOLIDAY.equalsIgnoreCase(type);
    }

    public boolean isSpecialWorkingDay() {
        return TYPE_SPECIAL_WORKING_DAY.equalsIgnoreCase(type);
    }
}
