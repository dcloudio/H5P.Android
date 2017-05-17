package io.dcloud.js.geolocation.system;

import io.dcloud.common.adapter.util.Logger;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
/**
 * 
 * <p>Description:网络定位（如：WIFI）</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-12 下午12:10:30 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 下午12:10:30</pre>
 */
public class NetworkListener implements LocationListener {
	/**
	 * 上下文对象
	 */
	private Context mCtx;				
	/**
	 * 位置管理者
	 */
	private LocationManager mLocMan;			// Location manager object
	private GeoListener owner;					// Geolistener object (parent)
	private boolean hasData = false;			// Flag indicates if location data is available in cLoc
	private Location cLoc;						// Last recieved location
	private boolean running = false;			// Flag indicates if listener is running

	/**
	 * 
	 * Description: 构造函数 
	 * @param ctx
	 * @param m 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 下午12:07:53</pre>
	 */
	public NetworkListener(Context ctx, GeoListener m) {
		this.owner = m;
		this.mCtx = ctx;
		this.mLocMan = (LocationManager) this.mCtx.getSystemService(Context.LOCATION_SERVICE);
		this.running = false;
	}
	
	/**
	 * Get last location.
	 * 
	 * @return 				Location object
	 */
	public Location getLocation() {
		this.cLoc = this.mLocMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (this.cLoc != null) {
			this.hasData = true;
		}
		return this.cLoc;
	}

	public boolean hasLocation(){
		return this.hasData;
	}
	/**
	 * Called when the provider is disabled by the user.
	 * 
	 * @param provider
	 */
	public void onProviderDisabled(String provider) {
		this.running = false;
		if(!this.hasData && (this.owner.mGps == null || !this.owner.mGps.hasLocation())){
			this.owner.fail(GeoListener.POSITION_UNAVAILABLE, "The provider " + provider + " is disabled",GeoListener.LOCATION_BY_NETWORK);
		}
		Logger.d("NetworkListener: The provider " + provider + " is disabled");
		
	}

	/**
	 * Called when the provider is enabled by the user.
	 * 
	 * @param provider
	 */
	public void onProviderEnabled(String provider) {
		Logger.d("NetworkListener: The provider "+ provider + " is enabled");
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
		Logger.d("NetworkListener: The status of the provider " + provider + " has changed");
		if (status == 0) {
			Logger.d("NetworkListener: " + provider + " is OUT OF SERVICE");
		}
		else if (status == 1) {
			Logger.d("NetworkListener: " + provider + " is TEMPORARILY_UNAVAILABLE");
		}
		else {
			Logger.d("NetworkListener: " + provider + " is Available");
		}
	}

	/**
	 * Called when the location has changed.
	 * 
	 * @param location
	 */
	public void onLocationChanged(Location location) {
		Logger.d("NetworkListener: The location has been updated!");
		this.hasData = true;
		this.cLoc = location;
		
		// The GPS is the primary form of Geolocation in 
		// Only fire the success variables if the GPS is down for some reason.
         //	if(this.owner.mGps != null && !this.owner.mGps.hasLocation())
			this.owner.success(location,GeoListener.LOCATION_BY_NETWORK);
	}
	
	/**
	 * Start requesting location updates.
	 * 
	 * @param interval
	 */
	public void start(int interval)	{
		if (!this.running) {
			this.running = true;
			this.mLocMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, 0, this);
//			this.getLocation();
//			
//			// If Network provider has data but GPS provider doesn't, then send ours
//			if (this.hasData && !this.owner.mGps.hasLocation()) {
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
