package com.seu.magicfilter;

import android.util.Log;

import com.seu.magicfilter.camera.CameraEngine;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.widget.MagicCameraView;
import com.seu.magicfilter.widget.base.MagicBaseView;
import com.upyun.hardware.Listener.DcUpYunStateListener;

import net.ossrs.yasea.rtmp.RtmpPublisher;

/**
 * Created by why8222 on 2016/2/25.
 */
public class MagicEngine implements RtmpPublisher.EventHandler {
    private static final String TAG = "MagicEngine";
    private static MagicEngine magicEngine;
    private static DcUpYunStateListener listener;

    public static MagicEngine getInstance() {
        if (magicEngine == null)
            throw new NullPointerException("MagicEngine must be built first");
        else
            return magicEngine;
    }

    private MagicEngine(Builder builder) {
        magicEngine = this;
    }

    public void setFilter(MagicFilterType type) {
        MagicParams.magicBaseView.setFilter(type);
    }

    public void startRecord() {
        new Thread() {
            @Override
            public void run() {
                if (MagicParams.magicBaseView instanceof MagicCameraView)
                    ((MagicCameraView) MagicParams.magicBaseView).changeRecordingState(true);
            }
        }.start();
    }

    public void stopRecord() {
        if (MagicParams.magicBaseView instanceof MagicCameraView)
            ((MagicCameraView) MagicParams.magicBaseView).changeRecordingState(false);
    }

    public void setBeautyLevel(int level) {
        if (MagicParams.magicBaseView instanceof MagicCameraView && MagicParams.beautyLevel != level) {
            MagicParams.beautyLevel = level;
            ((MagicCameraView) MagicParams.magicBaseView).onBeautyLevelChanged();
        }
    }

    public void switchCamera() {
        CameraEngine.switchCamera();
    }

    public void switchCamera2(int CameraType){ CameraEngine.switchCamera2(CameraType);}

    public void switchFlashlight() {
        CameraEngine.switchFlashlight();
    }

    public void focusOnTouch() {
        CameraEngine.focusOnTouch();
    }

    public void setSilence(boolean b) {
        MagicParams.SILENCE = b;
    }

    public void setStateListener(DcUpYunStateListener dclistener){
        listener = dclistener;
    }

    @Override
    public void onRtmpConnecting(String msg) {
        try {
            listener.onRtmpConnecting(msg);
        }catch (Exception ex){
            Log.i(TAG, "onRtmpConnecting"+msg);
        }
    }

    @Override
    public void onRtmpConnected(String msg) {
        try {
            listener.onRtmpConnected(msg);
        }catch (Exception ex){
            Log.i(TAG, "onRtmpConnected"+msg);
        }

    }

    @Override
    public void onRtmpVideoStreaming(String msg) {
        try {
            listener.onRtmpVideoStreaming(msg);
        }catch (Exception ex){
            Log.i(TAG, "onRtmpVideoStreaming"+msg);
        }

    }

    @Override
    public void onRtmpAudioStreaming(String msg) {
        try {
            listener.onRtmpAudioStreaming(msg);
        }catch (Exception ex){
            Log.i(TAG, "onRtmpAudioStreaming"+msg);
        }

    }

    @Override
    public void onRtmpStopped(String msg) {
        try {
            listener.onRtmpStopped(msg);
        }catch (Exception ex){
            Log.i(TAG, "onRtmpStopped"+msg);
        }

    }

    @Override
    public void onRtmpDisconnected(String msg) {
        try {
            listener.onRtmpDisconnected(msg);
        }catch (Exception ex){
            Log.i(TAG,"onRtmpDisconnected"+ msg);
        }

    }

    @Override
    public void onRtmpOutputFps(final double fps) {
        try {
            listener.onRtmpOutputFps(fps);
        }catch (Exception ex){
            Log.i(TAG, String.format("Output Fps: %f", fps));
        }

    }

    @Override
    public void onRtmpDataInfo(int bitrate, long totalSize) {
        try {
            listener.onRtmpDataInfo(bitrate,totalSize);
        }catch (Exception ex){
            Log.e(TAG, "onRtmpDataInfo:");
        }

    }

    @Override
    public void onNetWorkError(Exception e, int tag) {
        try {
            listener.onNetWorkError(e,tag);
        }catch (Exception ex){
            Log.e(TAG, "onNetWorkError:" + e.toString());
        }

    }

    public static class Builder {

        public MagicEngine build(MagicBaseView magicBaseView) {
            MagicParams.context = magicBaseView.getContext();
            MagicParams.magicBaseView = magicBaseView;
            return new MagicEngine(this);
        }

        public Builder setVideoPath(String path) {
            MagicParams.videoPath = path;
            return this;
        }

        public Builder setVideoName(String name) {
            MagicParams.videoName = name;
            return this;
        }

        public Builder setVideoHeight(int height) {
            MagicParams.HEIGHT = height;
            return this;
        }

        public Builder setVideoWidth(int width) {
            MagicParams.WIDTH = width;
            return this;
        }

    }
}
