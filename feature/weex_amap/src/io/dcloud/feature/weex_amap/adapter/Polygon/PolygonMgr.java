package io.dcloud.feature.weex_amap.adapter.Polygon;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.utils.WXViewUtils;

import java.util.ArrayList;

import io.dcloud.feature.weex_amap.adapter.MapAbsMgr;
import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;
import io.dcloud.feature.weex_amap.adapter.WXMapView;

public class PolygonMgr extends MapAbsMgr {
    ArrayList<Polygon> mPolygonCaches;
    public PolygonMgr(WXSDKInstance instance, WXMapView map) {
        super(instance, map);
        mPolygonCaches = new ArrayList<>();
    }

    public void setPolygon(JSONArray polygons) {
        if(polygons != null && polygons.size()> 0) {
            clearPolygon();
            for(int i= 0; i<polygons.size(); i++) {
                JSONObject item = polygons.getJSONObject(i);
                PolygonOptions p = createPolygonOptions(item);
                if(p != null) {
                    Polygon polygon = mMap.getMap().addPolygon(p);
                    mPolygonCaches.add(polygon);
                }
            }
        }
    }

    private PolygonOptions createPolygonOptions(JSONObject item) {
        PolygonOptions p = null;
        if(item != null) {

            if(item.containsKey("points")) {
                p = new PolygonOptions();
                JSONArray array = item.getJSONArray("points");
                p.addAll(MapResourceUtils.crateLatLngs(array));
            } else {
                return p;
            }
            if(item.containsKey("strokeWidth")) {
                float paramFloat = WXViewUtils.getRealSubPxByWidth(item.getFloatValue("strokeWidth"), mInstance.getInstanceViewPortWidth());
                p.strokeWidth(paramFloat);
            }
            if(item.containsKey("strokeColor")) {
                p.strokeColor(MapResourceUtils.getColor(item.getString("strokeColor")));
            }
            if(item.containsKey("fillColor")) {
                p.fillColor(MapResourceUtils.getColor(item.getString("fillColor")));
            }
            if(item.containsKey("zIndex")) {
                p.zIndex(item.getFloat("zIndex"));
            }
        }
        return p;
    }

    public void clearPolygon() {
        if(mPolygonCaches != null) {
            for(Polygon p: mPolygonCaches) {
                p.remove();
            }
            mPolygonCaches.clear();
        }
    }
}
