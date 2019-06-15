package com.pdnsoftware.writtendone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class PictureDisplay extends AppCompatActivity {

    //Context holder
    private Context app_context;

    //DataBase Manager exemplar
    private MyScriptDBManager myDB;

    private Bundle varSet; //объект для параметров вызывающей формы

    private ActionBar currActionBar; //Ссылка на ActionBar в Activity

    private int pictIdToShow = -1; //Идентификатор картинки, которую надо показывать

    private List<File> picturesToShow;
    private List<PictureRecord> pictRecs;
    //private ViewPager vp;
    private FreezeViewPager vp;

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
        currActionBar = getSupportActionBar();

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

        PictShowPagerAdapter currAdapter = new PictShowPagerAdapter(picturesToShow);

        vp.setAdapter(currAdapter);

        DepthTransformation depthTransformation = new DepthTransformation();

        vp.setPageTransformer(true, depthTransformation);


        vp.addOnPageChangeListener(pictSlide);

        vp.setCurrentItem(selectedItem);

        PictShowPagerAdapter.MyOnTouchListener.vpScrollStop = vp;

        if (currActionBar != null)
            currActionBar.setTitle(Integer.toString(selectedItem + 1).concat(getResources().getString(R.string.pictFromPict)).concat(Integer.toString(picturesToShow.size())));
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
                                    new DeletePictureTask(myDB).execute(pictRecs.get(i).getRowId());
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

    //Создаем Listener для смены надписи на ActionBare
    private final ViewPager.OnPageChangeListener pictSlide = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {}

        @Override
        public void onPageSelected(int i) {
            if (currActionBar != null)
                currActionBar.setTitle(Integer.toString(i + 1).concat(getResources().getString(R.string.pictFromPict)).concat(Integer.toString(picturesToShow.size())));

            if (vp != null) {

                //Запускаем асинхронную загрузку картинок


                //Сохраняем параметры предыдущих экранов
                if (vp.getChildCount() > 0) {

                    View v = vp.findViewWithTag(vp.getCurrentItem());

                    if (v != null) {
                        ImageView iV = v.findViewById(R.id.showPicture);
                        iV.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }

                    //Восстанавливаем значения меньшего
                    if (vp.findViewWithTag(vp.getCurrentItem() - 1) != null) {
                        View prevView = vp.findViewWithTag(vp.getCurrentItem() - 1);
                        ImageView prevIV = prevView.findViewById(R.id.showPicture);
                        prevIV.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    }

                    //Восстанавливаем значения старшего
                    if (vp.findViewWithTag(vp.getCurrentItem() + 1) != null) {
                        View nextView = vp.findViewWithTag(vp.getCurrentItem() + 1);
                        ImageView nextIV = nextView.findViewById(R.id.showPicture);
                        nextIV.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    }
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    };


    //Асинхронная задача для удаления фотографии
    private static class DeletePictureTask extends AsyncTask<Integer, Void, Boolean> {

        MyScriptDBManager innerDBManager;

        DeletePictureTask(MyScriptDBManager dbManager) {
            this.innerDBManager = dbManager;
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            int pictIdToDel = integers[0];

            try { innerDBManager.deletePictureRecordByPictId(pictIdToDel); }
            catch (SQLiteException se) { return false; }
            return true;
        }
    }

    //Трансформация картинок в ViewPagerе

    public class DepthTransformation implements ViewPager.PageTransformer{
        @Override
        public void transformPage(@NonNull View page, float position) {

            if (position < -1){    // [-Infinity,-1)
                // This page is way off-screen to the left.
                page.setAlpha(0);

            }
            else if (position <= 0){    // [-1,0]
                page.setAlpha(1);
                page.setTranslationX(0);
                page.setScaleX(1);
                page.setScaleY(1);

            }
            else if (position <= 1){    // (0,1]
                page.setTranslationX(-position*page.getWidth());
                page.setAlpha(1-Math.abs(position));
                page.setScaleX(1-Math.abs(position));
                page.setScaleY(1-Math.abs(position));

            }
            else {    // (1,+Infinity]
                // This page is way off-screen to the right.
                page.setAlpha(0);

            }
        }
    }
}