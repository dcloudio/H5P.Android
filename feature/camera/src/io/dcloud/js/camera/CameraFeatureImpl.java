package io.dcloud.js.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


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
import io.dcloud.common.util.ThreadPool;
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
												 ThreadPool.self().addThreadTask(new Runnable() {
													 @Override
													 public void run() {
														 final String backPath;
													 	if(_option.optimize) {
															String destFilePath = PhotoBitmapUtils.amendRotatePhoto(filepath);
															backPath = _app.convert2RelPath(destFilePath);
														} else {
															backPath = _app.convert2RelPath(filepath);
														}
														 _app.getActivity().runOnUiThread(new Runnable() {
															 @Override
															 public void run() {
																 JSUtil.execCallback(pWebViewImpl, pCallbackId, backPath, JSUtil.OK, false, false);
															 }
														 });
													 }
												 });
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


class PhotoBitmapUtils {

	// 防止实例化
	private PhotoBitmapUtils() {
	}

	/**
	 * 保存Bitmap图片在SD卡中
	 * 如果没有SD卡则存在手机中
	 *
	 * @param mbitmap 需要保存的Bitmap图片
	 * @return 保存成功时返回图片的路径，失败时返回null
	 */
	public static String savePhotoToSD(Bitmap mbitmap, String filePath) {
		FileOutputStream outStream = null;
		String fileName = filePath;
		try {
			outStream = new FileOutputStream(fileName);
			// 把数据写入文件，100表示不压缩
			mbitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			return fileName;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (outStream != null) {
					// 记得要关闭流！
					outStream.close();
				}
				if (mbitmap != null) {
					mbitmap.recycle();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 读取图片
	 *
	 * @param path 原图的路径
	 * @return 压缩后的图片
	 */
	public static Bitmap getCompressPhoto(String path) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap bmp = BitmapFactory.decodeFile(path, options);
		options = null;
		return bmp;
	}

	/**
	 * 处理旋转后的图片
	 * @param originpath 原图路径
	 * @return 返回修复完毕后的图片路径
	 */
	public static String amendRotatePhoto(String originpath) {
		// 取得图片旋转角度
		int angle = readPictureDegree(originpath);
		if(angle == 0) {
			return originpath;
		}
		// 把原图压缩后得到Bitmap对象
		Bitmap bmp = getCompressPhoto(originpath);
		// 修复图片被旋转的角度
		Bitmap bitmap = rotaingImageView(angle, bmp);
		// 保存修复后的图片并返回保存后的图片路径
		return savePhotoToSD(bitmap, originpath);
	}

	/**
	 * 读取照片旋转角度
	 *
	 * @param path 照片路径
	 * @return 角度
	 */
	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return degree;
	}

	/**
	 * 旋转图片
	 * @param angle 被旋转角度
	 * @param bitmap 图片对象
	 * @return 旋转后的图片
	 */
	public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
		Bitmap returnBm = null;
		// 根据旋转角度，生成旋转矩阵
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		try {
			// 将原始图片按照旋转矩阵进行旋转，并得到新的图片
			returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		} catch (OutOfMemoryError e) {
		}
		if (returnBm == null) {
			returnBm = bitmap;
		}
		if (bitmap != returnBm) {
			bitmap.recycle();
		}
		return returnBm;
	}
}
