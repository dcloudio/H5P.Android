package io.dcloud.media.live;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFrameView;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWaiter;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.media.live.push.LivePusher;

public class LiveMediaFeatureImpl extends StandardFeature implements IWaiter,ISysEventListener {
    HashMap<String, LivePusher> pusherList = null;
    LivePusher  activePusher = null;
    JSONArray   initOptions = null;

    private AbsMgr featureMgr;

    @Override
    public void onStart(Context pContext, Bundle pSavedInstanceState, String[] pRuntimeArgs) {
        super.onStart(pContext, pSavedInstanceState, pRuntimeArgs);
    }

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        super.init(pFeatureMgr, pFeatureName);
        this.featureMgr = pFeatureMgr;
    }

    @Override
    public void onPause() {
        super.onPause();
        if ( null != activePusher){
            activePusher.pause(null, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ( null != activePusher){
            activePusher.resume(null, null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( null != activePusher){
            activePusher.stop(null,null);
        }
    }



    public void LivePusher(IWebview pWebview, JSONArray objectArray) {
        String pluginID = null;
        LivePusher pusherObject = null;
        JSONArray functionArguments = objectArray.optJSONArray(1);
        if (objectArray != null){
            pluginID = objectArray.optString(0);
        }

        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject == null) {
            pusherObject = new LivePusher(pWebview,objectArray);
            if (pusherObject != null) {
                if (pusherList == null) {
                    pusherList = new HashMap<String, LivePusher>();
                }
                initOptions = objectArray;
                pusherList.put(pluginID, pusherObject);
                pusherObject.setStatusListener(new LivePusherStateListener() {
                    @Override
                    public void onRtmpStopped(String pusherid) {
                        pusherList.remove(pusherid);
                    }
                });
            }
        }
    }

    public String getLivePusherById(IWebview pWebview, JSONArray array){
        if (pusherList != null){
            for (String key : pusherList.keySet()) {
                LivePusher live = pusherList.get(key);
                if (live.getUserId().equals(JSONUtil.getString(array,0))) {
                    try {
                        return JSUtil.wrapJsVar(new JSONObject("{uuid:'"+key+"'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public void start(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);

        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            if (!pusherObject.isInited){
                pusherObject.initLivePusher(pWebview, initOptions);
//                pusherObject.appendLivePusher(null,pWebview.obtainFrameView());
            }
            pusherObject.start(pWebview, array);
            activePusher = pusherObject;
        }
    }

    public void stop(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }
        JSONObject args = array.optJSONObject(1);
        if (pusherObject != null){
            pusherObject.stop(pWebview, args);
            activePusher = null;
        }
    }

    public void preview(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.preview(pWebview);
        }
    }

    public void pause(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.pause(pWebview, array);
            activePusher = null;
        }
    }

    public void resume(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.resume(pWebview, array);
            activePusher = pusherObject;
        }
    }

    public void setOptions(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
//            if (initOptions != null && initOptions.length() > 0){
//                initOptions = joinOptions(initOptions, array);
//            }
            pusherObject.setOptions(pWebview, array.optJSONObject(1));
        }
    }

    public void switchCamera(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.switchCamera(pWebview, array);
        }
    }

    public void snapshot(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.snapshot(pWebview, array);
        }
    }

    public void addEventListener(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.addEventListener(pWebview, array);
        }
    }

    public void resize(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.resize(pWebview, array);
        }
    }

    public void close(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.destory(pluginID);
            pusherObject.removeFromFrame();
        }
    }


    @Override
    public void dispose(String pAppid) {
        super.dispose(pAppid);
        for (Map.Entry<String, LivePusher> entry : pusherList.entrySet()) {
            entry.getValue().destory(entry.getKey());
        }
        //pusherList.clear();
    }

//    public static void removePusher(String appId){
//        if (pusherList != null && appId != null){
//            if (pusherList.containsKey(appId)) {
//                pusherList.remove(appId);
//            }
//        }
//    }

    public static JSONArray joinOptions(JSONArray mData, JSONArray array) {
        JSONArray resultArray = new JSONArray();
        JSONObject destJSONObj = mData.optJSONObject(2);
        JSONObject srcJSONOBJ = array.optJSONObject(1);
        try {
            destJSONObj = deepMerge(destJSONObj, srcJSONOBJ);
            resultArray.put(mData.opt(0));
            resultArray.put(mData.opt(1));
            resultArray.put(destJSONObj);
            return resultArray;
        } catch (Exception e) {
        }
        return mData;
    }

    public static JSONObject deepMerge(JSONObject target, JSONObject source) throws JSONException {
        Iterator iterator = source.keys();
        while(iterator.hasNext()){
            String key = (String) iterator.next();
            Object value = source.getString(key);
            if (!target.has(key)){
                target.put(key,value);
            }else{
                if (value instanceof JSONObject){
                    JSONObject valueJson = (JSONObject)value;
                    deepMerge(valueJson, target.getJSONObject(key));
                }else{
                    target.put(key,value);
                }
            }
        }
        return target;
    }

    private AdaFrameItem appendLivePusher(String id,IFrameView mFrameView){
        LivePusher pusherObject = null;
        if (pusherList != null && id != null){
            pusherObject = pusherList.get(id);
        }
        if (pusherObject != null) {
            if (!pusherObject.isInited){
                pusherObject.initLivePusher(mFrameView.obtainWebView(), initOptions);
            }
            pusherObject.appendLivePusher("",mFrameView);

        }

        return pusherObject;
    }
    @Override
    public Object doForFeature(String actionType, Object args) {
        Object _ret = null;
        if("appendToFrameView".equals(actionType)) {
            Object[] pArgs = (Object[]) args;
            String pusherId = (String) pArgs[1];
            AdaFrameView pusherView = (AdaFrameView) pArgs[0];
            _ret = appendLivePusher(pusherId,pusherView);
        }
        return _ret;
    }
}
