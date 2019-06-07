package com.pdnsoftware.writtendone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import java.util.ArrayList;
import java.util.List;

public class ScriptEdit extends AppCompatActivity {

    private EditText etScriptTitle, etScriptContent;
    private MyScriptDBManager myDB;
    private Context app_context;

    private int rowToUpdate = -1; //Идентификатор строки, которую будем обновлять

    private Bundle varSet; //объект для параметров вызывающей формы

    //Константы для обозначения полей при передаче между формами
    public static final String TITLE_FIELD_CONTENT = "title_field_content";
    public static final String CONTENT_FIELD_CONTENT = "content_field_content";

    //Константа для передачи идентификатора фоторгафии, которую нужно открывать, в форму PictureDisplay
    public static final String PICTURE_ID = "pict_id";

    //Константа с результатом возврата картинки из галлереи
    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 3;

    private MenuItem addPhotoMenuItem, addPictMenuItem;

    //Actionbar
    private ActionBar currActionBar;

    //Переменная для адаптера
    private PictViewAdapter curr_adapter;
    private List<PictureRecord> pictsArray = new ArrayList<>();

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
        //Кнопка "Сохранить и добавить"
        Button saveAndAddButton = findViewById(R.id.saveAndAddButton);

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

        //Всё для RecyclerView
        RecyclerView pictRecyclerView = findViewById(R.id.recViewForPicts);

        // use a grid layout manager
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 1,
                    GridLayoutManager.VERTICAL, false);

        pictRecyclerView.setLayoutManager(layoutManager);

        pictsArray = getPicturesToShow();

        // specify an adapter (see also next example)
        curr_adapter = new PictViewAdapter(pictsArray, getApplicationContext());

        //Наводим красоту
        pictRecyclerView.setAdapter(curr_adapter);

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
                        backToMainActivity();
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
                            backToMainActivity();
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

        addPhotoMenuItem = menu.findItem(R.id.add_photo);
        addPictMenuItem = menu.findItem(R.id.add_picture);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Разрешения
        boolean permissionCameraGranted;
        boolean permissionStorageAccessGranted;

        switch (item.getItemId()) {
            case R.id.add_photo:
                //Проверяем наличие разрешения на доступ к камере
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    permissionCameraGranted = false;

                    // Permission is not granted
                    // Should we show an explanation?

/*                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                    } else { */
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
 //                   }
                }
                else {
                    permissionCameraGranted = true;
                }

                //Проверяем наличие в устройстве камеры
                PackageManager pm = app_context.getPackageManager();
                final boolean deviceHasCameraFlag = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
                if (deviceHasCameraFlag && permissionCameraGranted) {
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
                }

                break;
            case R.id.add_picture:
                //Проверяем дано ли разрешение на загрузку картинок из внешнего хранилища
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    permissionStorageAccessGranted = false;

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
                }
                else {
                    permissionStorageAccessGranted = true;
                }

                if (permissionStorageAccessGranted) {
                    //Зарускаем галлерею
                    Intent intGallery = new Intent();

                    intGallery.setType("image/*");
                    intGallery.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intGallery, "Select Picture"), RESULT_LOAD_IMAGE);
                }

                break;

            case android.R.id.home:
                backToMainActivity();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //*********************Окончание меню***************************************

    //Выгрузка параметров, переданных в Activity
    private void loadIntentParams() {

        app_context = getApplicationContext();

        //Объект Intent для считывания данных из вызывающей формы
        Intent currIntent = getIntent();
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

        if (rowToUpdate > 0) {

            if (currActionBar != null)
                currActionBar.setTitle(R.string.headerEditTask);

            //Обновляем картинки в адаптере
            pictsArray.clear();
            pictsArray.addAll(getPicturesToShow());
            curr_adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {

            try {

                File photoFileFolder;
                File photoFile = null;

                try {
                    photoFileFolder = CameraView.createImageGallery(getApplicationContext());
                    if (photoFileFolder == null)
                        throw new IOException("Pictures directory was not found!");
                    photoFile = CameraView.createImageFile(photoFileFolder);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.errorPicturesDirCreated),
                            Toast.LENGTH_SHORT).show();
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
                    if (rowToUpdate == -1) {
                        ScriptRecord newRec = new ScriptRecord(MyScriptDB.EMPTY_ROW_ID,
                                etScriptTitle.getText().toString(), etScriptContent.getText().toString());
                        rowToUpdate = myDB.insertScript(newRec);

                        if (varSet == null) {
                            varSet = new Bundle();
                            varSet.putInt(MyScriptDB.ROW_ID, rowToUpdate);
                        }
                        else if (!varSet.containsKey(MyScriptDB.ROW_ID)) {
                            varSet.putInt(MyScriptDB.ROW_ID, rowToUpdate);
                        }
                    }

                    if (rowToUpdate > 0) {
                        //Добавляем картинку к задаче
                        int pictureRecId = myDB.insertPictureRecord(rowToUpdate, photoFile.getParentFile().getAbsolutePath(), photoFile.getName());

                        if (pictureRecId == -1)
                            Toast.makeText(this, getResources().getString(R.string.photoNotAddedToDBError), Toast.LENGTH_LONG).show();
                        else {
                            //Делаем thumbnail
                            File thumbnail = CameraView.saveThumbnail(photoFile, getApplicationContext());

                            if (thumbnail != null)
                                myDB.addThumbnailName(pictureRecId, thumbnail);
                            else
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.errorThumbnailsDirCreated),
                                        Toast.LENGTH_SHORT).show();
                        }
                        //обновляем ActionBar и кнопку
                        if (currActionBar != null)
                            currActionBar.setTitle(R.string.headerEditTask);

                        //Обновляем картинки в адаптере
                        pictsArray.clear();
                        pictsArray.addAll(getPicturesToShow());
                        curr_adapter.notifyDataSetChanged();
                    }
                }
                else
                    Toast.makeText(this, getResources().getString(R.string.fileNotCopied), Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, getResources().getString(R.string.fileNotCopied), Toast.LENGTH_LONG).show();
            }

        }

    }

    private static void copyStream(@NotNull InputStream input, OutputStream output) throws IOException {
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
            case android.support.media.ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case android.support.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case android.support.media.ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case android.support.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case android.support.media.ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case android.support.media.ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case android.support.media.ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case android.support.media.ExifInterface.ORIENTATION_ROTATE_270:
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

    //Создаем новый Intent и возвращаемся в ScriptEdit Activity
    private void backToMainActivity() {

        Intent toMainActivityIntent = new Intent();
        toMainActivityIntent.setClass(app_context, MainActivity.class);
        startActivity(toMainActivityIntent);
        finish();
    }

    //Перехват нажатия кнопки Back
    @Override
    public void onBackPressed() {
         super.onBackPressed();
        backToMainActivity();
    }

    //Проверяем, предоставил ли пользователь разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    onOptionsItemSelected(addPhotoMenuItem);
                } else {
                    Toast.makeText(app_context, getResources().getString(R.string.noCameraAccessGranted),
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    onOptionsItemSelected(addPictMenuItem);
                }
                else {
                    Toast.makeText(app_context, getResources().getString(R.string.noExternalStorageAccessGranted),
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    //Функция готовит массив файлов картинок для отображения
    private List<PictureRecord> getPicturesToShow() {

        if (rowToUpdate <= 0)
            return new ArrayList<>();

        //Извлекаем данные из БД
        List<PictureRecord> testData;
        List<PictureRecord> verifiedData = new ArrayList<>();
        File tempFile;

        testData = myDB.getOneScriptPictures(rowToUpdate);

        File pictFolder = CameraView.createImageGallery(getApplicationContext());

        if (pictFolder != null) {

            for (int i = 0; i < testData.size(); i++) {
                tempFile = new File(pictFolder.getAbsolutePath() + "/" + testData.get(i).getPictureName());
                if (tempFile.exists())
                    verifiedData.add(testData.get(i));
            }
        }
        return verifiedData;
    }
}