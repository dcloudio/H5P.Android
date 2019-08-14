package io.dcloud.feature.weex_amap.adapter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.LatLng;
import com.taobao.weex.WXSDKInstance;

import java.util.ArrayList;

public class MapAbsMgr {
    protected WXMapView mMap;
    protected WXSDKInstance mInstance;

    public MapAbsMgr(WXSDKInstance instance, WXMapView map) {
        this.mInstance = instance;
        this.mMap = map;
    }
}
