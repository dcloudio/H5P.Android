package io.dcloud.feature.weex_amap.adapter.Marker;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import io.dcloud.feature.weex_amap.adapter.WXMapView;

public class MarkerLabel extends AbsMarkerTextView {
    private int x;
    private int y;
    private Marker mMarker;
    private Marker mRootMarker;
    JSONObject mData;
    Context mContext;
    public MarkerLabel(Context context, Marker rootMarker, JSONObject data, WXMapView mapView, int viewPort) {
        super(data, viewPort);
        mContext = context;
        mRootMarker = rootMarker;
        setData(data);
        createMarker(mRootMarker.getPosition(), mapView);
    }

    public void createMarker(LatLng point, WXMapView mapView) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(getIcon());
        markerOptions.position(point);
        markerOptions.anchor(0, 0);
        mMarker = mapView.getMap().addMarker(markerOptions);
        mMarker.setRotateAngle(mRootMarker.getRotateAngle());
    }

    public Marker getInstance() {
        return mMarker;
    }

    private void setData(JSONObject data) {
        mData = data;
        if(data.containsKey("anchorX")) {
            x = data.getIntValue("anchorX");
        }
        if(data.containsKey("anchorY")) {
            y = data.getIntValue("anchorY");
        }
    }

    public void update(JSONObject data) {
        setData(data);
        if(mMarker != null) {
            mMarker.setIcon(getIcon());
        }
    }

    private BitmapDescriptor getIcon() {
        LinearLayout rootView = new LinearLayout(mContext);
        TextView textView = getTextView(mContext, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = x;
        lp.topMargin = y;
        rootView.addView(textView, lp);
        return BitmapDescriptorFactory.fromView(rootView);
    }

    public void destroy() {
        if(mMarker != null) {
            mMarker.remove();
        }
    }

    public void setPosition(LatLng latLng) {
        if(mMarker != null) {
            mMarker.setPosition(latLng);
        }
    }

    public void setRotateAngle(float var1) {
        if(mMarker != null) {
            mMarker.setRotateAngle(var1);
        }
    }

}
