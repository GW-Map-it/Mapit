package com.kw.mapit;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.graphics.Canvas;

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
import java.util.Random;

import static java.lang.Math.abs;

public class PixelActivity2 extends NMapActivity implements NMapView.OnMapStateChangeListener {
    String myJSON;

    private static final String TAG_RESULTS = "result";
    private static final String TAG_TEXT_NUM = "text_num";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";

    String textNum;
    String longitude;
    String latitude;

    private int index = 0;
    private double[] longArr = new double[9];
    private double[] latiArr = new double[9];
    private double exLong;
    private double exLati;

    JSONObject jsonObj;
    JSONArray location = null;
    boolean isInit=false;

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
            jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

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

    /**
     * 원의 중심, 원의 반지름, 점의 위치(알고 있음)
     * 중심과 반지름 설정해서 점에서 원의 중심까지의 길이가 원의 반지름에서 원의 중심까지의 길이보다
     * 작으면 안에 포함되어 있는 점이라고 생각
     * */
    protected void meanShift(double initLong, double initLati, float radius) {
        //종결 조건
        if(abs(initLong - exLong) < 0.0000001 && abs(initLati - exLati) < 0.0000001)
        {
            Log.i(LOG_TAG, "initLong = " + initLong + ", exLong = " + exLong + ", initLati = " + initLati + ", exLong = " + exLong);
            //원의 중심을 배열에 추가
            if(index < 9)
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

            jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for (int i = 0; i < location.length(); i++) {   //모든 데이터
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);

                NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                outPoint = mMapView.getMapProjection().toPixels(point,outPoint);
                //Log.i(LOG_TAG, "pixel coor= " + outPoint);

                if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700)  //화면 안에 보이는 경우
                {
                    dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리
                    //Log.i(LOG_TAG,"dataDis = "+dataDis);

                    if(dataDis < radius) { //원 안에 있으면
                        count++;
                        sumLong += Double.parseDouble(longitude);
                        sumLati += Double.parseDouble(latitude);
                    }
                }
                exLong = initLong;
                exLati = initLati;
            }
            meanShift(sumLong/count, sumLati/count, radius);
        }
        catch (JSONException e){
            e.printStackTrace();
        }

    }

    /**
     * 9번 meanShift해 찾은 9개의 중심 중 비슷한 위치의 원들을 하나로 통일해 그리는 함수
     * (다 그린 후에는 원의 중심을 저장하는 배열의 index를 0으로 초기화)
     */
    protected void unifyCircles()
    {
        Log.i(LOG_TAG, "unifyCircles called");
        double[] distance = new double[8];

        drawCircle(longArr[4], latiArr[4], 1000f);

        //중심이 비슷한 위치에 있음을 판단
        for(int i = 0; i < 8; i++) {
            NGeoPoint circleCenter1 = new NGeoPoint(longArr[i], latiArr[i]);
            NGeoPoint circleCenter2 = new NGeoPoint(longArr[i + 1], latiArr[i + 1]);
            distance[i] = NGeoPoint.getDistance(circleCenter1, circleCenter2);
            float dist = mMapView.getMapProjection().metersToPixels(circleCenter2, (float)distance[i]);
            Log.i(LOG_TAG, "Distance = " + dist);

            //비슷한 위치의 원 중 하나의 원만 화면에 출력
            if (dist > 20)    // meanShift가 제대로 돌아가면 한 20으로 줄여야 될듯.
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

        //random circle color
        Random rand = new Random();
        int myRandomNumber = rand.nextInt(0xffffff);

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

    /**
     * 지도 레벨 변경 시 호출되며 변경된 지도 레벨이 파라미터로 전달된다.
     */
    @Override
    public void onZoomLevelChange(NMapView mapview, int level) {
        /*if(isInit){
            mapview.getOverlays().clear();

            NGeoPoint searchStart;
            matchData();

            for(int i=0; i<=1100; i+=550) {
                for(int j=0; j<=1800; j+=900) {
                    searchStart = mMapView.getMapProjection().fromPixels(550, 900);
                    meanShift(searchStart.longitude, searchStart.latitude, 1000f*(1/level));
                }
            }
        }*/

        if(isInit) {
            Log.e(LOG_TAG, "onZoomLevelChange called");
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
            int level = mMapController.getZoomLevel();
            //mapview.getOverlays().clear();

            NGeoPoint searchStart;

            for(int i=0; i<=1100; i+=550) {
                for(int j=0; j<=1800; j+=900) {
                    exLong = 10000;
                    exLati = 10000;

                    searchStart = mMapView.getMapProjection().fromPixels(i, j);
                    meanShift(searchStart.longitude, searchStart.latitude, 1000f);
                }
            }

            unifyCircles();
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
        //Log.e(LOG_TAG, "onAnimationStateChange called - not init");
        if(isInit)
            Log.e(LOG_TAG, "onAnimationStateChange called");
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

                matchData();
                //parseJson();
                long startTime = System.currentTimeMillis();

                for(int i=0; i<=1100; i+=550) {
                    for(int j=0; j<=1800; j+=900) {
                        //초기 위치를 아주 크게 해서 meanShift의 처음 if에서 걸리지 않게 함
                        exLong = 10000;
                        exLati = 10000;

                        searchStart = mMapView.getMapProjection().fromPixels(i, j);
                        meanShift(searchStart.longitude, searchStart.latitude, 1000f);
                    }
                }
                unifyCircles();

                long endTime = System.currentTimeMillis();
                long Total = endTime - startTime;
                Log.i(LOG_TAG, "Time : "+Total+" (ms) ");
            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);
    }

}
