package com.example.myscript;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyScriptDB extends SQLiteOpenHelper {

    public static final String DB_MYSCRIPT = "myscript_db.db";
    public static final Integer DB_VERSION = 2;
    public static final String TABLE_SCRIPTS = "scripts";

    public static final String ROW_ID = "_id";
    public static final String SCRIPTS_TITLE = "title";
    public static final String SCRIPTS_CONTENT = "content";
    public static final String SCRIPTS_CREATED_DATE = "createddate";
    public static final String SCRIPTS_FINISHED = "finished";
    public static final String SCRIPTS_FINISH_DATE = "finishdate";

    public static final String DB_DATETIME_FORMAT = "dd.MM.yyyy kk:mm";

    public static final Integer MARK_AS_FINISHED = 1;
    public static final Integer MARK_AS_OPENED = 0;

    public static final String DEFAULT_DATE_STRING = "01.01.2010 00:00";

    private Context con_var;

    public MyScriptDB(Context con) {
        super(con, DB_MYSCRIPT, null, DB_VERSION);
        this.con_var = con;
    }

    @Override
    public void onCreate (SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SCRIPTS
        + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + SCRIPTS_TITLE + " TEXT, "
                        + SCRIPTS_CONTENT + " TEXT, "
                + SCRIPTS_CREATED_DATE + " TEXT, "
                + SCRIPTS_FINISHED + " INTEGER, "
                + SCRIPTS_FINISH_DATE + " TEXT);"

        );

        // Вставляем тестовые данные
        ContentValues insert_row = new ContentValues();

        insert_row.put(SCRIPTS_TITLE, "Test record header");
        insert_row.put(SCRIPTS_CONTENT, "Test record content!");
        insert_row.put(SCRIPTS_CREATED_DATE, "");
        insert_row.put(SCRIPTS_FINISHED, MARK_AS_OPENED);
        insert_row.put(SCRIPTS_FINISH_DATE, "");

        db.insert(TABLE_SCRIPTS, null, insert_row);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_SCRIPTS + " ADD COLUMN " + SCRIPTS_CREATED_DATE + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_SCRIPTS + " ADD COLUMN " + SCRIPTS_FINISHED + " INTEGER;");
            db.execSQL("ALTER TABLE " + TABLE_SCRIPTS + " ADD COLUMN " + SCRIPTS_FINISH_DATE + " TEXT;");
        }
    }
}
