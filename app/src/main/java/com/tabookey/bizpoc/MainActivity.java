package com.tabookey.bizpoc;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private View frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frame = findViewById(R.id.frame_layout);
        Fragment firstFragment = new ImportApiKeyFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frame_layout, firstFragment).commit();
    }
}
