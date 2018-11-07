package com.kw.mapit;

public class DupCenter {
    private double longitude;
    private double latitude;
    private String hashtag;

    public DupCenter(double longi, double lati, String hash) {
        longitude = longi;
        latitude = lati;
        hashtag = hash;
    }
    public double getLong(){
        return longitude;
    }
    public double getLati(){
        return latitude;
    }
    public String getHash() {
        return hashtag;
    }
    public void setLong(double longitude){
        this.longitude = longitude;
    }
    public void setLati(double latitude){
        this.latitude = latitude;
    }
    public void setHash(String hash){
        this.hashtag = hash;
    }
}
