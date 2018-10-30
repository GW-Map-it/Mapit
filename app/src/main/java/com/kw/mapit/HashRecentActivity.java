package com.kw.mapit;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

public class HashRecentActivity extends AppCompatActivity {
    ListView listView;
    ListAdapter listAdapter;

    Intent getIntent;
    String[] hashtags;
    long[] num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hash_recent);

        listView = (ListView)findViewById(R.id.listView);

        //최근 해시태그 받아오기
        getIntent = getIntent();
        hashtags = getIntent.getStringArrayExtra("RECENT_HASHTAG");
        num = getIntent.getLongArrayExtra("NUM_RECENT_HASHTAG");

        listAdapter = new ListAdapter(this, hashtags, num);
        listView.setAdapter(listAdapter);
    }
}
