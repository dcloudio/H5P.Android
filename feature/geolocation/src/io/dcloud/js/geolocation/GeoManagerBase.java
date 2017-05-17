package io.dcloud.js.geolocation;

import java.util.ArrayList;

import android.content.Context;
import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;

public abstract class GeoManagerBase implements IReflectAble{

	protected ArrayList<String> keySet = null;
	/**
	 * 上下文对象
	 */
	protected Context mContext;
	
	public GeoManagerBase(Context pContext){
		mContext = pContext;
		keySet = new ArrayList<String>();
	}
	public boolean hasKey(String key){
		return keySet.contains(key);
	}
	public abstract String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) ;
	public abstract void onDestroy();
}
