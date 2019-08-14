package io.dcloud.feature.weex_amap.adapter.Polyline;

import android.net.Uri;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.utils.WXUtils;
import com.taobao.weex.utils.WXViewUtils;

import java.util.ArrayList;

import io.dcloud.feature.weex_amap.adapter.MapAbsMgr;
import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;
import io.dcloud.feature.weex_amap.adapter.WXMapView;

public class PolylineMgr extends MapAbsMgr {
    ArrayList<Polyline> mPolylineCaches;

    public PolylineMgr(WXSDKInstance instance, WXMapView map) {
        super(instance, map);
        mPolylineCaches = new ArrayList<>();
    }

    public void setPolyline(JSONArray polylines) {
        if(polylines != null && polylines.size()> 0) {
            clearPolylines();
            for(int i=0; i<polylines.size(); i++) {
                JSONObject item = polylines.getJSONObject(i);
                PolylineOptions polylineOptions = createPolylineOptions(item);
                if(polylineOptions != null) {
                    Polyline p = mMap.getMap().addPolyline(polylineOptions);
                    if(p != null) {
                        mPolylineCaches.add(p);
                    }
                }
            }
        }
    }

    public PolylineOptions createPolylineOptions(JSONObject item) {
        PolylineOptions polylineOptions = null;
        if(item != null) {
            if(item.containsKey("points")) {
                polylineOptions = new PolylineOptions();
                JSONArray array = item.getJSONArray("points");
                polylineOptions.setPoints(MapResourceUtils.crateLatLngs(array));
            } else {
                return polylineOptions;
            }
            if(item.containsKey("color")) {
                polylineOptions.color(MapResourceUtils.getColor(item.getString("color")));
            }
            if(item.containsKey("width")) {
                polylineOptions.width(WXViewUtils.getRealSubPxByWidth(WXUtils.getFloat(item.getString("width")), mInstance.getInstanceViewPortWidth()));
            }
            if(item.containsKey("dottedLine") && item.getBoolean("dottedLine")) {
                polylineOptions.setDottedLine(true);
                polylineOptions.setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE);
            } else {
                polylineOptions.setDottedLine(false);
            }
            if(item.containsKey("arrowLine") && item.getBoolean("arrowLine")) {
                polylineOptions.setUseTexture(true);
                if(item.containsKey("arrowIconPath")) {
                    Uri parsedUri = mInstance.rewriteUri(Uri.parse(item.getString("arrowIconPath")), URIAdapter.IMAGE);
                    BitmapDescriptor bitmap = BitmapDescriptorFactory.fromPath(parsedUri.getPath());
                    if(bitmap != null) {
                        polylineOptions.setCustomTexture(bitmap);
                    }
                }
            } else {
                polylineOptions.setUseTexture(false);
            }

        }
        return polylineOptions;
    }

    public void clearPolylines() {
        if(mPolylineCaches != null) {
            for(Polyline p: mPolylineCaches) {
                p.remove();
            }
            mPolylineCaches.clear();
        }
    }
}
