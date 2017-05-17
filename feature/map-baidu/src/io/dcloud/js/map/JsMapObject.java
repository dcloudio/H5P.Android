package io.dcloud.js.map;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.js.map.adapter.DHMapView;

import org.json.JSONArray;

import com.baidu.mapapi.map.MapView;

/**
 * <p>Description:JS对象类</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-12-24 下午4:02:21 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-24 下午4:02:21</pre>
 */
public abstract class JsMapObject {
	protected DHMapView mMapView = null;
	protected IWebview mWebview = null;
	String mUUID = null; 
	protected JsMapObject(IWebview pWebview){
		mWebview = pWebview;
	}
	
	void setUUID(String uuid){
		mUUID = uuid;
	}
	/**
	 * 
	 * Description:创建js中对应的native对象
	 * @param pJsArgs
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-5 下午3:14:08</pre>
	 */
	protected abstract void createObject(JSONArray pJsArgs);
	/**
	 * 
	 * Description:更新js中对应的native对象
	 * @param pStrEvent
	 * @param pJsArgs
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-5 下午3:14:11</pre>
	 */
	protected abstract void updateObject(String pStrEvent,JSONArray pJsArgs);
	
	protected String updateObjectSYNC(String pStrEvent,JSONArray pJsArgs){
		return null;
	}
	
	/**
	 * 当要添加到MapView的时候执行此事件
	 * @param pMapView
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:00:09
	 */
	public void onAddToMapView(DHMapView pMapView){
		mMapView = pMapView;
	}
}
