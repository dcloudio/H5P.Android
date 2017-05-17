package io.dcloud.feature.payment;

import io.dcloud.common.DHInterface.IReflectAble;

public interface IPaymentListener extends IReflectAble{
	static final int CODE_NO_INSTALL_MOBILE_SP = 62000,
	CODE_USER_CANCEL = 62001,//	用户取消支付操作
	CODE_DEVICE_NO_SUPPORT = 62002,//	此设备不支持支付
	CODE_DATA_ERROR = 62003,//	数据格式错误
	CODE_ACCOUNT_STATUS_ERROR = 62004,//	支付账号状态错误
	CODE_ORDER_INFO_ERROR = 62005,//	订单信息错误
	CODE_PAY_OPTION_ERROR = 62006,//	支付操作内部错误
	CODE_PAY_SERVER_ERROR = 62007,//	支付服务器错误
	CODE_NETWORK_ERROR = 62008,//	网络问题引起的错误
	CODE_UNKNOWN = 62009,//	其它未定义的错误
	CODE_SUCCESS = 1;
	void onError(int state,String msg);
	void onSuccess(PaymentResult result);
}
