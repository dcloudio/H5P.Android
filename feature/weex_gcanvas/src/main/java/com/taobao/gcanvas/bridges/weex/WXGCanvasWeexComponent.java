package com.taobao.gcanvas.bridges.weex;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.TextureView;

import com.taobao.gcanvas.GCanvasJNI;
import com.taobao.gcanvas.bridges.spec.module.IGBridgeModule;
import com.taobao.gcanvas.surface.GTextureView;
import com.taobao.gcanvas.util.GLog;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.Component;
import com.taobao.weex.ui.ComponentCreator;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXVContainer;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ertong
 */

@Component(lazyload = false)
public class WXGCanvasWeexComponent extends WXComponent<GWXTextureView> implements TextureView.SurfaceTextureListener {
    private GWXTextureView mSurfaceView;
    public AtomicBoolean mSended = new AtomicBoolean(false);
    public IGBridgeModule.ContextType mType;

    private static final String TAG = WXGCanvasWeexComponent.class.getSimpleName();

    public WXGCanvasWeexComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }

    public WXGCanvasWeexComponent(WXSDKInstance instance, WXVContainer parent, int type, BasicComponentData basicComponentData) {
        super(instance, parent, type, basicComponentData);
    }

    private void addGCanvasView() {
        String backgroundColor = getStyles().getBackgroundColor();
        mSurfaceView = new GWXTextureView(getContext(), this);
        GCanvasJNI.registerWXCallNativeFunc(getContext());
        if (backgroundColor.isEmpty()) {
            backgroundColor = "rgba(0,0,0,0)";
        }
        mSurfaceView.setBackgroundColor(backgroundColor);
        mSurfaceView.addSurfaceTextureListener(this);
    }

    public static class Creator implements ComponentCreator {
        @Override
        public WXComponent createInstance(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return new WXGCanvasWeexComponent(instance,parent,basicComponentData);
        }
    }

    @Override
    public void onActivityResume() {
        if (null != mSurfaceView) {
            mSurfaceView.resume();
//            mSurfaceView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityDestroy() {
        if (null != mSurfaceView) {
            mSurfaceView.setSurfaceTextureListener(null);
            mSurfaceView.requestExit();
        }
    }

//    @Override
//    public void destroy() {
//        super.destroy();
//        if (null != mSurfaceView) {
//            mSurfaceView.setSurfaceTextureListener(null);
//            mSurfaceView.requestExit();
//        }
//    }

    @Override
    public void onActivityPause() {
        if (null != mSurfaceView) {
            mSurfaceView.pause();
//            mSurfaceView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setBackgroundColor(String color) {
//        int colorInt = Color.TRANSPARENT;
//        if (color != null) {
//            colorInt = WXResourceUtils.getColor(color, Color.TRANSPARENT);
//        }
        getHostView().setBackgroundColor(color);
    }

    @Override
    protected GWXTextureView initComponentHostView(@NonNull Context context) {
        mSended.set(false);
        addGCanvasView();

        return mSurfaceView;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Context ctx = getContext();
        if (ctx == null) {
            GLog.e(TAG, "setDevicePixelRatio error ctx == null");
            return;
        }

        Display display = ((Activity) ctx).getWindowManager().getDefaultDisplay();

        int screenWidth = display.getWidth();
//        double devicePixelRatio = screenWidth / 750.0;
        double devicePixelRatio = width / getInstance().getInstanceViewPortWidth();

        GLog.d(TAG, "enable width = " + screenWidth + ",devicePixelRatio = " + devicePixelRatio);

        GCanvasJNI.setWrapperDevicePixelRatio(getRef(), devicePixelRatio);
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public GTextureView getSurfaceView() {
        return mSurfaceView;
    }

    public void sendEvent() {

        synchronized (this) {
            if (!mSended.get()) {
                Map<String, Object> params = new HashMap<>();
                params.put("ref", getRef());

                getInstance().fireGlobalEventCallback("GCanvasReady", params);
                mSended.set(true);
            } else {
                GLog.d("event sent.");
            }
        }
    }
}
