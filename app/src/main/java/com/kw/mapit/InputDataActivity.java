package com.kw.mapit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class InputDataActivity extends AppCompatActivity {

    private static final String LOG_TAG = "APISigninView";
    StringBuffer sbParams = null;

    TextView tv_signIn;
    EditText loginId;
    EditText loginPassword;
    EditText loginName;
    EditText loginEmail;
    EditText loginAge;
    RadioGroup loginSex;

    String userId;
    String userPassword;
    String userName;
    String userSex;
    int userAge;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data);

        loginId = (EditText) findViewById(R.id.ed_api_signin_id);
        loginPassword = (EditText) findViewById(R.id.ed_api_signin_password);
        loginName = (EditText) findViewById(R.id.ed_api_signin_name);
        loginSex = (RadioGroup) findViewById(R.id.rg_sex);
        loginAge = (EditText) findViewById(R.id.ed_api_signin_age);
        loginEmail = (EditText) findViewById(R.id.ed_api_signin_email);

        loginName.setText(userName);
        loginEmail.setText(userEmail);

        //Google/Kakao 로그인으로 연결한 사용자 정보 받아오기
        Intent getIntent = getIntent();
        if (getIntent != null) {
            String id = getIntent.getStringExtra("USER_ID");
            String name = getIntent.getStringExtra("USER_NAME");
            String email = getIntent.getStringExtra("USER_EMAIL");

            loginId.setText(id);
            loginEmail.setText(email);
            loginName.setText(name);

            Log.e("KakaoLogin", "ID : " + id + " / Name : " + name + " / Email : " + email);
        }

    }

    public void onClick(View v) throws ExecutionException, InterruptedException {
        //Back 버튼 클릭
        if(v.getId() == R.id.btn_back)
        {
            finish();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        //Sign in 버튼 클릭
        else if(v.getId() == R.id.btn_sign_in)
        {
            //EditText에서 정보 받아오기
            userId = loginId.getText().toString();
            userPassword = loginPassword.getText().toString();
            userName = loginName.getText().toString();
            int radioId = loginSex.getCheckedRadioButtonId();
            RadioButton rb = (RadioButton)findViewById(radioId);
            if(rb.getText().toString().equals("Male"))
            {
                userSex = "M";
            }
            else if(rb.getText().toString().equals("Female"))
            {
                userSex = "F";
            }

            try{
                userAge = Integer.parseInt(loginAge.getText().toString());
            }
            catch (NumberFormatException e){
                Log.e("superdroid","UserAge is Not Integer");
            }
            userEmail = loginEmail.getText().toString();

            Log.e("superdroid", userId + " - " + userPassword+ " - " + userName + " - " + userSex+ " - " + userAge+ " - " + userEmail);

            //Insert 사용자 정보
            sbParams = new StringBuffer();
            sbParams.append("id=").append(userName);
            sbParams.append("&").append("password=").append(userPassword);
            sbParams.append("&").append("name=").append(userName);
            sbParams.append("&").append("sex=").append(userSex);
            sbParams.append("&").append("age=").append(userAge);
            sbParams.append("&").append("email=").append(userEmail);

            InsertData task = new InsertData();
            String result = task.execute(userName).get();

            //DB에 사용자 정보 저장 성공 시
            if(result.contains("success")) {
                loginId.setText("");
                loginPassword.setText("");
                loginName.setText("");
                rb.setSelected(false);
                loginAge.setText("");
                loginEmail.setText("");

                //LoginActivity 이동
                redirectLoginActivity();
                InputDataActivity.this.finish();

                //이전 LoginActivity 종료
                Activity activity = (Activity)LoginActivity.activity_login;
                activity.finish();

                Toast.makeText(getApplicationContext(), "Mapit에 오신 것을 환영합니다:)", Toast.LENGTH_LONG).show();
            }
            //DB에 사용자 정보 저장 실패 시
            else {
                Toast.makeText(getApplicationContext(), "회원가입에 실패하였습니다:(", Toast.LENGTH_LONG).show();
            }
        }
    }

    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(InputDataActivity.this,"please wait",null,true, true);
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(LOG_TAG,"POST response - " + result);
        }
        @Override
        protected String doInBackground(String... params) {

            String hashtag = (String)params[0];

            String serverURL = "http://" + getString(R.string.ip) + "/insertUserData.php";

            try{
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(sbParams.toString().getBytes("UTF-8"));
                //outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode=httpURLConnection.getResponseCode();
                Log.d(LOG_TAG,"POST respose code - " +responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString();

            } catch (Exception e) {
                Log.e(LOG_TAG,"InsertData: Error ",e);

                return new String("Error: "+e.getMessage());
            }
        }
    }

    //MainActivity로 이동
    private void redirectLoginActivity() {
        Intent userProfileIntent = new Intent(this, LoginActivity.class);
        startActivity(userProfileIntent);
        finish();
    }
}
