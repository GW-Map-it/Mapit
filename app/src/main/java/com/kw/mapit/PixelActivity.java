﻿package com.kw.mapit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

    //인기/최신 해시태그 액티비티에 넘어갈 배열
    String[] popular_hash;
    int[] num_popular_hash;
    String[] recent_hash;
    int[] num_recent_hash;

    int myRandomNumber;

    boolean isInit=false;

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
            startActivity(intent);
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
    protected void meanShift(double initLong, double initLati, float radius, String hash, double percent) {
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

                    if(hashtag.contains(hash)) {

                        NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                        outPoint = mMapView.getMapProjection().toPixels(point, outPoint);

                        if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700) { //화면 안에 보이는 경우
                            circleCenter = new NGeoPoint((sumLong / count), (sumLati / count));
                            dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리

                            if (dataDis < radius) { //원 안에 있으면
                                count++;
                                sumLong = sumLong + Double.parseDouble(longitude);
                                sumLati = sumLati + Double.parseDouble(latitude);
                            }
                        }
                    }
                }
                /* 여기로 옮겨도 로직에 문제없을까 */
                sumLong = sumLong / count;
                sumLati = sumLati / count;

                NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay();

                NMapCircleData circleData = new NMapCircleData(1);
                if( (sumLong != initLong) || (sumLati != initLati) ) {
                    if( k == location.length() - 1) {
                        Log.i(LOG_TAG,"centerList size : "+String.valueOf(centerList.size()));

                        if(centerList.size() == 0){ //사이즈 0일 때
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

                            //centerList.add(centerIndex,new DupCenter(sumLong,sumLati));
                            //centerIndex++;

                        } /*else { //사이즈 여러 개 일 때

                            for(int i=0; i<centerList.size(); i++) {
                                //Log.i(LOG_TAG, "centerList.get.hashtag = "+centerList.get(i).hashtag+", hash = "+hash);
                                //Log.i(LOG_TAG,"centerList.get.longitude-sumLong = "+String.valueOf(Math.abs(center.longitude - sumLong)));
                                //if(centerList.get(i).hashtag.contains(hash)){

                                    if(Math.abs(centerList.get(i).longitude - sumLong) > 0.001 && Math.abs(centerList.get(i).latitude - sumLati) > 0.001 ) { //너무 겹치는 원은 안그릴 것
                                        circleData.initCircleData();
                                        //circleData.addCirclePoint(sumLong, sumLati, radius); //중심, 반지름 //원생성!!!

           //                            원 크기 데이터의 양에 따라 다르게 해야 함
           //                            지금 나오는 원 크기를 가장 데이터가 많을 때의 크기(max)로 잡고 더 작아지게 만들어줄 것
           //                            데이터 양 퍼센테이지. 어차피 15%까지는 나오지 않는다. 15~25, 25~35, 35~45, 45~55, 55~65, 75~85, 85~95. 95~100 총 8개로 구간 나눠서 반지름 정해준다
                                        Log.i(LOG_TAG,"몇번들어오니"); //들어올 때 그리는거지... 들어올때 그리는거

                                        if(percent>15 && percent<=25){
                                            circleData.addCirclePoint(sumLong , sumLati , radius*0.3f);
                                        }else if(percent>25 && percent<=35){
                                            circleData.addCirclePoint(sumLong , sumLati , radius*0.4f);
                                        }else if(percent>35 && percent<=45){
                                            circleData.addCirclePoint(sumLong , sumLati ,radius*0.5f);
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
                                        //랜덤 색깔
                                        Random rand = new Random();
                                        int myRandomNumber = rand.nextInt(0xffffff);

                                        //Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
                                        //System.out.printf("%x\n",myRandomNumber);
                                        circleStyle.setFillColor(myRandomNumber,0x22);
                                        circleStyle.setStrokeColor(myRandomNumber,0xaa);
                                        circleData.setCircleStyle(circleStyle);
                                    }
                                //}
                            }
                            centerList.add(centerIndex,new DupCenter(sumLong,sumLati));
                            centerIndex++;
                        }*/
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
        int total_text_num = 0;
        int total_sum = 0;
        int popular_index = 0;
        double percent;
        double total_percent;

        popular_hash = new String[10];
        num_popular_hash = new int[10];
        recent_hash = new String[10];
        num_recent_hash = new int[10];

        //시간
        long now = System.currentTimeMillis();
        //현재 시간
        Date currentDate = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String current_date = sdf.format(currentDate);


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

                if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700) { //화면 안에 보이는 경우
                    String[] split_hashtag = hashtag.split(" #");       //받아온 hashtag들 " #"로 자르기
                    total_text_num++;

                    //잘라진 hashtag들을 HashMap에 저장

                    for(int j=0;j<split_hashtag.length;j++) {
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
                    }

                    //해당 게시물의 시간(String >> Date)
                    try {
                        Date hashDate = sdf.parse(time);                //해당 게시물의 시간(Date형)

                        long duration = (currentDate.getTime() - hashDate.getTime()) / 1000 / 60 / 60;

                        if(duration < RECENT_TIME) {
                            Log.e("superdroid", "current(현재 시간) : " + current_date + " / Hashtag : " + hashtag + ", hashDate(게시물 시간) : " + time);

                            Log.e("superdroid", "Duration(현재시간-게시물 시간) : " + duration + "시간 / currentDate : " + currentDate.getTime() + " / hashDate : " + hashDate.getTime());
                        }
                    }
                    catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                
            }

            count_hashtag = sortByValue(count_hashtag);

            //HashMap에서 key가 null값인 데이터 삭제
            Iterator<String> remove_iterator = count_hashtag.keySet().iterator();
            while (remove_iterator.hasNext()) {
                String key = remove_iterator.next();

                if (key.equals("")) {
                    remove_iterator.remove();
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

                if(sum <= total_percent) {
                    //랜덤 색깔(Hashtag별로)
                    Random rand = new Random();
                    myRandomNumber = rand.nextInt(0xffffff);

                    //해당 hashtag가 전체 개수의 15%이상이면 >> 최종 인기 Hashtag
                    if(value >= hashtag_percent) {
                        Point searchStartPixel = new Point(0,0);
                        NGeoPoint searchStart = null;

                        percent = ((double)value / (double)total_sum) * 100; //하나의 해쉬태그가 전체에서 차지하는 비율

                        Log.e("superdorid", "(15%)" + key + " : " + value + "개, " + percent + "%");
                        Log.i(LOG_TAG, "percent="+percent);

                        //Hash Popular로 넘기는 String 배열
                        popular_hash[popular_index] = key;
                        num_popular_hash[popular_index] = value;
                        popular_index++;

                        for(int i=0; i<=1100; i+=1100) {
                            for(int j=0; j<=1800; j+=900) {
                                searchStartPixel.set(i,j);
                                searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
                                meanShift(searchStart.longitude, searchStart.latitude, radius, key, percent);
                            }
                        }
                    }
                }
            }

            //Log.e("superdroid", "========================================hash============================================");

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    //HashMap sort by Value (Hashtag Map 정렬)
    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hashmap) {
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

                matchData();
                getHashtag(1200F); //초기 줌레벨 11이기 때문
            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);

    }

}
