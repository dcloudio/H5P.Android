package io.dcloud.feature.weex_livepusher;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.dom.CSSConstants;
import com.taobao.weex.dom.WXAttr;
import com.taobao.weex.layout.ContentBoxMeasurement;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.utils.WXViewUtils;

import io.dcloud.common.util.DialogUtil;
import io.dcloud.common.util.PdrUtil;

public class PusherComponent extends WXComponent<TCPusherView> {
    private WXAttr attr;
    public PusherComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
        attr = basicComponentData.getAttrs();
        setContentBoxMeasurement(new ContentBoxMeasurement() {
            @Override
            public void measureInternal(float width, float height, int widthMeasureMode, int heightMeasureMode) {
                if (CSSConstants.isUndefined(width)) {
                    mMeasureWidth = WXViewUtils.getRealPxByWidth(300,getInstance().getInstanceViewPortWidth());
                }
                if (CSSConstants.isUndefined(height)) {
                    mMeasureHeight = WXViewUtils.getRealPxByWidth(225,getInstance().getInstanceViewPortWidth());
                }
            }

            @Override
            public void layoutBefore() {

            }

            @Override
            public void layoutAfter(float computedWidth, float computedHeight) {

            }
        });
    }

    public PusherComponent(WXSDKInstance instance, WXVContainer parent, int type, BasicComponentData basicComponentData) {
        super(instance, parent, type, basicComponentData);
    }

    private boolean isInit = false;
    @Override
    protected TCPusherView initComponentHostView(Context context) {
        if(Build.CPU_ABI.equalsIgnoreCase("x86")) {
            if (!isInit) {
                isInit = true;
                DialogUtil.showDialog((Activity) context, null, context.getString(R.string.livepusher_error_tips), new String[]{null});
            }
            return null;
        }
        return new TCPusherView(context, this, attr.get("devicePosition") != null && Boolean.parseBoolean(attr.get("devicePosition").toString()));
    }

    @Override
    protected void onFinishLayout() {
        super.onFinishLayout();
//        getHostView().init();
//        getHostView().onLayoutFinish();
    }

    @Override
    protected boolean setProperty(String key, Object param) {
        if (getHostView() == null)
            return true;
        return super.setProperty(key, param);
    }

    @WXComponentProp(name = "url")
    public void setSrc(String src) {
        getHostView().setSrc(src);
    }

    @WXComponentProp(name = "mode")
    public void setMode(String mode) {
        getHostView().setMode(mode);
    }

    @WXComponentProp(name = "autopush")
    public void setAutoPusher(boolean isAuto) {
//        getHostView().
    }

    @WXComponentProp(name = "muted")
    public void isMute(boolean mote) {
        getHostView().setMute(mote);
    }

    @WXComponentProp(name = "enableCamera")
    public void setEnableCamera(boolean isEnable) {
        getHostView().enableCamera(isEnable);
    }

    @WXComponentProp(name = "autoFocus")
    public void setAutoFocus(boolean isAuto) {
        getHostView().autoFocus(isAuto);
    }

    @WXComponentProp(name = "orientation")
    public void setorientation(String orientation) {
        getHostView().setOritation(orientation);
    }

    @WXComponentProp(name = "beauty")
    public void setBeauty(int beauty) {
        getHostView().setBeauty(beauty);
    }

    @WXComponentProp(name = "whiteness")
    public void setWhiteness(int whiteness) {
        getHostView().setWhite(whiteness);
    }

    @WXComponentProp(name = "aspect")
    public void setAspect(String aspect) {
//        getHostView().set(whiteness);
    }

    @WXComponentProp(name = "minBitrate")
    public void setMinBitrate(int minBitrate) {
        getHostView().setMinBitrate(minBitrate);
    }

    @WXComponentProp(name = "maxBitrate")
    public void setMaxBitrate(int maxBitrate) {
        getHostView().setMaxBitrate(maxBitrate);
    }

    @WXComponentProp(name = "waitingImage")
    public void setWaitingImage(String waitingImage) {
        getHostView().setWaintImage(waitingImage);
    }

    @WXComponentProp(name = "zoom")
    public void setZoom(boolean zoom) {
        getHostView().setZoom(zoom);
    }

    // SDK限制，中间设置不生效？？？？？？
//    @WXComponentProp(name = "devicePosition")
//    public void setDevicePosition(String devicePosition) {
//        getHostView().switchCamera(devicePosition);
//    }

//    boolean isBGMute = false;

    @WXComponentProp(name = "backgroundMute")
    public void setBackgroundMute(boolean backgroundMute) {
//        isBGMute = backgroundMute;
        getHostView().setBGMute(backgroundMute);
    }

    @JSMethod
    public void start(JSCallback callback) {
        getHostView().start(callback);
    }

    @JSMethod
    public void stop(JSCallback callback) {
        getHostView().stopPusher(callback);
    }

    @JSMethod
    public void pause(JSCallback callback) {
        getHostView().pause(callback);
    }

    @JSMethod
    public void resume(JSCallback callback) {
        getHostView().resume(callback);
    }

    @JSMethod
    public void switchCamera(JSCallback callback) {
        getHostView().sCamera(callback);
    }

    @JSMethod
    public void snapshot(JSCallback success) {
        getHostView().snapShot(success);
    }

    @JSMethod
    public void toggleTorch(JSCallback callback) {
        getHostView().toggleTorch(callback);
    }

    @JSMethod
    public void playBGM(JSONObject param, JSCallback success) {
        String url = param.getString("url");
        getHostView().playBGM(url, success);
    }

    @JSMethod
    public void stopBGM(JSCallback success) {
        getHostView().stopBGM(success);
    }

    @JSMethod
    public void pauseBGM(JSCallback success) {
        getHostView().pauseBGM(success);
    }

    @JSMethod
    public void resumeBGM(JSCallback success) {
        getHostView().resumeBGM(success);
    }

    @JSMethod
    public void setBGMVolume(JSONObject param, JSCallback success) {
        int volume = param.getInteger("volume");
        if (PdrUtil.isEmpty(volume)) return;
        getHostView().setBGNVolume(volume, success);
    }

    @JSMethod
    public void startPreview(JSCallback callback) {
        getHostView().preview(callback);
    }

    @JSMethod
    public void stopPreview(JSCallback callback) {
        getHostView().stopPreview(callback);
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        getHostView().pause(null);
//        if (isBGMute)
//            isMute(true);
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        getHostView().resume(null);
//        if (isBGMute)
//            isMute(true);
    }

//    private List<String> events = new ArrayList<>();

//    @Override
//    public void addEvent(String type) {
//        super.addEvent(type);
//        getHostView().addEvent(type);
//        // 权限未获取时缓存监听数据
////        if (getHostView() == null) {
////            events.add(type);
////        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        for (int i : grantResults) {
//            if (i == -1) {
//                return;
//            }
//        }
//        getHostView().init();
//        // 加入缓存的事件
//
//        for (String type : events) {
//            getHostView().addEvent(type);
//        }
//    }


    @Override
    public void destroy() {
        super.destroy();
        getHostView().stopPusher(null);
        getHostView().destory();
    }
}
