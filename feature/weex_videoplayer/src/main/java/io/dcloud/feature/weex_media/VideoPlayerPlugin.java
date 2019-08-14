package io.dcloud.feature.weex_media;

import android.content.Context;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;
import com.taobao.weex.ui.SimpleComponentHolder;

import io.dcloud.feature.weex.WeexInstanceMgr;

public class VideoPlayerPlugin {
    public static void initPlugin(Context context) {
        try {
            WXSDKEngine.registerComponent("u-video", VideoComponent.class);
            WXSDKEngine.registerComponent(new SimpleComponentHolder(VideoInnerViewComponent.class, new VideoInnerViewComponent.Ceator()), false, "u-scalable");
            WeexInstanceMgr.addWeexPluginNameForDebug("video", VideoComponent.class);
            WeexInstanceMgr.addWeexPluginNameForDebug("div", VideoInnerViewComponent.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }
}
