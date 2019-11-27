package io.dcloud.feature.weex_livepusher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.ui.component.WXComponent;
import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.util.PdrUtil;

public class TCPusherView extends LinearLayout implements ITXLivePushListener, TXLivePusher.OnBGMNotify {

    private TXLivePusher mLivePusher;
    private TXLivePushConfig mLivePushConfig;
    private TXCloudVideoView pusherView;
    private WXComponent component;
    private WXSDKInstance mInstance;

    private boolean cameraType = true; // front true back false
    private boolean isAutoFocus;
    private int videoQulity = TXLiveConstants.VIDEO_QUALITY_REALTIEM_VIDEOCHAT;
    private int videoResolution;
    private int beautyLevel = 0;
    private int whiteLevel = 0;
    private String mSrc;
    private String BGMPath;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                init();
            }
        }
    };

    public TCPusherView(final Context context, WXComponent component, boolean isFront) {
        super(context);
        this.component = component;
        this.mInstance = component.getInstance();
        mLivePushConfig = new TXLivePushConfig();
        mLivePushConfig.setVideoEncodeGop(5);// 设置视频编码GOP. 默认是3
        mLivePushConfig.enableNearestIP(false);
        mLivePushConfig.setFrontCamera(isFront);
        this.cameraType = isFront;
        if (!isFront) mLivePushConfig.setTouchFocus(false);
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO); // pause时关闭视频，不关闭音频，采用pause图片
//        ThreadPool.self().addThreadTask(new Runnable() {
//            @Override
//            public void run() {
        mLivePusher = new TXLivePusher(TCPusherView.this.component.getContext());
        init();
//                mHandler.sendEmptyMessage(1);
//            }
//        });
        // 示例中有电话监听？？？？？？
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (PdrUtil.isEmpty(component.getStyles().getBackgroundColor()))
            setBackgroundColor(Color.BLACK);
    }


    public void init() {
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.setPushListener(TCPusherView.this);
        mLivePusher.setBGMNofify(TCPusherView.this);
        mLivePusher.setVideoQuality(videoQulity, false, false);
        // 屏幕常亮
        ((Activity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.
                FLAG_KEEP_SCREEN_ON);
        pusherView = new TXCloudVideoView(getContext());
        addView(pusherView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        PermissionUtil.requestSystemPermissions((Activity) mInstance.getContext(), new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 20190419, new PermissionUtil.Request() {
            @Override
            public void onGranted(String streamPerName) {
                permissions.add(streamPerName);
                if (permissions.size() == 2) {
                    mLivePusher.startCameraPreview(pusherView);
                }
            }

            @Override
            public void onDenied(String streamPerName) {
                Map<String, Object> values = new HashMap<>();
                if (streamPerName.equals(PermissionUtil.PMS_CAMERA)) {
                    values.put("errCode", 10001);
                    values.put("errMsg","用户禁止使用摄像头");
                } else if (streamPerName.equals(PermissionUtil.PMS_RECORD)) {
                    values.put("errCode", 10002);
                    values.put("errMsg","用户禁止使用录音");
                }
                HashMap<String,Object> detail = new HashMap<>(1);
                detail.put("detail",values);
                fireEvent("error", detail);
            }
        });
    }

    public void switchCamera(String type) {
        this.cameraType = type.equals("front");
        if (mLivePusher.isPushing()) {
            mLivePusher.switchCamera();
        }
        mLivePushConfig.setFrontCamera(cameraType);
        mLivePusher.setConfig(mLivePushConfig);
    }

    public void sCamera(JSCallback callback) {
        Map<String, Object> data = new HashMap<>();

        /*if (mLivePusher.isPushing())*/ {
            this.cameraType = !this.cameraType;
            mLivePusher.switchCamera();
            data.put("type", "success");
//            if (cameraType.equals("front")) {
//
//            } else if(cameraType.equals("back")) {
//
//            }
        } /*else {
            data.put("type", "fail");
        }*/
        if (callback != null)
            callback.invoke(data);
    }

    public void autoFocus(boolean isAuto) {
        // 前置的时候不允许自动对焦
        if (cameraType) return;
        isAutoFocus = isAuto;
        mLivePushConfig.setTouchFocus(isAuto);
        if (mLivePusher.isPushing()) {
            mLivePusher.stopCameraPreview(false);
            mLivePusher.startCameraPreview(pusherView);
        }
    }

    public void setMute(boolean mote) {
        mLivePusher.setMute(mote);
    }

    public void preview(JSCallback callback) {
        mLivePushConfig.setFrontCamera(cameraType);
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.startCameraPreview(pusherView);
        Map<String, Object> data = new HashMap<>();
        data.put("type", "success");
        if (callback != null)
            callback.invoke(data);
    }

    public void stopPreview(JSCallback callback) {
        mLivePusher.stopCameraPreview(false);
        Map<String, Object> data = new HashMap<>();
        data.put("type", "success");
        if (callback != null)
            callback.invoke(data);
    }
    public void setBGMute(boolean ismute) {
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
    }

    public void setMode(String mode) {
        switch (mode) {
            case "SD":
                videoQulity = TXLiveConstants.VIDEO_QUALITY_STANDARD_DEFINITION;
                videoResolution = TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640;
                //标清默认开启了码率自适应，需要关闭码率自适应
                mLivePushConfig.setAutoAdjustBitrate(false);
                mLivePushConfig.setVideoBitrate(700);
                mLivePusher.setConfig(mLivePushConfig);
                break;
            case "HD":
                videoQulity = TXLiveConstants.VIDEO_QUALITY_HIGH_DEFINITION;
                videoResolution = TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960;
                break;
            case "FHD":
                videoQulity = TXLiveConstants.VIDEO_QUALITY_SUPER_DEFINITION;
                videoResolution = TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280;
                /*超清模式下是否开启硬件加速，腾讯官方提供硬件加速*/
                break;
            default:
            case "RTC":
                videoQulity = TXLiveConstants.VIDEO_QUALITY_REALTIEM_VIDEOCHAT;
                videoResolution = TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640;
                break;
        }
        mLivePusher.setVideoQuality(videoQulity, false, false);
        mLivePushConfig.setVideoResolution(videoResolution);
    }

    public void setOritation(String oritation) {
        if (oritation.equals("vertical")) {
            mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
            mLivePusher.setRenderRotation(TXLiveConstants.RENDER_ROTATION_PORTRAIT);
        } else if (oritation.equals("horizontal")) {
            mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_LEFT);
            mLivePusher.setRenderRotation(TXLiveConstants.RENDER_ROTATION_LANDSCAPE);
        }
    }

    public void setBeauty(int level) {
        if (level < 0) {
            level = 0;
        } else if (level > 9) {
            level = 9;
        }
        this.beautyLevel = level;
        mLivePushConfig.setBeautyFilter(beautyLevel, whiteLevel, 0);
        mLivePusher.setBeautyFilter(0, beautyLevel, whiteLevel, 0);
    }

    public void setWhite(int level) {
        if (level < 0) {
            level = 0;
        } else if (level > 9) {
            level = 9;
        }
        this.whiteLevel = level;
        mLivePushConfig.setBeautyFilter(beautyLevel, whiteLevel, 0);
        mLivePusher.setBeautyFilter(0, beautyLevel, whiteLevel, 0);
    }

    public void setMinBitrate(int bitrate) {
        mLivePushConfig.setMinVideoBitrate(bitrate);
//        mLivePusher.setConfig(mLivePushConfig);
    }

    public void setMaxBitrate(int bitrate) {
        mLivePushConfig.setMaxVideoBitrate(bitrate);
//        mLivePusher.setConfig(mLivePushConfig);
    }

    /**
     * 设置等待图片，当进入后台是显示
     *
     * @param imagePath
     */
    public void setWaintImage(String imagePath) {
        // 路径转换
        String realPath = component.getInstance().rewriteUri(Uri.parse(imagePath), URIAdapter.IMAGE).getPath();
        Bitmap bitmap = decodeResource(realPath);
        if (bitmap != null) {
            mLivePushConfig.setPauseImg(bitmap);
        }
    }

    private Bitmap decodeResource(String id) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        return BitmapFactory.decodeFile(id, opts);
    }

    public void enableCamera(boolean isEnable) {
        mLivePushConfig.enablePureAudioPush(!isEnable);
//        mLivePusher.setConfig(mLivePushConfig);
    }

    /**
     * 双手缩放
     *
     * @param isZoom
     */
    public void setZoom(boolean isZoom) {
        mLivePushConfig.setEnableZoom(isZoom);
    }

    public void start(JSCallback callback) {
        Map<String, Object> data = new HashMap<>();
        if (PdrUtil.isEmpty(mSrc)) {
            data.put("type", "fail");
            if (callback != null) callback.invoke(data);
            return;
        }
        mLivePushConfig.setFrontCamera(cameraType);
        mLivePushConfig.setBeautyFilter(beautyLevel, whiteLevel, 0);
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.startCameraPreview(pusherView);
        if (mLivePusher.startPusher(mSrc) == 0) {
            data.put("type", "success");
        } else {
            data.put("type", "fail");
        }
        if (callback != null)
            callback.invoke(data);
//        mLivePusher.switchCamera();
    }

    public void pause(JSCallback callback) {
        pusherView.onPause();
        mLivePusher.pausePusher();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "success");
        if (callback != null)
            callback.invoke(data);
    }

    public void resume(JSCallback callback) {
        pusherView.onResume();
        mLivePusher.resumePusher();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "success");
        if (callback != null)
            callback.invoke(data);
    }

    public void setSrc(String src) {
        if (PdrUtil.isEmpty(mSrc) && !PdrUtil.isEmpty(src)) {
//            changeSrc(src);
        } else if (!mSrc.equals(src)) {
//            changeSrc(src);
        } else {
            return;
        }
        mSrc = src;
    }

    private void changeSrc(String src) {
        mLivePushConfig.setCustomModeType(0);
        mLivePushConfig.setPauseImg(300, 5); // 设置推流暂停时,后台播放暂停图片的方式.
        mLivePusher.startPusher(src);
    }

    public void stopPusher(JSCallback callback) {
        // 清除常亮操作
        ((Activity) getContext()).getWindow().clearFlags(WindowManager.LayoutParams.
                FLAG_KEEP_SCREEN_ON);
        mLivePusher.stopBGM();
        mLivePusher.stopCameraPreview(false);
        mLivePusher.stopScreenCapture();
        mLivePusher.stopPusher();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "success");
        if (callback != null)
            callback.invoke(data);
    }

    public void destory(){
        pusherView.stop(true);
        mLivePusher.setPushListener(null);
        mLivePusher.setBGMNofify(null);
    }

    public void playBGM(String url, JSCallback success) {
        // 地址转换
        Map<String, Object> data = new HashMap<>();
        if (PdrUtil.isEmpty(url)) {
            data.put("type", "fail");
            if (success != null) success.invoke(data);
            return;
        }
        if (PdrUtil.isNetPath(url)) {
            this.BGMPath = url;
        } else
            this.BGMPath = mInstance.rewriteUri(Uri.parse(url), URIAdapter.IMAGE).getPath();
        if (mLivePusher.playBGM(BGMPath)) {
            // 成功回调
            data.put("type", "success");
            if (success != null) success.invoke(data);
        } else {
            // 回调失败
            data.put("type", "fail");
            if (success != null) success.invoke(data);
        }
    }

    public void setBGNVolume(int volume, JSCallback success) {
        if (mLivePusher.setBGMVolume(volume)) {
            // 成功回调
            Map<String, Object> data = new HashMap<>();
            data.put("type", "success");
            if (success != null) success.invoke(data);
        } else {
            // 回调失败
            Map<String, Object> data = new HashMap<>();
            data.put("type", "fail");
            if (success != null) success.invoke(data);
        }
    }

    public void pauseBGM(JSCallback success) {
        if (mLivePusher.pauseBGM()) {
            // 成功回调
            Map<String, Object> data = new HashMap<>();
            data.put("type", "success");
            if (success != null) success.invoke(data);
        } else {
            // 回调失败
            Map<String, Object> data = new HashMap<>();
            data.put("type", "fail");
            if (success != null) success.invoke(data);
        }
    }

    public void resumeBGM(JSCallback success) {
        if (mLivePusher.resumeBGM()) {
            // 成功回调
            Map<String, Object> data = new HashMap<>();
            data.put("type", "success");
            if (success != null) success.invoke(data);
        } else {
            // 回调失败
            Map<String, Object> data = new HashMap<>();
            data.put("type", "fail");
            if (success != null) success.invoke(data);
        }
    }

    public void stopBGM(JSCallback success) {
        if (mLivePusher.stopBGM()) {
            // 成功回调
            Map<String, Object> data = new HashMap<>();
            data.put("type", "success");
            if (success != null) success.invoke(data);
        } else {
            // 回调失败
            Map<String, Object> data = new HashMap<>();
            data.put("type", "fail");
            if (success != null) success.invoke(data);
        }
    }

    public void snapShot(final JSCallback success) {
        mLivePusher.snapshot(new TXLivePusher.ITXSnapshotListener() {
            @Override
            public void onSnapshot(Bitmap bitmap) {
                if (bitmap != null) {
                    // 保存到本地
                    String path = "_doc/snapshot/snapshot_" + System.currentTimeMillis() + ".jpg";
                    path = mInstance.rewriteUri(Uri.parse(path), URIAdapter.IMAGE).getPath();
                    try {
                        File snapImage = new File(path);
                        if (!snapImage.exists()) {
                            if (!snapImage.getParentFile().exists()) {
                                snapImage.getParentFile().mkdirs();
                            }
                            snapImage.createNewFile();
                        }
                        FileOutputStream fos = new FileOutputStream(snapImage);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.flush();
                        fos.close();
                        Map<String, Object> values = new HashMap<>();
                        HashMap<String,Object> message = new HashMap<>();
                        message.put("width",bitmap.getWidth());
                        message.put("height",bitmap.getHeight());
                        message.put("tempImagePath",path);
                        values.put("message", message);
                        values.put("type", "success");
                        values.put("code",0);
                        if (success != null) success.invoke(values);
                    } catch (Exception e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("code", "-99");
                        error.put("message", e.getMessage());
                        error.put("type", "fail");
                        if (success != null) success.invoke(error);
                    }
                } else {
                    // 异常回调
                    Map<String, Object> data = new HashMap<>();
                    data.put("type", "fail");
                    data.put("code", "-99");
                    data.put("message", "data error");
                    if (success != null) success.invoke(data);
                }
            }
        });
    }

    private boolean torchIsOn = false;

    public void toggleTorch(JSCallback callback) {
        torchIsOn = !torchIsOn;
        if (mLivePusher.turnOnFlashLight(torchIsOn)) {
            // 成功回调
            Map<String, Object> data = new HashMap<>();
            data.put("type", "success");
            if (callback != null) callback.invoke(data);
        } else {
            // 回调失败
            Map<String, Object> data = new HashMap<>();
            data.put("type", "fail");
            if (callback != null) callback.invoke(data);
        }
    }

//    public void addEvent(String event) {
//        events.add(event);
//    }

    private void fireEvent(String event, Map<String, Object> params) {
        if (component.containsEvent(event)) {
            component.fireEvent(event, params);
        }
    }

    @Override
    public void onPushEvent(int i, Bundle bundle) {
//        Log.e("onPushEvent-返回的状态及错误信息：","返回的状态："+i+"，错误信息："+bundle.toString());
        Map<String, Object> values = new HashMap<>();
        values.put("code", i);
        HashMap<String,Object> detail = new HashMap<>(1);
        detail.put("detail",values);
        fireEvent("statechange", detail);
    }

    @Override
    public void onNetStatus(Bundle bundle) {
        // 触发status事件
//        Log.e("onNetStatus----返回的错误信息：","错误信息："+bundle.toString());
        Map<String, Object> infos = new HashMap<>();
        Map<String, Object> values = new HashMap<>();
        try {
            values.put("videoBitrate", bundle.getInt("VIDEO_BITRATE"));
            values.put("audioBitrate", bundle.getInt("AUDIO_BITRATE"));
            values.put("videoFPS", bundle.getInt("VIDEO_FPS"));
            values.put("videoGOP", bundle.getInt("VIDEO_GOP"));
            values.put("netSpeed", bundle.getInt("NET_SPEED"));
            values.put("netJitter", 0); // 这个暂时获取不到，用0代替
            values.put("videoWidth", bundle.getInt("VIDEO_WIDTH"));
            values.put("videoHeight", bundle.getInt("VIDEO_HEIGHT"));
        } catch (Exception ignore) {
        }
        infos.put("info", values);
        HashMap<String,Object> detail = new HashMap<>(1);
        detail.put("detail",infos);
        fireEvent("netstatus", detail);
    }

    @Override
    public void onBGMStart() {
        fireEvent("bgmstart", new HashMap<String, Object>());
    }

    @Override
    public void onBGMProgress(long l, long l1) {
        Map<String, Object> values = new HashMap<>();
        values.put("progress", l);
        values.put("duration", l1);
        HashMap<String,Object> detail = new HashMap<>(1);
        detail.put("detail",values);
        fireEvent("bgmprogress", detail);
    }

    @Override
    public void onBGMComplete(int i) {
        fireEvent("bgmcomplete", new HashMap<String, Object>());
    }

    private List<String> permissions = new ArrayList<>();

    /*//观察屏幕旋转设置变化，类似于注册动态广播监听变化机制
    private class RotationObserver extends ContentObserver
    {
        ContentResolver mResolver;

        public RotationObserver(Handler handler)
        {
            super(handler);
            mResolver = LivePublisherActivity.this.getContentResolver();
        }

        //屏幕旋转设置改变时调用
        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);
            //更新按钮状态
            if (isActivityCanRotation()) {
                mBtnOrientation.setVisibility(View.GONE);
                onActivityRotation();
            } else {
                mBtnOrientation.setVisibility(View.VISIBLE);
                mPortrait = true;
                mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                mBtnOrientation.setBackgroundResource(R.drawable.landscape);
                mLivePusher.setRenderRotation(0);
                mLivePusher.setConfig(mLivePushConfig);
            }

        }

        public void startObserver()
        {
            mResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, this);
        }

        public void stopObserver()
        {
            mResolver.unregisterContentObserver(this);
        }
    }*/
}
