package com.lecturecap.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.lecturecap.model.AcademicCalendarItem;
import com.lecturecap.model.ExamScheduleItem;
import com.lecturecap.model.LectureSession;
import com.lecturecap.model.TemporaryScheduleChange;
import com.lecturecap.model.TimetableItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "LECTURE_SESSION";
    private static final String DATABASE_NAME = "lecture_capture.db";
    private static final int DATABASE_VERSION = 3; // Version 3 adds segments_json column

    // 1. Timetable table
    public static final String TABLE_TIMETABLE = "timetable";
    public static final String COL_TT_ID = "id";
    public static final String COL_TT_COURSE = "course_name";
    public static final String COL_TT_DAY = "day_of_week";
    public static final String COL_TT_START = "start_time";
    public static final String COL_TT_END = "end_time";

    // 2. Lecture sessions / history table
    public static final String TABLE_LECTURES = "lecture_sessions";
    public static final String COL_LEC_ID = "id";
    public static final String COL_LEC_USER_ID = "user_id";
    public static final String COL_LEC_COURSE = "course_name";
    public static final String COL_LEC_DATE = "date";
    public static final String COL_LEC_START = "start_time";
    public static final String COL_LEC_END = "end_time";
    public static final String COL_LEC_TRANSCRIPT = "transcript";
    public static final String COL_LEC_SEGMENTS_JSON = "segments_json";
    public static final String COL_LEC_SESSION_STATUS = "session_status";
    public static final String COL_LEC_UPLOAD_STATUS = "upload_status";
    public static final String COL_LEC_DURATION = "duration_seconds";
    public static final String COL_LEC_CREATED_AT = "created_at";

    // 3. Academic Calendar table
    public static final String TABLE_ACADEMIC_CALENDAR = "academic_calendar";
    public static final String COL_ACAD_ID = "id";
    public static final String COL_ACAD_DATE = "date";
    public static final String COL_ACAD_TYPE = "type";
    public static final String COL_ACAD_DESC = "description";
    public static final String COL_ACAD_EFFECTIVE_DAY = "effective_day";

    // 4. Exam Schedule table
    public static final String TABLE_EXAMS = "exam_schedule";
    public static final String COL_EXAM_ID = "id";
    public static final String COL_EXAM_NAME = "exam_name";
    public static final String COL_EXAM_DATE = "date";
    public static final String COL_EXAM_START = "start_time";
    public static final String COL_EXAM_END = "end_time";
    public static final String COL_EXAM_ROOM = "room";

    // 5. Temporary Schedule Changes table
    public static final String TABLE_TEMP_CHANGES = "temporary_schedule_changes";
    public static final String COL_TEMP_ID = "id";
    public static final String COL_TEMP_DATE = "date";
    public static final String COL_TEMP_TYPE = "change_type";
    public static final String COL_TEMP_COURSE = "course_name";
    public static final String COL_TEMP_START = "start_time";
    public static final String COL_TEMP_END = "end_time";
    public static final String COL_TEMP_REASON = "reason";
    public static final String COL_TEMP_SUBSTITUTE_DAY = "substitute_day";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Timetable
        String createTimetableQuery = "CREATE TABLE " + TABLE_TIMETABLE + " ("
                + COL_TT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TT_COURSE + " TEXT NOT NULL, "
                + COL_TT_DAY + " TEXT NOT NULL, "
                + COL_TT_START + " TEXT NOT NULL, "
                + COL_TT_END + " TEXT NOT NULL)";
        db.execSQL(createTimetableQuery);

        // Lecture sessions
        String createLecturesQuery = "CREATE TABLE " + TABLE_LECTURES + " ("
                + COL_LEC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_LEC_USER_ID + " TEXT, "
                + COL_LEC_COURSE + " TEXT NOT NULL, "
                + COL_LEC_DATE + " TEXT NOT NULL, "
                + COL_LEC_START + " TEXT NOT NULL, "
                + COL_LEC_END + " TEXT NOT NULL, "
                + COL_LEC_TRANSCRIPT + " TEXT, "
                + COL_LEC_SEGMENTS_JSON + " TEXT, "
                + COL_LEC_SESSION_STATUS + " TEXT, "
                + COL_LEC_UPLOAD_STATUS + " TEXT, "
                + COL_LEC_DURATION + " INTEGER, "
                + COL_LEC_CREATED_AT + " INTEGER)";
        db.execSQL(createLecturesQuery);

        // Academic Calendar
        String createAcadQuery = "CREATE TABLE " + TABLE_ACADEMIC_CALENDAR + " ("
                + COL_ACAD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ACAD_DATE + " TEXT NOT NULL, "
                + COL_ACAD_TYPE + " TEXT NOT NULL, "
                + COL_ACAD_DESC + " TEXT NOT NULL, "
                + COL_ACAD_EFFECTIVE_DAY + " TEXT)";
        db.execSQL(createAcadQuery);

        // Exam Schedule
        String createExamQuery = "CREATE TABLE " + TABLE_EXAMS + " ("
                + COL_EXAM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EXAM_NAME + " TEXT NOT NULL, "
                + COL_EXAM_DATE + " TEXT NOT NULL, "
                + COL_EXAM_START + " TEXT NOT NULL, "
                + COL_EXAM_END + " TEXT NOT NULL, "
                + COL_EXAM_ROOM + " TEXT)";
        db.execSQL(createExamQuery);

        // Temporary Schedule Changes
        String createTempQuery = "CREATE TABLE " + TABLE_TEMP_CHANGES + " ("
                + COL_TEMP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TEMP_DATE + " TEXT NOT NULL, "
                + COL_TEMP_TYPE + " TEXT NOT NULL, "
                + COL_TEMP_COURSE + " TEXT, "
                + COL_TEMP_START + " TEXT, "
                + COL_TEMP_END + " TEXT, "
                + COL_TEMP_REASON + " TEXT, "
                + COL_TEMP_SUBSTITUTE_DAY + " TEXT)";
        db.execSQL(createTempQuery);

        // Seed default sample data for testing
        seedSampleTimetable(db);
        seedSampleAcademicCalendar(db);
        seedSampleExams(db);
        seedSampleTemporaryChanges(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_LECTURES + " ADD COLUMN " + COL_LEC_SEGMENTS_JSON + " TEXT");
                Log.d(TAG, "Database upgraded to v3: added segments_json column");
            } catch (Exception e) {
                Log.e(TAG, "Failed to alter table for v3 upgrade", e);
            }
        }
    }

    private void seedSampleTimetable(SQLiteDatabase db) {
        insertSeedCourse(db, "DBMS", "Monday", "09:00", "10:00");
        insertSeedCourse(db, "Computer Networks", "Monday", "10:00", "11:00");
        insertSeedCourse(db, "Java Programming", "Monday", "11:00", "12:00");

        insertSeedCourse(db, "Operating Systems", "Tuesday", "09:00", "10:00");
        insertSeedCourse(db, "Software Engineering", "Tuesday", "10:00", "11:00");
        insertSeedCourse(db, "DBMS Lab", "Tuesday", "11:00", "13:00");

        insertSeedCourse(db, "Computer Networks", "Wednesday", "09:00", "10:00");
        insertSeedCourse(db, "Java Programming", "Wednesday", "10:00", "11:00");
        insertSeedCourse(db, "Web Technologies", "Wednesday", "11:00", "12:00");

        insertSeedCourse(db, "Theory of Computation", "Thursday", "09:00", "10:00");
        insertSeedCourse(db, "Operating Systems", "Thursday", "10:00", "11:00");
        insertSeedCourse(db, "Networks Lab", "Thursday", "11:00", "13:00");

        insertSeedCourse(db, "Java Programming", "Friday", "09:00", "10:00");
        insertSeedCourse(db, "DBMS", "Friday", "10:00", "11:00");
        insertSeedCourse(db, "Minor Project Lab", "Friday", "11:00", "13:00");
    }

    private void insertSeedCourse(SQLiteDatabase db, String course, String day, String start, String end) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TT_COURSE, course);
        cv.put(COL_TT_DAY, day);
        cv.put(COL_TT_START, start);
        cv.put(COL_TT_END, end);
        db.insert(TABLE_TIMETABLE, null, cv);
    }

    private void seedSampleAcademicCalendar(SQLiteDatabase db) {
        insertSeedAcad(db, "2026-08-15", AcademicCalendarItem.TYPE_HOLIDAY, "Independence Day", null);
        insertSeedAcad(db, "2026-10-02", AcademicCalendarItem.TYPE_HOLIDAY, "Gandhi Jayanti", null);
        insertSeedAcad(db, "2026-10-31", AcademicCalendarItem.TYPE_HOLIDAY, "Diwali Festival Break", null);
        insertSeedAcad(db, "2026-08-29", AcademicCalendarItem.TYPE_SPECIAL_WORKING_DAY, "Saturday working as Monday Timetable", "Monday");
    }

    private void insertSeedAcad(SQLiteDatabase db, String date, String type, String desc, String effDay) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ACAD_DATE, date);
        cv.put(COL_ACAD_TYPE, type);
        cv.put(COL_ACAD_DESC, desc);
        cv.put(COL_ACAD_EFFECTIVE_DAY, effDay);
        db.insert(TABLE_ACADEMIC_CALENDAR, null, cv);
    }

    private void seedSampleExams(SQLiteDatabase db) {
        insertSeedExam(db, "DBMS Mid-Term Exam", "2026-09-15", "10:00", "12:00", "Exam Hall A");
        insertSeedExam(db, "Computer Networks Mid-Term Exam", "2026-09-17", "14:00", "16:00", "Exam Hall B");
        insertSeedExam(db, "Java Programming Mid-Term Exam", "2026-09-19", "09:30", "11:30", "Exam Hall C");
    }

    private void insertSeedExam(SQLiteDatabase db, String name, String date, String start, String end, String room) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EXAM_NAME, name);
        cv.put(COL_EXAM_DATE, date);
        cv.put(COL_EXAM_START, start);
        cv.put(COL_EXAM_END, end);
        cv.put(COL_EXAM_ROOM, room);
        db.insert(TABLE_EXAMS, null, cv);
    }

    private void seedSampleTemporaryChanges(SQLiteDatabase db) {
        insertSeedTemp(db, "2026-08-25", TemporaryScheduleChange.TYPE_CLASS_CANCELLED, "DBMS", "09:00", "10:00", "Faculty attending conference", null);
        insertSeedTemp(db, "2026-08-26", TemporaryScheduleChange.TYPE_EXTRA_CLASS, "Java Programming", "14:00", "15:30", "Extra revision class before internal assessment", null);
    }

    private void insertSeedTemp(SQLiteDatabase db, String date, String type, String course, String start, String end, String reason, String subDay) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TEMP_DATE, date);
        cv.put(COL_TEMP_TYPE, type);
        cv.put(COL_TEMP_COURSE, course);
        cv.put(COL_TEMP_START, start);
        cv.put(COL_TEMP_END, end);
        cv.put(COL_TEMP_REASON, reason);
        cv.put(COL_TEMP_SUBSTITUTE_DAY, subDay);
        db.insert(TABLE_TEMP_CHANGES, null, cv);
    }

    // ================= TIMETABLE CRUD =================

    public long addTimetableItem(TimetableItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TT_COURSE, item.getCourseName());
        cv.put(COL_TT_DAY, item.getDayOfWeek());
        cv.put(COL_TT_START, item.getStartTime());
        cv.put(COL_TT_END, item.getEndTime());
        return db.insert(TABLE_TIMETABLE, null, cv);
    }

    public int updateTimetableItem(TimetableItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TT_COURSE, item.getCourseName());
        cv.put(COL_TT_DAY, item.getDayOfWeek());
        cv.put(COL_TT_START, item.getStartTime());
        cv.put(COL_TT_END, item.getEndTime());
        return db.update(TABLE_TIMETABLE, cv, COL_TT_ID + " = ?", new String[]{String.valueOf(item.getId())});
    }

    public int deleteTimetableItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TIMETABLE, COL_TT_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void clearAllTimetableItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TIMETABLE, null, null);
    }

    public List<TimetableItem> getTimetableByDay(String dayOfWeek) {
        List<TimetableItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TIMETABLE, null,
                COL_TT_DAY + " = ?",
                new String[]{dayOfWeek},
                null, null, COL_TT_START + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TT_ID));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_COURSE));
                String day = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_DAY));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_START));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_END));
                list.add(new TimetableItem(id, course, day, start, end));
            }
            cursor.close();
        }
        return list;
    }

    public List<TimetableItem> getAllTimetableItems() {
        List<TimetableItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TIMETABLE, null, null, null, null, null, COL_TT_DAY + ", " + COL_TT_START + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TT_ID));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_COURSE));
                String day = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_DAY));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_START));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_TT_END));
                list.add(new TimetableItem(id, course, day, start, end));
            }
            cursor.close();
        }
        return list;
    }

    // ================= LECTURE SESSIONS CRUD =================

    public long addLectureSession(LectureSession session) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LEC_USER_ID, session.getUserId());
        cv.put(COL_LEC_COURSE, session.getCourseName());
        cv.put(COL_LEC_DATE, session.getDate());
        cv.put(COL_LEC_START, session.getStartTime());
        cv.put(COL_LEC_END, session.getEndTime());
        cv.put(COL_LEC_TRANSCRIPT, session.getTranscript());
        cv.put(COL_LEC_SEGMENTS_JSON, session.getSegmentsJsonStr());
        cv.put(COL_LEC_SESSION_STATUS, session.getSessionStatus());
        cv.put(COL_LEC_UPLOAD_STATUS, session.getUploadStatus());
        cv.put(COL_LEC_DURATION, session.getDurationSeconds());
        cv.put(COL_LEC_CREATED_AT, session.getCreatedAt());

        long rowId = db.insert(TABLE_LECTURES, null, cv);
        Log.d(TAG, "[DATABASE_SAVE] Persisted lecture session ID=" + rowId + ", course=" + session.getCourseName() + ", transcript_len=" + (session.getTranscript() != null ? session.getTranscript().length() : 0));
        return rowId;
    }

    public int updateLectureSession(LectureSession session) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LEC_TRANSCRIPT, session.getTranscript());
        cv.put(COL_LEC_SEGMENTS_JSON, session.getSegmentsJsonStr());
        cv.put(COL_LEC_SESSION_STATUS, session.getSessionStatus());
        cv.put(COL_LEC_UPLOAD_STATUS, session.getUploadStatus());
        cv.put(COL_LEC_DURATION, session.getDurationSeconds());
        return db.update(TABLE_LECTURES, cv, COL_LEC_ID + " = ?", new String[]{String.valueOf(session.getId())});
    }

    public int updateUploadStatus(long id, String uploadStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LEC_UPLOAD_STATUS, uploadStatus);
        int rows = db.update(TABLE_LECTURES, cv, COL_LEC_ID + " = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "[DATABASE_UPLOAD_STATUS_UPDATE] Session ID=" + id + " upload_status=" + uploadStatus);
        return rows;
    }

    public List<LectureSession> getAllLectureSessions() {
        List<LectureSession> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LECTURES, null, null, null, null, null, COL_LEC_CREATED_AT + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                LectureSession session = parseSessionFromCursor(cursor);
                list.add(session);
            }
            cursor.close();
        }
        return list;
    }

    public LectureSession getLectureSessionById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LECTURES, null, COL_LEC_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        LectureSession session = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                session = parseSessionFromCursor(cursor);
            }
            cursor.close();
        }
        return session;
    }

    private LectureSession parseSessionFromCursor(Cursor cursor) {
        LectureSession session = new LectureSession();
        session.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_LEC_ID)));
        session.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_USER_ID)));
        session.setCourseName(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_COURSE)));
        session.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_DATE)));
        session.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_START)));
        session.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_END)));
        session.setTranscript(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_TRANSCRIPT)));

        int segColIdx = cursor.getColumnIndex(COL_LEC_SEGMENTS_JSON);
        if (segColIdx >= 0) {
            session.setSegmentsJsonStr(cursor.getString(segColIdx));
        }

        session.setSessionStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_SESSION_STATUS)));
        session.setUploadStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEC_UPLOAD_STATUS)));
        session.setDurationSeconds(cursor.getLong(cursor.getColumnIndexOrThrow(COL_LEC_DURATION)));
        session.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_LEC_CREATED_AT)));
        return session;
    }

    // ================= ACADEMIC CALENDAR CRUD =================

    public long addAcademicCalendarItem(AcademicCalendarItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ACAD_DATE, item.getDate());
        cv.put(COL_ACAD_TYPE, item.getType());
        cv.put(COL_ACAD_DESC, item.getDescription());
        cv.put(COL_ACAD_EFFECTIVE_DAY, item.getEffectiveDay());
        return db.insert(TABLE_ACADEMIC_CALENDAR, null, cv);
    }

    public int deleteAcademicCalendarItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_ACADEMIC_CALENDAR, COL_ACAD_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<AcademicCalendarItem> getAllAcademicCalendarItems() {
        List<AcademicCalendarItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACADEMIC_CALENDAR, null, null, null, null, null, COL_ACAD_DATE + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ACAD_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_DATE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_TYPE));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_DESC));
                String eff = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_EFFECTIVE_DAY));
                list.add(new AcademicCalendarItem(id, date, type, desc, eff));
            }
            cursor.close();
        }
        return list;
    }

    public AcademicCalendarItem getAcademicCalendarItemForDate(String dateIso) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACADEMIC_CALENDAR, null, COL_ACAD_DATE + " = ?", new String[]{dateIso}, null, null, null);
        AcademicCalendarItem item = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ACAD_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_DATE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_TYPE));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_DESC));
                String eff = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACAD_EFFECTIVE_DAY));
                item = new AcademicCalendarItem(id, date, type, desc, eff);
            }
            cursor.close();
        }
        return item;
    }

    // ================= EXAM SCHEDULE CRUD =================

    public long addExamScheduleItem(ExamScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXAM_NAME, item.getExamName());
        cv.put(COL_EXAM_DATE, item.getDate());
        cv.put(COL_EXAM_START, item.getStartTime());
        cv.put(COL_EXAM_END, item.getEndTime());
        cv.put(COL_EXAM_ROOM, item.getRoom());
        return db.insert(TABLE_EXAMS, null, cv);
    }

    public int deleteExamScheduleItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EXAMS, COL_EXAM_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<ExamScheduleItem> getAllExamScheduleItems() {
        List<ExamScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXAMS, null, null, null, null, null, COL_EXAM_DATE + " ASC, " + COL_EXAM_START + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXAM_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_DATE));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_START));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_END));
                String room = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_ROOM));
                list.add(new ExamScheduleItem(id, name, date, start, end, room));
            }
            cursor.close();
        }
        return list;
    }

    public List<ExamScheduleItem> getExamScheduleItemsForDate(String dateIso) {
        List<ExamScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXAMS, null, COL_EXAM_DATE + " = ?", new String[]{dateIso}, null, null, COL_EXAM_START + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXAM_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_DATE));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_START));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_END));
                String room = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAM_ROOM));
                list.add(new ExamScheduleItem(id, name, date, start, end, room));
            }
            cursor.close();
        }
        return list;
    }

    // ================= TEMPORARY SCHEDULE CHANGES CRUD =================

    public long addTemporaryScheduleChange(TemporaryScheduleChange item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TEMP_DATE, item.getDate());
        cv.put(COL_TEMP_TYPE, item.getChangeType());
        cv.put(COL_TEMP_COURSE, item.getCourseName());
        cv.put(COL_TEMP_START, item.getStartTime());
        cv.put(COL_TEMP_END, item.getEndTime());
        cv.put(COL_TEMP_REASON, item.getReason());
        cv.put(COL_TEMP_SUBSTITUTE_DAY, item.getSubstituteDay());
        return db.insert(TABLE_TEMP_CHANGES, null, cv);
    }

    public int deleteTemporaryScheduleChange(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TEMP_CHANGES, COL_TEMP_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<TemporaryScheduleChange> getAllTemporaryScheduleChanges() {
        List<TemporaryScheduleChange> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEMP_CHANGES, null, null, null, null, null, COL_TEMP_DATE + " ASC, " + COL_TEMP_START + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TEMP_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_DATE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_TYPE));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_COURSE));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_START));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_END));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_REASON));
                String subDay = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_SUBSTITUTE_DAY));
                list.add(new TemporaryScheduleChange(id, date, type, course, start, end, reason, subDay));
            }
            cursor.close();
        }
        return list;
    }

    public List<TemporaryScheduleChange> getTemporaryScheduleChangesForDate(String dateIso) {
        List<TemporaryScheduleChange> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEMP_CHANGES, null, COL_TEMP_DATE + " = ?", new String[]{dateIso}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TEMP_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_DATE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_TYPE));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_COURSE));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_START));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_END));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_REASON));
                String subDay = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_SUBSTITUTE_DAY));
                list.add(new TemporaryScheduleChange(id, date, type, course, start, end, reason, subDay));
            }
            cursor.close();
        }
        return list;
    }

    public int clearAllLectureHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_LECTURES, null, null);
    }
}
