package com.pdnsoftware.writtendone;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MyRecViewAdapter extends RecyclerView.Adapter<MyRecViewAdapter.MyViewHolder> {

    private List<ScriptRecord> data;
    private AppCompatActivity callerAppCompatActivity;
    private MyScriptDBManager myDB;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        TextView tv_title;
        TextView tv_content;
        TextView tvDateTimeCreated;
        ImageButton delRecordButton;
        ImageView clipImage, thumbnail1, thumbnail2, thumbnail3;
        TextView picturesCount;
        LinearLayout linLayoutForThumbnails;


        MyViewHolder(View v) {
            super(v);
            tv_title = v.findViewById(R.id.tv_title);
            tv_content = v.findViewById(R.id.tv_content);
            delRecordButton = v.findViewById(R.id.delRecordButton);
            tvDateTimeCreated = v.findViewById(R.id.tvDateTimeCreated);
            clipImage = v.findViewById(R.id.clip_image);
            picturesCount = v.findViewById(R.id.pictures_count);
            thumbnail1 = v.findViewById(R.id.thumbnail1);
            thumbnail2 = v.findViewById(R.id.thumbnail2);
            thumbnail3 = v.findViewById(R.id.thumbnail3);
            linLayoutForThumbnails = v.findViewById(R.id.lin_layout_tumbnails);

            v.setOnClickListener(this);
            delRecordButton.setOnClickListener(delButtonClick);

        }

        //По нажатию на запись открываем ее для редактирования
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setClass(view.getContext(), ScriptEdit.class);
            int itemToUpdateIndex = getLayoutPosition();
            intent.putExtra(MyScriptDB.ROW_ID, data.get(itemToUpdateIndex).getRowId());
            intent.putExtra(MainActivity.CALLER_ACTIVITY_NAME, MainActivity.NAME_INTENT_MAINACTIVITY);
            view.getContext().startActivity(intent);
        }


        View.OnClickListener delButtonClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View view = v;
                AlertDialog.Builder sureDeleteDialog = new AlertDialog.Builder(v.getContext());

                sureDeleteDialog.setMessage(v.getResources().getString(R.string.deleteTaskQuestion) + " \"" + tv_title.getText().toString() + "\"?");

                sureDeleteDialog.setPositiveButton(R.string.deleteTaskYes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int itemToDeleteIndex = getLayoutPosition();
                        ScriptRecord currItem = data.get(itemToDeleteIndex);
                        MyScriptDBManager myDB = new MyScriptDBManager(view.getContext());
                        myDB.deleteScript(currItem.getRowId());
                        data.remove(itemToDeleteIndex);
                        MyRecViewAdapter.this.notifyDataSetChanged();

                        updateActionBar();

                    }
                });

                sureDeleteDialog.setNegativeButton(R.string.deleteTaskCancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                sureDeleteDialog.setCancelable(false);
                sureDeleteDialog.create();
                sureDeleteDialog.show();
            }
        };
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    MyRecViewAdapter(List<ScriptRecord> myDataset) {
        data = myDataset;
    }

    void setActivity(AppCompatActivity appCompatActivity) {
        this.callerAppCompatActivity = appCompatActivity;
    }

    private void updateActionBar() {
        if (callerAppCompatActivity != null) {
            MyScriptDBManager myDB = new MyScriptDBManager(callerAppCompatActivity.getApplicationContext());
            int taskCount = myDB.getTasksCount();
            ActionBar currBar = callerAppCompatActivity.getSupportActionBar();
            if (currBar != null)
                currBar.setTitle(String.format(Locale.getDefault(),
                        callerAppCompatActivity.getResources().getString(R.string.mainActivitySign)
                                + " (%s)", taskCount));
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_layout, parent, false);

        return new MyViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.tv_title.setText(data.get(position).getTitle());
        holder.tv_content.setText(data.get(position).getContent());
        holder.tvDateTimeCreated.setText(data.get(position).getCreatedDate());

        int padding2dp = (int) holder.itemView.getContext().getResources().getDisplayMetrics().density * 2;

        if (data.get(position).getPictCount() == 0) {
            holder.clipImage.setVisibility(View.INVISIBLE);
            holder.picturesCount.setVisibility(View.INVISIBLE);
            holder.linLayoutForThumbnails.setVisibility(View.GONE);
            holder.thumbnail1.setVisibility(View.GONE);
            holder.thumbnail2.setVisibility(View.GONE);
            holder.thumbnail3.setVisibility(View.GONE);
        }
        else if (data.get(position).getPictCount() > 0) {
            holder.clipImage.setVisibility(View.VISIBLE);
            holder.picturesCount.setVisibility(View.VISIBLE);
            holder.picturesCount.setText(String.format(Locale.getDefault(), "%s", data.get(position).getPictCount()));
        }

        if (data.get(position).getTitle().length() > 0 && data.get(position).getContent().length() > 0) {
            holder.tv_title.setVisibility(View.VISIBLE);
            holder.tv_content.setVisibility(View.VISIBLE);
            holder.tv_content.setPadding(0, 0, 0, 0);

            holder.linLayoutForThumbnails.setVisibility(View.GONE);
            holder.thumbnail1.setVisibility(View.GONE);
            holder.thumbnail2.setVisibility(View.GONE);
            holder.thumbnail3.setVisibility(View.GONE);


        }
        else if (data.get(position).getTitle().length() == 0 && data.get(position).getContent().length() > 0) {
            holder.tv_title.setVisibility(View.GONE);
            holder.tv_content.setVisibility(View.VISIBLE);
            holder.tv_content.setPadding(0, padding2dp, 0, 0);

            holder.linLayoutForThumbnails.setVisibility(View.GONE);
            holder.thumbnail1.setVisibility(View.GONE);
            holder.thumbnail2.setVisibility(View.GONE);
            holder.thumbnail3.setVisibility(View.GONE);

        }
        else if (data.get(position).getContent().length() == 0 && data.get(position).getTitle().length() > 0) {
            holder.tv_title.setVisibility(View.VISIBLE);
            holder.tv_content.setVisibility(View.GONE);
            holder.tv_content.setPadding(0, 0, 0, 0);

            holder.linLayoutForThumbnails.setVisibility(View.GONE);
            holder.thumbnail1.setVisibility(View.GONE);
            holder.thumbnail2.setVisibility(View.GONE);
            holder.thumbnail3.setVisibility(View.GONE);

        }
        else if (data.get(position).getContent().length() == 0 && data.get(position).getTitle().length() == 0 &&
                data.get(position).getPictCount() > 0) {

            boolean gotPicture1, gotPicture2, gotPicture3;
            gotPicture1 = gotPicture2 = gotPicture3 = false;

            if (myDB == null)
                myDB = new MyScriptDBManager(callerAppCompatActivity.getApplicationContext());

            List<PictureRecord> pictArray = myDB.getOneScriptPictures(data.get(position).getRowId());

            int i;

            for (i = 0; i < pictArray.size(); i++) {
                if (i == 0) {
                    Bitmap tmb1 = getPictThumbnail(pictArray.get(i).getPicturePath() + "/" + pictArray.get(i).getPictureName());
                    if (tmb1 != null) {
                        holder.thumbnail1.setImageBitmap(tmb1);
                        holder.thumbnail1.setVisibility(View.VISIBLE);
                        gotPicture1 = true;
                    }
                }
                if (i == 1) {
                    Bitmap tmb2 = getPictThumbnail(pictArray.get(i).getPicturePath() + "/" + pictArray.get(i).getPictureName());
                    if (tmb2 != null) {
                        holder.thumbnail2.setImageBitmap(tmb2);
                        holder.thumbnail2.setVisibility(View.VISIBLE);
                        gotPicture2 = true;
                    }
                }
                if (i == 2) {
                    Bitmap tmb3 = getPictThumbnail(pictArray.get(i).getPicturePath() + "/" + pictArray.get(i).getPictureName());
                    if (tmb3 != null) {
                        holder.thumbnail3.setImageBitmap(tmb3);
                        holder.thumbnail3.setVisibility(View.VISIBLE);
                        gotPicture3 = true;
                    }
                }
            }

            if (gotPicture1 || gotPicture2 || gotPicture3) {
                holder.tv_title.setVisibility(View.GONE);
                holder.tv_content.setVisibility(View.GONE);
                holder.linLayoutForThumbnails.setVisibility(View.VISIBLE);
            }

            if (!gotPicture1)
                holder.thumbnail1.setVisibility(View.GONE);
            if (!gotPicture2)
                holder.thumbnail2.setVisibility(View.GONE);
            if (!gotPicture3)
                holder.thumbnail3.setVisibility(View.GONE);


        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return data.size();
    }

    private Bitmap getPictThumbnail(String filePath) {
        //Рассчитываем размеры картинок
        int pictWidth, pictHeight;
        int pictInDp = 30;

        Bitmap myBitmap1, freshBitmap1;
        File imgFile;

        //Setting bitmap options to avoid missing of memory
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;

        freshBitmap1 = null;

        int orientation;
        ExifInterface exifObject;

        pictHeight = pictWidth = pictInDp * (int) callerAppCompatActivity.getApplicationContext().getResources().getDisplayMetrics().density;

        imgFile = new File(filePath);

        if (imgFile.exists()) {

            myBitmap1 = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

            try {
                exifObject = new ExifInterface(imgFile.getAbsolutePath());
                orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                freshBitmap1 = ScriptEdit.rotateBitmap(myBitmap1, orientation);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (freshBitmap1 != null) {
//                if (myBitmap1 != null)
//                    myBitmap1.recycle();

                return ThumbnailUtils.extractThumbnail(freshBitmap1, pictWidth, pictHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            } else {
                if (myBitmap1 != null) {
                    return ThumbnailUtils.extractThumbnail(myBitmap1, pictWidth, pictHeight,
                            ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                }
            }
        }
        return null;
    }
}