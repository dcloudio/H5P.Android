package io.dcloud.feature.weex_amap.adapter.control;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.dcloud.feature.weex_amap.adapter.MapAbsMgr;
import io.dcloud.feature.weex_amap.adapter.WXMapView;

public class ControlMgr extends MapAbsMgr {
    private FrameLayout mControlRootView;
    private ConcurrentHashMap<String, ControlView> mConcurrentCaches;
    private String mRef;

    public ControlMgr(WXSDKInstance instance, String ref,WXMapView map, FrameLayout container) {
        super(instance, map);
        mRef = ref;
        mConcurrentCaches = new ConcurrentHashMap<>();
        mControlRootView = new FrameLayout(instance.getContext());
        container.addView(mControlRootView, 1, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setControls(JSONArray array) {
        if (array != null && array.size() > 0) {
            HashMap<String, ControlView> linshi = new HashMap<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                String id = String.valueOf(item.hashCode());
                if(item.containsKey("id")) {
                    id = item.getString("id");
                }
                ControlView controlView;
                if(mConcurrentCaches.containsKey(id)) {
                    controlView = mConcurrentCaches.remove(id);
                    controlView.update(item);
                    linshi.put(id, controlView);
                } else {
                    controlView = new ControlView(mInstance, mRef, id, item, mControlRootView);
                    linshi.put(id, controlView);
                }
            }
            removeDiscardControl(mConcurrentCaches);
            mConcurrentCaches.putAll(linshi);
        }
    }

    private void removeDiscardControl(ConcurrentHashMap<String, ControlView> caches) {
        for(int i =0;i<caches.size();i++) {
            caches.get(i).destroy();
        }
        mConcurrentCaches.clear();
    }
}
