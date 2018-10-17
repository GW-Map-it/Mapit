package com.kw.mapit;

public class dupCenter {
    double longitude;
    double latitude;

    public dupCenter(){

    }
    public dupCenter(double longi, double lati) {
        longitude=longi;
        latitude=lati;
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
