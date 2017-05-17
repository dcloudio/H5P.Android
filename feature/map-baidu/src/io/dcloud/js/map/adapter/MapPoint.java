package io.dcloud.js.map.adapter;

import com.baidu.mapapi.model.LatLng;

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
	private Double mLongitude;
	/**
	 * 点的纬度
	 */
	private Double mLatitude;
	
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
	public MapPoint(String pLongitude, String pLatitude) {
		mLongitude = Double.parseDouble(pLongitude);
		mLatitude = Double.parseDouble(pLatitude);
	}
	
	/**
	 * @return the longitude
	 */
	public Double getLongitude() {
		return mLongitude;
	}
	/**
	 * @param pLongitude the longitude to set
	 */
	public void setLongitude(Double pLongitude) {
		this.mLongitude = pLongitude;
	}
	/**
	 * @return the latitude
	 */
	public Double getLatitude() {
		return mLatitude;
	}
	/**
	 * @param pLatitude the latitude to set
	 */
	public void setLatitude(Double pLatitude) {
		this.mLatitude = pLatitude;
	}
	
	public LatLng getLatLng() {
		LatLng latLng = new LatLng(mLatitude, mLongitude);
		return latLng;
	}

}
