package com.kw.mapit;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HashPopularActivity extends AppCompatActivity {
    ListView listView;
    ListAdapter listAdapter;

    Intent getIntent;
    String[] hashtags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hash_popular);

        listView = (ListView)findViewById(R.id.listView);

        //인기 해시태그 받아오기
        getIntent = getIntent();
        hashtags = getIntent.getStringArrayExtra("POPULAR_HASHTAG");

        listAdapter = new ListAdapter(this, hashtags);
        listView.setAdapter(listAdapter);
    }

}
