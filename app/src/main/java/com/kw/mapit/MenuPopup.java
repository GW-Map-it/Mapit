package com.kw.mapit;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

public class MenuPopup extends Activity {
    String[] popular_hash;
    String[] recent_hash;
    int[] num_popular_hash;
    long[] num_recent_hash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_menu_popup);

        Intent getIntent = getIntent();
        popular_hash = getIntent.getStringArrayExtra("POPULAR_HASHTAG");
        num_popular_hash = getIntent.getIntArrayExtra("NUM_POPULAR_HASHTAG");
        recent_hash = getIntent.getStringArrayExtra("RECENT_HASHTAG");
        num_recent_hash = getIntent.getLongArrayExtra("NUM_RECENT_HASHTAG");
    }

    public void onClick(View v) {
        if(v.getId() == R.id.popularHashtag) {
            Intent intent = new Intent(this, HashPopularActivity.class);
            intent.putExtra("POPULAR_HASHTAG", popular_hash);
            intent.putExtra("NUM_POPULAR_HASHTAG", num_popular_hash);
            startActivity(intent);
        }
        else if(v.getId() == R.id.recentHashtag) {
            Intent intent = new Intent(this, HashRecentActivity.class);
            intent.putExtra("RECENT_HASHTAG", recent_hash);
            intent.putExtra("NUM_RECENT_HASHTAG", num_recent_hash);
            startActivity(intent);
        }
    }
}
