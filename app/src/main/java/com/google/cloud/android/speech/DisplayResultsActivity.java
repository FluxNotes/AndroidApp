package com.google.cloud.android.speech;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DisplayResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_results);
        TextView tv = (TextView)findViewById(R.id.textView);
        tv.setText(getIntent().getStringExtra("RESULT"));
    }
}
