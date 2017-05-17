package io.dcloud.js.geolocation.baidu;

import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.SP;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.geolocation.GeoManagerBase;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

public class BaiduGeoManager extends GeoManagerBase{


	public static String Tag = "BaiduGeoManager";
	LocationClient mGetCLocationClient = null;
	LocationClientOption mGetCLocationClientOption = null;
	LocationClient mWatchLocationClient = null;
	LocationClientOption mWatchLocationClientOption = null;
	/**是否存在百度key*/
	boolean hasAppkey = false;
	// 是否解析完整地理信息
	boolean isGeocode = true;

	boolean isStreamApp = false;
	public BaiduGeoManager(Context pContext) {
		super(pContext);
		hasAppkey = !PdrUtil.isEmpty(AndroidResources.getMetaValue("com.baidu.lbsapi.API_KEY"));
	}

	public String execute(IWebview pWebViewImpl, String pActionName,
						  String[] pJsArgs) {
		String result = "";
		try {
			isStreamApp = pWebViewImpl.obtainApp().isStreamApp();
			String t = pJsArgs.length > 7 ? pJsArgs[6] : "null";//不设置为"null"字符串
			int timeout = Integer.MAX_VALUE;
			if(!"null".equals(t)){
				timeout = Integer.parseInt(t);
			}
			String intervals = pJsArgs.length > 8 ? pJsArgs[7] : "5000";
			int interval = 5000;
			if (!intervals.equals("null")) {
				interval = Integer.parseInt(intervals);
				if(interval<1000) {
					interval = 1000;
				}
			}
			if (pActionName.equals("getCurrentPosition")) {
				isGeocode = Boolean.parseBoolean(pJsArgs[5]);
				boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[1]);

				boolean isNotWgs84 = !PdrUtil.isEquals("wgs84", pJsArgs[3]);
				if(/*PdrUtil.isEquals("baidu", pJsArgs[4]) &&*/ isNotWgs84){
					getCurrentLocation(pWebViewImpl, pJsArgs[0], _enableHighAccuracy, timeout, pJsArgs[3]);
				}else{
					String _json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,isNotWgs84 ? DOMException.MSG_GEOLOCATION_PROVIDER_ERROR : "only support gcj02|bd09|bd09ll");
					JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
				}
			}
			else if (pActionName.equals("watchPosition")) {
				isGeocode = Boolean.parseBoolean(pJsArgs[5]);
				boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[2]);
				pWebViewImpl.obtainFrameView().addFrameViewListener(new IEventCallback() {
					@Override
					public Object onCallBack(String pEventType, Object pArgs) {
						if((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) && pArgs instanceof IWebview){
							stop();
							((AdaFrameView)((IWebview)pArgs).obtainFrameView()).removeFrameViewListener(this);
						}
						return null;
					}
				});
				boolean isNotWgs84 = !PdrUtil.isEquals("wgs84", pJsArgs[3]);
				if(/*PdrUtil.isEquals("baidu", pJsArgs[4]) &&*/ isNotWgs84){
					watch(pWebViewImpl, pJsArgs[0], pJsArgs[1], _enableHighAccuracy, pJsArgs[3], timeout, interval);
				}else{
					String _json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,isNotWgs84 ? DOMException.MSG_GEOLOCATION_PROVIDER_ERROR : "only support gcj02|bd09|bd09ll");
					JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
				}
			}
			else if (pActionName.equals("clearWatch")) {
				if(keySet.contains(pJsArgs[0])){
					keySet.remove(pJsArgs[0]);
					stop();
				}
			}
			return result;
		} catch (Exception e) {
			return result;
		}
	}
	private int mUseCount = 0;
	private void count(int i){
		mUseCount += i;
	}
	String mGetCurrentLocationCoordsType = null;
	String mCoordsType = null;
	BDLocationListener mWatchBDLocationListenerImpl = new BDLocationListener(){
		@Override
		public void onReceiveLocation(BDLocation pLoc) {
					JSONObject _json = null;
			if(watchWebview != null && watchCallbackId != null){
				_json = makeJSON(pLoc,mCoordsType);
				if (_json == null) {
					geoDataError(watchWebview, watchCallbackId);
				} else {
					//处于监听状态
					JSUtil.execCallback(watchWebview, watchCallbackId, _json, JSUtil.OK, true);
				}
			}
		}

	};

	BDLocationListener mGetCBDLocationListenerImpl = new BDLocationListener(){
		@Override
		public void onReceiveLocation(BDLocation pLoc) {
			JSONObject _json = null;
			if(mGetCurrentLocation){
				_json = makeJSON(pLoc,mGetCurrentLocationCoordsType);
				if (_json == null) {
					geoDataError(mGetCurrentLocationWebview, mGetCurrentLocationCallbackId);
				} else {
					//获取当前位置
					Logger.d(Tag,"_json=" + _json);
					JSUtil.execCallback(mGetCurrentLocationWebview, mGetCurrentLocationCallbackId, _json, JSUtil.OK, false);
				}
				mGetCurrentLocation = false;
				mGetCurrentLocationWebview = null;
				mGetCurrentLocationCallbackId = null;
				mGetCLocationClient.unRegisterLocationListener(this);
				mGetCLocationClientOption.setOpenGps(false);
				mGetCLocationClient.stop();
			}
		}

	};
	private  void start(){
		synchronized(mWatchLocationClient){
			if(!mWatchLocationClient.isStarted() && mUseCount == 0){
				mWatchLocationClient.registerLocationListener(mWatchBDLocationListenerImpl);
				mWatchLocationClient.start();
			}
		}
		count(1);
	}

	private JSONObject makeJSON(BDLocation pLoc,String coordsType){
		JSONObject json = null;
		try {
			json = new JSONObject();
			json.put("latitude",pLoc.getLatitude());//维度
			json.put("longitude",pLoc.getLongitude());//经度
			json.put("altitude",pLoc.getAltitude());// 海拔信息
			json.put("accuracy",pLoc.getRadius()); //精确度
			json.put("altitudeAccuracy",0);//海拔精确度
			json.put("heading",pLoc.getDirection());//方向
			json.put("velocity",pLoc.getSpeed());//移动速度
			json.put("coordsType",coordsType);//坐标类型
			try {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				ParsePosition pos = new ParsePosition(0);
				Date strtodate = formatter.parse(pLoc.getTime(), pos);
				json.put("timestamp",strtodate.getTime());//时间戳
			} catch (Exception e) {
				e.printStackTrace();
				json.put("timestamp",pLoc.getTime());
			}
			if (isGeocode) {
				JSONObject address = new JSONObject();
				json.put("address",address); //获取到地理位置对应的地址信息
				address.put("country",pLoc.getCountry()); //国家
				address.put("province",pLoc.getProvince()); //省份名称
				address.put("city",pLoc.getCity()); //城市名称
				address.put("district",pLoc.getDistrict()); // 区（县）名称
				address.put("street",pLoc.getStreet()); //街道和门牌信息
				address.put("streetNum",pLoc.getStreetNumber()); //街道和门牌信息
				address.put("poiName",null); //POI信息
				address.put("postalCode",null); //邮政编码
				address.put("cityCode",pLoc.getCityCode()); //城市代码
				json.put("addresses",pLoc.getAddrStr());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		saveGeoData(pLoc, coordsType);
		return json;
	}

	/**
	 *
	 * Description:结束定位
	 * @param key 定位监听器ID
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:43:08</pre>
	 */
	public void stop() {
		count(-1);
		if(mUseCount <= 0){
			watchWebview = null;
			watchCallbackId = null;
			mWatchLocationClient.unRegisterLocationListener(mWatchBDLocationListenerImpl);
			mWatchLocationClientOption.setOpenGps(false);
			if(mWatchLocationClient.isStarted()){
				mWatchLocationClient.stop();
			}
			mUseCount = 0;
		}
		Logger.d(Tag, "stop mUseCount=" + mUseCount);
	}


	boolean mGetCurrentLocation = false;
	String mGetCurrentLocationCallbackId = null;
	IWebview mGetCurrentLocationWebview = null;
	public void getCurrentLocation(IWebview pWebViewImpl, String pCallbackId, boolean enableHighAccuracy, int timeout, String coordsType) {
		if(hasAppkey){
			if (mGetCLocationClient == null) {
				mGetCLocationClient = new LocationClient(pWebViewImpl.getContext());
			}
			mGetCLocationClientOption = new LocationClientOption();
			mGetCLocationClientOption.setOpenGps(true);
			mGetCLocationClientOption.setIsNeedAddress(isGeocode);
			//0，即仅定位一次
			mGetCLocationClientOption.setScanSpan(0);
			mGetCLocationClientOption.setTimeOut(timeout);
			mGetCurrentLocationCallbackId = pCallbackId;
			mGetCurrentLocationWebview = pWebViewImpl;
			mGetCurrentLocationCoordsType = getCoorType(coordsType);
			mGetCLocationClientOption.setCoorType(mGetCurrentLocationCoordsType);
			mGetCLocationClient.setLocOption(mGetCLocationClientOption);
			mGetCurrentLocation = true;
			mGetCLocationClient.registerLocationListener(mGetCBDLocationListenerImpl);
			mGetCLocationClient.start();
		}else{
			String _json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_HASNT_BAIDU_APPKEY,DOMException.MSG_GEOLOCATION_HASNT_BAIDU_APKEY);
			JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.ERROR, true, false);
		}

	}
	private String getCoorType(String coorType){
		//gcj02 bd09 bd09ll wgs84
		if(PdrUtil.isEquals(coorType, "gcj02")){
			return "gcj02";
		}else if(PdrUtil.isEquals(coorType, "bd09")){
			return "bd09";
		}else{
			return "bd09ll";
		}
	}

	IWebview watchWebview = null;
	String watchCallbackId = null;
	/**
	 *
	 * Description:开启定位
	 * @param pWebViewImpl WEBVIEW对象
	 * @param pCallbackId 回调方法ID
	 * @param key 定位监听者ID
	 * @param enableHighAccuracy 是否是高精度（目前没用到）
	 * @param coordsType TODO
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:42:50</pre>
	 */
	public String watch(IWebview pWebViewImpl, String pCallbackId, String key, boolean enableHighAccuracy, String coordsType, int timeOut, int intervals) {
		if(hasAppkey){
			if(mWatchLocationClient==null){
				mWatchLocationClient = new LocationClient(pWebViewImpl.getContext());
			}
			mWatchLocationClientOption = new LocationClientOption();
			mWatchLocationClientOption.setOpenGps(true);
			mWatchLocationClientOption.setScanSpan(intervals);
			mWatchLocationClientOption.setIsNeedAddress(isGeocode);
			mWatchLocationClientOption.setTimeOut(timeOut);
			watchWebview = pWebViewImpl;
			watchCallbackId = pCallbackId;
			if(enableHighAccuracy){
				mWatchLocationClientOption.setLocationMode(LocationMode.Hight_Accuracy);//4.1以上支持
			}else{
				mWatchLocationClientOption.setLocationMode(LocationMode.Device_Sensors);
			}
			mCoordsType = getCoorType(coordsType);
			mWatchLocationClientOption.setCoorType(mCoordsType);
			mWatchLocationClient.setLocOption(mWatchLocationClientOption);
			start();
			keySet.add(key);
		}else{
			String _json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_HASNT_BAIDU_APPKEY,DOMException.MSG_GEOLOCATION_HASNT_BAIDU_APKEY);
			JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.ERROR, true, false);
		}

		return key;
	}

	@Override
	public void onDestroy() {
		mWatchLocationClientOption.setIgnoreKillProcess(false);//设置是否退出定位进程
		mWatchLocationClient.setLocOption(null);
	}

	/**如果为5+应用 进行定位数据存储*/
	private void saveGeoData(BDLocation pLoc, String coordsType) {
		if (!isStreamApp) {
			JSONObject jsonObject = new JSONObject();
			JSONObject coordsJson = new JSONObject();

			try {
				coordsJson.put("latitude", pLoc.getLatitude());
				coordsJson.put("longitude", pLoc.getLongitude());
				jsonObject.put("coords", coordsJson);
				jsonObject.put("coordsType", coordsType);
				if (isGeocode) {
					jsonObject.put("addresses", pLoc.getAddrStr());
				}
				SharedPreferences startSp = SP.getOrCreateBundle(StringConst.START_STATISTICS_DATA);
				SP.setBundleData(startSp, StringConst.GEO_DATA, jsonObject.toString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void geoDataError(IWebview pWebViewImpl, String pCallbackId) {
		String err = String.format(DOMException.JSON_ERROR_INFO, 40, "定位异常");
		JSUtil.execCallback(pWebViewImpl, pCallbackId, err, JSUtil.ERROR, true, false);
	}

}
