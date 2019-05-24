package com.pdnsoftware.writtendone;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class pictures_viewer extends Fragment {

    public ImageButton pict1, pict2, pict3;
    private LinearLayout linLayout;

    public pictures_viewer() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pictures_viewer, container, false);

        pict1 = view.findViewById(R.id.pict1);
        pict2 = view.findViewById(R.id.pict2);
        pict3 = view.findViewById(R.id.pict3);
        linLayout = view.findViewById(R.id.layout_for_pictures);

        return view;
    }

    public void setPicture1(Bitmap pict) {
        pict1.setImageBitmap(pict);
    }

    public void setPicture2(Bitmap pict) {
        pict2.setImageBitmap(pict);
    }

    public void setPicture3(Bitmap pict) {
        pict3.setImageBitmap(pict);
    }

    public void hideLayout () {
        linLayout.setVisibility(View.GONE);
    }

    public void showLayout () {
        linLayout.setVisibility(View.VISIBLE);
    }
}