package com.example.myscript;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    public MyRecViewAdapter curr_adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ActionMode actionMode; //Переменная для вызова меню

    private List<ScriptRecord> test_data = new ArrayList<ScriptRecord>();

    private MyScriptDBManager myDB;

    /**/
    public RecyclerView recyclerView;
    /**/

    private ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.ma_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Заводим ActionBar, чтобы на нем была стрелка назад
        ActionBar currActionBar = getSupportActionBar();
        currActionBar.setTitle(R.string.mainActivitySign);

        recyclerView = (RecyclerView) findViewById(R.id.task_list);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Извлекаем данные из БД
        myDB = new MyScriptDBManager(this);
        test_data = myDB.getTasksList();

        // specify an adapter (see also next example)
        curr_adapter = new MyRecViewAdapter(test_data);

        //Наводим красоту
        recyclerView.setAdapter(curr_adapter);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ma_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addnewscript:
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), ScriptEdit.class);
                startActivity(intent);
                break;
            case R.id.quitApp:
                finish();
                break;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Перечитываем список задач заново
        test_data.clear();
        test_data.addAll(myDB.getTasksList());
        curr_adapter.notifyDataSetChanged();
    }
}
