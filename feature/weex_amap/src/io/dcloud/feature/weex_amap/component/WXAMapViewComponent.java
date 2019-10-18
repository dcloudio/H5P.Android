package io.dcloud.feature.weex_amap.component;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Poi;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.Constants;
import com.taobao.weex.dom.CSSConstants;
import com.taobao.weex.layout.ContentBoxMeasurement;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.dcloud.feature.weex_amap.adapter.Circle.CircleMgr;
import io.dcloud.feature.weex_amap.adapter.Constant;
import io.dcloud.feature.weex_amap.adapter.MapInterface;
import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;
import io.dcloud.feature.weex_amap.adapter.Marker.MarkerMgr;
import io.dcloud.feature.weex_amap.adapter.Marker.WXMarker;
import io.dcloud.feature.weex_amap.adapter.Polygon.PolygonMgr;
import io.dcloud.feature.weex_amap.adapter.Polyline.PolylineMgr;
import io.dcloud.feature.weex_amap.adapter.WXMapView;
import io.dcloud.feature.weex_amap.adapter.control.ControlMgr;


public class WXAMapViewComponent extends WXVContainer<FrameLayout> implements MapInterface {
    private static final String TAG = "WXAMapViewComponent";

    private static final int REQUEST_CODE_MAPVIEW = 1501224;
    private static String[] permissions = new String[]{
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
    };
    private WXMapView mMapView;
    private AMap mAMap;
    private UiSettings mUiSettings;
    private Activity mActivity;

    // 是否支持手势缩放
    private boolean isZoomEnable = true;
    private boolean isBuilding3D = false;
    private boolean isCompassEnable = false;
    private boolean isScrollEnable = true;
    private boolean isRotateEnable = false;
    private boolean isMyLocationEnable = false;
    private boolean isOverLookingEnable = false;
    private boolean isEnableSatellite = false;
    private boolean isEnableTraffic = false;
    private boolean isShowScale = false;
    private float mZoomLevel = 16;
    private float mRotate = 0;
    private float mSkew = 0;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    //private HashMap<String, WXMapInfoWindowComponent> mInfoWindowHashMap = new HashMap<>();
    private AtomicBoolean isMapLoaded = new AtomicBoolean(false);
    private AtomicBoolean isInited = new AtomicBoolean(false);
    private Queue<MapOperationTask> paddingTasks = new LinkedList<>();
    private FrameLayout mMapContainer;
    private String defBackgroundColor = "#f1f1f1";
    private long mLoadTime = 0;

    private boolean isChangeStart = false;
    private Point mMapCenterPoint;

    private MarkerMgr mMarkerMgr;
    private PolylineMgr mPolylineMgr;
    private PolygonMgr mPolygonMgr;
    private CircleMgr mCircleMgr;
    private ControlMgr mControlMgr;

    boolean mDragged = false;
    String mCameraType = "drag";
    boolean isSetUpdate = false;
    private float mDefHeight = 0;
    private float mDefWidth = 0;

    private boolean isShowAnimationEnd = false;
    public WXAMapViewComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
        mLoadTime = System.currentTimeMillis();
        mDefHeight = WXViewUtils.getRealPxByWidth(150, getInstance().getInstanceViewPortWidth());
        mDefWidth = WXViewUtils.getRealPxByWidth(300, getInstance().getInstanceViewPortWidth());
        if(!basicComponentData.getStyles().containsKey(Constants.Name.BACKGROUND_COLOR)) {
            basicComponentData.getStyles().put(Constants.Name.BACKGROUND_COLOR, defBackgroundColor);
        }
        setContentBoxMeasurement(new ContentBoxMeasurement() {
            @Override
            public void measureInternal(float width, float height, int widthMeasureMode, int heightMeasureMode) {
                if(CSSConstants.isUndefined(height)) {
                    height = mDefHeight;
                }
                mMeasureHeight = height;
            }

            @Override
            public void layoutBefore() {

            }

            @Override
            public void layoutAfter(float computedWidth, float computedHeight) {

            }
        });
        getInstance().addFrameViewEventListener(new WXSDKInstance.FrameViewEventListener() {
            @Override
            public void onShowAnimationEnd() {
                //页面动画结束 触发地图加载 规避动画过程中导致卡顿问题
                isShowAnimationEnd = true;
                if(mMapView != null) {
                    mMapView.setVisibility(View.VISIBLE);
                    WXLogUtils.e(TAG, "Map VISIBLE");
                }
                //createMap();
                getInstance().removeFrameViewEventListener(this);
            }
        });
    }

    @Override
    protected FrameLayout initComponentHostView(@NonNull Context context) {
        mMapContainer = new FrameLayout(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
        createMap();
        return mMapContainer;
    }

    private void createMap() {
        if(mMapContainer != null) {
            int index = -1;
            if(mMapContainer.getChildCount() > 0) {
                index = 0;
            }
            mMapView = new WXMapView(getContext());
            mMapContainer.addView(mMapView, index, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mMapView.onCreate(null);
            mMapView.setVisibility(View.INVISIBLE);
            WXLogUtils.e(TAG, "Create MapView " + mMapView.toString());
            initMap();
        }
    }

    @Override
    protected void setHostLayoutParams(FrameLayout host, int width, int height, int left, int right, int top, int bottom) {
        super.setHostLayoutParams(host, width, height, left, right, top, bottom);
        if (!isMapLoaded.get() && !isInited.get()) {
            isInited.set(true);
            mMapContainer.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if(isShowAnimationEnd || getInstance().isFrameViewShow()) {
                        mMapView.setVisibility(View.VISIBLE);
//                        createMap();
                    }
                }
            }, 0);
        }
    }

    public void initMap() {
        isMapLoaded.set(false);
        if (mAMap == null) {
            mMapCenterPoint = new Point();
            mAMap = mMapView.getMap();
            mAMap.setMinZoomLevel(5);
            mAMap.setMaxZoomLevel(18);
            mAMap.showBuildings(false);
            // 设置中心点
            setCenter(getAttrs().get("latitude"), getAttrs().get("longitude"));
            mAMap.setInfoWindowAdapter(new AMap.ImageInfoWindowAdapter() {
                @Override
                public long getInfoWindowUpdateTime() {
                    return 0;
                }

                @Override
                public View getInfoWindow(Marker m) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    return null;
                }
            });

            mAMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
                @Override
                public void onMapLoaded() {
                    WXLogUtils.e(TAG, "Map loaded");
                    isMapLoaded.set(true);
                    mMapView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            execPaddingTasks();
                        }
                    }, 16);
                    fireEventMapEvent(Constant.EVENT.UPDATED, null);
                }
            });

            mAMap.setOnMapClickListener(new AMap.OnMapClickListener(){

                @Override
                public void onMapClick(LatLng latLng) {
                    mMarkerMgr.hideMarkerCallout();
                    fireEventMapEvent(Constant.EVENT.BINDTAP, null);
                }
            });

            // 绑定 Marker 被点击事件
            mAMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
                // marker 对象被点击时回调的接口
                // 返回 true 则表示接口已响应事件，否则返回false
                @Override
                public boolean onMarkerClick(Marker m) {
                    if (m != null) {
                        JSONObject data = new JSONObject();
                        WXMarker marker = mMarkerMgr.getWXMarker(m);
                        if(marker != null) {
                            data.put("markerId", marker.getId());
                            mMarkerMgr.showMarkerCallout(marker);
                            fireEventMapEvent(Constant.EVENT.BIND_MARKER_TAP, data);
                        } else {
                            marker = mMarkerMgr.getCalloutToWXMarker(m);
                            if(marker != null) {
                                data.put("markerId", marker.getId());
                                fireEventMapEvent(Constant.EVENT.BIND_CALLOUT_TAP, data);
                            }
                        }
                    }
                    return true;
                }
            });

            mAMap.setOnInfoWindowClickListener(new AMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker m) {
                    if(m != null) {
                        WXMarker marker = mMarkerMgr.getWXMarker(m);
                        JSONObject data = new JSONObject();
                        if(marker != null) {
                            data.put("markerId", marker.getId());
                        }
                        fireEventMapEvent(Constant.EVENT.BIND_CALLOUT_TAP, data);
                    }
                }
            });
            mAMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    mMapCenterPoint.update(cameraPosition.target.latitude, cameraPosition.target.longitude);
                    if(!isChangeStart && (mDragged || isSetUpdate)) {
                        if(isSetUpdate) {
                            mCameraType = "update";
                        } else if(cameraPosition.zoom != mZoomLevel) {
                            mCameraType = "scale";
                        } else {
                            mCameraType = "drag";
                        }
                        mZoomLevel = cameraPosition.zoom;
                        isChangeStart = true;
                        fireEventMapEvent(Constant.EVENT.BINDREGION_CHANGE, "begin");
                    }
                }

                @Override
                public void onCameraChangeFinish(CameraPosition cameraPosition) {
                    if(isChangeStart) {
                        isChangeStart = false;
                        isSetUpdate = false;
                        fireEventMapEvent(Constant.EVENT.BINDREGION_CHANGE, "end");
                    }
                }
            });

            mAMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {

                @Override
                public void onTouch(MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            mDragged = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            if (mDragged) getInstance().fireEvent(getRef(), Constant.EVENT.DRAG_CHANGE);
                            mDragged = false;
                            break;
                    }
                }
            });

            mAMap.setOnPOIClickListener(new AMap.OnPOIClickListener() {
                @Override
                public void onPOIClick(Poi poi) {
                    JSONObject data = new JSONObject();
                    data.put("name", poi.getName());
                    data.put("longitude", poi.getCoordinate().longitude);
                    data.put("latitude", poi.getCoordinate().latitude);
                    fireEventMapEvent(Constant.EVENT.BIND_POI_TAP, data);
                }
            });
            setUpMap();
        }
    }

    private void setUpMap() {
        mAMap.showBuildings(isBuilding3D);
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(mZoomLevel));
        mUiSettings = mAMap.getUiSettings();
        mUiSettings.setZoomGesturesEnabled(isZoomEnable);
        mUiSettings.setCompassEnabled(isCompassEnable);
        mUiSettings.setRotateGesturesEnabled(isRotateEnable);
        mUiSettings.setScrollGesturesEnabled(isScrollEnable);
        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setTiltGesturesEnabled(isOverLookingEnable);
    }

    @Override
    public void updateAttrs(Map<String, Object> attrs) {
        super.updateAttrs(attrs);
        if(attrs.containsKey("longitude") || attrs.containsKey("latitude")) {
            updateCenter(getAttrs().get("latitude"), getAttrs().get("longitude"));
        }
    }

    /**
     * 设置中心点
     * @param latitude
     * @param longitude
     */
    public void updateCenter(final Object latitude, final Object longitude) {

        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isSetUpdate = true;
                setCenter(latitude, longitude);
            }
        });
    }

    public void setCenter(Object latitude, Object longitude) {
        LatLng latLng = MapResourceUtils.crateLatLng(latitude, longitude);
        if(latLng == null) {
            return;
        }
        mMapCenterPoint.update(latLng.latitude, latLng.longitude);
        mAMap.moveCamera(CameraUpdateFactory.changeLatLng(latLng));
    }

    /**
     * 设置缩放level
     * @param level
     */
    @WXComponentProp(name = Constant.Name.SCALE)
    public void setScale(final int level) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                mZoomLevel = level;
                isSetUpdate = true;
                mAMap.moveCamera(CameraUpdateFactory.zoomTo(level));
            }
        });
    }

    /**
     * 展示3D楼块
     * @param is3D
     */
    @WXComponentProp(name = Constant.Name.ENABLE3D)
    public void setBuilding3D(final boolean is3D) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isBuilding3D = is3D;
                mAMap.showBuildings(is3D);
            }
        });
    }

    /**
     * 显示指南针
     * @param compass
     */
    @WXComponentProp(name = Constant.Name.SHOW_COMPASS)
    public void setCompass(final boolean compass) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isCompassEnable = compass;
                mUiSettings.setCompassEnabled(compass);
            }
        });
    }

    /**
     * 设置卫星地图
     * @param is
     * @throws Exception
     */
    @WXComponentProp(name = Constant.Name.ENABLE_SATELLITE)
    public void setEnableSatellite(final boolean is) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isEnableSatellite = is;
                if(isEnableSatellite) {
                    mAMap.setMapType(mAMap.MAP_TYPE_SATELLITE);
                } else {
                    mAMap.setMapType(1);
                }
            }
        });
    }

    /**
     * 是否开启实时路况
     * @param is
     * @throws Exception
     */
    @WXComponentProp(name = Constant.Name.ENABLE_TRAFFIC)
    public void setEnableTraffic(final boolean is) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isEnableTraffic = is;
                mAMap.setTrafficEnabled(isEnableTraffic);
            }
        });
    }

    /**
     * 是否开启比例尺
     * @param is
     * @throws Exception
     */
    @WXComponentProp(name = Constant.Name.SHOW_SCALE)
    public void showScale(final boolean is) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isShowScale = is;
                mUiSettings.setScaleControlsEnabled(isShowScale);
            }
        });
    }


    /**
     * 是否支持缩放
     * @param zoomEnable
     */
    @WXComponentProp(name = Constant.Name.ENABLE_ZOOM)
    public void setZoomEnable(final boolean zoomEnable) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isZoomEnable = zoomEnable;
                mUiSettings.setZoomGesturesEnabled(zoomEnable);
            }
        });
    }

    /**
     * 是否支持拖动
     * @param scrollEnable
     */
    @WXComponentProp(name = Constant.Name.ENABLE_SCROLL)
    public void setScrollEnable(final boolean scrollEnable) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isScrollEnable = scrollEnable;
                mUiSettings.setScrollGesturesEnabled(scrollEnable);
            }
        });
    }

    /**
     * 是否支持旋转
     * @param rotateEnable
     */
    @WXComponentProp(name = Constant.Name.ENABLE_ROTATE)
    public void setRotateEnable(final boolean rotateEnable) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isRotateEnable = rotateEnable;
                mUiSettings.setRotateGesturesEnabled(rotateEnable);
            }
        });
    }

    /**
     * 旋转角度
     */
    @WXComponentProp(name = Constant.Name.ROTATE)
    public void setRotate(final float rotate) {
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                mRotate = rotate;
                mAMap.moveCamera(CameraUpdateFactory.changeBearing(mRotate));
            }
        });
    }

    /**
     * 倾斜角度
     * @param skew
     */
    @WXComponentProp(name = Constant.Name.SKEW)
    public void setSkew(final float skew) {
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                mSkew = skew;
                mAMap.moveCamera(CameraUpdateFactory.changeTilt(mSkew));
            }
        });
    }

    /**
     * 开启俯视视角
     * @param isEnable
     */
    @WXComponentProp(name = Constant.Name.ENABLE_OVERLOOKING)
    public void setOverLookingEnable(final boolean isEnable) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                isOverLookingEnable = isEnable;
                mUiSettings.setTiltGesturesEnabled(isEnable);
            }
        });
    }

    /**
     * 个性化地图使用的key，仅初始化地图时有效
     * @param keys
     */
    @WXComponentProp(name = Constant.Name.KEYS)
    public void setApiKey(String keys) throws Exception{
        JSONObject object = JSON.parseObject(keys);
        String key = object.getString("android");
        if (!TextUtils.isEmpty(key)) {
            MapsInitializer.setApiKey(key);
            AMapLocationClient.setApiKey(key);
            //ServiceSettings.getInstance().setApiKey(key);
            WXLogUtils.d(TAG, "Set API key success");
        }
    }

    /**
     * 标记点
     * @param markers
     */
    @WXComponentProp(name = Constant.Name.MARKERS)
    public void setMarkers(final JSONArray markers) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mMarkerMgr == null) {
                    mMarkerMgr = new MarkerMgr(getInstance(), mMapView);
                }
                try {
                    mMarkerMgr.setMarkers(markers);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 路线
     * @param polylines
     */
    @WXComponentProp(name = Constant.Name.POLYLINE)
    public void setPolyline(final JSONArray polylines) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mPolylineMgr == null) {
                    mPolylineMgr = new PolylineMgr(getInstance(), mMapView);
                }
                mPolylineMgr.setPolyline(polylines);
            }
        });
    }

    /**
     * 多边形
     * @param polygons
     */
    @WXComponentProp(name = Constant.Name.POLYGONS)
    public void setPolygon(final JSONArray polygons) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mPolygonMgr == null) {
                    mPolygonMgr = new PolygonMgr(getInstance(), mMapView);
                }
                mPolygonMgr.setPolygon(polygons);
            }
        });
    }

    /**
     * 圆
     * @param circles
     */
    @WXComponentProp(name = Constant.Name.CIRCLES)
    public void setCircles(final JSONArray circles) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mCircleMgr == null) {
                    mCircleMgr = new CircleMgr(getInstance(), mMapView);
                }
                mCircleMgr.setCircles(circles);
            }
        });
    }

    @WXComponentProp(name = Constant.Name.CONTROLS)
    public void setcontrols(final JSONArray controls) throws Exception{
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mControlMgr == null) {
                    mControlMgr = new ControlMgr(getInstance(), getRef(), mMapView, mMapContainer);
                }
                mControlMgr.setControls(controls);
            }
        });
    }

    /**
     * 显示带有方向的当前定位点
     */
    AMap.OnMyLocationChangeListener mMyLocation;
    private MyLocationStyle myLocationStyle;
    @WXComponentProp(name = Constant.Name.SHOW_LOCATION)
    public void showMyLocation(final boolean isShow) {
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(isShow) {
                    if(mMyLocation == null && requestPermissions()) {
                        mMyLocation = new AMap.OnMyLocationChangeListener() {
                            @Override
                            public void onMyLocationChange(Location location) {
                            }
                        };
                        mapView.getMap().setMyLocationEnabled(true);
                        mapView.getMap().setOnMyLocationChangeListener(mMyLocation);
                        myLocationStyle = new MyLocationStyle();
                        mapView.getMap().setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER));
                    }
                } else {
                    mapView.getMap().setMyLocationEnabled(false);
                    mapView.getMap().setOnMyLocationChangeListener(null);
                }
            }
        });
    }

    /**
     * 缩放视野以包含所有给定的坐标点
     * @param points
     */
    @WXComponentProp(name = Constant.Name.INCLUDE_POINTS)
    public void setincludePoints(final JSONArray points) throws Exception{
        setincludePoints(points, 0);
    }

    public void setincludePoints(final JSONArray points, final int padding) {
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                ArrayList<LatLng> latLngs = MapResourceUtils.crateLatLngs(points);
                if(latLngs.size() > 0) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for(LatLng l: latLngs) {
                        builder.include(l);
                    }
                    mapView.getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
                }
            }
        });
    }

    private void fireEventMapEvent(String event, Object data) {
        Map<String, Object> params = new HashMap<>();
        switch (event) {
            case Constant.EVENT.BINDREGION_CHANGE: {
                if(containsEvent(Constant.EVENT.BINDREGION_CHANGE)) {
                    params.put("type", data);
                    Map<String, Object> target = new HashMap<>();
                    target.put("id", getAttrs().get("id"));
                    params.put("target", target);
                    params.put("causedBy", mCameraType);
                    fireEvent(Constant.EVENT.BINDREGION_CHANGE, params);
                }
                break;
            }
            case Constant.EVENT.BIND_MARKER_TAP: {
                //params.put("type", "");
                if(data != null) {
                    params.put("detail", data);
                }
                fireEvent(Constant.EVENT.BIND_MARKER_TAP, params);
                break;
            }
            case Constant.EVENT.BIND_CALLOUT_TAP: {
                //params.put("type", "");
                if(data != null) {
                    params.put("detail", data);
                }
                fireEvent(Constant.EVENT.BIND_CALLOUT_TAP, params);
                break;
            }
            case Constant.EVENT.BINDTAP: {
                if(containsEvent(Constant.EVENT.BINDTAP)) {
                    params.put("type", "tap");
                    fireEvent(Constant.EVENT.BINDTAP, params);
                }
                break;
            }
            case Constant.EVENT.UPDATED: {
                if(containsEvent(Constant.EVENT.UPDATED)) {
                    params.put("timeStamp", System.currentTimeMillis() - mLoadTime);
                    params.put("type", Constant.EVENT.UPDATED);
                    fireEvent(Constant.EVENT.UPDATED, params);
                }
                break;
            }
            case Constant.EVENT.BIND_POI_TAP: {
                if(containsEvent(Constant.EVENT.BIND_POI_TAP)) {
					params.put("type", Constant.EVENT.UPDATED);
                    params.put("detail", data);
                    fireEvent(Constant.EVENT.BIND_POI_TAP, params);
                }
                break;
            }
        }
    }


    /**
     * 获取当前地图中心的经纬度。返回的是 gcj02 坐标系
     * @param callback
     */
    @JSMethod
    public void getCenterLocation(JSCallback callback) {
        Map<String, Object> params = new HashMap<>();
        if(isMapLoaded.get()) {
            params.put("type", "success");
            params.put("latitude", mMapCenterPoint.latitude);
            params.put("longitude", mMapCenterPoint.longitude);
        } else {
            params.put("type", "fail");
        }
        if(callback != null) {
            callback.invoke(params);
        }
    }

    /**
     * 获取当前地图的视野范围
     * @param callback
     */
    @JSMethod
    public void getRegion(JSCallback callback) {
        Map<String, Object> params = new HashMap<>();
        if(isMapLoaded.get()) {
            LatLngBounds latLngBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;// 获取可视区域的Bounds
            params.put("type", "success");
            params.put("northeast", latLngBounds.northeast);
            params.put("southwest", latLngBounds.southwest);
        } else {
            params.put("type", "fail");
        }
        if(callback != null) {
            callback.invoke(params);
        }
    }

    /**
     * 获取当前地图的缩放级别
     * @param callback
     */
    @JSMethod
    public void getScale(JSCallback callback) {
        Map<String, Object> params = new HashMap<>();
        if(isMapLoaded.get()) {
            params.put("type", "success");
            params.put("scale", mZoomLevel);
        } else {
            params.put("type", "fail");
        }
        if(callback != null) {
            callback.invoke(params);
        }
    }

    /**
     * 获取当前地图的旋转角
     * @param callback
     */
    @JSMethod
    public void getRotate(JSCallback callback) {
        Map<String, Object> params = new HashMap<>();
        if(isMapLoaded.get()) {
            params.put("type", "success");
            params.put("rotate", mRotate);
        } else {
            params.put("type", "fail");
        }
        if(callback != null) {
            callback.invoke(params);
        }
    }

    /**
     * 获取当前地图的倾斜角
     * @param callback
     */
    @JSMethod
    public void getSkew(JSCallback callback) {
        Map<String, Object> params = new HashMap<>();
        if(isMapLoaded.get()) {
            params.put("type", "success");
            params.put("rotate", mSkew);
        } else {
            params.put("type", "fail");
        }
        if(callback != null) {
            callback.invoke(params);
        }
    }

    /**
     * 缩放视野展示所有经纬度
     * @param data
     * @param callback
     */
    @JSMethod
    public void includePoints(JSONObject data, final JSCallback callback) throws Exception{
        Map<String, Object> params = new HashMap<>();
        if(isMapLoaded.get() && data != null) {
            JSONArray points = data.getJSONArray("points");
            JSONArray paddings = data.getJSONArray("padding");
            int padding = 20;
            if(paddings != null) {
                padding = paddings.getIntValue(0);
            }
            setincludePoints(points, padding);
            params.put("type", "success");
            params.put("scale", mZoomLevel);
        } else {
            params.put("type", "fail");
        }
        if(callback != null) {
            callback.invoke(params);
        }
    }

    /**
     * 将地图中心移动到当前定位点。需要配合map组件的show-location使用
     */
    @JSMethod
    public void moveToLocation() {
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mMyLocation != null) {
                    Location l = mapView.getMap().getMyLocation();
                    LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());
                    mapView.getMap().animateCamera(CameraUpdateFactory.changeLatLng(latLng));
                }
            }
        });
    }

    /**
     * 平移marker，带动画
     * @param data
     * @param callback
     */
    @JSMethod
    public void translateMarker(final JSONObject data, final JSCallback callback) {
        postTask(new MapOperationTask() {
            @Override
            public void execute(WXMapView mapView) {
                if(mMarkerMgr != null) {
                    mMarkerMgr.translateMarker(data, callback);
                }
            }
        });
    }


    @Override
    public void onActivityCreate() {
        super.onActivityCreate();
        WXLogUtils.e(TAG, "onActivityCreate");
    }

    @Override
    public void onActivityPause() {
        if (mMapView != null) {
            mMapView.onPause();
        }
        WXLogUtils.e(TAG, "onActivityPause");
    }

    @Override
    public void onActivityResume() {
        if (mMapView != null) {
            mMapView.onResume();
        }
        WXLogUtils.e(TAG, "onActivityResume");
    }

    private boolean requestPermissions() {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            granted = false;
            if (mActivity != null) {
                if (!checkPermissions(mActivity, permissions)) {
                    ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_CODE_MAPVIEW);
                } else {
                    granted = true;
                }
            }
        }
        return granted;
    }

    @Override
    public void onActivityDestroy() {
        onActivityPause();
        if (mMapView != null) {
            mMapView.setVisibility(View.GONE);
            mMapView.onDestroy();
            mMapContainer.removeView(mMapView);
            mAMap.clear();
            if(mMarkerMgr != null) {
                mMarkerMgr.destroy();
            }
        }
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        WXLogUtils.e(TAG, "onActivityDestroy");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_MAPVIEW:
                for(int i= 0; i< permissions.length; i++) {
                    String preName = permissions[i];
                    int granted = grantResults[i];
                    if("android.permission.ACCESS_FINE_LOCATION".equals(preName) && granted == PackageManager.PERMISSION_GRANTED) {
                        showMyLocation(true);
                    }
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean checkPermissions(Activity context, String[] permissions) {
        boolean granted = true;
        if (permissions != null && permissions.length > 0) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                        // 未得到权限
                    }
                }
            }
        }
        return granted;
    }

    private void execPaddingTasks() {
        while (!paddingTasks.isEmpty()) {
            MapOperationTask task = paddingTasks.poll();
            if (task != null && mMapView != null) {
                WXLogUtils.d(TAG, "Exec padding task " + task.toString());
                task.execute(mMapView);
            }
        }
    }

    public void postTask(MapOperationTask task) {
        if (mMapView != null && isMapLoaded.get()) {
            WXLogUtils.d(TAG, "Exec task " + task.toString());
            task.execute(mMapView);
        } else {
            WXLogUtils.d(TAG, "Padding task " + task.toString());
            paddingTasks.offer(task);
        }
    }

    interface MapOperationTask {
        void execute(WXMapView mapView);
    }

    class Point {
        private double latitude;
        private double longitude;

        public Point() {}

        public void update(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }


}
