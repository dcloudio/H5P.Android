package io.dcloud.js.map.amap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.map.amap.adapter.IFJsOverlay;
import io.dcloud.js.map.amap.adapter.MapPoint;
import io.dcloud.js.map.amap.adapter.MapPolylineProxy;

import java.util.ArrayList;

import org.json.JSONArray;

import com.amap.api.maps.MapView;

/**
 * <p>
 * Description:对应JS 的polyLine
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-12-25 下午6:04:28 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 下午6:04:28
 * </pre>
 */
class JsMapPolyline extends JsMapObject implements IFJsOverlay{

	protected JsMapPolyline(IWebview pWebview) {
		super(pWebview);
	}

	private MapPolylineProxy mPolyline;
	
	@Override
	protected void createObject(JSONArray pJsArgs) {
		ArrayList<JsMapPoint> _arrayJsPoint = JsMapManager.getJsMapManager().getJsToPointArry(mWebview,JSONUtil.getString(pJsArgs,0));
		ArrayList<MapPoint> _arrayPoint = jsArrToPointArr(_arrayJsPoint);
		mPolyline = new MapPolylineProxy(_arrayPoint);
	}

	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
		if("setPath".equals(pStrEvent)){
			ArrayList<JsMapPoint> _arrayJsPoint = JsMapManager.getJsMapManager().getJsToPointArry(mWebview,JSONUtil.getString(pJsArgs,0));
			ArrayList<MapPoint> _arrayPoint = jsArrToPointArr(_arrayJsPoint);
			mPolyline.setPath(_arrayPoint);
		}else if("setStrokeColor".equals(pStrEvent)){
			mPolyline.setStrokeColor(PdrUtil.stringToColor(JSONUtil.getString(pJsArgs,0)));
		}else if("setStrokeOpacity".equals(pStrEvent)){
			mPolyline.setStrokeOpacity(PdrUtil.parseFloat(JSONUtil.getString(pJsArgs,0),0f));
		}else if("setLineWidth".equals(pStrEvent)){
			mPolyline.setLineWidth(PdrUtil.parseInt(JSONUtil.getString(pJsArgs,0),0));
		}
		if(mMapView != null){
			mMapView.refreshDrawableState();
		}
	}

	private ArrayList<MapPoint> jsArrToPointArr(ArrayList<JsMapPoint> pArrayJsPoint){
		ArrayList<MapPoint> _arrayPoint = new ArrayList<MapPoint>();
		if(pArrayJsPoint != null && pArrayJsPoint.size() > 0){
			for(int i=0; i<pArrayJsPoint.size(); i++){
				_arrayPoint.add(pArrayJsPoint.get(i).getMapPoint());
			}
		}
		return _arrayPoint;
	}

	@Override
	public Object getMapOverlay() {
		return mPolyline;
	}
	
	@Override
	public void onAddToMapView(MapView pMapView) {
		super.onAddToMapView(pMapView);
		mPolyline.initMapPolyline(pMapView);
	}
	
}
