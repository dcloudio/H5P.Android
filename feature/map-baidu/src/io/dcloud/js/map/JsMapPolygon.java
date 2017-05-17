package io.dcloud.js.map;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.map.adapter.DHMapView;
import io.dcloud.js.map.adapter.IFJsOverlay;
import io.dcloud.js.map.adapter.MapPoint;
import io.dcloud.js.map.adapter.MapPolygonProxy;

import java.util.ArrayList;

import org.json.JSONArray;

import com.baidu.mapapi.map.MapView;

/**
 * <p>
 * Description:对应js 多边形类
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-12-25 下午6:05:14 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 下午6:05:14
 * </pre>
 */
class JsMapPolygon extends JsMapObject implements IFJsOverlay {

	protected JsMapPolygon(IWebview pWebview) {
		super(pWebview);
	}

	private MapPolygonProxy mPolygon;
	@Override
	protected void createObject(JSONArray pJsArgs) {

		ArrayList<JsMapPoint> _arrayJsPoint = JsMapManager.getJsMapManager().getJsToPointArry(mWebview,JSONUtil.getString(pJsArgs,0));
		ArrayList<MapPoint> _arrayPoint = jsArrToPointArr(_arrayJsPoint);
		mPolygon = new MapPolygonProxy(_arrayPoint);
	}

	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
		if("setPath".equals(pStrEvent)){
			ArrayList<JsMapPoint> _arrayJsPoint = JsMapManager.getJsMapManager().getJsToPointArry(mWebview,JSONUtil.getString(pJsArgs,0));
			ArrayList<MapPoint> _arrayPoint = jsArrToPointArr(_arrayJsPoint);
			mPolygon.setPath(_arrayPoint);
		}else if("setStrokeColor".equals(pStrEvent)){
			mPolygon.setStrokeColor(PdrUtil.stringToColor(JSONUtil.getString(pJsArgs,0)));
		}else if("setStrokeOpacity".equals(pStrEvent)){
			mPolygon.setStrokeOpacity(Float.parseFloat(JSONUtil.getString(pJsArgs,0)));
		}else if("setFillColor".equals(pStrEvent)){
			mPolygon.setFillColor(PdrUtil.stringToColor(JSONUtil.getString(pJsArgs,0)));
		}else if("setFillOpacity".equals(pStrEvent)){
			mPolygon.setFillOpacity(PdrUtil.parseFloat(JSONUtil.getString(pJsArgs,0),0f));
		}else if("setLineWidth".equals(pStrEvent)){
			mPolygon.setLineWidth(PdrUtil.parseInt(JSONUtil.getString(pJsArgs,0),0));
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
		return mPolygon;
	}
	
	@Override
	public void onAddToMapView(DHMapView pMapView) {
		super.onAddToMapView(pMapView);
		mPolygon.initMapPolygon(pMapView);
	}
}
