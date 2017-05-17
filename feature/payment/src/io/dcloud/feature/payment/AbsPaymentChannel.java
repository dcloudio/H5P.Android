package io.dcloud.feature.payment;

import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;
import android.content.Context;

/**
 * 支付通道抽象类
 *
 * @version 1.0
 * @author yanglei Email:yanglei@dcloud.io
 * @Date 2013-5-30 下午06:02:09 created.
 * 
 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-30 下午06:02:09
 */
public abstract class AbsPaymentChannel implements IReflectAble{
	protected String featureName;
	protected String name;
	//支付模块标示
	protected String id;
	//支付模块描述信息
	protected String description;
	//付通道依赖的服务是否安装
	protected boolean serviceReady;
	//支付成功失败回调functionid
	private String mCallbackId;
	protected Context mContext;
	protected IWebview mWebview;
	public void init(Context context){
		mContext = context;
	}
	public void updateWebview(IWebview pWebview){
		mWebview = pWebview;
	}
	abstract protected void installService();
	/**
	 * 提交订单信息
	 * @param pStatement 订单信息
	 * @param callbackId 成功失败回调时所需的functionid
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-30 下午06:04:35
	 */
	final void request(String pStatement,String callbackId){
		mCallbackId = callbackId;
		request(pStatement);
	}
	/**
	 * 提交订单信息(具体逻辑需要实现类实现)
	 * @param pStatement 订单信息
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-30 下午06:05:18
	 */
	abstract protected void request(String pStatement);
	/**
	 * 获得支付通道属性信息
	 * @return
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-30 下午06:06:04
	 */
	public JSONObject toJSONObject(){
		JSONObject json = new JSONObject();
		try {
			json.put("id", id);
			json.put("description", description);
			json.put("serviceReady", serviceReady);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	public String getFullDescription(){
		return featureName + description;
	}
	protected final IPaymentListener mListener = new IPaymentListener() {
		@Override
		public void onError(int code, String messsage) {
			String error_msg = DOMException.toJSON(code,messsage);
			JSUtil.execCallback(mWebview, mCallbackId, error_msg, JSUtil.ERROR, true, false);
		}

		@Override
		public void onSuccess(PaymentResult result) {
			JSUtil.execCallback(mWebview, mCallbackId, result.toJSONObject(), JSUtil.OK, false);
		}
	
	};
}
