package com.magicleap.httpserverangel;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class CreditsActivity extends AppCompatActivity {
    private static final String MIME_TYPE = "text/html";
    private static final String ENCODING = "utf-8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
    }

    @Override
    protected void onStart() {
        super.onStart();
        StringBuilder sb = new StringBuilder();
        String[] messages = getResources().getStringArray(R.array.credits);
        for (String message : messages) {
            sb.append(message).append('\n');
        }
        WebView web = findViewById(R.id.web_credits);
        web.loadData(sb.toString(), MIME_TYPE, ENCODING);
    }
}
