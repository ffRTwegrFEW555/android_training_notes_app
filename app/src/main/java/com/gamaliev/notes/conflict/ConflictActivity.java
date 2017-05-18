package com.gamaliev.notes.conflict;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.gamaliev.notes.R;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ConflictActivity extends AppCompatActivity {

    /* Logger */
    private static final String TAG = ConflictActivity.class.getSimpleName();


    /*
        Init
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conflict);
        init();
    }

    private void init() {
        initToolbar();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_conflict_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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

        Intent starter = new Intent(context, ConflictActivity.class);
        ((Activity) context).startActivityForResult(starter, requestCode);
    }
}
