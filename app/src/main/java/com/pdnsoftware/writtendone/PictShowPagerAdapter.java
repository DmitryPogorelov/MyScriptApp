package com.pdnsoftware.writtendone;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.jetbrains.annotations.NotNull;
import java.io.File;
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

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        Bitmap pictToReturn = BitmapFactory.decodeFile(pictList.get(position).getAbsolutePath(), options);
        ((ImageView)page.findViewById(R.id.showPicture)).setImageBitmap(pictToReturn);
        page.findViewById(R.id.showPicture).setOnTouchListener(new MyOnTouchListener());
        //Add the page to the front of the queue
        container.addView(page, 0);
        return page;
    }
    @Override
    public boolean isViewFromObject(@NotNull View arg0, @NotNull Object arg1) {
        //See if object from instantiateItem is related to the given view
        //required by API
        return arg0==arg1;
    }
    @Override
    public void destroyItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
        container.removeView((View) object);
        object=null;
    }


    class MyOnTouchListener implements View.OnTouchListener {

        private boolean pinch = false;
        private double deltaPointerDown = 1;
        private double deltaPointerMove = 1;

        MyOnTouchListener() {}

        @Override
        public boolean onTouch(@NotNull View v, @NotNull MotionEvent event) {

            v.performClick();

            float deltaX;
            float deltaY;

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    pinch = false;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    pinch = true;
                    deltaX = event.getX(0) - event.getX(1);
                    deltaY = event.getY(0) - event.getY(1);
                    deltaPointerDown = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (pinch) {
                        deltaX = event.getX(0) - event.getX(1);
                        deltaY = event.getY(0) - event.getY(1);
                        deltaPointerMove = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        if (deltaPointerDown != 0) {

                            setScaleBitmap(((ImageView)v), deltaPointerMove / deltaPointerDown);
                        }

                    }
                    break;
                case MotionEvent.ACTION_UP:
                    pinch = false;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    pinch = false;
                    break;
            }
            return true;
        }

        private void setScaleBitmap(ImageView iV, double curScale) {

            if (curScale > 1) {
                double greater1 = (curScale - 1) / 4;
                curScale = 1 + greater1;
            }
            if (curScale < 1) {
                double lessThan1 = (1 - curScale) / 4;
                curScale = 1 - lessThan1;
            }

            int newHeight = (int) (iV.getHeight() * curScale);
            int newWidth = (int) (iV.getWidth() * curScale);

            double minimalWidth = 0.25 * iV.getContext().getResources().getDisplayMetrics().widthPixels;
            double minimalHeight = 0.25 * iV.getContext().getResources().getDisplayMetrics().heightPixels;

            double maximalWidth = 2 * iV.getContext().getResources().getDisplayMetrics().widthPixels;
            double maximalHeight = 2 * iV.getContext().getResources().getDisplayMetrics().heightPixels;

            if (newWidth >  minimalWidth && newHeight > minimalHeight && newWidth <  maximalWidth && newHeight < maximalHeight) {
                //LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(newWidth, newHeight);
                //iV.setLayoutParams(parms);
                iV.getLayoutParams().height = newHeight;
                iV.getLayoutParams().width = newWidth;
                iV.requestLayout();
            }
        }
    }
}
