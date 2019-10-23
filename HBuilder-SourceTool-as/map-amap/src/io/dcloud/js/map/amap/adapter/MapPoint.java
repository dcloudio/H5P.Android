package io.dcloud.js.map.amap.adapter;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;

//import com.amap.mapapi.core.GeoPoint;

/**
 * <p>Description:map上的点位置</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-12-25 下午2:05:19 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-25 下午2:05:19</pre>
 */
public class MapPoint {
	/**
	 * 点的经度
	 */
	private float mLongitude;
	/**
	 * 点的纬度
	 */
	private float mLatitude;
	
	/**
	 * 
	 * Description: 构造函数 
	 * @param pLongitude 经度
	 * @param pLatitude 纬度
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-14 下午3:38:35</pre>
	 */
	public MapPoint(String pLatitude, String pLongitude) {
		mLongitude = Float.parseFloat(pLongitude);
		mLatitude = Float.parseFloat(pLatitude);
	}
	
	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return mLongitude;
	}
	
	/**
	 * @param pLongitude the longitude to set
	 */
	public void setLongitude(String pLongitude) {
		this.mLongitude = Float.parseFloat(pLongitude);
	}
	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return mLatitude;
	}
	/**
	 * @param pLatitude the latitude to set
	 */
	public void setLatitude(String pLatitude) {
		this.mLatitude = Float.parseFloat(pLatitude);
	}
	
	
	public LatLng getLatLng(){
		try {
			return new LatLng(mLatitude, mLongitude);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return new LatLng(39.90403, 116.407525);
	}
	
	public LatLonPoint getLatLngPoint(){
		try {
			return new LatLonPoint(mLatitude, mLongitude);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return new LatLonPoint(39.90403, 116.407525);
	}
}
