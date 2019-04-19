package com.example.myscript;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;


public class MyRecViewAdapter extends RecyclerView.Adapter<MyRecViewAdapter.MyViewHolder> {

    private List<ScriptRecord> data;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView tv_title;
        public TextView tv_content;
        public TextView tvDateTimeCreated;
        public ImageButton delRecordButton;

        public MyViewHolder(View v) {
            super(v);
            tv_title = (TextView)v.findViewById(R.id.tv_title);
            tv_content = (TextView)v.findViewById(R.id.tv_content);
            delRecordButton = (ImageButton)v.findViewById(R.id.delRecordButton);
            tvDateTimeCreated = (TextView)v.findViewById(R.id.tvDateTimeCreated);

            v.setOnClickListener(this);
            delRecordButton.setOnClickListener(delButtonClick);
        }

        //По нажатию на запись открываем ее для редактирования
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setClass(view.getContext(), ScriptEdit.class);
            int itemToUpdateIndex = getLayoutPosition();
            intent.putExtra(MyScriptDB.ROW_ID, data.get(itemToUpdateIndex).getRowId());
            view.getContext().startActivity(intent);
        }


        View.OnClickListener delButtonClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View view = v;
                AlertDialog.Builder sureDeleteDialog = new AlertDialog.Builder(v.getContext());

                sureDeleteDialog.setMessage(v.getResources().getString(R.string.deleteTaskQuestion) + " \"" + tv_title.getText().toString() + "\"");

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
    public MyRecViewAdapter(List<ScriptRecord> myDataset) {
        data = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_layout, parent, false);

        MyViewHolder vh = new MyViewHolder(v);

        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.tv_title.setText(data.get(position).getTitle());
        holder.tv_content.setText(data.get(position).getContent());
        holder.tvDateTimeCreated.setText(data.get(position).getCreatedDate());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return data.size();
    }
}
