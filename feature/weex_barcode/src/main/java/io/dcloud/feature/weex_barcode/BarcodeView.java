package io.dcloud.feature.weex_barcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.TextureView;
import android.widget.AbsoluteLayout;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.dcloud.zxing2.BarcodeFormat;
import com.dcloud.zxing2.Result;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.ui.component.WXComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.feature.barcode2.camera.CameraManager;
import io.dcloud.feature.barcode2.decoding.CaptureActivityHandler;
import io.dcloud.feature.barcode2.decoding.IBarHandler;
import io.dcloud.feature.barcode2.decoding.InactivityTimer;
import io.dcloud.feature.barcode2.view.DetectorViewConfig;
import io.dcloud.feature.barcode2.view.ViewfinderView;

public class BarcodeView extends AbsoluteLayout implements IBarHandler, TextureView.SurfaceTextureListener {

    private TextureView surfaceView;
    private ViewfinderView viewfinderView;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private boolean nopermission;
    private Vector<BarcodeFormat> decodeFormats;

    private boolean hasSurface = false;
    private Context context;

    private String characterSet;
    public String errorMsg = null;
    private boolean playBeep;
    private boolean vibrate;
    private MediaPlayer mediaPlayer;
    private WXComponent component;
    private WXSDKInstance mInstance;

    private boolean mConserve = false; //扫码成功后是否保存截图，解析的时候需要设置
    private String mFilename;

    private int viewWidth;
    private int viewHeight;

    private static final int ID_ADD_VIEW = 201;
    private static final int ID_UPDATE_VIEW = 202;
    private static final int ID_START_SCAN = 203;

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ID_ADD_VIEW) {
                LayoutParams param = (LayoutParams) msg.obj;
                addView(surfaceView, param);
                addView(viewfinderView);
            } else if (msg.what == ID_UPDATE_VIEW) {
                if (surfaceView != null && surfaceView.getParent() != null) {
                    surfaceView.setLayoutParams((LayoutParams) msg.obj);
                    viewfinderView.drawViewfinder();
                }
            } else if (msg.what == ID_START_SCAN) {
                if (surfaceView != null && surfaceView.getParent() != null) {
                    surfaceView.setLayoutParams((LayoutParams) msg.obj);
                    viewfinderView.drawViewfinder();
                    startP();
                }
            }
        }
    };

    public BarcodeView(Context context, WXComponent component, WXSDKInstance mInstance) {
        super(context);
        this.component = component;
        this.mInstance = mInstance;
        surfaceView = new TextureView(context);
        viewfinderView = new ViewfinderView(context, this);
        inactivityTimer = new InactivityTimer((Activity) context);
        CameraManager.init(context);
        this.context = context;
        //更换位置
        onResume(false);

        /**
         * 现在不支持方向旋转，考虑添加？
         */
        hasSurface = false;
    }

    public void initBarcodeView(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        ThreadPool.self().addThreadTask(new Runnable() {
            @Override
            public void run() {
                addBarcodeView();
            }
        });
    }

    public void updateStyles(int viewWidth, int viewHeight) {
        if (this.viewHeight == viewHeight && this.viewWidth == viewWidth) return;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        ThreadPool.self().addThreadTask(new Runnable() {
            @Override
            public void run() {
                LayoutParams params = setLayoutParams();
                if (params == null) return;
                Message msg = new Message();
                msg.what = ID_UPDATE_VIEW;
                msg.obj = params;
                mHandler.sendMessage(msg);
            }
        });
    }

    private void addBarcodeView() {
        LayoutParams lllp = setLayoutParams();
        if (lllp == null) return;
        Message msg = new Message();
        msg.what = ID_ADD_VIEW;
        msg.obj = lllp;
        mHandler.sendMessage(msg);

    }

    private boolean isVerticalScreen = true;

    private void initCamera() {
        SurfaceTexture surfaceHolder = surfaceView.getSurfaceTexture();
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException e) {
            errorMsg = e.getMessage();
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
            if (mRunning) {
                handler.restartPreviewAndDecode();
            }
        } else {
            handler.resume();
        }
    }

    public void setFrameColor(int color) {
        color = (color != -1) ? color : DetectorViewConfig.laserColor;
        DetectorViewConfig.cornerColor = color;
    }

    public void setBackground(int color) {
        color = (color != -1) ? color : DetectorViewConfig.laserColor;
        setBackgroundColor(color);
    }

    public void setScanBarColor(int color) {
        color = (color != -1) ? color : DetectorViewConfig.laserColor;
        DetectorViewConfig.laserColor = color;
    }

    public void setPlayBeep(boolean playBeep) {
        this.playBeep = playBeep;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public void setConserve(boolean mConserve) {
        this.mConserve = mConserve;
    }

    public void setFilename(String mFilename) {
        this.mFilename = mFilename;
    }

    public void setFlash(boolean enable) {
        CameraManager.get().setFlashlight(enable);
    }

    private boolean isCancelScan = false;
    private boolean mRunning = false;
    private Bitmap lastBiptmap;

    public void cancelScan() {
        if (mRunning) {
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            getViewfinderView().stopUpdateScreenTimer();
            CameraManager.get().removeAutoFocus();
            CameraManager.get().stopPreview();
            byte[] lastBitmapData = CameraManager.get().getLastBitmapData();
            Camera camera = CameraManager.get().getCameraHandler();
            if (lastBitmapData != null && camera != null)
                lastBiptmap = byte2bitmap(lastBitmapData, camera);
            CameraManager.get().closeDriver();
            mRunning = false;
            isCancelScan = true;
        }
    }

    public void closeScan() {
        onPause();
        CameraManager.get().closeDriver();
        DetectorViewConfig.clearData();
        surfaceView = null;
        if (lastBiptmap != null && !lastBiptmap.isRecycled()) {
            lastBiptmap.recycle();
            lastBiptmap = null;
        }
        CameraManager.get().clearLastBitmapData();
        /**
         * 添加恢复屏幕方向
         */
        System.gc();
    }

    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (!nopermission)
            CameraManager.get().closeDriver();
        boolean t = mRunning;
        cancel();
        mRunning = t;
    }

    public void onDestory() {
        inactivityTimer.shutdown();
        hasSurface = false;
        decodeFormats = null;
        characterSet = null;
    }

    private Bitmap byte2bitmap(byte[] data, Camera mCamera) {
        Bitmap bmp = null;
        try {
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
            bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
            Matrix m = new Matrix();
            m.postRotate(90);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmp;
    }

    public void start() {
        // 没设置filter的情况下添加默认的filter
        PermissionUtil.useSystemPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, new PermissionUtil.Request() {
            @Override
            public void onGranted(String streamPerName) {
                ThreadPool.self().addThreadTask(new Runnable() {
                    @Override
                    public void run() {
                        LayoutParams params = setLayoutParams();
                        if (params == null) return;
                        Message msg = new Message();
                        msg.what = ID_START_SCAN;
                        msg.obj = params;
                        mHandler.sendMessage(msg);
                    }
                });
            }

            @Override
            public void onDenied(String streamPerName) {
                nopermission = true;
                setBackground(Color.BLACK);
                invalidate();
            }
        });
    }

    private void startP() {
//        if (!isCancelScan) {
            initCamera();
//        }
        if (decodeFormats == null) {
            initDecodeFormats(null);
        }
        if (!TextUtils.isEmpty(errorMsg)) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 8);
            error.put("message", errorMsg);
            error.put("type","fail");
            fireEvent("error", error);
            return;
        }
        if (!mRunning) {
            getViewfinderView().startUpdateScreenTimer();
            if (handler != null)
                handler.restartPreviewAndDecode();
            else
                onResume(false);
            if (isCancelScan) {
                surfaceView.setBackgroundDrawable(null);
                if (lastBiptmap != null && !lastBiptmap.isRecycled()) {
                    lastBiptmap.recycle();
                    lastBiptmap = null;
                }
                CameraManager.get().clearLastBitmapData();
                surfaceView.postInvalidate();
                initCamera();
            }
            mRunning = true;
            isCancelScan = false;
        }
    }

    public void onResume(boolean isSysEvent) {
        if (lastBiptmap != null && isCancelScan && isSysEvent)
            surfaceView.setBackgroundDrawable(new BitmapDrawable(context.getResources(), lastBiptmap));
        if (hasSurface) {
//            initCamera();
        }else
            surfaceView.setSurfaceTextureListener(this);
        AudioManager audioService = (AudioManager) context.getSystemService(Activity.AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
            playBeep = false;
        initBeepSound();
        if (isSysEvent && mRunning) {
            mRunning = false;
            start();
        }
    }

    private LayoutParams setLayoutParams() {
        CameraManager.sScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        CameraManager.sScreenAllHeight = context.getResources().getDisplayMetrics().heightPixels;
        Rect gatherRect = DetectorViewConfig.getInstance().gatherRect;
        gatherRect.left = 0;
        gatherRect.top = 0;
        gatherRect.right = viewWidth;
        gatherRect.bottom = viewHeight;

        //获取相机预览分辨率，不应超过采集区域(div)大小，避免超出采集区域，影响到webview其他内容展示
        Point camearResolution = CameraManager.get().getCR(gatherRect.height(), gatherRect.width());
        if (camearResolution == null) {
//            nopermission = true;
//            setBackgroundColor(Color.BLACK);
//            invalidate();
//            return null;
            camearResolution = new Point(viewWidth,viewHeight);
        }
        // 部分设备surfaceview出现黏连的问题（即显示区域超出div范围），故将surfaceview切换为textureview
        // 重新计算surfaceView宽高，将短边扩大实现全屏显示
        int surfaceViewWidth, surfaceViewHeight;
        //公式 w/h = y/x
        surfaceViewWidth = viewWidth;
        surfaceViewHeight = surfaceViewWidth * camearResolution.x / camearResolution.y;//获得分辨率的宽高比，根据此宽高比计算出适合的surfaceview布局
        int left = 0, top = 0;

        if (surfaceViewHeight < viewHeight) {
            surfaceViewHeight = viewHeight;
            surfaceViewWidth = surfaceViewHeight * camearResolution.y / camearResolution.x;
            left = (viewWidth - surfaceViewWidth) / 2;
            DetectorViewConfig.detectorRectOffestLeft = left;
        } else {
            surfaceViewHeight = surfaceViewWidth * camearResolution.x / camearResolution.y;
            top = (viewHeight - surfaceViewHeight) / 2;
            DetectorViewConfig.detectorRectOffestTop = top;
        }
        LayoutParams lllp = new LayoutParams(surfaceViewWidth, surfaceViewHeight, left, top);
        DetectorViewConfig.getInstance().initSurfaceViewRect(left, top, surfaceViewWidth, surfaceViewHeight);
        return lllp;
    }

    /**
     * 设置支持的扫码字段
     *
     * @param filters
     */
    public void initDecodeFormats(JSONArray filters) {
        decodeFormats = new Vector<BarcodeFormat>();
        if (filters == null || filters.size() == 0) {//默认支持
            decodeFormats.add(BarcodeFormat.EAN_13);
            decodeFormats.add(BarcodeFormat.EAN_8);
            decodeFormats.add(BarcodeFormat.QR_CODE);
        } else {
            int size = filters.size();
            for (int i = 0; i < size; i++) {
                int filter = -1;
                try {
                    filter = filters.getInteger(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (filter != -1) {
                    decodeFormats.add(convertNumToBarcodeFormat(filter));
                }
            }
        }
    }

    static final int UNKOWN = -1000;
    static final int QR = 0;
    static final int EAN13 = 1;
    static final int EAN8 = 2;
    static final int AZTEC = 3;
    static final int DATAMATRIX = 4;
    static final int UPCA = 5;
    static final int UPCE = 6;
    static final int CODABAR = 7;
    static final int CODE39 = 8;
    static final int CODE93 = 9;
    static final int CODE128 = 10;
    static final int ITF = 11;
    static final int MAXICODE = 12;
    static final int PDF417 = 13;
    static final int RSS14 = 14;
    static final int RSSEXPANDED = 15;

    private BarcodeFormat convertNumToBarcodeFormat(int num) {
        BarcodeFormat _ret = null;
        switch (num) {
            case QR: {
                _ret = BarcodeFormat.QR_CODE;
                break;
            }
            case EAN13: {
                _ret = BarcodeFormat.EAN_13;
                break;
            }
            case EAN8: {
                _ret = BarcodeFormat.EAN_8;
                break;
            }
            case AZTEC: {
                _ret = BarcodeFormat.AZTEC;
                break;
            }
            case DATAMATRIX: {
                _ret = BarcodeFormat.DATA_MATRIX;
                break;
            }
            case UPCA: {
                _ret = BarcodeFormat.UPC_A;
                break;
            }
            case UPCE: {
                _ret = BarcodeFormat.UPC_E;
                break;
            }
            case CODABAR: {
                _ret = BarcodeFormat.CODABAR;
                break;
            }
            case CODE39: {
                _ret = BarcodeFormat.CODE_39;
                break;
            }
            case CODE93: {
                _ret = BarcodeFormat.CODE_93;
                break;
            }
            case CODE128: {
                _ret = BarcodeFormat.CODE_128;
                break;
            }
            case ITF: {
                _ret = BarcodeFormat.ITF;
                break;
            }
            case MAXICODE: {
                _ret = BarcodeFormat.MAXICODE;
                break;
            }
            case PDF417: {
                _ret = BarcodeFormat.PDF_417;
                break;
            }
            case RSS14: {
                _ret = BarcodeFormat.RSS_14;
                break;
            }
            case RSSEXPANDED: {
                _ret = BarcodeFormat.RSS_EXPANDED;
                break;
            }
        }
        return _ret;
    }

    private int convertTypestrToNum(BarcodeFormat format) {
        if (format == BarcodeFormat.QR_CODE) {
            return QR;
        } else if (format == BarcodeFormat.EAN_13) {
            return EAN13;
        } else if (format == BarcodeFormat.EAN_8) {
            return EAN8;
        } else if (format == BarcodeFormat.AZTEC) {
            return AZTEC;
        } else if (format == BarcodeFormat.DATA_MATRIX) {
            return DATAMATRIX;
        } else if (format == BarcodeFormat.UPC_A) {
            return UPCA;
        } else if (format == BarcodeFormat.UPC_E) {
            return UPCE;
        } else if (format == BarcodeFormat.CODABAR) {
            return CODABAR;
        } else if (format == BarcodeFormat.CODE_39) {
            return CODE39;
        } else if (format == BarcodeFormat.CODE_93) {
            return CODE93;
        } else if (format == BarcodeFormat.CODE_128) {
            return CODE128;
        } else if (format == BarcodeFormat.ITF) {
            return ITF;
        } else if (format == BarcodeFormat.MAXICODE) {
            return MAXICODE;
        } else if (format == BarcodeFormat.PDF_417) {
            return PDF417;
        } else if (format == BarcodeFormat.RSS_14) {
            return RSS14;
        } else if (format == BarcodeFormat.RSS_EXPANDED) {
            return RSSEXPANDED;
        }
        return UNKOWN;
    }

    @Override
    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    @Override
    public void autoFocus() {
        handler.autoFocus();
    }

    @Override
    public void handleDecode(Result obj, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        boolean saveSuc = false;
        String realPath = null;
        if (mConserve) {
            if (!PdrUtil.isEmpty(mFilename)) {
                if (!PdrUtil.isDeviceRootDir(mFilename) && !mFilename.startsWith(BaseInfo.REL_PRIVATE_DOC_DIR)) {
                    mFilename = BaseInfo.REL_PRIVATE_DOC_DIR + mFilename;
                }
            }
            realPath = mInstance.rewriteUri(Uri.parse(mFilename), URIAdapter.IMAGE).getPath();
            saveSuc = PdrUtil.saveBitmapToFile(barcode, realPath);
        }
        int num = convertTypestrToNum(obj.getBarcodeFormat());
        String json;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("code", num);
        params.put("message", obj.getText());
        if (saveSuc && !PdrUtil.isEmpty(realPath)) {
            params.put("file", realPath);
        }
        params.put("type","success");
        //这里触发回调
        fireEvent("marked", params);
        // 一次start只能触发一次回调，成功或失败
        cancelScan();
    }

    private void cancel() {
        if (mRunning) {
            if (handler != null)
                handler.stopDecode();
            getViewfinderView().stopUpdateScreenTimer();
            mRunning = false;
        }
    }

    private void initBeepSound() {
        if (mediaPlayer == null) {
            ((Activity) context).setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            try {
                AssetFileDescriptor file = context.getResources().getAssets().openFd(StringConst.RES_BEEP);
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };
    private static final float BEEP_VOLUME = 0.80f;
    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            try {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VIBRATE_DURATION);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (!hasSurface) {
            hasSurface = true;
            if (!isCancelScan) {
//                initCamera();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        hasSurface = false;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (nopermission) {
            Paint paint = new TextPaint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(PdrUtil.pxFromDp(18, context.getResources().getDisplayMetrics()));
            paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float top = fontMetrics.top;
            float bottom = fontMetrics.bottom;
            int baseLineY = (int) (viewHeight / 2f - top / 2 - bottom / 2);
            int pl = viewWidth / 2;
            String language;
            if (Build.VERSION.SDK_INT >= 24) {
                language = getResources().getConfiguration().getLocales().get(0).getLanguage();
            } else {
                language = getResources().getConfiguration().locale.getLanguage();
            }
            if (language.equalsIgnoreCase("en")){
                canvas.drawText("Need camera permission", pl, baseLineY, paint);
            } else {
                canvas.drawText("未获得相机权限", pl, baseLineY, paint);
            }
        }


    }

//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        super.onLayout(changed, l, t, r, b);
    // 解决部分低端机获取不到宽高的问题，重新计算并设置区域
//        if (surfaceView.getParent() != null && Build.VERSION.SDK_INT < 23 /*&& getContext().getApplicationInfo().targetSdkVersion < 23*/) {
//            AbsoluteLayout.LayoutParams param = setLayoutParams();
//            if (param != null)
//                surfaceView.setLayoutParams(param);
//        }
//    }

//    private List<String> events;
//
//    protected void addEventListener(String type) {
//        if (null == events)
//            events = new ArrayList<>();
//        if (!events.contains(type))
//            events.add(type);
//    }

    private void fireEvent(String type, Map<String, Object> params) {
//        if (null == events) return;
        if(component.containsEvent(type)) {
            component.fireEvent(type, params);
        }
    }
}
