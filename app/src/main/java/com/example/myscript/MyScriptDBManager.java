package com.example.myscript;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MyScriptDBManager {

    private SQLiteDatabase db;
    private final Context context;
    private MyScriptDB dbHelper;

    public MyScriptDBManager(Context c) {
        context = c;
        dbHelper = new MyScriptDB(c);
    }

    //Открытие БД на запись
    public void openDBWrite() {
        db = dbHelper.getWritableDatabase();
    }

    //Открытие БД на чтение
    public void openDBRead() {
        db = dbHelper.getReadableDatabase();
    }

    //Закрытие БД
    public void closeDB() {
        db.close();
    }

    //Получение списка контактов
    public List<ScriptRecord> getTasksList() {

        List<ScriptRecord> ret_array = new ArrayList<ScriptRecord>();
        openDBRead();
        Cursor cursor = db.rawQuery("SELECT * FROM " + MyScriptDB.TABLE_SCRIPTS + " ORDER BY _id DESC", null);

        if (cursor != null && cursor.getCount() > 0) {

            int idColumnIndex = cursor.getColumnIndex(MyScriptDB.ROW_ID);
            int titleColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_TITLE);
            int contentColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CONTENT);
            int createdDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CREATED_DATE);
            int finishedColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISHED);
            int finishedDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISH_DATE);

            while (cursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                ret_array.add(new ScriptRecord(cursor.getInt(idColumnIndex), cursor.getString(titleColumnIndex),
                        cursor.getString(contentColumnIndex), cursor.getString(createdDateColumnIndex), cursor.getInt(finishedColumnIndex),
                        cursor.getString(finishedDateColumnIndex)));
            }
        }

        closeDB();
        return ret_array;
    }

    public ScriptRecord getOneScript(int scriptId) {

        ScriptRecord ret_script = new ScriptRecord(0,"","", "", 0, "");

        openDBRead();
        String where = String.format("%s=%d", MyScriptDB.ROW_ID, scriptId); //Указываем id строки для чтения
        Cursor cursor = db.query(MyScriptDB.TABLE_SCRIPTS, null, where, null, null, null, null);
        if (cursor != null && cursor.getCount() == 1) {
            cursor.moveToFirst();
            int idColumnIndex = cursor.getColumnIndex(MyScriptDB.ROW_ID);
            int titleColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_TITLE);
            int contentColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CONTENT);
            int createdDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CREATED_DATE);
            int finishedColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISHED);
            int finishedDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISH_DATE);

            ret_script.setRowId(cursor.getInt(idColumnIndex));
            ret_script.setTitle(cursor.getString(titleColumnIndex));
            ret_script.setContent(cursor.getString(contentColumnIndex));
            ret_script.setCreatedDate(cursor.getString(createdDateColumnIndex));
            ret_script.setFinished(cursor.getInt(finishedColumnIndex), cursor.getString(finishedDateColumnIndex));
        }

        closeDB();
        return ret_script;
    }

    public int insertScript (ScriptRecord sr) {

        openDBWrite();

        ContentValues insert_row = new ContentValues(5);

        insert_row.put(MyScriptDB.SCRIPTS_TITLE, sr.getTitle());
        insert_row.put(MyScriptDB.SCRIPTS_CONTENT, sr.getContent());
        //Добавляем текущую дату
        DateFormat df = new SimpleDateFormat(MyScriptDB.DB_DATETIME_FORMAT);
        Calendar calendar = Calendar.getInstance();
        String curr_date = df.format(calendar.getTime());
        insert_row.put(MyScriptDB.SCRIPTS_CREATED_DATE, curr_date);
        insert_row.put(MyScriptDB.SCRIPTS_FINISHED, MyScriptDB.MARK_AS_OPENED);
        insert_row.put(MyScriptDB.SCRIPTS_FINISH_DATE, "");
        int result = (int)db.insertOrThrow(MyScriptDB.TABLE_SCRIPTS, null, insert_row);
        closeDB();
        return result;
    }

    public int updateScript(ScriptRecord sr) {
        openDBWrite(); //Открываем БД на запись

        ContentValues update_row = new ContentValues(2); //Создаем строку со значениями для обновления

        update_row.put(MyScriptDB.SCRIPTS_TITLE, sr.getTitle());
        update_row.put(MyScriptDB.SCRIPTS_CONTENT, sr.getContent());

        String where = String.format("%s=%d", MyScriptDB.ROW_ID, sr.getRowId()); //Указываем id строки для обновления

        int result = db.update(MyScriptDB.TABLE_SCRIPTS, update_row, where, null);
        closeDB();
        return result;
    }

    public int deleteScript (int rowId) {
        openDBWrite();

        String where = String.format("%s=%d", MyScriptDB.ROW_ID, rowId);
        int result = db.delete(MyScriptDB.TABLE_SCRIPTS, where, null);
        closeDB();

        return result;
    }
}
