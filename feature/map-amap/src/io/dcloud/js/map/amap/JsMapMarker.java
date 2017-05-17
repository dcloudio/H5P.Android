package io.dcloud.js.map.amap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.js.map.amap.adapter.IFJsOverlay;
import io.dcloud.js.map.amap.adapter.MapMarker;

import org.json.JSONArray;

import com.amap.api.maps.MapView;

/**
 * <p>
 * Description:对应JS Marker对象
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-10-31 下午3:19:54 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午3:19:54
 * </pre>
 */
class JsMapMarker extends JsMapObject implements IFJsOverlay {

	private MapMarker mMapMarker;

	/**
	 * Description: 构造函数
	 * 
	 * @param pFrameView
	 * @param pJsId
	 * 
	 *            <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:48:42
	 * </pre>
	 */
	public JsMapMarker(IWebview pWebview) {
		super(pWebview);
	}

	private void init(JsMapPoint pMapPoint) {
		mMapMarker = new MapMarker(pMapPoint.getMapPoint(), mWebview);
		mMapMarker.setUuid(mUUID);
	}
	@Override
	protected void createObject(JSONArray pJsArgs) {
		JsMapPoint _point = JsMapManager.getJsMapManager().getMapPoint(
				mWebview, JSONUtil.getJSONObject(pJsArgs,0));
		init(_point);
	}

	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
		if ("setPoint".equals(pStrEvent)) {
			JsMapPoint _point = JsMapManager.getJsMapManager().getMapPoint(
					mWebview, JSONUtil.getJSONObject(pJsArgs,0));
			mMapMarker.setMapPoint(_point.getMapPoint());
		} else if ("setLabel".equals(pStrEvent)) {
			mMapMarker.setLabel(JSONUtil.getString(pJsArgs,0));
		} else if ("setBubble".equals(pStrEvent)) {
			mMapMarker.setBubble(JSONUtil.getString(pJsArgs,0),JSONUtil.getString(pJsArgs,1), pJsArgs.optBoolean(4));	
		} else if ("setIcon".equals(pStrEvent)) {
			mMapMarker.setIcon(JSONUtil.getString(pJsArgs,0));
		} else if ("setBubbleIcon".equals(pStrEvent)) {
			mMapMarker.setBubbleIcon(JSONUtil.getString(pJsArgs,0));
		} else if ("setBubbleLabel".equals(pStrEvent)) {
			mMapMarker.setBubbleLabel(JSONUtil.getString(pJsArgs,0));
		} else if ("hide".equals(pStrEvent)) {
			mMapMarker.hide();
		} else if ("show".equals(pStrEvent)) {
			mMapMarker.show();
		} else if ("bringToTop".equals(pStrEvent)) {
			mMapMarker.bringToTop();
		} else if ("hideBubble".equals(pStrEvent)) {
			mMapMarker.hideBubble();
		} else if ("setIcons".equals(pStrEvent)) {
			JSONArray object = JSONUtil.getJSONArray(pJsArgs,0);
			int period = pJsArgs.optInt(1);
			mMapMarker.setIcons(object, period);
		} else if ("setDraggable".equals(pStrEvent)) {
			mMapMarker.setDraggable(pJsArgs.optBoolean(0));
		} else if ("loadImage".equals(pStrEvent)) {
			mMapMarker.loadImage(JSONUtil.getString(pJsArgs, 0));
		} else if ("loadImageDataURL".equals(pStrEvent)) {
			mMapMarker.loadImageDataURL(JSONUtil.getString(pJsArgs, 0));
		}
	}

	@Override
	public Object getMapOverlay() {
		return mMapMarker;
	}
	
	@Override
	public void onAddToMapView(MapView pMapView) {
		// TODO Auto-generated method stub
		super.onAddToMapView(pMapView);
		mMapMarker.initMapMarker(pMapView);
	}

}
