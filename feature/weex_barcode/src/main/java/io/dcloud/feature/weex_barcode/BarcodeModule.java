package io.dcloud.feature.weex_barcode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.alibaba.fastjson.JSONArray;
import com.dcloud.zxing2.Result;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.feature.barcode2.decoding.CaptureActivityHandler;

public class BarcodeModule extends WXModule {
    @JSMethod
    public void scan(String filepath, JSCallback callback, JSONArray filter) {
        // 设置filter
        try {
            String path = mWXSDKInstance.rewriteUri(Uri.parse(filepath), URIAdapter.IMAGE).getPath();
            Bitmap map = BitmapFactory.decodeFile(path);
            Result result = CaptureActivityHandler.decode(map);
            if (result != null) {
                Map<String, Object> values = new HashMap<>();
                values.put("type", "success");
                values.put("code", result.getBarcodeFormat().toString());
                values.put("message", JSONUtil.toJSONableString(result.getText()));
                values.put("file", path == null ? "" : path);
                Map<String,Object> details = new HashMap<>();
                details.put("detail",values);
                callback.invoke(details);
            } else {
                Map<String, Object> values = new HashMap<>();
                values.put("type", "fail");
                values.put("code", DOMException.CODE_BARCODE_ERROR);
                values.put("message", DOMException.MSG_BARCODE);
                Map<String,Object> details = new HashMap<>();
                details.put("detail",values);
                callback.invoke(details);
            }
        }catch (Exception e) {
            Map<String, Object> values = new HashMap<>();
            values.put("type", "fail");
            values.put("code", DOMException.CODE_BARCODE_ERROR);
            values.put("message", e.getMessage());
            Map<String,Object> details = new HashMap<>();
            details.put("detail",values);
            callback.invoke(details);
        }
    }
}
