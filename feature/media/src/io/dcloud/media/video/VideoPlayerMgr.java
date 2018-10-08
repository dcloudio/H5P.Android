package io.dcloud.media.video;

import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFrameView;
import io.dcloud.common.DHInterface.IMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.util.ViewRect;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;

public class VideoPlayerMgr {

    private static VideoPlayerMgr mInstance;

    private HashMap<String, DHVideoFrameItem> mPlayerCaches;

    private ViewGroup.LayoutParams _vlps;

//    public String position = "static";

//    private boolean isFromCreate = false;

    private AbsMgr mFeatureMgr;

    public VideoPlayerMgr() {
        mPlayerCaches = new HashMap<String, DHVideoFrameItem>();
    }

    public static VideoPlayerMgr getInstance() {
        if(mInstance == null) {
            mInstance = new VideoPlayerMgr();
        }
        return mInstance;
    }
    public void initFeature(AbsMgr mFeatureMgr) {
        this.mFeatureMgr = mFeatureMgr;
    }
    public IWebview findWebview(IWebview webview,String webId){
        Object object = mFeatureMgr.processEvent(IMgr.MgrType.FeatureMgr,IMgr.FeatureEvent.CALL_WAITER_DO_SOMETHING,
                new Object[]{webview,"ui","findWebview",new String[]{webview.obtainApp().obtainAppId(), webId}});
        if (object instanceof IWebview) {
            return (IWebview) object;
        }
        return null;
    }

    public void createVideoPlayer(IWebview webview, String id, JSONArray rectJa, JSONObject style, String userId,boolean isCerate) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        } else {
            frameItem = new DHVideoFrameItem(webview.getContext(), id, webview, rectJa,style,userId);
            mPlayerCaches.put(id, frameItem);
        }
        if (!isCerate)
            frameItem.appendToFrame(webview.obtainFrameView());
//        AdaFrameItem frameView = (AdaFrameItem)webview.obtainFrameView();
//        ViewRect webParentViewRect = frameView.obtainFrameOptions();
//        float scale = webview.getScale();
//        int _w = PdrUtil.convertToScreenInt(JSONUtil.getString(rectJa,2),webParentViewRect.width,webParentViewRect.width,scale);
//        int _h = PdrUtil.convertToScreenInt(JSONUtil.getString(rectJa,3),webParentViewRect.height,webParentViewRect.height,scale);
//        int _l = PdrUtil.convertToScreenInt(JSONUtil.getString(rectJa,0),webParentViewRect.width - _w,0,scale); /*+ webParentViewRect.left*/;
//        int _t = PdrUtil.convertToScreenInt(JSONUtil.getString(rectJa,1),webParentViewRect.height - _h,0,scale); /*+ webParentViewRect.top*/;
//        frameItem.updateViewRect((AdaFrameItem)webview.obtainFrameView(), new int[]{_l,_t,_w,_h}, new int[]{webParentViewRect.width,webParentViewRect.height});
//        ViewGroup.LayoutParams _lp = AdaFrameItem.LayoutParamsUtil.createLayoutParams(_l, _t,_w, _h);
//        position = style.optString("position");
//        if (userId == null) {
//            isFromCreate = true;
//            if (frameItem.obtainMainView().getParent() != null) {
//                frameItem.obtainMainView().setLayoutParams(_lp);
//            } else {
//                webview.addFrameItem(frameItem, _lp);
//            }
//        } else {
////            if("absolute".equals(position)){
////                webview.obtainFrameView().addFrameItem(frameItem, _lp);
////            }else{
//////            默认为"static",也可能为其它非法字符串
////                if(DeviceInfo.sDeviceSdkVer >= 11){//使用系统默认View硬件加速，可能引起闪屏，LAYER_TYPE_HARDWARE无效
////                    webview.obtainWebview().setLayerType(View.LAYER_TYPE_NONE, null);
////                }
////                webview.addFrameItem(frameItem, _lp);
////            }
//            isFromCreate = false;
//            _vlps = _lp;
//        }
    }

    public void play(String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.play();
        }
    }

    public void pause(String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.pause();
        }
    }

    public void stop(String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.stop();
        }
    }

    public void close(IWebview webview, String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.close();
            frameItem.removeFrameItem();
        }
    }

    public void sendDanmu(String id,JSONObject danmu){
        DHVideoFrameItem frameItem = null;
        if (mPlayerCaches != null && mPlayerCaches.containsKey(id)){
            frameItem = mPlayerCaches.get(id);
        }
        if (frameItem != null) {
            frameItem.sendDanmu(danmu);
        }
    }

    public JSONObject findVideoPlayer(String id){
        if (id == null)
            return null;
        DHVideoFrameItem frameItem = null;
        if (mPlayerCaches != null){
            for (String key : mPlayerCaches.keySet()) {
                DHVideoFrameItem item = mPlayerCaches.get(key);
                if(id.equals(item.getUserId())){
                    frameItem = item;
                }
            }
        }
        if (frameItem != null) {
            try {
                return new JSONObject("{'name':'"+frameItem.getUserId()+"','uid':'"+frameItem.getmId()+"'}");
            } catch (JSONException e) {
            }
        }
        return null;
    }

    public AdaFrameItem appendVideoPlayer(String id, IFrameView frameView){
        DHVideoFrameItem frameItem = null;
        if (mPlayerCaches != null && mPlayerCaches.containsKey(id)){
            frameItem = mPlayerCaches.get(id);
        }
        if (frameItem != null){
//            frameItem.setmAppendWebview(frameView.obtainWebView());
//            frameItem.removeMapFrameItem(frameView.obtainWebView());
//            frameItem.appendToFrameView((AdaFrameView) frameView);
            // absolute
//            if (position.equals("absolute")) {
//                if (_vlps != null) {
//                    frameView.addFrameItem(frameItem,_vlps);
//                }
//            } else if (position.equals("static")) {
//                if (_vlps != null) {
//                    frameView.obtainWebView().addFrameItem(frameItem,_vlps);
//                }
//            }
            frameItem.appendToFrame(frameView);

        }
        return frameItem;
    }

    public void seekTo(String id, String position) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.seek(position);
        }
    }

    public void recovery() {
        if(mPlayerCaches != null && mPlayerCaches.size() > 0) {
            Set<String> keySet = mPlayerCaches.keySet();
            for (String entry : keySet) {
                mPlayerCaches.get(entry).release();
            }
        }
        mPlayerCaches.clear();
    }

    public void rmovePlayer(String id) {
        if(mPlayerCaches != null && mPlayerCaches.size() > 0) {
            if(mPlayerCaches.containsKey(id)) {
                mPlayerCaches.remove(id);
            }
        }
    }

    public void requestFullScreen(String id, String direction) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.requestFullScreen(direction);
        }
    }

    public void exitFullScreen(String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.exitFullScreen();
        }
    }

    public void show(String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.show();
        }
    }

    public void hidden(String id) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.hidden();
        }
    }

    public void addEventListener(IWebview webview, String id, String evnet, String callbackId,String webId) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.addEventListener(evnet, callbackId,webId);
        }
    }

    public void setOptions(String id, JSONObject options) {
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null) {
            frameItem.setOptions(options);
        }
    }

    public void setPlayBackRate(String id,String rate){
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches != null && mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem != null)
            frameItem.sendPlayBackRate(rate);
    }

    public void resize(IWebview webview, String id, JSONArray pJsArgs){
        DHVideoFrameItem frameItem = null;
        if(mPlayerCaches.containsKey(id)) {
            frameItem = mPlayerCaches.get(id);
        }
        if(frameItem == null) {
            return;
        }
        frameItem.resize(pJsArgs);
    }
}
