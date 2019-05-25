package com.pdnsoftware.writtendone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

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

    private EditText etScriptTitle, etScriptContent;
    private MyScriptDBManager myDB;
    private Context app_context;

    private int rowToUpdate = -1; //Идентификатор строки, которую будем обновлять

    private Intent currIntent = new Intent(); //Объект Intent для считывания данных из вызывающей формы
    private Bundle varSet; //объект для параметров вызывающей формы

    //Константы для обозначения полей при передаче между формами
    public static final String TITLE_FIELD_CONTENT = "title_field_content";
    public static final String CONTENT_FIELD_CONTENT = "content_field_content";

    //Константа для передачи идентификатора фоторгафии, которую нужно открывать, в форму PictureDisplay
    public static final String PICTIRE_ID = "pict_id";

    //Массив для хранения id записей о картинках из БД, которые были загружены.
    private int[] addedPictIds;

    //Константа с результатом возврата картинки из галлереи
    private static final int RESULT_LOAD_IMAGE = 1;

    //Переменная для установки отступов между картинками
    private final int paddingDp = 4;
    private final int paddingPixel = 0;

    //Actionbar
    ActionBar currActionBar;

    //Кнопка "Сохранить и добавить"
    Button saveAndAddButton;

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

        etScriptTitle.addTextChangedListener(castFirstLetter);
        etScriptContent.addTextChangedListener(castFirstLetter);

        Button saveButton = findViewById(R.id.saveButton);
        saveAndAddButton = findViewById(R.id.saveAndAddButton);

        saveButton.setOnClickListener(seSaver);
        saveAndAddButton.setOnClickListener(seSaver);

        //Проверяем, не надо ли восстановить переменные из сохраненного перед уничтожением Активити массива
        if (savedInstanceState != null) {
            rowToUpdate = savedInstanceState.getInt(MyScriptDB.ROW_ID);
            etScriptTitle.setText(savedInstanceState.getString(ScriptEdit.TITLE_FIELD_CONTENT));
            etScriptContent.setText(savedInstanceState.getString(ScriptEdit.CONTENT_FIELD_CONTENT));
        }

        //Заводим ActionBar, чтобы на нем была стрелка назад
        //Переменная для управления ActionBarом
        currActionBar = getSupportActionBar();

        if (currActionBar != null) {
            currActionBar.setDisplayHomeAsUpEnabled(true);
        }

        //Проверяем, есть ли входящий параметр с id записи для редактирования
        //Если есть, то считываем данные зи БД и заполняем поля
        if (rowToUpdate > 0) {

            ScriptRecord recToEdit = myDB.getOneScript(rowToUpdate);

            if (savedInstanceState == null) {
                etScriptTitle.setText(recToEdit.getTitle());
                etScriptContent.setText(recToEdit.getContent());
            }

            if (currActionBar != null)
                currActionBar.setTitle(R.string.headerEditTask);
        }
        else
        {
            if (currActionBar != null)
                currActionBar.setTitle(R.string.headerNewTask);
        }

        //Загружаем фотографии
        picturesLoad(rowToUpdate);
    }

    private final View.OnClickListener seSaver = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        switch (v.getId()) {
            case R.id.saveButton:

                if (rowToUpdate > 0) {
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

                if (rowToUpdate <= 0) {
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
                        else {
                            //Запись не добавлена. Выводим сообщение об ошибке
                            Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.errorInsert), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            break;
            case R.id.saveAndAddButton:
                //Сохраняем введенную запись
                if (rowToUpdate <= 0) {
                    ScriptRecord script = new ScriptRecord(0, etScriptTitle.getText().toString(), etScriptContent.getText().toString());
                    if (isEmptyScriptRecord(script)) {
                        //Сообщаем пользователю о том, что он пытается сохранить пустую запись
                        Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.emptyRecordWarning), Toast.LENGTH_LONG).show();
                    }
                    if (!isEmptyScriptRecord(script)) {
                        myDB.insertScript(script);
                        //Оповещаем пользователя об успешном сохранении данных
                        Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.recordAddedSuccess), Toast.LENGTH_LONG).show();

                    }
                }
                else
                {
                    ScriptRecord script = new ScriptRecord(rowToUpdate, etScriptTitle.getText().toString(), etScriptContent.getText().toString());
                    //Проверяем запись на пустоту
                    if (isEmptyScriptRecord(script)) {
                        //Сообщаем пользователю о том, что он пытается сохранить пустую запись
                        Toast.makeText(ScriptEdit.this, v.getResources().getString(R.string.emptyRecordWarning), Toast.LENGTH_LONG).show();
                    }
                    //Обновляем непустую запись
                    if (!isEmptyScriptRecord(script)) {
                        myDB.updateScript(script);
                    }
                }

                //Очищаем поля
                rowToUpdate = -1;
                picturesLoad(rowToUpdate);
                etScriptTitle.setText("");
                etScriptContent.setText("");
            break;
        }
        }
    };

    private boolean isEmptyScriptRecord (@NotNull ScriptRecord sr) {
        boolean flag = false;
        if (sr.getTitle().isEmpty() && sr.getContent().isEmpty() && rowToUpdate < 0)
            flag = true;
        return flag;
    }

    //*********************Добавляется меню*******************************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.script_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean allowedToAddPicture = false;

        switch (item.getItemId()) {
            case R.id.add_photo:
                //Проверяем количество картинок в записи
                allowedToAddPicture = false;
                //Если rowToUpdate < 0 - значит запись новая и всё Ок
                if (rowToUpdate <= 0)
                    allowedToAddPicture = true;
                if (rowToUpdate > 0) {
                    int pictCount = myDB.picturesCounter(rowToUpdate);
                    if (pictCount < 3)
                        allowedToAddPicture = true;
                }

                //Проверяем наличие в устройстве камеры
                PackageManager pm = app_context.getPackageManager();
                final boolean deviceHasCameraFlag = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
                if (deviceHasCameraFlag && allowedToAddPicture) {
                    //Открываем Activity  с камерой
                    Intent cameraIntent = new Intent();

                    cameraIntent.setClass(getApplicationContext(), CameraView.class);

                    //Сохраняем в новый Intent все нужные значения
                    if (rowToUpdate > 0)
                        cameraIntent.putExtra(MyScriptDB.ROW_ID, rowToUpdate);

                    cameraIntent.putExtra(ScriptEdit.CONTENT_FIELD_CONTENT, etScriptContent.getText().toString());
                    cameraIntent.putExtra(ScriptEdit.TITLE_FIELD_CONTENT, etScriptTitle.getText().toString());

                    startActivity(cameraIntent);
                }
                else {
                    if (!deviceHasCameraFlag)
                        Toast.makeText(app_context, getResources().getString(R.string.deviceHasNoCameraError), Toast.LENGTH_LONG).show();
                    if (!allowedToAddPicture)
                        Toast.makeText(app_context, getResources().getString(R.string.tooMatchPictures), Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.add_picture:
                //Проверяем количество картинок в записи
                //Если rowToUpdate < 0 - значит запись новая и всё Ок
                if (rowToUpdate <= 0)
                    allowedToAddPicture = true;
                if (rowToUpdate > 0) {
                    int pictCount = myDB.picturesCounter(rowToUpdate);
                    if (pictCount < 3)
                        allowedToAddPicture = true;
                }

                if (allowedToAddPicture) {
                    //Зарускаем галлерею
                    Intent intGallery = new Intent();

                    intGallery.setType("image/*");
                    intGallery.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intGallery, "Select Picture"), RESULT_LOAD_IMAGE);
                }
                else {
                    Toast.makeText(app_context, getResources().getString(R.string.tooMatchPictures), Toast.LENGTH_LONG).show();
                }
                break;

            case android.R.id.home:
                finish();
        }

        return true;
    }

    //*********************Окончание меню***************************************

    public void picturesLoad (int rowId) {

        pictures_viewer fragment;

        // First get FragmentManager object.
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction;

        //Загрузить фотографии
        List<PictureRecord> recPictures = myDB.getOneScriptPictures(rowId);

        if (recPictures.size() > 0) {

            Bitmap myBitmap1, myBitmap2, myBitmap3, freshBitmap1, freshBitmap2, freshBitmap3;
            File imgFile;
            String pictPath;
            //Setting bitmap options to avoid missing of memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            freshBitmap1 = null;
            freshBitmap2 = null;
            freshBitmap3 = null;

            int orientation;
            ExifInterface exifObject;

            //Инициализируем массив для хранения идентификаторов фотографий
            addedPictIds = new int[3];

            int j = 0;

            //Get fragment object
            fragment =  (pictures_viewer)getSupportFragmentManager().findFragmentById(R.id.dynamic_fragment_frame_layout);
            fragmentTransaction = fragmentManager.beginTransaction();

            if (fragment != null) {
                fragment.showLayout();
                fragment.pict1.setVisibility(View.VISIBLE);
                fragment.pict2.setVisibility(View.VISIBLE);
                fragment.pict3.setVisibility(View.VISIBLE);
            }

            //Рассчитываем размеры картинок
            int pictWidth, pictHeight;
            int pictHeightInDp = 200;

            pictHeight = pictHeightInDp * (int)app_context.getResources().getDisplayMetrics().density;

            if (recPictures.size() == 1) {

                pictWidth = app_context.getResources().getDisplayMetrics().widthPixels;

            } else if (recPictures.size() == 2) {

                pictWidth = app_context.getResources().getDisplayMetrics().widthPixels/2 -
                        (int)app_context.getResources().getDisplayMetrics().density * paddingDp;

            } else if (recPictures.size() == 3) {

                pictWidth = (app_context.getResources().getDisplayMetrics().widthPixels -
                        4 * (int)app_context.getResources().getDisplayMetrics().density * paddingDp)/3;

            } else {
                pictWidth = pictHeight = 1;
            }

            for (int i = 0; i < recPictures.size(); i++) {

                pictPath = recPictures.get(i).getPicturePath() + "/" + recPictures.get(i).getPictureName();

                imgFile = new File(pictPath);

                if (imgFile.exists() && j < 3 && fragment != null) {

                    if (j == 0) {
                        myBitmap1 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        try {
                            exifObject = new ExifInterface(imgFile.getAbsolutePath());

                            orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                            freshBitmap1 = rotateBitmap(myBitmap1, orientation);

                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (freshBitmap1 != null) {
                            fragment.setPicture1(ThumbnailUtils.extractThumbnail(freshBitmap1, pictWidth, pictHeight,
                                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
                            if (myBitmap1 != null)
                                myBitmap1.recycle();
                        }
                        else {
                            if (myBitmap1 != null) {
                                fragment.setPicture1(ThumbnailUtils.extractThumbnail(myBitmap1, pictWidth, pictHeight,
                                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
                            }
                        }

                        fragment.pict1.setOnClickListener(openFullPicture);

                        addedPictIds[j] = recPictures.get(i).getRowId();
                    }
                    if (j == 1) {
                        myBitmap2 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                        try {
                            exifObject = new ExifInterface(imgFile.getAbsolutePath());
                            orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                            freshBitmap2 = rotateBitmap(myBitmap2, orientation);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (freshBitmap2 != null) {
                            fragment.setPicture2(ThumbnailUtils.extractThumbnail(freshBitmap2, pictWidth, pictHeight,
                                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
                        }
                        else {
                            if (myBitmap2 != null) fragment.setPicture2(ThumbnailUtils.extractThumbnail(myBitmap2, pictWidth, pictHeight,
                                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
                        }

                        fragment.pict2.setOnClickListener(openFullPicture);

                        addedPictIds[j] = recPictures.get(i).getRowId();
                    }
                    if (j == 2) {
                        myBitmap3 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                        try {
                            exifObject = new ExifInterface(imgFile.getAbsolutePath());
                            orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                            freshBitmap3 = rotateBitmap(myBitmap3, orientation);

                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (freshBitmap3 != null) {
                            fragment.setPicture3(ThumbnailUtils.extractThumbnail(freshBitmap3, pictWidth, pictHeight,
                                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
                        }
                        else {
                            if (myBitmap3 != null) fragment.setPicture3(ThumbnailUtils.extractThumbnail(myBitmap3, pictWidth, pictHeight,
                                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
                        }

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
                //Делаем границу между картинками
                fragment.pict1.setPadding(0, 0, 0, paddingPixel);
            }
            if (j == 2) {
                fragment.pict3.setVisibility(View.GONE);
                //Делаем границу между картинками
                float density = app_context.getResources().getDisplayMetrics().density;
                int paddingPixel = (int)(paddingDp * density);
                fragment.pict1.setPadding(0, 0, paddingPixel, paddingPixel);
                fragment.pict2.setPadding(paddingPixel, 0, 0, paddingPixel);
            }
            if (j == 3) {
                //Делаем границу между картинками
                float density = app_context.getResources().getDisplayMetrics().density;
                int paddingPixel = (int)(paddingDp * density);
                fragment.pict1.setPadding(0, 0, paddingPixel, paddingPixel);
                fragment.pict2.setPadding(paddingPixel/2, 0, paddingPixel/2, paddingPixel);
                fragment.pict3.setPadding(paddingPixel, 0, 0, paddingPixel);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (app_context == null)
            app_context = getApplicationContext();

        varSet = intent.getExtras();

        if (varSet != null) {

            if (varSet.containsKey(ScriptEdit.TITLE_FIELD_CONTENT))
                etScriptTitle.setText(varSet.getString(ScriptEdit.TITLE_FIELD_CONTENT));

            if (varSet.containsKey(ScriptEdit.CONTENT_FIELD_CONTENT))
                etScriptContent.setText(varSet.getString(ScriptEdit.CONTENT_FIELD_CONTENT));

            //Загружаем row_id, если он был передан
            if (varSet.containsKey(MyScriptDB.ROW_ID))
                rowToUpdate = varSet.getInt(MyScriptDB.ROW_ID);
        }

        //Загружаем фотографии
        picturesLoad(rowToUpdate);

        if (rowToUpdate > 0) {

            if (currActionBar != null)
                currActionBar.setTitle(R.string.headerEditTask);
        }
    }

    public final View.OnClickListener openFullPicture = new View.OnClickListener() {
        @Override
        public void onClick(@NotNull View v) {

            //Открываем Activity PictureDisplay для просмотра фотографии
            Intent PictureDisplayIntent = new Intent();

            PictureDisplayIntent.setClass(getApplicationContext(), PictureDisplay.class);
            //Сохраняем в новый Intent все нужные значения

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

                    ex.printStackTrace();
                }

                if (photoFile != null && data.getData() != null) {

                    //***********************************************

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

                        int pictureRecId = myDB.insertPictureRecord(rowToUpdate, photoFile.getParentFile().getAbsolutePath(), photoFile.getName());

                        if (pictureRecId == -1)
                            Toast.makeText(this, getResources().getString(R.string.photoNotAddedToDBError), Toast.LENGTH_LONG).show();

                        //Добавляем значение в набор varSet для корректной работы кнопки сохранения
                        if (varSet == null) {
                            varSet = new Bundle();
                            varSet.putInt(MyScriptDB.ROW_ID, rowToUpdate);
                        }
                        else if (!varSet.containsKey(MyScriptDB.ROW_ID)) {
                            varSet.putInt(MyScriptDB.ROW_ID, rowToUpdate);
                        }
                    }
                    //обновляем ActionBar и кнопку
                    if (rowToUpdate > 0) {

                        if (currActionBar != null)
                            currActionBar.setTitle(R.string.headerEditTask);
                    }

                }
                else
                    Toast.makeText(this, getResources().getString(R.string.fileNotCopied), Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, getResources().getString(R.string.fileNotCopied), Toast.LENGTH_LONG).show();
            }

        }

        //Загружаем фотографии
        picturesLoad(rowToUpdate);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        File storageDirectory = getApplicationContext().getDir("PICTURES", Context.MODE_PRIVATE);
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

    public static void copyStream(@NotNull InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    //обработчик ввода данных для того, чтобы заменять первые буквы на большие
    private final TextWatcher castFirstLetter = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0 && s.charAt(s.length() - 1) == 32) {
                int spaceCount = 0;
                for(int i=0; i < s.length(); i++) {
                    if(s.charAt(i) == 32)
                        spaceCount++;
                }

                String firstLetter = s.toString().substring(0, 1);

                if (spaceCount == 1 && !firstLetter.toUpperCase().equals(firstLetter)) {
                    String tempText = firstLetter.toUpperCase() + s.toString().substring(1);
                    s.replace(0, s.length(), tempText);
                }
            }

        }
    };

    //Сохраняем значение row_id и полей для случаев переворачивания устройства
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(MyScriptDB.ROW_ID, rowToUpdate);
        savedInstanceState.putString(ScriptEdit.TITLE_FIELD_CONTENT, etScriptTitle.getText().toString());
        savedInstanceState.putString(ScriptEdit.CONTENT_FIELD_CONTENT, etScriptContent.getText().toString());
    }

    //Функция для поворота картинки
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}