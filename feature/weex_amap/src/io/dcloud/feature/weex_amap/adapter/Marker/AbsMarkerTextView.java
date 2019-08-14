package io.dcloud.feature.weex_amap.adapter.Marker;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.utils.WXUtils;
import com.taobao.weex.utils.WXViewUtils;

import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;
import io.dcloud.feature.weex_amap.ui.ArrowTextView;

public class AbsMarkerTextView {
    private String content;
    private String color = "#000";
    private float fontSize = 11;
    private int borderRadius = 0;
    private int borderWidth = 0;
    private String borderColor;
    private String bgColor = "#fff";
    private int padding = 0;
    private String display;
    private int textAlign = Gravity.LEFT;
    private int mViewPort;

    public AbsMarkerTextView(JSONObject c, int viewPort) {
        mViewPort = viewPort;
        setContent(c);
        setColor(c);
        setFontSize(c);
        setBorderRadius(c);
        setBorderWidth(c);
        setBorderColor(c);
        setBgColor(c);
        setPadding(c);
        setDisplay(c);
        setTextAlign(c);
    }

    public void setContent(JSONObject c) {
        if(c.containsKey("content")) {
            this.content = c.getString("content");
        }
    }
    public void setColor(JSONObject c) {
        if(c.containsKey("color")) {
            this.color = c.getString("color");
        }
    }

    public void setFontSize(JSONObject c) {
        if(c.containsKey("fontSize")) {
            this.fontSize = c.getFloatValue("fontSize");
        }
    }

    public void setBorderRadius(JSONObject c) {
        if(c.containsKey("borderRadius")) {
            this.borderRadius = c.getIntValue("borderRadius");
        }
    }

    public void setBorderWidth(JSONObject c) {
        if(c.containsKey("borderWidth")) {
            this.borderWidth = c.getIntValue("borderWidth");
            this.padding = (int) WXViewUtils.getRealSubPxByWidth(borderWidth, mViewPort);
        }
    }

    public void setBorderColor(JSONObject c) {
        if(c.containsKey("borderColor")) {
            this.borderColor = c.getString("borderColor");
        }
    }

    public void setBgColor(JSONObject c) {
        if(c.containsKey("bgColor")) {
            this.bgColor = c.getString("bgColor");
        }
    }

    public void setPadding(JSONObject c) {
        if(c.containsKey("padding")) {
            this.padding = c.getIntValue("padding");
            this.padding = (int) WXViewUtils.getRealSubPxByWidth(padding, mViewPort);
        }
    }

    public void setDisplay(JSONObject c) {
        if(c.containsKey("display")) {
            this.display = c.getString("display");
        }
    }

    public boolean isAlwaysDisPlay() {
        boolean isDp = false;
        if(!TextUtils.isEmpty(display) && display.equals("ALWAYS")) {
            isDp = true;
        }
        return isDp;
    }

    public void setTextAlign(JSONObject c) {
        if(c.containsKey("textAlign")) {
            textAlign = Gravity.LEFT;
            String align = c.getString("textAlign");
            switch (align) {
                case "right" : {
                    textAlign = Gravity.RIGHT;
                    break;
                }
                case "center" : {
                    textAlign = Gravity.CENTER;
                }
            }
        }
    }

    public void update(JSONObject c) {
        setContent(c);
        setColor(c);
        setFontSize(c);
        setBorderRadius(c);
        setBorderWidth(c);
        setBorderColor(c);
        setBgColor(c);
        setPadding(c);
        setDisplay(c);
        setTextAlign(c);
    }

    public TextView getTextView(Context context, boolean isSharp) {
        ArrowTextView textView = new ArrowTextView(context, isSharp);
        textView.setBgColor(MapResourceUtils.getColor(bgColor));
        textView.setTextPadding(padding);
        textView.setGravity(textAlign);
        textView.setRadius(borderRadius);
        textView.setStrokeWidth(borderWidth);
        if(!TextUtils.isEmpty(borderColor)) {
            textView.setStrokeColor(MapResourceUtils.getColor(borderColor));
        }
        textView.setText(content);
        textView.setIncludeFontPadding(false);
        if(!TextUtils.isEmpty(color)) {
            textView.setTextColor(MapResourceUtils.getColor(color));
        }
        if(fontSize > 0) {
            float size = WXViewUtils.getRealPxByWidth(fontSize, mViewPort);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
        return textView;
    }

    public void destroy() {

    }
}
