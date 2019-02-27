package com.upyun.hardware.Listener;

import android.util.Log;

public interface DcUpYunStateListener {
    // RTMP States

    public void onRtmpVideoStreaming(String msg);
    public void onRtmpAudioStreaming(String msg);
    public void onRtmpStopped(String msg);
    public void onRtmpConnecting(String msg);
    public void onRtmpConnected(String msg);
    public void onRtmpDisconnected(String msg);
    public void onRtmpOutputFps(final double fps);
    public void onRtmpDataInfo(int bitrate, long totalSize);
    public void onNetWorkError(Exception e, int tag);

    //
}
