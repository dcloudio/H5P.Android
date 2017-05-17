package io.dcloud.feature.barcode.view;

import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.feature.barcode.camera.CameraManager;
import io.dcloud.feature.barcode.decoding.CaptureActivityHandler;
import io.dcloud.feature.barcode.decoding.IBarHandler;
import io.dcloud.feature.barcode.decoding.InactivityTimer;

import java.io.IOException;
import java.util.Vector;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.AbsoluteLayout;

import com.dcloud.zxing.BarcodeFormat;
import com.dcloud.zxing.Result;

public class BarcodeView extends AbsoluteLayout implements Callback, IBarHandler,IReflectAble {
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	SurfaceView surfaceView;
	String mCallbackId = null;
	/** 是否处于待扫描状态 */
	private boolean mRunning = false;
	public String errorMsg = null;
	boolean mConserve = false;
	String mFilename = null;
	Activity mContext;

	public BarcodeView(Activity pProxy, Rect lp/* ,JSONArray filters */) {
		super(pProxy);
		mContext = pProxy;
		surfaceView = new SurfaceView(pProxy);
		viewfinderView = new ViewfinderView(pProxy, this);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(pProxy);
		CameraManager.init(pProxy.getApplication());
		CameraManager.sScreenWidth = pProxy.getResources().getDisplayMetrics().widthPixels;
		CameraManager.sScreenAllHeight = pProxy.getResources().getDisplayMetrics().heightPixels;

		Rect gatherRect =  DetectorViewConfig.getInstance().gatherRect;

		// 获取相机预览分辨率，不应超过采集区域(div)大小，避免超出采集区域，影响到webview其他内容展示
		Point camearResolution = CameraManager.getCR(gatherRect.width(),gatherRect.height());
		if (camearResolution == null) {
			return;
		}
		// 计算surfaceView宽高，前提不能超过采集区域(div)大小
		int surfaceViewWidth, surfaceViewHeight;
		// 公式 w/h = y/x
		surfaceViewWidth = lp.width();
		surfaceViewHeight = (int) (surfaceViewWidth * camearResolution.x / camearResolution.y);// 获得分辨率的宽高比，根据此宽高比计算出适合的surfaceview布局
		int left = 0, top = 0;
		if (lp.top == 0 && surfaceViewHeight > lp.height()) {// 如果div
																// top为零，则继续使用宽基准
			top = lp.height() - surfaceViewHeight;
			DetectorViewConfig.detectorRectOffestTop = top;
		} else {// div top值不为0时
			if (surfaceViewHeight > lp.height()) {// 超出div高，需要使用高基准，调整宽
				surfaceViewHeight = lp.height();
				surfaceViewWidth = surfaceViewHeight * camearResolution.y
						/ camearResolution.x;
			} else {
				top = (lp.height() - surfaceViewHeight) / 2;
				DetectorViewConfig.detectorRectOffestTop = top;
			}
 
			if (lp.width() - surfaceViewWidth > 0) {
				left = (lp.width() - surfaceViewWidth) / 2;
				DetectorViewConfig.detectorRectOffestLeft = left;
			}
		}

		surfaceView.setClickable(false);
		addView(surfaceView, new AbsoluteLayout.LayoutParams(surfaceViewWidth,surfaceViewHeight, left, top));
		DetectorViewConfig.getInstance().initSurfaceViewRect(left, top,surfaceViewWidth, surfaceViewHeight);
		addView(viewfinderView);
		initDecodeFormats();
		onResume(false);// 启动预览，绘制探测区域

	}

	@Override
	public void autoFocus() {
		handler.autoFocus();
	}

	public void dispose() {
		onPause();
		DetectorViewConfig.clearData();
		surfaceView = null;
	}

	public void onResume(boolean isSysEvent) {
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		playBeep = true;
		AudioManager audioService = (AudioManager) mContext
				.getSystemService(Activity.AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
		if (isSysEvent) {// 系统事件过来的通知
			if (mRunning) {// 系统时间过来的时候处于扫描状态
				mRunning = false;// 认为设置处于非扫描状态，因为onpause事件可能引起扫描状态改变
				start();
			}
		}
	}

	public void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
		boolean t = mRunning;// 保存取消前的扫描状态
		cancel();
		mRunning = t;// 恢复扫描状态
	}

	public void start() {
		if (!mRunning) {
			getViewfinderView().startUpdateScreenTimer();
			if (handler != null) {
				handler.restartPreviewAndDecode();
			}
			mRunning = true;
		}
	}

	public void setFlash(boolean enable) {
		CameraManager.get().setFlashlight(enable);
	}

	public void cancel() {
		if (mRunning) {
			if (handler != null) {
				handler.stopDecode();
			}
			getViewfinderView().stopUpdateScreenTimer();
			mRunning = false;
		}
	}

	public void onDestroy() {
		inactivityTimer.shutdown();
		hasSurface = false;
		decodeFormats = null;
		characterSet = null;
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
			handler = new CaptureActivityHandler(this, decodeFormats,
					characterSet);
			if (mRunning && handler != null) {// 可能start的调用早于此处运行
				handler.restartPreviewAndDecode();
			}
		} else {
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
			initCamera(holder);
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
		if (mScanListener != null) {
			mScanListener.onCompleted(obj.getText(), barcode);
		}
	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			mContext.setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			try {
				AssetFileDescriptor file = mContext.getResources().getAssets()
						.openFd("res/dcloud_beep.ogg");
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
			Vibrator vibrator = (Vibrator) mContext
					.getSystemService(mContext.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
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

	ScanListener mScanListener = null;

	public void setScanListener(ScanListener listener) {
		mScanListener = listener;
	}

	public ScanListener getScanListener() {
		return mScanListener;
	}

	public static interface ScanListener extends IReflectAble {
		void onCompleted(String str, Bitmap bitmap);
	}

	private void initDecodeFormats(/* JSONArray filters */) {
		decodeFormats = new Vector<BarcodeFormat>();
		decodeFormats.add(BarcodeFormat.EAN_13);
		decodeFormats.add(BarcodeFormat.EAN_8);
		decodeFormats.add(BarcodeFormat.QR_CODE);
	}
	
	public Result decode(Bitmap b) {
		return CaptureActivityHandler.decode(b);
	}

}
