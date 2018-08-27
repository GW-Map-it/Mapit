package com.kw.mapit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText edit_id= (EditText)findViewById(R.id.ed_login_id);
        EditText edit_pw= (EditText)findViewById(R.id.ed_login_password);
        edit_id.setTextColor(Color.BLACK);
        edit_pw.setTextColor(Color.BLACK);
    }

    public void onClick(View v) {
        if(v.getId() == R.id.btn_sign_in) {
            Intent goIntent = new Intent(this, InputDataActivity.class);
            startActivity(goIntent);
        }
    }
}
