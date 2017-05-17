package io.dcloud.feature.speech;

import io.dcloud.common.DHInterface.IReflectAble;

/**
 * 语音回调接口
 *
 * @version 1.0
 * @author yanglei Email:yanglei@dcloud.io
 * @Date 2013-5-22 下午05:05:23 created.
 * 
 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-22 下午05:05:23
 */
public interface ISpeechListener extends IReflectAble{
	byte ONSTART = 1;
	byte ONEND = 2;
	byte ONAUDIOSTART = 3;
	byte ONAUDIOEND = 4;
	byte ONRECOGNIZESTART = 5;
	byte ONRECOGNIZEEND = 6;
	byte ONERROR = 7;
	byte ONSUCCESS = 8;
	/**
	 * 当语音解析引擎，解析语音状态发生变化时调用
	 * @param state 语音解析状态
	 * @param args 回传参数
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-22 下午05:05:30
	 * @param keepCallback TODO
	 */
	void onStateChange(byte state,Object args, boolean keepCallback);
}
