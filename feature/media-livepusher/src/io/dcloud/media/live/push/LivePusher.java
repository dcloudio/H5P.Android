package io.dcloud.media.live.push;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;

import com.dcloud.android.widget.AbsoluteLayout;
import com.seu.magicfilter.MagicEngine;
import com.seu.magicfilter.camera.CameraEngine;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.widget.MagicCameraView;
import com.seu.magicfilter.widget.MagicCameraViewStateListener;
import com.upyun.hardware.Listener.DcUpYunStateListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IFrameView;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.util.ViewRect;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.media.live.LivePusherStateListener;

public class LivePusher extends AdaFrameItem implements  DcUpYunStateListener{
    private android.widget.AbsoluteLayout mainView = null;
    Pattern rtmpUrlPattern = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");
    public  boolean isInited = false;
    MagicEngine.Builder builder = null;
    MagicCameraView cameraView = null;
    MagicEngine  magicEngine = null;
//    IWebview eventListenerWebview = null;
    private boolean onCloseing = false;
    private boolean onStoped = false;
    private boolean inRecording = false;
    private String curPusherID = null;
    private Configuration curConfig;
    private LivePusherStateListener listener;
    private String startCallbackID = null;
    private IWebview startWebview = null;
    private Activity mAttachActivity = null;

    private AbsoluteLayout.LayoutParams _lp = null;

    private String userId;
    private String uuid;

    private JSONArray arr;
    private JSONObject options = null;
    private String position = "static";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private enum EScreenOrientation{
        VERTIAL,
        HORIZONTAL
    }

    // Optons
    String rtmpULR = null;
    int  nCameraIndex = 1;
    boolean bautoFocus = true;
    int bBeauty = 0;
    boolean bSilence = false;
    boolean bOpenCamera = true;
    int bWhiteness = 0;
    int     nMinBitrate = 200;
    int     nMaxBitrate = 1000;

    String aspect = "3:4";
    String mode = "HD";

    int pusherWidth = 0;
    int pusherHeight = 0;
    EScreenOrientation  styOrientation = EScreenOrientation.VERTIAL;

    private IWebview initWebview;

    private boolean isFromCreate = false;//不是通过create创建，但是参数中含有position等参数的问题

    public LivePusher(IWebview pWebview,JSONArray style){
        super(pWebview.getContext());
        setUserId(style.optString(1));
        uuid = style.optString(0);
        arr = style.optJSONArray(2);
        options = style.optJSONObject(3);
        position = JSONUtil.getString(options,"position");
        cameraView = new MagicCameraView(pWebview.getContext(), null);
        mainView = new android.widget.AbsoluteLayout(pWebview.getContext()){
            Paint paint = new Paint();
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
            }
        };
        setMainView(mainView);
        eventCallBacks = new HashMap<String,HashMap<String, IWebview>>();
    }

    public void initLivePusher(IWebview pWebViewImpl, JSONArray array){

        this.initWebview = pWebViewImpl;
        //解析html控件位置大小
        try {
            if (arr == null || arr.length() <= 0){
                isFromCreate = true;
                arr = new JSONArray();
                arr.put(0,JSONUtil.getString(options,"left"));
                arr.put(1,JSONUtil.getString(options,"top"));
                arr.put(2,JSONUtil.getString(options,"width"));
                arr.put(3,JSONUtil.getString(options,"height"));
            }
            if (options != null){
                setOptions(null,options);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Rect dvc = getRect(pWebViewImpl, arr);

        int cameraWidth = 0;
        if (dvc.width()%2 == 0){
             cameraWidth = dvc.width();
        }else{
            cameraWidth = dvc.width()-1;
        }

        int cameraHeight = 0;
        if (dvc.height() % 2 == 0){
            cameraHeight = dvc.height();
        }else{
            cameraHeight = dvc.height() - 1;
        }

        builder = new MagicEngine.Builder().setVideoPath(rtmpULR).setVideoName("").setVideoWidth(cameraWidth).setVideoHeight(cameraHeight);

        if(dvc.width() != 0 && dvc.height() != 0){

            _lp = (AbsoluteLayout.LayoutParams) AdaFrameItem.LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
            if (cameraView == null){
                cameraView = new MagicCameraView(pWebViewImpl.getContext(), null);
            }
            cameraView.setUpListener(this);
            magicEngine = builder.build(cameraView);
            if(null == cameraView.getParent()){
                mainView.addView(cameraView);
            }

            // settings
            if (bautoFocus){
                cameraView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        magicEngine.focusOnTouch();
                        return false;
                    }
                });
            }

            cameraView.setViewstateListener(new MagicCameraViewStateListener() {
                @Override
                public void onSurfaceChanged() {
                    if (bBeauty == 0) {
                        magicEngine.setFilter(MagicFilterType.NONE);
                    } else {
                        magicEngine.setFilter(MagicFilterType.WHITECAT);
                    }
                    magicEngine.setBeautyLevel(bWhiteness);
                }

                @Override
                public void onFilterChanged() {
                }

                @Override
                public void onBeautyLevelChanged() {
                }

                @Override
                public void onConfigurationChanged(Configuration newConfig) {
                    curConfig = newConfig;
                    setOrientation();

                }
            });
            CameraEngine.openCamera(nCameraIndex);
            if (!bOpenCamera){
                CameraEngine.stopPreview();
            }

            CameraEngine.setRotation(0);

            magicEngine.setSilence(bSilence);
            magicEngine.setStateListener(this);
            isInited = true;
            if (!isFromCreate)
                pWebViewImpl.addFrameItem(this,_lp);

            //注册手机方向监听，规避截图时图像方向颠倒
            moel = new OrientationEventListener(initWebview.getContext(),
                    SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                        return;
                    }
                    if (orientation > 350 || orientation < 10 ) {
                        oritation = 270;
                    }else if (orientation > 80 && orientation < 100) {
                        oritation = 180;
                    }else if (orientation > 170 && orientation < 190) { //180度
                        oritation = 90;
                    } else if (orientation > 260 && orientation < 280) {
                        oritation = 0;
                    } else {
                        return;
                    }
                }
            };
            if (moel.canDetectOrientation()) {
                moel.enable();
            } else {
                moel.disable();
            }
        }
    }

    private boolean startPreview = false;
    public void preview(IWebview pwebview) {
        if (!isInited)
            initLivePusher(pwebview,null);
        startPreview = true;
        if (bOpenCamera) {//当前为关闭摄像头模式时不允许预览
            cameraView.setBackgroundColor(Color.TRANSPARENT);
            CameraEngine.startPreview();
        }
    }
    @NonNull
    private Rect getRect(IWebview pWebViewImpl, JSONArray arr) {
        AdaFrameItem frameView = (AdaFrameItem)pWebViewImpl.obtainFrameView();
        ViewRect webParentViewRect = frameView.obtainFrameOptions();

        float s = pWebViewImpl.getScale();

        Rect dvc = new Rect();
        int _w = PdrUtil.convertToScreenInt(JSONUtil.getString(arr,2),webParentViewRect.width,webParentViewRect.width,s);
        int _h = PdrUtil.convertToScreenInt(JSONUtil.getString(arr,3),webParentViewRect.height,webParentViewRect.height,s);
        dvc.left = PdrUtil.convertToScreenInt(JSONUtil.getString(arr,0),webParentViewRect.width,0,s);
        dvc.top = PdrUtil.convertToScreenInt(JSONUtil.getString(arr,1),webParentViewRect.height,0,s);
        dvc.right = dvc.left + _w;
        dvc.bottom = dvc.top + _h;

        pusherHeight = _h;
        pusherWidth = _w;

        updateViewRect((AdaFrameItem)pWebViewImpl.obtainFrameView(), new int[]{dvc.left,dvc.top,dvc.width(),dvc.height()}, new int[]{webParentViewRect.width,webParentViewRect.height});
        return dvc;
    }

    public void appendLivePusher(String positions, IFrameView frameView){
        if(obtainMainView() != null && obtainMainView().getParent() != null){
            removeFromFrame();
        }
        initWebview = frameView.obtainWebView();
        Rect dvc = getRect(initWebview,arr);
        _lp = (AbsoluteLayout.LayoutParams) AdaFrameItem.LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
        if (isFromCreate) {
            if ("absolute".equals(position)) {
                initWebview.obtainFrameView().addFrameItem(this, _lp);

            } else {
                initWebview.addFrameItem(this, _lp);
            }
        } else {
            initWebview.addFrameItem(this, _lp);
        }

    }

    public void removeFromFrame() {
        if("absolute".equals(position)) {
            initWebview.obtainFrameView().removeFrameItem(this);
        } else {
            initWebview.removeFrameItem(this);
        }
    }

    @Override
    protected void onResize() {
        if (isRegisterResize)
            return;
        super.onResize();
        Rect dvc = getRect(initWebview,arr);

        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) AdaFrameItem.LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
        mainView.setLayoutParams(lp);


//        int orientation = mAttachActivity.getRequestedOrientation();
        setOrientation();
    }

    private boolean isRegisterResize = false;
    public void resize (IWebview pWebview, JSONArray objarray){
        Rect dvc = getRect(pWebview,objarray.optJSONArray(1));

        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) AdaFrameItem.LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
        mainView.setLayoutParams(lp);


//        int orientation = mAttachActivity.getRequestedOrientation();
        setOrientation();
        isRegisterResize = true;

    }

    private void setResolution(String resolution){
        if (magicEngine == null)
            return;
        switch (resolution) {
            case "FHD":
                //1080*1920;1080*1440
                changeMode(1920,1440,1080);
                break;
            case "HD":
                //480*720;480*640

                changeMode(1920,1440,1080);
                break;
            case "SD":
                //360*480;320*480
                if (aspect.equals("3:4")) {
                    magicEngine.setVideoWidth(360);
                    magicEngine.setVideoHeight(480);
                } else if (aspect.equals("9:16")) {
                    magicEngine.setVideoWidth(320);
                    magicEngine.setVideoHeight(480);
                }
                break;
            default:
                changeMode(1920,1440,1080);
                break;
        }
    }

    private void changeMode(int width16,int width4,int height){
        if (aspect.equals("3:4")) {
            magicEngine.setVideoWidth(height);
            magicEngine.setVideoHeight(width4);
        } else if (aspect.equals("9:16")) {
            magicEngine.setVideoWidth(height);
            magicEngine.setVideoHeight(width16);
        }
    }

    public void setOptions(IWebview pwebview, JSONObject jsonObject){
//        JSONObject jsonObject = array.optJSONObject(1);
        options = JSONUtil.combinJSONObject(options, jsonObject);
        if(jsonObject.has("top") || jsonObject.has("left") || jsonObject.has("width") || jsonObject.has("height") || jsonObject.has("position")) {
            String mPosition = JSONUtil.getString(jsonObject,"position");
            try {
                arr.put(0, JSONUtil.getString(jsonObject, StringConst.JSON_KEY_LEFT));
                arr.put(1, JSONUtil.getString(jsonObject, StringConst.JSON_KEY_TOP));
                arr.put(2, JSONUtil.getString(jsonObject, StringConst.JSON_KEY_WIDTH));
                arr.put(3, JSONUtil.getString(jsonObject, StringConst.JSON_KEY_HEIGHT));
            }catch (JSONException e){
            }
            Rect dvc = getRect(initWebview,arr);
            _lp = (AbsoluteLayout.LayoutParams) AdaFrameItem.LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
            if (jsonObject.has("position")) {
                if (!mPosition.equals(position)) {
                    if ("absolute".equals(position)) {
                        initWebview.obtainFrameView().removeFrameItem(this);
                        initWebview.addFrameItem(this, _lp);
                    } else if ("static".equals(position)) {
                        initWebview.removeFrameItem(this);
                        initWebview.obtainFrameView().addFrameItem(this, _lp);
                    }
                } else {
                    obtainMainView().setLayoutParams(_lp);
                }
                position = mPosition;
            } else {
                obtainMainView().setLayoutParams(_lp);
            }
        }
        if (null != options){
            //url
            rtmpULR = options.optString("url");
            MagicParams.videoPath = rtmpULR;
            //mute
            bSilence = options.optBoolean("mute",false);
            if (magicEngine != null)
                magicEngine.setSilence(bSilence);
            //enable-camera
            bOpenCamera = options.optBoolean("enable-camera",true);
            CameraEngine.openCamera(nCameraIndex);
            if (cameraView != null) {
                if (!bOpenCamera) {
                    cameraView.setBackgroundColor(Color.BLACK);
                    CameraEngine.stopPreview();
                } else {
                    cameraView.setBackgroundColor(Color.TRANSPARENT);
                    CameraEngine.startPreview();
                }
            }
            //auto-focus
            bautoFocus = options.optBoolean("auto-focus",true);
            //orientation
            String orientation = options.optString("orientation");
            if (orientation != null && orientation.equalsIgnoreCase("vertical")){
                styOrientation = EScreenOrientation.VERTIAL;
                CameraEngine.setRotation(90);
            }else{
                styOrientation = EScreenOrientation.HORIZONTAL;
                CameraEngine.setRotation(0);
            }
            //beauty美颜
            bBeauty = options.optInt("beauty",0);
            if (magicEngine != null) {
//                magicEngine.setBeautyLevel(bBeauty);
                if (bBeauty == 1) {
                    magicEngine.setFilter(MagicFilterType.WHITECAT);
                } else {
                    magicEngine.setFilter(MagicFilterType.NONE);
                }
            }
            //whiteness美白程度
            bWhiteness = options.optInt("whiteness",0);
            if (magicEngine != null) {
                magicEngine.setBeautyLevel(bWhiteness);
            }
            //aspect
            aspect = options.optString("aspect","3:4");
            if (aspect.equals("16:9")||aspect.equals("3:4")) {
                aspect.equals("9:16");
            } else {
                aspect.equals("3:4");
            }
            // mode
            mode = options.optString("mode","FUD").toUpperCase();
        }
    }

    private HashMap<String,HashMap<String,IWebview>> eventCallBacks;
    public void addEventListener(IWebview pwebview, JSONArray array){
//        eventListenerWebview = pwebview;
        String webId = array.optString(1);
        String event = array.optString(2);
        String callBackId = array.optString(3);
        HashMap<String,IWebview> callbacks = eventCallBacks.get(event);
        if (callbacks == null) {
            callbacks = new HashMap<String,IWebview>();
        }
        callbacks.put(callBackId,pwebview);
        eventCallBacks.put(event,callbacks);

    }

    public void start(IWebview pWebView, JSONArray array){
        startCallbackID = array.optString(1);
        setResolution(mode);
        //修改初始横屏时画面颠倒
        setOrientation();
        startWebview = pWebView;
        if (magicEngine != null && !inRecording){
            try {
                String rtmpURL = cameraView.resetOutputPath();
                if (rtmpUrlPattern.matcher(rtmpURL).matches()){
                    if (bOpenCamera) {
                        cameraView.setBackgroundColor(Color.TRANSPARENT);
                        CameraEngine.startPreview();
                    } else {
                        CameraEngine.stopPreview();
                        cameraView.setBackgroundColor(Color.BLACK);
                    }
                    magicEngine.startRecord();
                }else{
                    if (!startPreview) {
                        CameraEngine.stopPreview();
                        cameraView.setBackgroundColor(Color.BLACK);
                    }
                    try {
                        JSONObject result = new JSONObject("{code:1,message:'rtmp url invalable'}");
                        JSUtil.execCallback(pWebView,startCallbackID,result, JSUtil.ERROR,false);
                    }catch (Exception exc){
                    }


                }
            }catch (Exception exc){
              }
            inRecording = true;
        }
    }

    private void setOrientation() {

        if (CameraEngine.getCamera() == null) return;
        int rotation = initWebview.getActivity().getWindowManager().getDefaultDisplay().getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                CameraEngine.getCamera().setDisplayOrientation(0);
                break;
            case Surface.ROTATION_90:
                CameraEngine.getCamera().setDisplayOrientation(270);
                break;
            case Surface.ROTATION_180:
                CameraEngine.getCamera().setDisplayOrientation(180);
                break;
            case Surface.ROTATION_270:
                CameraEngine.getCamera().setDisplayOrientation(90);
                break;
        }
    }

    public void resume(IWebview pWebView, JSONArray array){
        if (magicEngine != null && !inRecording){
            try {
                magicEngine.startRecord();
            }catch (Exception exc){}
            inRecording = true;
        }
    }

    public void pause(IWebview pWebView, JSONArray array){
        if (magicEngine != null && inRecording){
            magicEngine.stopRecord();
            inRecording = false;
        }
    }

    private boolean stopPreview = false;
    public void stop(IWebview pWebView, JSONObject array){
        if (array != null) {
            stopPreview = array.optBoolean("preview",false);
        }
        if (magicEngine != null && inRecording){
            magicEngine.stopRecord();
            inRecording = false;
            onStoped = true;
        }
    }

    public void destory(String pusherID){
        curPusherID = pusherID;
        if (inRecording){
            onCloseing = true;
            magicEngine.stopRecord();
        }else{
            try{
                listener.onRtmpStopped(curPusherID);
            }catch (Exception e){

            }
        }
    }

    private int oritation = 0;
    private OrientationEventListener moel;

    @Override
    public void dispose() {
        super.dispose();
        stop(null, null);
//        LiveMediaFeatureImpl.removePusher(uuid);
        cameraView.stopRecording();
        destory(uuid);
        moel.disable();
    }

//    @Override
//    public boolean onDispose() {
//        stop(null, null);
//        cameraView.stopRecording();
//        return super.onDispose();
//    }


    public void setStatusListener(LivePusherStateListener statelistener){
        listener = statelistener;
    }

    public void switchCamera(IWebview pWebView, JSONArray array){
//        int cameraid = array.optInt(1);
//        if (cameraid > 0){
//            cameraid = 1;
//        }
        CameraEngine.switchCamera();
    }

    public void snapshot(final IWebview pWebView, JSONArray array){
        final String callBackID = array.optString(1);
        final IApp _app = pWebView.obtainFrameView().obtainApp();
        final String filepath = _app.obtainAppDocPath().concat(getImageFileName());

        File destFile = new File(filepath);
        File parentFile = destFile.getParentFile();
        if(!parentFile.exists()){
            parentFile.mkdirs();
        }

        CameraEngine.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                int errorCode = 1;
                String errorMessage = "无法获取截图";
                if (data != null){
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null){

                        saveImg(bitmap, filepath, pWebView, callBackID);
                        CameraEngine.startPreview();
                        return;
                    }
                    errorCode = 2;
                    errorMessage = "截图数据转换失败";
                }

                String errorString = String.format("{code:\"%\",message:\"\"}",errorCode, errorMessage);
                JSUtil.execCallback(pWebView, callBackID, errorString, JSUtil.ERROR, false, false);
                return;
            }
        });
    }

    private void saveImg(final Bitmap bitmap,final String filepath, final IWebview pWebView, final String callBackID) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap finalBitmap  = rotateBitmap(bitmap);
                String bitmapPath = saveBitmap(finalBitmap, filepath);
                String resultMessage = String.format("{width:\"%d\",height:\"%d\",tempImagePath:\"%s\"}", finalBitmap.getWidth(), finalBitmap.getHeight(),bitmapPath);
                JSUtil.execCallback(pWebView, callBackID, resultMessage, JSUtil.OK, true, false);
                bitmap.recycle();
                finalBitmap.recycle();
            }
        }).start();
    }

    public Bitmap rotateBitmap(Bitmap bitmap) {
        int id = CameraEngine.getCameraID();
        if (id == 0) {
            oritation -= 180;
        }
        if (oritation == 0 || null == bitmap) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(oritation, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (null != bitmap) {
            bitmap.recycle();
        }
        return bmp;
    }
    private static String getImageFileName() {
        Date date = new Date();
        return new SimpleDateFormat("/yyyy-MM-dd-HH-mm-ss-SSS").format(date).concat(".jpg");
    }

    private static String saveBitmap(Bitmap mBitmap, String savePath) {
        File filePic;
        try {
            filePic = new File(savePath);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return filePic.getAbsolutePath();
    }

    private void setVideoWithOption(JSONObject jsonObject){
        try{
            if(jsonObject.has("url")){
                rtmpULR = jsonObject.optString("url");
            }

            if (jsonObject.has("muted")){
                bSilence = jsonObject.optBoolean("mute");
            }

            if (jsonObject.has("enable-camera")){
                bOpenCamera = jsonObject.optBoolean("enable-camera");
            }

            if (jsonObject.has("auto-focus")){
                bautoFocus = jsonObject.optBoolean("auto-focus");
            }

            if (jsonObject.has("orientation")){
                String orientation = jsonObject.optString("orientation");
                if (orientation != null && orientation.equalsIgnoreCase("vertical")){
                    styOrientation = EScreenOrientation.VERTIAL;
                    CameraEngine.setRotation(90);
                }else{
                    styOrientation = EScreenOrientation.HORIZONTAL;
                    CameraEngine.setRotation(0);
                }
            }

            if (jsonObject.has("beauty")){
//                bBeauty = jsonObject.optBoolean("beauty");
            }

            if (jsonObject.has("whiteness")){
//                bWhiteness = jsonObject.optBoolean("whiteness");
            }

            if (jsonObject.has("aspect")){

            }

        }catch (Exception e){

        }
    }

    protected static final String EVENT_TEMPLATE = "window.__Media_Live__Push__.execCallback_LivePush('%s', %s,'%s');";
    protected static final String EVENT_RESULT_TEMPLATE = "{code:'%d',message:'%s'}";
    @Override
    public void onRtmpVideoStreaming(String msg) {
//        if (null != eventListenerWebview){
//            String _json = String.format(EVENT_TEMPLATE, "statechange", "", _message.toJSON());
//            eventListenerWebview.executeScript(_json);
//        }
    }

    @Override
    public void onRtmpAudioStreaming(String msg) {
//        if (null != eventListenerWebview){
//            String _json = String.format(EVENT_TEMPLATE, "statechange","",  _message.toJSON());
//            eventListenerWebview.executeScript(_json);
//        }
    }

    @Override
    public void onRtmpStopped(String msg) {
//        if (null != eventListenerWebview){
            String resultStr = String.format(EVENT_RESULT_TEMPLATE, 1001, msg);
//            String _json = String.format(EVENT_TEMPLATE,  "statechange",resultStr);
//            eventListenerWebview.executeScript(_json);
            statusEventListener("statechange",resultStr);
//        }


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                //已在主线程中，可以更新UI
                if (onStoped){
                    try {
                        if (!stopPreview) {
                            cameraView.setBackgroundColor(Color.BLACK);
                            CameraEngine.stopPreview();
                        }
                        onStoped = false;

                    }catch (Exception e){
                        int i = 0;
                    }
                }

                if (onCloseing){
                    try{
                        listener.onRtmpStopped(curPusherID);
                        CameraEngine.releaseCamera();
                        //mainView.removeView(cameraView);
                        cameraView.setBackgroundColor(Color.BLACK);
                        cameraView = null;
                    }catch (Exception e){

                    }
                }
            }
        });
    }

    @Override
    public void onRtmpConnecting(String msg) {
//        if (null != eventListenerWebview){
            String resultStr = String.format(EVENT_RESULT_TEMPLATE, 1001, msg);
//            String _json = String.format(EVENT_TEMPLATE, "statechange",resultStr);
//            eventListenerWebview.executeScript(_json);
            statusEventListener("statechange",resultStr);
//        }

    }

    @Override
    public void onRtmpConnected(String msg) {
//        if (null != eventListenerWebview){
            String resultStr = String.format(EVENT_RESULT_TEMPLATE, 1002, msg);
//            String _json = String.format(EVENT_TEMPLATE, "statechange", resultStr);
//            eventListenerWebview.executeScript(_json);
            statusEventListener("statechange",resultStr);

            if (null != startCallbackID){
                JSUtil.execCallback(startWebview, startCallbackID,"", JSUtil.OK, false);
                startCallbackID = null;
                startWebview = null;
            }
//        }
    }

    @Override
    public void onRtmpDisconnected(String msg) {
//        if (null != eventListenerWebview){
            String resultStr = String.format(EVENT_RESULT_TEMPLATE, 3004, msg);
//            String _json = String.format(EVENT_TEMPLATE,  "statechange", resultStr);
//            eventListenerWebview.executeScript(_json);
            statusEventListener("statechange",resultStr);
//        }
    }


    private  double curFps = 30;
    private  int curBitrate = nMinBitrate;
    private  long curTotalSize = 0;
    @Override
    public void onRtmpOutputFps(double fps) {
//        if (null != eventListenerWebview){
            curFps = fps;
            String message = String.format("{fps:%.0f,bitrate:%d,totalsize:%d}",curFps, curBitrate, curTotalSize);
            statusEventListener("netstatus",message);
//        }
    }

    private void statusEventListener(String event,String message) {
        if (eventCallBacks.containsKey(event)){
            HashMap<String,IWebview> callBacks = eventCallBacks.get(event);
            for (String key : callBacks.keySet()) {
                String _json = String.format(EVENT_TEMPLATE, event, message,key);
                callBacks.get(key).executeScript(_json);
            }
        }

    }

    @Override
    public void onRtmpDataInfo(int bitrate, long totalSize) {
//        if (null != eventListenerWebview){
            curBitrate = bitrate;
            curTotalSize = totalSize;

            String message = String.format("{fps:%.0f,bitrate:%d,totalsize:%d}",curFps, bitrate, totalSize);
            statusEventListener("netstatus",message);

//        }
    }

    @Override
    public void onNetWorkError(Exception e, int tag) {
//        if (null != eventListenerWebview){
            String resultStr = String.format(EVENT_RESULT_TEMPLATE, 1102, e.toString());
//            String _json = String.format(EVENT_TEMPLATE,  "error", resultStr);
//            eventListenerWebview.executeScript(_json);
            statusEventListener("error",resultStr);
//        }
    }

}
