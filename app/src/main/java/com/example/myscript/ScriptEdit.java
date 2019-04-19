package com.example.myscript;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ScriptEdit extends AppCompatActivity {

    public EditText etScriptTitle, etScriptContent;
    public Button saveButton, saveAndAddButton;
    private MyScriptDBManager myDB;

    int rowToUpdate; //Идентификатор строки, которую будем обновлять

    Intent currIntent = new Intent(); //Объект Intent для считывания данных из вызывающей формы

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_edit);
        //Инициализируем базу для работы с ней
        myDB = new MyScriptDBManager(getApplicationContext());

        //Связываем переменные с элементами управления
        etScriptTitle = (EditText)findViewById(R.id.et_script_title);
        etScriptContent = (EditText)findViewById(R.id.et_script_content);

        saveButton = (Button)findViewById(R.id.saveButton);
        saveAndAddButton = (Button)findViewById(R.id.saveAndAddButton);

        saveButton.setOnClickListener(seSaver);
        saveAndAddButton.setOnClickListener(seSaver);

        //Заводим ActionBar, чтобы на нем была стрелка назад
        ActionBar currActionBar = getSupportActionBar();

        //Проверяем, есть ли входящий параметр с id записи для редактирования
        //Если есть, то считываем данные зи БД и заполняем поля
        currIntent = getIntent();
        if (currIntent.hasExtra(MyScriptDB.ROW_ID)) {
            Bundle varSet = currIntent.getExtras();
            rowToUpdate = varSet.getInt(MyScriptDB.ROW_ID);

            ScriptRecord recToEdit = myDB.getOneScript(rowToUpdate);

            etScriptTitle.setText(recToEdit.getTitle());
            etScriptContent.setText(recToEdit.getContent());

            currActionBar.setTitle(R.string.headerEditTask);

            saveAndAddButton.setVisibility(View.INVISIBLE);
        }
        else
        {
            currActionBar.setTitle(R.string.headerNewTask);
        }
    }

    private View.OnClickListener seSaver = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.saveButton:

                    if (currIntent.hasExtra(MyScriptDB.ROW_ID)) {
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

                    if (!currIntent.hasExtra(MyScriptDB.ROW_ID)) {
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
}
