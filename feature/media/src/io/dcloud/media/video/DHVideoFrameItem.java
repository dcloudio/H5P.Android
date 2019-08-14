package io.dcloud.media.video;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IFrameView;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.util.ViewRect;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.media.video.ijkplayer.VideoPlayerView;

public class DHVideoFrameItem extends AdaFrameItem implements ISysEventListener {

    private IWebview mIWebview;
    private VideoPlayerView mPlayerView;
    private String mId;
    private long resumeTime = 0;//时间点为了矫正锁屏后生命周期走两次

    private String userId;

    private IWebview mAppendWebview = null;

    public String position = "static";
    private ViewGroup.LayoutParams _vlps;

    private IWebview mContenterView = null;
    private JSONArray rect = null;
    private JSONObject styles = null;

    public void removeFrameItem() {
        if(position.equals("absolute")) {
            mContenterView.obtainFrameView().removeFrameItem(this);
        } else {
            mContenterView.removeFrameItem(this);
        }
    }

    protected DHVideoFrameItem(Context pContext, String id, IWebview pWebview,JSONArray rectJa, JSONObject style,String userId) {
        super(pContext);
        mIWebview = pWebview;
        mContenterView = pWebview;
        mId = id;
        this.userId = userId;
        this.rect = rectJa;
        this.styles = style;
        IApp app = mIWebview.obtainFrameView().obtainApp();
        app.registerSysEventListener(this, ISysEventListener.SysEventType.onPause);
        app.registerSysEventListener(this, ISysEventListener.SysEventType.onResume);
        app.registerSysEventListener(this, ISysEventListener.SysEventType.onStop);
        app.registerSysEventListener(this, ISysEventListener.SysEventType.onKeyUp);
        mPlayerView = new VideoPlayerView(mIWebview.getActivity(), pWebview, styles);
        setMainView(mPlayerView);
        initFrame(rect);
        position = styles.optString("position");
    }

    private void initFrame(JSONArray rect) {
        AdaFrameItem frameView = (AdaFrameItem)mContenterView.obtainFrameView();
        ViewRect webParentViewRect = frameView.obtainFrameOptions();
        float scale = mContenterView.getScale();
        int _w = PdrUtil.convertToScreenInt(JSONUtil.getString(rect,2),webParentViewRect.width,webParentViewRect.width,scale);
        int _h = PdrUtil.convertToScreenInt(JSONUtil.getString(rect,3),webParentViewRect.height,webParentViewRect.height,scale);
        int _l = PdrUtil.convertToScreenInt(JSONUtil.getString(rect,0),webParentViewRect.width,0,scale); /*+ webParentViewRect.left*/
        int _t = PdrUtil.convertToScreenInt(JSONUtil.getString(rect,1),webParentViewRect.height,0,scale); /*+ webParentViewRect.top*/
        updateViewRect((AdaFrameItem)mContenterView.obtainFrameView(), new int[]{_l,_t,_w,_h}, new int[]{webParentViewRect.width,webParentViewRect.height});
        ViewGroup.LayoutParams _lp = LayoutParamsUtil.createLayoutParams(_l, _t,_w, _h);
        _vlps = _lp;
        mPlayerView.setRect(new int[]{_l,_t,_w+_l,_h+_t});
    }

    public String getUserId() {
        return userId;
    }

    public String getmId() {
        return mId;
    }

    @Override
    public void dispose() {
        super.dispose();
        release();
        VideoPlayerMgr.getInstance().rmovePlayer(mId);
    }

    @Override
    public boolean onExecute(SysEventType pEventType, Object pArgs) {
        if(pEventType == ISysEventListener.SysEventType.onPause){
            if (System.currentTimeMillis()-resumeTime > 100) {
                pause();
                resumeTime = 0;
            }
            return true;
        } else if(pEventType == ISysEventListener.SysEventType.onResume){
            if (resumeTime > 0)
                resume();
            resumeTime = System.currentTimeMillis();
            return true;
        } else if(pEventType == ISysEventListener.SysEventType.onStop){
            release();
            return true;
        } else if(pEventType == SysEventType.onKeyUp) {
            Object[] _args = (Object[])pArgs;
            int keyCode = (Integer) _args[0];
            if(keyCode == KeyEvent.KEYCODE_BACK && isFullScreen()) {
                if(mPlayerView != null) {
                    return mPlayerView.onBackPressed();
                }
            }
        }
        return false;
    }

    public void play() {
        if(mPlayerView != null) {
            mPlayerView.play();
        }
    }

    public void pause() {
        if(mPlayerView != null) {
            mPlayerView.pause();
        }
    }

    public void resume() {
        if(mPlayerView != null) {
            mPlayerView.resume();
        }
    }

    public void seek(String position) {
        if(mPlayerView != null) {
            mPlayerView.seek(position);
        }
    }

    public void sendDanmu(JSONObject danmu) {
        if(mPlayerView != null) {
            mPlayerView.sendDanmu(danmu);
        }
    }
    public void sendPlayBackRate(String rate) {
        if(mPlayerView != null) {
            mPlayerView.playbackRate(rate);
        }
    }

    public void playbackRate(String rate) {
        if(mPlayerView != null) {
            mPlayerView.playbackRate(rate);
        }
    }

    public void requestFullScreen(String direction) {
        if(mPlayerView != null) {
            mPlayerView.requestFullScreen(direction);
        }
    }

    public void exitFullScreen() {
        if(mPlayerView != null) {
            mPlayerView.exitFullScreen();
        }
    }

    public void setOptions(JSONObject options) {
        if(mPlayerView != null) {
            styles = JSONUtil.combinJSONObject(styles, options);
            if (mPlayerView.isFullScreen()) {
                //全屏状态下不允许设置与位置相关属性。
                try {
                    JSONObject fullscreenStyle = new JSONObject(styles.toString());
                    fullscreenStyle.remove("top");
                    fullscreenStyle.remove("left");
                    fullscreenStyle.remove("width");
                    fullscreenStyle.remove("height");
                    fullscreenStyle.remove("position");
                    mPlayerView.setOptions(fullscreenStyle);
                } catch (JSONException e) {
                }
                return;
            }
            if(options.has("top") || options.has("left") || options.has("width") || options.has("height") || options.has("position")) {
                try {
                    rect.put(0, JSONUtil.getString(options, StringConst.JSON_KEY_LEFT));
                    rect.put(1, JSONUtil.getString(options, StringConst.JSON_KEY_TOP));
                    rect.put(2, JSONUtil.getString(options, StringConst.JSON_KEY_WIDTH));
                    rect.put(3, JSONUtil.getString(options, StringConst.JSON_KEY_HEIGHT));
                }catch (JSONException e) {
                }
                String mPosition = JSONUtil.getString(options, "position");
                initFrame(rect);
                if(options.has("position")) {
                    if (!mPosition.equals(position)) {
                        if ("absolute".equals(position)) {
                            mContenterView.obtainFrameView().removeFrameItem(this);
                            mContenterView.addFrameItem(this, _vlps);
                        } else if ("static".equals(position)) {
                            mContenterView.removeFrameItem(this);
                            mContenterView.obtainFrameView().addFrameItem(this, _vlps);
                        }
                    } else {
                        obtainMainView().setLayoutParams(_vlps);
                    }
                    position = mPosition;
                } else {
                    obtainMainView().setLayoutParams(_vlps);
                }
            }
            mPlayerView.setOptions(styles);
        }
    }

    public void addEventListener(String event, String callbackId,String webId) {
        if(mPlayerView != null) {
            mPlayerView.addEventListener(event, callbackId,webId);
        }
    }

    public void release() {
        if(mPlayerView != null) {
            mPlayerView.release();
            mPlayerView = null;
        }
    }

    public boolean isFullScreen() {
        if(mPlayerView != null) {
            return mPlayerView.isFullScreen();
        }
        return false;
    }

    public void stop() {
        if(mPlayerView != null) {
            mPlayerView.stop();
        }
    }

    public void close() {
        if(mPlayerView != null) {
            mPlayerView.close();
        }
    }

    public void hidden(){
        if (mPlayerView != null) {
            mPlayerView.setVisibility(View.INVISIBLE);
        }
    }

    public void show(){
        if (mPlayerView != null) {
            mPlayerView.setVisibility(View.VISIBLE);
        }
    }

    public void appendToFrame(IFrameView mFrameView){
        View mainView = obtainMainView();
        if(mainView != null && mainView.getParent() != null) {
            removeFrameItem();
        }
        mContenterView = mFrameView.obtainWebView();
//        if (mainView == null || (mainView!=null && !(mainView instanceof VideoPlayerView))) {
//            mPlayerView = new VideoPlayerView(mContenterView.getActivity(), mContenterView, styles);
//            setMainView(mPlayerView);
//            initFrame(rect);
//        }
        initFrame(rect);
        if (position.equals("static")) {
            mContenterView.addFrameItem(this,_vlps);
        } else if (position.equals("absolute")) {
            mContenterView.obtainFrameView().addFrameItem(this,_vlps);
        } else {
            mContenterView.addFrameItem(this,_vlps);
        }
    }

    @Override
    protected void onResize() {
        if (isRegisterResize)
            return;
        super.onResize();
        initFrame(rect);
        obtainMainView().setLayoutParams(_vlps);
    }
    private boolean isRegisterResize = false;
    public void resize(JSONArray pJsArgs){
        initFrame(pJsArgs);
        obtainMainView().setLayoutParams(_vlps);
        isRegisterResize = true;
    }
}
