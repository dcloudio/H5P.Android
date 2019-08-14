package io.dcloud.feature.weex_media;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.dom.CSSConstants;
import com.taobao.weex.dom.WXAttr;
import com.taobao.weex.layout.ContentBoxMeasurement;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.view.WXBaseRefreshLayout;
import com.taobao.weex.utils.WXViewUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.weex.WeexInstanceMgr;


public class VideoComponent extends WXVContainer<VideoPlayerView> implements ISysEventListener {

    private WXAttr attrs;
    private Map<String, Object> params;
    private IApp mApp;
    private AtomicBoolean isLoad = new AtomicBoolean(false);

//    public VideoComponent(WXSDKInstance instance, WXVContainer parent, int type, BasicComponentData basicComponentData) {
//        super(instance, parent, type, basicComponentData);
//    }

    public VideoComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
        attrs = basicComponentData.getAttrs();
        setContentBoxMeasurement(new ContentBoxMeasurement() {
            @Override
            public void measureInternal(float width, float height, int widthMeasureMode, int heightMeasureMode) {
                if (CSSConstants.isUndefined(width))
                    mMeasureWidth = WXViewUtils.getRealPxByWidth(300, getInstance().getInstanceViewPortWidth());
                if (CSSConstants.isUndefined(height))
                    mMeasureHeight = WXViewUtils.getRealPxByWidth(225, getInstance().getInstanceViewPortWidth());
            }

            @Override
            public void layoutBefore() {

            }

            @Override
            public void layoutAfter(float computedWidth, float computedHeight) {

            }
        });
    }

    @Override
    protected VideoPlayerView initComponentHostView(Context context) {
        IWebview webview = WeexInstanceMgr.self().findWebview(getInstance());
        if (webview != null) {
            mApp = webview.obtainApp();
            mApp.registerSysEventListener(this, ISysEventListener.SysEventType.onKeyUp);
        }
        return new VideoPlayerView(getContext(), this);
    }

    @Override
    public ViewGroup getRealView() {
        return getHostView().getPlayerView();
    }

    @Override
    public void addChild(WXComponent child, int index) {
        if (child instanceof VideoInnerViewComponent) {
            super.addChild(child, index);
        }
    }

    @Override
    public void addSubView(final View child, final int index) {
        if (child == null || getRealView() == null) {
            return;
        }

        if (child instanceof WXBaseRefreshLayout) {
            return;
        }
        // 判断
        int count = getRealView().getChildCount();
        int index1 = index >= count ? -1 : index;
        if (getRealView().indexOfChild(child) == -1)
            if (index1 == -1) {
                getRealView().addView(child);
            } else {
                getRealView().addView(child, index1);
            }
        child.bringToFront();
    }

    @WXComponentProp(name = "src")
    public void setSrc(String src) {
        if (PdrUtil.isEmpty(src)) return;
        getHostView().setSrc(src);
    }

    @WXComponentProp(name = "autoplay")
    public void setAutoPlay(boolean autoPlay) {
        if (PdrUtil.isEmpty(autoPlay)) return;
        getHostView().setAutoplay(autoPlay);
    }

    @WXComponentProp(name = "initialTime")
    public void setInitTime(float time) {
        getHostView().setInitialTime(time);
    }

    @WXComponentProp(name = "duration")
    public void setDuration(float duration) {
        getHostView().setDuration(duration);
    }

    @WXComponentProp(name = "danmuList")
    public void setDanmuList(JSONArray list) {
        getHostView().setDanmuList(list);
    }

    @WXComponentProp(name = "loop")
    public void setLoop(boolean isloop) {
        getHostView().setLoop(isloop);
    }

    @WXComponentProp(name = "muted")
    public void setMute(boolean isMute) {
        getHostView().setMuted(isMute);
    }

    @WXComponentProp(name = "direction")
    public void setDirection(int direction) {
        getHostView().setDirection(direction);
    }

    @WXComponentProp(name = "objectFit")
    public void setFit(String type) {
        if (PdrUtil.isEmpty(type)) return;
        getHostView().setObjectFit(type);
    }

    @WXComponentProp(name = "showMuteBtn")
    public void isShowMuteBtn(boolean isshow) {
        getHostView().setMuteBtn(isshow);
    }

    @WXComponentProp(name = "playBtnPosition")
    public void setPlayBtnPosition(String position) {
        if (PdrUtil.isEmpty(position)) return;
        getHostView().setPlayBtnPosition(position);
    }

    @WXComponentProp(name = "title")
    public void setTitle(String title) {
        if (PdrUtil.isEmpty(title)) return;
        getHostView().setTitle(title);
    }

    @Override
    public void updateProperties(Map<String, Object> props) {
        if (props.size() > 0) {
            params = combinMap(params, props);
            // 合并map
            getHostView().setProgress(!params.containsKey("showProgress") || Boolean.parseBoolean(params.get("showProgress").toString()));
            getHostView().setShowFullScreenBtn(!params.containsKey("showFullscreenBtn") || Boolean.parseBoolean(params.get("showFullscreenBtn").toString()));
            getHostView().setPlayBtnVisibility(!params.containsKey("showPlayBtn") || Boolean.parseBoolean(params.get("showPlayBtn").toString()));
            getHostView().setEnableProgressGesture(!params.containsKey("enableProgressGesture") || Boolean.parseBoolean(params.get("enableProgressGesture").toString()));
            if (props.containsKey("src")) {
                getHostView().setSrc((String) props.get("src"));
            }
            getHostView().setShowCenterPlayBtn(!params.containsKey("showCenterPlayBtn") || Boolean.parseBoolean(params.get("showCenterPlayBtn").toString()));
            getHostView().setPageGesture(!params.containsKey("vslideGestureInFullscreen") || Boolean.parseBoolean(params.get("vslideGestureInFullscreen").toString()));
            getHostView().setControls(!params.containsKey("controls") || Boolean.parseBoolean(params.get("controls").toString()));
        }
        super.updateProperties(props);
        if (props.size() > 0 && props.containsKey("src")) {
            getHostView().onLayoutFinished();
        }
    }

    private Map<String, Object> combinMap(Map<String, Object> main, Map<String, Object> second) {
        if (main == null && second == null) return new HashMap<>();
        if (main == null) return second;
        if (second == null) return main;
        main.putAll(second);
        return main;
    }

    @WXComponentProp(name = "poster")
    public void setPoster(String poster) {
        getHostView().setPoster(poster);
    }

    @JSMethod
    public void requestFullScreen(JSONObject param) {
        int direction = param.getInteger("direction");
        getHostView().requestFullScreen(direction);
    }

    @JSMethod
    public void play() {
        getHostView().play();
    }

    @JSMethod
    public void pause() {
        getHostView().pause();
    }

    @JSMethod
    public void stop() {
        getHostView().stop();
    }

    @JSMethod
    public void seek(int position) {
        if (PdrUtil.isEmpty(position)) return;
        getHostView().seek(position * 1000);
    }

    @JSMethod
    public void sendDanmu(JSONObject danmu) {
        if (danmu != null) {
            getHostView().sendDanmu(danmu);
        }
    }

    @JSMethod
    public void playbackRate(float rate) {
        if (PdrUtil.isEmpty(rate)) return;
        getHostView().sendPlayBackRate(String.valueOf(rate));
    }

    @JSMethod
    public void exitFullScreen() {
        getHostView().exitFullScreen();
    }

    @Override
    protected void onHostViewInitialized(VideoPlayerView host) {
        super.onHostViewInitialized(host);
        if (attrs != null && attrs.size() > 0) {
            /* 这俩属性只允许创建的时候设置，所以放在这里 */
            getHostView().setEnableDanmu(attrs.containsKey("enableDanmu") && Boolean.parseBoolean(attrs.get("enableDanmu").toString()));
            getHostView().setDanmuBtn(attrs.containsKey("danmuBtn") && Boolean.parseBoolean(attrs.get("danmuBtn").toString()));
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        getHostView().resume();
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        getHostView().pause();
    }

    @Override
    public void destroy() {
        super.destroy();
        getHostView().destory();
        if (mApp != null) {
            mApp.unregisterSysEventListener(this, SysEventType.onKeyUp);
        }
    }

    @Override
    public boolean onExecute(SysEventType pEventType, Object pArgs) {
        if (pEventType == SysEventType.onKeyUp) {
            Object[] _args = (Object[]) pArgs;
            int keyCode = (Integer) _args[0];
            if (keyCode == KeyEvent.KEYCODE_BACK && getHostView().isFullScreen()) {
                if (getHostView() != null) {
                    return getHostView().onBackPress();
                }
            }
        }
        return false;
    }
}
