package com.lecturecap.utils;

import android.content.Context;

import com.lecturecap.database.DatabaseHelper;
import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.TemporaryScheduleChange;
import com.lecturecap.model.TimetableItem;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleImporter {

    public static int importAcademicCalendarJson(Context context, String jsonStr) throws Exception {
        JSONArray array = new JSONArray(jsonStr);
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        int count = 0;

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String date = obj.getString("date");
            String type = obj.optString("type", AcademicCalendarItem.TYPE_HOLIDAY);
            String desc = obj.optString("description", "Holiday");
            String effDay = obj.optString("effectiveDay", null);

            AcademicCalendarItem item = new AcademicCalendarItem(date, type, desc, effDay);
            db.addAcademicCalendarItem(item);
            count++;
        }
        return count;
    }

    public static int importExamScheduleJson(Context context, String jsonStr) throws Exception {
        JSONArray array = new JSONArray(jsonStr);
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        int count = 0;

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String name = obj.getString("examName");
            String date = obj.getString("date");
            String start = obj.getString("startTime");
            String end = obj.getString("endTime");
            String room = obj.optString("room", "Main Hall");

            ExamScheduleItem item = new ExamScheduleItem(name, date, start, end, room);
            db.addExamScheduleItem(item);
            count++;
        }
        return count;
    }

    public static int importTemporaryChangesJson(Context context, String jsonStr) throws Exception {
        JSONArray array = new JSONArray(jsonStr);
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        int count = 0;

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String date = obj.getString("date");
            String type = obj.getString("changeType");
            String course = obj.optString("courseName", null);
            String start = obj.optString("startTime", null);
            String end = obj.optString("endTime", null);
            String reason = obj.optString("reason", "Notice");
            String subDay = obj.optString("substituteDay", null);

            TemporaryScheduleChange item = new TemporaryScheduleChange(date, type, course, start, end, reason, subDay);
            db.addTemporaryScheduleChange(item);
            count++;
        }
        return count;
    }
}
