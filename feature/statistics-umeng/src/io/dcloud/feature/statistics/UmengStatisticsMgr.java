package io.dcloud.feature.statistics;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IWebview;
import android.content.Context;
import android.text.TextUtils;

import com.umeng.analytics.MobclickAgent;

/**
 * <p>Description:友盟统计管理者</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-6 上午10:32:48 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-6 上午10:32:48</pre>
 */
public class UmengStatisticsMgr {
	/**
	 * 上下文对象
	 */
	private Context mContext;
	
	protected UmengStatisticsMgr(Context pContext){
		mContext = pContext;
	}
	/**
	 * 
	 * Description:友盟统计事件处理
	 * @param pWebViewImpl
	 * @param pActionName
	 * @param pJsArgs
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-6 上午10:49:45</pre>
	 */
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		if(pActionName.equals("eventTrig")){
			if(pJsArgs[1] == null){
				MobclickAgent.onEvent(mContext, pJsArgs[0]);
			}else{
				HashMap<String, String> vs = null;
				try {
					JSONObject json = new JSONObject(pJsArgs[1]);
					vs = new HashMap<String, String>();
					Iterator ir = json.keys();
					while(ir.hasNext()){
						String k = (String)ir.next();
						vs.put(k, json.optString(k));
					}
				} catch (JSONException e) {
//					e.printStackTrace();
				}
				if(vs != null){
					MobclickAgent.onEvent(mContext, pJsArgs[0], vs);
				}else{
					MobclickAgent.onEvent(mContext, pJsArgs[0], pJsArgs[1]);
				}
			}
		}else if(pActionName.equals("eventStart")){
//			if(pJsArgs[1] == null ){
//				MobclickAgent.onPageStart(pJsArgs[0]);
//			}else{
//				MobclickAgent.onPageStart(pJsArgs[0]);
//			}
		}else if(pActionName.equals("eventEnd")){
//			if(pJsArgs[1] == null ){
//				MobclickAgent.onEventValue(arg0, arg1, arg2, arg3).onEventEnd(pJsArgs[0]);
//			}else{
//				MobclickAgent.onEventEnd(mContext, pJsArgs[0], pJsArgs[1]);
//			}
		}else if(pActionName.equals("eventDuration")){
			int duration = Integer.parseInt(pJsArgs[1]);
			HashMap<String, String> vs = null;
			try {
				JSONObject json = new JSONObject(pJsArgs[2]);
				vs = new HashMap<String, String>();
				Iterator ir = json.keys();
				while(ir.hasNext()){
					String k = (String)ir.next();
					vs.put(k, json.optString(k));
				}
			} catch (JSONException e) {
//				e.printStackTrace();
			}
			if(vs == null && !TextUtils.isEmpty(pJsArgs[2])){//可能传入参数不是json
				vs = new HashMap<String, String>(1);
				vs.put(pJsArgs[2], "");
			}
			MobclickAgent.onEventValue(mContext,pJsArgs[0],vs, duration);
		}
		return null;
	}
	
	
}
