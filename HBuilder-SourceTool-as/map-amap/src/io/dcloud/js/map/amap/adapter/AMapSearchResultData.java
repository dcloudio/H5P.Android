package io.dcloud.js.map.amap.adapter;

public class AMapSearchResultData {

    public Object pStart;
    public String startCity;
    public Object pEnd;
    public String endCity;
    public int type;
    private boolean isError = false;

    public AMapSearchResultData(int type, Object pStart, String startCity, Object pEnd, String endCity) {
        this.type = type;
        this.pStart = pStart;
        this.startCity = startCity;
        this.pEnd = pEnd;
        this.endCity = endCity;
    }

    public void setError(boolean isError){
        this.isError = isError;
    }
}
