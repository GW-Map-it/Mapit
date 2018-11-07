package com.kw.mapit.pixel;

import android.graphics.Point;
import android.util.Log;

public class PixelUtil {

    public static boolean isScreenInside(Point point){
        //화면 크기 : 1100,1800
        //화면 크기보다 10% 여유 있게 추출
        return point.x <= 1210 && point.x >= -110 && point.y >= -180 && point.y <=1980;
    }
    public static float getCircleRadius(double percent, float radius){

        float ratio;

        if(percent > 95)
            ratio = 1f;
        else if(percent > 85)
            ratio = 0.9f;
        else if(percent > 75)
            ratio = 0.8f;
        else if(percent > 65)
            ratio = 0.7f;
        else if(percent > 55)
            ratio = 0.6f;
        else if(percent > 45)
            ratio = 0.5f;
        else if(percent > 35)
            ratio = 0.4f;
        else if(percent > 25)
            ratio = 0.3f;
        else if(percent > 15)
            ratio = 0.2f;
        else
            ratio = 0.0f;

        return radius * ratio;
    }

    public static float getRadiusByZoomLevel(int zoomLevel){
        switch(zoomLevel){
            case 1:
                return 0f;
            case 2:
                return 0f;
            case 3:
                return 300_000F;
            case 4:
                return 150_000F;
            case 5:
                return 70_000F;
            case 6:
                return 38_000F;
            case 7:
                return 18_000F;
            case 8:
                return 9_000F;
            case 9:
                return 4_500F;
            case 10:
                return 2_500F;
            case 11:
                return 1_200F;
            case 12:
                return 600F;
            case 13:
                return 300F;
            case 14:
                return 200F;
            default:
                return -1f;
        }
    }
}
