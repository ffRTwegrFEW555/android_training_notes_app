package com.gamaliev.list.list;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.gamaliev.list.R;

public class ItemDetailsActivity extends AppCompatActivity {

    public final static String EXTRA_ID = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        Toast.makeText(
                this,
                String.valueOf(getIntent().getLongExtra(EXTRA_ID, -1)),
                Toast.LENGTH_SHORT)

                .show();
    }

    public static Intent getStartIntent(Context context, long id) {
        Intent intent = new Intent(context, ItemDetailsActivity.class);
        intent.putExtra(EXTRA_ID, id);
        return intent;
    }

}
