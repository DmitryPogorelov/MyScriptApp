package com.example.myscript;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ScriptEdit extends AppCompatActivity {

    public EditText etScriptTitle, etScriptContent;
    public Button saveButton, saveAndAddButton;
    private MyScriptDBManager myDB;
    private Context app_context;

    private ActionMode actionMode; //Переменная для вызова меню

    int rowToUpdate; //Идентификатор строки, которую будем обновлять

    Intent currIntent = new Intent(); //Объект Intent для считывания данных из вызывающей формы
    Bundle varSet; //объект для параметров вызывающей формы

    //Константы для обозначения полей при передаче между формами
    public static final String TITLE_FIELD_CONTENT = "title_field_content";
    public static final String CONTENT_FIELD_CONTENT = "content_field_content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_edit);

        //Сохраняем контекст в переменную
        app_context = getApplicationContext();

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

        //Создаю массив входящих параметров формы
        currIntent = getIntent();
        varSet = currIntent.getExtras();
        //Проверяем, есть ли входящий параметр с id записи для редактирования
        //Если есть, то считываем данные зи БД и заполняем поля
        if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID)) {

            rowToUpdate = varSet.getInt(MyScriptDB.ROW_ID);

            ScriptRecord recToEdit = myDB.getOneScript(rowToUpdate);

            etScriptTitle.setText(recToEdit.getTitle());
            etScriptContent.setText(recToEdit.getContent());

            try {
                currActionBar.setTitle(R.string.headerEditTask);
            }
            catch (NullPointerException e) {

            }

            saveAndAddButton.setVisibility(View.INVISIBLE);

        }
        else
        {
            try {
                currActionBar.setTitle(R.string.headerNewTask);
            }
            catch (NullPointerException e) {

            }
        }

        //Проверяем, есть ли путь к файлу
        if (currIntent.hasExtra(CameraView.PICTURE_NAME)) {
            Toast.makeText(app_context, "Пришла картинка: " + varSet.getString(CameraView.PICTURE_NAME), Toast.LENGTH_LONG).show();
        }

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
            actionMode = null;
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

                break;
        }

        return true;
    }

    //*********************Окончание меню***************************************
}
