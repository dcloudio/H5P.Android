package io.dcloud.js.map.adapter;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.ErrorDialogUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.map.IFMapDispose;
import io.dcloud.js.map.MapInitImpl;
import io.dcloud.js.map.MapJsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapDoubleClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapLongClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerDragListener;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Text;
import com.baidu.mapapi.model.LatLng;
import com.baidu.vi.VMsg;

public class DHMapView implements IFMapDispose, OnMarkerClickListener, OnMapClickListener, OnMapStatusChangeListener
       ,OnMarkerDragListener, OnMapDoubleClickListener, OnMapLongClickListener{
	public boolean mAutoPopFromStack = false;
	static int aaaaaaaaaaa = 0;
	protected IWebview mWebView;
	
	/**
	 * 地图模式
	 */
	public static final int MAPTYPE_NORMAL = 0;
	public static final int MAPTYPE_SATELLITE = 1;
	public static final int MAPTYPE_TRAFFIC = 1001;
	public static final int MAPTYPE_UNTRAFFIC = 1002;
	
	public String mUUID = null;
	private String flag="";
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	// 定位图标 默认为空
	BitmapDescriptor mCurrentMarker;
	
	/**
	 * marker覆盖图层集
	 */
	private ArrayList<MapMarker> mMarkersOverlay;
	private HashMap<Marker, MapMarker> mMarkersMap;
	
	private ArrayList<MapPolylineProxy> mPolylineOptionsList;
	
	private ArrayList<MapPolygonProxy> mPolygonProxiesList;
	
	private ArrayList<MapRoute> mMapRoutes;
	
	private ArrayList<MapCircleProxy> mMapCircleProxyList;
	
	public DHMapView(Context pContext,IWebview pWebView, LatLng center, int zoom, int mapType, boolean traffic, boolean zoomControls) {
		flag = "我是编号：" + aaaaaaaaaaa++;
		mWebView = pWebView;
		BaiduMapOptions options = new BaiduMapOptions();
		MapStatus status = new MapStatus.Builder().target(center).zoom(zoom).build();
		options.mapStatus(status);
		mMapView = new MapView(pContext, options);
		mMapView.showZoomControls(zoomControls);
		VMsg.init();
		initMap();
		mBaiduMap.setMapType(mapType);
		mBaiduMap.setTrafficEnabled(traffic);
		if (MapInitImpl.isKeyError && BaseInfo.ISDEBUG) {
			String msg = "配置的百度地图密钥（appkey）校验失败，参考http://ask.dcloud.net.cn/article/29";
			Dialog dialog = ErrorDialogUtil.getLossDialog(pWebView, msg, "http://ask.dcloud.net.cn/article/29", "地图KEY");
			if (dialog != null) {
				dialog.show();
			}
		}
	}
	Text mText;

	/**
	 * 注销KEY广播监听
	 *
	 */
//	public void unReceiver(IWebview pWebView) {
//		pWebView.getActivity().unregisterReceiver(mReceiver);
//	}
	
	public IWebview getWebview() {
		return mWebView;
	}
	public MapView getMapView() {
		return mMapView;
	}
	
	public BaiduMap getBaiduMap() {
		return mBaiduMap;
	}
	/**解决地图第一次出现闪一下问题*//*
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
	}*/
	public void initMap(){
		mBaiduMap = mMapView.getMap();
		initOverlays();
		mBaiduMap.setOnMarkerClickListener(this);
		mBaiduMap.setOnMapClickListener(this);
		mBaiduMap.setOnMapStatusChangeListener(this);
		mBaiduMap.setOnMarkerDragListener(this);
		mBaiduMap.setOnMapDoubleClickListener(this);
		mBaiduMap.setOnMapLongClickListener(this);
	}
	
	/**
	 * 
	 * Description:初始化Marker覆盖图层
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 上午10:49:35</pre>
	 */
	public void initOverlays(){
		mMarkersOverlay = new ArrayList<MapMarker>();
		mMarkersMap = new HashMap<Marker, MapMarker>();
		mPolylineOptionsList = new ArrayList<MapPolylineProxy>();
		mPolygonProxiesList = new ArrayList<MapPolygonProxy>();
		mMapCircleProxyList = new ArrayList<MapCircleProxy>();
		mMapRoutes = new ArrayList<MapRoute>();
	}
	
	public void dispose() {
		try {
			if (mLocClient != null) {
				mLocClient.unRegisterLocationListener(myListener);
			}
            //clearOverlays();
            if (!PdrUtil.isEmpty(mMapView)) {
                //mMapView.setVisibility(View.GONE);//释放资源并不会关闭地图不显示，所以释放之前使之隐藏
                mMapView.onDestroy();
                mMapView = null;
            }
        } catch(Exception e) {
			//e.printStackTrace();
		}
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
	public void setCenter(final LatLng pCenter){
		try{
			MapStatus ms = new MapStatus.Builder().target(pCenter).build();
			MapStatusUpdate u = MapStatusUpdateFactory.newMapStatus(ms);
			mBaiduMap.setMapStatus(u);
		}catch(Exception e){
			e.printStackTrace();
		}
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
		MapStatus ms = new MapStatus.Builder().zoom(pZoom).build();
		MapStatusUpdate u = MapStatusUpdateFactory.newMapStatus(ms);
		mBaiduMap.setMapStatus(u);
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
			mMapView.setVisibility(View.VISIBLE);
		}else {
			mMapView.setVisibility(View.GONE);
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
	public void addOverlay(Object pOverlay) {
		if (pOverlay instanceof MapMarker) {
			MapMarker pMarker = (MapMarker) pOverlay;
			mMarkersOverlay.add(pMarker);
			mMarkersMap.put(pMarker.getMarkerOverlay(), pMarker);
		} else if (pOverlay instanceof MapPolylineProxy) {
			mPolylineOptionsList.add((MapPolylineProxy) pOverlay);
		} else if (pOverlay instanceof MapPolygonProxy) {
			mPolygonProxiesList.add((MapPolygonProxy) pOverlay);
		} else if (pOverlay instanceof MapRoute) {
			mMapRoutes.add((MapRoute)pOverlay);
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
		if (pOverlay instanceof MapMarker) { 
			MapMarker pMarker = (MapMarker) pOverlay;
			if (pMarker.getMarkerOverlay() != null) {
				mMarkersMap.remove(pMarker.getMarkerOverlay());
				pMarker.getMarkerOverlay().remove();
				mMarkersOverlay.remove(pMarker);
			}
		} else if (pOverlay instanceof MapPolylineProxy) {
			MapPolylineProxy proxy = (MapPolylineProxy) pOverlay;
			if (proxy.getMapPolyLine() != null) {
				proxy.getMapPolyLine().remove();
				mPolylineOptionsList.remove(proxy);
			}
		} else if (pOverlay instanceof MapPolygonProxy) {
			MapPolygonProxy polygonProxy = (MapPolygonProxy) pOverlay;
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
		mBaiduMap.clear();
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
		if (mMapView != null)
			mMapView.showZoomControls(pDisplay);
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
			mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
			break;
		case MAPTYPE_TRAFFIC:
			mBaiduMap.setTrafficEnabled(true);
			break;
		case MAPTYPE_UNTRAFFIC:
			mBaiduMap.setTrafficEnabled(false);
			break;
		case MAPTYPE_NORMAL:
			mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			break;
		}
	
	}
	/**是否执行过showUserLocation*/
	private boolean mShowUserLoc = false;
	/**
	 * 
	 * Description:在地图中显示用户位置信息
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-5 下午2:35:56</pre>
	 */
	public void showUserLocation(boolean pDisplay) {
		if (pDisplay) {
			if (mShowUserLoc) {
				return;
			}
			// LocationMode.FOLLOWING 跟随  LocationMode.NORMAL 普通  LocationMode.COMPASS 罗盘 三种样式 默认为跟随
			mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
					LocationMode.NORMAL, false, mCurrentMarker));
			if (mLocClient == null) {
				createLocClient();
			}
			// 开启定位图层
			mBaiduMap.setMyLocationEnabled(true);
			LocationClientOption option = new LocationClientOption();
			option.setOpenGps(true);// 打开gps
			option.setCoorType(COORTYPE); // 设置坐标类型 
			option.setScanSpan(SCAN_SPAN_TIME);
			mLocClient.setLocOption(option);
			mLocClient.start();
			mShowUserLoc = true;
		} else {
			if (mLocClient != null) {
				mLocClient.stop();
				mBaiduMap.setMyLocationEnabled(false);
				mShowUserLoc = false;
			}
		}
	}
	
	private void createLocClient() {
		mLocClient = new LocationClient(mWebView.getContext());
		mLocClient.registerLocationListener(myListener);
	}
	
	IWebview tGetUserLocWebview = null;
	String tGetUserLocCallbackId = null;
	static final String GET_USER_LOCATION_TEMPLATE = "{state:%s,point:%s}" ;
	static final String PLUS_MAPS_POINT_TEMPLATE = "new plus.maps.Point(%s,%s)";
	
	public void getUserLocation(IWebview webview,String callBackId){
		tGetUserLocWebview = webview;
		tGetUserLocCallbackId = callBackId;
		if (mLocClient == null) {
			createLocClient();
		}
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开gps
		option.setCoorType(COORTYPE); // 设置坐标类型 
		option.setScanSpan(SCAN_SPAN_TIME);
		mLocClient.setLocOption(option);
		if(!mLocClient.isStarted()) {
			mLocClient.start();
		}
	}
	private void userLocationCallback(IWebview webview, String callBackId,MyLocationData ld) {
		String js = String.format(GET_USER_LOCATION_TEMPLATE, 0,String.format(Locale.ENGLISH,PLUS_MAPS_POINT_TEMPLATE, ld.longitude,ld.latitude));
		MapJsUtil.execCallback(webview, callBackId, js);
	}
	
	
	
	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	/**已经定位了*/
	boolean mLocated = false;
	
	static final String COORTYPE = "bd09ll";//返回国测局经纬度坐标系：gcj02 返回百度墨卡托坐标系 ：bd09 返回百度经纬度坐标系 ：bd09ll
	static final int SCAN_SPAN_TIME = 1000;
	/**
	 * 位置监听器
	 *
	 * @version 1.0
	 * @author yanglei Email:yanglei@dcloud.io
	 * @Date 2014-4-3 上午11:58:09 created.
	 * 
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2014-4-3 上午11:58:09
	 */
	class MyLocationListenner implements BDLocationListener {
    	
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mMapView == null)
                return ;
            try{
            	MyLocationData locData = new MyLocationData.Builder()
     			.accuracy(location.getRadius())
     			// 此处设置开发者获取到的方向信息，顺时针0-360
     			.direction(100).latitude(location.getLatitude())
     			.longitude(location.getLongitude()).build();
     	        mBaiduMap.setMyLocationData(locData);
    	        if (mLocated) {
    	        	mLocated = false;
    				LatLng ll = new LatLng(location.getLatitude(),
    						location.getLongitude());
    				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
    				mBaiduMap.animateMapStatus(u);
    			}
    	        if (tGetUserLocWebview != null) {
    	        	userLocationCallback(tGetUserLocWebview, tGetUserLocCallbackId, locData);
    	        	tGetUserLocWebview = null;
    	        	if (!mShowUserLoc) {
    		        	mLocClient.stop();
    					mBaiduMap.setMyLocationEnabled(false);
    	        	}
    	        }
            }catch(Exception e){
            	e.printStackTrace();
            }
        	/*//是手动触发请求或首次定位时，移动到定位点
        	if (mLocated && !mShowUserLocEnd && mShowUserLoc){
        		//更新定位数据
        		mLocationOverlay.setData(locData);
        		//移动地图到定位点
        		mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)));
        		mShowUserLocEnd = true;
        		refresh();
        	}
        	//如果plus.maps.getUserLocation不能立刻获取当前位置，则当收到位置回调时立刻通知
        	if(tGetUserLocWebview != null){
        		userLocationCallback(tGetUserLocWebview, tGetUserLocCallbackId, locData);
        		tGetUserLocWebview = null;
        		tGetUserLocCallbackId = null;
        	}
        	if(mLocClient.isStarted()){
        		mLocClient.stop();
        	}*/
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }

        public void onReceivePoi(BDLocation poiLocation) {
            if (poiLocation == null){
                return ;
            }
        }
    }
	
	// 标记 是否需要还原定位服务 用于页面暂停在回来时的逻辑
	private boolean isLoctionReduction = false;
	/**
	 * 判断如果当前处于定位状态，则停止定位
	 */
	public void locationStop() {
		if (mShowUserLoc) {
        	mLocClient.stop();
        	mShowUserLoc = false;
			mBaiduMap.setMyLocationEnabled(false);
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
	public boolean onMarkerClick(Marker marker) {
		// TODO Auto-generated method stub
		MapMarker mapMarker = mMarkersMap.get(marker);
		if (mapMarker != null) {
			MapJsUtil.execCallback(mWebView, mapMarker.getUuid(), "{type:'markerclick'}");
			mapMarker.showInfoWindow(mBaiduMap, mWebView.getActivity(), mWebView);
			return true;
		}
		return false;
	}

	@Override
	public void onMapClick(LatLng arg0) {
		// TODO Auto-generated method stub
		MapJsUtil.execCallback(mWebView,mUUID, String.format(Locale.ENGLISH,POINT_CLICK_TEMPLATE, "click", arg0.longitude, arg0.latitude));
		mBaiduMap.hideInfoWindow();
	}

	@Override
	public boolean onMapPoiClick(MapPoi arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMapStatusChange(MapStatus arg0) {
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
	public void onMapStatusChangeFinish(MapStatus arg0) {
		// TODO Auto-generated method stub
		try {
			MapJsUtil.execCallback(mWebView,mUUID, String.format(Locale.ENGLISH,MAP_STATUS_CHANGE, "change", arg0.target.longitude, arg0.target.latitude
					, arg0.bound.northeast.longitude, arg0.bound.northeast.latitude, arg0.bound.southwest.longitude, arg0.bound.southwest.latitude,arg0.zoom));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	/**
	 * 地图状态改变回调
	 */
	private static final String T_GETBOUNDS = "{ " +
			"northease:{longitude:%f,latitude:%f}" +
			",southwest:{longitude:%f,latitude:%f}" +
			"}";
	
	public String getBounds(){
		MapStatus ms = mBaiduMap.getMapStatus();
		return String.format(Locale.ENGLISH, T_GETBOUNDS,
				ms.bound.northeast.longitude, ms.bound.northeast.latitude,
				ms.bound.southwest.longitude, ms.bound.southwest.latitude);
	}
	@Override
	public void onMapStatusChangeStart(MapStatus arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMarkerDrag(Marker arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		// TODO Auto-generated method stub
		MapMarker mapMarker = mMarkersMap.get(marker);
		if(mapMarker != null) {
			String code = "{" +
					"type:'onDrag'" +
					",pt:new plus.maps.Point(%f, %f)" +
					"}";
			LatLng latLng = marker.getPosition();
			MapJsUtil.execCallback(mWebView, mapMarker.getUuid(), String.format(Locale.ENGLISH,code, latLng.longitude, latLng.latitude));
		}
	}

	@Override
	public void onMarkerDragStart(Marker arg0) {
		// TODO Auto-generated method stub
		
	}
	private static final String POINT_CLICK_TEMPLATE = "{" +
	"callbackType:'%s'" +
	",payload:new plus.maps.Point(%f, %f)" +   	
	"}";
	@Override
	public void onMapLongClick(LatLng arg0) {
		// TODO Auto-generated method stub
		MapJsUtil.execCallback(mWebView,mUUID, String.format(Locale.ENGLISH, POINT_CLICK_TEMPLATE, "click", arg0.longitude,arg0.latitude));
	}

	@Override
	public void onMapDoubleClick(LatLng arg0) {
		// TODO Auto-generated method stub
		MapJsUtil.execCallback(mWebView,mUUID, String.format(Locale.ENGLISH, POINT_CLICK_TEMPLATE, "click", arg0.longitude,arg0.latitude));
	}

	
}
