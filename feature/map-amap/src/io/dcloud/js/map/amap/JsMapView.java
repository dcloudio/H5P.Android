package io.dcloud.js.map.amap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.js.map.amap.adapter.DHMapFrameItem;
import io.dcloud.js.map.amap.adapter.IFJsOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	 * map的frame
	 */
	private DHMapFrameItem mMapFrameItem;
	
	/**
	 * Description: 构造函数 
	 * @param pWebViewImpl
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
	protected void updateObject(String pStrEvent, final JSONArray pJsArgs) {
		switch (pStrEvent) {
			case "centerAndZoom": {
				JsMapPoint _mapPoint = JsMapManager.getJsMapManager().getMapPoint(mWebview, JSONUtil.getJSONObject(pJsArgs,0));
				mMapFrameItem.centerAndZoom(_mapPoint.getMapPoint(), JSONUtil.getString(pJsArgs,1));
				break;
			}
			case "setCenter": {
				JsMapPoint _mapPoint = JsMapManager.getJsMapManager().getMapPoint(mWebview, JSONUtil.getJSONObject(pJsArgs,0));
				mMapFrameItem.setCenter(_mapPoint.getMapPoint());
				break;
			}
			case "setZoom": {
				mMapFrameItem.setZoom(JSONUtil.getString(pJsArgs,0));
				break;
			}
			case "reset": {
				mMapFrameItem.reset();
				break;
			}
			case "show": {
				mMapFrameItem.show();
				break;
			}
			case "hide": {
				mMapFrameItem.hide();
				break;
			}
			case "setMapType": {
				mMapFrameItem.setMapType(JSONUtil.getString(pJsArgs,0));
				break;
			}
			case "setTraffic": {
				mMapFrameItem.setTraffic(Boolean.parseBoolean(JSONUtil.getString(pJsArgs,0)));
				break;
			}
			case "showUserLocation": {
				PermissionUtil.usePermission(mWebview.getActivity(), mWebview.obtainApp().isStreamApp(), PermissionUtil.PMS_LOCATION, new PermissionUtil.StreamPermissionRequest(mWebview.obtainApp()) {
					@Override
					public void onGranted(String streamPerName) {
						if(PermissionUtil.PMS_LOCATION.equals(streamPerName)) {
							mMapFrameItem.setShowUserLocation(JSONUtil.getString(pJsArgs,0));
						}
					}

					@Override
					public void onDenied(String streamPerName) {
					}
				});
				break;
			}
			case "showZoomControls": {
				mMapFrameItem.setShowZoomControls(JSONUtil.getString(pJsArgs,0));
				break;
			}
			case "addOverlay": {
				JsMapObject _jsMapObj = JsMapManager.getJsMapManager().getJsObject(JSONUtil.getString(pJsArgs,0));
				if(_jsMapObj instanceof IFJsOverlay){
					_jsMapObj.onAddToMapView(mMapFrameItem.getMapView());
					mMapFrameItem.addOverlay(((IFJsOverlay) _jsMapObj).getMapOverlay());
				}
				break;
			}
			case "addRoute": {
				JsMapObject _jsMapObj = JsMapManager.getJsMapManager().getJsObject(JSONUtil.getString(pJsArgs,0));
				if(_jsMapObj instanceof IFJsOverlay){
					mMapFrameItem.addOverlay(((IFJsOverlay) _jsMapObj).getMapOverlay());
				}
				break;
			}
			case "removeOverlay": {
				JsMapObject _jsMapObj = JsMapManager.getJsMapManager().getJsObject(JSONUtil.getString(pJsArgs,0));
				if(_jsMapObj instanceof IFJsOverlay){
					mMapFrameItem.removeOverlay(((IFJsOverlay) _jsMapObj).getMapOverlay());
				}
				break;
			}
			case "getUserLocation": {
				final String callBackId = JSONUtil.getString(pJsArgs,0);
				PermissionUtil.usePermission(mWebview.getActivity(), mWebview.obtainApp().isStreamApp(), PermissionUtil.PMS_LOCATION, new PermissionUtil.StreamPermissionRequest(mWebview.obtainApp()) {
					@Override
					public void onGranted(String streamPerName) {
						if(PermissionUtil.PMS_LOCATION.equals(streamPerName)) {
							if(pJsArgs.length() > 1) {
								String uuid = JSONUtil.getString(pJsArgs, 1);
								IWebview cWebview = JsMapManager.getJsMapManager().findWebviewByUuid(mWebview, uuid);
								if(cWebview != null) {
									mMapFrameItem.getUserLocation(cWebview, callBackId);
									return;
								}
							}
							mMapFrameItem.getUserLocation(mWebview, callBackId);
						}
					}

					@Override
					public void onDenied(String streamPerName) {
						String _json = DOMException.toJSON(DOMException.CODE_GEOLOCATION_PERMISSION_ERROR, DOMException.MSG_GEOLOCATION_PERMISSION_ERROR);
						JSUtil.execCallback(mWebview, callBackId, _json, JSUtil.ERROR, true, false);
					}
				});
				break;
			}
			case "clearOverlays": {
				mMapFrameItem.clearOverlays();
				break;
			}
			case "resize": {
				mMapFrameItem.resize(pJsArgs);
				break;
			}
			case "getCurrentCenter": {
				String callBackId = JSONUtil.getString(pJsArgs,0);
				if(pJsArgs.length() > 1) {
					String uuid = JSONUtil.getString(pJsArgs, 1);
					IWebview cWebview = JsMapManager.getJsMapManager().findWebviewByUuid(mWebview, uuid);
					if(cWebview != null) {
						mMapFrameItem.getCurrentCenter(cWebview, callBackId);
						return;
					}
				}
				mMapFrameItem.getCurrentCenter(mWebview, callBackId);
				break;
			}
		}
	}

	/**
	 * Description:设置显现层的mapview
	 * @param pMapFrameItem
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
		if(pJsArgs.length() > 4) {
			appendToFrameView((AdaFrameView) mWebview.obtainFrameView());
		}
	}
	@Override
	public void dispose() {
		mMapFrameItem.dispose();
	}

	@Override
	public void close() {
		mMapFrameItem.close();
	}

	@Override
	protected String updateObjectSYNC(String pStrEvent, JSONArray pJsArgs) {
		String ret = super.updateObjectSYNC(pStrEvent, pJsArgs);
		if("getBounds".equals(pStrEvent)){
			ret = JSUtil.wrapJsVar(mMapFrameItem.getBounds(),false);
		}
		return ret;
	}

	public void appendToFrameView(AdaFrameView frameView) {
		mMapFrameItem.appendToFrameView(frameView);
	}

	public JSONObject getJsJsonMap() {
		JSONObject object = new JSONObject();
		if(mMapFrameItem != null) {
			try {
				object.put("uuid", mMapFrameItem.mUUID);
				object.put("options", mMapFrameItem.getMapOptions());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return object;
		}
		return null;
	}

	public void setStyles(JSONObject styles) {
		if(mMapFrameItem != null) {
			mMapFrameItem.setStyles(styles);
		}
	}

	public void setCallBackWebUuid(String uuid) {
		if(mMapFrameItem != null && mMapFrameItem.getMapView() != null) {
			mMapFrameItem.getMapView().addMapCallBackWebUuid(uuid);
		}
	}
}
