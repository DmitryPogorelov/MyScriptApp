package com.example.myscript;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScriptEdit extends AppCompatActivity {

    public EditText etScriptTitle, etScriptContent;
    public Button saveButton, saveAndAddButton;
    private MyScriptDBManager myDB;
    private Context app_context;

    int rowToUpdate = -1; //Идентификатор строки, которую будем обновлять

    Intent currIntent = new Intent(); //Объект Intent для считывания данных из вызывающей формы
    Bundle varSet; //объект для параметров вызывающей формы

    //Константы для обозначения полей при передаче между формами
    public static final String TITLE_FIELD_CONTENT = "title_field_content";
    public static final String CONTENT_FIELD_CONTENT = "content_field_content";

    public static final String NAME_INTENT_SCRIPTEDIT = "intent_ScriptEdit";

    //Константа для передачи идентификатора фоторгафии, которую нужно открывать, в форму PictureDisplay
    public static final String PICTIRE_ID = "pict_id";

    //Массив для хранения id записей о картинках из БД, которые были загружены.
    private int[] addedPictIds;

    //Константа с результатом возврата картинки из галлереи
    private static int RESULT_LOAD_IMAGE = 1;

    //Константа для журнала
    private static final String TAG = "MyScript-ScriptEditAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_edit);

        //Создаю массив входящих параметров формы
        loadIntentParams();

        //Инициализируем базу для работы с ней
        myDB = new MyScriptDBManager(app_context);

        //Связываем переменные с элементами управления
        etScriptTitle = findViewById(R.id.et_script_title);
        etScriptContent = findViewById(R.id.et_script_content);

        saveButton = findViewById(R.id.saveButton);
        saveAndAddButton = findViewById(R.id.saveAndAddButton);

        saveButton.setOnClickListener(seSaver);
        saveAndAddButton.setOnClickListener(seSaver);

        //Заводим ActionBar, чтобы на нем была стрелка назад
        ActionBar currActionBar = getSupportActionBar();

        if (currActionBar != null) {
            currActionBar.setDisplayHomeAsUpEnabled(true);
        }

        //Проверяем, есть ли входящий параметр с id записи для редактирования
        //Если есть, то считываем данные зи БД и заполняем поля
        if (rowToUpdate > 0) {

            ScriptRecord recToEdit = myDB.getOneScript(rowToUpdate);

            etScriptTitle.setText(recToEdit.getTitle());
            etScriptContent.setText(recToEdit.getContent());

            if (currActionBar != null)
                currActionBar.setTitle(R.string.headerEditTask);

            //Делаем кнопку "Сохранить и добавить еще невидимой"
            saveAndAddButton.setVisibility(View.INVISIBLE);


        }
        else
        {
            if (currActionBar != null)
                currActionBar.setTitle(R.string.headerNewTask);
        }

        //Если произшел возврат из экрана с камерой
        if (varSet != null && varSet.containsKey(MainActivity.CALLER_ACTIVITY_NAME)) {
            if (varSet.getString(MainActivity.CALLER_ACTIVITY_NAME).equals(CameraView.NAME_INTENT_CAMERAVIEW)) {
                //Восстановить значение полей
                if (varSet.containsKey(ScriptEdit.TITLE_FIELD_CONTENT))
                    etScriptTitle.setText(varSet.getString(ScriptEdit.TITLE_FIELD_CONTENT));

                if (varSet.containsKey(ScriptEdit.CONTENT_FIELD_CONTENT))
                    etScriptContent.setText(varSet.getString(ScriptEdit.CONTENT_FIELD_CONTENT));
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();


        //Загружаем фотографии
        if (rowToUpdate > 0)
            picturesLoad(rowToUpdate);

        Log.i(TAG, "On Resume DONE!");
    }

    private View.OnClickListener seSaver = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.saveButton:

                    if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID)) {
                        ScriptRecord script = new ScriptRecord(rowToUpdate, etScriptTitle.getText().toString(), etScriptContent.getText().toString());
                        //Проверяем запись на пустоту
                        if (isEmptyScriptRecord(script)) {
                            //Сообщаем пользователю о том, что он пытается сохранить пустую запись
                            Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.emptyRecordWarning), Toast.LENGTH_LONG).show();
                        }
                        //Обновляем непустую запись
                        if (!isEmptyScriptRecord(script)) {
                            myDB.updateScript(script);
                            finish();
                        }
                    }

                    if (varSet != null && !varSet.containsKey(MyScriptDB.ROW_ID)) {
                        ScriptRecord script = new ScriptRecord(0, etScriptTitle.getText().toString(), etScriptContent.getText().toString());
                        //Проверяем запись на пустоту
                        if (isEmptyScriptRecord(script)) {
                            //Сообщаем пользователю о том, что он пытается сохранить пустую запись
                            Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.emptyRecordWarning), Toast.LENGTH_LONG).show();
                        }
                        // Вставляем непустую запись
                        if (!isEmptyScriptRecord(script)) {
                            //Вставляем новую запись
                            int insert_res = myDB.insertScript(script);
                            //Проверяем, добавилась ли запись в БД
                            if (insert_res != -1)
                                //Запись добавлена успешно, закрываем окно
                                finish();
                            else
                            {
                                //Запись не добавлена. Выводим сообщение об ошибке
                                Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.errorInIsert), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    break;
                case R.id.saveAndAddButton:
                    //Сохраняем введенную запись
                    ScriptRecord script = new ScriptRecord(0, etScriptTitle.getText().toString(), etScriptContent.getText().toString());
                    if (isEmptyScriptRecord(script)) {
                        //Сообщаем пользователю о том, что он пытается сохранить пустую запись
                        Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.emptyRecordWarning), Toast.LENGTH_LONG).show();
                    }
                    if (!isEmptyScriptRecord(script)) {
                        myDB.insertScript(script);
                        //Оповещаем пользователя об успешном сохранении данных
                        Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.recordAddedSuccess), Toast.LENGTH_LONG).show();
                        //Очищаем поля
                        etScriptTitle.setText("");
                        etScriptContent.setText("");
                    }
                    break;
            }
        }
    };

    private boolean isEmptyScriptRecord (ScriptRecord sr) {
        boolean flag = false;
        if (sr.getTitle().isEmpty() && sr.getContent().isEmpty())
            flag = true;
        return flag;
    }

    //*********************Добавляется меню*******************************

    private ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.ma_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //Переменная для вызова меню
            ActionMode actionMode = null;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.script_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_photo:
                //Проверяем наличие в устройстве камеры
                PackageManager pm = app_context.getPackageManager();
                final boolean deviceHasCameraFlag = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
                if (deviceHasCameraFlag) {
                    //Открываем Activity  с камерой
                    Intent cameraIntent = new Intent();

                    cameraIntent.setClass(getApplicationContext(), CameraView.class);

                    //Сохраняем в новый Intent все нужные значения
                    if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID))
                        cameraIntent.putExtra(MyScriptDB.ROW_ID, varSet.getInt(MyScriptDB.ROW_ID));

                    cameraIntent.putExtra(ScriptEdit.TITLE_FIELD_CONTENT, etScriptTitle.getText().toString());
                    cameraIntent.putExtra(ScriptEdit.CONTENT_FIELD_CONTENT, etScriptContent.getText().toString());

                    startActivity(cameraIntent);
                }
                else {
                    Toast.makeText(app_context, getResources().getString(R.string.deviceHasNoCameraError), Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.add_picture:
                //Зарускаем галлерею
                Intent intGallery = new Intent();

                intGallery.setType("image/*");
                intGallery.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intGallery, "Select Picture"), RESULT_LOAD_IMAGE);

                break;

            case android.R.id.home:
                finish();
        }

        return true;
    }

    //*********************Окончание меню***************************************

    public void picturesLoad (int rowId) {

        //Загрузить фотографии
        List<PictureRecord> recPictures = myDB.getOneScriptPictures(rowId);

        pictures_viewer fragment;
        //Добавляем фрагмент для размещения картинок
        Fragment pictFragment = new pictures_viewer();

        // First get FragmentManager object.
        FragmentManager fragmentManager = this.getSupportFragmentManager();

        // Begin Fragment transaction.
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Replace the layout holder with the required Fragment object.
        fragmentTransaction.add(R.id.dynamic_fragment_frame_layout, pictFragment);

        // Commit the Fragment replace action.
        fragmentTransaction.commit();

        if (recPictures.size() > 0) {

            Bitmap myBitmap1, myBitmap2, myBitmap3;
            File imgFile;
            String pictPath;
            //Setting bitmap options to avoid missing of memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;

            //Инициализируем массив для хранения идентификаторов фотографий
            addedPictIds = new int[3];

            int j = 0;

            //Get fragment object
            fragment =  (pictures_viewer)getSupportFragmentManager().findFragmentById(R.id.dynamic_fragment_frame_layout);
            fragmentTransaction = fragmentManager.beginTransaction();

            if (fragment != null) {
                fragment.pict1.setVisibility(View.VISIBLE);
                fragment.pict2.setVisibility(View.VISIBLE);
                fragment.pict3.setVisibility(View.VISIBLE);
            }

            for (int i = 0; i < recPictures.size(); i++) {


                pictPath = recPictures.get(i).getPicturePath() + "/" + recPictures.get(i).getPictureName();

                imgFile = new File(pictPath);

                if (imgFile.exists() && j < 3 && fragment != null) {

                    if (j == 0) {
                        myBitmap1 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        fragment.setPicture1(myBitmap1);
                        fragment.pict1.setOnClickListener(openFullPicture);

                        addedPictIds[j] = recPictures.get(i).getRowId();

                    }
                    if (j == 1) {
                        myBitmap2 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        fragment.setPicture2(myBitmap2);
                        fragment.pict2.setOnClickListener(openFullPicture);

                        addedPictIds[j] = recPictures.get(i).getRowId();
                    }
                    if (j == 2) {
                        myBitmap3 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        fragment.setPicture3(myBitmap3);
                        fragment.pict3.setOnClickListener(openFullPicture);

                        addedPictIds[j] = recPictures.get(i).getRowId();
                    }
                    j++;
                }

            }

            //Скрываем ненужные объекты
            if (j == 1) {
                fragment.pict2.setVisibility(View.GONE);
                fragment.pict3.setVisibility(View.GONE);
            }
            if (j == 2) {
                fragment.pict3.setVisibility(View.GONE);
            }
            if (fragment != null)
                fragmentTransaction.replace(R.id.dynamic_fragment_frame_layout, fragment);
            fragmentTransaction.commit();

        }
        else {
            //Убираем весь фрагмент с фотками когда фоток в задаче нет
            fragment =  (pictures_viewer)getSupportFragmentManager().findFragmentById(R.id.dynamic_fragment_frame_layout);
            fragmentTransaction = fragmentManager.beginTransaction();
            if (fragment != null)
                fragment.hideLayout();
            fragmentTransaction.commit();
        }

    }

    //Выгрузка параметров, переданных в Activity
    private void loadIntentParams() {

        app_context = getApplicationContext();

        currIntent = getIntent();
        varSet = currIntent.getExtras();

        if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID))
            rowToUpdate = varSet.getInt(MyScriptDB.ROW_ID);  //Загружаем rowId, если он передан
    }

    public View.OnClickListener openFullPicture = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //Открываем Activity PictureDisplay для просмотра фотографии
            Intent PictureDisplayIntent = new Intent();

            PictureDisplayIntent.setClass(getApplicationContext(), PictureDisplay.class);
            //Сохраняем в новый Intent все нужные значения
            if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID))
                PictureDisplayIntent.putExtra(MyScriptDB.ROW_ID, varSet.getInt(MyScriptDB.ROW_ID));


            PictureDisplayIntent.putExtra(MainActivity.CALLER_ACTIVITY_NAME, ScriptEdit.NAME_INTENT_SCRIPTEDIT);
            PictureDisplayIntent.putExtra(ScriptEdit.TITLE_FIELD_CONTENT, etScriptTitle.getText().toString());
            PictureDisplayIntent.putExtra(ScriptEdit.CONTENT_FIELD_CONTENT, etScriptContent.getText().toString());

            switch (v.getId()) {
                case R.id.pict1:
                    PictureDisplayIntent.putExtra(ScriptEdit.PICTIRE_ID, addedPictIds[0]);
                    break;
                case R.id.pict2:
                    PictureDisplayIntent.putExtra(ScriptEdit.PICTIRE_ID, addedPictIds[1]);
                    break;
                case R.id.pict3:
                    PictureDisplayIntent.putExtra(ScriptEdit.PICTIRE_ID, addedPictIds[2]);
                    break;
            }

            startActivity(PictureDisplayIntent);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {

            try {

                File photoFile = null;
                try {
                    photoFile = createImageFile();

                } catch (IOException ex) {

                    Log.i(TAG, "Error occurred while creating the file");
                }

                if (photoFile != null && data.getData() != null) {

                    InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
                    FileOutputStream fileOutputStream = new FileOutputStream(photoFile);
                    // Copying
                    if (inputStream != null)
                        copyStream(inputStream, fileOutputStream);

                    fileOutputStream.close();
                    if (inputStream != null)
                        inputStream.close();


                    //Добавляем запись о файле в БД
                    //Создаем новую задачу в БД, если ее не было
                    if (rowToUpdate > 0) {
                        //Добавляем картинку к задаче
                        int pictureRecId = myDB.insertPictureRecord(rowToUpdate, photoFile.getParentFile().getAbsolutePath(), photoFile.getName());

                        if (pictureRecId == -1)
                            Toast.makeText(this, getResources().getString(R.string.photoNotAddedToDBError), Toast.LENGTH_LONG).show();
                    }
                    else if (rowToUpdate == -1) {
                        ScriptRecord newRec = new ScriptRecord(MyScriptDB.EMPTY_ROW_ID, "", "");
                        rowToUpdate = myDB.insertScript(newRec);

                        //Добавляем значение в набор varSet для корректной работы кнопки сохранения
                        if (varSet == null) {
                            varSet = new Bundle();
                            varSet.putInt(MyScriptDB.ROW_ID, rowToUpdate);
                        }
                        else if (!varSet.containsKey(MyScriptDB.ROW_ID)) {
                            varSet.putInt(MyScriptDB.ROW_ID, rowToUpdate);
                        }
                    }


                }
                else
                    Toast.makeText(this, getResources().getString(R.string.fileNotCopied), Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                Log.d(TAG, "onActivityResult: " + e.toString());
                Toast.makeText(this, getResources().getString(R.string.fileNotCopied), Toast.LENGTH_LONG).show();
            }

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));

        if (!galleryFolder.exists()) {
            if (!galleryFolder.mkdirs()) {
                galleryFolder = storageDirectory;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_file_" + timeStamp;

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                galleryFolder      /* directory */
        );
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}
