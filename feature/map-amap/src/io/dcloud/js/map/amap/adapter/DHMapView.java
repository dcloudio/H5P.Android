package io.dcloud.js.map.amap.adapter;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.js.map.amap.IFMapDispose;
import io.dcloud.js.map.amap.JsMapManager;
import io.dcloud.js.map.amap.MapJsUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.PdrUtil;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.AMap.OnMapLongClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMap.OnMarkerDragListener;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.LocationSource.OnLocationChangedListener;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.VisibleRegion;

public class DHMapView extends MapView implements IFMapDispose , OnMarkerClickListener, OnInfoWindowClickListener
      , OnCameraChangeListener, OnMarkerDragListener, InfoWindowAdapter, OnMapClickListener, OnMapLongClickListener,AMapLocationListener{
	public boolean mAutoPopFromStack = false;
	static int aaaaaaaaaaa = 0;
	protected IWebview mWebView;
	/**
	 * 地图上所有覆盖图层集
	 */
	private AMap mAMap = null;
	/**
	 * marker覆盖图层集
	 */
	private ArrayList<MapMarker> mMarkersOverlay;

	private ArrayList<MapPolylineProxy> mPolylineOptionsList;
	
	private ArrayList<MapPolygonProxy> mPolygonProxiesList;
	
	private ArrayList<MapRoute> mMapRoutes;
	
	private ArrayList<MapCircleProxy> mMapCircleProxyList;

	
	/**
	 * 地图模式
	 * 地图类型：MAP_TYPE_NORMAL：普通地图，值为1; MAP_TYPE_SATELLITE：卫星地图，值为2；MAP_TYPE_NIGHT：黑夜地图，值为3。
	 */
	public static final int MAPTYPE_NORMAL = 0;
	public static final int MAPTYPE_SATELLITE = 1;
	public static final int MAPTYPE_TRAFFIC = 1001;
	public static final int MAPTYPE_UNTRAFFIC = 1002;
	
	public String mUUID = null;
	private String flag="";

	private ArrayList<String> mMapCallBackWebUuids;

	public DHMapView(Context pContext,IWebview pWebView,LatLng center, int zoom, int mapType, boolean traffic, boolean zoomControls) {
		super(pContext);flag = "我是编号：" + aaaaaaaaaaa++;
		mWebView = pWebView;
		mMapCallBackWebUuids = new ArrayList<String>();
		addMapCallBackWebUuid(pWebView.getWebviewUUID());
		onResume();
		initMap();
		if (center == null) {
			setZoom(zoom);
		} else {
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(center, zoom);
			mAMap.moveCamera(cameraUpdate);
		}

		//mAMap.camera
		mAMap.setTrafficEnabled(traffic);
		mAMap.setMapType(mapType);

		showZoomControls(zoomControls);
	}
	/**解决地图第一次出现闪一下问题*/
	boolean show = false;
	@Override
	protected void dispatchDraw(Canvas canvas) {
		if(show){
			super.dispatchDraw(canvas);
		}else{
			show = true;
			postDelayed(new Runnable(){
				@Override
				public void run() {
					invalidate();
				}}, 1);
		}
	}
	public void initMap(){
		mAMap = getMap();
		// 设置点击marker事件监听器
		mAMap.setOnMarkerClickListener(this);
		mAMap.setOnInfoWindowClickListener(this);
		mAMap.setOnCameraChangeListener(this);
		mAMap.setOnMarkerDragListener(this);
		mAMap.setInfoWindowAdapter(this);
		mAMap.setOnMapClickListener(this);
		mAMap.setOnMapLongClickListener(this);
		initMarkerOverlays();
		initUserLocationOverlay();
	}
	
	/**
	 * 
	 * Description:初始化Marker覆盖图层
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 上午10:49:35</pre>
	 */
	public void initMarkerOverlays(){
		Bitmap b = BitmapFactory.decodeStream(PlatformUtil.getResInputStream("res/point.png"));
		Drawable pointImg  = new BitmapDrawable(b);
		pointImg.setBounds(0, 0, b.getWidth(), b.getHeight());
		mMarkersOverlay = new ArrayList<MapMarker>();
		mPolylineOptionsList = new ArrayList<MapPolylineProxy>();
		mPolygonProxiesList = new ArrayList<MapPolygonProxy>();
		mMapCircleProxyList = new ArrayList<MapCircleProxy>();
		mMapRoutes = new ArrayList<MapRoute>();
	}
	/**
	 * 
	 * Description:初始化用户当前所在点
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 下午2:40:56</pre>
	 */
	public void initUserLocationOverlay() {
	
	}
	
	public void dispose() {
		mAMap.clear();
		clearMapCallBack();
	}

	/**
	 * 
	 * Description:设置地图中心点
	 * @param pCenter
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午4:30:45</pre>
	 */
	public void setCenter(final MapPoint pCenter){
		// 定位设置起点
		CameraUpdate cameraUpdate = CameraUpdateFactory.changeLatLng(pCenter.getLatLng());
		// 可视区域动画是指从当前可视区域转换到一个指定位置的可视区域的过程
		mAMap.animateCamera(cameraUpdate);
	}
	/**
	 * 
	 * Description:设置地图缩放大小
	 * @param pZoom
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午4:29:28</pre>
	 */
	public void setZoom(int pZoom){
		// 设置倍数
		CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(pZoom);
		mAMap.moveCamera(cameraUpdate);
	}
	
	public void setCenterAndZoom(MapPoint pCenter, int pZoom) {
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(pCenter.getLatLng(), pZoom);
		mAMap.animateCamera(cameraUpdate);
	}
	/**
	 * 
	 * Description:设置是否地图显示
	 * @param pIsVisible
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 下午2:15:40</pre>
	 */
	protected void setVisible(boolean pIsVisible){
		if (pIsVisible) {
			DHMapView.this.setVisibility(View.VISIBLE);
		}else {
			DHMapView.this.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 
	 * Description:添加图层对象
	 * @param pOverlay
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-1 上午11:56:38</pre>
	 */
	public void addOverlay(Object pOverlay){
		if(pOverlay instanceof MapMarker) {
			MapMarker pMarker = (MapMarker) pOverlay;
			mMarkersOverlay.add(pMarker);
			pMarker.checkPop(); //加入到地图中判断是否需要显示弹层
		} else if (pOverlay instanceof MapPolylineProxy) {
			MapPolylineProxy mapPolylineProxy = (MapPolylineProxy)pOverlay;
			mPolylineOptionsList.add(mapPolylineProxy);
		} else if (pOverlay instanceof MapPolygonProxy) {
			MapPolygonProxy polygonProxy = (MapPolygonProxy)pOverlay;
			mPolygonProxiesList.add(polygonProxy);
		} else if (pOverlay instanceof MapRoute) {
			MapRoute mapRoute = (MapRoute)pOverlay;
			mMapRoutes.add(mapRoute);
		} else if (pOverlay instanceof MapCircleProxy) {
			mMapCircleProxyList.add((MapCircleProxy)pOverlay);
		}
	}
	
	/**
	 * 
	 * Description:删除覆盖物对象
	 * @param pOverlay
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 上午10:45:15</pre>
	 */
	public void removeOverlay(Object pOverlay){
		if(pOverlay instanceof MapMarker){
			MapMarker pMarker = (MapMarker) pOverlay;
			if(mMarkersOverlay.contains(pMarker) && pMarker.getMarker() != null){
				pMarker.getMarker().remove();
				mMarkersOverlay.remove(pMarker);
			}
		} else if (pOverlay instanceof MapPolylineProxy) {
			MapPolylineProxy mapPolylineProxy = (MapPolylineProxy)pOverlay;
			if (mapPolylineProxy.getPolyline() != null) {
				mapPolylineProxy.getPolyline().remove();
				mPolylineOptionsList.remove(mapPolylineProxy);
			}
		} else if (pOverlay instanceof MapPolygonProxy) {
			MapPolygonProxy polygonProxy = (MapPolygonProxy)pOverlay;
			if (polygonProxy.getPolygon() != null) {
				polygonProxy.getPolygon().remove();
				mPolygonProxiesList.remove(polygonProxy);
			}
		} else if (pOverlay instanceof MapRoute) {
			MapRoute mapRoute = (MapRoute)pOverlay;
			mapRoute.removeFromMap();
			mMapRoutes.remove(mapRoute);
		} else if (pOverlay instanceof MapCircleProxy) {
			MapCircleProxy circleProxy = (MapCircleProxy)pOverlay;
			if (circleProxy.getCircle() != null) {
				circleProxy.getCircle().remove();
				mMapCircleProxyList.remove(circleProxy);
			}
		}
	}
	/**
	 * 
	 * Description:清除所有的overlays
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-2 下午3:36:57</pre>
	 */
	public void clearOverlays(){
		mAMap.clear();
		if (mPolygonProxiesList != null) {
			mPolygonProxiesList.clear();
			mPolylineOptionsList.clear();
			mMarkersOverlay.clear();
			mMapRoutes.clear();
		}
		refreshDrawableState();
	}
	/**
	 * 
	 * Description:设置是否显示地图内置缩放控件
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 下午12:11:55</pre>
	 */
	public void showZoomControls(boolean pDisplay ){
		mAMap.getUiSettings().setZoomControlsEnabled(pDisplay);
		
	}
	/**
	 * 
	 * Description:设置地图类型
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 上午10:35:12</pre>
	 */
	public void setMapType(int pType){
		
		switch (pType) {
		case MAPTYPE_SATELLITE:
			mAMap.setMapType(AMap.MAP_TYPE_SATELLITE);
			break;
		case MAPTYPE_TRAFFIC:
			mAMap.setTrafficEnabled(true);
			break;
		case MAPTYPE_UNTRAFFIC:
			mAMap.setTrafficEnabled(false);
			break;
		case MAPTYPE_NORMAL:
			mAMap.setMapType(AMap.MAP_TYPE_NORMAL);
			break;
//		default:
//			setSatellite(false);
//			break;
		}
	
	}

    AMapLocationClient mSULClient;
    public AMapLocationClientOption mLocationOption = null;
	/**
	 * 
	 * Description:在地图中显示用户位置信息
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 下午2:35:56</pre>
	 */
	public void showUserLocation(boolean pDisplay) {
        if (!pDisplay) {
            mAMap.setMyLocationEnabled(false);
        } else {
            mAMap.setLocationSource(mLocationSource);
            mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
            // 设置定位的类型为定位模式：定位（AMap.LOCATION_TYPE_LOCATE）、跟随（AMap.LOCATION_TYPE_MAP_FOLLOW）
            // 地图根据面向方向旋转（AMap.LOCATION_TYPE_MAP_ROTATE）三种模式
            mAMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        }
	}
    OnLocationChangedListener mChangedListener;
    LocationSource mLocationSource = new LocationSource() {

        @Override
        public void deactivate() {
            // TODO Auto-generated method stub
            if (mChangedListener != null) {
                mChangedListener = null;
            }
            disposeClientResource(mSULClient);
        }

        @Override
        public void activate(OnLocationChangedListener arg0) {
            // TODO Auto-generated method stub
            mChangedListener = arg0;
            mSULClient = new AMapLocationClient(getContext().getApplicationContext());
            mLocationOption = new AMapLocationClientOption();
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mSULClient.setLocationOption(mLocationOption);
            mSULClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    if (!PdrUtil.isEmpty(aMapLocation) && aMapLocation.getErrorCode() == 0) {
                        if (mChangedListener != null) {
                            // 移动地图到定位点
                            mChangedListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                        }
                    } else {
                        disposeClientResource(mSULClient);
                    }
                }
            });        //启动定位
            mSULClient.startLocation();

        }
    };
    IWebview tGetUserLocWebview = null;
    String tGetUserLocCallbackId = null;
    static final String GET_USER_LOCATION_TEMPLATE = "{state:%s,point:%s}" ;
    static final String PLUS_MAPS_POINT_TEMPLATE = "new plus.maps.Point(%s,%s)";

    AMapLocationClient mGULClient;
    /**
     * map.getUserLocation()
     * @param webview
     * @param callBackId
     */
    public void getUserLocation(IWebview webview,String callBackId){
        tGetUserLocWebview = webview;
        tGetUserLocCallbackId = callBackId;
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setOnceLocation(true);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        mGULClient = new AMapLocationClient(getContext().getApplicationContext());
        //设置定位回调监听
        mGULClient.setLocationListener(this);        //启动定位
        mGULClient.startLocation();
    }

    /**
     * map.getUserLocation()位置信息回调
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        String js=null;
        if (PdrUtil.isEmpty(aMapLocation)) {
            String message = DOMException.toString(DOMException.CODE_GEOLOCATION_PROVIDER_ERROR, "geolocation", DOMException.MSG_GEOLOCATION_PROVIDER_ERROR, null);
            js = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_GEOLOCATION_PROVIDER_ERROR, message);
        }else{
            js = String.format(GET_USER_LOCATION_TEMPLATE, aMapLocation.getErrorCode(), String.format(Locale.ENGLISH,PLUS_MAPS_POINT_TEMPLATE, aMapLocation.getLongitude(),aMapLocation.getLatitude()));
        }
        MapJsUtil.execCallback(tGetUserLocWebview, tGetUserLocCallbackId, js);
        disposeClientResource(mGULClient);
    }

    /**
     * 停止定位服务并销毁
     * @param aMapLocationClient
     */
    private void disposeClientResource(AMapLocationClient aMapLocationClient) {
        aMapLocationClient.stopLocation();
        aMapLocationClient.onDestroy();
    }
	public void getCurrentCenter(IWebview webview,String callBackId) {
		LatLng latLng = mAMap.getCameraPosition().target;
		String js = null;
		if (latLng != null) {
			js = String.format(GET_USER_LOCATION_TEMPLATE, 0, String.format(Locale.ENGLISH,PLUS_MAPS_POINT_TEMPLATE, latLng.longitude,latLng.latitude));
		} else {
			js = String.format(GET_USER_LOCATION_TEMPLATE, -1, String.format(Locale.ENGLISH,PLUS_MAPS_POINT_TEMPLATE, 0,0));
		}
		 
		MapJsUtil.execCallback(webview, callBackId, js);
	}

	
	static final int SCAN_SPAN_TIME = 10000;
	public static boolean isRightLocation(double lat,double lng){
		return lat != 4.9E-324 && lng != 4.9E-324;
	}

	

	// 标记 是否需要还原定位服务 用于页面暂停在回来时的逻辑
	private boolean isLoctionReduction = false;
	/**
	 * 判断如果当前处于定位状态，则停止定位
	 */
	public void locationStop() {
		if (mChangedListener != null) { // mChangedListener不为空表示 正在长时间定位
            showUserLocation(false);
			isLoctionReduction = true;
		}
	}
	/**
	 * 判断是否需要还原定位，如果需要还原定位咋打开定位服务
	 */
	public void locationReStart() {
		if (isLoctionReduction) { // mChangedListener不为空表示 正在长时间定位
			showUserLocation(true);
			isLoctionReduction = false;
		}
	}
	

	
	@Override
	protected void onDetachedFromWindow() {
		if(!mAutoPopFromStack){//当执行自动出栈逻辑致使mapview丢失Window，不调用真正的onDetachedFromWindow处理逻辑
			super.onDetachedFromWindow();
		}
	}
	
	@Override
	public boolean onMarkerClick(Marker arg0) {
		// TODO Auto-generated method stub
		mClickMapMarker = getMapMarker(arg0);
		if (mClickMapMarker != null) {
			if(!TextUtils.isEmpty(mClickMapMarker.getBubbleLabel())) {
				if(!mClickMapMarker.getMarker().isInfoWindowShown()) {
					mClickMapMarker.getMarker().showInfoWindow();
				} else {
					mClickMapMarker.getMarker().hideInfoWindow();
				}
			}
			MapJsUtil.execCallback(mClickMapMarker.getWebview(), mClickMapMarker.getUuid(), "{type:'markerclick'}");
		}
		return true;
	}

	@Override
	public void onInfoWindowClick(Marker arg0) {
		// TODO Auto-generated method stub
		arg0.hideInfoWindow();
		MapMarker mapMarker = getMapMarker(arg0);
		if (mapMarker != null) {
			/*String iconPath = mapMarker.getBubbleIcon();
			if(iconPath != null){
				try {
					InputStream is = mWebView.obtainFrameView().obtainApp().obtainResInStream(mWebView.obtainFullUrl(),iconPath);
					Options o = new Options();
					o.inScaled = false;
					Bitmap b = BitmapFactory.decodeStream( is,null,o);
					show |= true;
				} catch (Exception e) {
					show |= false;
				}
			}*/
			MapJsUtil.execCallback(mapMarker.getWebview(), mapMarker.getUuid(), "{type:'bubbleclick'}");
		}
	}
	
	/**
	 * 获取MapMarker 
	 * @param marker
	 * @return
	 */
	private MapMarker getMapMarker(Marker marker) {
		for (MapMarker mapMarker : mMarkersOverlay) {
			if (marker.equals(mapMarker.getMarker())) {
				return mapMarker;
			}
		}
		return null;
	}
	@Override
	public void onCameraChange(CameraPosition arg0) {
		// TODO Auto-generated method stub
		
	}
	/**
	 * 地图状态改变回调
	 */
	private static final String MAP_STATUS_CHANGE = "{ " +
			"callbackType:'%s'" +
			",center:{long:%f,lat:%f}" +
			",northease:{long:%f,lat:%f}" +
			",southwest:{long:%f,lat:%f}" +
			",zoom:%f" +
			"}";
	@Override
	public void onCameraChangeFinish(CameraPosition arg0) {
		// TODO Auto-generated method stub
		// 用户对地图做出一系列改变地图可视区域的操作（如拖动、动画滑动、缩放）完成之后回调此方法。
		try {
			VisibleRegion vr = mAMap.getProjection().getVisibleRegion();
			execCallBack(String.format(Locale.ENGLISH,MAP_STATUS_CHANGE, "change",arg0.target.longitude,
					arg0.target.latitude, vr.farRight.longitude, vr.farRight.latitude,
					vr.nearLeft.longitude, vr.nearLeft.latitude,arg0.zoom));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void onMarkerDrag(Marker arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onMarkerDragEnd(Marker arg0) {
		// TODO Auto-generated method stub
		MapMarker mapMarker = getMapMarker(arg0);
		if (mapMarker != null) {
			String code = "{" +
					"type:'onDrag'" +
					",pt:new plus.maps.Point(%f, %f)" +
					"}";
			LatLng latLng = arg0.getPosition();
			MapJsUtil.execCallback(mapMarker.getWebview(), mapMarker.getUuid(), String.format(Locale.ENGLISH,code, latLng.longitude, latLng.latitude));
		}
	}
	@Override
	public void onMarkerDragStart(Marker arg0) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 监听自定义infowindow窗口的infocontents事件回调
	 */
	@Override
	public View getInfoContents(Marker arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 监听自定义infowindow窗口的infowindow事件回调
	 */
	@Override
	public View getInfoWindow(Marker arg0) {
		// TODO Auto-generated method stub
		PopViewLayout viewLayout = null;
		mClickMapMarker = getMapMarker(arg0);
		if (mClickMapMarker != null) {
			if (!TextUtils.isEmpty(mClickMapMarker.getLoadImageDataURL())) {
				ImageView imageView = new ImageView(getContext());
				mClickMapMarker.base64ToBitmap(imageView);
				return imageView;
			} else if (!TextUtils.isEmpty(mClickMapMarker.getLoadImage())) {
				ImageView imageView = new ImageView(getContext());
				mClickMapMarker.loadImageBitmap(imageView);
				return imageView;
			}
			viewLayout = new PopViewLayout(getContext(),mClickMapMarker.getBubbleLabel(), mClickMapMarker.getPopIcon());
		}
		return viewLayout;
	}
	
	/**
	 * 被点击的marker
	 */
	private MapMarker mClickMapMarker;
	
	private static final String POINT_CLICK_TEMPLATE = "{" +
	"callbackType:'%s'" +
	",payload:new plus.maps.Point(%f, %f)" +   	
	"}";
	/**
	 * 地图单击事件
	 * @param arg0
	 */
	@Override
	public void onMapClick(LatLng arg0) {
		// TODO Auto-generated method stub
		if (mClickMapMarker != null && mClickMapMarker.getMarker().isInfoWindowShown()) {
			mClickMapMarker.getMarker().hideInfoWindow();
		}
		execCallBack(String.format(Locale.ENGLISH, POINT_CLICK_TEMPLATE, "click",arg0.longitude, arg0.latitude));
	}
	@Override
	public void onMapLongClick(LatLng arg0) {
		// TODO Auto-generated method stub
		execCallBack(String.format(Locale.ENGLISH, POINT_CLICK_TEMPLATE, "click",arg0.longitude, arg0.latitude));
	}
	/**
	 * 地图状态改变回调
	 */
	private static final String T_GETBOUNDS = "{ " +
			"northease:{longitude:%f,latitude:%f}" +
			",southwest:{longitude:%f,latitude:%f}" +
			"}";
	
	public String getBounds() {
		VisibleRegion vr = mAMap.getProjection().getVisibleRegion();
		return String.format(Locale.ENGLISH, T_GETBOUNDS,
				vr.farRight.longitude, vr.farRight.latitude,
				vr.nearLeft.longitude, vr.nearLeft.latitude);
	}

	public void addMapCallBackWebUuid(String uuid) {
		if(!mMapCallBackWebUuids.contains(uuid))
			mMapCallBackWebUuids.add(uuid);
	}

	public void clearMapCallBack() {
		if(mMapCallBackWebUuids != null) {
			mMapCallBackWebUuids.clear();
		}
	}

	private void execCallBack(String pMessage) {
		if(mMapCallBackWebUuids != null) {
			for(String uuid: mMapCallBackWebUuids) {
				IWebview webview = JsMapManager.getJsMapManager().findWebviewByUuid(mWebView, uuid);
				if(webview != null) {
					MapJsUtil.execCallback(webview, mUUID, pMessage);
				}
			}
		}
	}
	
}
