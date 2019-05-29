package com.pdnsoftware.writtendone;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class PictShowPagerAdapter extends PagerAdapter {

    private List<File> pictList;

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

        View page = LayoutInflater.from(container.getContext()).inflate(R.layout.pict_view_pager, null);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        Bitmap pictToReturn = BitmapFactory.decodeFile(pictList.get(position).getAbsolutePath(), options);
        ((ImageView)page.findViewById(R.id.showPicture)).setImageBitmap(pictToReturn);

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
}
