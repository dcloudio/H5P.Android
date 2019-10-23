package io.dcloud.feature.gcanvas;

import android.content.Context;

import com.taobao.gcanvas.bridges.weex.GCanvasWeexModule;
import com.taobao.gcanvas.bridges.weex.WXGCanvasWeexComponent;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import io.dcloud.feature.weex.WeexInstanceMgr;

public class GCanvasRegister {
    public static void initPlugin(Context context) {
        try {
            WXSDKEngine.registerComponent("gcanvas", WXGCanvasWeexComponent.class);
            WXSDKEngine.registerModule("gcanvas", GCanvasWeexModule.class);
            WeexInstanceMgr.addWeexPluginNameForDebug("canvas", WXGCanvasWeexComponent.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }
}
