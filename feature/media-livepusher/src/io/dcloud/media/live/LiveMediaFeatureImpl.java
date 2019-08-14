package io.dcloud.media.live;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFrameView;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWaiter;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.PermissionUtil;
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



    public void LivePusher(final IWebview pWebview, final JSONArray objectArray) {
        String pluginID = null;
        final LivePusher[] pusherObject = {null};
        JSONArray functionArguments = objectArray.optJSONArray(1);
        if (objectArray != null){
            pluginID = objectArray.optString(0);
        }

        if (pusherList != null && pluginID != null){
            pusherObject[0] = pusherList.get(pluginID);
        }

        if (pusherObject[0] == null) {
            final String finalPluginID = pluginID;
            PermissionUtil.StreamPermissionRequest request = new PermissionUtil.StreamPermissionRequest(pWebview.obtainApp()) {
                private List<String> gramtPermission = new ArrayList<>();
                @Override
                public void onGranted(String streamPerName) {
                    gramtPermission.add(streamPerName);
                    if (gramtPermission.size() < this.getSystemRequestPermission().length) {
                        return;
                    }
                    pusherObject[0] = new LivePusher(pWebview,objectArray);
                    if (pusherObject[0] != null) {
                        if (pusherList == null) {
                            pusherList = new HashMap<String, LivePusher>();
                        }
                        initOptions = objectArray;
                        pusherList.put(finalPluginID, pusherObject[0]);
                        pusherObject[0].setStatusListener(new LivePusherStateListener() {
                            @Override
                            public void onRtmpStopped(String pusherid) {
                                pusherList.remove(pusherid);
                            }
                        });
                    }
                    if (listeners.containsKey(finalPluginID)) {
                        Map<JSONArray,IWebview> callback = listeners.get(finalPluginID);
                        for (JSONArray array : callback.keySet()) {
                            pusherObject[0].addEventListener(callback.get(array),array);
                        }
                        listeners.remove(finalPluginID);
                    }

                    if (appendView.containsKey(finalPluginID)){
                        appendLivePusher(finalPluginID,appendView.get(finalPluginID));
                        appendView.remove(finalPluginID);
                    }
                    if (previewMap.containsKey(finalPluginID)) {
                        pusherObject[0].preview(previewMap.get(finalPluginID));
                        previewMap.remove(finalPluginID);
                    }
                    if (pusherOptions.containsKey(finalPluginID)) {
                        Map<IWebview,JSONObject> option = pusherOptions.get(finalPluginID);
                        for (IWebview key : option.keySet()) {
                            pusherObject[0].setOptions(key, option.get(key));
                        }
                        pusherOptions.remove(finalPluginID);
                    }
                    if (startMap.containsKey(finalPluginID)) {
                        if(!pusherObject[0].isInited){
                            pusherObject[0].initLivePusher(pWebview, initOptions);
                        }
                        Map<String,Object> startItem = startMap.get(finalPluginID);
                        IWebview pwebviewImpl = (IWebview) startItem.get("webView");
                        JSONArray array = (JSONArray) startItem.get("array");
                        pusherObject[0].start(pwebviewImpl, array);
                        activePusher = pusherObject[0];
                        startMap.remove(finalPluginID);
                    }
                }

                @Override
                public void onDenied(String streamPerName) {
                }
            };
            request.setRequestPermission("android.permission.CAMERA","android.permission.RECORD_AUDIO");
            PermissionUtil.useSystemPermissions(pWebview.getActivity(), new String[]{"android.permission.CAMERA","android.permission.RECORD_AUDIO"}, request);
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

    private Map<String,Map<String,Object>> startMap = new HashMap<>();
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
        }else {
            Map<String,Object> startparams = new HashMap<>();
            if (startMap.containsKey(pluginID)) {
                startparams = startMap.get(pluginID);
            }
            startparams.put("webView",pWebview);
            startparams.put("array",array);
            startMap.put(pluginID,startparams);
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

    private Map<String,IWebview> previewMap = new HashMap<>();
    public void preview(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.preview(pWebview);
        } else {
            previewMap.put(pluginID,pWebview);
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

    private Map<String,Map<IWebview,JSONObject>> pusherOptions = new HashMap<>();
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
        } else {
            Map<IWebview,JSONObject> options = new HashMap<>();
            if (pusherOptions.containsKey(pluginID)) {
                options = pusherOptions.get(pluginID);
            }
            options.put(pWebview,array.optJSONObject(1));
            pusherOptions.put(pluginID,options);
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

    private Map<String,Map<JSONArray,IWebview>> listeners = new HashMap<>();
    public void addEventListener(IWebview pWebview, JSONArray array){
        LivePusher pusherObject = null;
        String pluginID = array.optString(0);
        if (pusherList != null && pluginID != null){
            pusherObject = pusherList.get(pluginID);
        }

        if (pusherObject != null){
            pusherObject.addEventListener(pWebview, array);
        } else {
            Map<JSONArray,IWebview> callbacks = new HashMap<>();
            if (listeners.containsKey(array.optString(0))) {
                callbacks = listeners.get(array.optString(0));
            }
            callbacks.put(array,pWebview);
            listeners.put(array.optString(0),callbacks);
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

    private Map<String,IFrameView> appendView = new HashMap<>();
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
        } else {
            if (id != null)
                appendView.put(id,mFrameView);
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
