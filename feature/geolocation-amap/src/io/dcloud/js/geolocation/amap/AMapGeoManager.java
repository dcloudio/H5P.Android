package io.dcloud.js.geolocation.amap;

import android.content.Context;
import android.content.SharedPreferences;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.json.JSONException;
import org.json.JSONObject;

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

public class AMapGeoManager extends GeoManagerBase {
    public static final String TAG = AMapGeoManager.class.getSimpleName();
    boolean hasAppkey = false;
    // 是否解析完整地理信息
    boolean isGeocode = true;
    boolean isStreamApp = false;
    static AMapGeoManager mInstance;

    AMapLocationClient mClient = null;
    AMapLocationClientOption mOption = null;

    HashMap<String, AMapLocationClient> mContinuousMap = new HashMap<String, AMapLocationClient>();
    HashMap<String, AMapLocationClient> mSingleTimeMap = new HashMap<String, AMapLocationClient>();


    public AMapGeoManager(Context pContext) {
        super(pContext);
        hasAppkey = !PdrUtil.isEmpty(AndroidResources.getMetaValue("com.amap.api.v2.apikey"));
    }

    public static AMapGeoManager getInstance(Context pContext) {
        pContext = pContext.getApplicationContext();
        if (mInstance != null) {
            return mInstance;
        } else {
            mInstance = new AMapGeoManager(pContext);
        }
        return mInstance;
    }

    @Override
    public String execute(IWebview pWebViewImpl, String pActionName,
                          String[] pJsArgs) {
        String result = "";
        try {
            isStreamApp = pWebViewImpl.obtainApp().isStreamApp();
            String t = pJsArgs.length > 7 ? pJsArgs[6] : "null";//不设置为"null"字符串
            int timeout = Integer.MAX_VALUE;
            if (!"null".equals(t)) {
                timeout = Integer.parseInt(t);
            }
            String intervals = pJsArgs.length > 8 ? pJsArgs[7] : "5000";
            int interval = 5000;
            if (!intervals.equals("null")) {
                interval = Integer.parseInt(intervals);
                if (interval < 1000) {
                    interval = 1000;
                }
            }
            if (pActionName.startsWith("getCurrentPosition")) {
                isGeocode = Boolean.parseBoolean(pJsArgs[5]);
                boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[1]);
                startLocating(pWebViewImpl, pJsArgs[0], null, _enableHighAccuracy, timeout, -1, pActionName.endsWith("DLGEO"), false);
            } else if (pActionName.startsWith("watchPosition")) {
                isGeocode = Boolean.parseBoolean(pJsArgs[5]);
                boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[2]);
                pWebViewImpl.obtainFrameView().addFrameViewListener(new IEventCallback() {
                    @Override
                    public Object onCallBack(String pEventType, Object pArgs) {
                        if ((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) && pArgs instanceof IWebview) {
                            stopContinuousLocating();
                            ((AdaFrameView) ((IWebview) pArgs).obtainFrameView()).removeFrameViewListener(this);
                        }
                        return null;
                    }
                });
                startLocating(pWebViewImpl, pJsArgs[0], pJsArgs[1], _enableHighAccuracy, timeout, interval, pActionName.endsWith("DLGEO"), true);
            } else if (pActionName.startsWith("clearWatch")) {
                //mContinuousMap.get(pJsArgs[0]).stopLocation();
                keySet.remove(pJsArgs[0]);
                AMapLocationClient client = mContinuousMap.remove(pJsArgs[0]);
                if(client != null) {
                    client.setLocationListener(null);
                    client.stopLocation();
                }
            }
            return result;
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return result;
        }
    }

    public void startLocating(final IWebview pWebViewImpl, final String pCallbackId, final String key, boolean enableHighAccuracy, int timeOut, int intervals, final boolean isDLGeo, final boolean continuous) {
        if (hasAppkey) {
            mClient = new AMapLocationClient(pWebViewImpl.getContext());
            mOption = new AMapLocationClientOption();
            mOption.setOnceLocationLatest(false);
            if (PdrUtil.isEmpty(key)) {
                mOption.setOnceLocation(true);
                mSingleTimeMap.put(pCallbackId, mClient);
            } else {
                mOption.setInterval(intervals);
                keySet.add(key);
                mContinuousMap.put(key, mClient);
            }
            if (NetTool.isNetworkAvailable(mContext)) {
                if (enableHighAccuracy) {
                    //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。需要连接网络，否则无法定位
                    mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                    mOption.setOnceLocationLatest(true);
                } else {
                    //低功耗定位模式：不会使用GPS和其他传感器，只会使用网络定位（Wi-Fi和基站定位）；
                    mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
                    mOption.setOnceLocationLatest(false);

                    //不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位，自 v2.9.0 版本支持返回地址描述信息。
                    //mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
                }
                mOption.setLocationCacheEnable(false);
                mOption.setHttpTimeOut(timeOut);
            }else{
                mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
                mOption.setOnceLocationLatest(false);
                if (Integer.MAX_VALUE==timeOut) {
                    mOption.setHttpTimeOut(3000);
                }else{
                    mOption.setHttpTimeOut(timeOut);
                }
            }

            mClient.setLocationOption(mOption);

            mClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    if(aMapLocation.getAddress() != null){
                        FeatureMessageDispatcher.dispatchMessage("record_address",aMapLocation.getAddress() != null ? aMapLocation.getAddress() : null);
                    }
                    callBack2Front(aMapLocation, pWebViewImpl, pCallbackId, isDLGeo, continuous);
                }
            });
            mClient.startLocation();
        } else {
            String _json = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_GEOLOCATION_HASNT_BAIDU_APPKEY, DOMException.MSG_GEOLOCATION_HASNT_AMAP_KEY);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.ERROR, true, false);
        }

    }

    /**
     * 关闭页面时，停止持续定位
     */
    private void stopContinuousLocating() {
        for (Map.Entry<String, AMapLocationClient> entry : mContinuousMap.entrySet()) {
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
            if (!PdrUtil.isEmpty(entry.getValue())) {
                entry.getValue().stopLocation();
            }
        }
    }


    private JSONObject makeJSON(AMapLocation pLoc) {
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", pLoc.getLatitude());//维度
            json.put("longitude", pLoc.getLongitude());//经度
            json.put("altitude", pLoc.getAltitude());// 海拔信息
            json.put("accuracy", pLoc.getAccuracy()); //精确度
            json.put("altitudeAccuracy", 0);//海拔精确度
            json.put("heading", pLoc.getBearing());//方向
            json.put("velocity", pLoc.getSpeed());//移动速度
            json.put("coordsType", "gcj02");//坐标类型
            json.put("timestamp", pLoc.getTime());//时间戳
            if (isGeocode) {
                JSONObject address = new JSONObject();
                json.put("address", address); //获取到地理位置对应的地址信息
                address.put("country", pLoc.getCountry()); //国家
                address.put("province", pLoc.getProvince()); //省份名称
                address.put("city", pLoc.getCity()); //城市名称
                address.put("district", pLoc.getDistrict()); // 区（县）名称
                address.put("street", pLoc.getStreet()); //街道和门牌信息
                address.put("streetNum", pLoc.getStreetNum()); //街道和门牌信息
                address.put("poiName", pLoc.getPoiName()); //POI信息
                address.put("postalCode", null); //邮政编码
                address.put("cityCode", pLoc.getCityCode()); //城市代码
                json.put("addresses", pLoc.getAddress());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        saveGeoData(pLoc, "gcj02");
        return json;
    }

    @Override
    public void onDestroy() {
        for (Map.Entry<String, AMapLocationClient> entry : mContinuousMap.entrySet()) {
            if (!PdrUtil.isEmpty(entry.getValue())) {
                entry.getValue().onDestroy();
            }
        }
        mContinuousMap.clear();
        for (Map.Entry<String, AMapLocationClient> entry : mSingleTimeMap.entrySet()) {
            if (!PdrUtil.isEmpty(entry.getValue())) {
                entry.getValue().onDestroy();
            }
        }
        mSingleTimeMap.clear();
    }

    /**
     * 返回结果给前端网页
     * @param location
     * @param pWebViewImpl
     * @param callbackId
     */
    private void callBack2Front(AMapLocation location, IWebview pWebViewImpl, String callbackId, boolean isDLGeo,boolean continuous) {
        if (!continuous) {//非持续定位，得到定位结果后，停止
            if (!PdrUtil.isEmpty(mSingleTimeMap.get(callbackId))) {
                mSingleTimeMap.get(callbackId).stopLocation();
            }
        }
        if (location != null && location.getErrorCode() == 0) {
            JSONObject json = makeJSON(location);
            callback(pWebViewImpl, callbackId, json.toString(), JSUtil.OK, true, continuous, isDLGeo);
        } else {
            String _json = null;
            if (location == null) {
                String message = DOMException.toString(DOMException.CODE_GEOLOCATION_PROVIDER_ERROR, "geolocation", DOMException.MSG_GEOLOCATION_PROVIDER_ERROR, null);
                _json = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_GEOLOCATION_PROVIDER_ERROR, message);
            } else {
                String message = DOMException.toString(location.getErrorCode(), "geolocation", location.getErrorInfo(), null);
                _json = StringUtil.format(DOMException.JSON_ERROR_INFO, convertGeolocationErrorCode(location.getErrorCode()), message);
            }
            callback(pWebViewImpl, callbackId, _json, JSUtil.ERROR, true, continuous, isDLGeo);
        }
    }

    /**
     * 将amap的errorcode转为前端api对应errorcode
     *
     * @param sdkCode
     * @return
     */
    private int convertGeolocationErrorCode(int sdkCode) {
        int ret;
        switch (sdkCode) {
            case AMapLocation.ERROR_CODE_FAILURE_LOCATION_PERMISSION://缺少定位权限
                ret = 1;//访问权限被拒绝
                break;
            case AMapLocation.ERROR_CODE_FAILURE_WIFI_INFO://定位失败，由于仅扫描到单个wifi，且没有基站信息。
            case AMapLocation.ERROR_CODE_FAILURE_NOWIFIANDAP://定位失败，由于设备未开启WIFI模块或未插入SIM卡，且GPS当前不可用。
            case AMapLocation.ERROR_CODE_FAILURE_NOENOUGHSATELLITES://GPS 定位失败，由于设备当前 GPS 状态差。
            case AMapLocation.ERROR_CODE_FAILURE_SIMULATION_LOCATION://定位结果被模拟导致定位失败
                ret = 2;//位置信息不可用
                break;
            case AMapLocation.ERROR_CODE_FAILURE_CONNECTION://请求服务器过程中的异常，多为网络情况差，链路不通导致
                ret = 3;//获取位置信息超时
                break;
            default:
                ret = 4;//未知错误
        }
        return ret;
    }

    /**
     * 如果为5+应用 进行定位数据存储
     */
    private void saveGeoData(AMapLocation pLoc, String coordsType) {
        if (!isStreamApp) {
            JSONObject jsonObject = new JSONObject();
            JSONObject coordsJson = new JSONObject();
            try {
                coordsJson.put("latitude", pLoc.getLatitude());
                coordsJson.put("longitude", pLoc.getLongitude());
                jsonObject.put("coords", coordsJson);
                jsonObject.put("coordsType", coordsType);
                if (isGeocode) {
                    jsonObject.put("addresses", pLoc.getAddress());
                }
                SharedPreferences startSp = SP.getOrCreateBundle(StringConst.START_STATISTICS_DATA);
                SP.setBundleData(startSp, StringConst.GEO_DATA, jsonObject.toString());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void callback(IWebview webview, String callId, String json, int code, boolean isJson, boolean keback, boolean isDLGeo) {
        if (isDLGeo) {
            JSUtil.execGEOCallback(webview, callId, json, code, isJson, keback);
        } else {
            JSUtil.execCallback(webview, callId, json, code, isJson, keback);
        }
    }
}
