package com.pdnsoftware.writtendone;

import android.content.Context;
import android.util.AttributeSet;

public class ExtendedImageView extends android.support.v7.widget.AppCompatImageView {

    public ExtendedImageView(Context context) {
        super(context);
    }

    public ExtendedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean performClick() {
        // если что-то необходимо
        super.performClick();
        return true;
    }
}
