package io.dcloud.feature.weex_barcode;

import android.content.Context;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import io.dcloud.feature.weex.WeexInstanceMgr;

public class BarcodePlugin {
    public static void initPlugin(Context context) {
        try {
            WXSDKEngine.registerComponent("barcode", BarcodeComponent.class);
            WXSDKEngine.registerModule("barcodeScan",BarcodeModule.class);
            WeexInstanceMgr.addWeexPluginNameForDebug("barcode", BarcodeComponent.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }
}
