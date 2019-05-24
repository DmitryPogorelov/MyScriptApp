package com.pdnsoftware.writtendone;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;


public class MyRecViewAdapter extends RecyclerView.Adapter<MyRecViewAdapter.MyViewHolder> {

    private List<ScriptRecord> data;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        TextView tv_title;
        TextView tv_content;
        TextView tvDateTimeCreated;
        ImageButton delRecordButton;
        ImageView clipImage;
        TextView picturesCount;


        MyViewHolder(View v) {
            super(v);
            tv_title = v.findViewById(R.id.tv_title);
            tv_content = v.findViewById(R.id.tv_content);
            delRecordButton = v.findViewById(R.id.delRecordButton);
            tvDateTimeCreated = v.findViewById(R.id.tvDateTimeCreated);
            clipImage = v.findViewById(R.id.clip_image);
            picturesCount = v.findViewById(R.id.pictures_count);

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

        int padding2dp = (int)holder.itemView.getContext().getResources().getDisplayMetrics().density * 2;

        if (data.get(position).getPictCount() == 0) {
            holder.clipImage.setVisibility(View.INVISIBLE);
            holder.picturesCount.setVisibility(View.INVISIBLE);
        }
        else if (data.get(position).getPictCount() > 0) {
            holder.clipImage.setVisibility(View.VISIBLE);
            holder.picturesCount.setVisibility(View.VISIBLE);
            holder.picturesCount.setText( String.format(Locale.US, "%s", data.get(position).getPictCount()));
        }

        if (data.get(position).getTitle().length() > 0 && data.get(position).getContent().length() > 0) {
            holder.tv_title.setVisibility(View.VISIBLE);
            holder.tv_content.setVisibility(View.VISIBLE);
            holder.tv_content.setPadding(0, 0, 0,0);
        }
        else if (data.get(position).getTitle().length() == 0 && data.get(position).getContent().length() > 0) {
            holder.tv_title.setVisibility(View.GONE);
            holder.tv_content.setVisibility(View.VISIBLE);
            holder.tv_content.setPadding(0, padding2dp, 0,0);

        }
        else if (data.get(position).getContent().length() == 0 && data.get(position).getTitle().length() > 0) {
            holder.tv_title.setVisibility(View.VISIBLE);
            holder.tv_content.setVisibility(View.GONE);
            holder.tv_content.setPadding(0, 0, 0,0);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return data.size();
    }
}