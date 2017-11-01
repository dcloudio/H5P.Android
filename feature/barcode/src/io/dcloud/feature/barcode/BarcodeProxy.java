package io.dcloud.feature.barcode;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.ui.AdaFrameItem.LayoutParamsUtil;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.ViewRect;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.barcode.decoding.CaptureActivityHandler;
import io.dcloud.feature.barcode.view.DetectorViewConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.text.TextUtils;

import com.dcloud.android.widget.AbsoluteLayout;
import com.dcloud.zxing.Result;
//import android.content.Context;

public class BarcodeProxy implements ISysEventListener {
	
	public static boolean save =false;
	public static Context context = null;
	BarcodeFrameItem mBarcodeView = null;
	boolean mIsRegisetedSysEvent = false;
	void execute(IWebview pWebViewImpl, String pActionName,String[] pJsArgs){
//		IApp _app = pWebViewImpl.obtainApp();
//		if(_app.checkSelfPermission("android.permission.CAMERA") != 2){
//			_app.registerSysEventListener(new io.dcloud.common.DHInterface.ISysEventListener(){
//				public boolean onExecute(io.dcloud.common.DHInterface.ISysEventListener.SysEventType pEventType,Object pArgs){
//					if(io.dcloud.common.DHInterface.ISysEventListener.SysEventType.onRequestPermissionsResult == pEventType){
//					}
//					return true;
//				}
//			}, io.dcloud.common.DHInterface.ISysEventListener.SysEventType.onRequestPermissionsResult);
//			_app.requestPermissions(new String[]{"android.permission.CAMERA"},1000);
//		}
		if("start".equals(pActionName)){
			if(!PdrUtil.isEmpty(mBarcodeView.errorMsg)){
				String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BARCODE_ERROR,mBarcodeView.errorMsg);
				JSUtil.execCallback(pWebViewImpl, mBarcodeView.mCallbackId,msg , JSUtil.ERROR, true, true);
			}else{
				boolean _conserve = false;
				JSONObject args = JSONUtil.createJSONObject(pJsArgs[0]);
				if(args != null){
					_conserve = PdrUtil.parseBoolean(JSONUtil.getString(args, "conserve"), _conserve, false);
					if(_conserve){
						String _filename = PdrUtil.getDefaultPrivateDocPath(JSONUtil.getString(args, "filename"), "png");
						mBarcodeView.mFilename = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),_filename);
						Logger.d("Filename:" + mBarcodeView.mFilename);
					}
					mBarcodeView.vibrate = PdrUtil.parseBoolean(JSONUtil.getString(args, "vibrate"), true, false);
					mBarcodeView.playBeep = !TextUtils.equals(JSONUtil.getString(args, "sound"), "none");
				}
				mBarcodeView.mConserve = _conserve;
				mBarcodeView.start();
			}
		}else if("cancel".equals(pActionName)){
			mBarcodeView.cancel_scan();
		}else if("setFlash".equals(pActionName)){
			mBarcodeView.setFlash(Boolean.parseBoolean(pJsArgs[0]));
//			save = true;
//			context = pWebViewImpl.getContext();
		}else if("Barcode".equals(pActionName)){
			if(!mIsRegisetedSysEvent){
				IApp app = pWebViewImpl.obtainFrameView().obtainApp();
				app.registerSysEventListener(this, SysEventType.onPause);
				app.registerSysEventListener(this, SysEventType.onResume);
				mIsRegisetedSysEvent = true;
			}
			//解析html控件位置大小
			JSONArray arr = JSONUtil.createJSONArray(pJsArgs[1]);
			Rect dvc = DetectorViewConfig.getInstance().gatherRect;
			dvc.left = PdrUtil.parseInt(JSONUtil.getString(arr, 0), 0);
			dvc.top = PdrUtil.parseInt(JSONUtil.getString(arr, 1), 0);
			dvc.right = dvc.left + PdrUtil.parseInt(JSONUtil.getString(arr, 2), 0);
			dvc.bottom = dvc.top + PdrUtil.parseInt(JSONUtil.getString(arr, 3), 0);
			
			float s = pWebViewImpl.getScale();
			dvc.left *= s;
			dvc.top *= s;
			dvc.right *= s; 
			dvc.bottom *= s;
			int[] frameLocationOnScreen = new int[2];
			pWebViewImpl.obtainWebview().getLocationOnScreen(frameLocationOnScreen);
			dvc.left += frameLocationOnScreen[0];
			dvc.top += frameLocationOnScreen[1];
			if(dvc.width() != 0 && dvc.height() != 0){
				//创建barcode系统控件
				JSONArray filters = null;
				if(!PdrUtil.isEmpty(pJsArgs[2])){
					filters = JSONUtil.createJSONArray(pJsArgs[2]);//获取支持扫描
				}
				JSONObject styles = null;
				if(!PdrUtil.isEmpty(pJsArgs[3])){
					styles = JSONUtil.createJSONObject(pJsArgs[3]);
				}
				AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams)LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
				mBarcodeView = new BarcodeFrameItem(this,pWebViewImpl,lp,filters,styles);
				AdaFrameItem frameView = (AdaFrameItem)pWebViewImpl.obtainFrameView();
				ViewRect frameViewRect = frameView.obtainFrameOptions();
				mBarcodeView.updateViewRect((AdaFrameItem)pWebViewImpl.obtainFrameView(), new int[]{dvc.left,dvc.top,dvc.width(),dvc.height()}, new int[]{frameViewRect.width,frameViewRect.height});
				mBarcodeView.mCallbackId = pJsArgs[0];
				pWebViewImpl.obtainFrameView().addFrameItem(mBarcodeView,lp);
//				pWebViewImpl.addFrameItem(mBarcodeView,lp);
			}else{
				Logger.e("Barcode","LayoutParams l=" + dvc.left + ";t=" + dvc.top + ";r=" + dvc.right + ";b=" + dvc.bottom);
				//创建失败
			}
		}else if("scan".equals(pActionName)){
			String callbackId = pJsArgs[0];
			IApp app = pWebViewImpl.obtainFrameView().obtainApp();
			String path = app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pJsArgs[1]);
			Bitmap map = BitmapFactory.decodeFile(path);
			Result result = CaptureActivityHandler.decode(map);
			if(result != null){
				String message = "{type:'%s',message:'%s',file:'%s'}";
				 message = String.format(message, result.getBarcodeFormat().toString(),JSONUtil.toJSONableString(result.getText()),path);
				 JSUtil.execCallback(pWebViewImpl, callbackId, message, JSUtil.OK, true, false);
			}else{
				String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BARCODE_ERROR,DOMException.MSG_BARCODE);
				JSUtil.execCallback(pWebViewImpl, callbackId,msg , JSUtil.ERROR, true, false);
			}
		}else if("close".equals(pActionName)){
			mBarcodeView.close_scan();
		}
	}
	
	protected void onDestroy() {
		if(mBarcodeView != null){
			mBarcodeView.onDestroy();
			mBarcodeView = null;
		}
		mIsRegisetedSysEvent = false;
	}
	
	protected void onPause() {
		if(mBarcodeView != null)
			mBarcodeView.onPause();
	}
	protected void onResume() {
		if(mBarcodeView != null)
			mBarcodeView.onResume(true);
	}
	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {
		if(pEventType == SysEventType.onResume){
			onResume();
		}else if(pEventType == SysEventType.onPause){
			onPause();
		}
		return false;
	}

}