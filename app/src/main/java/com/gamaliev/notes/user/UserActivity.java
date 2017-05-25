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
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserActivity extends AppCompatActivity {

    /* Logger */
    @SuppressWarnings("unused")
    private static final String TAG = UserActivity.class.getSimpleName();

    /* Intents */
    public static final int REQUEST_CODE_CONFIGURE_USER = 101;


    /*
        Lifecycle
     */

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        init();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    /*
        ...
     */

    private void init() {
        initToolbar();
        initFabOnClickListener();
        initListView();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_user_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initListView() {
        // Create adapter.
        final BaseAdapter adapter = new UserAdapter(getApplicationContext());

        // Init list view
        final ListView listView = (ListView) findViewById(R.id.activity_user_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SpUsers.setSelected(getApplicationContext(), String.valueOf(id));
                setResult(RESULT_OK);
                finish();
            }
        });
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
     *
     * @param context     Context.
     * @param requestCode This code will be returned in onActivityResult() when the activity exits.
     */
    public static void startIntent(
            @NonNull final Context context,
            final int requestCode) {

        Intent starter = new Intent(context, UserActivity.class);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }


    /*
        Callbacks
     */

    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            final Intent data) {

        if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_CONFIGURE_USER) {
                initListView();
            }
        }
    }
}
