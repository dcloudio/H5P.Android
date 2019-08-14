package io.dcloud.feature.weex_amap.adapter.Circle;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.utils.WXViewUtils;

import java.util.ArrayList;

import io.dcloud.feature.weex_amap.adapter.MapAbsMgr;
import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;
import io.dcloud.feature.weex_amap.adapter.WXMapView;

/**
 * 圆 管理
 */
public class CircleMgr extends MapAbsMgr {
    ArrayList<Circle> mCircleCaches;
    public CircleMgr(WXSDKInstance instance, WXMapView map) {
        super(instance, map);
        mCircleCaches = new ArrayList<>();
    }

    public void setCircles(JSONArray circles) {
        if(circles != null) {
            clearCircles();
            for(int i =0; i< circles.size(); i++) {
                JSONObject item = circles.getJSONObject(i);
                CircleOptions cp = createCircleOptions(item);
                if(cp != null) {
                    Circle c = mMap.getMap().addCircle(cp);
                    mCircleCaches.add(c);
                }
            }
        }
    }

    private CircleOptions createCircleOptions(JSONObject item) {
        CircleOptions co = null;
        if(item != null) {
            if(item.containsKey("latitude")) {
                co = new CircleOptions();
                double lat = item.getDouble("latitude");
                double lng = item.getDouble("longitude");
                LatLng c = new LatLng(lat, lng);
                co.center(c);
            } else {
                return co;
            }
            if(item.containsKey("color")) {
                co.strokeColor(MapResourceUtils.getColor(item.getString("color")));
            }
            if(item.containsKey("fillColor")) {
                co.fillColor(MapResourceUtils.getColor(item.getString("fillColor")));
            }
            if(item.containsKey("radius")) {
                float paramFloat = WXViewUtils.getWeexPxByReal(item.getFloatValue("radius"), mInstance.getInstanceViewPortWidth());
                co.radius(paramFloat);
            }
            if(item.containsKey("strokeWidth")) {
                float paramFloat = WXViewUtils.getWeexPxByReal(item.getFloatValue("strokeWidth"), mInstance.getInstanceViewPortWidth());
                co.strokeWidth(paramFloat);
            }
        }
        return co;
    }

    public void clearCircles() {
        if(mCircleCaches != null) {
            for(Circle c: mCircleCaches) {
                if(c != null) {
                    c.remove();
                }
            }
        }
    }
}
