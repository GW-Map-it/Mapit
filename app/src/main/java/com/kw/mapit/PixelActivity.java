package com.kw.mapit;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
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
import java.util.Collections;
import java.util.Comparator;
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
    ArrayList<dupCenter> centerList = new ArrayList<>(); //겹침원  없게 중심점 모아둘 리스트

    private static final String TAG_RESULTS = "result";
    private static final String TAG_TEXT_NUM = "text_num";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_HASHTAG = "hashtag";

    String textNum;
    String longitude;
    String latitude;
    String hashtag;

    HashMap<String, Integer> count_hashtag;
    String[] popular_hash;

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
        else if(v.getId() == R.id.btn_nextActivity) {
            Intent intent = new Intent(this, HashPopularActivity.class);
            intent.putExtra("POPULAR_HASHTAG", popular_hash);
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
    protected void meanShift(double initLong, double initLati, float radius, String hash) {
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

                NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay();

                NMapCircleData circleData = new NMapCircleData(1);
                if(sumLong != initLong || sumLati != initLati) {
                    if( k == location.length() - 1) {
                        //for(int i=0; i<centerList.size(); i++) {
                          //  if(centerList.get(i).longitude - sumLong > 100 && centerList.get(i).latitude - sumLati > 100 ) { //너무 겹치는 원은 안그릴 것 기준은 임의로 100으로 줌
                                circleData.initCircleData();
                                //circleData.addCirclePoint(sumLong / count, sumLati / count, radius * (count/5)); //중심, 반지름 //원생성!!!
                                //원 크기 데이터의 양에 따라 다르게 해야 함
                                //지금 나오는 원 크기를 가장 데이터가 많을 때의 크기(max)로 잡고 더 작아지게 만들어줄 것
                                if(radius-count > 1){
                                    circleData.addCirclePoint(sumLong / count, sumLati / count, radius-count);
                                }else{
                                    //데이터 양이 원 크기보다 커지는 경우
                                }
                                circleData.endCircleData();
                                pathDataOverlay.addCircleData(circleData);

                                NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());
                                //랜덤 색깔
                                Random rand = new Random();
                                int myRandomNumber = rand.nextInt(0xffffff);

                                Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
                                System.out.printf("%x\n",myRandomNumber);
                                circleStyle.setFillColor(myRandomNumber,0x22);
                                circleStyle.setStrokeColor(myRandomNumber,0xaa);
                                circleData.setCircleStyle(circleStyle);

                                //centerList.add(new dupCenter(sumLong,sumLati));
                           // }
                      //  }
                    }
                    //circleData.setRendered(true);

                    //pathDataOverlay.showAllPathData(mMapController.getZoomLevel()); //줌이랑 센터 영향
                    sumLong = sumLong / count;
                    sumLati = sumLati / count;
                }
            }

            Log.i(LOG_TAG,"마지막 중심좌표! = " + sumLong + " , " + sumLati);

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
    protected void getHashtag(double initLong, double initLati, float radius) {

        Point outPoint = null;
        count_hashtag = new HashMap<>();
        int total_text_num = 0;
        int total_sum = 0;
        double total_percent;
        int popular_index = 0;
        popular_hash = new String[10];

        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            Log.e("superdroid", "========================================hash============================================");

            for (int i = 0; i < location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);
                hashtag = c.optString(TAG_HASHTAG);

                NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                outPoint = mMapView.getMapProjection().toPixels(point, outPoint);
                Log.e("superdroid", outPoint.toString());

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
                                if (count_hashtag.containsKey(split_hashtag[j]) == true) {
                                    count_hashtag.put(split_hashtag[j], count_hashtag.get(split_hashtag[j]) + 1);
                                    break;
                                }
                                //hashtag와 일치하는 key가 없으면 HashMap에 추가
                                else if (count_hashtag.containsKey(split_hashtag[j]) == false && iterator.hasNext() == false) {
                                    count_hashtag.put(split_hashtag[j], 1);
                                    break;
                                }
                            }
                        }
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

            Log.e("superdroid", "탐색 게시물 수 : " + total_text_num);
            Log.e("superdroid", "Hashtag 개수(total_sum) : " + total_sum + "개 / 전체 Hashtag의 70% : " + total_percent + "개");
            Log.e("superdroid", "전체 Hashtag의 15% : " + hashtag_percent + "개");

            int sum = 0;
            Iterator<String> seventy_it = count_hashtag.keySet().iterator();
            while (seventy_it.hasNext()) {
                String key = seventy_it.next();
                int value = count_hashtag.get(key);
                sum += value;

                if(sum <= total_percent) {
                    //해당 hashtag가 전체 개수의 15%이상이면
                    if(value >= hashtag_percent) {
                        Log.e("superdorid", "(15%)" + key + " : " + value);

                        popular_hash[popular_index] = key;
                        popular_index++;
                        //tagList.add(key);
                        meanShift(initLong,initLati,radius, key);
                    }
                }
            }

            Log.e("superdroid", "========================================hash============================================");

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
            Point searchStartPixel = new Point(0,0);
            NGeoPoint searchStart = null;
            int s_level=level;
            float radius=0;
            float meters;

            //mapview.getOverlays().clear();
            /*
            meanShift(mapview.getMapController().getMapCenter().longitude,
                    mapview.getMapController().getMapCenter().latitude, 900f);
            */
            centerList.clear();

            switch(s_level){
                case 1:
                    Log.i(LOG_TAG,"줌을 줄여주세요~><");
                    break;
                case 2:
                    //radius = 710000F;
                    Log.i(LOG_TAG, "줌을 더 줄여주세요~!><");
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
            //6번 meanshift 돌림
            for(int i=0; i<=1100; i+=1100) {
                for(int j=0; j<=1800; j+=900) {
                    searchStartPixel.set(i,j);
                    searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
                    getHashtag(searchStart.longitude, searchStart.latitude, radius);
                }
            }
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
    @Override
    public void onMapCenterChange(NMapView mapview, NGeoPoint center) {
        int level = mMapController.getZoomLevel();

        if(isInit){
            Point searchStartPixel = new Point(0,0);
            NGeoPoint searchStart = null;
            int s_level=mapview.getMapController().getZoomLevel();
            float radius=0;

            //mapview.getOverlays().clear();
            centerList.clear();

            switch(s_level){
                case 1:
                    Log.i(LOG_TAG,"줌을 줄여주세요~><");
                    break;
                case 2:
                    //radius = 710000F;
                    Log.i(LOG_TAG, "줌을 더 줄여주세요~!><");
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
            //6번 meanshift 돌림
            for(int i=0; i<=1100; i+=1100) {
                for(int j=0; j<=1800; j+=900) {
                    searchStartPixel.set(i,j);
                    searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
                    getHashtag(searchStart.longitude, searchStart.latitude, radius);
                }
            }
            Log.i(LOG_TAG, "C: center-longitude : " + String.valueOf(center.longitude));
            Log.i(LOG_TAG, "C: center-latitude : " + String.valueOf(center.latitude));
        }
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
                    Log.i(LOG_TAG, "onFocusChanged: " + item.toString());
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
                BufferedReader bufferedReader = null;
                try {
                    URL url = new URL(uri);

                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String json;
                    while((json = bufferedReader.readLine())!=null){
                        sb.append(json+"\n");
                    }
                    return sb.toString().trim();
                } catch(Exception e) {
                    return null;
                }
            }
            protected  void onPostExecute(String result) {
                myJSON = result;
                NGeoPoint LTPoint = mMapView.getMapProjection().fromPixels(0,0);
                NGeoPoint LMPoint = mMapView.getMapProjection().fromPixels(0,900);
                NGeoPoint LBPoint = mMapView.getMapProjection().fromPixels(0, 1800);

                NGeoPoint RTPoint = mMapView.getMapProjection().fromPixels(1100,0);
                NGeoPoint RMPoint = mMapView.getMapProjection().fromPixels(1100,900);
                NGeoPoint RBPoint = mMapView.getMapProjection().fromPixels(1100,1800);

                long startTime = System.currentTimeMillis();

                matchData();
                getHashtag(LTPoint.longitude, LTPoint.latitude, 1200F);

                long endTime = System.currentTimeMillis();
                long Total = endTime - startTime;
                Log.i(LOG_TAG, "Time : "+Total+" (ms) ");

                getHashtag(LMPoint.longitude,LMPoint.latitude,1200F);
                getHashtag(LBPoint.longitude,LBPoint.latitude,1200F);

                getHashtag(RTPoint.longitude,RTPoint.latitude,1200F);
                getHashtag(RMPoint.longitude,RMPoint.latitude,1200F);
                getHashtag(RBPoint.longitude,RBPoint.latitude,1200F);

//                for(int i=0; i<=1100; i+=1100) {
//                    for(int j=0; j<=1800; j+=900) {
//                        searchStartPixel.set(i,j);
//                        searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
//                        meanShift(searchStart.longitude, searchStart.latitude, radius);
//                    }
//                }
            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);

    }

}
