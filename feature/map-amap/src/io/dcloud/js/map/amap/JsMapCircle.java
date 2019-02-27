package io.dcloud.js.map.amap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.js.map.amap.adapter.IFJsOverlay;
import io.dcloud.js.map.amap.adapter.MapCircleProxy;

import org.json.JSONArray;

import com.amap.api.maps.MapView;
import com.amap.api.maps.TextureMapView;

/**
 * <p>Description:JS中的圆对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-12-25 下午1:56:36 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 下午1:56:36</pre>
 */
class JsMapCircle extends JsMapObject implements IFJsOverlay{

	private MapCircleProxy mMapCircle;
	public JsMapCircle(IWebview pWebview){
		super(pWebview);
	}
	
	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
		if("setCenter".equals(pStrEvent)){
			JsMapPoint _center = JsMapManager.getJsMapManager().getMapPoint(mWebview, JSONUtil.getJSONObject(pJsArgs,0));
			mMapCircle.setCenter(_center.getMapPoint());
		}else if("setRadius".equals(pStrEvent)){
			mMapCircle.setRadius(Double.parseDouble(JSONUtil.getString(pJsArgs,0)));
		}else if("setStrokeColor".equals(pStrEvent)){
			mMapCircle.setStrokeColor(JsMapManager.hexString2Int(JSONUtil.getString(pJsArgs,0)));
		}else if("setStrokeOpacity".equals(pStrEvent)){
			mMapCircle.setStrokeOpacity(Float.parseFloat(JSONUtil.getString(pJsArgs,0)));
		}else if("setFillColor".equals(pStrEvent)){
			mMapCircle.setFillColor(JsMapManager.hexString2Int(JSONUtil.getString(pJsArgs,0)));
		}else if("setFillOpacity".equals(pStrEvent)){
			mMapCircle.setFillOpacity(Float.parseFloat(JSONUtil.getString(pJsArgs,0)));
		}else if("setLineWidth".equals(pStrEvent)){
			mMapCircle.setLineWidth(Float.parseFloat(JSONUtil.getString(pJsArgs,0)));
		}
		if(mMapView != null){
			mMapView.refreshDrawableState();
		}
	}

	@Override
	protected void createObject(JSONArray pJsArgs) {
		JsMapPoint _jsMapPoint = JsMapManager.getJsMapManager().getMapPoint(mWebview, JSONUtil.getJSONObject(pJsArgs,0));
		int _rad = Integer.parseInt(JSONUtil.getString(pJsArgs,1));
		mMapCircle = new MapCircleProxy(_jsMapPoint.getMapPoint(), _rad);
	}

	@Override
	public Object getMapOverlay() {
		return mMapCircle;
	}
	
	@Override
	public void onAddToMapView(TextureMapView pMapView) {
		super.onAddToMapView(pMapView);
		mMapCircle.initMapCircle(pMapView);
	}
}
