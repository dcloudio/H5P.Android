package io.dcloud.feature.weex_livepusher;

import android.content.Context;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import io.dcloud.feature.weex.WeexInstanceMgr;

public class LivePusherPlugin {
    public static void initPlugin(Context context) {
        try {
            WXSDKEngine.registerComponent("live-pusher", PusherComponent.class);
            WeexInstanceMgr.addWeexPluginNameForDebug("live-pusher", PusherComponent.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }
}
