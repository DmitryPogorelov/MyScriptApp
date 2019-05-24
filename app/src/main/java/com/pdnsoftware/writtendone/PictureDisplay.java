package com.pdnsoftware.writtendone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import java.io.File;

public class PictureDisplay extends AppCompatActivity {

    //Variable for actionbar manipulation
    ActionBar currActionBar;

    //Context holder
    private Context app_context;

    //DataBase Manager exeplar
    private MyScriptDBManager myDB;

    Intent fromIntent = new Intent(); //Объект Intent для считывания данных из вызывающей формы
    Bundle varSet; //объект для параметров вызывающей формы

    int pictIdToShow = -1;

    //Идентификатор Activity
    public static final String NAME_INTENT_PICTUREDISPLAY = "intent_PictureDisplay";

    ImageView bigPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_display);

        //Считываем параметры вызова
        loadIntentParams();

        //Инициализируем базу для работы с ней
        myDB = new MyScriptDBManager(app_context);

        //Заводим ActionBar, чтобы на нем была стрелка назад
        currActionBar = getSupportActionBar();

        if (currActionBar != null) {
            currActionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (currActionBar != null && pictIdToShow > 0) {
            currActionBar.setTitle(myDB.getTaskNameByPictId(pictIdToShow));
        }
        //Connect interface objects to variables
        bigPicture = findViewById(R.id.showPicture);

        //Get image from Database/file system and put it into ImageView
        bigPicture.setImageBitmap(pictureShow(pictIdToShow));
    }

    //Выгрузка параметров, переданных в Activity
    private void loadIntentParams() {

        app_context = getApplicationContext();

        fromIntent = getIntent();
        varSet = fromIntent.getExtras();

        if (varSet != null && varSet.containsKey(ScriptEdit.PICTIRE_ID)) {
            pictIdToShow = varSet.getInt(ScriptEdit.PICTIRE_ID);  //Загружаем rowId, если он передан
        }

    }

    private Bitmap pictureShow(int pictId) {

        Bitmap pictToReturn;
        pictToReturn = BitmapFactory.decodeResource(getResources(), R.drawable.camera);

        File imgFile;
        String pictPath;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        PictureRecord imageRecord = new PictureRecord(-1, -1, "", "", "");

        if (pictId > 0) {

            imageRecord = myDB.getOnePicture(pictId);

        }

        if (imageRecord.getRowId() > 0) {

            pictPath = imageRecord.getPicturePath() + "/" + imageRecord.getPictureName();

            imgFile = new File(pictPath);

            if (imgFile.exists()) {

                pictToReturn = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
            }

        }

        return pictToReturn;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.picture_display_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Обработка нажатия на кнопку удаления
            case R.id.delete_photo:
                AlertDialog.Builder sureToDelDialog = new AlertDialog.Builder(this);
                sureToDelDialog.setMessage(getResources().getString(R.string.sureToDeletePhoto));

                sureToDelDialog.setPositiveButton(getResources().getString(R.string.sureToDeletePhotoYes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (pictIdToShow > 0)
                            myDB.deletePictureRecordByPictId(pictIdToShow);
                        backToScriptEditActivity();
                    }
                });
                sureToDelDialog.setNegativeButton(getResources().getString(R.string.sureToDeletePhotoNo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                sureToDelDialog.setCancelable(false);
                sureToDelDialog.create();
                sureToDelDialog.show();

                break;

                //Обработчик нажатия на кнопку Back
            case android.R.id.home:
                backToScriptEditActivity();
                break;
        }

        return true;
    }

    //Создаем новый Intent и возвращаемся в ScriptEdit Activity
    private void backToScriptEditActivity() {

        Intent toScrEditActivityIntent = new Intent();
        toScrEditActivityIntent.setClass(app_context, ScriptEdit.class);

        //Удаляем из набора ID картинки
        if (varSet != null && varSet.containsKey(ScriptEdit.PICTIRE_ID))
            varSet.remove(ScriptEdit.PICTIRE_ID);
        //Изменяем в наборе ниаменование вызывающей активности
        if (varSet != null && varSet.containsKey(MainActivity.CALLER_ACTIVITY_NAME)) {
            varSet.remove(MainActivity.CALLER_ACTIVITY_NAME);
            varSet.putString(MainActivity.CALLER_ACTIVITY_NAME, PictureDisplay.NAME_INTENT_PICTUREDISPLAY);
        }

        if (varSet != null)
            toScrEditActivityIntent.putExtras(varSet);

        startActivity(toScrEditActivityIntent);
        finish();
    }

    //Перехват нажатия кнопки Back
    @Override
    public void onBackPressed() {
        backToScriptEditActivity();
    }
}
