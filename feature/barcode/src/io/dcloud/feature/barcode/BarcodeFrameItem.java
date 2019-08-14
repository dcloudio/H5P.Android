package io.dcloud.feature.barcode;

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
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;

import com.dcloud.zxing.BarcodeFormat;
import com.dcloud.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.CanvasHelper;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.MessageHandler;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.adapter.util.ViewRect;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.feature.barcode.camera.CameraManager;
import io.dcloud.feature.barcode.decoding.CaptureActivityHandler;
import io.dcloud.feature.barcode.decoding.IBarHandler;
import io.dcloud.feature.barcode.decoding.InactivityTimer;
import io.dcloud.feature.barcode.view.DetectorViewConfig;
import io.dcloud.feature.barcode.view.ViewfinderView;

class BarcodeFrameItem extends AdaFrameItem implements Callback,IBarHandler{
    public static final String TAG=BarcodeFrameItem.class.getSimpleName();
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	boolean playBeep = true;
	private static final float BEEP_VOLUME = 0.80f;
	boolean vibrate = true;
	SurfaceView surfaceView;
	Map<String, String> mCallbackIds = null;
	private Context mAct;
	private IWebview mWebViewImpl;
	private IWebview mContainerWebview;
	private IApp mAppHandler;
	/**是否处于待扫描状态*/
	private boolean mRunning = false;
	public String errorMsg = null;
	boolean mConserve = false;
	String mFilename = null;
	BarcodeProxy mProxy;
	static BarcodeFrameItem sBarcodeFrameItem = null;
	boolean noPermission = false;
	JSONArray mDivRectJson;
	JSONObject mStyles;
	JSONArray mFilters;
	private String mPosition = "static";
	public String mUuid;
	protected BarcodeFrameItem(BarcodeProxy pProxy,IWebview pWebViewImpl, String uuid, JSONArray divRect,JSONArray filters,JSONObject styles) {
		super(pWebViewImpl.getContext());
		sBarcodeFrameItem = this;
		mProxy = pProxy;
		mUuid = uuid;
		mCallbackIds = new HashMap<String, String>();
		mAct = pWebViewImpl.getContext();
		mWebViewImpl = pWebViewImpl;
		mContainerWebview = pWebViewImpl;
		mAppHandler = pWebViewImpl.obtainApp();
		mDivRectJson = divRect;
		mStyles = styles;
		mFilters = filters;
		final AbsoluteLayout.LayoutParams lp = getFrameLayoutParam(mDivRectJson, mStyles);
		final AbsoluteLayout mainView = new AbsoluteLayout(mAct){
			Paint paint = new Paint();
			@Override
			protected void onDraw(Canvas canvas) {
				super.onDraw(canvas);
				if(noPermission){
					paint.setColor(Color.WHITE);
					paint.setTextSize(CanvasHelper.dip2px(mAct,18));
					paint.setTextAlign(Paint.Align.CENTER);
					Paint.FontMetrics fontMetrics = paint.getFontMetrics();
					float top = fontMetrics.top;//为基线到字体上边框的距离,即上图中的top
					float bottom = fontMetrics.bottom;//为基线到字体下边框的距离,即上图中的bottom
					int baseLineY = (int) (lp.height / 2 - top/2 - bottom/2);//基线中间点的y轴计算公式
					int pl = lp.width / 2;
					canvas.drawText("未获取相机权限",pl,baseLineY,paint);
				}
			}
		};//会使得相机预览成像超出div区域,不会变形
//		RelativeLayout mainView = new RelativeLayout(mAct);//相对布局不会超出div区域
//		FrameLayout mainView = new FrameLayout(mAct);//相对布局不会超出div区域
		setMainView(mainView);
		if(styles!=null){
			initStyles(styles,mainView);
		}
		initDecodeFormats(filters);
	}

	public void appendToFrameView(AdaFrameView frameView) {
		if(obtainMainView() != null && obtainMainView().getParent() != null) {
			removeMapFrameItem(mContainerWebview);
		}
		mContainerWebview = frameView.obtainWebView();
		toFrameView();
	}

	public void removeMapFrameItem(IWebview pFrame) {
		if(mPosition.equals("absolute")) {
			pFrame.obtainFrameView().removeFrameItem(BarcodeFrameItem.this);
		} else {
			pFrame.removeFrameItem(BarcodeFrameItem.this);
		}
	}

	public void addCallBackId(String callBackId, String webUuid) {
		if(!mCallbackIds.containsKey(callBackId)) {
			mCallbackIds.put(callBackId, webUuid);
		}
	}

	public void toFrameView() {
		final AbsoluteLayout mainView = (AbsoluteLayout) obtainMainView();
		final AbsoluteLayout.LayoutParams lp = getFrameLayoutParam(mDivRectJson, mStyles);
		surfaceView = new SurfaceView(mAct);
		viewfinderView = new ViewfinderView(mAct,this);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(getActivity());
		PermissionUtil.usePermission(mContainerWebview.getActivity(), mContainerWebview.obtainApp().isStreamApp(),PermissionUtil.PMS_CAMERA , new PermissionUtil.StreamPermissionRequest(mContainerWebview.obtainApp()) {
			@Override
			public void onGranted(String streamPerName) {
				initCameraView(lp, mainView);
			}

			@Override
			public void onDenied(String streamPerName) {
				noPermission = true;
				MessageHandler.sendMessage(new MessageHandler.IMessages() {
					@Override
					public void execute(Object pArgs) {
						mainView.setBackgroundColor(Color.BLACK);
						mainView.invalidate();
					}
				},null);
			}
		});

		onResume(false);//启动预览，绘制探测区域
		saveOrientationState();//记录进来时候屏幕重力感应设置，退出时候进行还原
		isVerticalScreen = mAppHandler.isVerticalScreen();
		if(isVerticalScreen){//竖屏进来
			mAppHandler.setRequestedOrientation("portrait");
		}else{//横屏进来
			mAppHandler.setRequestedOrientation("landscape");
		}
		listenHideAndShow(mContainerWebview);
		if(mPosition.equals("absolute")) {
			mContainerWebview.obtainFrameView().addFrameItem(BarcodeFrameItem.this, lp);
		} else {
			mContainerWebview.addFrameItem(BarcodeFrameItem.this, lp);
		}
	}


	private AbsoluteLayout.LayoutParams getFrameLayoutParam(JSONArray arr, JSONObject styles) {
		float s = mContainerWebview.getScale();
		com.dcloud.android.widget.AbsoluteLayout.LayoutParams lp = null;
		if(arr.length() > 3) {
			Rect dvc = DetectorViewConfig.getInstance().gatherRect;
			dvc.left = PdrUtil.parseInt(JSONUtil.getString(arr, 0), 0);
			dvc.top = PdrUtil.parseInt(JSONUtil.getString(arr, 1), 0);
			dvc.right = dvc.left + PdrUtil.parseInt(JSONUtil.getString(arr, 2), 0);
			dvc.bottom = dvc.top + PdrUtil.parseInt(JSONUtil.getString(arr, 3), 0);

			dvc.left *= s;
			dvc.top *= s;
			dvc.right *= s;
			dvc.bottom *= s;
			if(dvc.width() != 0 && dvc.height() != 0){
				lp = (com.dcloud.android.widget.AbsoluteLayout.LayoutParams)LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
			}
		} else {
			if(styles == null){
				return lp;
			}
			AdaFrameItem frameView = (AdaFrameItem)mContainerWebview.obtainFrameView();
			ViewRect webParentViewRect = frameView.obtainFrameOptions();
			int l = PdrUtil.convertToScreenInt(JSONUtil.getString(styles, StringConst.JSON_KEY_LEFT), webParentViewRect.width, /*_wOptions.left*/0, s);
			int t = PdrUtil.convertToScreenInt(JSONUtil.getString(styles, StringConst.JSON_KEY_TOP), webParentViewRect.height, /*_wOptions.top*/0, s);
			int w = PdrUtil.convertToScreenInt(JSONUtil.getString(styles, StringConst.JSON_KEY_WIDTH), webParentViewRect.width, webParentViewRect.width, s);
			int h = PdrUtil.convertToScreenInt(JSONUtil.getString(styles, StringConst.JSON_KEY_HEIGHT), webParentViewRect.height, webParentViewRect.height, s);
			Rect dvc = DetectorViewConfig.getInstance().gatherRect;
			dvc.left = l;
			dvc.top = t;
			dvc.right = dvc.left + w;
			dvc.bottom = dvc.top + h;
			if(dvc.width() != 0 && dvc.height() != 0){
				lp = (com.dcloud.android.widget.AbsoluteLayout.LayoutParams)LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
			}
		}
		return lp;
	}

	private void initCameraView(AbsoluteLayout.LayoutParams lp, AbsoluteLayout mainView) {
		CameraManager.init(getActivity().getApplication());
		CameraManager.sScreenWidth = mAppHandler.getInt(IApp.SCREEN_WIDTH);
		CameraManager.sScreenAllHeight = mAppHandler.getInt(IApp.SCREEN_ALL_HEIGHT);
		
		Rect gatherRect = DetectorViewConfig.getInstance().gatherRect;
		
		//获取相机预览分辨率，不应超过采集区域(div)大小，避免超出采集区域，影响到webview其他内容展示
		Point camearResolution = CameraManager.getCR(gatherRect.width(),gatherRect.height());
		
		//计算surfaceView宽高，前提不能超过采集区域(div)大小
		int surfaceViewWidth,surfaceViewHeight;
		//公式 w/h = y/x
		surfaceViewWidth = lp.width;
		surfaceViewHeight = (int)(surfaceViewWidth * camearResolution.x / camearResolution.y);//获得分辨率的宽高比，根据此宽高比计算出适合的surfaceview布局
		int left = 0, top = 0;
		if(lp.y == 0 && surfaceViewHeight > lp.height){//如果div top为零，则继续使用宽基准
			top = lp.height - surfaceViewHeight ;
			DetectorViewConfig.detectorRectOffestTop = top;
		}else{//div top值不为0时
			if(surfaceViewHeight > lp.height){//超出div高，需要使用高基准，调整宽
				surfaceViewHeight = lp.height;
				surfaceViewWidth = surfaceViewHeight * camearResolution.y / camearResolution.x;
			}else{
				top = (lp.height - surfaceViewHeight)/2;
				DetectorViewConfig.detectorRectOffestTop = top;
			}
			
			if(lp.width - surfaceViewWidth > 0){
				left = (lp.width - surfaceViewWidth)/2;
				DetectorViewConfig.detectorRectOffestLeft = left;
			}
		}
		
		surfaceView.setClickable(false);
		mainView.addView(surfaceView,new AbsoluteLayout.LayoutParams(surfaceViewWidth,surfaceViewHeight,left,top));
		DetectorViewConfig.getInstance().initSurfaceViewRect(left,top,surfaceViewWidth,surfaceViewHeight);
		mainView.addView(viewfinderView);
	}

	private void initStyles(JSONObject obj,View v){//给控件设置样式
		mStyles = obj;
		DetectorViewConfig.laserColor = DetectorViewConfig.F_LASER_COLOR;
		DetectorViewConfig.cornerColor = DetectorViewConfig.F_CORNER_COLOR;
		if(obj.has("position")) {
			mPosition = obj.optString("position");
		}
		if(!TextUtils.isEmpty(obj.optString("scanbarColor"))){
			int scanbarColor = PdrUtil.stringToColor(obj.optString("scanbarColor"));
			scanbarColor = (scanbarColor!=-1) ? scanbarColor : DetectorViewConfig.laserColor;
			DetectorViewConfig.laserColor=scanbarColor;
		}
		if(!TextUtils.isEmpty(obj.optString("frameColor"))){
			int frameColor = PdrUtil.stringToColor(obj.optString("frameColor"));
			frameColor = (frameColor!=-1) ? frameColor : DetectorViewConfig.laserColor;
			DetectorViewConfig.cornerColor=frameColor;
		}
		if(!TextUtils.isEmpty(obj.optString("background"))){
			int background = PdrUtil.stringToColor(obj.optString("background"));
			background = (background!=-1) ? background : DetectorViewConfig.laserColor;
			v.setBackgroundColor(background);
		}	
	}

	public void upateStyles(JSONObject styles) {
		JSONUtil.combinJSONObject(mStyles, styles);
		if(styles.has("top") || styles.has("left") || styles.has("width") || styles.has("height") || styles.has("position")) {
			ViewGroup.LayoutParams _lp = getFrameLayoutParam(mDivRectJson, mStyles);
			if(styles.has("position")) {
				String position = styles.optString("position");
				if(!position.equals(mPosition)) {
					if(mPosition.equals("absolute")) {
						mContainerWebview.obtainFrameView().removeFrameItem(BarcodeFrameItem.this);
						mContainerWebview.addFrameItem(BarcodeFrameItem.this, _lp);
					} else {
						mContainerWebview.removeFrameItem(BarcodeFrameItem.this);
						mContainerWebview.obtainFrameView().addFrameItem(BarcodeFrameItem.this, _lp);
					}
					mPosition = position;
				}
			} else {
				obtainMainView().setLayoutParams(_lp);
			}
		}
	}

	private void listenHideAndShow(IWebview pWebViewImpl){
		pWebViewImpl.obtainFrameView().addFrameViewListener(new IEventCallback() {
			@Override
			public Object onCallBack(String pEventType, Object pArgs) {
				if(PdrUtil.isEquals(pEventType, StringConst.EVENTS_WEBVIEW_HIDE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE)){
					onPause();
				}else if(PdrUtil.isEquals(pEventType, StringConst.EVENTS_SHOW_ANIMATION_END)){
					onResume(true);
				}
				return null;
			}
		});
	}
	@Override
	public void autoFocus() {
		handler.autoFocus();
	}
	@Override
	public void dispose() {
		super.dispose();
		Logger.d("Barcode","dispose");
		onPause();
		DetectorViewConfig.clearData();
		mProxy.mBarcodeView = null;
		surfaceView = null;
		if(lastBitmap != null && !lastBitmap.isRecycled()){
			lastBitmap.recycle();
			lastBitmap = null;
		}
		CameraManager.get().clearLastBitmapData();
		resumeOrientationState();
		BarcodeProxyMgr.getBarcodeProxyMgr().removeBarcodeProxy(mUuid);
	}
	/**是否是竖屏*/
	boolean isVerticalScreen = true;
	int mOrientationState;
	/** 保存当前设置的重力感应状态*/
	private void saveOrientationState(){
		mOrientationState = mAppHandler.getRequestedOrientation();
	}
	/**恢复进入扫描页面前的重力感应状态*/
	private void resumeOrientationState(){
		mAppHandler.setRequestedOrientation(mOrientationState);
	}
	protected void onResume(boolean isSysEvent){
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if(lastBitmap!=null&&isCancelScan&&isSysEvent){
			surfaceView.setBackground(new BitmapDrawable(mAct.getResources(),lastBitmap));
		}
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
//		decodeFormats = null;
//		characterSet = null;
		AudioManager audioService = (AudioManager) mAct.getSystemService(Activity.AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		if(isSysEvent){//系统事件过来的通知
			if(mRunning){//系统时间过来的时候处于扫描状态
				mRunning = false;//认为设置处于非扫描状态，因为onpause事件可能引起扫描状态改变
				start();
			}
		}
	}

    @Override
    public void onPopFromStack(boolean autoPop) {
        super.onPopFromStack(autoPop);
        if (autoPop)
        onPause();
    }

    @Override
    public void onPushToStack(boolean autoPush) {
        super.onPushToStack(autoPush);
        if (autoPush)
        onResume(false);
    }

    protected void onPause(){
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		if(!noPermission) {
			CameraManager.get().closeDriver();
		}
		boolean t = mRunning;//保存取消前的扫描状态
		cancel();
		mRunning = t;//恢复扫描状态
	}
	
	protected void start() {
		if(!mRunning){
			getViewfinderView().startUpdateScreenTimer();
			if (handler != null) {
				handler.restartPreviewAndDecode();
			}else{
				onResume(false);
			}
			if(isCancelScan){
				surfaceView.setBackground(null);
				if(lastBitmap != null && !lastBitmap.isRecycled()){
					lastBitmap.recycle();
					lastBitmap = null;
				}
				CameraManager.get().clearLastBitmapData();
				surfaceView.postInvalidate();
				initCamera(surfaceView.getHolder());
			}
			mRunning = true;
			isCancelScan=false;
		}
	}

	public void setFlash(boolean enable){
		CameraManager.get().setFlashlight(enable);
	}
	protected void cancel() {
		if(mRunning){
			if (handler != null) {
				handler.stopDecode();
			}
			getViewfinderView().stopUpdateScreenTimer();
			mRunning = false;
		}
	}
	private boolean isCancelScan=false;
	private Bitmap lastBitmap=null;
	protected void cancel_scan(){
		if(mRunning){
			if (handler != null) {
				handler.quitSynchronously();
				handler = null;
			}
			getViewfinderView().stopUpdateScreenTimer();
			CameraManager.get().removeAutoFocus();
			CameraManager.get().stopPreview();
			byte[] lastBitmapData=CameraManager.get().getLastBitmapData();
			Camera camera=CameraManager.get().getCameraHandler();
			if(lastBitmapData!=null&&camera!=null){
				lastBitmap=byte2bitmap(lastBitmapData,camera);
			}
			CameraManager.get().closeDriver();
			mRunning = false;
			isCancelScan=true;
		}
	}
	protected void close_scan(){
		dispose();
		setMainView(null);
		System.gc();
	}
	protected void onDestroy() {
		inactivityTimer.shutdown();
		hasSurface = false;
		decodeFormats = null;
		characterSet = null;
		mCallbackIds.clear();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			errorMsg = ioe.getMessage();
			return;
		} catch (RuntimeException e) {
			errorMsg = e.getMessage();
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
			if (mRunning && handler != null) {//可能start的调用早于此处运行
				handler.restartPreviewAndDecode();
			}
		}else{
			handler.resume();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			if(!isCancelScan){
				try {
					initCamera(holder);
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}
	@Override
	public boolean isRunning() {
		return mRunning;
	}
	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}
	public void handleDecode(Result obj, Bitmap barcode) {
		inactivityTimer.onActivity();
		 playBeepSoundAndVibrate();
		 boolean saveSuc = false;
		 if(mConserve){
			 saveSuc = PdrUtil.saveBitmapToFile(barcode, mFilename);
//			 PdrUtil.showNativeAlert(mWebViewImpl.getContext(), "获取的扫描信息", barcode);
		 }
		 int num = convertTypestrToNum(obj.getBarcodeFormat());
		 String json = null;
		 if(saveSuc){
			 String message = "{type:%d,message:%s,file:'%s'}";
			 String doc  = mWebViewImpl.obtainFrameView().obtainApp().obtainAppDocPath();
			 Logger.d("doc:" + doc);
			 if(mFilename.startsWith(doc)){
				 mFilename = "_doc" + mFilename.substring(doc.length()-1);
			 }
			 String relPath =  mWebViewImpl.obtainFrameView().obtainApp().convert2RelPath(mFilename);
			 Logger.d("Filename:" + mFilename + ";relPath:" + relPath);
			 json = StringUtil.format(message, num,JSONUtil.toJSONableString(obj.getText()),relPath);
		 }else{
			 String message = "{type:%d,message:%s}";
			 json = StringUtil.format(message, num,JSONUtil.toJSONableString(obj.getText()));
		 }
		 runJsCallBack(json, JSUtil.OK, true, true);
		 cancel();//start一次只能有一次结果，所以成功之后需要停止
	}

	private void initBeepSound() {
		if (mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);
			
			try {
				AssetFileDescriptor file = mAct.getResources().getAssets().openFd(StringConst.RES_BEEP);
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

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			try {
				Vibrator vibrator = (Vibrator) mAct.getSystemService(mAct.VIBRATOR_SERVICE);
				vibrator.vibrate(VIBRATE_DURATION);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};
	
	private int convertTypestrToNum(BarcodeFormat format){
		if(format == BarcodeFormat.QR_CODE){
			return QR;
		}else if(format == BarcodeFormat.EAN_13){
			return EAN13;
		}else if(format == BarcodeFormat.EAN_8){
			return EAN8;
		}else if(format == BarcodeFormat.AZTEC){
			return AZTEC;
		}else if(format == BarcodeFormat.DATA_MATRIX){
			return DATAMATRIX;
		}else if(format == BarcodeFormat.UPC_A){
			return UPCA;
		}else if(format == BarcodeFormat.UPC_E){
			return UPCE;
		}else if(format == BarcodeFormat.CODABAR){
			return CODABAR;
		}else if(format == BarcodeFormat.CODE_39){
			return CODE39;
		}else if(format == BarcodeFormat.CODE_93){
			return CODE93;
		}else if(format == BarcodeFormat.CODE_128){
			return CODE128;
		}else if(format == BarcodeFormat.ITF){
			return ITF;
		}else if(format == BarcodeFormat.MAXICODE){
			return MAXICODE;
		}else if(format == BarcodeFormat.PDF_417){
			return PDF417;
		}else if(format == BarcodeFormat.RSS_14){
			return RSS14;
		}else if(format == BarcodeFormat.RSS_EXPANDED){
			return RSSEXPANDED;
		}
		return UNKOWN;
	}
	
	
	
	private void initDecodeFormats(JSONArray filters){
		decodeFormats = new Vector<BarcodeFormat>();
		if(filters == null || filters.length() == 0){//默认支持
			decodeFormats.add(BarcodeFormat.EAN_13);
			decodeFormats.add(BarcodeFormat.EAN_8);
			decodeFormats.add(BarcodeFormat.QR_CODE);
		}else{
			int size = filters.length();
			for(int i = 0; i < size; i++){
				int filter = -1;
				try {
					filter = filters.getInt(i);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if(filter != -1){
					decodeFormats.add(convertNumToBarcodeFormat(filter));
				}
			}
		}
	}

	private BarcodeFormat convertNumToBarcodeFormat(int num){
		BarcodeFormat _ret = null;
		switch (num) {
		case QR :{
			_ret = BarcodeFormat.QR_CODE;
			break;
		}
	    case EAN13 :{
			_ret = BarcodeFormat.EAN_13;
			break;
		}
	    case EAN8 :{
			_ret = BarcodeFormat.EAN_8;
			break;
		}
	    case AZTEC :{
			_ret = BarcodeFormat.AZTEC;
			break;
		}
	    case DATAMATRIX :{
			_ret = BarcodeFormat.DATA_MATRIX;
			break;
		}
	    case UPCA :{
			_ret = BarcodeFormat.UPC_A;
			break;
		}
	    case UPCE :{
			_ret = BarcodeFormat.UPC_E;
			break;
		}
	    case CODABAR :{
			_ret = BarcodeFormat.CODABAR;
			break;
		}
	    case CODE39 :{
			_ret = BarcodeFormat.CODE_39;
			break;
		}
	    case CODE93 :{
			_ret = BarcodeFormat.CODE_93;
			break;
		}
	    case CODE128 :{
			_ret = BarcodeFormat.CODE_128;
			break;
		}
	    case ITF :{
			_ret = BarcodeFormat.ITF;
			break;
		}
	    case MAXICODE :{
			_ret = BarcodeFormat.MAXICODE;
			break;
		}
	    case PDF417 :{
			_ret = BarcodeFormat.PDF_417;
			break;
		}
	    case RSS14 :{
			_ret = BarcodeFormat.RSS_14;
			break;
		}
	    case RSSEXPANDED :{
			_ret = BarcodeFormat.RSS_EXPANDED;
			break;
		}
		}
		return _ret;
	}
	
	static final int UNKOWN      = -1000;
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
    
	private Bitmap byte2bitmap(byte[] data,Camera mCamera){
		Bitmap bmp=null;
		try{
			Size size = mCamera.getParameters().getPreviewSize();
			YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width,size.height, null);
			if (image != null) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				image.compressToJpeg(new Rect(0, 0, size.width, size.height),80, stream);
				bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
				stream.close();
				Matrix m=new Matrix();
				m.postRotate(90);
				bmp=Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return bmp;
	}

	public JSONObject getJsBarcode() {
		JSONObject data = null;
		if(obtainMainView() != null) {
			data = new JSONObject();
			try {
				data.put("uuid", mUuid);
				data.put("filters", mFilters);
				data.put("options", mStyles);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	public void runJsCallBack(String msg, int code, boolean isJson, boolean pKeepCallBack) {
		for(String callbackId : mCallbackIds.keySet()) {
			String webUuid = mCallbackIds.get(callbackId);
			IWebview webview = BarcodeProxyMgr.getBarcodeProxyMgr().findWebviewByUuid(mWebViewImpl, webUuid);
			if(webview != null) {
				JSUtil.execCallback(webview, callbackId, msg, code, isJson, pKeepCallBack);
			}
		}
	}
}
