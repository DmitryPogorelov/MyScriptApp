package com.pdnsoftware.writtendone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PictureDisplay extends AppCompatActivity {

    //Context holder
    private Context app_context;

    //DataBase Manager exemplar
    private MyScriptDBManager myDB;

    private Bundle varSet; //объект для параметров вызывающей формы

    private int pictIdToShow = -1;

    private List<File> picturesToShow;
    private List<PictureRecord> pictRecs;
    private ViewPager vp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_display);

        //Считываем параметры вызова
        loadIntentParams();

        //Инициализируем базу для работы с ней
        myDB = new MyScriptDBManager(app_context);

        //Заводим ActionBar, чтобы на нем была стрелка назад
        //Variable for actionbar manipulation
        ActionBar currActionBar = getSupportActionBar();

        if (currActionBar != null) {
            currActionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (currActionBar != null && pictIdToShow > 0) {
            currActionBar.setTitle(myDB.getTaskNameByPictId(pictIdToShow));
        }

        picturesToShow = new ArrayList<>();
        pictRecs = myDB.loadPictFilesToShow(pictIdToShow);

        int selectedItem = 0;

        if (pictRecs != null && pictRecs.size() > 0) {

            File gallery = CameraView.createImageGallery(getApplicationContext());
            File tempFile;

            for (int i = 0; i < pictRecs.size(); i++) {

                if (pictRecs.get(i).getRowId() == pictIdToShow)
                    selectedItem = i;

                tempFile = new File(gallery, pictRecs.get(i).getPictureName());

                if (tempFile.exists())
                    picturesToShow.add(tempFile);
            }
        }

        //get an inflater to be used to create single pages
//        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Reference ViewPager defined in activity
        vp = findViewById(R.id.picture_view_pager);
        //set the adapter that will create the individual pages
        vp.setAdapter(new PictShowPagerAdapter(picturesToShow));

        vp.setCurrentItem(selectedItem);

    }

    //Выгрузка параметров, переданных в Activity
    private void loadIntentParams() {

        app_context = getApplicationContext();

        //Объект Intent для считывания данных из вызывающей формы
        Intent fromIntent = getIntent();
        varSet = fromIntent.getExtras();

        if (varSet != null && varSet.containsKey(ScriptEdit.PICTURE_ID)) {
            pictIdToShow = varSet.getInt(ScriptEdit.PICTURE_ID);  //Загружаем rowId, если он передан
        }

    }

 /*   private Bitmap pictureShow(int pictId) {

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

            pictPath = CameraView.createImageGallery(getApplicationContext()) + "/" + imageRecord.getPictureName();

            imgFile = new File(pictPath);

            if (imgFile.exists()) {

                pictToReturn = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                return pictToReturn;

            }
        }
        return pictToReturn;
    }*/

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
                        if (pictIdToShow > 0) {

                            File fileToDel = picturesToShow.get(vp.getCurrentItem());

                            for (int i  = 0; i < pictRecs.size(); i++) {
                                if (fileToDel.getName().equals(pictRecs.get(i).getPictureName())) {
                                    myDB.deletePictureRecordByPictId(pictRecs.get(i).getRowId());
                                    break;
                                }
                            }
                        }
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
        if (varSet != null && varSet.containsKey(ScriptEdit.PICTURE_ID))
            varSet.remove(ScriptEdit.PICTURE_ID);

        if (varSet != null)
            toScrEditActivityIntent.putExtras(varSet);

        startActivity(toScrEditActivityIntent);
        finish();
    }

    //Перехват нажатия кнопки Back
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backToScriptEditActivity();
    }
}