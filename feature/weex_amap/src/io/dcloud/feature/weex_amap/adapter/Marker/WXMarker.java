package io.dcloud.feature.weex_amap.adapter.Marker;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.RotateAnimation;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.utils.WXViewUtils;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.weex.adapter.FrescoLoadUtil;
import io.dcloud.feature.weex_amap.adapter.Constant;
import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;
import io.dcloud.feature.weex_amap.adapter.WXMapView;

public class WXMarker {
    private Marker mMarker;
    private MarkerCallout mCallout;
    private MarkerLabel mLabel;
    private String mid;
    private WXSDKInstance mInstance;

    public WXMarker(WXSDKInstance instance, WXMapView mapView, JSONObject data, String id) {
        mInstance = instance;
        mid = id;
        mMarker = mapView.getMap().addMarker(createMarkerOptions(data));
        initCalloutAndLabel(mInstance.getContext(), mapView, data);
    }

    public String getId() {
        return mid;
    }

    public Marker getInstance() {
        return mMarker;
    }

    public void setCallout(MarkerCallout mCallout) {
        this.mCallout = mCallout;
    }

    public MarkerCallout getCallout() {
        return mCallout;
    }

    public void isShowInfoWindow() {
        if(mCallout != null && mCallout.isAlwaysDisPlay()) {
            mCallout.setVisible(true);
        }
    }

    private MarkerOptions createMarkerOptions(JSONObject m) {
        MarkerOptions options = new MarkerOptions();
        setPosition(m, options);
        setTitle(m, options);
        setZIndex(m, options);
        setIconInfo(m, options);
        setRotate(m, options);
        setAlphe(m, options);
        setAnchor(m, options);
        return options;
    }


    public void updateMarkerOptions(JSONObject m) {
        if(getInstance() != null) {
            setPosition(m, null);
            setTitle(m, null);
            setZIndex(m, null);
            setIconInfo(m, null);
            setRotate(m, null);
            setAlphe(m, null);
            setAnchor(m, null);
        }
    }

    /**
     * 设置更新 position
     * @param m
     * @param options
     */
    private void setPosition(JSONObject m, MarkerOptions options) {
        double lat = m.getDouble(Constant.JSONKEY.LATITUDE);
        double lng = m.getDouble(Constant.JSONKEY.LONGITUDE);
        LatLng latLng = new LatLng(lat, lng);
        if(getInstance() != null) {
            if(!getInstance().getPosition().equals(latLng)) {
                getInstance().setPosition(latLng);
                if(mCallout != null) {
                    mCallout.setPosition(latLng);
                }
                if(mLabel != null) {
                    mLabel.setPosition(latLng);
                }
            }
        } else if(options != null) {
            options.position(latLng);
        }
    }

    private void setTitle(JSONObject m, MarkerOptions options) {
        if(m.containsKey(Constant.JSONKEY.TITLE)) {
            String title = m.getString(Constant.JSONKEY.TITLE);
            if(getInstance() != null && !TextUtils.isEmpty(getInstance().getTitle()) && !getInstance().getTitle().equals(title)) {
                getInstance().setTitle(title);
            } else if(options != null){
                options.title(title);
            }
        }
    }

    private void setZIndex(JSONObject m, MarkerOptions options) {
        if(m.containsKey(Constant.JSONKEY.ZINDEX)) {
            float zIndex = m.getFloat(Constant.JSONKEY.ZINDEX);
            if(getInstance() != null && zIndex != getInstance().getZIndex()) {
                getInstance().setZIndex(zIndex);
            } else if(options != null){
                options.zIndex(zIndex);
            }
        }
    }

    private void setIconInfo(JSONObject m, MarkerOptions options) {
        if(m.containsKey(Constant.JSONKEY.ICONPATH)) {
            String path = m.getString(Constant.JSONKEY.ICONPATH);
            int width = 0;
            if(m.containsKey(Constant.JSONKEY.WIDTH)) {
                width = m.getIntValue(Constant.JSONKEY.WIDTH);
                width = (int) WXViewUtils.getRealSubPxByWidth(width, mInstance.getInstanceViewPortWidth());
            }
            int height = 0;
            if(m.containsKey(Constant.JSONKEY.HEIGHT)) {
                height = m.getIntValue(Constant.JSONKEY.HEIGHT);
                height = (int) WXViewUtils.getRealSubPxByWidth(height, mInstance.getInstanceViewPortWidth());
            }
            Uri parsedUri = mInstance.rewriteUri(Uri.parse(path), URIAdapter.IMAGE);
            loadImageToIcon(parsedUri, width, height, options, m);
        }
    }

    private void setRotate(JSONObject m, MarkerOptions options) {
        if(m.containsKey(Constant.JSONKEY.ROTATE)) {
            float rotate = m.getFloat(Constant.JSONKEY.ROTATE);
            if(getInstance() != null && getInstance().getRotateAngle() != rotate) {
                getInstance().setRotateAngle(rotate);
                if(mLabel != null) {
                    mLabel.setRotateAngle(rotate);
                }
                if(mCallout != null) {
                    mCallout.setRotateAngle(rotate);
                }
            } else if(options != null){
                options.rotateAngle(rotate);
            }
        }
    }

    private void setAlphe(JSONObject m, MarkerOptions options) {
        if(m.containsKey(Constant.JSONKEY.ALPHE)) {
            float alphe = m.getFloat(Constant.JSONKEY.ALPHE);
            if(getInstance() != null && getInstance().getAlpha() != alphe) {
                getInstance().setAlpha(alphe);
            } else if(options != null){
                options.alpha(alphe);
            }
        }
    }

    private void setAnchor(JSONObject m, MarkerOptions options) {
        if(m.containsKey(Constant.JSONKEY.ANCHOR)) {
            JSONObject j = m.getJSONObject(Constant.JSONKEY.ANCHOR);
            if(j != null) {
                if(getInstance() != null) {
                    getInstance().setAnchor(j.getFloat("x"), j.getFloat("y"));
                } else {
                    options.anchor(j.getFloat("x"), j.getFloat("y"));
                }
            }
        }
    }

    private void loadImageToIcon(Uri uri, int width, int height, final MarkerOptions options, final JSONObject m) {
        FrescoLoadUtil.getInstance().loadImageBitmap(mInstance.getContext(), uri.toString(), width, height, new FrescoLoadUtil.BitmapCallback<Bitmap>() {
            @Override
            public void onSuccess(Uri uri, Bitmap result) {
                loadIcon(result, options, m);
            }

            @Override
            public void onFailure(Uri uri, Throwable throwable) {

            }
        });
    }

    private void loadIcon(Bitmap bitmap, MarkerOptions options, JSONObject m) {
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        if(getInstance() != null) {
            getInstance().setIcon(descriptor);
        } else if(options !=  null){
            options.icon(descriptor);
        }
        isShowInfoWindow();
    }

    public void destroy() {
        if (mMarker != null) {
            mMarker.destroy();
        }
        if (mCallout != null) {
           mCallout.destroy();
        }
        if(mLabel != null) {
            mLabel.destroy();
        }
    }

    public void initCalloutAndLabel(Context context, WXMapView mapView, JSONObject item) {
        if(getCallout() != null) {
            if(item.containsKey(Constant.JSONKEY.CALLOUT)) {
                getCallout().update(item.getJSONObject(Constant.JSONKEY.CALLOUT));
            }
        } else {
            MarkerCallout callout = createMarkerCallout(context, mapView, item);
            setCallout(callout);
        }
        if(item.containsKey(Constant.JSONKEY.LABEL)) {
            if(mLabel != null) {
                mLabel.update(item.getJSONObject(Constant.JSONKEY.LABEL));
            } else {
                mLabel = new MarkerLabel(context, mMarker, item.getJSONObject(Constant.JSONKEY.LABEL), mapView, mInstance.getInstanceViewPortWidth());
            }
        }
    }

    private MarkerCallout createMarkerCallout(Context context, WXMapView mapView, JSONObject m) {
        MarkerCallout callout = null;
        if(m.containsKey(Constant.JSONKEY.CALLOUT)) {
            JSONObject c = m.getJSONObject(Constant.JSONKEY.CALLOUT);
            callout = new MarkerCallout(context, mMarker, c, mapView, mInstance.getInstanceViewPortWidth());
        }
        return callout;
    }

    public void translateMarker(JSONObject data, final JSCallback callback) {
        JSONObject point = data.getJSONObject("destination");
        final LatLng d = MapResourceUtils.crateLatLng(point);
        boolean autoRotate = true;
        if(data.containsKey("autoRotate")) {
            autoRotate = data.getBoolean("autoRotate");
        }
        long duration = 1000;
        if(data.containsKey("duration")) {
            duration = data.getLongValue("duration");
        }
        final Marker m = getInstance();
        if(data.containsKey("rotate")) {
            float rotate = - data.getFloat("rotate");
            rotateToTranslateAnimation(m, rotate, duration, d, callback);
            if(mLabel != null) {
                rotateToTranslateAnimation(mLabel.getInstance(), rotate, duration, d, null);
            }
            if(mCallout != null) {
                rotateToTranslateAnimation(mCallout.getInstance(), rotate, duration, d, null);
            }
        } else {
            translateAnimation(d, duration, m, callback);
            if(mLabel != null) {
                translateAnimation(d, duration, mLabel.getInstance(), null);
            }
            if(mCallout != null) {
                translateAnimation(d, duration, mCallout.getInstance(), null);
            }
        }
        if(callback != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("type", "success");
            callback.invokeAndKeepAlive(params);
        }
    }

    /**
     * 执行旋转并移动动画
     * @param m
     * @param rotate
     * @param duration
     * @param d
     * @param callback
     */
    private void rotateToTranslateAnimation(final Marker m, float rotate, final long duration, final LatLng d, final JSCallback callback) {
        RotateAnimation rotateAnimation = new RotateAnimation(m.getRotateAngle(), rotate);
//        final long rotateDn = (long) (duration * 0.4);
//        final long translateDn = duration - rotateDn;
        rotateAnimation.setDuration(duration);
        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart() {
            }

            @Override
            public void onAnimationEnd() {
                translateAnimation(d, duration, m, callback);
            }
        });
        m.setAnimation(rotateAnimation);
        m.startAnimation();
    }

    /**
     * 执行移动动画
     * @param d
     * @param duration
     * @param m
     * @param callback
     */
    private void translateAnimation(LatLng d, long duration, Marker m, final JSCallback callback) {
        TranslateAnimation translateAnimation = new TranslateAnimation(d);
        translateAnimation.setDuration(duration);
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart() {

            }

            @Override
            public void onAnimationEnd() {
                if(callback != null) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("type", "animationEnd");
                    callback.invokeAndKeepAlive(params);
                }
            }
        });
        m.setAnimation(translateAnimation);
        m.startAnimation();
    }
}
