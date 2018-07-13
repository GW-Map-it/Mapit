package com.kw.mapit;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class GoogleLoginActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GoogleLoginActivity";
    static public GoogleApiClient mGoogleApiClient;

    ImageButton gLogin = null;
    String userName = null;
    TextView text = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        gLogin = (ImageButton)findViewById(R.id.googleLogin);
    }

    public void onClick(View v)
    {
        if(v.getId() == R.id.googleSignin)
        {
            Toast.makeText(this, "Google에 접속", Toast.LENGTH_SHORT).show();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .build();

            mGoogleApiClient.connect();
        }

        if(v.getId() == R.id.googleLogin) {
            if(mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            } else {
                Toast.makeText(this, "구글 아이디로 연동해주세요", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //구글 연결
        if (!mGoogleApiClient.isConnected() || Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) == null) {
            //연결 실패
        } else {
            //연결 성공
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

            if (currentPerson.hasImage()) {
                //이미지 경로 : currentPerson.getImage().getUrl());
               /* Glide.with(MainActivity.this)
                        .load(currentPerson.getImage().getUrl())
                        .into(userphoto);*/
            }
            if (currentPerson.hasDisplayName()) {
                //디스플레이 이름 :currentPerson.getDisplayName());
                //디스플레이 아이디 : currentPerson.getId());
                userName = currentPerson.getDisplayName();
            }

            //로그인 액티비티로 이동
            Intent intent = new Intent(this, GoogleLogin2Activity.class);
            intent.putExtra("NAME", userName);
            startActivity(intent);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    //연결 실패 시
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            Log.e(TAG,
                    String.format(
                            "Connection to Play Services Failed, error: %d, reason: %s",
                            connectionResult.getErrorCode(),
                            connectionResult.toString()));
            try {
                connectionResult.startResolutionForResult(this, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString(), e);
            }
        } else {
            Toast.makeText(getApplicationContext(), "이미 로그인 중", Toast.LENGTH_SHORT).show();
        }
    }

}
