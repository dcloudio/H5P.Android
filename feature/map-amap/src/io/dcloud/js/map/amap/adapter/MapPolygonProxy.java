package io.dcloud.js.map.amap.adapter;

import java.util.ArrayList;
import java.util.List;

import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;

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
	private float mLineWidth = 5;
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
	 * @param pMapPoints 
	 *</pre> Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:37:02
	 */
	public MapPolygonProxy(ArrayList<MapPoint> pMapPoints){
		mMapPoints = pMapPoints;
	}
	Polygon mMapPolygonImpl = null;
	
	public Polygon getPolygon() {
		return mMapPolygonImpl;
	}
	public void initMapPolygon(MapView mapview){
		mMapPolygonImpl = mapview.getMap().addPolygon(getPolygonOptions());
	}
	
	private int combineOpacity(int color,double opacity){
		return ((int)(opacity * 255) << 24) + color ;
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
		if(mMapPolygonImpl != null) mMapPolygonImpl.setPoints(createRectangle());
	}
	
	public int getStrokeColor() {
		return mStrokeColor;
	}

	public void setStrokeColor(int pStrokeColor) {
		this.mStrokeColor = 0xFF000000 + pStrokeColor;
		if(mMapPolygonImpl != null) mMapPolygonImpl.setStrokeColor(combineOpacity(this.mStrokeColor, this.mStrokeOpacity));
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
		if(mMapPolygonImpl != null) mMapPolygonImpl.setStrokeColor(combineOpacity(this.mStrokeColor, this.mStrokeOpacity));
	}
	/**
	 * @return the lineWidth
	 */
	public float getLineWidth() {
		return mLineWidth;
	}
	/**
	 * @param pLineWidth the lineWidth to set
	 */
	public void setLineWidth(float pLineWidth) {
		this.mLineWidth = pLineWidth;
		if(mMapPolygonImpl != null) mMapPolygonImpl.setStrokeWidth(pLineWidth);
	}
	/**
	 * @return the fillStyle
	 */
	public int getFillColor() {
		return mFillColor;
	}
	/**
	 * @param pFillColor the fillStyle to set
	 */
	public void setFillColor(int pFillColor) {
		this.mFillColor =  0xFF000000 | pFillColor;
		if(mMapPolygonImpl != null) mMapPolygonImpl.setFillColor(combineOpacity(this.mFillColor, this.mFillOpacity));
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
		if(mMapPolygonImpl != null) mMapPolygonImpl.setFillColor(combineOpacity(this.mFillColor, this.mFillOpacity));
	}

	private PolygonOptions getPolygonOptions(){
		PolygonOptions options = new PolygonOptions();
		options.addAll(createRectangle());
		options.strokeColor(combineOpacity(this.mStrokeColor, this.mStrokeOpacity));
		options.fillColor(combineOpacity(this.mFillColor, this.mFillOpacity));
		options.strokeWidth(mLineWidth);
		return options;
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
