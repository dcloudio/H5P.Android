package io.dcloud.js.map.amap.adapter;

import io.dcloud.common.util.PdrUtil;

import com.amap.api.maps.MapView;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;

//import com.amap.mapapi.map.MapView;
//import com.amap.mapapi.map.Overlay;
//import com.amap.mapapi.map.Projection;

/**
 * <p>Description:地图上的圆对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-12 上午11:12:18 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-12 上午11:12:18</pre>
 */
public class MapCircleProxy {
	
	
	/**
	 * JS对应的ID
	 */
	private String mJsId;
	/**
	 * 圈的中心位置
	 */
	private MapPoint mCenter;
	/**
	 * 圈的半径
	 */
	private double mRadius;
	/**
	 * 圈的边框颜色
	 */
	private int mStrokeColor = 0xFF000000;
	/**
	 * 圈的边框透明度
	 */
	private double mStrokeOpacity = 1;
	/**
	 * 圆圈的填充颜色
	 */
	private int mFillColor = 0xFF000000;
	/**
	 * 圆圈的填充颜色透明度
	 */
	private double mFillOpacity;
	/**
	 * 圆圈边框的宽度
	 */
	private double mLineWidth = 5;
	
	/**
	 * 
	 * Description: 构造函数 
	 * @param pMapview 父类GraphicsOverlay需要MapView
	 * @param pCen
	 * @param pRad 
	 *</pre> Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:33:23
	 */
	public MapCircleProxy(MapPoint pCen,double pRad){
		mCenter = pCen;
		mRadius = pRad;
	}

	public double getRadius() {
		return mRadius;
	}
	public void setRadius(double pRadius) {
		this.mRadius = pRadius;
		if(mMapCircle != null) mMapCircle.setRadius(pRadius);
	}
	public int getStrokeColor() {
		return mStrokeColor;
	}
	/**
	 * 
	 * Description:这里在地图显示的颜色必须要是8位切前面 有2位表示透明度FF为不透明所以需要加上
	 * @param pStrokeColor
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-12 上午11:21:29</pre>
	 */
	public void setStrokeColor(int pStrokeColor) {
		this.mStrokeColor = 0xFF000000 | pStrokeColor;
		if(mMapCircle != null) mMapCircle.setStrokeColor(combineOpacity(this.mStrokeColor, this.mStrokeOpacity));
	}
	
	/**
	 * @return the center
	 */
	public MapPoint getCenter() {
		return mCenter;
	}

	/**
	 * @param pCenter the center to set
	 */
	public void setCenter(MapPoint pCenter) {
		this.mCenter = pCenter;
		if(mMapCircle != null) mMapCircle.setCenter(pCenter.getLatLng());
	}

	/**
	 * @return the strokeOpacity
	 */
	public double getStrokeOpacity() {
		return mStrokeOpacity;
	}

	/**
	 * @param pStrokeOpacity the strokeOpacity to set
	 */
	public void setStrokeOpacity(double pStrokeOpacity) {
		this.mStrokeOpacity = pStrokeOpacity;
		if(mMapCircle != null) mMapCircle.setStrokeColor(combineOpacity(this.mStrokeColor, this.mStrokeOpacity));
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
		this.mFillColor = pFillColor;
		if(mMapCircle != null) mMapCircle.setFillColor(combineOpacity(pFillColor, this.mFillOpacity));
	}

	/**
	 * @return the fillOpacity
	 */
	public double getFillOpacity() {
		return mFillOpacity;
	}

	/**
	 * @param pFillOpacity the fillOpacity to set
	 */
	public void setFillOpacity(double pFillOpacity) {
		this.mFillOpacity = pFillOpacity;
		if(mMapCircle != null) mMapCircle.setFillColor(combineOpacity(mFillColor, pFillOpacity));
	}
	
	private int combineOpacity(int color,double opacity){
		return ((int)(opacity * 255) << 24) + color ;
	}

	/**
	 * @return the lineWidth
	 */
	public double getLineWidth() {
		return mLineWidth;
	}

	/**
	 * @param pLineWidth the lineWidth to set
	 */
	public void setLineWidth(double pLineWidth) {
		this.mLineWidth = pLineWidth;
		if(mMapCircle != null) mMapCircle.setStrokeWidth((int)pLineWidth);
	}
	
	public void initMapCircle(MapView mapview){
		mMapCircle = mapview.getMap().addCircle(getCircleOptions());
	}
	
	private CircleOptions getCircleOptions(){
		return new CircleOptions().center(mCenter.getLatLng())
		.radius(mRadius).strokeColor(combineOpacity(mStrokeColor,mStrokeOpacity))
		.fillColor(combineOpacity(mFillColor, mFillOpacity)).strokeWidth((int)mLineWidth);
	}
	Circle mMapCircle = null;
	
	public Circle getCircle() {
		return mMapCircle;
	}
}
