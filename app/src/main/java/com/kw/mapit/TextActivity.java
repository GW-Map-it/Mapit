package com.kw.mapit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.nhn.android.maps.NMapActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class TextActivity extends Activity {
    String myJSON;

    private static final String LOG_TAG = "TEXTViewer";
    private static final String TAG_RESULTS = "result";
    private static final String TAG_TEXT_NUM = "text_num";
    private static final String TAG_HASHTAG = "hashtag";
    private static final String TAG_TIME = "time";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";

    private String latitude = null;
    private String longitude = null;

    double centerLati;
    double centerLongi;

    JSONArray users = null;

    StringBuffer sbParams = null;

    private EditText EditText_HashTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_text);

        EditText_HashTag = (EditText)findViewById(R.id.editText_hashTag);

        //위도, 경도 값 받아오기
        Intent intent = getIntent();
        latitude = intent.getExtras().getString("latitude");
        longitude = intent.getExtras().getString("longitude");
        centerLati = intent.getExtras().getDouble("CENTER_LATITUDE");
        centerLongi = intent.getExtras().getDouble("CENTER_LONGITUDE");
        Log.e("Text", centerLati + ", " + centerLongi);
    }

    public void onClick(View v) {
        //"저장" 클릭 시
        if(v.getId() == R.id.button_save) {
            String hashtag = EditText_HashTag.getText().toString();
            sbParams = new StringBuffer();
            sbParams.append("hashtag=").append(" " + hashtag);
            sbParams.append("&").append("longitude=").append(longitude);
            sbParams.append("&").append("latitude=").append(latitude);

            InsertData task = new InsertData();
            String result = null;
            try {
                result = task.execute(hashtag).get();

                //DB에 사용자 정보 저장 성공 시
                if(result.contains("success")) {
                    EditText_HashTag.setText("");

                    Intent intent;
                    intent = new Intent(TextActivity.this, PixelActivity.class);
                    intent.putExtra("CENTER_LATITUDE", centerLati);
                    intent.putExtra("CENTER_LONGITUDE", centerLongi);
                    Log.e("Text", "Put : "+centerLati + ", " + centerLongi);
                    startActivity(intent);
                    TextActivity.this.finish();

                    //이전 DbConnectActivity 종료
                    NMapActivity activity = (NMapActivity)DbConnectActivity.activity_dbConnect;
                    activity.finish();

                    Toast.makeText(getApplicationContext(), "게시물 등록 성공!", Toast.LENGTH_LONG).show();
                }
                //DB에 사용자 정보 저장 실패 시
                else if(result.contains("Error")) {
                    Toast.makeText(getApplicationContext(), "게시물 등록 실패..", Toast.LENGTH_LONG).show();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            EditText_HashTag.setText("");
        }
        //"취소" 클릭 시
        else if(v.getId() == R.id.button_cancel) {
            finish();
        }
    }

    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(TextActivity.this,"please wait",null,true, true);
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(LOG_TAG,"POST response - " + result);
        }
        @Override
        protected String doInBackground(String... params) {

            String hashtag = (String)params[0];

            String serverURL = "http://" + getString(R.string.ip) + "/insertData.php";

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

    protected void showLog() {
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            users = jsonObj.getJSONArray(TAG_RESULTS);

            for (int i = 0; i < users.length(); i++) {
                JSONObject c = users.getJSONObject(i);
                String text_num = c.optString(TAG_TEXT_NUM);
                String hashtag = c.optString(TAG_HASHTAG);
                String time = c.optString(TAG_TIME);
                String latitude = c.optString(TAG_LATITUDE);
                String longitude = c.optString(TAG_LONGITUDE);

                Log.e(LOG_TAG, "text_num = " + text_num);
                Log.e(LOG_TAG, "hashtag = " + hashtag);
                Log.e(LOG_TAG, time);
                Log.e(LOG_TAG, latitude);
                Log.e(LOG_TAG, longitude);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getData(String url) {
        class GetDataJSON extends AsyncTask<String, Integer, String> {
            @Override
            protected String doInBackground(String... urls) {
                String uri = urls[0];
                BufferedReader bufferedReader = null;
                try {
                    //연결 url설정
                    URL url = new URL(uri);
                    //커넥션 객체 생성
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String json;
                    while ((json = bufferedReader.readLine()) != null) {
                        sb.append(json + "\n");
                    }
                    return sb.toString().trim();
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(String result) {
                Log.e(LOG_TAG, "RESULT = " + result);
                myJSON = result;
                showLog();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }
}
