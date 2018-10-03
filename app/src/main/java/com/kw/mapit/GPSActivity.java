package com.kw.mapit;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class GPSActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    final int PERISSIONS_ACCESS_LOCATION = 0;

    GoogleApiClient mGoogleApiClient;
    LocationManager locationManager;

    // 업데이트 거리 및 시간
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10m
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1분

    TextView tv_latitude;
    TextView tv_longitude;

    double latitude;            //위도
    double longitude;           //경도

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        //Permission 없으면 요청
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},PERISSIONS_ACCESS_LOCATION);
        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            Log.e("GPSActivity", "00000000000000000000000000000");
        }

    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    //Permission 요청 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERISSIONS_ACCESS_LOCATION: {
                //Permission 획득 성공
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Allow Mapit to access your location!", Toast.LENGTH_SHORT).show();
                }
                //Permission 획득 실패 >> MainActivity로 이동
                else {
                    Toast.makeText(getApplicationContext(), "Location permissions are required.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
                return;
            }
        }
    }

    //위치 변경 시
    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            latitude= location.getLatitude();
            longitude = location.getLongitude();

            tv_latitude.setText(String.valueOf(latitude));
            tv_longitude.setText(String.valueOf(longitude));
            Log.e("GPSActivity", "Latitude : " + latitude + " / Longitude : " + longitude);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("GPSActivity", "??????????????????????????");

        //Permission 확인
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.e("GPSActivity", "=========================");

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener) this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener) this);

            //Location 받아오기
            Location mLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (mLocation != null) {
                latitude= mLocation.getLatitude();
                longitude = mLocation.getLongitude();

                Log.e("GPSActivity", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1111");

                tv_latitude.setText(String.valueOf(latitude));
                tv_longitude.setText(String.valueOf(longitude));
                Log.e("GPSActivity", "Latitude : " + latitude + " / Longitude : " + longitude);
            }

        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("GPSActivity", "????????????????????///" + connectionResult.toString());
    }
}
