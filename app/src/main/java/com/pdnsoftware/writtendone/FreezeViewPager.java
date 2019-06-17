package com.pdnsoftware.writtendone;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class FreezeViewPager extends ViewPager implements PictShowPagerAdapter.MyOnTouchListener.StopVPScrolling {
    private boolean enabled;

    public FreezeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.enabled) {

            super.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;

                case MotionEvent.ACTION_UP:
                    performClick();
                    return true;
            }

        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
