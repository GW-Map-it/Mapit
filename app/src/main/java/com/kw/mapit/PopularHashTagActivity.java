package com.kw.mapit;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
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
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class PopularHashTagActivity extends NMapActivity implements NMapView.OnMapStateChangeListener {
    String myJSON;

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
        setContentView(R.layout.activity_popular_hash_tag);

        getData("http://"+ getString(R.string.ip) +"/selectLocation.php");

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
    protected void matchData(double initLong, double initLati, float radius, String hash){ //데이터를 점에 매칭
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0; i<location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);
                hashtag = c.optString(TAG_HASHTAG);
                //Log.e("superdroid", "hashtag : " + hashtag);

                //전달받은 특정 hashtag가 있거나 hashtag 전달이 없었을 경우
                //if(hashtag.contains("one")) {

                    //Log.e("superdroid", "===========" + hashtag + "===========");

                    // set path data points
                    NMapPathData pathData = new NMapPathData(9);

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
                //}
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    protected void meanShift(double initLong, double initLati, float radius) {
        //원의 중심, 원의 반지름, 점의 위치(알고 있음)
        //중심과 반지름 설정해서 점에서 원의 중심까지의 길이가 원의 반지름에서 원의 중심까지의 길이보다
        //작으면 안에 포함되어 있는 점이라고 생각

        Point outPoint = null;
        count_hashtag = new HashMap<>();
        int total_sum = 0;
        double total_percent;

        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for (int i = 0; i < location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);
                hashtag = c.optString(TAG_HASHTAG);

                NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                outPoint = mMapView.getMapProjection().toPixels(point, outPoint);
                //Log.e("superdroid", outPoint.toString());

                if (outPoint.x <= 1100 && outPoint.y <= 1800) { //화면 안에 보이는 경우
                    String[] split_hashtag = hashtag.split(" #");       //받아온 hashtag들 " #"로 자르기

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

                Log.e("superdorid", key + " : " + value);
            }

            //전체 개수의 70%인 hashtag 개수
            total_percent = total_sum * 0.7;
            double hashtag_percent = total_sum * 0.15;

            Log.e("superdroid", "total_sum : " + total_sum + "개 / 전체의 70% : " + total_percent + "개");
            Log.e("superdroid", "전체의 15% : " + hashtag_percent + "개");

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
                        matchData(127.0569, 37.5293, 900f, key);
                    }
                }
            }

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    //HashMap sort by Value
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

    /**
     * 지도 레벨 변경 시 호출되며 변경된 지도 레벨이 파라미터로 전달된다.
     */
    @Override
    public void onZoomLevelChange(NMapView mapview, int level) {
        if(isInit){
            mapview.getOverlays().clear();
            /*
            meanShift(mapview.getMapController().getMapCenter().longitude,
                    mapview.getMapController().getMapCenter().latitude, 900f);
            */
            NGeoPoint LTPoint = mMapView.getMapProjection().fromPixels(0,0);
            NGeoPoint LMPoint = mMapView.getMapProjection().fromPixels(0,900);
            NGeoPoint LBPoint = mMapView.getMapProjection().fromPixels(0, 1800);

            NGeoPoint RTPoint = mMapView.getMapProjection().fromPixels(1100,0);
            NGeoPoint RMPoint = mMapView.getMapProjection().fromPixels(1100,900);
            NGeoPoint RBPoint = mMapView.getMapProjection().fromPixels(1100,1800);

            meanShift(LTPoint.longitude,LTPoint.latitude,1000.0F*(15-level));
            meanShift(LMPoint.longitude,LMPoint.latitude,1000f*(15-level));
            meanShift(LBPoint.longitude,LBPoint.latitude,1000f*(15-level));

            meanShift(RTPoint.longitude,RTPoint.latitude,1000f*(15-level));
            meanShift(RMPoint.longitude,RMPoint.latitude,1000f*(15-level));
            meanShift(RBPoint.longitude,RBPoint.latitude,1000f*(15-level));

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
        Log.i(LOG_TAG, "center-longitude : "+String.valueOf(center.longitude));
        Log.i(LOG_TAG, "center-latitude : "+String.valueOf(center.latitude));
        //meanShift(center.longitude, center.latitude, 900f);
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
            intent = new Intent(PopularHashTagActivity.this, TextActivity.class);
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
                        Toast.makeText(PopularHashTagActivity.this, text, Toast.LENGTH_LONG).show();
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

                Toast.makeText(PopularHashTagActivity.this, errInfo.toString(), Toast.LENGTH_LONG).show();
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
                Log.i(LOG_TAG, result);
                myJSON = result;
                //showLog();
                long startTime = System.currentTimeMillis();
                matchData(127.0569, 37.5293, 900f, "");
                meanShift(127.0569, 37.5293, 900f);
                long endTime = System.currentTimeMillis();
                long Total = endTime - startTime;
                Log.i(LOG_TAG, "Time : "+Total+" (ms) ");

            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);
    }

}
