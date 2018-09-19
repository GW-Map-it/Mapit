package com.kw.mapit;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void myClick(View v) {
        Intent goIntent = null;

        //Main Login
        if(v.getId() == R.id.main_login) {
            goIntent = new Intent(this, LoginActivity.class);
        }
        //DB_Connect & Write text
        else if(v.getId() == R.id.db_connect) {
            goIntent = new Intent(this, DbConnectActivity.class);
        }
        //Divide by pixel
        else if(v.getId() == R.id.pixel) {
            goIntent = new Intent(this, PixelActivity.class);
        }
        else if(v.getId() == R.id.main_map) {
            Toast.makeText(MainActivity.this, "아직..", Toast.LENGTH_LONG).show();
            //goIntent = new Intent(this, MainMapActivity.class);
        }
        else if(v.getId() == R.id.get_hashtag) {
            goIntent = new Intent(this, PopularHashTagActivity.class);
        }

        if(goIntent != null) {
            startActivity(goIntent);
        }
    }
}
