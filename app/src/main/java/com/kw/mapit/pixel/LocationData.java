package com.kw.mapit.pixel;

import android.text.TextUtils;
import org.json.JSONObject;

public class LocationData {
    private final String KEY_TEXT_NUM = "text_num";
    private final String KEY_LONGITUDE = "longitude";
    private final String KEY_LATITUDE = "latitude";
    private final String KEY_HASHTAG = "hashtag";
    private final String KEY_TIME = "time";

    private String text_num;
    private double longitude;
    private double latitude;
    private String hashtag;
    private String time;

    public LocationData(JSONObject object){
        if(object != null){
            text_num = object.optString(KEY_TEXT_NUM,"");
            longitude = object.optDouble(KEY_LONGITUDE, .0d);
            latitude = object.optDouble(KEY_LATITUDE,.0d);

            //대 소문자 구분 없이 비교하기 위해 소문자로 일괄 변경
            hashtag = object.optString(KEY_HASHTAG,"").toLowerCase();
            time = object.optString(KEY_TIME,"");
        }
    }

    public String getTextNum(){
        return text_num;
    }

    public double getLongitude(){
        return longitude;
    }

    public double getLatitude(){
        return latitude;
    }

    public String getHashtag(){
        return hashtag;
    }

    public String[] getSplitHashtag(){
        return TextUtils.isEmpty(hashtag)
                ? new String[]{} : hashtag.split(" #");
    }
    public String getTime(){
        return time;
    }
}
