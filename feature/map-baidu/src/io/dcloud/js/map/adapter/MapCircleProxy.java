package io.dcloud.js.map.adapter;

import io.dcloud.common.util.PdrUtil;

import com.baidu.mapapi.map.Circle;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.Stroke;

/**
 * 地图上的圆对象
 * @author shutao
 *
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
	private int mRadius;
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
	private int mLineWidth = 5;
	
	private Circle mCircle;
	
//	Geometry circleGeometry = null;
//	Symbol circleSymbol = null;
	/**
	 * 
	 * Description: 构造函数 
	 * @param pMapview 父类GraphicsOverlay需要MapView
	 * @param pCen
	 * @param pRad 
	 *</pre> Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:33:23
	 */
	public MapCircleProxy(MapPoint pCen,int pRad){
		mCenter = pCen;
		mRadius = pRad;
	}

	public double getRadius() {
		return mRadius;
	}
	public void setRadius(int pRadius) {
		this.mRadius = pRadius;
	}
	/**
	 * 获取圈的边框颜色
	 * @return
	 */
	public int getStrokeColor() {
		return combineOpacity(this.mStrokeColor, this.mStrokeOpacity);
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
		if (mCircle != null) {
			Stroke stroke = new Stroke(getLineWidth(), getStrokeColor());
			mCircle.setStroke(stroke);
		}
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
		if (mCircle != null) {
			mCircle.setCenter(pCenter.getLatLng());
		}
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
		if (mCircle != null) {
			Stroke stroke = new Stroke(getLineWidth(), getStrokeColor());
			mCircle.setStroke(stroke);
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
		this.mFillColor = pFillColor;
		if(mCircle != null) mCircle.setFillColor(combineOpacity(mFillColor, this.mFillOpacity));
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
		if (mCircle != null) {
			mCircle.setFillColor(combineOpacity(mFillColor, mFillOpacity));
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
		if (mCircle != null) {
			Stroke stroke = new Stroke(pLineWidth, getStrokeColor());
			mCircle.setStroke(stroke);
		}
	}
	
	private int combineOpacity(int color,double opacity) {
		return ((int)(opacity * 255) << 24) + color ;
	}
	
	public void initMapCircle(DHMapView mapview) {
		mCircle = (Circle) mapview.getBaiduMap().addOverlay(getMapCircle());
	}

	public CircleOptions getMapCircle() {
		Stroke stroke = new Stroke(getLineWidth(), getStrokeColor());
		return new CircleOptions().center(mCenter.getLatLng()).radius(mRadius).fillColor(
				combineOpacity(getFillColor(), this.mFillOpacity)
				).stroke(stroke);
	}
	
	public Circle getCircle() {
		return mCircle;
	}
	
}
