package io.dcloud.js.geolocation;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

import java.lang.reflect.Constructor;

import android.content.Context;
import android.util.Log;

public class GeoOptDispatcher {

	AbsMgr mFeatureMgr = null;
	GeoManagerBase mBaiduGeoManager = null;
	GeoManagerBase mAMapGeoManager = null;
	GeoManagerBase mGpsGeoManager = null;
//	GeoManagerBase mQihooGeoManager = null;
	GeoManagerBase mQihooBrowserGeoManager = null;
	boolean mIsLightApp = false;
	public GeoOptDispatcher(AbsMgr mgr) {
		mFeatureMgr = mgr;
		mIsLightApp = BaseInfo.isForQihooHelper(mFeatureMgr.getContext());
	}
	private GeoManagerBase initGeoManager(String clsName){
		try {
			GeoManagerBase impl = null;
			if (clsName.equals("io.dcloud.js.geolocation.amap.AMapGeoManager")) {
                if (BaseInfo.isStreamSDK()&&PdrUtil.isEmpty(AndroidResources.getMetaValue("com.amap.api.v2.apikey"))) {
                    return null;
                }else{
                    impl = (GeoManagerBase) PlatformUtil.invokeMethod(clsName, "getInstance", null, new Class[]{Context.class}, new Object[]{mFeatureMgr.getContext()});
                    if (impl != null) {
                        return impl;
                    }
                }
			}
			Class cls = Class.forName(clsName);
			Constructor c = cls.getConstructor(Context.class);
			impl =  (GeoManagerBase)c.newInstance(new Object[]{mFeatureMgr.getContext()});
			return impl;
		} catch (Exception e) {
			Log.w("geoLoaction", clsName + " exception");
		}
		return null;
	}
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		GeoManagerBase optGeoManager = null;
		boolean isClearWatchOpt = "clearWatch".equals(pActionName);
		if(isClearWatchOpt){
//			if(!mIsLightApp){
				if(mAMapGeoManager != null && mAMapGeoManager.hasKey(pJsArgs[0])){
					optGeoManager = mAMapGeoManager;
				}else if(mBaiduGeoManager != null && mBaiduGeoManager.hasKey(pJsArgs[0])){
					optGeoManager = mBaiduGeoManager;
				}else if(mGpsGeoManager != null && mGpsGeoManager.hasKey(pJsArgs[0])){
					optGeoManager = mGpsGeoManager;
				}
//			}else{
//				if(mQihooGeoManager != null && mQihooGeoManager.hasKey(pJsArgs[0])){
//					optGeoManager = mQihooGeoManager;
//				}
//			}
		}else{
//			if(mIsLightApp ){
//				if(mQihooGeoManager == null){
//					mQihooGeoManager = initGeoManager("io.dcloud.js.geolocation.qihoo.QihooGeoManager");
//				}
//				optGeoManager = mQihooGeoManager;
//			}else{
				String provider = pJsArgs[4];
				optGeoManager = initBestGeoManager(provider);
//			}
		}
		if(optGeoManager != null){
			optGeoManager.execute(pWebViewImpl, pActionName, pJsArgs);
		}else if(!isClearWatchOpt){
			String _json = DOMException.toJSON(DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,DOMException.MSG_GEOLOCATION_PROVIDER_ERROR);
			JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
		}
		return null;
	}
	/**
	 * 初始化最何时的定位引擎，
	 * @param provider
	 * @return
	 */
	private GeoManagerBase initBestGeoManager(String provider) {
		GeoManagerBase optGeoManager = null;
		//是否尝试优先级寻找
		if(((!"system".equals(provider) && !"sytem".equals(provider)) && !"baidu".equals(provider))){// "amap".equals(provider) ||
			optGeoManager = mAMapGeoManager = (mAMapGeoManager == null ?  initGeoManager("io.dcloud.js.geolocation.amap.AMapGeoManager") :mAMapGeoManager);
			if(optGeoManager == null){
				optGeoManager = mBaiduGeoManager = (mBaiduGeoManager == null ? initGeoManager("io.dcloud.js.geolocation.baidu.BaiduGeoManager") : mBaiduGeoManager);
			}
		}else if("baidu".equals(provider)){
			optGeoManager = mBaiduGeoManager = (mBaiduGeoManager == null ? initGeoManager("io.dcloud.js.geolocation.baidu.BaiduGeoManager") : mBaiduGeoManager);
			if(optGeoManager == null){
				optGeoManager = mAMapGeoManager = (mAMapGeoManager == null ?  initGeoManager("io.dcloud.js.geolocation.amap.AMapGeoManager") :mAMapGeoManager);
			}
		}
		// 360浏览器拆件需要特殊处理
		if(optGeoManager == null && BaseInfo.isForQihooBrowser(mFeatureMgr.getContext())) {
			optGeoManager = mQihooBrowserGeoManager = (mQihooBrowserGeoManager == null ? initGeoManager("io.dcloud.js.geolocation.browser.Browser360GeoManager"): mQihooBrowserGeoManager);
		}
		if(optGeoManager == null){//走到此处代表高德百度定位引擎均未能成功初始化
			optGeoManager = mGpsGeoManager = (mGpsGeoManager == null ? initGeoManager("io.dcloud.js.geolocation.system.LocalGeoManager") : mGpsGeoManager);
		}

        //流应用SDK会将AMapGeoManager和BaiduGeoManage及LocalGeoManager均打入jar包，故此选择合适的定位provider时，
        //还需要判断清单文件是否进行了完整的配置
        //    如果清单文件中也配置完整，则选择完整的那个provider进行定位
        //    如果清单文件中对高德和百度均配置了apikey,则按照5+api的优先级进行选择。
        if (BaseInfo.isStreamSDK()){
            if (!PdrUtil.isEmpty(mAMapGeoManager)) {
                if (PdrUtil.isEmpty(AndroidResources.getMetaValue("com.amap.api.v2.apikey"))){
                    if (!PdrUtil.isEmpty(mBaiduGeoManager)) {
                        if (PdrUtil.isEmpty(AndroidResources.getMetaValue("com.baidu.lbsapi.API_KEY"))){
                            return mGpsGeoManager;
                        }else{
                            return mBaiduGeoManager;
                        }
                    }
                }else{
                    return mAMapGeoManager;
                }
            }else{
                if (!PdrUtil.isEmpty(mBaiduGeoManager)) {
                    if (PdrUtil.isEmpty(AndroidResources.getMetaValue("com.baidu.lbsapi.API_KEY"))){
                        return optGeoManager = mGpsGeoManager = (mGpsGeoManager == null ? initGeoManager("io.dcloud.js.geolocation.system.LocalGeoManager") : mGpsGeoManager);
                    }else{
                        return mBaiduGeoManager;
                    }
                }else{
                    return mGpsGeoManager;
                }

            }
        }
		return optGeoManager;
	}
	
	 public void onDestroy(){
		 if(mBaiduGeoManager != null){
			 mBaiduGeoManager.onDestroy();
		 }
		 if(mGpsGeoManager != null){
			 mGpsGeoManager.onDestroy();
		 }
		 if(mAMapGeoManager != null){
			 mAMapGeoManager.onDestroy();
		 }
//		 if(mQihooGeoManager != null){
//			 mQihooGeoManager.onDestroy();
//		 }
	 }
	 
}
