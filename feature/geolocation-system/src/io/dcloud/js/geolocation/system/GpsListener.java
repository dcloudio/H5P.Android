package io.dcloud.js.geolocation.system;

import io.dcloud.common.adapter.util.Logger;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * 
 * <p>Description:GPS定位监听</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-12 上午11:59:46 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:59:46</pre>
 */
public class GpsListener implements LocationListener {
	
	private Context mCtx;				// 
	
	private LocationManager mLocMan;			// Location manager object
	private GeoListener owner;					// Geolistener object (parent)
	/**
	 * 是否有定位信息
	 */
	private boolean hasData = false;			// Flag indicates if location data is available in cLoc
	/**
	 * 定位信息
	 */
	private Location cLoc;						// Last recieved location
	/**
	 * 是否正在监听中
	 */
	private boolean running = false;			// Flag indicates if listener is running
	
	/**
	 * 
	 * Description: 构造函数 
	 * @param pCtx
	 * @param pGeoLtr 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 下午12:03:51</pre>
	 */
	public GpsListener(Context pCtx, GeoListener pGeoLtr) {
		owner = pGeoLtr;
		mCtx = pCtx;
		mLocMan = (LocationManager) mCtx.getSystemService(Context.LOCATION_SERVICE);
		running = false;
	}
	
	/**
	 * Get last location.
	 * 
	 * @return 				Location object
	 */
	public Location getLocation() {
		cLoc = mLocMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (cLoc != null) {
			gpsDataChange(true);
		}
		return cLoc;
	}
	long lastChangeTime = System.currentTimeMillis();
	static final int MAXINUMAGE = 10000;
	private void gpsDataChange(boolean bln){
		hasData = bln;
		if(bln){
			lastChangeTime = System.currentTimeMillis();
		}
	}
	/**
	 * Called when the provider is disabled by the user.
	 * 
	 * @param provider
	 */
	public void onProviderDisabled(String provider) {
		this.running = false;
//		if(!this.hasData && (this.owner.mNetwork == null || !this.owner.mNetwork.hasLocation())){
		if(!this.hasData ){
			this.owner.fail(GeoListener.POSITION_UNAVAILABLE, "GPS provider disabled.",GeoListener.LOCATION_BY_GPS);
		}
	}

	/**
	 * Called when the provider is enabled by the user.
	 * 
	 * @param provider
	 */
	public void onProviderEnabled(String provider) {
		Logger.d("GpsListener: The provider "+ provider + " is enabled");
	}

	/**
	 * Called when the provider status changes. This method is called when a 
	 * provider is unable to fetch a location or if the provider has recently 
	 * become available after a period of unavailability.
	 * 
	 * @param provider
	 * @param status
	 * @param extras
	 */
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Logger.d("GpsListener: The status of the provider " + provider + " has changed");
		if (status == 0) {
			Logger.d("GpsListener: " + provider + " is OUT OF SERVICE");
			this.owner.fail(GeoListener.POSITION_UNAVAILABLE, "GPS out of service.",GeoListener.LOCATION_BY_GPS);
		}
		else if (status == 1) {
			Logger.d("GpsListener: " + provider + " is TEMPORARILY_UNAVAILABLE");
		}
		else {
			Logger.d("GpsListener: " + provider + " is Available");
		}
	}

	/**
	 * Called when the location has changed.
	 * 
	 * @param location
	 */
	public void onLocationChanged(Location location) {
		Logger.d("GpsListener: The location has been updated!");
		gpsDataChange(true);
		this.cLoc = location;
		this.owner.success(location,GeoListener.LOCATION_BY_GPS);
	}

	/**
	 * Determine if location data is available.
	 * 
	 * @return
	 */
	public boolean hasLocation() {
		boolean f = System.currentTimeMillis() - lastChangeTime < MAXINUMAGE;
		if(!f){
			this.hasData = false;
		}
		return this.hasData;
	}
	
	/**
	 * Start requesting location updates.
	 * 
	 * @param interval
	 */
	public void start(int interval) {
		if (!this.running) {
			this.running = true;
			this.mLocMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0, this);
//			this.getLocation();

			// If GPS provider has data, then send now
//			if (this.hasData) {
//				this.owner.success(this.cLoc);
//			}
		}
	}

	/**
	 * Stop receiving location updates.
	 */
	public void stop() {
		if (this.running) {
			this.mLocMan.removeUpdates(this);
		}
		this.running = false;
	}
	
}
