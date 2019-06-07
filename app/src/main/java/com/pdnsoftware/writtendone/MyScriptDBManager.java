package com.pdnsoftware.writtendone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.pdnsoftware.writtendone.MyScriptDB.TABLE_PICTURES;
import static com.pdnsoftware.writtendone.MyScriptDB.TABLE_SCRIPTS;

class MyScriptDBManager {

    private SQLiteDatabase db;
    private final MyScriptDB dbHelper;
    private final Context con;

    MyScriptDBManager(Context c) {
        dbHelper = new MyScriptDB(c);
        this.con = c;
    }

    //Открытие БД на запись
    private void openDBWrite() {
        db = dbHelper.getWritableDatabase();
    }

    //Открытие БД на чтение
    private void openDBRead() {
        db = dbHelper.getReadableDatabase();
    }

    //Закрытие БД
    private void closeDB() {
        db.close();
    }

    //Получение списка контактов
    List<ScriptRecord> getTasksList() {

        List<ScriptRecord> ret_array = new ArrayList<>();
        openDBRead();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SCRIPTS + " ORDER BY _id DESC", null);

        if (cursor != null && cursor.getCount() > 0) {

            int idColumnIndex = cursor.getColumnIndex(MyScriptDB.ROW_ID);
            int titleColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_TITLE);
            int contentColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CONTENT);
            int createdDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CREATED_DATE);
            int finishedColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISHED);
            int finishedDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISH_DATE);
            int pictCountColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_PICTCOUNT);

            while (cursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                ret_array.add(new ScriptRecord(cursor.getInt(idColumnIndex), cursor.getString(titleColumnIndex),
                        cursor.getString(contentColumnIndex), cursor.getString(createdDateColumnIndex), cursor.getInt(finishedColumnIndex),
                        cursor.getString(finishedDateColumnIndex), cursor.getInt(pictCountColumnIndex)));


            }
        }
        if (cursor != null) cursor.close();
        closeDB();
        return ret_array;
    }

    ScriptRecord getOneScript(int scriptId) {

        ScriptRecord ret_script = new ScriptRecord(-1,"","", "", 0, "");

        openDBRead();
        String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, scriptId); //Указываем id строки для чтения
        Cursor cursor = db.query(TABLE_SCRIPTS, null, where, null, null, null, null);
        if (cursor != null && cursor.getCount() == 1) {
            cursor.moveToFirst();
            int idColumnIndex = cursor.getColumnIndex(MyScriptDB.ROW_ID);
            int titleColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_TITLE);
            int contentColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CONTENT);
            int createdDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_CREATED_DATE);
            int finishedColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISHED);
            int finishedDateColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_FINISH_DATE);
            int pictCountColumnIndex = cursor.getColumnIndex(MyScriptDB.SCRIPTS_PICTCOUNT);

            ret_script.setRowId(cursor.getInt(idColumnIndex));
            ret_script.setTitle(cursor.getString(titleColumnIndex));
            ret_script.setContent(cursor.getString(contentColumnIndex));
            ret_script.setCreatedDate(cursor.getString(createdDateColumnIndex));
            ret_script.setFinished(cursor.getInt(finishedColumnIndex), cursor.getString(finishedDateColumnIndex));
            ret_script.setPictCount(cursor.getInt(pictCountColumnIndex));
        }
        if (cursor != null) cursor.close();
        closeDB();
        return ret_script;
    }

    int insertScript (ScriptRecord sr) {

        openDBWrite();

        ContentValues insert_row = new ContentValues(6);

        insert_row.put(MyScriptDB.SCRIPTS_TITLE, sr.getTitle());
        insert_row.put(MyScriptDB.SCRIPTS_CONTENT, sr.getContent());
        //Добавляем текущую дату
        DateFormat df = new SimpleDateFormat(MyScriptDB.DB_DATETIME_FORMAT, Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String curr_date = df.format(calendar.getTime());
        insert_row.put(MyScriptDB.SCRIPTS_CREATED_DATE, curr_date);
        insert_row.put(MyScriptDB.SCRIPTS_FINISHED, MyScriptDB.MARK_AS_OPENED);
        insert_row.put(MyScriptDB.SCRIPTS_FINISH_DATE, "");
        insert_row.put(MyScriptDB.SCRIPTS_PICTCOUNT, 0);

        int result = (int)db.insertOrThrow(TABLE_SCRIPTS, null, insert_row);

        closeDB();

        return result;
    }

    void updateScript(ScriptRecord sr) {
        openDBWrite(); //Открываем БД на запись

        ContentValues update_row = new ContentValues(2); //Создаем строку со значениями для обновления

        update_row.put(MyScriptDB.SCRIPTS_TITLE, sr.getTitle());
        update_row.put(MyScriptDB.SCRIPTS_CONTENT, sr.getContent());

        String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, sr.getRowId()); //Указываем id строки для обновления

        db.update(TABLE_SCRIPTS, update_row, where, null);
        closeDB();
    }

    void deleteScript (int rowId) {

        deletePictureRecordByScriptId(rowId);

        openDBWrite();
        String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, rowId);
        db.delete(TABLE_SCRIPTS, where, null);
        closeDB();

    }

    //Добавляем запись о месте хранения фотографии
    int insertPictureRecord (int scriptId, String filePath, String fileName) {
        openDBWrite();

        ContentValues insert_row = new ContentValues(5);

        insert_row.put(MyScriptDB.PICTURES_SCRIPT_ID, scriptId);
        insert_row.put(MyScriptDB.PICTURES_PICTURE_PATH, filePath);
        insert_row.put(MyScriptDB.PICTURES_PICTURE_FILENAME, fileName);

        //Добавляем текущую дату
        DateFormat df = new SimpleDateFormat(MyScriptDB.DB_DATETIME_FORMAT, Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String curr_date = df.format(calendar.getTime());
        insert_row.put(MyScriptDB.PICTURES_CREATED_DATE, curr_date);
        insert_row.put(MyScriptDB.PICTURES_THUMBNAIL, "");

        int result = (int)db.insertOrThrow(MyScriptDB.TABLE_PICTURES, null, insert_row);

        closeDB();

        updatePictCountByScriptId(scriptId);

        return result;
    }

    //Функция возвращает массив фоток одной записи
    List<PictureRecord> getOneScriptPictures(int scriptId) {

        List<PictureRecord> ret_array = new ArrayList<>();

        if (scriptId > 0) {

            openDBRead();
            Cursor cursor = db.rawQuery("SELECT * FROM " + MyScriptDB.TABLE_PICTURES + " WHERE "
                    + MyScriptDB.PICTURES_SCRIPT_ID + " = " + scriptId + " ORDER BY _id ASC", null);

            if (cursor != null && cursor.getCount() > 0) {

                int idColumnIndex = cursor.getColumnIndex(MyScriptDB.ROW_ID);
                int scriptIdColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_SCRIPT_ID);
                int picturePathColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_PICTURE_PATH);
                int pictureFilenameColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_PICTURE_FILENAME);
                int pictureCreatedDateColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_CREATED_DATE);
                int thumbnailColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_THUMBNAIL);

                while (cursor.moveToNext()) {
                    // Используем индекс для получения строки или числа
                    ret_array.add(new PictureRecord(cursor.getInt(idColumnIndex), cursor.getInt(scriptIdColumnIndex),
                            cursor.getString(picturePathColumnIndex), cursor.getString(pictureFilenameColumnIndex),
                            cursor.getString(pictureCreatedDateColumnIndex), cursor.getString(thumbnailColumnIndex)));
                }

            }

            if (cursor != null) cursor.close();
            closeDB();
        }
        return ret_array;
    }

    //Функция возвращает одну картинку по id
    private PictureRecord getOnePicture(int pictureId) {

        PictureRecord ret_record = new PictureRecord(-1, -1, "", "", "");

        openDBRead();

        Cursor cursor = db.rawQuery("SELECT * FROM " + MyScriptDB.TABLE_PICTURES + " WHERE "
                + MyScriptDB.ROW_ID + " = " + pictureId + " ORDER BY _id DESC", null);

        if (cursor != null && cursor.getCount() == 1) {

            int idColumnIndex = cursor.getColumnIndex(MyScriptDB.ROW_ID);
            int scriptIdColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_SCRIPT_ID);
            int picturePathColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_PICTURE_PATH);
            int pictureFilenameColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_PICTURE_FILENAME);
            int pictureCreatedDateColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_CREATED_DATE);
            int thumbnailColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_THUMBNAIL);

            cursor.moveToFirst();

            // Используем индекс для получения строки или числа
            ret_record.setRowId(cursor.getInt(idColumnIndex));
            ret_record.setScriptId(cursor.getInt(scriptIdColumnIndex));
            ret_record.setPicturePath(cursor.getString(picturePathColumnIndex));
            ret_record.setPictureName(cursor.getString(pictureFilenameColumnIndex));
            ret_record.setCreatedDate(cursor.getString(pictureCreatedDateColumnIndex));
            ret_record.setThumbnail(cursor.getString(thumbnailColumnIndex));
        }
        if (cursor != null) cursor.close();
        closeDB();
        return ret_record;
    }


    //Удаляем картинку из БД - конкретную картинку по id картинки
    int deletePictureRecordByPictId (int pictureId) {

        boolean fileDelResult = false;
        boolean thumbnailDelResult = false;
        int dbRecDeleteResult = -1;
        File pictDir = CameraView.createImageGallery(con);
        File thumbnailDir = CameraView.createThumbnailsGallery(con);

        PictureRecord picToDelete = this.getOnePicture(pictureId);

        if (picToDelete.getRowId() > 0) {
            //Удаляем файл с картинкой
            if (pictDir != null)
                fileDelResult = this.fileDelete(pictDir.getAbsolutePath()
                        + "/" + picToDelete.getPictureName());

            //Удаляем файл с тумбнейлом
            if (thumbnailDir != null)
            thumbnailDelResult = this.fileDelete(thumbnailDir.getAbsolutePath()
                    + "/" + picToDelete.getThumbnail());
        }

        if (fileDelResult && thumbnailDelResult) {
            //Вносим изменения в базу
            openDBWrite();
            String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, pictureId);
            dbRecDeleteResult = db.delete(MyScriptDB.TABLE_PICTURES, where, null);

            closeDB();

            //обновляем количество картинок у в таблице scripts
            updatePictCountByScriptId(picToDelete.getScriptId());
        }

        return dbRecDeleteResult;
    }

    //Удаляем картинки из БД - ВСЕ картинки, принадлежащие конкретной задаче
    //Функция возвращает количество удаленных строк
    private void deletePictureRecordByScriptId(int scriptId) {

        List<PictureRecord> listToDel = getOneScriptPictures(scriptId);

        if (listToDel.size() > 0) {
            for (int i = 0; i < listToDel.size(); i++) {
                deletePictureRecordByPictId(listToDel.get(i).getRowId());
            }
        }
    }

    //Функция удаляет файл. Возвращает true, если файл удален успешно. false, если при удалении
    //появилась ошибка
    private boolean fileDelete(String path) {
        boolean fileDeleteResult = true;

        File imgFile = new File(path);

        if (imgFile.exists()) {
            try {
                fileDeleteResult = imgFile.delete();
            }
            catch (SecurityException se) {
                 return false;
            }
        }
        return fileDeleteResult;
    }

    //Функция извлекает название задачи по id картинки для заголовка окна просмотра фоток
    String getTaskNameByPictId (int pictId) {
        String taskName = "";
        PictureRecord pictRec;
        ScriptRecord script;

        pictRec = this.getOnePicture(pictId);
        if (pictRec != null) {
            script = this.getOneScript(pictRec.getScriptId());

            if (script != null)
                taskName = script.getTitle();
        }

        return taskName;
    }

    //Функция считает количество картинок по указанному script_id
    private int picturesCounter(int scriptId) {
        int pictCount = 0;

        openDBRead();

        Cursor cursor = db.rawQuery("SELECT count(*) AS PICT_COUNT FROM " + MyScriptDB.TABLE_PICTURES + " WHERE "
                + MyScriptDB.PICTURES_SCRIPT_ID + " = " + scriptId, null);

        if (cursor != null && cursor.getCount() == 1) {

            int idPictCountIndex = cursor.getColumnIndex("PICT_COUNT");

            cursor.moveToFirst();

            // Используем индекс для получения строки или числа
            pictCount = cursor.getInt(idPictCountIndex);

        }
        if (cursor != null) cursor.close();
        closeDB();

        return pictCount;
    }

    //Функция обновляет количество фотографий в записи скрипта
    private void updatePictCountByScriptId(int scriptId) {

        int pictCount = picturesCounter(scriptId);

        ContentValues update_row = new ContentValues(1); //Создаем строку со значениями для обновления

        update_row.put(MyScriptDB.SCRIPTS_PICTCOUNT, pictCount);

        String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, scriptId); //Указываем id строки для обновления

        openDBWrite();
        db.update(TABLE_SCRIPTS, update_row, where, null);
        closeDB();
    }

    //Функция возвращает количество задач
    int getTasksCount() {
        int taskCount = 0;

        openDBRead();

        Cursor cursor = db.rawQuery("SELECT count(*) AS TASK_COUNT FROM " + TABLE_SCRIPTS, null);

        if (cursor != null && cursor.getCount() == 1) {
            int idTaskCountIndex = cursor.getColumnIndex("TASK_COUNT");
            cursor.moveToFirst();
            //Используем индекс для получения строки или числа
            taskCount = cursor.getInt(idTaskCountIndex);
        }

        if (cursor != null) cursor.close();
        closeDB();
        return taskCount;
    }

    //Функция добавляет имя thumbnail к записи о картинке
    void addThumbnailName (int pictRowId, File thumbnail) {

        ContentValues update_row = new ContentValues(1); //Создаем строку со значениями для обновления

        update_row.put(MyScriptDB.PICTURES_THUMBNAIL, thumbnail.getName());

        String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, pictRowId); //Указываем id строки для обновления

        openDBWrite();
        db.update(TABLE_PICTURES, update_row, where, null);
        closeDB();
    }

    //Создаем файлы для просмотра изображений
    List<PictureRecord> loadPictFilesToShow(int pictId) {

        List<PictureRecord> retArray;
        int scriptId = -1;

        if (pictId > 0) {
            //Определяем идентификатор задачи
            openDBRead();
            String[] tableColumns = new String[]{MyScriptDB.PICTURES_SCRIPT_ID};

            String where = String.format(Locale.getDefault(), "%s=%d", MyScriptDB.ROW_ID, pictId); //Указываем id строки для чтения

            Cursor cursor = db.query(TABLE_PICTURES, tableColumns, where, null, null, null, null);

            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                int scriptIdColumnIndex = cursor.getColumnIndex(MyScriptDB.PICTURES_SCRIPT_ID);

                scriptId = cursor.getInt(scriptIdColumnIndex);
            }
            if (cursor != null) cursor.close();
            closeDB();
        }
        else
            return null;

        //Считываем все файлы, которые прикреплены к задаче

        if (scriptId > 0) {

            retArray = getOneScriptPictures(scriptId);

            return retArray;
        }
        else
            return null;
    }
}