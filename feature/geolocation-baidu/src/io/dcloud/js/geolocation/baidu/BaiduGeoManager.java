package io.dcloud.js.geolocation.baidu;

import android.content.Context;
import android.content.SharedPreferences;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.SP;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.js.geolocation.GeoManagerBase;

public class BaiduGeoManager extends GeoManagerBase{


    public static final String TAG = BaiduGeoManager.class.getSimpleName();
    /**是否存在百度key*/
	boolean hasAppkey = false;
	// 是否解析完整地理信息
	boolean isGeocode = true;
    boolean isStreamApp = false;
    static BaiduGeoManager mInstance;

    LocationClient mClient = null;
    LocationClientOption mOption = null;
    HashMap<String, LocationClient> mContinuousMap = new HashMap<String, LocationClient>();
    HashMap<String, LocationClient> mSingleTimeMap = new HashMap<String, LocationClient>();



    public BaiduGeoManager(Context pContext) {
		super(pContext);
		hasAppkey = !PdrUtil.isEmpty(AndroidResources.getMetaValue("com.baidu.lbsapi.API_KEY"));
	}

    public static BaiduGeoManager getInstance(Context pContext) {
        pContext = pContext.getApplicationContext();
        if (mInstance != null) {
            return mInstance;
        } else {
            mInstance = new BaiduGeoManager(pContext);
        }
        return mInstance;
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
			if (pActionName.startsWith("getCurrentPosition")) {
				isGeocode = Boolean.parseBoolean(pJsArgs[5]);
				boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[1]);
				boolean isNotWgs84 = !PdrUtil.isEquals("wgs84", pJsArgs[3]);
				if(/*PdrUtil.isEquals("baidu", pJsArgs[4]) &&*/ isNotWgs84){
                    startLocating(pWebViewImpl, pJsArgs[0], null, _enableHighAccuracy, timeout, -1, pActionName.endsWith("DLGEO"),pJsArgs[3],false);
				}else{
					String _json = StringUtil.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,isNotWgs84 ? DOMException.MSG_GEOLOCATION_PROVIDER_ERROR : "only support gcj02|bd09|bd09ll");
					JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
				}
			}
			else if (pActionName.startsWith("watchPosition")) {
				isGeocode = Boolean.parseBoolean(pJsArgs[5]);
				boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[2]);
				pWebViewImpl.obtainFrameView().addFrameViewListener(new IEventCallback() {
					@Override
					public Object onCallBack(String pEventType, Object pArgs) {
						if((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) && pArgs instanceof IWebview){
                            stopContinuousLocating();
							((AdaFrameView)((IWebview)pArgs).obtainFrameView()).removeFrameViewListener(this);
						}
						return null;
					}
				});
				boolean isNotWgs84 = !PdrUtil.isEquals("wgs84", pJsArgs[3]);
				if(/*PdrUtil.isEquals("baidu", pJsArgs[4]) &&*/ isNotWgs84){
                    startLocating(pWebViewImpl, pJsArgs[0], pJsArgs[1], _enableHighAccuracy, timeout, interval, pActionName.endsWith("DLGEO"), pJsArgs[3],true);
				}else{
					String _json = StringUtil.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,isNotWgs84 ? DOMException.MSG_GEOLOCATION_PROVIDER_ERROR : "only support gcj02|bd09|bd09ll");
					JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
				}
			}
			else if (pActionName.startsWith("clearWatch")) {
                keySet.remove(pJsArgs[0]);
                mContinuousMap.remove(pJsArgs[0]).stop();
			}
			return result;
		} catch (Exception e) {
            Logger.e(TAG,"e.getMessage()=="+e.getMessage());
			return result;
		}
	}

    public void startLocating(final IWebview pWebViewImpl, final String pCallbackId, final String key, boolean enableHighAccuracy, int timeOut, int intervals, final boolean isDLGeo, final String coordsType, final boolean continuous) {
        if (hasAppkey) {
            mClient = new LocationClient(pWebViewImpl.getContext());
            mOption = new LocationClientOption();
            if (PdrUtil.isEmpty(key)) {
                //0，即仅定位一次
                mOption.setScanSpan(0);
                mSingleTimeMap.put(pCallbackId, mClient);
            } else {
                mOption.setScanSpan(intervals);
                mOption.setLocationNotify(true);
                //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
                keySet.add(key);
                mContinuousMap.put(key, mClient);
            }
            if (NetTool.isNetworkAvailable(mContext)) {
                if (enableHighAccuracy) {
                    mOption.setLocationMode(LocationMode.Hight_Accuracy);//4.1以上支持
                } else {
                    mOption.setLocationMode(LocationMode.Battery_Saving);
                }
                mOption.setTimeOut(timeOut);
            }else{
                mOption.setLocationMode(LocationMode.Device_Sensors);
                if (Integer.MAX_VALUE==timeOut) {
                    mOption.setTimeOut(3000);
                }else{
                    mOption.setTimeOut(timeOut);
                }
            }
            mOption.setIsNeedAddress(isGeocode);
            mOption.setCoorType(getCoorType(coordsType));
            mClient.setLocOption(mOption);
            mClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
					if(bdLocation.getAddress() != null){
						FeatureMessageDispatcher.dispatchMessage("record_address",bdLocation.getAddress() != null ? bdLocation.getAddress().address : null);
					}
					Logger.e(TAG,"onReceiveLocation bdLocation=="+bdLocation.toString());
					callBack2Front(pWebViewImpl, pCallbackId,bdLocation, getCoorType(coordsType),isDLGeo,continuous);
                }
            });
            mClient.start();
        } else {
            String _json = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_GEOLOCATION_HASNT_BAIDU_APPKEY, DOMException.MSG_GEOLOCATION_HASNT_BAIDU_APKEY);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.ERROR, true, false);
        }

    }
    /**
     *关闭页面时，停止持续定位
     */
    private void stopContinuousLocating() {
        for (Map.Entry<String, LocationClient> entry : mContinuousMap.entrySet()) {
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
            if (!PdrUtil.isEmpty(entry.getValue())) {
                entry.getValue().stop();
            }
        }
    }



    private void callBack2Front(IWebview mWebview , String mCallbackId,BDLocation location,String CoordsType,boolean isDLGeo,boolean continuous) {
        if (!continuous) {//非持续定位，得到定位结果后，停止
            if(!PdrUtil.isEmpty(mSingleTimeMap.get(mCallbackId))){
                mSingleTimeMap.get(mCallbackId).stop();
            }
        }
        JSONObject _json;
        _json = makeJSON(location,CoordsType);
        if (_json == null) {
            geoDataError(mWebview, mCallbackId,isDLGeo,continuous);
        } else {
            //处于监听状态
            callback(mWebview, mCallbackId, _json.toString(), JSUtil.OK, true,isDLGeo,continuous);
        }
    }

    public void callback(IWebview webview, String callId, String json, int code, boolean isJson, boolean isDLGeo,boolean continuous) {
        if (isDLGeo) {
            JSUtil.execGEOCallback(webview, callId, json, code, isJson,continuous);
        } else {
            JSUtil.execCallback(webview, callId, json, code, isJson,continuous);
        }
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
				address.put("poiName",pLoc.getPoiList() != null && pLoc.getPoiList().size() > 0 ? pLoc.getPoiList().get(0) : null); //POI信息
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

	private String getCoorType(String coorType){
		//gcj02 bd09 bd09ll wgs84
		if(PdrUtil.isEquals(coorType, "bd09ll")){
			return "bd09ll";
		}else if(PdrUtil.isEquals(coorType, "bd09")){
			return "bd09";
		}else{
			return "gcj02";
		}
	}

	@Override
	public void onDestroy() {
        for (Map.Entry<String, LocationClient> entry : mContinuousMap.entrySet()) {
            if (!PdrUtil.isEmpty(entry.getValue())) {
                entry.getValue().stop();
            }
        }
        mContinuousMap.clear();
        for (Map.Entry<String, LocationClient> entry : mSingleTimeMap.entrySet()) {
            if (!PdrUtil.isEmpty(entry.getValue())) {
                entry.getValue().stop();
            }
        }
        mSingleTimeMap.clear();
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


	private void geoDataError(IWebview pWebViewImpl, String pCallbackId,boolean isDLGeo,boolean continuous) {
		String err = StringUtil.format(DOMException.JSON_ERROR_INFO, 40, "定位异常");
        if (isDLGeo) {
            JSUtil.execGEOCallback(pWebViewImpl, pCallbackId, err, JSUtil.ERROR, true,continuous);
        } else {
            JSUtil.execCallback(pWebViewImpl, pCallbackId, err, JSUtil.ERROR, true,continuous);
        }
	}

}
