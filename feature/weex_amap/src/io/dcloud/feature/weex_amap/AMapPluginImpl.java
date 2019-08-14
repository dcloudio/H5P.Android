package io.dcloud.feature.weex_amap;

import android.content.Context;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import io.dcloud.feature.weex.WeexInstanceMgr;
import io.dcloud.feature.weex_amap.Module.WXMapSearchModule;
import io.dcloud.feature.weex_amap.component.WXAMapViewComponent;

public class AMapPluginImpl {
    public static void initPlugin(Context context) {
        try {
            WXSDKEngine.registerComponent("map", WXAMapViewComponent.class);
            WXSDKEngine.registerModule("mapSearch", WXMapSearchModule.class);
            WeexInstanceMgr.self().addWeexPluginNameForDebug("map", WXAMapViewComponent.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }
}
