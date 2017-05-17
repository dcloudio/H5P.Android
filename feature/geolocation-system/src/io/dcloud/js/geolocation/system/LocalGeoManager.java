package io.dcloud.js.geolocation.system;

import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.geolocation.GeoManagerBase;


import android.content.Context;


/**
 * 
 * <p>Description:定位管理者</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-11 下午5:21:47 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-11 下午5:21:47</pre>
 */
public class LocalGeoManager extends GeoManagerBase{
    public static final String TAG=LocalGeoManager.class.getSimpleName();
    /**
     * 用来监听getCurrentPosition定位
     */
	private GeoListener mGeoLtr;
	
	public LocalGeoManager(Context pContext) {
		super(pContext);
	}
	
/**
 * 
 * Description: 执行分发
 * @param pWebViewImpl
 * @param pActionName
 * @param pJsArgs
 * @return
 *
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:58:39</pre>
 */
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		String result = "";		
		try {
			if (pActionName.equals("getCurrentPosition")) {
                boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[1]);
				int _maximumAge = Integer.parseInt(pJsArgs[2]);
				boolean iswgs84 = PdrUtil.isEquals(pJsArgs[3], "wgs84") || PdrUtil.isEmpty(pJsArgs[3]);
				if(iswgs84){
					getCurrentLocation(pWebViewImpl, pJsArgs[0], _enableHighAccuracy, _maximumAge);
				}else{
					String _json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,"only support wgs84");
					JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
				}
			}
			else if (pActionName.equals("watchPosition")) {
				boolean _enableHighAccuracy = Boolean.parseBoolean(pJsArgs[2]);
				pWebViewImpl.obtainFrameView().addFrameViewListener(new IEventCallback() {
					@Override
					public Object onCallBack(String pEventType, Object pArgs) {
						if((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) && pArgs instanceof IWebview){
							if(mGeoLtr != null) mGeoLtr.stop(GeoListener.LOCATION_BY_BOTH);
							((AdaFrameView)((IWebview)pArgs).obtainFrameView()).removeFrameViewListener(this);
						}
						return null;
					}
				});
				boolean iswgs84 = PdrUtil.isEquals(pJsArgs[3], "wgs84") || PdrUtil.isEmpty(pJsArgs[3]);
				String t = pJsArgs.length > 7 ? pJsArgs[6] : "null";//不设置为"null"字符串
				int timeout = GeoListener.LOCATION_TIME_OUT;
				if(!"null".equals(t)){
					timeout = Integer.parseInt(t);
				}
				String intervals = pJsArgs.length > 8 ? pJsArgs[7] : "5000";
				int interval = 5000;
				if (!intervals.equals("null")) {
					interval = Integer.parseInt(intervals);
				}
				if(iswgs84){
					start(pWebViewImpl, pJsArgs[0], pJsArgs[1], _enableHighAccuracy, timeout, interval);
				}else{
					String _json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_GEOLOCATION_PROVIDER_ERROR,"only support wgs84");
					JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
				}
			}
			else if (pActionName.equals("clearWatch")) {
				stop(pJsArgs[0]);
			}
			return result;
		} catch (Exception e) {
			return result;
		}
	}
    
    /**
     * 
     * Description:摧毁管理者
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:58:07</pre>
     */
    public void onDestroy() {
        if (mGeoLtr != null) {
        	mGeoLtr.destroy();
        }
        mGeoLtr = null;
    }


    /**
     * 
     * Description: 获取当前定位信息
     * @param pWebViewImpl WEBVIEW对象
     * @param pCallbackId 回调方法ID
     * @param enableHighAccuracy  是否高精度
     * @param maximumAge 间隔时间
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:53:00</pre>
     */
	public void getCurrentLocation(IWebview pWebViewImpl, String pCallbackId, boolean enableHighAccuracy, int maximumAge) {
		initGeoListener().getCurrentLocation(pWebViewImpl, maximumAge, pCallbackId);
	}
	
	/**
	 * 
	 * Description:开启定位
	 * @param pWebViewImpl WEBVIEW对象
	 * @param pCallbackId 回调方法ID
	 * @param key 定位监听者ID
	 * @param enableHighAccuracy 是否是高精度（目前没用到）
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-12 上午11:42:50</pre>
	 */
	public void start(IWebview pWebViewImpl, String pCallbackId, String key, boolean enableHighAccuracy, int intervals, int timeout) {
		if(initGeoListener().start(pWebViewImpl, intervals, pCallbackId, timeout)){
			keySet.add(key);
		}
	}
	
	GeoListener initGeoListener(){
		if(mGeoLtr == null){
			mGeoLtr = new GeoListener(mContext, "");
		}
		return mGeoLtr;
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
	public void stop(String key) {
		if(mGeoLtr != null && keySet.contains(key)){
			keySet.remove(key);
			mGeoLtr.stop(GeoListener.LOCATION_BY_BOTH);
		}
	}
}
