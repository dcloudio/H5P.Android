package io.dcloud.feature.speech;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

public class SpeechManager {

    private static SpeechManager mgr;
    private AbsSpeechEngine mSpeechEngine = null;
    private HashMap<String,ConcurrentHashMap<String,IWebview>> webCallBackIds;

    public static SpeechManager getInstance(){
        if (mgr == null) {
            synchronized (SpeechManager.class) {
                if (mgr == null) {
                    mgr = new SpeechManager();
                }
            }
        }
        return mgr;
    }

    private SpeechManager() {
        webCallBackIds = new HashMap<String,ConcurrentHashMap<String,IWebview>>();
    }

    public void execute(IWebview pWebViewImpl, String pActionName, String[] pJsArgs, HashMap<String, String> mSpeechMap){
        if("startRecognize".equals(pActionName)){//启动语音识别
            String callbackId = pJsArgs[0];
            JSONObject speechOption = JSONUtil.createJSONObject(pJsArgs[1]);
            JSONObject eventCallbackIds = JSONUtil.createJSONObject(pJsArgs[2]);
            String engine = speechOption.optString(StringConst.JSON_KEY_ENGINE,"ifly").toLowerCase();
            try {
                stopRecognize(false);
                if (!mSpeechMap.containsKey(engine)) {
                    if (mSpeechMap.containsKey("ifly")){
                        engine = "ifly";
                    } else if (mSpeechMap.containsKey("baidu")) {
                        engine = "baidu";
                    } else {
                        JSUtil.execCallback(pWebViewImpl, callbackId, "{code:'-1',message:'not found engine="+engine+"'}", JSUtil.ERROR, true, false);
                        eventListener("error","{code:'-1',message:'not found engine="+engine+"'}",JSUtil.ERROR,false);
                    }
                }
                String engine_value = mSpeechMap.get(engine);
                if(!PdrUtil.isEmpty(engine_value)){//指定engine语音识别引擎存在时，实例化对象
                    mSpeechEngine = (AbsSpeechEngine)Class.forName(engine_value).newInstance();
                    mSpeechEngine.init(pWebViewImpl.getActivity(), pWebViewImpl);
                    mSpeechEngine.startRecognize(callbackId, speechOption, eventCallbackIds);
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (Exception e){
                JSUtil.execCallback(pWebViewImpl, callbackId, "{code:'-1',message:'not found engine="+engine+"'}", JSUtil.ERROR, true, false);
                eventListener("error","{code:'-1',message:'not found engine="+engine+"'}",JSUtil.ERROR,false);
            }
        }else if("stopRecognize".equals(pActionName)){
            stopRecognize(true);
        } else if ("addEventListener".equals(pActionName)) {
            String event = pJsArgs[0];
            String callbackID = pJsArgs[1];
            addEventListener(pWebViewImpl,event,callbackID);
        }
    }

    public void stopRecognize(boolean isExeOnEnd){
        if(mSpeechEngine != null){
            mSpeechEngine.stopRecognize(isExeOnEnd);
            mSpeechEngine = null;
        }
    }
    private void addEventListener(IWebview pwebview,String action ,String callbackId) {
//        if(mSpeechEngine != null) {
            ConcurrentHashMap<String,IWebview> events = webCallBackIds.get(action);
            if (null == events) {
                events = new ConcurrentHashMap<String, IWebview>();
            }
            events.put(callbackId,pwebview);
            webCallBackIds.put(action,events);
            addWindowCloseListener(pwebview);
//        }
    }

    /**
     * 添加指定webview销毁时候的监听
     *
     * @param pWebview
     */
    private void addWindowCloseListener(IWebview pWebview) {
        // 新的webview需要注册页面监听，当webview被关闭的时候需要清除webview相关的记录
        AdaFrameView frameView = (AdaFrameView) pWebview.obtainFrameView();
        frameView.addFrameViewListener(new IEventCallback() {
            @Override
            public Object onCallBack(String pEventType, Object pArgs) {
                if ((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) && pArgs instanceof IWebview) {
                    removeWebviewCallback((IWebview) pArgs);
                    ((AdaFrameView) ((IWebview) pArgs).obtainFrameView()).removeFrameViewListener(this);
                }
                return null;
            }
        });
    }
    public void removeWebviewCallback(IWebview pWebview) {
        if (null != webCallBackIds) {
            for (String event : webCallBackIds.keySet()) {
                for (String callbackId : webCallBackIds.get(event).keySet()) {
                    if (webCallBackIds.get(event).get(callbackId) == pWebview) {
                        webCallBackIds.get(event).remove(callbackId);
                    }
                }
            }
        }
    }

    public void eventListener(String action,String msg, int status, boolean keepCallback){
        if (null != webCallBackIds) {
            if (webCallBackIds.containsKey(action) && webCallBackIds.get(action) != null) {
                for (String callbackId : webCallBackIds.get(action).keySet()) {
                    IWebview webview = webCallBackIds.get(action).get(callbackId);
                    if (((AdaFrameItem) webview).isDisposed()) continue;
                    JSUtil.execCallback(webview, callbackId, msg, status, true, keepCallback);
                }
            }
        }
    }
}
