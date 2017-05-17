package io.dcloud.feature.payment;

import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IReflectAble;

public final class PaymentResult implements IReflectAble{
	AbsPaymentChannel channel;
	public String url;
	public String signature;
	public String tradeno;
	public String description;
	public String rawDataJson;
	public PaymentResult(AbsPaymentChannel pChannel){
		channel = pChannel;
	}
	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		try {
			json.put("channel", channel.toJSONObject());
			json.put("description", description);
			json.put("url", url);
			json.put("signature", signature);
			json.put("tradeno", tradeno);
			json.put("rawdata", new JSONObject(rawDataJson).toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
}
