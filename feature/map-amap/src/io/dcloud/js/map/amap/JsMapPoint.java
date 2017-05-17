package io.dcloud.js.map.amap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.js.map.amap.adapter.MapPoint;

import org.json.JSONArray;

/**
 * <p>
 * Description:js中的point
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-10-31 下午3:14:08 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午3:14:08
 * </pre>
 */
class JsMapPoint extends JsMapObject {

	/**
	 * 地图点对象
	 */
	private MapPoint mMapPoint;

	/**
	 * 
	 * Description: 构造函数
	 * 
	 * @param pLongitude
	 *            经度
	 * @param pLatitude
	 *            纬度
	 * 
	 *            <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-14 下午3:38:35
	 * </pre>
	 */
	public JsMapPoint(IWebview pWebview, String pLatitude, String pLongitude) {
		super(pWebview);
		mMapPoint = new MapPoint(pLatitude, pLongitude);
	}

	public MapPoint getMapPoint() {
		return mMapPoint;
	}
	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
	}

	@Override
	protected void createObject(JSONArray pJsArgs) {
	}
}
