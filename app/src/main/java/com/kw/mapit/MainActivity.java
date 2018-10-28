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
        else if(v.getId() == R.id.db_making){
            goIntent = new Intent(this, DataMakingActivity.class);
        }
        //DB_Connect & Write text
        else if(v.getId() == R.id.db_connect) {
            goIntent = new Intent(this, DbConnectActivity.class);
        }
        //Divide by pixel
        else if(v.getId() == R.id.pixel) {
            goIntent = new Intent(this, PixelActivity.class);
        }
        else if(v.getId() == R.id.pixel2) {
            goIntent = new Intent(this, PixelActivity2.class);
        }
        else if(v.getId() == R.id.get_hashtag) {
            goIntent = new Intent(this, PopularHashTagActivity.class);
        }
        else if(v.getId() == R.id.gps) {
            goIntent = new Intent(this, GPSActivity.class);
        }
        else if(v.getId() == R.id.mapit) {
            goIntent = new Intent(this, LoginActivity.class);
        }

        if(goIntent != null) {
            startActivity(goIntent);
        }
    }
}
