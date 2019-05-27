package com.pdnsoftware.writtendone;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MyRecViewAdapter curr_adapter;

    private List<ScriptRecord> test_data = new ArrayList<>();

    private MyScriptDBManager myDB;
    public static final String CALLER_ACTIVITY_NAME = "caller_activity_name";
    public static final String NAME_INTENT_MAINACTIVITY = "intent_MainActivity";
    /************************************************************************/
    private ActionBar currActionBar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.ma_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDB = new MyScriptDBManager(this);

        //Заводим ActionBar, чтобы на нем была стрелка назад и количество задач
        currActionBar = getSupportActionBar();
        if (currActionBar != null) {
            int taskCount = myDB.getTasksCount();
            currActionBar.setTitle(String.format(Locale.getDefault(), getResources().getString(R.string.mainActivitySign) + " (%s)", taskCount));
        }

        RecyclerView recyclerView = findViewById(R.id.task_list);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Извлекаем данные из БД

        test_data = myDB.getTasksList();

        // specify an adapter (see also next example)
        curr_adapter = new MyRecViewAdapter(test_data);

        curr_adapter.setActivity(this);

        //Наводим красоту
        recyclerView.setAdapter(curr_adapter);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addnewscript:
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), ScriptEdit.class);
                intent.putExtra(MainActivity.CALLER_ACTIVITY_NAME, MainActivity.NAME_INTENT_MAINACTIVITY);
                startActivity(intent);
                break;
            case R.id.goToPrivacyPolicy:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.linkToPrivacyPolicy)));
                startActivity(browserIntent);
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

        //Обновляем количество задач в ActionBar
        if (currActionBar != null) {
            int taskCount = myDB.getTasksCount();
            currActionBar.setTitle(String.format(Locale.getDefault(), getResources().getString(R.string.mainActivitySign) + " (%s)", taskCount));
        }
    }
}