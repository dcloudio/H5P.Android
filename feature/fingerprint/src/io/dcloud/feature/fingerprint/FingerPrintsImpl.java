package io.dcloud.feature.fingerprint;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.util.JSUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.support.v4.os.CancellationSignal;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

/**
 * 5+ SDK 扩展插件示例
 * 5+ 扩扎插件在使用时需要以下两个地方进行配置
 * 		1  WebApp的mainfest.json文件的permissions节点下添加JS标识
 * 		2  assets/data/properties.xml文件添加JS标识和原生类的对应关系
 * 本插件对应的JS文件在 assets/apps/H5Plugin/js/test.js
 * 本插件对应的使用的HTML assest/apps/H5plugin/index.html
 * 
 * 更详细说明请参考文档http://ask.dcloud.net.cn/article/66
 * **/
public class FingerPrintsImpl extends StandardFeature
{

    private FingerprintManagerCompat mFingerprintManager;
    private CancellationSignal mCancellationSignal;

    public String isSupport(IWebview pWebview, JSONArray array) {
        if (mFingerprintManager == null){
            mFingerprintManager = FingerprintManagerCompat.from(pWebview.getActivity());
        }
        if (isHardwareDetected()) {
            return JSUtil.wrapJsVar(true);
        }
        return JSUtil.wrapJsVar(false);
    }

    public String isEnrolledFingerprints(IWebview pWebview, JSONArray array){
        if (mFingerprintManager == null){
            mFingerprintManager = FingerprintManagerCompat.from(pWebview.getActivity());
        }
        if (isHasEnrolledFingerprints()) {
            return JSUtil.wrapJsVar(true);
        }
        return JSUtil.wrapJsVar(false);
    }

    public String isKeyguardSecure(IWebview pwebview, JSONArray array){
        if (isKeyguardSecure(pwebview.getActivity())) {
            return JSUtil.wrapJsVar(true);
        }
        return JSUtil.wrapJsVar(false);

    }

    public void cancel(IWebview pWebview, JSONArray array){
        cancelAuthenticate();
    }

    public void authenticate(final IWebview pWebview, JSONArray array)
    {
        final String  cbid = array.optString(0);
        if(mFingerprintManager == null){
            mFingerprintManager = FingerprintManagerCompat.from(pWebview.getActivity());
        }

        if (!isHardwareDetected()){
            resultCallback(pWebview,cbid, 1, "no fingerprint device", JSUtil.ERROR, false);
            return;
        }

        if (!isHasEnrolledFingerprints()){
            resultCallback(pWebview,cbid, 3, "UNENROLLED",JSUtil.ERROR, false);
            return;
        }

        if (!isKeyguardSecure(pWebview.getActivity())){
            resultCallback(pWebview,cbid, 2, "IN SECURE", JSUtil.ERROR, false);
            return;
        }

        callFingerPrintVerify(new IFingerprintResultListener() {
            @Override
            public void onAuthenticateError(int errMsgId, CharSequence errString) {
                int nErrorCode = 7;
                if (errMsgId == 5) {
                    // cancel
                    nErrorCode = 6;
                }else if(errMsgId == 7){
                    // Exceeded retries
                    nErrorCode = 5;
                }
                resultCallback(pWebview, cbid, nErrorCode, errString.toString(), JSUtil.ERROR, false);
            }

            @Override
            public void onAuthenticateFailed() {
                resultCallback(pWebview, cbid, 4, "Authenticate not match", JSUtil.ERROR, true);
            }

            @Override
            public void onAuthenticateHelp(int helpMsgId, CharSequence helpString) {
                //resultCallback(pWebview, cbid, 4, helpString.toString(), JSUtil.ERROR, true);
            }

            @Override
            public void onAuthenticateSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                resultCallback(pWebview, cbid, 0, "Authenticate Succeeded", JSUtil.OK, false);
            }
        });
    }

    private void resultCallback(IWebview pWebiew, String strCbid, int nErrCode, String resString, int resState, Boolean keepCallback){
        JSONObject resultObj = new JSONObject();
        try {
            resultObj.put("code", nErrCode);
            resultObj.put("message", resString);
        }catch (Exception exc){

        }

        JSUtil.execCallback(pWebiew, strCbid, resultObj, resState,keepCallback);
    }

    @SuppressLint("NewApi")
    public void callFingerPrintVerify(final IFingerprintResultListener listener) {

        if (mCancellationSignal == null) {
            mCancellationSignal = new CancellationSignal();
        }
        try {
            mFingerprintManager.authenticate(null, 0, mCancellationSignal, new FingerprintManagerCompat.AuthenticationCallback() {
                //多次尝试都失败会走onAuthenticationError。会停止响应一段时间。提示尝试次数过多。请稍后再试。
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    if (listener != null)
                        listener.onAuthenticateError(errMsgId, errString);
                }

                //指纹验证失败走此方法，比如小米前4次验证失败走onAuthenticationFailed,第5次走onAuthenticationError
                @Override
                public void onAuthenticationFailed() {
                    if (listener != null)
                        listener.onAuthenticateFailed();
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    if (listener != null)
                        listener.onAuthenticateHelp(helpMsgId, helpString);
                }

                //当验证的指纹成功时会回调此函数。然后不再监听指纹sensor
                @Override
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    if (listener != null)
                        listener.onAuthenticateSucceeded(result);
                }

            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否录入指纹，有些设备上即使录入了指纹，可是没有开启锁屏password的话此方法还是返回false
     *
     * @return
     */
    @SuppressLint("NewApi")
    private boolean isHasEnrolledFingerprints() {
        try {
            return mFingerprintManager.hasEnrolledFingerprints();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否有指纹识别硬件支持
     *
     * @return
     */
    @SuppressLint("NewApi")
    public boolean isHardwareDetected() {
        try {
            return mFingerprintManager.isHardwareDetected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 推断是否开启锁屏password
     *
     * @return
     */
    @SuppressLint("NewApi")
    private boolean isKeyguardSecure(Activity activity) {
        try {
            KeyguardManager mKeyManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
            return mKeyManager.isKeyguardSecure();
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * 指纹识别回调接口
     */
    public interface IFingerprintResultListener {
        void onAuthenticateError(int errMsgId, CharSequence errString);
        void onAuthenticateFailed();
        void onAuthenticateHelp(int helpMsgId, CharSequence helpString);
        void onAuthenticateSucceeded(FingerprintManagerCompat.AuthenticationResult result);
    }

    public void cancelAuthenticate() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

}