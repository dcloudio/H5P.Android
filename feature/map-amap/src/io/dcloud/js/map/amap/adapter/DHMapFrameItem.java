package io.dcloud.js.map.amap.adapter;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ITypeofAble;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.ViewRect;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.map.amap.IFMapDispose;
import io.dcloud.js.map.amap.JsMapObject;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;


/**
 * <p>Description:MapFrameItem继承FrameItem
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-12-25 上午11:21:17 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 上午11:21:17</pre>
 */
public class DHMapFrameItem extends AdaFrameItem implements IFMapDispose,ISysEventListener,ITypeofAble{

	/**
	 * 设置地图的中心点
	 */
	private MapPoint mCenter;
	/**
	 * 显示地图缩放级别
	 */
	private int mZoom = 12;
	/**
	 * 设置地图类型
	 */
	private String mapType;
	/**
	 * 在地图中显示用户位置信息
	 */
	private boolean mShowUserLocation;
	/**
	 * 设置是否显示地图内置缩放控件
	 */
	private boolean mShowZoomControls;
	/**
	 * 否打开地图交通信息图层
	 */
	private boolean mTraffic;
	/**
	 * 地图上添加的所有overlay
	 */
	private ArrayList<Object> mOverlaysId;
	/**
	 * 显示的地图对象
	 */
	private DHMapView mMapView;
	/**
	 * 地图内handler（用来处理map的UI）
	 */
	private MapHandler mMapHandler;
	/**
	 * 封装的webview
	 */
	private IWebview mWebview;
	public String mUUID;
	
	JsMapObject mJsMapView = null;
	/**
	 * Handler处理message常量
	 */
	public static final int MSG_CREATE = 0;
	public static final int MSG_SCALE = 1;
	public static final int MSG_UPDATE_CENTER = 2;
	public static final int MSG_ADD_OVERLAY = 3;
	public static final int MSG_REMOVE_OVERLAY = 4;
	public static final int MSG_CLEAR_OVERLAY = 5;
	public static final int MSG_RESET = 6;
	public static final int MSG_SHOWLOCATION = 7;
	public static final int MSG_SET_MAPTYPE = 8;
	public static final int MSG_VISIBLE = 9;
	public static final int MSG_SHOWZOOMCONTROLS = 10;
	public static final int MSG_CENTER_ZOOM = 11;
	
	/**
	 * Description: 构造函数 
	 * @param pContext 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 上午11:24:14</pre>
	 */
	public DHMapFrameItem(Context pContext,IWebview pWebview,JsMapObject jsMapObject) {
		super(pContext);
		mWebview = pWebview;
		mJsMapView = jsMapObject;
		mMapHandler = new MapHandler(Looper.getMainLooper());
		mOverlaysId = new ArrayList<Object>();
		IApp app = mWebview.obtainFrameView().obtainApp(); 
		app.registerSysEventListener(this, ISysEventListener.SysEventType.onPause);
		app.registerSysEventListener(this, ISysEventListener.SysEventType.onResume);
		app.registerSysEventListener(this, ISysEventListener.SysEventType.onStop);
		app.registerSysEventListener(this, ISysEventListener.SysEventType.onSaveInstanceState);
	}
	
	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {
        if (!PdrUtil.isEmpty(mMapView)) {

            if (pEventType == ISysEventListener.SysEventType.onPause) {
                mMapView.onPause();
                mMapView.locationStop();
                return true;
            } else if (pEventType == ISysEventListener.SysEventType.onResume) {
                mMapView.onResume();
                mMapView.locationReStart();
                return true;
            } else if (pEventType == ISysEventListener.SysEventType.onStop) {
                mMapView.onDestroy();
                return true;
            } else if (pEventType == ISysEventListener.SysEventType.onSaveInstanceState) {
                mMapView.onSaveInstanceState((Bundle) pArgs);
                return true;
            }
        }
		return false;
	}
	
	/**
	 * 
	 * Description:设置地图
	 * @param pMapView
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-26 下午12:03:26</pre>
	 */
	private void setMapView(DHMapView pMapView){
		mMapView = pMapView;
		setMainView(mMapView);
		mJsMapView.onAddToMapView(mMapView);
		mMapView.onCreate(null);
	}
	public DHMapView getMapView(){
		return mMapView;
	}
	
	/**
	 * @param pCenter the center to set
	 */
	public void setCenter(MapPoint pCenter) {
		if(pCenter != null){
			this.mCenter = pCenter;
			Message m = Message.obtain();
			m.what = MSG_UPDATE_CENTER;
			m.obj = this.mCenter;
			mMapHandler.sendMessage(m);
		}
	}
	/**
	 * @param mZoom the zoom to set
	 */
	public void setZoom(String pZoom) {
		
		int _zoom = 0;
		try{
			_zoom = Integer.parseInt(pZoom == null ? "12":pZoom);
			this.mZoom = _zoom;
			Message m = Message.obtain();
			m.what = MSG_SCALE;
			m.obj = _zoom;
			mMapHandler.sendMessage(m);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * Description:设置地图中心点和缩放大小
	 * @param pCenter
	 * @param pZoom
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 下午12:21:27</pre>
	 */
	public void centerAndZoom(MapPoint pCenter,String pZoom) {
		int _zoom = 0;
		try{
			_zoom = Integer.parseInt(pZoom == null ? "12":pZoom);
			if (pCenter != null) {
				this.mCenter = pCenter;
			}
			this.mZoom = _zoom;
			Message m = Message.obtain();
			m.what = MSG_CENTER_ZOOM;
			Object[] _objs = new Object[]{mZoom,mCenter};
			m.obj = _objs;
			mMapHandler.sendMessage(m);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * @param pMapType the mapType to set
	 */
	public void setMapType(String pMapType) {
		this.mapType = pMapType;
		Message m = Message.obtain();
		m.what = MSG_SET_MAPTYPE;
		if("MAPTYPE_SATELLITE".equals(pMapType)){
			m.obj = DHMapView.MAPTYPE_SATELLITE;
		}else{
			m.obj = DHMapView.MAPTYPE_NORMAL;
		}
		mMapHandler.sendMessage(m);
	}
	/**
	 * @return the showUserLocation
	 */
	public boolean isShowUserLocation() {
		return mShowUserLocation;
	}
	/**
	 * @param pShowUserLocation the showUserLocation to set
	 */
	public void setShowUserLocation(String pShowUserLocation) {
		
		this.mShowUserLocation = Boolean.parseBoolean(pShowUserLocation);
		Message m = Message.obtain();
		m.what = MSG_SHOWLOCATION;
		m.obj = this.mShowUserLocation;
		mMapHandler.sendMessage(m);
	}
	
	public void getUserLocation(IWebview webview,String callBackId){
		mMapView.getUserLocation(webview,callBackId);
	}
	
	public void getCurrentCenter(IWebview webview,String callBackId) {
		mMapView.getCurrentCenter(webview, callBackId);
	}
	/**
	 * @return the showZoomControls
	 */
	public boolean isShowZoomControls() {
		return mShowZoomControls;
	}
	/**
	 * @param pShowZoomControls the showZoomControls to set
	 */
	public void setShowZoomControls(String pShowZoomControls) {
		this.mShowZoomControls = Boolean.parseBoolean(pShowZoomControls);
		Message m = Message.obtain();
		m.what = MSG_SHOWZOOMCONTROLS;
		m.obj = this.mShowZoomControls;
		mMapHandler.sendMessage(m);
	}
	/**
	 * @return the traffic
	 */
	public boolean isTraffic() {
		return mTraffic;
	}
	/**
	 * @param traffic the traffic to set
	 */
	public void setTraffic(boolean traffic) {
		this.mTraffic = traffic;
		int _type = DHMapView.MAPTYPE_UNTRAFFIC;
		if(traffic){
			_type = DHMapView.MAPTYPE_TRAFFIC;
		}
		Message m = Message.obtain();
		m.what = MSG_SET_MAPTYPE;
		m.obj = _type;
		mMapHandler.sendMessage(m);
	}
	/**
	 * 
	 * Description:添加图层对象
	 * @param Overly
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 上午11:38:53</pre>
	 */
	public void addOverlay(Object pMapOverlay){
		if(pMapOverlay != null){
			mOverlaysId.add(pMapOverlay);
			Message m = Message.obtain();
			m.what = MSG_ADD_OVERLAY;
			m.obj = pMapOverlay;
			mMapHandler.sendMessage(m);
		}
	}
	
	/**
	 * 
	 * Description:删除覆盖物对象
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 下午12:33:35</pre>
	 */
	public void removeOverlay(Object pMapOverlay){
		if(pMapOverlay != null){
			mOverlaysId.remove(pMapOverlay);
			Message m = Message.obtain();
			m.what = MSG_REMOVE_OVERLAY;
			m.obj = pMapOverlay;
			mMapHandler.sendMessage(m);
		}
	}
	
	/**
	 * 
	 * Description:清楚所有的overlays
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-2 下午3:36:57</pre>
	 */
	public void clearOverlays(){
		Message m = Message.obtain();
		m.what = MSG_CLEAR_OVERLAY;
		mMapHandler.sendMessage(m);
	}
	/**
	 * 
	 * Description:重置地图中心点和放大倍数
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 下午12:31:23</pre>
	 */
	public void reset(){
		if(mCenter != null){
			Message m = Message.obtain();
			m.what = MSG_RESET;
			Object[] _objs = new Object[]{mZoom,mCenter};
			m.obj = _objs;
			mMapHandler.sendMessage(m);
		}
	}
	/** 
	 * 
	 * Description:显示地图
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 上午9:50:51</pre>
	 */
	public void show(){
		Message m = Message.obtain();
		m.what = MSG_VISIBLE;
		m.obj = true;
		mMapHandler.sendMessage(m);
	}
	
	/**
	 * 
	 * Description:隐藏地图
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 上午9:51:01</pre>
	 */
	public void hide(){
		Message m = Message.obtain();
		m.what = MSG_VISIBLE;
		m.obj = false;
		mMapHandler.sendMessage(m);
	}
	
	
	public void createMap(JSONArray array){
		
		Message m = Message.obtain();
		m.what = MSG_CREATE;
		m.obj = array;
		mMapHandler.sendMessage(m);
		
	}
	
	public void resize(JSONArray pJsArgs){
		AdaFrameItem frameView = (AdaFrameItem)mWebview.obtainFrameView();
		ViewRect webParentViewRect = frameView.obtainFrameOptions();
		
		float scale = mWebview.getScale();
		
		int _l = (int)(Integer.parseInt(JSONUtil.getString(pJsArgs,0)) * scale) /*+ webParentViewRect.left*/;
		int _t = (int)(Integer.parseInt(JSONUtil.getString(pJsArgs,1)) * scale) /*+ webParentViewRect.top*/;
		int _w = Math.min((int)(Integer.parseInt(JSONUtil.getString(pJsArgs,2)) * scale), webParentViewRect.width);
		int _h = Math.min((int)(Integer.parseInt(JSONUtil.getString(pJsArgs,3)) * scale), webParentViewRect.height);
		Logger.d("Maps","DHMapFrameItem.resize _l=" + _l + ";_t=" + _t + ";_w=" + _w + ";_h=" + _h);
		obtainMainView().setLayoutParams(LayoutParamsUtil.createLayoutParams(_l, _t, _w, _h));
	}
	
	class MapHandler extends Handler {

		/**
		 * Description: 构造函数 
		 * @param mainLooper 
		 *
		 * <pre><p>ModifiedLog:</p>
		 * Log ID: 1.0 (Log编号 依次递增)
		 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午6:06:41</pre>
		 */
		public MapHandler(Looper looper) {
			super(looper);
		}

//		@Override
//		public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
//			handleMessage(msg);
//			if(mMapView != null){
//				mMapView.refresh();
//			}
//			return true;
//		}
		public void handleMessage(Message m) {// 处理消息
		
			switch (m.what) {
			case MSG_CREATE:
				JSONArray array = (JSONArray)m.obj;
				AdaFrameItem frameView = (AdaFrameItem)mWebview.obtainFrameView();
				ViewRect webParentViewRect = frameView.obtainFrameOptions();
				// 高德地图动态remove再add会出现花屏，所以这里去除自动出栈防止花屏
				mWebview.obtainFrameView().setNeedRender(true);
				float scale = mWebview.getScale();
				int _l = (int)(array.optInt(0) * scale) /*+ webParentViewRect.left*/;
				int _t = (int)(array.optInt(1) * scale) /*+ webParentViewRect.top*/;
				int _w = Math.min((int)(array.optInt(2) * scale), webParentViewRect.width);
				int _h = Math.min((int)(array.optInt(3) * scale), webParentViewRect.height);
				DHMapFrameItem.this.updateViewRect((AdaFrameItem)mWebview.obtainFrameView(), new int[]{_l,_t,_w,_h}, new int[]{webParentViewRect.width,webParentViewRect.height});
				Logger.d("mapview","_l=" + _l + ";_t=" + _t + ";_w=" + _w + ";_h=" + _h);
				LayoutParams _lp = LayoutParamsUtil.createLayoutParams(_l, _t,_w, _h);
				JSONObject option = array.optJSONObject(4);
				LatLng center = null;
				if (option.optJSONObject("center") != null) {
					center = new LatLng(option.optJSONObject("center").optDouble("latitude"), 
							option.optJSONObject("center").optDouble("longitude"));
				}
				int zoom = option.optInt("zoom");
				boolean isTraffic = option.optBoolean("traffic");
				boolean isZoomControls = option.optBoolean("zoomControls");
				int type = AMap.MAP_TYPE_NORMAL;
				if (option.optString("type").equals("MAPTYPE_SATELLITE")) {
					type = AMap.MAP_TYPE_SATELLITE;
				}
				DHMapView _mapView = new DHMapView(getActivity(),mWebview, center, zoom, type, isTraffic, isZoomControls);
//				mMapManager.start();
				_mapView.mUUID = mUUID;
				setMapView(_mapView);
				if(_mapView.getParent() != null){
					obtainMainView().setLayoutParams(_lp);	
				}else{
//					_mapView.initMap();
//					View frameView = ((AdaFrameItem)mWebview.obtainFrameView()).obtainMainView();
//					if(DeviceInfo.sDeviceSdkVer >= 11){
//						frameView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//					}
//					mWebview.addFrameItem(DHMapFrameItem.this,_lp);
					//将地图添加到frameView上,android 2.3地图添加到webview上为透明的
					String position = option.optString("position");
					if("absolute".equals(position)){
						mWebview.obtainFrameView().addFrameItem(DHMapFrameItem.this,_lp);
						Logger.d(Logger.MAP_TAG,"addMapView webview_name=" + mWebview.obtainFrameId());
					}else{//默认为"static",也可能为其它非法字符串
						if(DeviceInfo.sDeviceSdkVer >= 11){//使用系统默认View硬件加速，可能引起闪屏，LAYER_TYPE_HARDWARE无效
							mWebview.obtainWebview().setLayerType(View.LAYER_TYPE_NONE, null);
						}
						mWebview.addFrameItem(DHMapFrameItem.this,_lp);
					}
//					((ViewGroup)((AdaFrameItem)mWebview.obtainFrameView()).obtainMainView())
//					.addView(_mapView,_lp);
				}
				break;
			case MSG_SCALE:
				mMapView.setZoom((Integer)m.obj);
				break;
			case MSG_UPDATE_CENTER:
				mMapView.setCenter((MapPoint) m.obj);
				break;
			case MSG_ADD_OVERLAY:
				mMapView.addOverlay(m.obj);
				break;
			case MSG_REMOVE_OVERLAY:
				mMapView.removeOverlay(m.obj);
				break;
			case MSG_CLEAR_OVERLAY:
				mMapView.clearOverlays();
				break;
			case MSG_SHOWLOCATION:
				mMapView.showUserLocation((Boolean) m.obj);
				break;
			case MSG_SET_MAPTYPE:
				mMapView.setMapType((Integer)m.obj);
				break;
			case MSG_VISIBLE:
				mMapView.setVisible((Boolean)m.obj);
				break;
			case MSG_SHOWZOOMCONTROLS:
				mMapView.showZoomControls((Boolean) m.obj);
				break;
			case MSG_RESET:
			case MSG_CENTER_ZOOM:
				Object[] czobjs = (Object[]) m.obj;
				mMapView.setCenterAndZoom((MapPoint) czobjs[1], (Integer)czobjs[0]);
				break;
			}
			mMapView.refreshDrawableState();
		}
	}

	@Override
	public void onPopFromStack(boolean autoPop) {
		super.onPopFromStack(autoPop);
		if(autoPop){
			mMapView.mAutoPopFromStack = true;
		}
	}
	@Override
	public void onPushToStack(boolean autoPush) {
		super.onPushToStack(autoPush);
		if(autoPush){
			mMapView.mAutoPopFromStack = false;
			mMapView.onResume();
		}
	}
	@Override
	public void dispose() {
        if (!PdrUtil.isEmpty(mMapView)) {
            mMapView.dispose();
            mMapView.setVisible(false);
            mMapView.onDestroy();
            mMapView.mAutoPopFromStack = false;
            mMapView = null;
            mWebview.removeFrameItem(DHMapFrameItem.this);
        }
	}

	public String getBounds() {
		return mMapView.getBounds();
	}
}
