package io.dcloud.js.map.adapter;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Path;
import android.graphics.Point;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Polygon;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;

/**
 * <p>Description:地图上画一个多边形</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-10-29 上午11:07:13 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-29 上午11:07:13</pre>
 */
public class MapPolygonProxy {
	
	/**
	 * JS对应的ID
	 */
	private String mJsId;
	/**
	 * GeoPoint点的集合
	 */
	private ArrayList<MapPoint> mMapPoints;
	/**
	 * 边框的颜色
	 */
	private int mStrokeColor = 0xFF000000;
	/**
	 * 边框的透明度
	 */
	private float mStrokeOpacity = 1;
	/**
	 * 边框的宽度
	 */
	private int mLineWidth = 5;
	/**
	 * 多边形的填充颜色
	 */
	private int mFillColor = 0x00000000;
	/**
	 * 多边形的填充颜色透明度
	 */
	private float mFillOpacity = 0;
	
//	Geometry polygonGeometry = null;
//	Symbol polygonSymbol = null;
	/**
	 * 
	 * Description: 构造函数 
	 * @param mapview 父类GraphicsOverlay需要MapView
	 * @param pMapPoints 
	 *</pre> Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:37:02
	 */
	public MapPolygonProxy(ArrayList<MapPoint> pMapPoints){
		mMapPoints = pMapPoints;
	}
	Polygon mMapPolygonImpl = null;
	public void initMapPolygon(DHMapView mapview){
		mMapPolygonImpl = (Polygon) mapview.getBaiduMap().addOverlay(getMapPolygon());
	}
	
	public PolygonOptions getMapPolygon(){
		Stroke stroke = new Stroke(mLineWidth, 
				combineOpacity(mStrokeColor, mStrokeOpacity));
		return new PolygonOptions().points(createRectangle()).fillColor(combineOpacity(mFillColor, mFillOpacity))
				.stroke(stroke);
	}
	/**
	 * 
	 * Description:设置点的集合
	 * @param pAryPoint
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-6 下午6:11:27</pre>
	 */
	public void setPath(ArrayList<MapPoint> pAryPoint){
		mMapPoints = pAryPoint;
		if (mMapPolygonImpl != null) {
			mMapPolygonImpl.setPoints(createRectangle());
		}
	}
	
	public int getStrokeColor() {
		return mStrokeColor;
	}

	public void setStrokeColor(int pStrokeColor) {
		this.mStrokeColor = 0xFF000000 + pStrokeColor;
		if (mMapPolygonImpl != null) {
			Stroke stroke = new Stroke(mLineWidth, 
					combineOpacity(mStrokeColor, mStrokeOpacity));
			mMapPolygonImpl.setStroke(stroke);
		}
	}
	
	public Polygon getPolygon() {
		return mMapPolygonImpl;
	}
	/**
	 * @return the strokeOpacity
	 */
	public float getStrokeOpacity() {
		return mStrokeOpacity;
	}
	/**
	 * @param pStrokeOpacity the strokeOpacity to set
	 */
	public void setStrokeOpacity(float pStrokeOpacity) {
		this.mStrokeOpacity = pStrokeOpacity;
		if (mMapPolygonImpl != null) {
			Stroke stroke = new Stroke(mLineWidth, 
					combineOpacity(mStrokeColor, mStrokeOpacity));
			mMapPolygonImpl.setStroke(stroke);
		}
	}
	/**
	 * @return the lineWidth
	 */
	public int getLineWidth() {
		return mLineWidth;
	}
	/**
	 * @param pLineWidth the lineWidth to set
	 */
	public void setLineWidth(int pLineWidth) {
		this.mLineWidth = pLineWidth;
		if (mMapPolygonImpl != null) {
			Stroke stroke = new Stroke(mLineWidth, 
					combineOpacity(mStrokeColor, mStrokeOpacity));
			mMapPolygonImpl.setStroke(stroke);
		}
	}
	/**
	 * @return the fillStyle
	 */
	public int getFillColor() {
		return mFillColor;
	}
	/**
	 * @param pFillStyle the fillStyle to set
	 */
	public void setFillColor(int pFillColor) {
		this.mFillColor =  0xFF000000 | pFillColor;
		if (mMapPolygonImpl != null) {
			mMapPolygonImpl.setFillColor(combineOpacity(mFillColor, mFillOpacity));
		}
	}
	/**
	 * @return the fillOpacity
	 */
	public float getFillOpacity() {
		return mFillOpacity;
	}
	/**
	 * @param pFillOpacity the fillOpacity to set
	 */
	public void setFillOpacity(float pFillOpacity) {
		this.mFillOpacity = pFillOpacity;
		if (mMapPolygonImpl != null) {
			mMapPolygonImpl.setFillColor(combineOpacity(mFillColor, mFillOpacity));
		}
	}

	private int combineOpacity(int color,double opacity){
		return ((int)(opacity * 255) << 24) + color ;
	}
	
	private List<LatLng> createRectangle() {
		ArrayList<LatLng> list = new ArrayList<LatLng>();
		if(mMapPoints != null && !mMapPoints.isEmpty()){
			for(MapPoint p : mMapPoints){
				list.add(p.getLatLng());
			}
		}
		return list;
	}
}
