package io.dcloud.js.map.adapter;

import io.dcloud.common.DHInterface.IWebview;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;

import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.WalkingRouteLine;


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
	/**
	 * 具体路径对象
	 */
	private Object mRoute;
	private Object mOverlay;
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
	IWebview mWebview;
	DHMapView mMapview;
	public void initMapRoute(IWebview pWebview,DHMapView mapview){
		mWebview = pWebview;
		mMapview = mapview;
		if (mRoute instanceof WalkingRouteLine) { // 步行路线
			WalkingRouteOverlay overlay = new WalkingRouteOverlay(mMapview.getBaiduMap());
			overlay.setData((WalkingRouteLine)mRoute);
			mOverlay = overlay;
			overlay.addToMap();
            overlay.zoomToSpan();
		} else if (mRoute instanceof TransitRouteLine) { // 公交路线
			TransitRouteOverlay overlay = new TransitRouteOverlay(mMapview.getBaiduMap());
			overlay.setData((TransitRouteLine)mRoute);
			mOverlay = overlay;
			overlay.addToMap();
            overlay.zoomToSpan();
		} else if (mRoute instanceof DrivingRouteLine) { // 驾车路线
			DrivingRouteOverlay overlay = new DrivingRouteOverlay(mMapview.getBaiduMap());
			overlay.setData((DrivingRouteLine)mRoute);
			mOverlay = overlay;
			overlay.addToMap();
            overlay.zoomToSpan();
		}
	}
	
	/**
	 * 去掉RouteOverlay
	 */
	public void removeFromMap() {
		if (mRoute instanceof WalkingRouteLine) { // 步行路线
			WalkingRouteOverlay walkRouteOverlay = (WalkingRouteOverlay)mOverlay;
			walkRouteOverlay.removeFromMap();
		} else if (mRoute instanceof DrivingRouteLine) { // 公交路线
			DrivingRouteOverlay drivingRouteOverlay = (DrivingRouteOverlay)mOverlay;
			drivingRouteOverlay.removeFromMap();
		} else if (mRoute instanceof TransitRouteLine) { // 驾车路线
			TransitRouteOverlay transitRouteOverlay = (TransitRouteOverlay)mOverlay;
			transitRouteOverlay.removeFromMap();
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
	public Object getRoute() {
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
	protected class MapLine{

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
