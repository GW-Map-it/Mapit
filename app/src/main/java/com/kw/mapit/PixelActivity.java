package com.kw.mapit;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.kw.mapit.pixel.LocationData;
import com.kw.mapit.pixel.LocationDataRequest;
import com.kw.mapit.pixel.PixelUtil;

import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapOverlay;
import com.nhn.android.maps.NMapOverlayItem;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.nmapmodel.NMapPlacemark;
import com.nhn.android.maps.overlay.NMapCircleData;
import com.nhn.android.maps.overlay.NMapCircleStyle;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.maps.overlay.NMapPathData;
import com.nhn.android.maps.overlay.NMapPathLineStyle;
import com.nhn.android.mapviewer.overlay.NMapCalloutCustomOverlay;
import com.nhn.android.mapviewer.overlay.NMapCalloutOverlay;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;
import com.nhn.android.mapviewer.overlay.NMapPathDataOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class PixelActivity extends NMapActivity implements NMapView.OnMapStateChangeListener, LocationDataRequest.OnLocationResponseListener {

    ArrayList<DupCenter> centerList = new ArrayList<>(); //겹침원 없게 중심점 모아둘 리스트

    private static final int RECENT_TIME = 24;              //최근 24시간 내
    final int PERMISSION_ACCESS_FINE_LOCATION_AND_COARSE_LOCATION = 1;          //위치 권한

    //인기/최신 해시태그 액티비티에 넘어갈 배열
    String[] popular_hash;
    int[] num_popular_hash;
    String[] recent_hash;
    long[] num_recent_hash;

    int myRandomNumber;

    LinearLayout container;

    boolean isInit=false;
    boolean isCircle = false;           //원이 그려졌는가

    long now;                       //시간
    Date currentDate;
    SimpleDateFormat sdf;
    String current_date;

    double currentLati;
    double currentLongi;

    JSONArray location = null;

    // API-KEY
    public static final String API_KEY = "29AIQje_U7muB8tVyofe";  //<---맨위에서 발급받은 본인 ClientID 넣으세요.
    // 네이버 맵 객체
    NMapView mMapView = null;
    // 맵 컨트롤러
    NMapController mMapController = null;
    // 맵을 추가할 레이아웃
    LinearLayout MapContainer;

    private static final String LOG_TAG = "NMapViewer";
    private static final boolean DEBUG = false;

    private NMapViewerResourceProvider mMapViewerResourceProvider;
    private NMapOverlayManager mOverlayManager;

    private NMapPOIdataOverlay mFloatingPOIdataOverlay;
    private NMapPOIitem mFloatingPOIitem;

    private List<LocationData> locationList = new ArrayList<>();

    private NMapMyLocationOverlay mMyLocationOverlay;
    private NMapLocationManager mMapLocationManager;
    private NMapCompassManager mMapCompassManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pixel);

        // 네이버 지도를 넣기 위한 LinearLayout 컴포넌트
        MapContainer = (LinearLayout) findViewById(R.id.MapContainer);

        // 네이버 지도 객체 생성
        mMapView = new NMapView(this);

        // 지도 객체로부터 컨트롤러 추출
        mMapController = mMapView.getMapController();

        // 네이버 지도 객체에 APIKEY 지정
        mMapView.setClientId(API_KEY);
        // 생성된 네이버 지도 객체를 LinearLayout에 추가시킨다.
        MapContainer.addView(mMapView);

        // 지도를 터치할 수 있도록 옵션 활성화f
        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setFocusable(true);
        mMapView.setFocusableInTouchMode(true);
        mMapView.requestFocus();

        // 확대/축소를 위한 줌 컨트롤러 표시 옵션 활성화
        mMapView.setBuiltInZoomControls(true, null);

        // 지도에 대한 상태 변경 이벤트 연결
        mMapView.setOnMapStateChangeListener(this);

        //create resource provider
        mMapViewerResourceProvider = new NMapViewerResourceProvider(this);

        //create overlay manager
        mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);

        // set data provider listener
        super.setMapDataProviderListener(onDataProviderListener);

        //Markers for POI item
        //int marker1 = NMapPOIflagType.PIN;

        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(1, mMapViewerResourceProvider);

        /*poiData.beginPOIdata(1);
        NMapPOIitem item = poiData.addPOIitem(null, "Touch & drag to Move", marker1, 0);
        if (item != null) {
            //initialize location to the center of the map view
            item.setPoint(mMapController.getMapCenter());

            //set floating mode
            item.setFloatingMode(NMapPOIitem.FLOATING_TOUCH | NMapPOIitem.FLOATING_DRAG);

            //show right button on callout
            item.setRightButton(true);

            item.setRightAccessory(true, NMapPOIflagType.CLICKABLE_ARROW);
            mFloatingPOIitem = item;
        }
        poiData.endPOIdata();
        */

        //create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        if (poiDataOverlay != null) {
            poiDataOverlay.setOnFloatingItemChangeListener(onPOIdataFloatingItemChangeListener);

            //set event listener to the overlay
            poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

            poiDataOverlay.selectPOIitem(0, false);

            mFloatingPOIdataOverlay = poiDataOverlay;
        }

        //register callout overlay listener to customize it.
        mOverlayManager.setOnCalloutOverlayListener(onCalloutOverlayListener);

        container = findViewById(R.id.parent);

        String serverURL = "http://" + getString(R.string.ip) + "/selectLocation.php";
        //getData(serverURL);
        LocationDataRequest.run(serverURL, this);

        //시간
        now = System.currentTimeMillis();
        //현재 시간
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //Location 권한 받기
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION_AND_COARSE_LOCATION);
        }
    }

    /**
     * 데이터 수신 완료
     * @param object {@link LocationData} 를 만들 수 있는 JSONObject result
     */
    @Override
    public void onLocationResponse(@NonNull JSONObject object) {
        locationList.clear();

        JSONArray jsonList = object.optJSONArray("result");
        Log.i(LOG_TAG,"result = "+jsonList);

        for(int i=0; i<jsonList.length(); i++){
            JSONObject location = jsonList.optJSONObject(i);
            if(location != null){
                LocationData data = new LocationData(location);
                locationList.add(data);

                //mapView.showPathDataOverlay(data);
            }
        }
    }

    public void onClick(View v) {
        if(v.getId() == R.id.btn_getLocation) {         //현재 위치로 이동
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_ACCESS_FINE_LOCATION_AND_COARSE_LOCATION);
            } else {
                startMyLocation();
            }
        }
        //"+"버튼 클릭 시 게시글 작성 액티비티로 이동
        else if(v.getId() == R.id.btn_addText) {
            Intent intent = new Intent(this, DbConnectActivity.class);
            Log.e("superdroid", mMapController.getMapCenter().getLatitude()+", "+mMapController.getMapCenter().getLongitude());
            intent.putExtra("CENTER_LATITUDE", mMapController.getMapCenter().getLatitude());
            intent.putExtra("CENTER_LONGITUDE", mMapController.getMapCenter().getLongitude());
            startActivity(intent);
            finish();
        }
        else if(v.getId() == R.id.btn_viewHashtag) {
            Intent intent = new Intent(this, MenuPopup.class);
            intent.putExtra("POPULAR_HASHTAG", popular_hash);
            intent.putExtra("NUM_POPULAR_HASHTAG", num_popular_hash);
            intent.putExtra("RECENT_HASHTAG", recent_hash);
            intent.putExtra("NUM_RECENT_HASHTAG", num_recent_hash);
            startActivity(intent);
        }
        else if(v.getId() == R.id.btn_logout) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            Toast.makeText(getApplicationContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    protected void matchData(){ //데이터를 점에 매칭

        for(LocationData data : locationList) {
            double longitude = data.getLongitude();
            double latitude = data.getLatitude();

            // set path data points
            NMapPathData pathData = new NMapPathData(1);

            //데이터 위치 점 찍어주는 부분
            pathData.initPathData();
            pathData.addPathPoint(longitude,latitude, NMapPathLineStyle.TYPE_SOLID);
            pathData.addPathPoint(longitude+0.00001, latitude+0.00001, 0);
            pathData.endPathData();

            NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
            pathLineStyle.setLineColor(0xA04DD2, 0xff);
            pathLineStyle.setFillColor(0xFFFFFF,0x00);
            pathData.setPathLineStyle(pathLineStyle);

            NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);

            // show all path data
            // pathDataOverlay.showAllPathData(mMapController.getZoomLevel());
        }
    }
    /**
     * 원과 점 사이의 거리로 원 안의 포함여부 계산한다
     * meanshift로 원 위치 계속 옮기고 마지막 한 번만 그리는 알고리즘
     */
    protected void meanShift(double initLong, double initLati, float radius, String hash, double percent) {

        if(locationList.size()==0)
            return;

        double dataDis;
        double sumLong = initLong;         //원 안에 속한 점이면 계속 더해줄 위도
        double sumLati = initLati;         //원 안에 속한 점이면 계속 더해줄 경도
        int count;                         //원 한 번 계산해줄때마다 sumLong과 sumLati 나눠줄 count
        NGeoPoint circleCenter;
        Point outPoint = null;

        for(int i = 0; i < locationList.size(); i++) {
            count = 1;
            for (LocationData data : locationList) {

                long duration = 1;

                if(data.getHashtag().contains(hash)){
                    double longitude = data.getLongitude();
                    double latitude = data.getLatitude();

                    Date hashDate = null;                //해당 게시물의 시간(Date형)
                    try {
                        hashDate = sdf.parse(data.getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    if(hashDate != null) {
                        duration = (currentDate.getTime() - hashDate.getTime()) / 1000 / 60;
                    }

                    if(duration >= 0 && duration <= 60) {               //60분 이내 게시물만 탐색
                        //Log.e("superdroid", "탐색 해시태그 : " + hash + " / 시간 : " + data.getTime());

                        NGeoPoint point = new NGeoPoint(longitude, latitude);
                        outPoint = mMapView.getMapProjection().toPixels(point, outPoint);

                        if (PixelUtil.isScreenInside(outPoint)) { //화면 안에 보이는 경우
                            circleCenter = new NGeoPoint((sumLong / count), (sumLati / count));
                            dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리

                            if (dataDis < radius) { //원 안에 있으면
                                count++;
                                sumLong = sumLong + longitude;
                                sumLati = sumLati + latitude;
                            }
                        }
                    }
                }
            }
            sumLong = sumLong / count;
            sumLati = sumLati / count;

            NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay();
            NMapCircleData circleData = new NMapCircleData(1);

            if( (sumLong != initLong) || (sumLati != initLati) ) { //초기 6점의 위치와 다를 때
                if( i == locationList.size() - 1) { //마지막 번째에 한 번만 그려준다

                    if(centerList.size() == 0){ //리스트에 아무것도 안들어가있을 때
                        circleData.initCircleData();
                        circleData.addCirclePoint(sumLong, sumLati, PixelUtil.getCircleRadius(percent, radius));
                        circleData.endCircleData();

                        pathDataOverlay.addCircleData(circleData);

                        NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());

                        //Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
                        //System.out.printf("%x\n",myRandomNumber);
                        circleStyle.setFillColor(myRandomNumber,0x22);
                        circleStyle.setStrokeColor(myRandomNumber,0xaa);
                        circleData.setCircleStyle(circleStyle);

                        Log.i(LOG_TAG,"11 sumLong = "+sumLong+"sumLati = "+sumLati);

                        centerList.add(new DupCenter(sumLong,sumLati,hash));

                    }else{
                        //dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리

                        for(int j=0; j<centerList.size(); j++){ //있는만큼 무조건 돌려준다, 한 해쉬태그에 대해 6번 먼저 돌고 다음으로 넘어감
                            Log.i(LOG_TAG,"여긴 들어가겟지?"+Math.abs(centerList.get(j).getLati() - sumLati));

                            if(Math.abs(centerList.get(j).getLong() - sumLong) >= 0.005 &&
                                    Math.abs(centerList.get(j).getLati() - sumLati) >= 0.005 &&
                                    !centerList.get(j).getHash().contains(hash)) {

                                Log.i(LOG_TAG,"22 long-sumLong = "+Math.abs(centerList.get(j).getLong() - sumLong)
                                        +"lati-sumLati = "+Math.abs(centerList.get(j).getLati() - sumLati));

                                circleData.initCircleData(); //for문 안에 넣어야되는지 확인

                                circleData.addCirclePoint(sumLong, sumLati, PixelUtil.getCircleRadius(percent, radius));

                                circleData.endCircleData();
                                pathDataOverlay.addCircleData(circleData);

                                NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());

                                //Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
                                //System.out.printf("%x\n",myRandomNumber);
                                circleStyle.setFillColor(myRandomNumber,0x22);
                                circleStyle.setStrokeColor(myRandomNumber,0xaa);
                                circleData.setCircleStyle(circleStyle);

                                centerList.add(new DupCenter(sumLong,sumLati,hash));
                            }
                        }

                        for(int j=0; j<centerList.size(); j++){
                            Log.i(LOG_TAG,"22");
                        }
                    }
                    //출력 원의 개수
                    if(circleData.count() != 0) {
                        isCircle = true;
                    }
                }
                //circleData.setRendered(true);
                //pathDataOverlay.showAllPathData(mMapController.getZoomLevel()); //줌이랑 센터 영향
            }
        }

    }

    /**
     * 지도가 초기화된 후 호출된다.
     * 정상적으로 초기화되면 errorInfo 객체는 null이 전달되며,
     * 초기화 실패 시 errorInfo객체에 에러 원인이 전달된다
     */
    @Override
    public void onMapInitHandler(NMapView mapview, NMapError errorInfo) {

        if (errorInfo == null) { // success
            //새 게시물 작성 후 center 연결
            Intent getIntent = getIntent();
            if(getIntent.getExtras() != null) {
                double lati = getIntent.getExtras().getDouble("CENTER_LATITUDE");
                double longi = getIntent.getExtras().getDouble("CENTER_LONGITUDE");
                mMapController.setMapCenter(
                        new NGeoPoint(longi, lati), 11);
            }
            else {
                mMapController.setMapCenter(
                        new NGeoPoint(127.061, 37.51), 11);
            }
            Log.i(LOG_TAG, "inithandler : zoomlevel = "+mapview.getMapController().getZoomLevel());
            isInit = true;

            //matchData();
            getHashtag(1200F);
        } else { // fail
            android.util.Log.e("NMAP", "onMapInitHandler: error="
                    + errorInfo.toString());
        }
    }

    //인기 해시태그 Pick
    protected void getHashtag(float radius) {
        Point outPoint = null;
        HashMap<String, Integer> count_hashtag = new HashMap<>();
        HashMap<String, Long> recent_hashtag = new HashMap<>();

        int total_text_num = 0;
        int total_sum = 0;
        int popular_index = 0;
        int recent_index = 0;
        double percent;
        double total_percent;
        long duration = 0;

        popular_hash = new String[10];
        num_popular_hash = new int[10];
        recent_hash = new String[10];
        num_recent_hash = new long[10];

        currentDate = new Date(now);                //현재 시간
        current_date = sdf.format(currentDate);

        container.removeAllViews();                     //이전 동적 TextView 삭제
        isCircle = false;

        Log.e("superdroid", "========================================hash============================================");

        for (LocationData data : locationList) {
            NGeoPoint point = new NGeoPoint(data.getLongitude(), data.getLatitude());
            outPoint = mMapView.getMapProjection().toPixels(point, outPoint);
            //Log.e("superdroid", outPoint.toString());

            //해당 게시물의 시간(String >> Date)
            try {
                Date hashDate = sdf.parse(data.getTime());                //해당 게시물의 시간(Date형)

                duration = (currentDate.getTime() - hashDate.getTime()) / 1000 / 60;

                /*if(duration < RECENT_TIME) {
                    Log.e("superdroid", "current(현재 시간) : " + current_date + " / Hashtag : " + data.getHashtag() + ", hashDate(게시물 시간) : " + data.getTime());
                    Log.e("superdroid", "Duration(현재시간-게시물 시간) : " + duration + "분 / currentDate : " + currentDate.getTime() + " / hashDate : " + hashDate.getTime());
                }*/
            }
            catch (ParseException e) {
                e.printStackTrace();
            }

            if (PixelUtil.isScreenInside(outPoint)) { //화면 안에 보이는 경우
                total_text_num++;

                for(String hashtag : data.getSplitHashtag()){

                    //해시태그 개수
                    int count = count_hashtag.containsKey(hashtag) ? count_hashtag.get(hashtag) : 0;
                    count_hashtag.put(hashtag, count + 1);

                    //최근 해시태그
                    if(recent_hashtag.containsKey(hashtag)){
                        if(recent_hashtag.get(hashtag) > duration)
                            recent_hashtag.put(hashtag, duration);
                    }else{
                        recent_hashtag.put(hashtag, duration);
                    }
                }
            }
        }

        count_hashtag = sortByValue_des(count_hashtag);
        recent_hashtag = sortByValue_asc(recent_hashtag);

        //HashMap에서 key가 null값인 데이터 삭제
        count_hashtag.remove("");
        recent_hashtag.remove("");

        /*
        Iterator<String> remove_iterator = count_hashtag.keySet().iterator();
        while (remove_iterator.hasNext()) {
            String key = remove_iterator.next();

            if (key.equals("")) {
                remove_iterator.remove();
            }
        }
        Iterator<String> remove_iterator_recent = recent_hashtag.keySet().iterator();
        while (remove_iterator_recent.hasNext()) {
            String key = remove_iterator_recent.next();

            if (key.equals("")) {
                remove_iterator_recent.remove();
            }
        }
        */

        //전체 HashMap Log에 출력
        if(count_hashtag.size() > 0 ){
            Iterator<String> it = count_hashtag.keySet().iterator();

            while (it.hasNext()) {
                String key = it.next();
                int value = count_hashtag.get(key);
                //전체 hashtag 개수 계산
                total_sum += value;

                //Log.e("superdorid", key + " : " + value);
            }
        }
        if(recent_hashtag.size() > 0){
            Iterator<String> it_recent = recent_hashtag.keySet().iterator();
            while (it_recent.hasNext()) {
                String key = it_recent.next();
                Long value = recent_hashtag.get(key);

                if(recent_index >= 0 && recent_index < 10) {
                    recent_hash[recent_index] = key;
                    num_recent_hash[recent_index] = value;
                    recent_index++;
                }

                //Log.e("superdorid", "(HashMap)RECENT >>>>> key : " + key + " + value : " + value);
            }
        }


        //전체 개수의 70%인 hashtag 개수
        total_percent = total_sum * 0.7;
        double hashtag_percent = total_sum * 0.15;

        //Log.e("superdroid", "탐색 게시물 수 : " + total_text_num);
        //Log.e("superdroid", "Hashtag 개수(total_sum) : " + total_sum + "개 / 전체 Hashtag의 70% : " + total_percent + "개");
        //Log.e("superdroid", "전체 Hashtag의 15% : " + hashtag_percent + "개");

        int sum = 0;
        Iterator<String> seventy_it = count_hashtag.keySet().iterator();
        while (seventy_it.hasNext()) {
            String key = seventy_it.next();
            int value = count_hashtag.get(key);
            sum += value;

            if (sum <= total_percent) {
                //랜덤 색깔(Hashtag별로)
                Random rand = new Random();
                myRandomNumber = rand.nextInt(0xffffff);

                //해당 hashtag가 전체 개수의 15%이상이면 >> 최종 인기 Hashtag
                if (value >= hashtag_percent) {
                    Point searchStartPixel = new Point(0, 0);
                    NGeoPoint searchStart = null;

                    percent = ((double) value / (double) total_sum) * 100; //하나의 해쉬태그가 전체에서 차지하는 비율

                    Log.e("superdorid", "(15%)" + key + " : " + value + "개, " + percent + "%");

                    //Hash Popular로 넘기는 String 배열
                    popular_hash[popular_index] = key;
                    num_popular_hash[popular_index] = value;
                    popular_index++;

                    centerList.clear();

                    for (int i = 0; i <= 1200; i += 1100) {
                        for (int j = 0; j <= 1900; j += 900) {
                            searchStartPixel.set(i, j);
                            searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
                            meanShift(searchStart.longitude, searchStart.latitude, radius, key, percent);
                        }
                    }

                    //출력 원이 1개 이상이면 Hashtag 화면에 출력
                    if(isCircle == true) {
                        printHashtag(key);
                    }
                }
            }
        }
    }

    //메인 지도 하단에 Hashtag 출력
    private void printHashtag(String hash) {
        //ImageView & TextView의 부모 뷰
        LinearLayout linear = new LinearLayout(this);
        LayoutParams lp = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 15;
        linear.setOrientation(LinearLayout.HORIZONTAL);
        linear.setGravity(Gravity.CENTER_VERTICAL);
        linear.setBackgroundResource(R.drawable.linearlayout_hashtag_round);
        linear.setLayoutParams(lp);

        //원 그리기
        ImageView image = new ImageView(this);
        image.setBackgroundResource(R.drawable.imageview_circle);
        //Log.e("superdroid", "Color : " + "#"+String.format("#%06X", (0xFFFFFF & myRandomNumber)) + " / RandomNum : " + myRandomNumber);
        GradientDrawable gd = (GradientDrawable) image.getBackground().getCurrent();
        gd.setColor(Color.parseColor(String.format("#%06X", (0xFFFFFF & myRandomNumber))));

        //Hashtag 출력 textview
        TextView text = new TextView(this);
        text.setText(hash);
        text.setTextColor(Color.BLACK);
        text.setTextSize(20);
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        marginParams.setMargins(15, 0, 0, 0);
        text.setLayoutParams(new LinearLayout.LayoutParams(marginParams));

        //부모 뷰에 추가
        linear.addView(image);
        linear.addView(text);
        container.addView(linear);
    }


    //HashMap sort by Value (Hashtag Map 내림차순 정렬)
    public static HashMap<String, Integer> sortByValue_des(HashMap<String, Integer> hashmap) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(
                hashmap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue() > o2.getValue() ? -1 : o1.getValue() < o2.getValue() ? 1:0;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        });

        HashMap<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    //HashMap sort by Value (Hashtag Map 오름차순 정렬)
    public static HashMap<String, Long> sortByValue_asc(HashMap<String, Long> hashmap) {
        List<Map.Entry<String, Long>> list = new LinkedList<>(
                hashmap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return o1.getValue() < o2.getValue() ? -1 : o1.getValue() > o2.getValue() ? 1:0;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        });

        HashMap<String, Long> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSION_ACCESS_FINE_LOCATION_AND_COARSE_LOCATION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)        //위치 권한 설정 성공
            {
                Log.e("GPS Permission", "Location Permission Success");
                startMyLocation();          //현재 위치로 이동
            }

            else        //위치 권한 설정 실패
            {
                Log.e("GPS Permission", "Location Permission Failed");
            }
            return;
        }

    }

    private void startMyLocation() {            //내 위치로 이동
        mMapLocationManager = new NMapLocationManager(this);
        mMapLocationManager.setOnLocationChangeListener(onMyLocationChangeListener);
        mMapController.setMapCenter(new NGeoPoint(currentLongi, currentLati), mMapController.getZoomLevel());

        boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(true);
        if (!isMyLocationEnabled) {
            Toast.makeText(getApplicationContext(), "위치 권한 설정이 필요합니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopMyLocation() {
        if (mMyLocationOverlay != null) {
            mMapLocationManager.disableMyLocation();

            if (mMapView.isAutoRotateEnabled()) {
                mMyLocationOverlay.setCompassHeadingVisible(false);
                mMapCompassManager.disableCompass();
                mMapView.setAutoRotateEnabled(false, false);
                MapContainer.requestLayout();
            }
        }
    }

    private final NMapLocationManager.OnLocationChangeListener onMyLocationChangeListener = new NMapLocationManager.OnLocationChangeListener() {
        @Override
        public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {       //현재 내 위치 변경 시 지도 이동
			if (mMapController != null) {
				//mMapController.animateTo(myLocation);
				currentLati = myLocation.getLatitude();
				currentLongi = myLocation.getLongitude();
			}
            return true;
        }

        @Override
        public void onLocationUpdateTimeout(NMapLocationManager nMapLocationManager) {
            Toast.makeText(getApplicationContext(), "일시적으로 위치를 불러올 수 없습니다.", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onLocationUnavailableArea(NMapLocationManager nMapLocationManager, NGeoPoint nGeoPoint) {
            Toast.makeText(getApplicationContext(), "현재 위치를 불러올 수 없습니다.", Toast.LENGTH_LONG).show();

            stopMyLocation();
        }
    };


    /**
     * 지도 레벨 변경 시 호출되며 변경된 지도 레벨이 파라미터로 전달된다.
     */
    @Override
    public void onZoomLevelChange(NMapView mapview, int level) {
        if(isInit){

            float radius;

            mapview.getOverlays().clear();

            centerList.clear();

            radius = PixelUtil.getRadiusByZoomLevel(level);

            //matchData();
            getHashtag(radius);

            Log.i(LOG_TAG, "현재 원크기 = "+radius);
            Log.i(LOG_TAG, "zoomLevel = "+level);
            Log.i(LOG_TAG, "Z: center-longitude : " + mapview.getMapController().getMapCenter().longitude);
            Log.i(LOG_TAG, "Z: center-latitude : " + mapview.getMapController().getMapCenter().latitude);
        }
    }

    /**
     * 지도 중심 변경 시 호출되며 변경된 중심 좌표가 파라미터로 전달된다.
     */
    /**
     * @see #onZoomLevelChange(NMapView, int)
     * @param mapview
     * @param center
     */
    @Override
    public void onMapCenterChange(NMapView mapview, NGeoPoint center) {
        onZoomLevelChange(mapview, mMapController.getZoomLevel());
    }

    /**
     * 지도 애니메이션 상태 변경 시 호출된다.
     * animType : ANIMATION_TYPE_PAN or ANIMATION_TYPE_ZOOM
     * animState : ANIMATION_STATE_STARTED or ANIMATION_STATE_FINISHED
     */
    @Override
    public void onAnimationStateChange(
            NMapView arg0, int animType, int animState) {
    }

    @Override
    public void onMapCenterChangeFine(NMapView arg0) {
    }

    public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem, Rect itemBounds) {
        // set your callout overlay
        return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);
    }

    /* POI data State Change Listener*/
    private final NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = new NMapPOIdataOverlay.OnStateChangeListener() {

        public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCalloutClick: title=" + item.getTitle());
            }

            // [[TEMP]] handle a click event of the callout
            //Toast.makeText(MainActivity.this, "onCalloutClick: " + item.getTitle(), Toast.LENGTH_LONG).show();
            Intent intent;
            intent = new Intent(PixelActivity.this, TextActivity.class);
            startActivity(intent);
        }

        public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                if (item != null) {
                    Log.i(LOG_TAG, "onFocusChanged: " + (item != null));
                } else {
                    Log.i(LOG_TAG, "onFocusChanged: ");
                }
            }
        }
    };

    private final NMapOverlayManager.OnCalloutOverlayListener onCalloutOverlayListener = new NMapOverlayManager.OnCalloutOverlayListener() {

        public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem,
                                                         Rect itemBounds) {

            // handle overlapped items
            if (itemOverlay instanceof NMapPOIdataOverlay) {
                NMapPOIdataOverlay poiDataOverlay = (NMapPOIdataOverlay) itemOverlay;

                // check if it is selected by touch event
                if (!poiDataOverlay.isFocusedBySelectItem()) {
                    int countOfOverlappedItems = 1;

                    NMapPOIdata poiData = poiDataOverlay.getPOIdata();
                    for (int i = 0; i < poiData.count(); i++) {
                        NMapPOIitem poiItem = poiData.getPOIitem(i);

                        // skip selected item
                        if (poiItem == overlayItem) {
                            continue;
                        }

                        // check if overlapped or not
                        if (Rect.intersects(poiItem.getBoundsInScreen(), overlayItem.getBoundsInScreen())) {
                            countOfOverlappedItems++;
                        }
                    }

                    if (countOfOverlappedItems > 1) {
                        String text = countOfOverlappedItems + " overlapped items for " + overlayItem.getTitle();
                        Toast.makeText(PixelActivity.this, text, Toast.LENGTH_LONG).show();
                        return null;
                    }
                }
            }

            // use custom old callout overlay
            if (overlayItem instanceof NMapPOIitem) {
                NMapPOIitem poiItem = (NMapPOIitem) overlayItem;

                /*if (poiItem.showRightButton()) {
                    return new NMapCalloutCustomOldOverlay(itemOverlay, overlayItem, itemBounds,
                           mMapViewerResourceProvider);
                }*/
            }

            // use custom callout overlay
            return new NMapCalloutCustomOverlay(itemOverlay, overlayItem, itemBounds, mMapViewerResourceProvider);

            // set basic callout overlay
            // return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);
        }
    };
    /* NMapDataProvider Listener */
    private final OnDataProviderListener onDataProviderListener = new OnDataProviderListener() {

        public void onReverseGeocoderResponse(NMapPlacemark placeMark, NMapError errInfo) {

            //if (DEBUG) {
            Log.i(LOG_TAG, "onReverseGeocoderResponse: placeMark="
                    + ((placeMark != null) ? placeMark.toString() : null));
            //}

            if (errInfo != null) {
                Log.e(LOG_TAG, "Failed to findPlacemarkAtLocation: error=" + errInfo.toString());

                Toast.makeText(PixelActivity.this, errInfo.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            if (mFloatingPOIitem != null && mFloatingPOIdataOverlay != null) {
                mFloatingPOIdataOverlay.deselectFocusedPOIitem();

                if (placeMark != null) {
                    mFloatingPOIitem.setTitle(placeMark.toString());
                }
                mFloatingPOIdataOverlay.selectPOIitemBy(mFloatingPOIitem.getId(), false);
            }
        }
    };

    private final NMapPOIdataOverlay.OnFloatingItemChangeListener onPOIdataFloatingItemChangeListener = new NMapPOIdataOverlay.OnFloatingItemChangeListener() {

        @Override
        public void onPointChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            NGeoPoint point = item.getPoint();
            Point outPoint = null;
            //if (DEBUG) {
            Log.i(LOG_TAG, "onPointChanged: point=" + point.toString());
            //}

            outPoint=mMapView.getMapProjection().toPixels(point,outPoint);
            Log.i(LOG_TAG, "outPoint="+outPoint.toString());

            findPlacemarkAtLocation(point.longitude, point.latitude);

            item.setTitle(null);
        }
    };
}
