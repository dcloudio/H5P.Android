package io.dcloud.js.map.amap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;

import java.util.ArrayList;
import java.util.HashMap;

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
	 * DHMapManager实例对象
	 */
	private static JsMapManager mJsMapManager;
	/**
	 * 保存route对象
	 */
	private HashMap<String, JsMapRoute> mMapRoutes;
	/**
	 * 
	 * Description: 构造函数 
	 * @param pFrameView
	 * @param pJsId 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:37:18</pre>
	 */
	private JsMapManager(){
		mJsMapObjects = new HashMap<String, JsMapObject>();
		mMapRoutes = new HashMap<String, JsMapRoute>();
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
	 * 
	 * Description:通过JSON对象转换为mappoin
	 * @param pWebview TODO
	 * @param pJson
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
}
