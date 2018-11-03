package com.kw.mapit;

public class DupCenter {
    private double longitude;
    private double latitude;

    public DupCenter(double longi, double lati) {
        longitude = longi;
        latitude = lati;
    }
    public double getLong(){
        return longitude;
    }
    public double getLati(){
        return latitude;
    }
    public void setLong(double longitude){
        this.longitude = longitude;
    }
    public void setLati(double latitude){
        this.latitude = latitude;
    }
}
