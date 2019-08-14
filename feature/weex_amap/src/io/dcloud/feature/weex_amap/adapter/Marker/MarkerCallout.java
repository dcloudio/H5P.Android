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

public class MarkerCallout extends AbsMarkerTextView {
    private Marker mMarker;
    private Marker mRootMarker;
    Context mContext;
    public MarkerCallout(Context context, Marker rootMarker, JSONObject c, WXMapView mapView, int viewPort) {
        super(c, viewPort);
        mContext = context;
        mRootMarker = rootMarker;
        createMarker(mRootMarker.getPosition(), mapView);
    }
    public Marker getInstance() {
        return mMarker;
    }
    public void createMarker(LatLng point, WXMapView mapView) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.anchor(0.5f, 1);
        markerOptions.visible(false);
        mMarker = mapView.getMap().addMarker(markerOptions);
        mMarker.setRotateAngle(mRootMarker.getRotateAngle());
    }

    @Override
    public void update(JSONObject c) {
        super.update(c);
        if(mMarker != null) {
            mMarker.setIcon(getIcon());
        }
    }

    private BitmapDescriptor getIcon() {
        LinearLayout rootView = new LinearLayout(mContext);
        TextView textView = getTextView(mContext, true);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int bottomMargin = 20;
        if(mRootMarker.getIcons() != null && mRootMarker.getIcons().size() > 0) {
            bottomMargin = mRootMarker.getIcons().get(0).getHeight();
        }
        l.bottomMargin = bottomMargin;
        rootView.addView(textView, l);
        return BitmapDescriptorFactory.fromView(rootView);
    }

    public void destroy() {
        if(mMarker != null) {
            mMarker.remove();
        }
    }

    public boolean isVisible() {
        if(mMarker != null) {
         return mMarker.isVisible();
        }
        return false;
    }

    public void setVisible(boolean visible) {
        if(mMarker != null) {
            if(visible) {
                if(!mMarker.isVisible()) {
                    mMarker.setVisible(true);
                }
                mMarker.setIcon(getIcon());
            } else {
                mMarker.setVisible(false);
            }
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
