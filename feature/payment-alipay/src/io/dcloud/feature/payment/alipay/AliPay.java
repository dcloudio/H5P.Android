package io.dcloud.feature.payment.alipay;

import org.json.JSONObject;

import io.dcloud.common.constant.DOMException;
import io.dcloud.feature.payment.AbsPaymentChannel;
import io.dcloud.feature.payment.IPaymentListener;
import io.dcloud.feature.payment.PaymentResult;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.alipay.sdk.app.PayTask;

public class AliPay extends AbsPaymentChannel {
	
	private static final int SDK_PAY_FLAG = 1;
	boolean DEBUG = false;
	static String TAG = "AliPay";
	@Override
	protected void request(final String payInfo) {
		Runnable payRunnable = new Runnable() {
			@Override
			public void run() {
				// 构造PayTask 对象
				PayTask payTask = new PayTask(mWebview.getActivity());
				String result = payTask.pay(payInfo,true);
//				String result = "resultStatus={9000};memo={};result={service=\"mobile.securitypay.pay\"&partner=\"2088801273866834\"&_input_charset=\"UTF-8\"&out_trade_no=\"20150804183835\"&subject=\"DCloud项目捐赠\"&payment_type=\"1\"&seller_id=\"payservice@dcloud.io\"&total_fee=\"1\"&body=\"DCloud致力于打造HTML5最好的移动开发工具，包括终端的Runtime、云端的服务和IDE，同时提供各项配套的开发者服务。\"&it_b_pay=\"1d\"&notify_url=\"http%3A%2F%2Fdemo.dcloud.net.cn%2Fpayment%2Falipay%2Fnotify.php\"&show_url=\"http%3A%2F%2Fwww.dcloud.io%2Fhelloh5%2F\"&success=\"true\"&sign_type=\"RSA\"&sign=\"M0qBuDdC7dNH/N3seQ+26HoEXIyP9/u9kwRmK3fhUaf1uROYJJOHDifBDxOlNDBYw8f0wlx1otf9GT8R0BmTuFDhAIzjVzn6ErqDw7jC05oTsAgEtKOaENsPxAL+SwBdbsJQSj08JsxpfepjWQPkWrpTTFLTe5hu+8yKE1xKDH8=\"}";
				Message msg = new Message();
				msg.what = SDK_PAY_FLAG;
				msg.obj = result;
				mHandler.sendMessage(msg);
			}
		};
		// 必须异步调用
		Thread payThread = new Thread(payRunnable);
		payThread.start();
	}

	
	@Override
	public void init(Context context) {
		super.init(context);
		description = "支付宝";
		serviceReady = true;
	}

	// 这里接收支付结果，支付宝手机端同步通知
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			try {
				String strRet = (String) msg.obj;
//				Log.e(TAG, strRet);	// strRet范例：resultStatus={9000};memo={支付成功};result={partner="2088201564809153"&seller="2088201564809153"&out_trade_no="050917083121576"&subject="123456"&body="2010新款NIKE 耐克902第三代板鞋 耐克男女鞋 386201 白红"&total_fee="0.01"&notify_url="http://notify.java.jpxx.org/index.jsp"&success="true"&sign_type="RSA"&sign="d9pdkfy75G997NiPS1yZoYNCmtRbdOP0usZIMmKCCMVqbSG1P44ohvqMYRztrB6ErgEecIiPj9UldV5nSy9CrBVjV54rBGoT6VSUF/ufjJeCSuL510JwaRpHtRPeURS1LXnSrbwtdkDOktXubQKnIMg2W0PreT1mRXDSaeEECzc="}
				switch (msg.what) {
				case SDK_PAY_FLAG: {
					// 处理交易结果
					try {
							// 获取交易状态码，具体状态代码请参看文档
							String tradeStatus = "resultStatus={";
							int imemoStart = strRet.indexOf("resultStatus=");
							imemoStart += tradeStatus.length();
							int imemoEnd = strRet.indexOf("};memo=");
							tradeStatus = strRet.substring(imemoStart, imemoEnd);
							{
								JSONObject rawDataJson = new JSONObject();
								rawDataJson.put("resultStatus", tradeStatus);
								if(tradeStatus.equals("9000")){//判断交易状态码，只有9000表示交易成功
									String memo = "memo={";
									imemoStart = strRet.indexOf("memo={");
									imemoStart += memo.length();
									imemoEnd = strRet.indexOf("};result");
									memo = strRet.substring(imemoStart, imemoEnd);
									rawDataJson.put("memo", memo);
									
									String result = "result={";
									imemoStart = strRet.indexOf("result={");
									imemoStart += result.length();
									imemoEnd = strRet.length() - 1;
									result = strRet.substring(imemoStart, imemoEnd);
									
									rawDataJson.put("result", result);
									String url = null,signature = null,tradeno=null;
									String[] params = result.split("\\&");
									if(params != null && params.length > 0){
										int len = params.length;
										for(int i = 0; i < len; i++){
											String param = params[i];
											if(param.indexOf("=") > 0){
												if(param.indexOf("sign=\"") >= 0){//signature需要特殊处理，因为字符串中含有‘=’字符
													signature = param.substring("sign=\"".length(), param.length() - 1);//除去值前后的‘"’字符
												}else{
													String[] params0 = param.split("\\=");
													if(params0.length >= 2){
														if("notify_url".equals(params0[0])){
															url = params0[1].substring(1, params0[1].length() - 1);//除去值前后的‘"’字符
														}else if("out_trade_no".equals(params0[0])){
															tradeno = params0[1].substring(1, params0[1].length() - 1);//除去值前后的‘"’字符
														}
													}
												}
											}
										}
									}
									PaymentResult pr = new PaymentResult(AliPay.this);
									pr.description = memo;
									pr.url = url;
									pr.tradeno = tradeno;
									pr.signature = signature;
									pr.rawDataJson = rawDataJson.toString();
									mListener.onSuccess(pr);
							}else{
								int state = IPaymentListener.CODE_UNKNOWN;
								String error_msg = null;
								if(tradeStatus.equals("4000")){//系统异常
									state = IPaymentListener.CODE_UNKNOWN;
									error_msg = "未知错误";
								}else if(tradeStatus.equals("4001")){//数据格式不正确
									state = IPaymentListener.CODE_DATA_ERROR;
									error_msg = "数据格式不正确";
								}else if(tradeStatus.equals("4003")){//该用户绑定的支付宝账户被冻结或不允许支付
									state = IPaymentListener.CODE_ACCOUNT_STATUS_ERROR;
									error_msg = "该用户绑定的支付宝账户被冻结或不允许支付";
								}else if(tradeStatus.equals("4004")){//该用户已解除绑定
									state = IPaymentListener.CODE_ACCOUNT_STATUS_ERROR;
									error_msg = "该用户已解除绑定";
								}else if(tradeStatus.equals("4005")){//绑定失败或没有绑定
									state = IPaymentListener.CODE_ACCOUNT_STATUS_ERROR;
									error_msg = "绑定失败或没有绑定";
								}else if(tradeStatus.equals("4006")){//订单支付失败
									state = IPaymentListener.CODE_PAY_OPTION_ERROR;
									error_msg = "订单支付失败";
								}else if(tradeStatus.equals("4010")){//重新绑定账户。
									state = IPaymentListener.CODE_ACCOUNT_STATUS_ERROR;
									error_msg = "重新绑定账户";
								}else if(tradeStatus.equals("6000")){//支付服务正在进行升级操作。
									state = IPaymentListener.CODE_PAY_SERVER_ERROR;
									error_msg = "支付服务正在进行升级操作";
								}else if(tradeStatus.equals("6001")){//用户中途取消支付操作。
									state = IPaymentListener.CODE_USER_CANCEL;
									error_msg = "用户中途取消支付操作";
								}else if(tradeStatus.equals("6002")){//网络连接异常。
									state = IPaymentListener.CODE_NETWORK_ERROR;
									error_msg = "网络连接异常";
								}
								error_msg = DOMException.toString(state, getFullDescription(), error_msg, null);
								mListener.onError(DOMException.CODE_BUSINESS_INTERNAL_ERROR, error_msg);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						mListener.onError(DOMException.CODE_BUSINESS_INTERNAL_ERROR,DOMException.toString(IPaymentListener.CODE_UNKNOWN, getFullDescription(), e.getMessage(), null));
					}
				}
					break;
				}

				super.handleMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	@Override
	protected void installService() {
	}
	
}
