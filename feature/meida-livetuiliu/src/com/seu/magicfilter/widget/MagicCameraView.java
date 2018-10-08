package com.seu.magicfilter.widget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.seu.magicfilter.camera.CameraEngine;
import com.seu.magicfilter.camera.utils.CameraInfo;
import com.seu.magicfilter.encoder.video.TextureMovieEncoder;
import com.seu.magicfilter.filter.advanced.MagicBeautyFilter;
import com.seu.magicfilter.filter.advanced.MagicWaterFilter;
import com.seu.magicfilter.filter.base.MagicCameraInputFilter;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.utils.OpenGlUtils;
import com.seu.magicfilter.widget.base.MagicBaseView;
import com.upyun.hardware.Listener.DcUpYunStateListener;
import com.upyun.hardware.Watermark;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by why8222 on 2016/2/25.
 */
public class MagicCameraView extends MagicBaseView {

    private static final int SCREEN_ON_FLAG = 9000;
    private static final int SCREEN_OFF_FLAG = 9001;

    private static final String TAG = "MagicCameraView";
    private MagicCameraInputFilter cameraInputFilter;
    private MagicCameraViewStateListener listener;
    private SurfaceTexture surfaceTexture;

    public MagicCameraView(Context context) {
        this(context, null);
    }

    private boolean recordingEnabled;
    private int recordingStatus;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static TextureMovieEncoder videoEncoder = new TextureMovieEncoder();

    private String outputPath;
    private Object mSurfaceAvailable = new Object();
    private boolean surfaceAvailable = false;
    private Activity mAttachActivity;

    private DcUpYunStateListener upListener;

    public void setUpListener(DcUpYunStateListener upListener) {
        this.upListener = upListener;
    }

    public MagicCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.getHolder().addCallback(this);
        outputPath = MagicParams.videoPath + MagicParams.videoName;
        recordingStatus = -1;
        recordingEnabled = false;
        scaleType = ScaleType.CENTER_CROP;
        //修改屏幕常亮和横竖屏切换
        if (context instanceof Activity){
            mAttachActivity = (Activity) context;
        } else {
            throw new IllegalArgumentException("Context must be Activity");
        }
    }

    public String resetOutputPath(){
        outputPath = MagicParams.videoPath + MagicParams.videoName;
        return outputPath;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            listener.onConfigurationChanged(newConfig);

            if (CameraEngine.getCamera() == null)
                CameraEngine.openCamera();
            CameraInfo info = CameraEngine.getCameraInfo();

            inputSizeChanged(info);
            adjustSize(info.orientation, info.isFront, false);
//

        }catch (Exception e){

        }
    }

    private void inputSizeChanged(CameraInfo info) {
        int orientation = 0;
        switch (mAttachActivity.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                orientation = 270;
                break;
            case Surface.ROTATION_90:
                orientation = 0;
                break;
            case Surface.ROTATION_180:
                orientation = 90;
                break;
            case Surface.ROTATION_270:
                orientation = 180;
                break;
        }
        if (orientation == 90 || orientation == 270) {
            imageWidth = info.previewHeight;
            imageHeight = info.previewWidth;
        } else {
            imageWidth = info.previewWidth;
            imageHeight = info.previewHeight;
        }
        cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        recordingEnabled = videoEncoder.isRecording();
        if (recordingEnabled)
            recordingStatus = RECORDING_RESUMED;
        else
            recordingStatus = RECORDING_OFF;
        if (cameraInputFilter == null)
            cameraInputFilter = new MagicCameraInputFilter();
        cameraInputFilter.init();
        if (textureId == OpenGlUtils.NO_TEXTURE) {
            textureId = OpenGlUtils.getExternalOESTextureID();
            if (textureId != OpenGlUtils.NO_TEXTURE) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
            }
        }

        synchronized (mSurfaceAvailable) {
            surfaceAvailable = true;
            mSurfaceAvailable.notify();
        }

        try {
            listener.onSurfaceChanged();
        }catch (Exception e){}
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        openCamera();

        synchronized (mSurfaceAvailable) {
            surfaceAvailable = true;
            mSurfaceAvailable.notify();
        }
    }

    public void setViewstateListener(MagicCameraViewStateListener stlistener)
    {
        listener = stlistener;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        if (surfaceTexture == null)
            return;
        try { surfaceTexture.updateTexImage(); }catch (Exception e) { }
//        Log.e("onDrawFrame", "recordingEnabled " + recordingEnabled);
        if (recordingEnabled) {
            switch (recordingStatus) {
                case RECORDING_OFF:
                    if (videoEncoder.isRecording()) {
                        if (upListener != null)
                            upListener.onNetWorkError(new Exception("livePusher is stoping,please wait one minute"),10);
                        return;
                    }
                    CameraInfo info = CameraEngine.getCameraInfo();
                    Log.d("CameraInfo:", info.previewWidth + "::" + info.previewHeight);
                    videoEncoder.setPreviewSize(info.previewHeight, info.previewWidth);
                    videoEncoder.setTextureBuffer(gLTextureBuffer);
                    videoEncoder.setCubeBuffer(gLCubeBuffer);
                    try {
                        videoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                                outputPath, MagicParams.WIDTH, MagicParams.HEIGHT,
                                700000, EGL14.eglGetCurrentContext(),
                                info));
                    } catch (Exception e) {
//                        e.printStackTrace();
                        if (upListener != null)
                            upListener.onNetWorkError(new Exception("livePusher is stoping,please wait one minute"),10);
                        recordingEnabled = false;
                        recordingStatus = RECORDING_RESUMED;
                    }
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    videoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    if (filterChanged) {
                        videoEncoder.changeFilter();
                        filterChanged = false;
                    }
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    videoEncoder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }
        float[] mtx = new float[16];
        surfaceTexture.getTransformMatrix(mtx);
        cameraInputFilter.setTextureTransformMatrix(mtx);
        int id = textureId;
        if (filter == null) {
            cameraInputFilter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        } else {
            id = cameraInputFilter.onDrawToTexture(textureId);
            filter.onDrawFrame(id, gLCubeBuffer, gLTextureBuffer);
        }
        videoEncoder.setTextureId(id);
        videoEncoder.frameAvailable(surfaceTexture);
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };

    @Override
    public void setFilter(MagicFilterType type) {
        super.setFilter(type);
        videoEncoder.setFilter(type);
    }

    private Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SCREEN_ON_FLAG) {
                mAttachActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else if (msg.what == SCREEN_OFF_FLAG) {
                mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    };

    private void openCamera() {
        if (CameraEngine.getCamera() == null)
            CameraEngine.openCamera();
        CameraInfo info = CameraEngine.getCameraInfo();
        inputSizeChanged(info);
        adjustSize(info.orientation, info.isFront, false);
        if (surfaceTexture != null)
            CameraEngine.startPreview(surfaceTexture);
        mhandler.sendEmptyMessage(SCREEN_ON_FLAG);
    }


    public void switchCamera() {
        CameraEngine.switchCamera();
        CameraInfo info = CameraEngine.getCameraInfo();
        inputSizeChanged(info);
        adjustSize(info.orientation, info.isFront, false);
        if (surfaceTexture != null)
            CameraEngine.startPreview(surfaceTexture);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        CameraEngine.releaseCamera();
        mhandler.sendEmptyMessage(SCREEN_OFF_FLAG);
        synchronized (mSurfaceAvailable) {
            surfaceAvailable = false;
        }
    }

    public void changeRecordingState(boolean isRecording) {

        synchronized (mSurfaceAvailable) {

            if (!isRecording){
                recordingEnabled = isRecording;
                return;
            }

            while (!surfaceAvailable) {
                try {
                    mSurfaceAvailable.wait();
                    Log.e(TAG, "changeRecordingState: " + surfaceAvailable);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            recordingEnabled = isRecording;
        }
    }

    public void stopRecording(){
        videoEncoder.stopRecording();
    }
    protected void onFilterChanged() {
        super.onFilterChanged();
        cameraInputFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
        if (filter != null)
            cameraInputFilter.initCameraFrameBuffer(imageWidth, imageHeight);
        else
            cameraInputFilter.destroyFramebuffers();

        try {
            listener.onFilterChanged();
        }catch (Exception e){}
    }

    public void onBeautyLevelChanged() {
        cameraInputFilter.onBeautyLevelChanged();
        try {
            listener.onBeautyLevelChanged();
        }catch (Exception e){}
    }

    public void setWatermark(final Watermark watermark) {
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                MagicWaterFilter.mWatermark = watermark;
                setFilter(MagicFilterType.WATERMARK);
            }
        }, 1000);
    }
}
