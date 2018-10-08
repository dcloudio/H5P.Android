package io.dcloud.media;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IWaiter;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.media.video.VideoPlayerMgr;

public class MediaFeatureImpl extends StandardFeature implements IWaiter {

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        super.init(pFeatureMgr, pFeatureName);
        VideoPlayerMgr.getInstance().initFeature(pFeatureMgr);
    }

    public void VideoPlayer(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        JSONArray rectJa = JSONUtil.getJSONArray(args, 1);
        JSONObject style = JSONUtil.getJSONObject(args, 2);
        boolean isCreate = false;
        if (rectJa == null || rectJa.length() <= 0){
            isCreate = true;
            rectJa = new JSONArray();
            try {
                rectJa.put(0,JSONUtil.getString(style,"left"));
                rectJa.put(1,JSONUtil.getString(style,"top"));
                rectJa.put(2,JSONUtil.getString(style,"width"));
                rectJa.put(3,JSONUtil.getString(style,"height"));
            } catch (JSONException e) {
            }
        }
        String userId = JSONUtil.getString(args,3);
        VideoPlayerMgr.getInstance().createVideoPlayer(webview, id, rectJa, style,userId,isCreate);
    }

    public String getVideoPlayerById(IWebview webview, JSONArray args){
        String id = JSONUtil.getString(args, 0);
        String js = JSUtil.wrapJsVar(VideoPlayerMgr.getInstance().findVideoPlayer(id));
        return js;
    }

    public void resize(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        JSONArray rectJa = JSONUtil.getJSONArray(args, 1);
        VideoPlayerMgr.getInstance().resize(webview, id, rectJa);
    }

    public void VideoPlayer_play(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().play(id);
    }

    public void VideoPlayer_pause(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().pause(id);
    }

    public void VideoPlayer_stop(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().stop(id);
    }

    public void VideoPlayer_close(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().close(webview, id);
    }

    public void VideoPlayer_sendDanmu(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().sendDanmu(id,JSONUtil.getJSONObject(args, 1));
    }

    public void VideoPlayer_seek(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        String position = JSONUtil.getString(args, 1);
        VideoPlayerMgr.getInstance().seekTo(id, position);
    }

    public void VideoPlayer_playbackRate(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        String rate = JSONUtil.getString(args,1);
        VideoPlayerMgr.getInstance().setPlayBackRate(id,rate);
    }

    public void VideoPlayer_requestFullScreen(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        String direction = JSONUtil.getString(args, 1);
        if (PdrUtil.isEmpty(direction))
            direction = "-90";
        VideoPlayerMgr.getInstance().requestFullScreen(id, direction);
    }

    public void VideoPlayer_exitFullScreen(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().exitFullScreen(id);
    }
    public void VideoPlayer_show(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().show(id);
    }
    public void VideoPlayer_hide(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        VideoPlayerMgr.getInstance().hidden(id);
    }

    public void VideoPlayer_setOptions(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        JSONObject options = JSONUtil.getJSONObject(args, 1);
        VideoPlayerMgr.getInstance().setOptions(id, options);
    }

    public void VideoPlayer_addEventListener(IWebview webview, JSONArray args) {
        String id = JSONUtil.getString(args, 0);
        String evnet = JSONUtil.getString(args, 1);
        String callbackId = JSONUtil.getString(args, 2);
        String webId = JSONUtil.getString(args,3);
        VideoPlayerMgr.getInstance().addEventListener(webview, id, evnet, callbackId, webId);
    }

    @Override
    public void dispose(String pAppid) {
        super.dispose(pAppid);
        VideoPlayerMgr.getInstance().recovery();
    }

    @Override
    public Object doForFeature(String actionType, Object args) {
        Object _ret = null;
        if("appendToFrameView".equals(actionType)) {
            Object[] pArgs = (Object[])args;
            String videoId = (String) pArgs[1];
            AdaFrameView videoFrame = (AdaFrameView) pArgs[0];
            _ret = VideoPlayerMgr.getInstance().appendVideoPlayer(videoId,videoFrame);
        }
        return _ret;
    }
}
