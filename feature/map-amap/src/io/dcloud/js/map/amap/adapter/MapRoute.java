package io.dcloud.js.map.amap.adapter;

import io.dcloud.common.DHInterface.IWebview;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;

import com.amap.api.maps.MapView;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.overlay.BusRouteOverlay;
import com.amap.api.maps.overlay.DrivingRouteOverlay;
import com.amap.api.maps.overlay.WalkRouteOverlay;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.WalkPath;


/**
 * <p>Description:地图路径对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-6 上午10:44:54 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-6 上午10:44:54</pre>
 */
public class MapRoute {

	MapPoint mStart;
	MapPoint mEnd;
	
	IWebview mWebview;
	MapView mMapview;
	
	private Object overlay;
	/**
	 * 具体路径对象
	 */
	private Object mRoute;
	/**
	 * 绘制路径的画笔
	 */
	private Paint mPaint;
	/**
	 * Description: 构造函数 
	 * @param pFrameView
	 * @param pJsId 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:50:48</pre>
	 */
	public MapRoute() {
		initPaint();
	}
	/**
	 * 
	 * Description:根据起始点终点设置路径
	 * @param pStart 起始点
	 * @param pEnd	终点
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-26 上午9:55:06</pre>
	 */
	public void setRoute(MapPoint pStart,MapPoint pEnd){
		mStart = pStart;
		mEnd = pEnd;
	}

	public void initMapRoute(IWebview pWebview, MapView mapview) {
		mWebview = pWebview;
		mMapview = mapview;
		if (mRoute instanceof WalkPath) { // 步行路线方案
			WalkPath path = (WalkPath) mRoute;
			WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(
					mWebview.getContext(), mMapview.getMap(), path,
					mStart.getLatLngPoint(), mEnd.getLatLngPoint());
			overlay = walkRouteOverlay;
			walkRouteOverlay.zoomToSpan();
			walkRouteOverlay.addToMap();
		} else if (mRoute instanceof DrivePath) { // 驾车路线方案
			DrivePath drivePath = (DrivePath) mRoute;
			DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
					mWebview.getContext(), mMapview.getMap(), drivePath,
					mStart.getLatLngPoint(), mEnd.getLatLngPoint());
			overlay = drivingRouteOverlay;
			drivingRouteOverlay.zoomToSpan();
			drivingRouteOverlay.addToMap();
		} else if (mRoute instanceof BusPath) { // 公交路线方案
			BusPath busPath = (BusPath) mRoute;
			BusRouteOverlay busRouteOverlay = new BusRouteOverlay(
					mWebview.getContext(), mMapview.getMap(), busPath,
					mStart.getLatLngPoint(), mEnd.getLatLngPoint());
			busRouteOverlay.zoomToSpan();
			busRouteOverlay.addToMap();
		}
	}
	
	
	/**
	 * 去掉RouteOverlay上所有的Marker。
	 */
	public void removeFromMap() {
		if (mRoute instanceof WalkPath) {
			WalkRouteOverlay walkRouteOverlay = (WalkRouteOverlay)overlay;
			walkRouteOverlay.removeFromMap();
		} else if (mRoute instanceof DrivePath) {
			DrivingRouteOverlay drivingRouteOverlay = (DrivingRouteOverlay)overlay;
			drivingRouteOverlay.removeFromMap();
		} else if (mRoute instanceof BusPath) {
			BusRouteOverlay busRouteOverlay = (BusRouteOverlay)overlay;
			busRouteOverlay.removeFromMap();
		}
	}
	/**
	 * 
	 * Description:设置搜索出来的路径
	 * @param pRoute 路径对象
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-26 上午9:56:28</pre>
	 */
	public void setRoute(Object pRoute){
		mRoute = pRoute;
	}
	
	/**
	 * 
	 * Description:获取路径对象
	 * @return 路径（MapLine/BusLineItem/Route）
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-26 上午9:58:00</pre>
	 */
	public Object getRoute(){
		return mRoute;
	}
	/**
	 * 
	 * <p>Description:起始点终点的路径</p>
	 *
	 * @version 1.0
	 * @author cuidengfeng Email:cuidengfeng@dcloud.io
	 * @Date 2012-11-26 下午6:07:15 created.
	 * 
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-26 下午6:07:15</pre>
	 */
	protected class MapLine {
		public PolylineOptions options;
		//应该目前用不到这里
		public MapLine(MapView mapview,MapPoint pStartPos,MapPoint pEndPos) {
			options = new PolylineOptions();
			options.add(pStartPos.getLatLng(), pEndPos.getLatLng());
			options.color(Color.RED);
			options.width(10);
		}
	}
	/**
	 * 
	 * Description:初始化 画笔
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-26 下午6:05:05</pre>
	 */
	private void initPaint() {
		mPaint = new Paint();
		mPaint.setStyle(Style.STROKE);
		mPaint.setColor(Color.rgb(54, 114, 227));
		mPaint.setAlpha(180);
		mPaint.setStrokeWidth(5.5f);
		mPaint.setStrokeJoin(Join.ROUND);
		mPaint.setStrokeCap(Cap.ROUND);
		mPaint.setAntiAlias(true);
	}

}
