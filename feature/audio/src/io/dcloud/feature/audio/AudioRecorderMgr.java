package io.dcloud.feature.audio;

import android.os.Build;

import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;
import io.dcloud.feature.audio.recorder.AbsRecorder;
import io.dcloud.feature.audio.recorder.AudioRecorder;
import io.dcloud.feature.audio.recorder.HighGradeRecorder;
import io.dcloud.feature.audio.recorder.RecordOption;

public class AudioRecorderMgr extends AbsAudio {
    String mFunId;//成功失败时回调functionid
    private static AudioRecorderMgr mInstance;
    AbsRecorder mNativeRecorder;
    RecordOption mOption;

    private AudioRecorderMgr() {
    }

	static AudioRecorderMgr startRecorder(final RecordOption pOption, final String pFunId) {
		if (mInstance == null) {
			mInstance = new AudioRecorderMgr();
		}
        mInstance.mOption = pOption;
        mInstance.mFunId = pFunId;
		PermissionUtil.usePermission(mInstance.mOption.mWebview.getActivity(), mInstance.mOption.mWebview.obtainApp().isStreamApp(), PermissionUtil.PMS_RECORD, new PermissionUtil.StreamPermissionRequest(mInstance.mOption.mWebview.obtainApp()) {
			@Override
			public void onGranted(String streamPerName) {
				if(isPause(mInstance.mOption.mFormat)) {
					mInstance.mNativeRecorder = new HighGradeRecorder().setRecordOption(mInstance.mOption);
					if(mInstance.mOption.mFormat.equals("aac") && Build.VERSION.SDK_INT < 16) {
						mInstance.failCallback("当前系统不支持AAC录制！");
						return;
					}
					try {
						mInstance.mNativeRecorder.start();
					} catch (Exception e) {
						e.printStackTrace();
						mInstance.failCallback(e.getMessage());
						mInstance.stop();
					}
				} else {
					mInstance.mNativeRecorder = new AudioRecorder(mInstance.mOption);
					AbsRecorder mRecorder = mInstance.mNativeRecorder;
					try {
						mRecorder.start();
					} catch (Exception e) {
						e.printStackTrace();
						mInstance.failCallback(e.getMessage());
						mInstance.stop();
					}
				}

			}

			@Override
			public void onDenied(String streamPerName) {
				mInstance.failCallback(DOMException.MSG_NO_PERMISSION);
			}
		});
		return mInstance;
	}

	void successCallback(){
		String _filePath = mOption.mFileName;
		_filePath = mOption.mWebview.obtainFrameView().obtainApp().convert2RelPath(_filePath);
		JSUtil.excCallbackSuccess(mOption.mWebview, AudioRecorderMgr.this.mFunId, _filePath);
	}

	private void failCallback(String msg){
		String error_json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_RECORDER_ERROR,msg);
		JSUtil.excCallbackError(mOption.mWebview, AudioRecorderMgr.this.mFunId, error_json,true);
	}

	public void pause() {
		if(mInstance != null && mInstance.mOption != null && isPause(mInstance.mOption.mFormat)) {
			mInstance.mNativeRecorder.pause();
		}
	}

	public void resume() {
		if(mInstance != null && mInstance.mOption != null && isPause(mInstance.mOption.mFormat)) {
			mInstance.mNativeRecorder.resume();
		}
	}

	public void stop(){
		if(mInstance != null && mInstance.mNativeRecorder != null){
			mInstance.mNativeRecorder.stop();
			mInstance.mNativeRecorder.release();
			mInstance.mNativeRecorder = null;
			mInstance = null;
		}
	}

	public static boolean isPause(String format) {
		return format.equalsIgnoreCase("mp3") || format.equalsIgnoreCase("aac");
	}

}
