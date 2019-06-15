package com.pdnsoftware.writtendone;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PictViewAdapter extends RecyclerView.Adapter<PictViewAdapter.PictViewHolder> {

    private final List<File> data = new ArrayList<>();
    private final List<PictureRecord> incomingData;
    private final BitmapFactory.Options options = new BitmapFactory.Options();

    private final int pictHeight;
    private final int[] pictWidth = new int[3];
    private final Context currContext;

    //Константа для установки отступов между картинками
    private final static int paddingDp = 2;

    //Константа устанавливает высоту картинок
    private final static int pictHeightInDp = 200;

    // Provide a suitable constructor (depends on the kind of dataset)
    PictViewAdapter(List<PictureRecord> pictSet, Context con) {

        options.inSampleSize = 4;

        pictHeight = pictHeightInDp * (int)con.getResources().getDisplayMetrics().density;

        //Ширина картинки, когода она одна в строке
        pictWidth[0] = con.getResources().getDisplayMetrics().widthPixels;

        //Ширина картинки, когода их две в строке
        pictWidth[1] = con.getResources().getDisplayMetrics().widthPixels/2 -
                    2 * (int)con.getResources().getDisplayMetrics().density * paddingDp;

        //Ширина картинки, когода их три в строке
        pictWidth[2] = (con.getResources().getDisplayMetrics().widthPixels -
                    6 * (int)con.getResources().getDisplayMetrics().density * paddingDp)/3;

        currContext = con;

        incomingData = pictSet;

        data.clear();

        File tempFile;

        File pictFolder = CameraView.createImageGallery(currContext);

        if (pictFolder != null) {

            for (int i = 0; i < incomingData.size(); i++) {
                tempFile = new File(pictFolder.getAbsolutePath() + "/" + incomingData.get(i).getPictureName());
                data.add(tempFile);
            }
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public PictViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_pictures_viewer, parent, false);

        return new PictViewAdapter.PictViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull PictViewHolder holder, int position) {

        Bitmap myBitmap1;
        Bitmap myBitmap2;
        Bitmap myBitmap3;

        if (incomingData.size() != data.size()) {
            data.clear();

            File tempFile;

            File pictFolder = CameraView.createImageGallery(currContext);

            if (pictFolder != null) {

                for (int i = 0; i < incomingData.size(); i++) {
                    tempFile = new File(pictFolder.getAbsolutePath() + "/" + incomingData.get(i).getPictureName());
                    data.add(tempFile);
                }
            }
        }

        int rowIndex = position * 3;
        int pictInArrayIndex;
        //Проверяем, сколько фоток размещать в строке
        int pictsToLoad = data.size() - rowIndex;

        if (pictsToLoad > 3)
            pictsToLoad = 3;

        if (pictsToLoad >= 1) {
            pictInArrayIndex = rowIndex;
            try {
                android.support.media.ExifInterface exifObject =
                        new android.support.media.ExifInterface(data.get(pictInArrayIndex).getAbsolutePath());

                int orientation = exifObject.getAttributeInt(android.support.media.ExifInterface.TAG_ORIENTATION,
                        android.support.media.ExifInterface.ORIENTATION_UNDEFINED);

                myBitmap1 = ScriptEdit.rotateBitmap(BitmapFactory.decodeFile(data.get(pictInArrayIndex).getAbsolutePath(),
                        options), orientation);

            }
            catch (IOException e) {
                e.printStackTrace();
                myBitmap1 = BitmapFactory.decodeFile(data.get(pictInArrayIndex).getAbsolutePath(), options);
            }

            if (myBitmap1 != null) {

                holder.img1.setImageBitmap(ThumbnailUtils.extractThumbnail(myBitmap1, getPictWidth(pictsToLoad), pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
            }
            else {
                myBitmap1 = BitmapFactory.decodeResource(holder.img1.getContext().getResources(), R.drawable.camera, options);
                holder.img1.setImageBitmap(ThumbnailUtils.extractThumbnail(myBitmap1, getPictWidth(pictsToLoad), pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
            }
            //присваиваем листенер
            holder.img1.setOnClickListener(new AwesomeButtonClick(incomingData.get(pictInArrayIndex).getRowId(), currContext));
        }

        if (pictsToLoad >= 2) {
            pictInArrayIndex = rowIndex + 1;
            try {
                android.support.media.ExifInterface exifObject =
                        new android.support.media.ExifInterface(data.get(pictInArrayIndex).getAbsolutePath());

                int orientation = exifObject.getAttributeInt(android.support.media.ExifInterface.TAG_ORIENTATION,
                        android.support.media.ExifInterface.ORIENTATION_UNDEFINED);

                myBitmap2 = ScriptEdit.rotateBitmap(BitmapFactory.decodeFile(data.get(pictInArrayIndex).getAbsolutePath(),
                        options), orientation);

            }
            catch (IOException e) {
                e.printStackTrace();
                myBitmap2 = BitmapFactory.decodeFile(data.get(pictInArrayIndex).getAbsolutePath(), options);
            }

            if (myBitmap2 != null) {

                holder.img2.setImageBitmap(ThumbnailUtils.extractThumbnail(myBitmap2, getPictWidth(pictsToLoad), pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
            }
            else {
                myBitmap2 = BitmapFactory.decodeResource(holder.img2.getContext().getResources(), R.drawable.camera, options);
                holder.img2.setImageBitmap(ThumbnailUtils.extractThumbnail(myBitmap2, getPictWidth(pictsToLoad), pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
            }
            //присваиваем листенер
            holder.img2.setOnClickListener(new AwesomeButtonClick(incomingData.get(pictInArrayIndex).getRowId(), currContext));
        }

        if (pictsToLoad >= 3) {
            pictInArrayIndex = rowIndex + 2;
            try {
                android.support.media.ExifInterface exifObject =
                        new android.support.media.ExifInterface(data.get(pictInArrayIndex).getAbsolutePath());

                int orientation = exifObject.getAttributeInt(android.support.media.ExifInterface.TAG_ORIENTATION,
                        android.support.media.ExifInterface.ORIENTATION_UNDEFINED);

                myBitmap3 = ScriptEdit.rotateBitmap(BitmapFactory.decodeFile(data.get(pictInArrayIndex).getAbsolutePath(),
                        options), orientation);

            }
            catch (IOException e) {
                e.printStackTrace();
                myBitmap3 = BitmapFactory.decodeFile(data.get(pictInArrayIndex).getAbsolutePath(), options);
            }

            if (myBitmap3 != null) {

                holder.img3.setImageBitmap(ThumbnailUtils.extractThumbnail(myBitmap3, getPictWidth(pictsToLoad), pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
            }
            else {
                myBitmap3 = BitmapFactory.decodeResource(holder.img3.getContext().getResources(), R.drawable.camera, options);
                holder.img3.setImageBitmap(ThumbnailUtils.extractThumbnail(myBitmap3, getPictWidth(pictsToLoad), pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
            }
            //присваиваем листенер
            holder.img3.setOnClickListener(new AwesomeButtonClick(incomingData.get(pictInArrayIndex).getRowId(), currContext));
        }
        //оформление
        float density = currContext.getResources().getDisplayMetrics().density;
        int paddingPixel = (int)(paddingDp * density);
        //Скрываем ненужные объекты
        if (pictsToLoad == 1) {
            holder.img1.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.GONE);
            holder.img3.setVisibility(View.GONE);
            //Делаем границу между картинками
            holder.img1.setPadding(0, 0, 0, 3*paddingPixel);
        }
        if (pictsToLoad == 2) {
            holder.img1.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.VISIBLE);
            holder.img3.setVisibility(View.GONE);
            //Делаем границу между картинками
            holder.img1.setPadding(0, 0, paddingPixel, 3*paddingPixel);
            holder.img2.setPadding(paddingPixel, 0, 0, 3*paddingPixel);
        }
        if (pictsToLoad == 3) {
            holder.img1.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.VISIBLE);
            holder.img3.setVisibility(View.VISIBLE);
            //Делаем границу между картинками
            holder.img1.setPadding(0, 0, 2*paddingPixel, 3*paddingPixel);
            holder.img2.setPadding(paddingPixel, 0, paddingPixel, 3*paddingPixel);
            holder.img3.setPadding(2*paddingPixel, 0, 0, 3*paddingPixel);
        }
    }

    @Override
    public int getItemCount() {

        int rowsCount = incomingData.size() / 3;

        if ((incomingData.size() % 3) > 0)
            rowsCount++;

        return rowsCount;
    }

    class PictViewHolder extends RecyclerView.ViewHolder {
        final ImageView img1, img2, img3;

        PictViewHolder(View v) {
            super(v);

            img1 = v.findViewById(R.id.pict1);
            img2 = v.findViewById(R.id.pict2);
            img3 = v.findViewById(R.id.pict3);
        }
    }

    //Функция для определения ширины картинки
    private int getPictWidth(int pictCount) {
        if (pictCount == 1)
            return pictWidth[0];
        else if (pictCount == 2)
            return pictWidth[1];
        else
            return pictWidth[2];
    }

    class AwesomeButtonClick implements View.OnClickListener {

        final int pictId;
        final Context innerContext;

        AwesomeButtonClick(int pictureId, Context context) {
            this.pictId = pictureId;
            this.innerContext = context;
        }

        @Override
        public void onClick(View v) {
            //Открываем Activity PictureDisplay для просмотра фотографии
            Intent PictureDisplayIntent = new Intent();
            PictureDisplayIntent.setClass(innerContext, PictureDisplay.class);
            //Сохраняем в новый Intent все нужные значения
            PictureDisplayIntent.putExtra(ScriptEdit.PICTURE_ID, pictId);
            currContext.startActivity(PictureDisplayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
