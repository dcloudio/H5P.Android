package io.dcloud.js.map.amap;

import android.text.TextUtils;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * <p>Description:JS地图管理者</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-8 下午3:09:24 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:09:24</pre>
 */
public class JsMapManager {
	/**
	 * 保存所有的mapApi对象对应的Js对象键值对
	 */
	private HashMap<String,JsMapObject> mJsMapObjects;

	/**
	 * 保存所有jsmap对象
	 */
	private HashMap<String, LinkedHashMap<String, JsMapView>> mJsMapViews;
	/**
	 * DHMapManager实例对象
	 */
	private static JsMapManager mJsMapManager;
	/**
	 * 保存route对象
	 */
	private HashMap<String, JsMapRoute> mMapRoutes;

	private AbsMgr mFeatureMgr;

	public void setFeatureMgr(AbsMgr pFeatureMgr) {
		mFeatureMgr = pFeatureMgr;
	}
	/**
	 * 
	 * Description: 构造函数
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:37:18</pre>
	 */
	private JsMapManager(){
		mJsMapObjects = new HashMap<String, JsMapObject>();
		mMapRoutes = new HashMap<String, JsMapRoute>();
		mJsMapViews = new HashMap<String, LinkedHashMap<String, JsMapView>>();
	}
	/**
	 * 
	 * Description:获取DHMapManager对象
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 上午11:42:09</pre>
	 */
	public static JsMapManager getJsMapManager(){
		if(mJsMapManager == null){
			mJsMapManager = new JsMapManager();
		}
		return mJsMapManager;
	}

	public void dispose(String appid) {
		//mJsMapObjects.clear();
		LinkedHashMap<String, JsMapView> maps = mJsMapViews.remove(appid);
		if(maps != null) {
			for(String key : maps.keySet()) {
				maps.get(key).dispose();
			}
			maps.clear();
		}
	}

	public JsMapView getJsMapViewById(String appid, String id) {
		if(mJsMapViews.containsKey(appid)) {
			LinkedHashMap<String, JsMapView> maps = mJsMapViews.get(appid);
			for(String key: maps.keySet()) {
				String itmeId = maps.get(key).mJsId;
				if(!TextUtils.isEmpty(itmeId) && id.equals(itmeId)){
					return maps.get(key);
				}
			}
		}
		return null;
	}

	public JsMapView getJsMapViewByUuid(String appid, String uuid) {
		if(mJsMapViews.containsKey(appid)) {
			LinkedHashMap<String, JsMapView> maps = mJsMapViews.get(appid);
			if(maps.containsKey(uuid)) {
				return maps.get(uuid);
			}
		}
		return null;
	}

	public void putJsMapView(String appid, String uuid, JsMapView mapView) {
		if(!mJsMapViews.containsKey(appid)) {
			LinkedHashMap<String, JsMapView> maps = new LinkedHashMap<String, JsMapView>();
			mJsMapViews.put(appid, maps);
		}
		if(!mJsMapViews.get(appid).containsKey(uuid)) {
			mJsMapViews.get(appid).put(uuid, mapView);
		}
	}

	public void removeJsMapView(String appid, String uuid) {
		if(mJsMapViews.containsKey(appid)) {
			LinkedHashMap<String, JsMapView> maps = mJsMapViews.get(appid);
			if(maps.containsKey(uuid)) {
				maps.remove(uuid);
			}
		}
	}
	/**
	 * 
	 * Description:保存Js对应Java对象
	 * @param pId
	 * @param pJsMapObjects
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-9 上午9:44:44</pre>
	 */
	public void putJsObject(String pId,JsMapObject pJsMapObjects){
		mJsMapObjects.put(pId, pJsMapObjects);
	}
	/**
	 * 
	 * Description:取出Js对应Java对象
	 * @param pId
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-9 上午9:44:55</pre>
	 */
	public JsMapObject getJsObject(String pId){
		return mJsMapObjects.get(pId);
	}

	/**
	 * 删除Js对应Java对象
	 * @param appid
	 * @param uuid
	 * @return
	 */
	public JsMapObject removeJsObject(String appid, String uuid){
		JsMapObject jsObject = mJsMapObjects.remove(uuid);
		if(jsObject instanceof JsMapView) {
			removeJsMapView(appid, uuid);
		}
		return jsObject;
	}

	/**
	 * 
	 * Description:通过JSON对象转换为mappoin
	 * @param pWebview TODO
	 * @param _json
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-19 下午4:45:45</pre>
	 */
	public JsMapPoint getMapPoint(IWebview pWebview, JSONObject _json){
		JsMapPoint _point = null;
		try {
			if(_json != null){
				String _longitude = _json.getString("longitude");
				String _latitude = _json.getString("latitude");
				_point = new JsMapPoint(pWebview,_latitude, _longitude);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _point;
	}
	/**
	 * 
	 * Description:JSON转换为point集合
	 * @param pJson
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 下午6:43:48</pre>
	 */
	public ArrayList<JsMapPoint> getJsToPointArry(IWebview pWebview,String pJson) {
		ArrayList<JsMapPoint> arry = new ArrayList<JsMapPoint>();
		if(pJson != null){
			try {
				JSONArray _jsonA = new JSONArray(pJson);
				JsMapPoint _p=null;
				for(int i=0;i<_jsonA.length(); i++){
					_jsonA.get(i);
					_p = getMapPoint(pWebview, JSONUtil.getJSONObject(_jsonA,i));
					arry.add(_p);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		return arry;
	}
	/**
	 * 
	 * Description:JSON转换为String集合
	 * @param pJson
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 下午6:43:48</pre>
	 */
	public static ArrayList<String> getStrToStrArry(String pJson) {
		ArrayList<String> arry = new ArrayList<String>();
		if(pJson != null){
			try {
				JSONArray _jsonA = new JSONArray(pJson);
				for(int i=0;i<_jsonA.length(); i++){
					String _p = null;
					if(_jsonA.get(i) instanceof String){
						_p = (String)_jsonA.get(i);
					}else{
						_p = _jsonA.get(i).toString();
					}
					arry.add(_p);
				}
				_jsonA = null;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return arry;
	}
	
	/**
	 * 
	 * Description:字符串TO颜色值
	 * @param pString
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-19 下午4:25:22</pre>
	 */
	public static int hexString2Int(String pString){
		int _ret = 0;
		if(pString != null) {
			pString = pString.trim();
			if (pString.startsWith("#")) {
				pString = pString.substring(1);
			} 
			if (pString.length() == 8) {
				pString = pString.substring(2);
			}
			try {
				_ret = Integer.valueOf(pString, 16).intValue();
			} catch (NumberFormatException ex) {
				_ret = 0;
			}
		}
		return _ret;
	}

	public IWebview findWebviewByUuid(IWebview webview, String uuid) {
		if(mFeatureMgr != null) {
			Object object =  mFeatureMgr.processEvent(IMgr.MgrType.FeatureMgr, IMgr.MgrEvent.CALL_WAITER_DO_SOMETHING,new Object[]{webview,"ui","findWebview",new String[]{webview.obtainApp().obtainAppId(), uuid}});
			if(object != null) {
				return (IWebview) object;
			}
		}
		return null;
	}
}
