package io.dcloud.feature.weex_barcode;

import android.content.Context;
import android.graphics.Color;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.utils.WXResourceUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.dcloud.common.util.PdrUtil;

public class BarcodeComponent extends WXComponent<BarcodeView> {

    private AtomicBoolean isLoad = new AtomicBoolean(false);
    public BarcodeComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }

    public BarcodeComponent(WXSDKInstance instance, WXVContainer parent, int type, BasicComponentData basicComponentData) {
        super(instance, parent, type, basicComponentData);
    }

    @Override
    protected BarcodeView initComponentHostView(final Context context) {
        return new BarcodeView(context, this, getInstance());
    }

    @Override
    public void updateProperties(Map<String, Object> props) {
        super.updateProperties(props);
        if (!getAttrs().containsKey("background")) {
            getHostView().setBackgroundColor(Color.BLACK);
        }
    }

    /**
     * 颜色转换，包括rgb和16进制
     *
     * @param color
     */
    @WXComponentProp(name = "frameColor")
    public void setFrameColor(String color) {
        getHostView().setFrameColor(WXResourceUtils.getColor(color));
    }

    @WXComponentProp(name = "background")
    public void setBackground(String color) {
        getHostView().setBackground(WXResourceUtils.getColor(color));
    }

    @WXComponentProp(name = "scanbarColor")
    public void setScanbarColor(String color) {
        getHostView().setScanBarColor(WXResourceUtils.getColor(color));
    }

    @WXComponentProp(name = "filters")
    public void setFilters(JSONArray filters) {
        getHostView().initDecodeFormats(filters);
    }

    private boolean isAnimationEnd = false;
    @WXComponentProp(name = "autostart")
    public void setSutoStart(final boolean isstart) {
        if (!isAnimationEnd) {
            getInstance().addFrameViewEventListener(new WXSDKInstance.FrameViewEventListener() {
                @Override
                public void onShowAnimationEnd() {
                    isAnimationEnd = true;
                    if (isstart)
                        start(null);
                    getInstance().removeFrameViewEventListener(this);
                }
            });
        } else {
            if (isstart) {
                start(null);
            }
        }
    }

    @JSMethod
    public void start(JSONObject option) {
        if (option != null) {
            getHostView().setConserve(option.containsKey("conserve") ? option.getBoolean("conserve") : false);
            getHostView().setFilename(PdrUtil.getDefaultPrivateDocPath(option.getString("filename"), "png"));
            getHostView().setVibrate(option.containsKey("vibrate") ? option.getBoolean("vibrate") : true);
            getHostView().setPlayBeep(!option.containsKey("sound") || option.getString("sound").equals("default"));
        }
        getHostView().start();
    }

    @Override
    protected void setHostLayoutParams(BarcodeView host, int width, int height, int left, int right, int top, int bottom) {
        super.setHostLayoutParams(host, width, height, left, right, top, bottom);
        if (!isLoad.get()) {
            isLoad.set(true);
            getHostView().initBarcodeView(width, height);
        } else {
            //更新view
            getHostView().updateStyles(width,height);
        }
    }

    @JSMethod
    public void cancel() {
        getHostView().cancelScan();
    }

    @JSMethod
    public void close() {
//        getHostView().closeScan();
    }

    @JSMethod
    public void setFlash(boolean open) {
        if (PdrUtil.isEmpty(open)) return;
        getHostView().setFlash(open);
    }

    /**
     * 重写销毁方法，instance销毁时同时销毁相机占用
     */
    @Override
    public void destroy() {
        super.destroy();
        getHostView().closeScan();
        getHostView().onDestory();
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        getHostView().onResume(true);
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        getHostView().onPause();
    }
}
