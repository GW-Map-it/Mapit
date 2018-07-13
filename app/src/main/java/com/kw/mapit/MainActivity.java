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

        //Google Login
        if(v.getId() == R.id.google_login) {
            goIntent = new Intent(this, GoogleLoginActivity.class);
        }
        //Kakao Login
        else if(v.getId() == R.id.kakao_login) {
            Toast.makeText(MainActivity.this, "Kakao Login 연결 필요", Toast.LENGTH_LONG).show();
            //goIntent = new Intent(this, KakaoLoginActivity.class);
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

        if(goIntent != null) {
            startActivity(goIntent);
        }
    }
}
