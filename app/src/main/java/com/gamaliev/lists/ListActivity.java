package com.gamaliev.lists;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.view.View;
import android.view.Window;

import com.gamaliev.lists.colorpicker.ColorPickerActivity;

public class ListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
*/

        setContentView(R.layout.activity_list);
        init();
    }

    private void init() {

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ListActivity.this, ColorPickerActivity.class));
            }
        });

/*
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setExitTransition(new Explode());
                    Intent intent = new Intent(ListActivity.this, ColorPickerActivity.class);
                    startActivity(intent,
                            ActivityOptions
                                    .makeSceneTransitionAnimation(ListActivity.this).toBundle());
                } else {
                    Intent intent = new Intent(ListActivity.this, ColorPickerActivity.class);
                    startActivity(intent);
                }
            }
        });
*/

    }
}
