package io.dcloud.js.camera;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;

import java.io.File;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.util.ContentUriUtil;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.camera.CameraManager.CameraOption;

/**
 * 摄像头特征实现类
 *
 * @version 1.0
 * @author yanglei Email:yanglei@dcloud.io
 * @Date 2013-1-9 下午01:43:51 created.
 * 
 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-1-9 下午01:43:51
 */
public class CameraFeatureImpl implements IFeature{

	AbsMgr mFeature = null;

	@Override
	public String execute(final IWebview pWebViewImpl, String pActionName,
			final String[] pJsArgs) {
		String _result = null;
		final IApp _app = pWebViewImpl.obtainFrameView().obtainApp();
		final String pCallbackId = pJsArgs[0];
        //Android 7.0 FileUriExposedException 解决
        if (Build.VERSION.SDK_INT >= 24) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
		if(pActionName.equals("captureImage")){
				 PermissionUtil.usePermission(_app.getActivity(), _app.isStreamApp(),PermissionUtil.PMS_CAMERA , new PermissionUtil.StreamPermissionRequest(_app) {
					 @Override
					 public void onGranted(String streamPerName) {
						 try {
							 final CameraOption _option = CameraManager.parseOption(pJsArgs[1],true);
							 final String filepath = _app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),_option.getFilename());
							 if(JSUtil.checkOperateDirErrorAndCallback(pWebViewImpl, pCallbackId, filepath)){//是否操作_www目录了
								 String callMsg = DOMException.toJSON(DOMException.CODE_IO_ERROR,DOMException.MSG_IO_ERROR);
								 JSUtil.execCallback(pWebViewImpl, pCallbackId, callMsg, JSUtil.ERROR, true, false);
								 return ;
							 }
							 File destFile = new File(filepath);
							 File parentFile = destFile.getParentFile();
							 if(!parentFile.exists()){
								 parentFile.mkdirs();
							 }
							 _app.registerSysEventListener(new ISysEventListener(){
								 @Override
								 public boolean onExecute(SysEventType pEventType, Object pArgs) {
									 Object[] _args = (Object[])pArgs;
									 int requestCode = (Integer)_args[0];
									 int resultCode = (Integer)_args[1];
									 if(pEventType == SysEventType.onActivityResult){
										 if (requestCode == CameraManager.IMAGE_CAPTURE) {
											 if (resultCode == Activity.RESULT_OK) {
												 String backPath = _app.convert2RelPath(filepath);
												 JSUtil.execCallback(pWebViewImpl, pCallbackId, backPath, JSUtil.OK, false, false);
											 }else{
												 String callMsg = DOMException.toJSON(DOMException.CODE_CAMERA_ERROR,"resultCode is wrong");
												 JSUtil.execCallback(pWebViewImpl, pCallbackId, callMsg, JSUtil.ERROR, true, false);

											 }
											 _app.unregisterSysEventListener(this, pEventType);
										 }
									 }
									 return false;
								 }
							 }, SysEventType.onActivityResult);
							 Intent _intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							 Uri _uri = Uri.fromFile(destFile);
							 _intent.putExtra(MediaStore.EXTRA_OUTPUT, _uri);
							 pWebViewImpl.getActivity().startActivityForResult(_intent, CameraManager.IMAGE_CAPTURE);
						 } catch (Exception e) {
							 String callMsg = DOMException.toJSON(DOMException.CODE_CAMERA_ERROR,e.getMessage());
							 JSUtil.execCallback(pWebViewImpl, pCallbackId, callMsg, JSUtil.ERROR, true, false);
						 }
					 }

					 @Override
					 public void onDenied(String streamPerName) {
						 String callMsg = DOMException.toJSON(DOMException.CODE_CAMERA_ERROR,DOMException.MSG_NO_PERMISSION);
						 JSUtil.execCallback(pWebViewImpl, pCallbackId, callMsg, JSUtil.ERROR, true, false);
					 }
				 });

		}else if(pActionName.equals("startVideoCapture")){
			PermissionUtil.usePermission(_app.getActivity(), _app.isStreamApp(),PermissionUtil.PMS_CAMERA , new PermissionUtil.StreamPermissionRequest(_app) {
				@Override
				public void onGranted(String streamPerName) {
					try{
						CameraOption _option = CameraManager.parseOption(pJsArgs[1],false);
						final String savePath = _app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), _option.getFilename());
						if(JSUtil.checkOperateDirErrorAndCallback(pWebViewImpl, pCallbackId, savePath)){//是否操作_www目录了
							String callMsg = DOMException.toJSON(DOMException.CODE_IO_ERROR,DOMException.MSG_IO_ERROR);
							JSUtil.execCallback(pWebViewImpl, pCallbackId, callMsg, JSUtil.ERROR, true, false);
							return ;
						}
						File destFile = new File(savePath);
						File parentFile = destFile.getParentFile();
						if(!parentFile.exists()){
							parentFile.mkdirs();
						}
						_app.registerSysEventListener(new ISysEventListener(){
							@Override
							public boolean onExecute(SysEventType pEventType, Object pArgs) {
								Object[] _args = (Object[])pArgs;
								int requestCode = (Integer)_args[0];
								int resultCode = (Integer)_args[1];
								if(pEventType == SysEventType.onActivityResult){
									if (requestCode == CameraManager.VIDEO_CAPTURE) {
										if (resultCode == Activity.RESULT_OK) {
											File file = new File(savePath);
											if(!file.exists()){
												String filePath = ContentUriUtil.getImageAbsolutePath(_app.getActivity(), ((Intent) _args[2]).getData());
												DHFile.copyFile(filePath,savePath);
											}
											String backPath = _app.convert2RelPath(savePath);
											JSUtil.execCallback(pWebViewImpl, pCallbackId, backPath, JSUtil.OK, false, false);
										}else{
											JSUtil.execCallback(pWebViewImpl, pCallbackId, null, JSUtil.ERROR, false, false);
										}
										_app.unregisterSysEventListener(this, pEventType);
									}
								}
								return false;
							}
						}, SysEventType.onActivityResult);
						Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
						Uri _uri = Uri.fromFile(destFile);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, _uri);
						pWebViewImpl.getActivity().startActivityForResult(intent, CameraManager.VIDEO_CAPTURE);
					}catch(Exception e){
						String msg = DOMException.toJSON(DOMException.CODE_CAMERA_ERROR,e.getMessage());
						JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
					}
				}

				@Override
				public void onDenied(String streamPerName) {
					String callMsg = DOMException.toJSON(DOMException.CODE_CAMERA_ERROR,DOMException.MSG_NO_PERMISSION);
					JSUtil.execCallback(pWebViewImpl, pCallbackId, callMsg, JSUtil.ERROR, true, false);
				}
			});

			
		}else if(pActionName.equals("getCamera")){
			int _index = PdrUtil.parseInt(pJsArgs[1],1);
			CameraManager mMgr = new CameraManager(_index);
			_result = mMgr.getJsCamera();
		}
		return _result;
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mFeature = pFeatureMgr;
	}

	@Override
	public void dispose(String pAppid) {
	}

}
