package com.gamaliev.notes.user;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = UserActivity.class.getSimpleName();

    /* Intents */
    public static final int REQUEST_CODE_CONFIGURE_USER = 101;

    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        init();
    }

    private void init() {
        initToolbar();
        initFabOnClickListener();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_user_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initListView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Create adapter.
                final UserAdapter adapter = new UserAdapter(getApplicationContext());

                // Init list view
                final ListView listView = (ListView) findViewById(R.id.activity_user_list_view);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                    }
                });
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        SpUsers.setSelected(getApplicationContext(), String.valueOf(id));
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
        }).start();
    }

    /**
     * Add new user.
     */
    private void initFabOnClickListener() {
        findViewById(R.id.activity_user_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String newUserId = SpUsers.add(getApplicationContext(), null);
                UserPreferenceActivity.startIntent(
                        UserActivity.this,
                        REQUEST_CODE_CONFIGURE_USER,
                        newUserId);
            }
        });
    }


    /*
        Intents
     */

    /**
     * Start intent.
     * @param context       Context.
     * @param requestCode   This code will be returned in onActivityResult() when the activity exits.
     */
    public static void startIntent(
            @NonNull final Context context,
            final int requestCode) {

        Intent starter = new Intent(context, UserActivity.class);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }


    /*
        ...
     */

    @Override
    protected void onResume() {
        super.onResume();
        initListView();
    }
}