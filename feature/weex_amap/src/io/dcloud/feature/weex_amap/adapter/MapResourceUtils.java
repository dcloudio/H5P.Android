package io.dcloud.feature.weex_amap.adapter;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.taobao.weex.utils.WXResourceUtils;

import java.util.ArrayList;

public class MapResourceUtils {

    public static int getColor(String strColor) {
        if(!TextUtils.isEmpty(strColor)) {
            int length = strColor.length();
            if(length == 9 && strColor.startsWith("#")) {
                strColor = "#"+strColor.substring(7, 9) + strColor.substring(1, 7);
            }
        }
        return WXResourceUtils.getColor(strColor);
    }

    public static ArrayList<LatLng> crateLatLngs(JSONArray s) {
        ArrayList<LatLng> m = new ArrayList<>();
        for(int i = 0; i<s.size(); i++) {
            JSONObject j = s.getJSONObject(i);
            LatLng l = crateLatLng(j);
            if(l != null) {
                m.add(l);
            }
        }
        return m;
    }

    public static LatLng crateLatLng(JSONObject item) {
        if(item != null && item.containsKey("latitude") && item.containsKey("longitude")) {
            try {
                return new LatLng(item.getDouble("latitude"), item.getDouble("longitude"));
            } catch (Exception e){

            }
        }
        return null;
    }

    public static LatLonPoint createLatLonPoint(JSONObject item) {
        if(item != null && item.containsKey("latitude") && item.containsKey("longitude")) {
            try {
                return new LatLonPoint(item.getDouble("latitude"), item.getDouble("longitude"));
            }catch (Exception e){

            }
        }
        return null;
    }

    public static LatLng crateLatLng(Object latitude, Object longitude) {
        if(latitude != null && longitude != null) {
            try {
                double lat = Double.valueOf(latitude.toString());
                double lng = Double.valueOf(longitude.toString());
                return new LatLng(lat, lng);
            } catch (Exception e){

            }
        }
        return null;
    }
}
