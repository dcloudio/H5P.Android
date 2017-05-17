package io.dcloud.js.geolocation.system;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
/**
 * 
 * <p>Description:定位监听器</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-12 上午11:37:32 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:37:32</pre>
 */
public class GeoListener {
	/**
	 * 失败值常量
	 */
	public static int PERMISSION_DENIED = 1;
	public static int POSITION_UNAVAILABLE = 2;
	public static int TIMEOUT = 3;
	/**
	 * 定位需求
	 */
	public static int GET_LOCATION=0;
	public static int WATCH_LOCATION=1;
	/**
	 * 定位类型
	 */
	public static int LOCATION_BY_GPS=0;
	public static int LOCATION_BY_NETWORK=1;
	public static int LOCATION_BY_BOTH=2;
	/**
	 * 定位超时时间
	 *
	 */
	public static int LOCATION_TIME_OUT=5000;// TODO:暂时定为5000ms
    /**
     * 定时器
     */
	private Timer timer;
	/**
	 * 定时器任务
	 */
	private MyTimerTask mTimerTask;
	/**
	 * 监听器ID
	 */
	String mId;							// Listener ID
	/**
	 * GPS监听器
	 */
    GpsListener mGps;					// GPS listener
    /**
     * 网络监听器
     */
    NetworkListener mNetwork;			// Network listener
   	/**
   	 * 回调方法ID
   	 */
    String mCallbackId;
    /**
     * webview对象
     */
    IWebview mWebview;
    /**
     * 上下文对象
     */
    private Context mContext;	
    /**
     * 定位管理者
     */
    LocationManager mLocMan;		
	
	/**
	 * 
	 * Description: 构造函数 
	 * @param pContext
	 * @param pId 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:49:21</pre>
	 */
	GeoListener(Context pContext, String pId) {
		mId = pId;
		mContext = pContext;
		mGps = null;
		mNetwork = null;
		mLocMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		if(timer==null)
		timer=new Timer();
	}
	
	/**
	 * Destroy listener.
	 */
	public void destroy() {
		this.stop(LOCATION_BY_BOTH);
	}
	
	static final String RETURN_JSON = "{" +
	"latitude:%f" +//维度
	",longitude:%f" +//经度
	",altitude:%f" +//
	",accuracy:%f" + //精确度
	",heading:%f" +//方向
	",velocity:%f" +//移动速度
	",altitudeAccuracy:%d" +//海拔精确度
	",timestamp:new Date('%s')" +//时间戳
	",coordsType:'%s'" +//坐标类型
	"}";
	private String makeJSON(Location pLoc,String coordsType){
	return String.format(Locale.ENGLISH,RETURN_JSON,
			pLoc.getLatitude() 
			,pLoc.getLongitude()
			,pLoc.getAltitude()
			,pLoc.getAccuracy()
			,pLoc.getBearing()
			,pLoc.getSpeed()
			,0
			,pLoc.getTime()
			,coordsType
			);
	}

	/**
	 * 
	 * Description:成功回调
	 * @param pLoc 位置对象
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:47:19</pre>
	 */
	@SuppressWarnings("deprecation")
	void success(Location pLoc, int locationType) {
		Log.i("geoListener", "successType=="+locationType);
		String _json = makeJSON(pLoc,"wgs84");
		/**
		 * 如果是getCurrentPosition的需要清除回调
		 */
		if(getCurLocationCallbackId != null && getCurLocationWebview != null){
			JSUtil.excCallbackSuccess(getCurLocationWebview, getCurLocationCallbackId, _json, true, false);
			this.stop(LOCATION_BY_BOTH);
			getCurLocationCallbackId = null;
			getCurLocationWebview = null;
		}
		if(mWebview != null && mCallbackId != null){
			JSUtil.excCallbackSuccess(mWebview, mCallbackId, _json, true, true);
		}
	}
	
	/**
	 * 
	 * Description: 失败回调
	 * @param code 失败码
	 * @param msg 失败消息
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:46:29</pre>
	 */
	@SuppressWarnings("deprecation")
	void fail(int pCode, String pMessage, int locationType) {
		Log.i("geoListener", "failType=="+locationType);
		this.stop(locationType);
		if (getCurLocationCallbackId != null && getCurLocationWebview != null) {
			
			if (this.mGps == null && this.mNetwork == null) {
				JSUtil.excCallbackError(getCurLocationWebview,
						getCurLocationCallbackId,
						DOMException.toJSON(pCode, pMessage), true);
			}
			
		}
		if (mWebview != null && mCallbackId != null) {
			
			if (this.mGps == null && this.mNetwork == null) {
				JSUtil.excCallbackError(mWebview, mCallbackId,
						DOMException.toJSON(pCode, pMessage), true);
			}
		}
		
	}
	
	String getCurLocationCallbackId = null;
	IWebview getCurLocationWebview = null;
	void getCurrentLocation(IWebview pWebViewImpl, int interval, String pCallbackId) {
		getCurLocationWebview = pWebViewImpl;
		getCurLocationCallbackId = pCallbackId;
		start(interval,GET_LOCATION);
	}
	
	int mUseCount = 0;
	private void count(int f){
		mUseCount += f;
		Logger.d("GeoListener","mUseCount=" + mUseCount);
	}
	
	private boolean start(int interval,int requestType){
		if(mUseCount == 0){
			// If GPS provider, then create and start GPS listener
			//this.mLocMan.getProvider(LocationManager.GPS_PROVIDER) != null
			if (this.mGps == null && this.mLocMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				this.mGps = new GpsListener(mContext, this);
			}
			
			// If network provider, then create and start network listener
//			this.mLocMan.getProvider(LocationManager.NETWORK_PROVIDER) != null this.mLocMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
			if (this.mNetwork == null && this.mLocMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				this.mNetwork = new NetworkListener(mContext, this);
			}
			if (this.mGps != null) {
					this.mGps.start(interval);
			}
			if (this.mNetwork != null) {
					this.mNetwork.start(interval);
			}
			// 开启定时器，防止某些手机超时无返回值异常
			if (requestType == GET_LOCATION) {
				startTimeOutFiledWatcher(LOCATION_TIME_OUT);
			} 
		}
		if (requestType == WATCH_LOCATION)  {
			if (timer != null) {
				if (mTimerTask != null) {
					mTimerTask.cancel();
				}
			}
		}
		count(1);
		if (this.mNetwork == null && this.mGps == null) {
			this.fail(POSITION_UNAVAILABLE, "No location providers available.",LOCATION_BY_BOTH);
			return false;
		}
		return true;
	}
	/**
	 * 
	 * Description:开启定位监听
	 * @param pWebViewImpl webview对象
	 * @param interval 定位更新间隔时间
	 * @param pCallbackId 回调方法ID
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:45:18</pre>
	 */
	boolean start(IWebview pWebViewImpl, int interval, String pCallbackId, int timeout) {
		mWebview = pWebViewImpl;
		mCallbackId = pCallbackId;
		LOCATION_TIME_OUT = timeout;
		return start(interval,WATCH_LOCATION);
	}
	
	/**
	 * 
	 * Description:关闭定位监听
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-11 下午5:30:14</pre>
	 * @param locationType 
	 */
	void stop(int locationType) {
		count(-1);
		if (mUseCount <= 0) {
			if (locationType == LOCATION_BY_GPS) {
				if (this.mGps != null) {
					this.mGps.stop();
					this.mGps = null;
				}
			} else if (locationType == LOCATION_BY_NETWORK) {
				if (this.mNetwork != null) {
					this.mNetwork.stop();
					this.mNetwork = null;
				}
			} else {
				if (this.mGps != null) {
					this.mGps.stop();
					this.mGps = null;
				}
				if (this.mNetwork != null) {
					this.mNetwork.stop();
					this.mNetwork = null;
				}
			}

			mUseCount = 0;
		}
		Logger.d("GeoListener", "mUseCount=" + mUseCount);
	}
	
	/**
	 * 开启超时异常监听
	 */
	private void startTimeOutFiledWatcher(int runOutTime) {
		if (timer != null) {
			if (mTimerTask != null) {
				mTimerTask.cancel();
			}
			mTimerTask = new MyTimerTask();
			timer.schedule(mTimerTask, runOutTime);
		}
	}
	/**
	 * 计时器任务
	 * @author peng
	 *
	 */
	class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			if ((GeoListener.this.mGps != null)
					|| (GeoListener.this.mNetwork != null))

				GeoListener.this.fail(POSITION_UNAVAILABLE,
						"No location providers available.",LOCATION_BY_BOTH);
		}
	}
}
