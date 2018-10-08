package com.baidu.aip.core.recog.listener;

import android.os.Handler;
import android.os.Message;

import com.baidu.speech.asr.SpeechConstant;

import com.baidu.aip.core.recog.RecogResult;

/**
 * Created by fujiayi on 2017/6/16.
 */

public class MessageStatusRecogListener extends StatusRecogListener {
    private Handler handler;

    private long speechEndTime = 0;

    private boolean needTime = true;

    private static final String TAG = "MesStatusRecogListener";

    public MessageStatusRecogListener(Handler handler) {
        this.handler = handler;
    }


    @Override
    public void onAsrReady() {
        super.onAsrReady();
        speechEndTime = 0;
//        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_WAKEUP_READY, "引擎就绪，可以开始说话。");
    }

    @Override
    public void onAsrBegin() {
        super.onAsrBegin();
//        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN, "检测到用户说话");
    }

    @Override
    public void onAsrEnd() {
        super.onAsrEnd();
        speechEndTime = System.currentTimeMillis();
//        sendMessage("【asr.end事件】检测到用户说话结束");
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        super.onAsrPartialResult(results, recogResult);
        sendMessage(results[0],status,true);
    }

    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        super.onAsrFinalResult(results, recogResult);
        speechEndTime = 0;
        sendMessage(results, status, true);
    }

    @Override
    public void onAsrFinishError(int errorCode, int subErrorCode, String descMessage,
                                 RecogResult recogResult) {
        super.onAsrFinishError(errorCode, subErrorCode, descMessage, recogResult);
        speechEndTime = 0;
        String message = "{errorCode:'"+errorCode+"',subErrorCode:'"+subErrorCode+"',descMessage:'"+descMessage+"'}";
        sendMessage(message, status, true);
        speechEndTime = 0;
    }

    @Override
    public void onAsrOnlineNluResult(String nluResult) {
        super.onAsrOnlineNluResult(nluResult);
        if (!nluResult.isEmpty()) {
            sendMessage(new String[]{nluResult}, status, true);
        }
    }

    @Override
    public void onAsrFinish(RecogResult recogResult) {
        super.onAsrFinish(recogResult);
        String[] result = recogResult.getResultsRecognition();
        if (null != result && result.length > 0)
            sendMessage(recogResult.getResultsRecognition(), status, true);
    }

    /**
     * 长语音识别结束
     */
    @Override
    public void onAsrLongFinish() {
        super.onAsrLongFinish();
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_LONG_SPEECH, "长语音识别结束。");
    }


    /**
     * 使用离线命令词时，有该回调说明离线语法资源加载成功
     */
//    @Override
//    public void onOfflineLoaded() {
//        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_LOADED, "离线资源加载成功。没有此回调可能离线语法功能不能使用。");
//    }

    /**
     * 使用离线命令词时，有该回调说明离线语法资源加载成功
     */
//    @Override
//    public void onOfflineUnLoaded() {
//        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_UNLOADED, "离线资源卸载成功。");
//    }

    @Override
    public void onAsrExit() {
        super.onAsrExit();
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_EXIT, "识别引擎结束并空闲中");
    }

    @Override
    public void onAsrVolume(int volumePercent, int volume) {
        super.onAsrVolume(volumePercent, volume);
        sendMessage(volume,status,true);
    }

    private void sendStatusMessage(String eventName, String message) {
        message = "[" + eventName + "]" + message;
        sendMessage(message, status);
    }

    private void sendMessage(String message) {
        sendMessage(message, WHAT_MESSAGE_STATUS);
    }

    private void sendMessage(String message, int what) {
        sendMessage(message, what, false);
    }


    private void sendMessage(Object message, int what, boolean highlight) {


        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = status;
        if (highlight) {
            msg.arg2 = 1;
        }
        msg.obj = message;
        handler.sendMessage(msg);
    }
}
