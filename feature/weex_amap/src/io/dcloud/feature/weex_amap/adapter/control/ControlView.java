package io.dcloud.feature.weex_amap.adapter.control;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.utils.WXViewUtils;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.weex.adapter.FrescoLoadUtil;
import io.dcloud.feature.weex_amap.adapter.Constant;

public class ControlView {
    public String id;
    public JSONObject position;
    public String iconPath;
    public boolean clickable = false;
    private ImageView mView;
    private FrameLayout rootView;
    private String ref;
    private WXSDKInstance mInstance;

    public ControlView(WXSDKInstance instance, String ref, String id, JSONObject data, FrameLayout rootView) {
        if (data.containsKey("iconPath") && data.containsKey("position")) {
            this.mInstance = instance;
            this.ref = ref;
            this.rootView = rootView;
            this.iconPath = data.getString("iconPath");
            this.position = data.getJSONObject("position");
            this.id = id;
            if (data.containsKey("clickable")) {
                this.clickable = data.getBooleanValue("clickable");
            }
            createView(instance.getContext(), rootView);
        }
    }

    private void createView(Context context, FrameLayout rootView) {
        mView = new ImageView(context);
        loadIcon();
    }

    public void update(JSONObject data) {
        if (data.containsKey("iconPath")) {
            iconPath = data.getString("iconPath");
        }
        if (data.containsKey("position")) {
            position = data.getJSONObject("position");
        }
        if (data.containsKey("clickable")) {
            clickable = data.getBooleanValue("clickable");
        }
    }

    private void loadIcon() {
        int width = FrameLayout.LayoutParams.WRAP_CONTENT;
        int height = FrameLayout.LayoutParams.WRAP_CONTENT;
        int left = 0;
        int top = 0;
        if (position != null) {
            if (position.containsKey("width")) {
                width = position.getIntValue("width");
                width =  WXViewUtils.getRealPxByWidth2(width, mInstance.getInstanceViewPortWidth());
            }
            if (position.containsKey("height")) {
                height = position.getIntValue("height");
                height =  WXViewUtils.getRealPxByWidth2(height, mInstance.getInstanceViewPortWidth());
            }
            if(position.containsKey("top")) {
                top = position.getIntValue("top");
            }
            if(position.containsKey("left")) {
                left = position.getIntValue("left");
            }
        }
        if (clickable) {
            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, Object> params = new HashMap<>();
                    JSONObject data = new JSONObject();
                    data.put("controlId", id);
                    params.put("detail", data);
                    mInstance.fireEvent(ref, Constant.EVENT.BIND_CONTROL_TAP, params);
                }
            });
        } else {
            mView.setOnClickListener(null);
        }
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(width, height);
        fl.leftMargin = WXViewUtils.getRealPxByWidth2(left, mInstance.getInstanceViewPortWidth());
        fl.topMargin = WXViewUtils.getRealPxByWidth2(top, mInstance.getInstanceViewPortWidth());
        rootView.addView(mView, fl);
        Uri parsedUri = mInstance.rewriteUri(Uri.parse(iconPath), URIAdapter.IMAGE);
        FrescoLoadUtil.getInstance().loadImageBitmap(mInstance.getContext(), parsedUri.toString(), width, height, new FrescoLoadUtil.BitmapCallback<Bitmap>() {
            @Override
            public void onSuccess(Uri uri, Bitmap result) {
                mView.setImageBitmap(result);
            }

            @Override
            public void onFailure(Uri uri, Throwable throwable) {

            }
        });
    }

    public void destroy() {
        if (mView != null) {
            mView.setImageBitmap(null);
        }
    }
}
