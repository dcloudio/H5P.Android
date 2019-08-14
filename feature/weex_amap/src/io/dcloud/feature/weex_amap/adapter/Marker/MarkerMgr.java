package io.dcloud.feature.weex_amap.adapter.Marker;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.Marker;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.bridge.JSCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.dcloud.feature.weex_amap.adapter.Constant;
import io.dcloud.feature.weex_amap.adapter.MapAbsMgr;
import io.dcloud.feature.weex_amap.adapter.WXMapView;

public class MarkerMgr extends MapAbsMgr {

    ConcurrentHashMap<String, WXMarker> mMarkerCaches;
    ConcurrentHashMap<String, WXMarker> mMarkerTemporary;

    public MarkerMgr(WXSDKInstance instance, WXMapView map) {
        super(instance, map);
        mMarkerCaches = new ConcurrentHashMap<>();
        mMarkerTemporary = new ConcurrentHashMap<>();
    }

    /**
     * 设置更新marker
     * @param markers
     */
    public synchronized void setMarkers(JSONArray markers) throws Exception{
        if(markers != null && markers.size()> 0) {
            mMarkerTemporary.clear();
            for(int i=0; i<markers.size(); i++) {
                JSONObject item = markers.getJSONObject(i);
                String id = String.valueOf(item.hashCode());
                if(item.containsKey(Constant.JSONKEY.ID)) {
                    id = item.getString(Constant.JSONKEY.ID);
                }
                if(mMarkerCaches.containsKey(id)) {
                    WXMarker marker = mMarkerCaches.remove(id);
                    marker.updateMarkerOptions(item);
                    marker.initCalloutAndLabel(mInstance.getContext(), mMap, item);
                    mMarkerTemporary.put(id ,marker);
                } else {
                    WXMarker marker = new WXMarker(mInstance, mMap, item, id);
                    mMarkerTemporary.put(id, marker);
                }
            }
            destroyMarkers(mMarkerCaches);
            mMarkerCaches.putAll(mMarkerTemporary);
        }
    }

    private void isShowInfoWindow(ConcurrentHashMap<Long, WXMarker> markerCaches) {
        Set<Long> keys = markerCaches.keySet();
        for(long key: keys) {
            WXMarker marker = markerCaches.get(key);
            marker.isShowInfoWindow();
        }
    }

    /**
     * 通过Marker获取WXMarker
     * @param m
     * @return
     */
    public WXMarker getWXMarker(Marker m) {
        if(m != null) {
            Set<String> keys = mMarkerCaches.keySet();
            for(String key: keys) {
                WXMarker marker = mMarkerCaches.get(key);
                if(m.equals(marker.getInstance())) {
                    return marker;
                }
            }
        }
        return null;
    }

    /**
     * 清理不需要的marker
     * @param markers
     */
    private void destroyMarkers(Map<String, WXMarker> markers) {
        Set<String> sets = markers.keySet();
        for(String id: sets) {
            WXMarker marker = markers.get(id);
            if(marker != null) {
                marker.destroy();
            }
        }
    }

    /**
     * 获取WXMarker对象
     * @param id
     * @return
     */
    private WXMarker getMarker(String id) {
        if(mMarkerTemporary.containsKey(id)) {
            return mMarkerTemporary.get(id);
        }
        if(mMarkerCaches.containsKey(id)) {
            return mMarkerCaches.get(id);
        }
        return null;
    }

    /**
     * 清理marker
     */
    public void destroy() {
        if(mMarkerCaches != null) {
            Set<String> keys = mMarkerCaches.keySet();
            for (String key: keys) {
                WXMarker marker = mMarkerCaches.get(key);
                if(marker != null) {
                    marker.destroy();
                }
            }
        }
    }

    /**
     * 隐藏弹层
     */
    public void hideMarkerCallout() {
        if(mMarkerCaches != null) {
            Set<String> keys = mMarkerCaches.keySet();
            for (String key: keys) {
                WXMarker marker = mMarkerCaches.get(key);
                if(marker != null) {
                    if(marker.getCallout() != null && !marker.getCallout().isAlwaysDisPlay()) {
                        marker.getCallout().setVisible(false);
                    }
                }
            }
        }
    }

    public void showMarkerCallout(WXMarker marker) {
        if(marker != null && marker.getCallout() != null && !marker.getCallout().isAlwaysDisPlay()) {
            marker.getCallout().setVisible(true);
        }
    }

    public void translateMarker(JSONObject data, final JSCallback callback) {
        if(data != null) {
            String id = data.getString("markerId");
            final WXMarker marker = getMarker(id);
            if(marker != null) {
                marker.translateMarker(data, callback);
            }
        } else {
            if(callback != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("type", "fail");
                callback.invokeAndKeepAlive(params);
            }
        }
    }
}
