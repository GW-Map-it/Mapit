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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Canvas;

import com.kw.mapit.pixel.LocationData;
import com.kw.mapit.pixel.PixelUtil;
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
import java.lang.Math;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.Math.abs;

public class PixelActivity2 extends NMapActivity implements NMapView.OnMapStateChangeListener {
    String myJSON;

    private static final String TAG_RESULTS = "result";
    private static final String TAG_TEXT_NUM = "text_num";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_HASHTAG = "hashtag";
    private static final String TAG_TIME = "time";

    String textNum;
    String longitude;
    String latitude;
    String hashtag;
    String time;

    private int index = 0;
    private double[] longArr = new double[6];
    private double[] latiArr = new double[6];
    private double exLong;
    private double exLati;

    JSONObject jsonObj;
    JSONArray location = null;
    boolean isInit=false;

    int myRandomNumber;          //랜덤 원 색깔
    String pick_hash;               //탐색할 Hashtag

    LinearLayout container;             //Hashtag 출력

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
        setContentView(R.layout.activity_pixel2);

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

        // 지도를 터치할 수 있도록 옵션 활성화
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
    protected   void showLog() {
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0; i<location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);

                Log.i(LOG_TAG, "text_num = "+textNum);
                Log.i(LOG_TAG, "longitude = "+longitude);
                Log.i(LOG_TAG, "latitude = "+latitude);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void parseJson()
    {
        try {
            jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0; i<location.length(); i++) {

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void matchData(){ //데이터를 점에 매칭
        try {
            for(int i=0; i<location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);

                Point outPoint = null;
                NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                outPoint = mMapView.getMapProjection().toPixels(point,outPoint);
                //Log.i(LOG_TAG, "pixel coor= "+outPoint);

                //if문 사용하면 첫 화면 안에 보이는 데이터만 점으로 출력
                //if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700)
                {
                    // set path data points
                    NMapPathData pathData = new NMapPathData(1);

                    //데이터 위치 점 찍어주는 부분
                    pathData.initPathData();
                    pathData.addPathPoint(Float.parseFloat(longitude), Float.parseFloat(latitude), NMapPathLineStyle.TYPE_SOLID);
                    pathData.addPathPoint(Float.parseFloat(longitude) + 0.00001, Float.parseFloat(latitude) + 0.00001, 0);
                    pathData.endPathData();

                    NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
                    pathLineStyle.setLineColor(0xA04DD2, 0xff);
                    pathLineStyle.setFillColor(0xFFFFFF, 0x00);
                    pathData.setPathLineStyle(pathLineStyle);

                    NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);

                    // show all path data
                    pathDataOverlay.showAllPathData(0);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int meanShift_count = 0; //meanShift 무한 루프 방지 횟수 제한
    /**
     * 원의 중심, 원의 반지름, 점의 위치(알고 있음)
     * 중심과 반지름 설정해서 점에서 원의 중심까지의 길이가 원의 반지름에서 원의 중심까지의 길이보다
     * 작으면 안에 포함되어 있는 점이라고 생각
     * */
    protected void meanShift(double initLong, double initLati, float radius, String hash) {
        //종결 조건
        if((abs(initLong - exLong) < 0.00001 && abs(initLati - exLati) < 0.00001) || meanShift_count > 50)
        {
            //원의 중심을 배열에 추가
            if(index < 6)
            {
                longArr[index] = initLong;
                latiArr[index] = initLati;
                Log.e(LOG_TAG, "Location  ||  longArr[" + index + "] = " + longArr[index] + "   latiArr[" + index + "] = " + latiArr[index]);
                index++;
            }
            return;
        }

        try {
            double dataDis;
            int count=0;
            double sumLong = 0;
            double sumLati = 0;
            NGeoPoint circleCenter = new NGeoPoint((initLong),(initLati));
            Point outPoint = null;

            for (int i = 0; i < location.length(); i++) {   //모든 데이터
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);
                hashtag = c.optString(TAG_HASHTAG);

                if(hashtag.contains(" #" + hash)) {         //Pick한 Hashtag가 포함되어 있는 게시물만 연산
                    NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                    outPoint = mMapView.getMapProjection().toPixels(point, outPoint);
                    //Log.i(LOG_TAG, "pixel coor= " + outPoint);

                    if (PixelUtil.isScreenInside(outPoint))  //화면 안에 보이는 경우
                    {
                        dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리

                        if (dataDis < radius) { //원 안에 있으면
                            count++;
                            sumLong += Double.parseDouble(longitude);
                            sumLati += Double.parseDouble(latitude);
                        }
                    }
                }
            }
            meanShift_count++;

            if(count == 0) // to prevent devide by zero problem
            {
                if(exLong == 0 && exLati == 0)  // 초기 위치에서 멈췄을 경우
                {
                    // 화면 중앙에서 탐색 재시작
                    NGeoPoint searchStart = mMapView.getMapProjection().fromPixels(550, 900);
                    meanShift(searchStart.longitude, searchStart.latitude, radius, hash);
                }
                else
                {
                    exLong = initLong;
                    exLati = initLati;
                    meanShift(exLong, exLati, radius, hash);
                }
            }
            else
            {
                exLong = initLong;
                exLati = initLati;
                meanShift(sumLong/count, sumLati/count, radius, hash);
            }
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * 9번 meanShift해 찾은 9개의 중심 중 비슷한 위치의 원들을 하나로 통일해 그리는 함수
     */
    protected void unifyCircles(double percent, float radius)
    {
        Log.i(LOG_TAG, "unifyCircles called");
        double[] distance = new double[8];

        drawCircle(longArr[2], latiArr[2], PixelUtil.getCircleRadius(percent, radius)); //percent로 radius 조절하기

        //중심이 비슷한 위치에 있음을 판단
        for(int i = 0; i < 5; i++) {
            NGeoPoint circleCenter1 = new NGeoPoint(longArr[i], latiArr[i]);
            NGeoPoint circleCenter2 = new NGeoPoint(longArr[i + 1], latiArr[i + 1]);
            distance[i] = NGeoPoint.getDistance(circleCenter1, circleCenter2);

            float dist = mMapView.getMapProjection().metersToPixels(circleCenter2, (float)distance[i]);
            Log.i(LOG_TAG, "Distance = " + dist);

            //비슷한 위치의 원 중 하나의 원만 화면에 출력
            if (dist > 20)
            {
                drawCircle(longArr[i], latiArr[i], 1000f);
            }
        }

        //index 초기화
        index = 0;
    }

    protected void drawCircle(double longitude, double latitude, float radius)
    {
        NMapCircleData circleData = new NMapCircleData(1);

        circleData.initCircleData();
        circleData.addCirclePoint(longitude, latitude, radius); //중심, 반지름 //원생성!!!
        circleData.endCircleData();

        NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());

        //Log.e(LOG_TAG, "random Hax : " + myRandomNumber);
        System.out.printf("%x\n", myRandomNumber);
        circleStyle.setFillColor(myRandomNumber, 0x22);
        circleStyle.setStrokeColor(myRandomNumber, 0xaa);
        circleData.setCircleStyle(circleStyle);

        NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay();
        pathDataOverlay.addCircleData(circleData);
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
            //Log.e(LOG_TAG, "init");
            isInit=true;
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

        /*popular_hash = new String[10];
        num_popular_hash = new int[10];
        recent_hash = new String[10];
        num_recent_hash = new long[10];*/

        /*//시간
        long now = System.currentTimeMillis();
        //현재 시간
        Date currentDate = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String current_date = sdf.format(currentDate);*/

        container.removeAllViews();                     //이전 동적 TextView 삭제


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
                /*try {
                    Date hashDate = sdf.parse(time);                //해당 게시물의 시간(Date형)

                    duration = (currentDate.getTime() - hashDate.getTime()) / 1000 / 60;

                    if(duration < RECENT_TIME) {
                        Log.e("superdroid", "current(현재 시간) : " + current_date + " / Hashtag : " + hashtag + ", hashDate(게시물 시간) : " + time);

                        Log.e("superdroid", "Duration(현재시간-게시물 시간) : " + duration + "분 / currentDate : " + currentDate.getTime() + " / hashDate : " + hashDate.getTime());
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }*/

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

            /*Iterator<String> it_recent = recent_hashtag.keySet().iterator();
            while (it_recent.hasNext()) {
                String key = it_recent.next();
                Long value = recent_hashtag.get(key);

                if(recent_index >= 0 && recent_index < 10) {
                    recent_hash[recent_index] = key;
                    num_recent_hash[recent_index] = value;
                    recent_index++;
                }

                //Log.e("superdorid", "(HashMap)RECENT >>>>> key : " + key + " + value : " + value);
            }*/

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

                        pick_hash = key;

                        //Hash Popular로 넘기는 String 배열
                        /*popular_hash[popular_index] = key;
                        num_popular_hash[popular_index] = value;
                        popular_index++;

                        centerList.clear();
                        centerIndex = 0;*/

                        for (int i = 0; i <= 1100; i += 1100) {
                            for (int j = 0; j <= 1800; j += 900) {
                                exLong = 0;
                                exLati = 0;
                                meanShift_count = 0;

                                searchStartPixel.set(i, j);
                                searchStart = mMapView.getMapProjection().fromPixels(searchStartPixel.x, searchStartPixel.y);
                                meanShift(searchStart.longitude, searchStart.latitude, radius, key);
                            }
                        }
                        unifyCircles(percent, radius);
                        printHashtag(key);
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
            float level_radius = PixelUtil.getRadiusByZoomLevel(mMapController.getZoomLevel());

            Log.i(LOG_TAG, "현재 원크기 = "+level_radius);
            //Log.i(LOG_TAG, "중심에서 실제거리만큼의 픽셀거리 = "+meters);
            Log.i(LOG_TAG, "zoomLevel = "+level);
            Log.i(LOG_TAG, "Z: center-longitude : " + mapview.getMapController().getMapCenter().longitude);
            Log.i(LOG_TAG, "Z: center-latitude : " + mapview.getMapController().getMapCenter().latitude);
        }
    }

    /**
     * 지도 중심 변경 시 호출되며 변경된 중심 좌표가 파라미터로 전달된다.
     */
    @Override
    public void onMapCenterChange(NMapView mapview, NGeoPoint center)
    {
        if(isInit){
            Log.e(LOG_TAG, "onCenterChange called");
            mapview.getOverlays().clear();

            float level_radius = PixelUtil.getRadiusByZoomLevel(mMapController.getZoomLevel());
            getHashtag(level_radius);

            getHashtag(level_radius);
        }
    }

    /**
     * 지도 애니메이션 상태 변경 시 호출된다.
     * animType : ANIMATION_TYPE_PAN or ANIMATION_TYPE_ZOOM
     * animState : ANIMATION_STATE_STARTED or ANIMATION_STATE_FINISHED
     */
    @Override
    public void onAnimationStateChange(NMapView arg0, int animType, int animState)
    {/**
        if(isInit) {
            Log.e(LOG_TAG, "onAnimationStateChange called");
            int level = mMapController.getZoomLevel();
            //mapview.getOverlays().clear();

            NGeoPoint searchStart;

            for(int i=0; i<=1100; i+=1100) {
                for(int j=0; j<=1800; j+=1800) {
                    exLong = 10000;
                    exLati = 10000;

                    searchStart = mMapView.getMapProjection().fromPixels(i, j);
                    meanShift(searchStart.longitude, searchStart.latitude, 1000f);
                }
            }

            unifyCircles();
        }*/
    }

    @Override
    public void onMapCenterChangeFine(NMapView arg0) { // centerChange 아주 조금 할 때
        if(isInit)
            Log.e(LOG_TAG, "CenterChangeFine called");
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
            intent = new Intent(PixelActivity2.this, TextActivity.class);
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
                        Toast.makeText(PixelActivity2.this, text, Toast.LENGTH_LONG).show();
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

                Toast.makeText(PixelActivity2.this, errInfo.toString(), Toast.LENGTH_LONG).show();
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
            protected  void onPostExecute(String result) {
                myJSON = result;
                NGeoPoint searchStart;//지도좌표

                parseJson();
                //matchData();

                long startTime = System.currentTimeMillis();

                //초기 radius 지정
                float level_radius = PixelUtil.getRadiusByZoomLevel(mMapController.getZoomLevel());
                getHashtag(level_radius);

                long endTime = System.currentTimeMillis();
                long Total = endTime - startTime;
                Log.i(LOG_TAG, "Time : "+Total+" (ms) ");
            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);
    }

}
