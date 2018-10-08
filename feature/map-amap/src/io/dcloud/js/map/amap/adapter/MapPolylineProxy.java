package io.dcloud.js.map.amap.adapter;
import java.util.ArrayList;
import java.util.List;

import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;


/**
 * 
 * <p>Description:在地图上显示的折线</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-6 下午6:00:35 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-6 下午6:00:35</pre>
 */
public class MapPolylineProxy{
	
	/**
	 * JS对应的ID
	 */
	private String mJsId;
	/**
	 * GeoPoint点的集合
	 */
	private ArrayList<MapPoint> mMapPoints;
	/**
	 * 线的颜色
	 */
	private int mStrokeColor = 0xFF000000;
	/**
	 * 线的透明度
	 */
	private float mStrokeOpacity = 1;
	/**
	 * 线的宽度
	 */
	private int mLineWidth = 5;
	
//	Symbol lineSymbol = null;
//	Geometry lineGeometry = null;
	/**
	 * 
	 * Description: 构造函数
	 * @param pMapPoints 
	 *</pre> Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:36:16
	 */
	public MapPolylineProxy(ArrayList<MapPoint> pMapPoints){
		mMapPoints = pMapPoints;
	}
	Polyline mMapPolylineImpl = null;
	
	public Polyline getPolyline() {
		return mMapPolylineImpl;
	}
	
	public void initMapPolyline(MapView mapView){
		mMapPolylineImpl = mapView.getMap().addPolyline(getNewGraphic());
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
	/**
	 * 
	 * Description:设置折线的顶点坐标
	 * @param pMapPoints
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-6 下午6:03:55</pre>
	 */
	public void setPath(ArrayList<MapPoint> pMapPoints){
		mMapPoints = pMapPoints;
		if(mMapPolylineImpl != null) mMapPolylineImpl.setPoints(createRectangle());
	}
	
	public int getStrokeColor() {
		return mStrokeColor;
	}
	private int combineOpacity(int color,double opacity){
		return ((int)(opacity * 255) << 24) + color ;
	}
	public void setStrokeColor(int pStrokeColor) {
		this.mStrokeColor = 0x88000000 | pStrokeColor;
		if(mMapPolylineImpl != null) mMapPolylineImpl.setColor(combineOpacity(this.mStrokeColor, mStrokeOpacity));
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
		if(mMapPolylineImpl != null) mMapPolylineImpl.setColor(combineOpacity(this.mStrokeColor, mStrokeOpacity));
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
	public void setLineWidth(int pLineWidth) {
		this.mLineWidth = pLineWidth;
		if(mMapPolylineImpl != null) mMapPolylineImpl.setWidth(this.mLineWidth);
	}
	
	private PolylineOptions getNewGraphic() {
		PolylineOptions options = new PolylineOptions();
		options.width(this.mLineWidth);
		if (mStrokeOpacity != 1) {
			options.color(combineOpacity(this.mStrokeColor, mStrokeOpacity));
		} else {
			options.color(mStrokeColor);
		}
		options.addAll(createRectangle());
		return options;
	}
}
