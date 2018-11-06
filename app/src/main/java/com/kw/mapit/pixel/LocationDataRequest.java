package com.kw.mapit.pixel;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * OkHttp3 + Retrofit2 ?
 **/
public class LocationDataRequest {

    public interface OnLocationResponseListener {
        void onLocationResponse(@NonNull JSONObject object);
    }

    public static void run(@NonNull final String requestUrl, final OnLocationResponseListener listener){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if(listener == null)
                    return;

                try {
                    URL url = new URL(requestUrl);

                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String json;
                    while( (json = bufferedReader.readLine()) != null){
                        sb.append(json+"\n");
                    }

                    if(sb.length() > 0)
                        listener.onLocationResponse(new JSONObject(sb.toString().trim()));
                } catch(Exception e) {
                    //Pass to return line
                }
                //listener.onLocationResponse(new JSONObject());
            }
        });
    }
}
