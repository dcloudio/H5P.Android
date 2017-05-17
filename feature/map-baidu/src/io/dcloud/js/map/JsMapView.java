package io.dcloud.js.map;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.js.map.adapter.DHMapFrameItem;
import io.dcloud.js.map.adapter.IFJsOverlay;

import org.json.JSONArray;

/**
 * <p>Description:JS中的Map对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-16 上午9:58:03 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 上午9:58:03</pre>
 */
class JsMapView extends JsMapObject implements IFMapDispose{
	
	/**
	 * DIV id
	 */
	private String id;
	/**
	 * 对应JS对象ID
	 */
	private String mJsId;
	/**
	 * map的frame
	 */
	private DHMapFrameItem mMapFrameItem;
	
	/**
	 * Description: 构造函数 
	 * @param id
	 * @param jsId 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 上午11:15:48</pre>
	 */
	public JsMapView(IWebview pWebViewImpl) {
		super(pWebViewImpl);
		mMapFrameItem =  new DHMapFrameItem(pWebViewImpl.getContext(),pWebViewImpl,this);
		Logger.d(Logger.MAP_TAG,"JsMapView create DHMapFrameItem");
	}
	@Override
	void setUUID(String uuid) {
		super.setUUID(uuid);
		mMapFrameItem.mUUID = uuid;
	}
	/**
	 * 
	 * Description:初始化
	 * @param id
	 * @param jsId
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 下午3:40:34</pre>
	 */
	private void init(String id, String jsId){
		this.id = id;
	}
	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
		if("centerAndZoom".equals(pStrEvent)){
			JsMapPoint _mapPoint = JsMapManager.getJsMapManager().getMapPoint(mWebview, JSONUtil.getJSONObject(pJsArgs,0));
			mMapFrameItem.centerAndZoom(_mapPoint.getMapPoint(), JSONUtil.getString(pJsArgs,1));
		}else if("setCenter".equals(pStrEvent)){
			JsMapPoint _mapPoint = JsMapManager.getJsMapManager().getMapPoint(mWebview, JSONUtil.getJSONObject(pJsArgs,0));
			mMapFrameItem.setCenter(_mapPoint.getMapPoint());
		}else if("setZoom".equals(pStrEvent)){
			mMapFrameItem.setZoom(JSONUtil.getString(pJsArgs,0));
		}else if("reset".equals(pStrEvent)){
			mMapFrameItem.reset();
		}else if("show".equals(pStrEvent)){
			mMapFrameItem.show();
		}else if("hide".equals(pStrEvent)){
			mMapFrameItem.hide();
		}else if("setMapType".equals(pStrEvent)){
			mMapFrameItem.setMapType(JSONUtil.getString(pJsArgs,0));
		}else if("setTraffic".equals(pStrEvent)){
			mMapFrameItem.setTraffic(Boolean.parseBoolean(JSONUtil.getString(pJsArgs,0)));
		}else if("showUserLocation".equals(pStrEvent)){
			mMapFrameItem.setShowUserLocation(JSONUtil.getString(pJsArgs,0));
		}else if("showZoomControls".equals(pStrEvent)){
			mMapFrameItem.setShowZoomControls(JSONUtil.getString(pJsArgs,0));
		}else if("addOverlay".equals(pStrEvent)){
			JsMapObject _jsMapObj = JsMapManager.getJsMapManager().getJsObject(JSONUtil.getString(pJsArgs,0));
			if(_jsMapObj instanceof IFJsOverlay){
				mMapFrameItem.addOverlay(((IFJsOverlay) _jsMapObj).getMapOverlay());
				_jsMapObj.onAddToMapView(mMapFrameItem.getMapView());
			}
		}else if("addRoute".equals(pStrEvent)){
			JsMapObject _jsMapObj = JsMapManager.getJsMapManager().getJsObject(JSONUtil.getString(pJsArgs,0));
			if(_jsMapObj instanceof IFJsOverlay){
				mMapFrameItem.addOverlay(((IFJsOverlay) _jsMapObj).getMapOverlay());
			}
		}else if("removeOverlay".equals(pStrEvent)){
			JsMapObject _jsMapObj = JsMapManager.getJsMapManager().getJsObject(JSONUtil.getString(pJsArgs,0));
			if(_jsMapObj instanceof IFJsOverlay){
				mMapFrameItem.removeOverlay(((IFJsOverlay) _jsMapObj).getMapOverlay());
			}
		}else if("getUserLocation".equals(pStrEvent)){
			String callBackId = JSONUtil.getString(pJsArgs,0);
			mMapFrameItem.getUserLocation(mWebview, callBackId);
		}else if("clearOverlays".equals(pStrEvent)){
			mMapFrameItem.clearOverlays();
		}else if("resize".equals(pStrEvent)){
			mMapFrameItem.resize(pJsArgs);
		}else if ("getCurrentCenter".equals(pStrEvent)) {
			String callBackId = JSONUtil.getString(pJsArgs,0);
			mMapFrameItem.getCurrentCenter(mWebview, callBackId);
		}
	}

	@Override
	protected String updateObjectSYNC(String pStrEvent, JSONArray pJsArgs) {
		String ret = super.updateObjectSYNC(pStrEvent, pJsArgs);
		if("getBounds".equals(pStrEvent)){
			ret = JSUtil.wrapJsVar(mMapFrameItem.getBounds(),false);
		}
		return ret;
	}
	/**
	 * Description:设置显现层的mapview
	 * @param _mapView
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 下午12:12:27</pre>
	 */
	public void setMapView(DHMapFrameItem pMapFrameItem) {
		this.mMapFrameItem = pMapFrameItem;
	}

	@Override
	protected void createObject(JSONArray pJsArgs) {
		mMapFrameItem.createMap(pJsArgs);
	}
	@Override
	public void dispose() {
		mMapFrameItem.dispose();
	}
}
