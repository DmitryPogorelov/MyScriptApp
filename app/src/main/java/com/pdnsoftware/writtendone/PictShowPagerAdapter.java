package com.pdnsoftware.writtendone;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

class PictShowPagerAdapter extends PagerAdapter {

    private final List<File> pictList;

    PictShowPagerAdapter (List<File> pictToShow) {

        pictList = pictToShow;
    }

    @Override
    public int getCount() {
        //Return total pages, here one for each data item
        return pictList.size();
    }
    //Create the given page (indicated by position)
    @NotNull
    @Override
    public Object instantiateItem(@NotNull ViewGroup container, int position) {

        View page = LayoutInflater.from(container.getContext()).inflate(R.layout.pict_view_pager, (LinearLayout)container.findViewById(R.id.view_pager_root));

        page.setTag(position);

        ImageView iView = page.findViewById(R.id.showPicture);

        new AsynkPhotoLoader(iView).execute(pictList.get(position));

        iView.setOnTouchListener(new MyOnTouchListener());
        iView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        //Add the page to the front of the queue
        container.addView(page);
        return page;
    }
    @Override
    public boolean isViewFromObject(@NotNull View arg0, @NotNull Object arg1) {
        //See if object from instantiateItem is related to the given view
        //required by API
        return arg0==(arg1);
    }
    @Override
    public void destroyItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
        container.removeView((View) object);
        object=null;
    }

    //Класс для асинхронной загрузки фотографий
    private static class AsynkPhotoLoader extends AsyncTask<File, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;

        AsynkPhotoLoader(ImageView imageView) {
            this.imageViewReference = new WeakReference<>(imageView);
        }

        protected Bitmap doInBackground(File... pictToLoad) {
            File fileToLoad = pictToLoad[0];

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;

            Bitmap pictToReturn;

            //****************Поворачиваем фото, если надо****************************
            try {
                android.support.media.ExifInterface exifObject =
                        new android.support.media.ExifInterface(fileToLoad.getAbsolutePath());

                int orientation = exifObject.getAttributeInt(android.support.media.ExifInterface.TAG_ORIENTATION,
                        android.support.media.ExifInterface.ORIENTATION_UNDEFINED);

                pictToReturn = ScriptEdit.rotateBitmap(BitmapFactory.decodeFile(fileToLoad.getAbsolutePath(),
                        options), orientation);

            }
            catch (IOException e) {
                e.printStackTrace();
                pictToReturn = BitmapFactory.decodeFile(fileToLoad.getAbsolutePath(), options);
            }

            return pictToReturn;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (isCancelled()) {
                bitmap = null;
            }

            ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
                else {
                    Drawable placeholder = imageView.getContext().getResources().getDrawable(R.drawable.camera, null);
                    imageView.setImageDrawable(placeholder);
                }
            }
        }
    }

    public static class MyOnTouchListener implements View.OnTouchListener {

        static final int NONE = 0;
        static final int DRAG = 1;
        static final int ZOOM = 2;

        static final float ZOOM_MIN = 1;
        static final float ZOOM_MAX = 4;

        int mode = NONE;

        PointF start = new PointF();
        PointF secondFingerDown = new PointF();
        PointF secondFingerMove = new PointF();
        PointF midPoint = new PointF();

        Matrix currMatrix = new Matrix();
        Matrix matrix = new Matrix();
        float[] currCoords = {0, 0, 0, 0, 0, 0, 0, 0, 0};

        float scaleX = 1;
        float scaleY = 1;

        float zoomScale = 1;
        double deltaDown = 1;
        double deltaMove = 1;

        float xToMove, yToMove;
        float limitX = 0, limitY = 0;
        float imageWidth = 0, imageHeight = 0;

        //Интерфейс для управления прокруткой ViewPagera
        static StopVPScrolling vpScrollStop;

        MyOnTouchListener() {}

        public interface StopVPScrolling {
            void setPagingEnabled(boolean enabled);
        }

        @Override
        public boolean onTouch(@NotNull View v, @NotNull MotionEvent event) {

            v.performClick();
            //Zoom

            ImageView iView = (ImageView) v;
            iView.setScaleType(ImageView.ScaleType.MATRIX);

            limitX = iView.getMeasuredWidth();
            limitY = iView.getMeasuredHeight();

            imageWidth = iView.getDrawable().getIntrinsicWidth();
            imageHeight = iView.getDrawable().getIntrinsicHeight();

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mode = DRAG;
                    start.set(event.getX(0), event.getY(0));
                    currMatrix = iView.getImageMatrix();
                    currMatrix.getValues(currCoords);

                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ZOOM;
                    secondFingerDown.x = event.getX(0) - event.getX(1);
                    secondFingerDown.y = event.getY(0) - event.getY(1);

                    midPoint.x = (event.getX(0) + event.getX(1)) / 2;
                    midPoint.y = (event.getY(0) + event.getY(1)) / 2;

                    break;
                case MotionEvent.ACTION_MOVE:

                    xToMove = currCoords[2] + event.getX(0) - start.x;
                    yToMove = currCoords[5] + event.getY(0) - start.y;
                    scaleX = currCoords[0];
                    scaleY = currCoords[4];

                    if (mode == DRAG) {

                        if (yToMove > 0 && imageHeight * scaleY >= limitY) { yToMove = 0; }
                        if ((yToMove + imageHeight * scaleY) < limitY && imageHeight * scaleY >= limitY) { yToMove = limitY - imageHeight * scaleY; }


                        if (xToMove > 0 && imageWidth * scaleX >= limitX) { xToMove = 0; }
                        if ((xToMove + imageWidth * scaleX) < limitX && imageWidth * scaleX >= limitX) { xToMove = limitX - imageWidth * scaleX; }


                        if ( imageWidth * scaleX <= limitX ) { xToMove = (int)((limitX - imageWidth * scaleX) / 2); }
                        //if ( imageWidth * scaleX > limitX && xToMove > 0 ) { xToMove = 0; }

                        if ( imageHeight * scaleY <= limitY ) { yToMove = (int)((limitY - imageHeight * scaleY) / 2); }
                        //if ( imageHeight * scaleY > limitY && yToMove > 0) { yToMove = 0; }

                        //if ((xToMove + imageWidth * scaleX) < limitX) { xToMove = (int)(limitX - imageWidth * scaleX) / 2; }
                        //if ((yToMove + imageHeight * scaleY) < limitY) { yToMove = (int)(limitY - imageHeight * scaleY) / 2; }

                        Matrix vvv = new Matrix();
                        vvv.setTranslate(xToMove, yToMove);
                        vvv.preScale(scaleX, scaleY);

                        iView.setImageMatrix(vvv);

                        //Даём команду ViewPager
                        if (xToMove > -5 || (( xToMove + scaleX * imageWidth - limitX) < 5 ) ) {
                            vpScrollStop.setPagingEnabled(true);
                        }
                        else
                            vpScrollStop.setPagingEnabled(false);

                    }
                    else if (mode == ZOOM) {
                        secondFingerMove.x = event.getX(0) - event.getX(1);
                        secondFingerMove.y = event.getY(0) - event.getY(1);

                        deltaDown = Math.sqrt(secondFingerDown.x * secondFingerDown.x + secondFingerDown.y * secondFingerDown.y);
                        deltaMove = Math.sqrt(secondFingerMove.x * secondFingerMove.x + secondFingerMove.y * secondFingerMove.y);

                        if (deltaDown != 0 && deltaMove > 5f) {
                            zoomScale = (float) (deltaMove / deltaDown);

                           //Снижаем скорость масштабирований
                            if (zoomScale > 1) {
                                float greater1 = (zoomScale - 1) / 4;
                                zoomScale = 1 + greater1;
                            }
                            if (zoomScale < 1) {
                                float lessThan1 = (1 - zoomScale) / 4;
                                zoomScale = 1 - lessThan1;
                            }

                            int innerImageWidth = iView.getDrawable().getIntrinsicWidth();
                            int innerImageHeight = iView.getDrawable().getIntrinsicHeight();

                            Matrix scaleMatrix = new Matrix();
                            scaleMatrix.set(iView.getImageMatrix());

                            float[] click = {0, 0, 0, 0, 0, 0, 0, 0, 0};

                            scaleMatrix.getValues(click);

                            float beginX = click[2];
                            float beginY = click[5];
                            float beginScaleX = click[0];
                            float beginScaleY = click[4];

                            //Вычисляем финальный коэффициент масштабирования
                            if (beginScaleX * zoomScale * innerImageWidth >= ZOOM_MAX * limitX) {
                                zoomScale = (ZOOM_MAX  * limitX)/ (beginScaleX * innerImageWidth);
                            }
                            if (beginScaleY * zoomScale * innerImageHeight >= ZOOM_MAX * limitY) {
                                zoomScale = (ZOOM_MAX * limitY) / (beginScaleY * innerImageHeight);
                            }

                       /*     if ((innerImageWidth >= innerImageHeight) && (beginScaleX * zoomScale * innerImageWidth <= ZOOM_MIN * limitX)) {
                                zoomScale = (ZOOM_MIN  * limitX)/ (beginScaleX * innerImageWidth);
                            }

                            if ((innerImageWidth < innerImageHeight) && (beginScaleY * zoomScale * innerImageHeight <= ZOOM_MIN * limitY)) {
                                zoomScale = (ZOOM_MIN * limitY) / (beginScaleY * innerImageHeight);
                            } */

                            if (( limitX / innerImageWidth ) < ( limitY / innerImageHeight ) ) {
                                if (beginScaleX * zoomScale * innerImageWidth <= ZOOM_MIN * limitX)
                                    zoomScale = (ZOOM_MIN  * limitX)/ (beginScaleX * innerImageWidth);
                            }
                            else {
                                if (beginScaleY * zoomScale * innerImageHeight <= ZOOM_MIN * limitY)
                                    zoomScale = (ZOOM_MIN * limitY) / (beginScaleY * innerImageHeight);
                            }

                            float finalX = midPoint.x - (midPoint.x - beginX) * zoomScale;
                            float finalY = midPoint.y - (midPoint.y - beginY) * zoomScale;

                            //Ищем середину фотки
                            if (zoomScale < 1) {
                                float photoMiddleY = beginScaleY * zoomScale * innerImageHeight / 2 + finalY;
                                if (photoMiddleY < limitY / 2) {

                                    float deltaY = finalY - beginY;

                                    if ( (limitY / 2 - photoMiddleY) > 2 * deltaY )
                                        finalY = finalY + deltaY;
                                    else
                                        finalY = finalY + limitY / 2 - photoMiddleY;
                                }
                                else {
                                    float deltaY = finalY - beginY;

                                    if ( (photoMiddleY - limitY / 2) > 2 * deltaY )
                                        finalY = finalY - deltaY;
                                    else
                                        finalY = finalY - (photoMiddleY - limitY / 2);
                                }
                            }

                            if (zoomScale < 1) {
                                float photoMiddleX = beginScaleX * zoomScale * innerImageWidth / 2 + finalX;
                                if (photoMiddleX < limitX / 2) {
                                    float deltaX = finalX - beginX;

                                    if ( (limitX / 2 - photoMiddleX) > 2 * deltaX )
                                        finalX = finalX + deltaX;
                                    else
                                        finalX = finalX + limitX / 2 - photoMiddleX;
                                }
                                else {
                                    float deltaX = finalX - beginX;

                                    if ( (photoMiddleX - limitX / 2) > 2 * deltaX )
                                        finalX = finalX - deltaX;
                                    else
                                        finalX = finalX - (photoMiddleX - limitX / 2);
                                }
                            }

                            if ( (beginScaleX * zoomScale * innerImageWidth <= limitX) ) {
                                finalX = (limitX / 2) - (beginScaleX * zoomScale * innerImageWidth / 2);
                            }

                            if ( (beginScaleY * zoomScale * innerImageHeight <= limitY) ) {
                                finalY = (limitY / 2) - (beginScaleY * zoomScale * innerImageHeight / 2);
                            }

/*                            //Проверяем начальные координаты на правильность
                            if ((innerImageWidth >= innerImageHeight) && (finalX > 0)) {
                                finalX = 0;
                            }
                            if ((innerImageWidth < innerImageHeight) && (finalY > 0)) {
                                finalY = 0;
                            }*/

                            if ((beginScaleX * zoomScale * innerImageWidth > limitX) && (beginScaleY * zoomScale * innerImageHeight > limitY)) {
                                if (finalX > 0) { finalX = 0; }
                                if (finalY > 0) { finalY = 0; }

                                if (finalX + beginScaleX * zoomScale * innerImageWidth < limitX) {
                                    finalX = limitX - beginScaleX * zoomScale * innerImageWidth;
                                }

                                if (finalY + beginScaleY * zoomScale * innerImageHeight < limitY) {
                                    finalY = limitY - beginScaleY * zoomScale * innerImageHeight;
                                }
                            }

                            if ((innerImageWidth >= innerImageHeight) && (beginScaleX * zoomScale * innerImageWidth > limitX) &&
                                    (finalX + beginScaleX * zoomScale * innerImageWidth < limitX)) {
                                finalX = limitX - beginScaleX * zoomScale * innerImageWidth;
                            }
                            if ((innerImageWidth < innerImageHeight) && (beginScaleY * zoomScale * innerImageHeight > limitY) &&
                                    (finalY + beginScaleY * zoomScale * innerImageHeight < limitY)) {
                                finalY = limitY - beginScaleY * zoomScale * innerImageHeight;
                            }

                            matrix.reset();
                            matrix.setScale(beginScaleX * zoomScale, beginScaleY * zoomScale, 0, 0);
                            matrix.postTranslate(finalX, finalY);
                            iView.setScaleType(ImageView.ScaleType.MATRIX);
                            iView.setImageMatrix(matrix);

                        }

                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }

            return true;
        }
    }
}