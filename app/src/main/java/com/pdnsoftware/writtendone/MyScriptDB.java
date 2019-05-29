package com.pdnsoftware.writtendone;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MyScriptDB extends SQLiteOpenHelper {

    private static final String DB_MYSCRIPT = "myscript_db.db";
    private static final Integer DB_VERSION = 1;

    //Таблица scripts - в ней хранятся основные записи задач
    static final String TABLE_SCRIPTS = "scripts";

    static final String ROW_ID = "_id";
    static final String SCRIPTS_TITLE = "title";
    static final String SCRIPTS_CONTENT = "content";
    static final String SCRIPTS_CREATED_DATE = "createddate";
    static final String SCRIPTS_FINISHED = "finished";
    static final String SCRIPTS_FINISH_DATE = "finishdate";
    static final String SCRIPTS_PICTCOUNT = "pictcount";

    //Таблица pictures - в ней хранятся записи о картинках (фото или из галлереи, которые
    // прикреплены к задачам
    static final String TABLE_PICTURES = "pictures";
    //public static final String ROW_ID = "_id"; - Не задаем повторно, т.к. константа уже объявлена,
    // но этот столбец есть в таблице
    static final String PICTURES_SCRIPT_ID          = "script_id";
    static final String PICTURES_PICTURE_PATH       = "picture_path";
    static final String PICTURES_PICTURE_FILENAME   = "picture_filename";
    static final String PICTURES_CREATED_DATE       = "createddate";
    static final String PICTURES_THUMBNAIL          = "thumbnail";

    static final String DB_DATETIME_FORMAT;

    static {
        DB_DATETIME_FORMAT = "dd.MM.yyyy kk:mm";
    }

    //public static final Integer MARK_AS_FINISHED = 1;
    static final int MARK_AS_OPENED = 0;

    static final String DEFAULT_DATE_STRING = "01.01.2010 00:00";
    static final int EMPTY_ROW_ID = 0;

    MyScriptDB(Context con) {
        super(con, DB_MYSCRIPT, null, DB_VERSION);
        Context con_var = con;
    }

    @Override
    public void onCreate (SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SCRIPTS
        + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + SCRIPTS_TITLE + " TEXT, "
                        + SCRIPTS_CONTENT + " TEXT, "
                + SCRIPTS_CREATED_DATE + " TEXT, "
                + SCRIPTS_FINISHED + " INTEGER, "
                + SCRIPTS_FINISH_DATE + " TEXT,"
                + SCRIPTS_PICTCOUNT + " INTEGER);"

        );

        db.execSQL("CREATE TABLE " + TABLE_PICTURES
                + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PICTURES_SCRIPT_ID + " INTEGER, "
                + PICTURES_PICTURE_PATH + " TEXT, "
                + PICTURES_PICTURE_FILENAME + " TEXT, "
                + PICTURES_CREATED_DATE + " TEXT, "
                + PICTURES_THUMBNAIL + " TEXT);"
        );

        // Вставляем тестовые данные
        ContentValues insert_row = new ContentValues();

        insert_row.put(SCRIPTS_TITLE, "Test record header");
        insert_row.put(SCRIPTS_CONTENT, "Test record content!");
        insert_row.put(SCRIPTS_CREATED_DATE, "26.12.2014 08:15");
        insert_row.put(SCRIPTS_FINISHED, MARK_AS_OPENED);
        insert_row.put(SCRIPTS_FINISH_DATE, "");
        insert_row.put(SCRIPTS_PICTCOUNT, 0);

        db.insert(TABLE_SCRIPTS, null, insert_row);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}