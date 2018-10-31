package com.kw.mapit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapController;
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

public class PixelActivity extends NMapActivity implements NMapView.OnMapStateChangeListener {
    String myJSON;

    //ArrayList<double> centerList = new ArrayList<double>();
    ArrayList<DupCenter> centerList = new ArrayList<>(); //겹침원 없게 중심점 모아둘 리스트
    int centerIndex = 0;

    private static final String TAG_RESULTS = "result";
    private static final String TAG_TEXT_NUM = "text_num";
    private static final String TAG_TIME = "time";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_HASHTAG = "hashtag";

    private static final int RECENT_TIME = 24;              //최근 24시간 내

    private String textNum;
    private String time;
    private String longitude;
    private String latitude;
    private String hashtag;

    HashMap<String, Integer> count_hashtag;
    HashMap<String, Long> recent_hashtag;

    //인기/최신 해시태그 액티비티에 넘어갈 배열
    String[] popular_hash;
    int[] num_popular_hash;
    String[] recent_hash;
    long[] num_recent_hash;

    int myRandomNumber;

    LinearLayout container;

    boolean isInit=false;
    boolean isCircle = false;           //원이 그려졌는가

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pixel);

        String serverURL = "http://" + getString(R.string.ip) + "/selectLocation.php";
        getData(serverURL);

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
        int marker1 = NMapPOIflagType.PIN;

        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(1, mMapViewerResourceProvider);

        poiData.beginPOIdata(1);
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
    }

    public void onClick(View v) {
        //"+"버튼 클릭 시 게시글 작성 액티비티로 이동
        if(v.getId() == R.id.btn_addText) {
            Intent intent = new Intent(this, DbConnectActivity.class);
            startActivity(intent);
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
        try {
            JSONObject jsonObj = new JSONObject(myJSON);

            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0; i<location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);

                // set path data points
                NMapPathData pathData = new NMapPathData(1);

                //데이터 위치 점 찍어주는 부분
                pathData.initPathData();
                pathData.addPathPoint(Float.parseFloat(longitude), Float.parseFloat(latitude), NMapPathLineStyle.TYPE_SOLID);
                pathData.addPathPoint(Float.parseFloat(longitude)+0.00001, Float.parseFloat(latitude)+0.00001, 0);
                pathData.endPathData();

                NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
                pathLineStyle.setLineColor(0xA04DD2, 0xff);
                pathLineStyle.setFillColor(0xFFFFFF,0x00);
                pathData.setPathLineStyle(pathLineStyle);

                NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);

                // show all path data
                // pathDataOverlay.showAllPathData(mMapController.getZoomLevel());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 원과 점 사이의 거리로 원 안의 포함여부 계산한다
     * meanshift로 원 위치 계속 옮기고 마지막 한 번만 그리는 알고리즘
     */
    protected void meanShift(double initLong, double initLati, float radius, double percent) {
        double dataDis;
        double sumLong = initLong;         //원 안에 속한 점이면 계속 더해줄 위도
        double sumLati = initLati;         //원 안에 속한 점이면 계속 더해줄 경도
        int count;                         //원 한 번 계산해줄때마다 sumLong과 sumLati 나눠줄 count
        NGeoPoint circleCenter;
        Point outPoint = null;

        try {
            /* 제이슨 받아오는 부분 matchData 함수랑 겹치는 곳 나중에 빼줄 것
               일단 혹시 몰라서 놔둠 */

            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int k = 0; k < location.length(); k++) {
                count=1;
                for (int i = 0; i < location.length(); i++) {
                    JSONObject c = location.getJSONObject(i);
                    textNum = c.optString(TAG_TEXT_NUM);
                    longitude = c.optString(TAG_LONGITUDE);
                    latitude = c.optString(TAG_LATITUDE);
                    hashtag = c.optString(TAG_HASHTAG);

                    NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                    outPoint = mMapView.getMapProjection().toPixels(point, outPoint);

                    if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700) { //화면 안에 보이는 경우
                        circleCenter = new NGeoPoint((sumLong / count), (sumLati / count));
                        dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리

                        if (dataDis < radius) { //원 안에 있으면
                            count++;
                            sumLong = sumLong + Double.parseDouble(longitude);
                            sumLati = sumLati + Double.parseDouble(latitude); }
                    }
                }
                sumLong = sumLong / count;
                sumLati = sumLati / count;

                NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay();

                NMapCircleData circleData = new NMapCircleData(1);
                if( (sumLong != initLong) || (sumLati != initLati) ) {
                    if( k == location.length() - 1) {

                        if(centerList.size()==0){
                            circleData.initCircleData();

                            if(percent>15 && percent<=25){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.3f);
                            }else if(percent>25 && percent<=35){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.4f);
                            }else if(percent>35 && percent<=45){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.5f);
                            }else if(percent>45 && percent<=55){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.6f);
                            }else if(percent>55 && percent<=65){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.65f);
                            }else if(percent>65 && percent<=75){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.7f);
                            }else if(percent>75 && percent<=85){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.8f);
                            }else if(percent>85 && percent<=95){
                                circleData.addCirclePoint(sumLong , sumLati , radius*0.9f);
                            }else if(percent>95 && percent<=100){
                                circleData.addCirclePoint(sumLong , sumLati , radius*1f);
                            }
                            circleData.endCircleData();
                            pathDataOverlay.addCircleData(circleData);

                            NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());

                            //Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
                            //System.out.printf("%x\n",myRandomNumber);
                            circleStyle.setFillColor(myRandomNumber,0x22);
                            circleStyle.setStrokeColor(myRandomNumber,0xaa);
                            circleData.setCircleStyle(circleStyle);

                            centerList.add(centerIndex,new DupCenter(sumLong,sumLati));
                            centerIndex++;
                        }else{
                            circleData.initCircleData(); //for문 안에 넣어야되는지 확인

                            for(int i=0; i<centerList.size(); i++){ //있는만큼 무조건 돌려준다, 한 해쉬태그에 대해 6번 먼저 돌고 다음으로 넘어감
                                if(Math.abs(centerList.get(i).longitude - sumLong) > 0.05 &&
                                        Math.abs(centerList.get(i).latitude - sumLati) > 0.05) {
                                    //수정. 처음껀 들어가고 그 다음부터 겹치는 게 안들어가야 하는 것

                                    Log.i(LOG_TAG,"long-sumLong = "+Math.abs(centerList.get(i).longitude - sumLong)
                                            +"lati-sumLati = "+Math.abs(centerList.get(i).latitude - sumLati));

                                    if(percent>15 && percent<=25){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.3f);
                                    }else if(percent>25 && percent<=35){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.4f);
                                    }else if(percent>35 && percent<=45){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.5f);
                                    }else if(percent>45 && percent<=55){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.6f);
                                    }else if(percent>55 && percent<=65){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.65f);
                                    }else if(percent>65 && percent<=75){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.7f);
                                    }else if(percent>75 && percent<=85){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.8f);
                                    }else if(percent>85 && percent<=95){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*0.9f);
                                    }else if(percent>95 && percent<=100){
                                        circleData.addCirclePoint(sumLong , sumLati , radius*1f);
                                    }
                                }
                            }
                            circleData.endCircleData();
                            pathDataOverlay.addCircleData(circleData);

                            NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());

                            //Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
                            //System.out.printf("%x\n",myRandomNumber);
                            circleStyle.setFillColor(myRandomNumber,0x22);
                            circleStyle.setStrokeColor(myRandomNumber,0xaa);
                            circleData.setCircleStyle(circleStyle);

                            centerList.add(centerIndex,new DupCenter(sumLong,sumLati));
                            centerIndex++;
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

            //Log.i(LOG_TAG,"마지막 중심좌표! = " + sumLong + " , " + sumLati);

        } catch (JSONException e){
            e.printStackTrace();
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
            mMapController.setMapCenter(
                    new NGeoPoint(127.061, 37.51), 11);
            Log.i(LOG_TAG, "inithandler : zoomlevel = "+mapview.getMapController().getZoomLevel());
            isInit=true;
        } else { // fail
            android.util.Log.e("NMAP", "onMapInitHandler: error="
                    + errorInfo.toString());
        }
    }

    //인기 해시태그 Pick
    protected void getHashtag(float radius) {
        Point outPoint = null;
        count_hashtag = new HashMap<>();
        recent_hashtag = new HashMap<>();
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

        //시간
        long now = System.currentTimeMillis();
        //현재 시간
        Date currentDate = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String current_date = sdf.format(currentDate);

        container.removeAllViews();                     //이전 동적 TextView 삭제
        isCircle = false;


        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            Log.e("superdroid", "========================================hash============================================");

            for (int i = 0; i < location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                time = c.optString(TAG_TIME);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);
                hashtag = c.optString(TAG_HASHTAG);

                NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                outPoint = mMapView.getMapProjection().toPixels(point, outPoint);
                //Log.e("superdroid", outPoint.toString());

                //해당 게시물의 시간(String >> Date)
                try {
                    Date hashDate = sdf.parse(time);                //해당 게시물의 시간(Date형)

                    duration = (currentDate.getTime() - hashDate.getTime()) / 1000 / 60;

                    if(duration < RECENT_TIME) {
                        Log.e("superdroid", "current(현재 시간) : " + current_date + " / Hashtag : " + hashtag + ", hashDate(게시물 시간) : " + time);

                        Log.e("superdroid", "Duration(현재시간-게시물 시간) : " + duration + "분 / currentDate : " + currentDate.getTime() + " / hashDate : " + hashDate.getTime());
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

                if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700) { //화면 안에 보이는 경우
                    String[] split_hashtag = hashtag.split(" #");       //받아온 hashtag들 " #"로 자르기
                    total_text_num++;

                    //잘라진 hashtag들을 HashMap에 저장

                    for(int j=0;j<split_hashtag.length;j++) {
                        //해시태그 개수 Hashmap
                        Iterator<String> iterator = count_hashtag.keySet().iterator();
                        if(count_hashtag.size() == 0) {
                            count_hashtag.put(split_hashtag[j], 1);
                        }
                        else {
                            while (iterator.hasNext()) {
                                String key = iterator.next();
                                int value = count_hashtag.get(key);
                                //hashtag와 일치하는 key가 있으면 value+1
                                if (count_hashtag.containsKey(split_hashtag[j])) {
                                    count_hashtag.put(split_hashtag[j], count_hashtag.get(split_hashtag[j]) + 1);
                                    break;
                                }
                                //hashtag와 일치하는 key가 없으면 HashMap에 추가
                                else if (!count_hashtag.containsKey(split_hashtag[j]) && !iterator.hasNext()) {
                                    count_hashtag.put(split_hashtag[j], 1);
                                    break;
                                }
                            }
                        }

                        //최근 해시태그 Hashmap
                        Iterator<String> rec_iterator = recent_hashtag.keySet().iterator();
                        if(recent_hashtag.size() == 0) {
                            recent_hashtag.put(split_hashtag[j], duration);
                        }
                        else {
                            while (iterator.hasNext()) {
                                String key = rec_iterator.next();
                                Long value = recent_hashtag.get(key);
                                //hashtag와 일치하는 key가 있으면
                                if (recent_hashtag.containsKey(split_hashtag[j])) {
                                    if(recent_hashtag.get(split_hashtag[j]) > duration) {           //저장된 시간 >  새로운 시간이면 새로운 시간으로 저장
                                        recent_hashtag.put(split_hashtag[j], duration);
                                    }
                                    break;
                                }
                                //hashtag와 일치하는 key가 없으면 HashMap에 추가
                                else if (!recent_hashtag.containsKey(split_hashtag[j]) && !rec_iterator.hasNext()) {
                                    recent_hashtag.put(split_hashtag[j], duration);
                                    break;
                                }
                            }
                        }
                    }

                }

            }

            count_hashtag = sortByValue_des(count_hashtag);
            recent_hashtag = sortByValue_asc(recent_hashtag);

            //HashMap에서 key가 null값인 데이터 삭제
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

            //전체 HashMap Log에 출력
            Iterator<String> it = count_hashtag.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                int value = count_hashtag.get(key);
                //전체 hashtag 개수 계산
                total_sum += value;

                //Log.e("superdorid", key + " : " + value);
            }

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
                        Log.i(LOG_TAG, "percent=" + percent);

                        //Hash Popular로 넘기는 String 배열
                        popular_hash[popular_index] = key;
                        num_popular_hash[popular_index] = value;
                        popular_index++;

                        centerList.clear();
                        centerIndex = 0;

                        for (int i = 0; i <= 1100; i += 1100) {
                            for (int j = 0; j <= 1800; j += 900) {
                                searchStartPixel.set(i, j);
                                searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
                                meanShift(searchStart.longitude, searchStart.latitude, radius, percent);
                            }
                        }
                        //출력 원이 1개 이상이면 Hashtag 화면에 출력
                        if(isCircle == true) {
                            printHashtag(key);
                        }
                    }
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
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


    /**
     * 지도 레벨 변경 시 호출되며 변경된 지도 레벨이 파라미터로 전달된다.
     */
    @Override
    public void onZoomLevelChange(NMapView mapview, int level) {
        if(isInit){

            float radius=0;
            float meters;

            mapview.getOverlays().clear();

            centerList.clear();
            centerIndex = 0;

            switch(level){
                case 1:
                    Log.i(LOG_TAG,"줌을 줄여주세요~><");
                    break;
                case 2:
                    //radius = 710000F;
                    Log.i(LOG_TAG, "줌을 더 줄여주세요~!>_<");
                    break;
                case 3:
                    radius = 300000F;
                    break;
                case 4:
                    radius = 150000F;
                    break;
                case 5:
                    radius = 70000F;
                    break;
                case 6:
                    radius = 38000F;
                    break;
                case 7:
                    radius = 18000F;
                    break;
                case 8:
                    radius = 9000F;
                    break;
                case 9:
                    radius = 4500F;
                    break;
                case 10:
                    radius = 2500F;
                    break;
                case 11:
                    radius = 1200F;
                    break;
                case 12:
                    radius = 600F;
                    break;
                case 13:
                    radius = 300F;
                    break;
                case 14:
                    radius = 200F;
                    break;
            }

            getHashtag(radius);

            meters = mMapView.getMapProjection().metersToPixels(radius);

            Log.i(LOG_TAG, "현재 원크기 = "+radius);
            Log.i(LOG_TAG, "중심에서 실제거리만큼의 픽셀거리 = "+meters);
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
    public void getData(String url) {
        class getDataJSON extends AsyncTask<String, Integer, String> {
            @Override
            protected String doInBackground(String... urls) {
                String uri = urls[0];

                try {
                    URL url = new URL(uri);

                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String json;
                    while( (json = bufferedReader.readLine()) != null){
                        sb.append(json+"\n");
                    }
                    return sb.toString().trim();
                } catch(Exception e) {
                    return null;
                }
            }
            protected  void onPostExecute(String result) {
                myJSON = result;

                /* 시간재는 부분
                long startTime = System.currentTimeMillis();
                long endTime = System.currentTimeMillis();
                long Total = endTime - startTime;
                Log.i(LOG_TAG, "Time : "+Total+" (ms) ");
                */

                mMapController.setMapCenter(new NGeoPoint(127.061, 37.51), 11);
                matchData();
                getHashtag(1200F); //초기 줌레벨 11이기 때문
            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);

    }

}
