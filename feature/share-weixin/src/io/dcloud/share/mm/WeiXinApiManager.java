package io.dcloud.share.mm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.widget.Toast;

import com.nostra13.dcloudimageloader.core.ImageLoaderL;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMiniProgramObject;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import io.dcloud.ProcessMediator;
import io.dcloud.RInformation;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher.MessageListener;
import io.dcloud.common.DHInterface.IActivityHandler;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.share.IFShareApi;

/**
 * <p>Description:微信api管理者</p>
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-6-13 下午3:09:14 created.
 * <p>
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-6-13 下午3:09:14</pre>
 */
public class WeiXinApiManager implements IFShareApi {

    private static final String WEIXIN_DES = "微信";
    public static final String WEIXIN_ID = "weixin";
    public static final String KEY_APPID = "appid";
    public static final int THUMB_SIZE = 150;
    private static final String TAG = "WeiXinApiManager";
    private IWXAPI api;
    private static String APPID;

    private static int ERROR_NOTYPE = -100;
    private static int ERROR_NOT_COMPLETE = -101;

    @Override
    public void initConfig() {
        initData();
    }

    public void initData() {
//		APPID = "wxd930ea5d5a258f4f";//wx489313a817400fa0 demo——appid : wxd930ea5d5a258f4f  pname : net.sourceforge.simcpux
        APPID = AndroidResources.getMetaValue("WX_APPID");//AndroidManifest.xml中配置多个相同meta-data数据时使用后边的
    }

    private boolean hasFullConfigData() {
        return !TextUtils.isEmpty(APPID);
    }

    //返回false时则继续
    private boolean hasGeneralError(IWebview pWebViewImpl, String pCallbackId) {
        if (!hasFullConfigData()) {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return true;
        } else if (!PlatformUtil.isAppInstalled(pWebViewImpl.getContext(), "com.tencent.mm")) {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_CLIENT_UNINSTALLED, DOMException.toString(DOMException.MSG_CLIENT_UNINSTALLED));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return true;
        }
        return false;
    }

    @Override
    public String getId() {
        return WEIXIN_ID;
    }

    Object[] sendCallbackMsg = null;

    private void registerSendCallbackMsg(Object[] args) {
        sendCallbackMsg = args;
    }

    void executeSendCallbackMsg(BaseResp resp) {
        if (sendCallbackMsg != null) {
            IWebview pWebViewImpl = (IWebview) sendCallbackMsg[0];
            String pCallbackId = (String) sendCallbackMsg[1];
            if (resp != null) {
                onSendCallBack(pWebViewImpl, pCallbackId, resp.errCode);
            }
        }
    }

    MessageListener sSendCallbackMessageListener = new MessageListener() {

        @Override
        public void onReceiver(Object msg) {
            if (msg instanceof BaseResp) {
                executeSendCallbackMsg((BaseResp) msg);
                FeatureMessageDispatcher.unregisterListener(sSendCallbackMessageListener);
            }
        }
    };

    //	@Override
    public void send(final IWebview pWebViewImpl, final String pCallbackId, final String pShareMsg) {
        if (hasGeneralError(pWebViewImpl, pCallbackId)) {
            return;
        }
        new Thread() {
            public void run() {
                try {
                    JSONObject _msg = new JSONObject(pShareMsg);
                    String type = _msg.optString("type");
                    String _content = _msg.optString("content");
                    String _title = _msg.optString("title");
                    String href = JSONUtil.getString(_msg, "href");
                    String media = _msg.optString("media");
                    JSONArray _thumbs = _msg.optJSONArray("thumbs");
                    JSONArray _pictures = _msg.optJSONArray("pictures");
                    JSONObject extraInfo = JSONUtil.getJSONObject(_msg, "extra");
                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.scene = SendMessageToWX.Req.WXSceneTimeline;//默认分享到朋友圈
                    //是否同步到朋友圈WXSceneTimeline 默认为不同步WXSceneSession，只有4.2以上的微信客户端才支持同步朋友圈
                    if (extraInfo != null && !extraInfo.isNull("scene")) {
                        String senceValue = JSONUtil.getString(extraInfo, "scene");
                        if ("WXSceneSession".equals(senceValue)) {
                            req.scene = SendMessageToWX.Req.WXSceneSession;//会话
                        } else if ("WXSceneFavorite".equals(senceValue)) {
                            req.scene = SendMessageToWX.Req.WXSceneFavorite;//收藏
                        }
                    }
                    boolean isContinue = false;
                    String pImg=null;
                    String pThumbImg=null;
                    String AbsFullPath=null;
                    String AbsFullPathThumb=null;
                    int mRunningMode=0;
                    try {
                        if(!TextUtils.isEmpty(type)) {
                            if(type.equals("text")) {
                                isContinue = reqTextMsg(req, _content, _title);
                            } else if(type.equals("image")) {
                                if(_pictures == null || _pictures.length() ==0) {
                                    onSendCallBack(pWebViewImpl, pCallbackId, ERROR_NOT_COMPLETE);
                                    return;
                                }
                                /*pImg = _pictures.optString(0);
                                pThumbImg = _thumbs == null || _thumbs.isNull(0) ? pImg : _thumbs.optString(0);
                                AbsFullPath= pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pImg);
                                AbsFullPathThumb= pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pThumbImg);
                                mRunningMode  = pWebViewImpl.obtainFrameView().obtainApp().obtainRunningAppMode();*/
                                isContinue = reqImageMsg(pWebViewImpl, req, _pictures, _thumbs, _title);
                            } else if(type.equals("music")) {
                                if(PdrUtil.isEmpty(media)) {
                                    onSendCallBack(pWebViewImpl, pCallbackId, ERROR_NOT_COMPLETE);
                                    return;
                                }
                                isContinue = reqMusicMsg(pWebViewImpl, req, media, _thumbs != null ? _thumbs.optString(0, null) : null, _content, _title);
                            } else if(type.equals("video")) {
                                if(PdrUtil.isEmpty(media)) {
                                    onSendCallBack(pWebViewImpl, pCallbackId, ERROR_NOT_COMPLETE);
                                    return;
                                }
                                isContinue = reqVideoMsg(pWebViewImpl, req, media, _thumbs != null ? _thumbs.optString(0, null) : null, _content, _title);
                            } else if(type.equals("web")) {
                                if(PdrUtil.isEmpty(href)) {
                                    onSendCallBack(pWebViewImpl, pCallbackId, ERROR_NOT_COMPLETE);
                                    return;
                                }
                                isContinue = reqWebPageMsg(pWebViewImpl, req, href, _thumbs != null ? _thumbs.optString(0, null) : null, _content, _title);
                            } else if(type.equals("miniProgram")) {
                                JSONObject miniProgram = _msg.optJSONObject("miniProgram");
                                if(miniProgram == null) {
                                    onSendCallBack(pWebViewImpl, pCallbackId, ERROR_NOT_COMPLETE);
                                    return;
                                }
                                isContinue = reqMiniMsg(pWebViewImpl, req, (_thumbs != null ? _thumbs.optString(0, null) : null), _content, _title, miniProgram);
                            } else {
                                onSendCallBack(pWebViewImpl, pCallbackId, ERROR_NOTYPE);
                                return;
                            }
                        } else if (!PdrUtil.isEmpty(href)) {
                            isContinue = reqWebPageMsg(pWebViewImpl, req, href, _thumbs != null ? _thumbs.optString(0, null) : null, _content, _title);
                        } else if (_pictures != null && _pictures.length() > 0) {
                            pImg = _pictures.optString(0);
                            pThumbImg = _thumbs == null || _thumbs.isNull(0) ? pImg : _thumbs.optString(0);
                            AbsFullPath= pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pImg);
                            AbsFullPathThumb= pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pThumbImg);
                            mRunningMode  = pWebViewImpl.obtainFrameView().obtainApp().obtainRunningAppMode();
                            isContinue = reqImageMsg(pWebViewImpl, req, _pictures, _thumbs, _title);
                        } else {
                            isContinue = reqTextMsg(req, _content, _title);
                        }
                    } catch (Exception e) {//如遇到异常需要执行错误回调
                        e.printStackTrace();
                    }
                    if (!isContinue) {
                        pWebViewImpl.obtainWindowView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onSendCallBack(pWebViewImpl, pCallbackId, BaseResp.ErrCode.ERR_OK);
                            }
                        }, 500);
                        return;
                    }
                    if (pWebViewImpl.getActivity() instanceof IActivityHandler && ((IActivityHandler) pWebViewImpl.getActivity()).isMultiProcessMode()) {//多进程模式
                        startWeiXinMediator(pWebViewImpl, pCallbackId, req,pImg,pThumbImg,AbsFullPath,AbsFullPathThumb,mRunningMode);
                        return;
                    }
                    final boolean suc = api.sendReq(req);
                    if (suc && hasWXEntryActivity(pWebViewImpl.getContext())) {
                        FeatureMessageDispatcher.registerListener(sSendCallbackMessageListener);
                        registerSendCallbackMsg(new Object[]{pWebViewImpl, pCallbackId});
                    } else {
                        pWebViewImpl.obtainWindowView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onSendCallBack(pWebViewImpl, pCallbackId, BaseResp.ErrCode.ERR_SENT_FAILED);
                            }
                        }, 500);
                    }
                } catch (Exception e) {
                    //code ShareApiManager.SHARE_CONTENT_ERROR
                    e.printStackTrace();
                }
            }

            ;
        }.start();

    }

    private void startWeiXinMediator(final IWebview pWebViewImpl, final String pCallbackId, SendMessageToWX.Req req,String pImg,String pThumbImg,String absFullPath,String AbsFullPathThumb,int mRunningMode) {
        Intent intent = new Intent();
        intent.putExtra(ProcessMediator.LOGIC_CLASS, WeiXinMediator.class.getName());
        Bundle bundle = new Bundle();
        req.toBundle(bundle);
        bundle.putString("pImg",pImg);
        bundle.putString("pThumbImg",pThumbImg);
        bundle.putString("absFullPath",absFullPath);
        bundle.putString("AbsFullPathThumb", AbsFullPathThumb);
        bundle.putInt("mRunningMode",mRunningMode);
        intent.putExtra(ProcessMediator.REQ_DATA, bundle);
        intent.setClassName(pWebViewImpl.getActivity(), ProcessMediator.class.getName());
        pWebViewImpl.getActivity().startActivityForResult(intent, ProcessMediator.CODE_REQUEST);
        pWebViewImpl.getActivity().overridePendingTransition(0, 0);
        pWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener() {
            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                Object[] _args = (Object[]) pArgs;
                int requestCode = (Integer) _args[0];
                int resultCode = (Integer) _args[1];
                Intent data = (Intent) _args[2];
                if (pEventType == SysEventType.onActivityResult && requestCode == ProcessMediator.CODE_REQUEST) {
                    Bundle bundle = data.getBundleExtra(ProcessMediator.RESULT_DATA);
                    if (bundle == null) {
                        onSendCallBack(pWebViewImpl, pCallbackId, BaseResp.ErrCode.ERR_SENT_FAILED);
                    } else {
                        String s = bundle.getString(ProcessMediator.STYLE_DATA);
                        if ("BaseResp".equals(s)) {
                            SendMessageToWX.Resp resp = new SendMessageToWX.Resp();
                            resp.fromBundle(bundle);
                            onSendCallBack(pWebViewImpl, pCallbackId, resp.errCode);
                        } else if ("BaseReq".equals(s)) {
                        }
                    }
                }
                return false;
            }
        }, ISysEventListener.SysEventType.onActivityResult);
    }

    /**
     * 供原生代码分享调用
     *
     * @param activity
     * @param msg      flag 1是朋友圈，0是好友
     */
    public void send(final Activity activity, final String msg) {
        try {
            JSONObject msgJs = new JSONObject(msg);
            String content = msgJs.getString("content");
            String href = msgJs.getString("href");
            String thumbs = msgJs.optString("thumbs");
            int flag = msgJs.getInt("flag");
            initData();
            if (api == null) {
                api = WXAPIFactory.createWXAPI(activity.getApplicationContext(), APPID, true);
            }
            api.registerApp(APPID);
            if (!api.isWXAppInstalled()) {
                Toast.makeText(activity.getApplicationContext(), "您还未安装微信客户端", Toast.LENGTH_SHORT).show();
                return;
            }
            WXWebpageObject webpage = new WXWebpageObject();
            webpage.webpageUrl = href;
            WXMediaMessage wxmsg = new WXMediaMessage(webpage);
            wxmsg.description = content;
            wxmsg.title = content;
            Bitmap thumb = null;
            if (!TextUtils.isEmpty(thumbs) && new File(thumbs).exists()) {
                thumb = BitmapFactory.decodeFile(thumbs);
            }
            //如果图标为null，默认用App的图标。
            if (null == thumb) {
                thumb = BitmapFactory.decodeResource(activity.getResources(), RInformation.DRAWABLE_ICON);
            }
            wxmsg.setThumbImage(thumb);
            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = String.valueOf(System.currentTimeMillis());
            req.message = wxmsg;
            req.scene = flag;
            api.sendReq(req);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }

    /**
     * 供原生代码分享调用-分享图片
     *
     * @param activity
     * @param msg      flag 1是朋友圈，0是好友
     */
    public void sendImage(final Activity activity, final String msg) {
        try {
            JSONObject msgJs = new JSONObject(msg);
            String image = msgJs.optString("image");
            String thumbs = msgJs.optString("thumbs");
            String textToImage = msgJs.optString("textToImage");
            int flag = msgJs.optInt("flag");
            initData();
            if (api == null) {
                api = WXAPIFactory.createWXAPI(activity.getApplicationContext(), APPID, true);
            }
            api.registerApp(APPID);
            if (!api.isWXAppInstalled()) {
                Toast.makeText(activity.getApplicationContext(), "您还未安装微信客户端", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap imageBitmap = null;
            if (!TextUtils.isEmpty(image) && new File(image).exists()) {
                imageBitmap = BitmapFactory.decodeFile(image);
            }
            if (null == imageBitmap) {
                if (!TextUtils.isEmpty(textToImage)) {
                    imageBitmap = StringUtil.textToBitmap(activity, textToImage);
                }
            }
            if (null == imageBitmap) {
                return;
            }

            WXImageObject imageObject = new WXImageObject(imageBitmap);

            WXMediaMessage wxmsg = new WXMediaMessage(imageObject);
            wxmsg.mediaObject = imageObject;
            Bitmap thumb = null;
            if (!TextUtils.isEmpty(thumbs) && new File(thumbs).exists()) {
                thumb = BitmapFactory.decodeFile(thumbs);
            }
            //如果图标为null，默认用App的图标。
            if (null == thumb) {
                thumb = BitmapFactory.decodeResource(activity.getResources(), RInformation.DRAWABLE_ICON);
            }
            wxmsg.setThumbImage(thumb);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = String.valueOf(System.currentTimeMillis());
            req.message = wxmsg;
            req.scene = flag;
            api.sendReq(req);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }

    /**
     * 供原生代码分享调用-分享文本
     *
     * @param activity
     * @param msg      flag 1是朋友圈，0是好友
     */
    public void sendText(final Activity activity, final String msg) {
        try {
            JSONObject msgJs = new JSONObject(msg);
            String text = msgJs.optString("text");
            int flag = msgJs.optInt("flag");
            initData();
            if (api == null) {
                api = WXAPIFactory.createWXAPI(activity.getApplicationContext(), APPID, true);
            }
            api.registerApp(APPID);
            if (!api.isWXAppInstalled()) {
                Toast.makeText(activity.getApplicationContext(), "您还未安装微信客户端", Toast.LENGTH_SHORT).show();
                return;
            }

            WXTextObject textObject = new WXTextObject();
            textObject.text = text;

            WXMediaMessage wxmsg = new WXMediaMessage(textObject);
            wxmsg.mediaObject = textObject;
            wxmsg.description = text;

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = String.valueOf(System.currentTimeMillis());
            req.message = wxmsg;
            req.scene = flag;
            api.sendReq(req);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private void onSendCallBack(final IWebview pWebViewImpl,
                                final String pCallbackId, int code) {
        boolean suc = false;
        String errorMsg = DOMException.MSG_SHARE_SEND_ERROR;
        if (code == BaseResp.ErrCode.ERR_OK) {
            suc = true;
        } else if (code == BaseResp.ErrCode.ERR_AUTH_DENIED) {
            errorMsg = "Authentication failed";
        } else if (code == BaseResp.ErrCode.ERR_COMM) {
            errorMsg = "General errors";
        } else if (code == BaseResp.ErrCode.ERR_SENT_FAILED) {
            errorMsg = "Unable to send";
        } else if (code == BaseResp.ErrCode.ERR_UNSUPPORT) {
            errorMsg = "Unsupport error";
        } else if (code == BaseResp.ErrCode.ERR_USER_CANCEL) {
            errorMsg = "User canceled";
        } else if(code == ERROR_NOT_COMPLETE) {
            errorMsg = "参数不完整无法正确send";
        } else if(code == ERROR_NOTYPE) {
            errorMsg = "type参数无法正确识别，请按规范范围填写";
        }
        if (suc) {//由于调用微信发送接口，会立马回复true，而不是真正分享成功，甚至连微信界面都没有启动，在此延迟回调，以增强体验
            JSUtil.execCallback(pWebViewImpl, pCallbackId, "", JSUtil.OK, false, false);
        } else {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(code, "Share微信分享", errorMsg, mLink));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }
    }

    private boolean reqTextMsg(SendMessageToWX.Req req, String pText, String pTitle) {

        // 初始化一个WXTextObject对象
        WXTextObject textObj = new WXTextObject();
        textObj.text = pText;

        // 用WXTextObject对象初始化一个WXMediaMessage对象
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        if (!PdrUtil.isEmpty(pTitle)) {
            msg.title = pTitle;
        } else if (req.scene == SendMessageToWX.Req.WXSceneTimeline && !TextUtils.isEmpty(pText)) {
            // 如果为朋友圈分享 title为空 则使用_content未标题
            msg.title = pText;
        }
        msg.description = pText;

        // 构造一个Req
        req.transaction = buildTransaction("text"); // transaction字段用于唯一标识一个请求
        req.message = msg;

        return true;
    }

    private boolean reqImageMsg(IWebview pWebViewImpl, SendMessageToWX.Req req, JSONArray pImgs, JSONArray pThumbImgs, String pTitle) {
        WXMediaMessage msg = new WXMediaMessage();
        //判断个数，分享情形
        boolean mul = pImgs.length() > 1;
        mul = false;//暂只处理单个图片
        if (!mul) {//单个图片
//			pImg 内容大小不超过10MB  https://open.weixin.qq.com/zh_CN/htmledition/res/dev/document/sdk/android/index.html
            if (!(pWebViewImpl.getActivity() instanceof IActivityHandler && ((IActivityHandler) pWebViewImpl.getActivity()).isMultiProcessMode())) {//多进程模式
                String pImg = pImgs.optString(0);
                String pThumbImg = pThumbImgs == null || pThumbImgs.isNull(0) ? pImg : pThumbImgs.optString(0);
                if (PdrUtil.isNetPath(pImg)) {
                    WXImageObject imgObj = new WXImageObject();
                    try {
                        Bitmap bmp = BitmapFactory.decodeStream(new URL(pImg).openStream());
                        imgObj.imageData = bmpToByteArray(bmp, true);//content size within 10MB.
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    msg.mediaObject = imgObj;
                    pThumbImg = PdrUtil.isEmpty(pThumbImg) ? pImg : pThumbImg;
                    msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);
                } else {//imagePath The length should be within 10KB and content size within 10MB.
                    String AbsFullPath= pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(),pImg);
                    //Bitmap bitmap=scaleLoadPic(pWebViewImpl,AbsFullPath);
                    WXImageObject imgObj = new WXImageObject();
//                    imgObj.imagePath = AbsFullPath;
                    // 适配AndroidQ，"content://"文件（如相册获取）
                    if (AbsFullPath.startsWith("content://")) {
                        Uri contentUri = Uri.parse(AbsFullPath);
                        InputStream inputStream = null;
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        try {
                            inputStream = pWebViewImpl.getContext().getContentResolver().openInputStream(contentUri);
                            byte[] data = new byte[16384];
                            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        byte[] fileByteArr = buffer.toByteArray();
                        imgObj.imageData = fileByteArr;
                    } else {
                        imgObj.imagePath = AbsFullPath;
                    }
                    //bitmap.recycle();
//				WXImageObject imgObj = new WXImageObject();
//				imgObj.imagePath = pImg;//避免将来资源放置在程序私有目录第三方程序无权访问问题
                    msg.mediaObject = imgObj;
                    msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);

                }
            }
        } else {
            String clssName = "com.tencent.mm.ui.tools.ShareToTimeLineUI";
            switch (req.scene) {
                case SendMessageToWX.Req.WXSceneSession:
                    clssName = "com.tencent.mm.ui.chatting.ChattingUI";
                    break;
                case SendMessageToWX.Req.WXSceneTimeline://朋友圈
                    clssName = "com.tencent.mm.ui.tools.ShareToTimeLineUI";
                    break;
                case SendMessageToWX.Req.WXSceneFavorite://收藏
                    clssName = "com.tencent.mm.plugin.favorite.ui.FavoriteIndexUI";
                    break;
            }
            ArrayList<Uri> localArrayList = new ArrayList<Uri>();
            for (int i = 0; i < pImgs.length(); i++) {
                String pic = pImgs.optString(i);
                if (PdrUtil.isNetPath(pic)) {
                    try {
                        Bitmap bmp = BitmapFactory.decodeStream(new URL(pic).openStream());
                        String t_path = pWebViewImpl.obtainApp().obtainAppTempPath() + System.currentTimeMillis() + ".png";
                        PdrUtil.saveBitmapToFile(bmp, t_path);
                        localArrayList.add(Uri.fromFile(new File(t_path)));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//					localArrayList.add(Uri.parse(pic));
                } else {
                    pic = pWebViewImpl.obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), pic);
                    localArrayList.add(Uri.fromFile(new File(pic)));
                }
//				String filePath = sdcardDir + "/1/" + pics[i];
            }
            sedMultiplePic(pWebViewImpl.getActivity(), localArrayList, clssName);
            return false;
        }

//		if(PdrUtil.isNetPath(pImg)){
//			WXImageObject imgObj = new WXImageObject();
//			imgObj.imageUrl = pImg;
//			msg.mediaObject = imgObj;
//			pThumbImg = PdrUtil.isEmpty( pThumbImg ) ? pImg:pThumbImg;
//			msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);
//		}else{
//			pImg = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), pImg);
//			Bitmap bmp = BitmapFactory.decodeFile(pImg);
//			WXImageObject imgObj = new WXImageObject(bmp);
//			msg.mediaObject = imgObj;
//			msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);
//		}

        if (!PdrUtil.isEmpty(pTitle)) {//The length should be within 512Bytes
            msg.title = pTitle;
        }
        req.transaction = buildTransaction("img");
        req.message = msg;
        return true;
    }

    private boolean reqWebPageMsg(IWebview pWebViewImpl, SendMessageToWX.Req req, String webPage, String pThumbImg, String pText, String pTitle) {
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = webPage;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        if (!PdrUtil.isEmpty(pTitle)) {//The length should be within 512Bytes
            msg.title = pTitle;
        } else if (req.scene == SendMessageToWX.Req.WXSceneTimeline && !TextUtils.isEmpty(pText)) {
            // 如果为朋友圈分享 title为空 则使用_content未标题
            msg.title = pText;
        }
        msg.description = pText;//The length should be within 1KB
        if (!PdrUtil.isEmpty(pThumbImg)) {
            msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);
        }
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        return true;
    }

    private boolean reqMusicMsg(IWebview pWebViewImpl, SendMessageToWX.Req req, String musicUrl, String pThumbImg, String pText, String pTitle) {
        WXMusicObject music = new WXMusicObject();
        music.musicUrl = musicUrl;
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = music;
        if (!PdrUtil.isEmpty(pTitle)) {
            msg.title = pTitle;
        } else if (req.scene == SendMessageToWX.Req.WXSceneTimeline && !TextUtils.isEmpty(pText)) {
            // 如果为朋友圈分享 title为空 则使用_content未标题
            msg.title = pText;
        }
        if (!PdrUtil.isEmpty(pThumbImg)) {
            msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);
        }
        msg.description = pText;
        req.transaction = buildTransaction("music");
        req.message = msg;
        return true;
    }

    private boolean reqVideoMsg(IWebview pWebViewImpl, SendMessageToWX.Req req, String videoUrl, String pThumbImg, String pText, String pTitle) {
        WXVideoObject video = new WXVideoObject();
        video.videoUrl = videoUrl;
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = video;
        if (!PdrUtil.isEmpty(pTitle)) {
            msg.title = pTitle;
        } else if (req.scene == SendMessageToWX.Req.WXSceneTimeline && !TextUtils.isEmpty(pText)) {
            // 如果为朋友圈分享 title为空 则使用_content未标题
            msg.title = pText;
        }
        if (!PdrUtil.isEmpty(pThumbImg)) {
            msg.thumbData = buildThumbData(pWebViewImpl, pThumbImg);
        }
        msg.description = pText;
        req.transaction = buildTransaction("video");
        req.message = msg;
        return true;
    }

    private boolean reqMiniMsg(IWebview pWebViewImpl, SendMessageToWX.Req req, String pThumbImg, String pText, String pTitle, JSONObject miniProgram) {
        WXMiniProgramObject miniProgramObj = new WXMiniProgramObject();
        miniProgramObj.webpageUrl = miniProgram.optString("webUrl");
        miniProgramObj.miniprogramType = miniProgram.optInt("type");// 正式版:0，测试版:1，体验版:2
        miniProgramObj.userName = miniProgram.optString("id");     // 小程序原始id
        miniProgramObj.path = miniProgram.optString("path");            //小程序页面路径
        WXMediaMessage msg = new WXMediaMessage(miniProgramObj);
        if (!PdrUtil.isEmpty(pTitle)) {
            msg.title = pTitle;
        } else if (req.scene == SendMessageToWX.Req.WXSceneTimeline && !TextUtils.isEmpty(pText)) {
            // 如果为朋友圈分享 title为空 则使用_content未标题
            msg.title = pText;
        }
        req.scene = SendMessageToWX.Req.WXSceneSession; // 目前支持会话
        if (!PdrUtil.isEmpty(pThumbImg)) {
            msg.thumbData = getMiniThumbaData(pWebViewImpl, pThumbImg);
        }
        msg.description = pText;
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        return true;
    }

    private byte[] getMiniThumbaData(IWebview pWebViewImpl, String thumeImgPath) {
        byte[] ret = null;
        Bitmap bitmap = null;
        InputStream is = null;
        String AbsFullPath= pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), thumeImgPath);
        // The thumeImg size should be within 128KB * 1024 = 131072
        int maxSize = 131072;
        boolean isCompress = false;
        try {
            if (PdrUtil.isNetPath(thumeImgPath)) {//是网络地址
                //bitmap=ImageLoaderL.getInstance().loadImageSync(AbsFullPath);
                try {
                    is = new URL(thumeImgPath).openStream();
                    isCompress = is.available() > maxSize;
                    if(is != null){
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(is != null)
                    is.close();
                }
            } else {
                InputStream stream = new FileInputStream(AbsFullPath);
                isCompress = stream.available() > maxSize;
                bitmap= BitmapFactory.decodeStream(stream);
                if(stream != null)
                    stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e("buildThumbData Exception=" + e);
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(pWebViewImpl.getActivity().getResources(), RInformation.DRAWABLE_ICON);
        }

        if (bitmap != null) {
            if(isCompress) {
                bitmap=cpBitmap(bitmap);
            }
        }
        ret = bmpToByteArray(bitmap, true);  // 设置缩略图
        return ret;
    }

    public Bitmap scaleLoadPic(IWebview pWebViewImpl,String path){
        BitmapFactory.Options opts=new BitmapFactory.Options();
        //默认为false，设为true，则decoder返回null，
        //即BitmapFactory.decodeResource(getResources(),R.drawable.juhua,opts);返回null
        //但会返回图片的参数的信息到Options对象里
        //不解析图片到内存里
        opts.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,opts);
        //获取图片的宽，高
        int imageWidth=opts.outWidth;
        int imageHeigth=opts.outHeight;

        //获取屏幕的高宽
        Display dp=pWebViewImpl.getActivity().getWindowManager().getDefaultDisplay();
        //在高版本里有新的方法获取，但图片加载是常用功能，要兼容低版本，所以过时了也用
        int screenWidth=dp.getWidth();
        int screenHeight=dp.getHeight();

        //计算缩放比例
        int scale=1;
        int scaleWidth=imageWidth/screenWidth;
        int scaleHeight=imageHeigth/screenHeight;

        //取缩放比例，取那个大的值
        if(scaleWidth>=scaleHeight && scaleWidth>=1){
            scale=scaleWidth;
        }else if(scaleWidth<scaleHeight && scaleHeight>=1){
            scale=scaleHeight;
        }

        //设置缩放比例
        opts.inSampleSize=scale;
        opts.inJustDecodeBounds=false;
//        Bitmap bm=BitmapFactory.decodeFile(path,opts);
        Bitmap bm= null;
        // 适配AndroidQ，"content://"文件（如相册获取）
        if (path.startsWith("content://")) {
            Uri contentUri = Uri.parse(path);
            Cursor cursor = pWebViewImpl.getContext().getContentResolver().query(contentUri, null, null, null, null);
            if(cursor != null){
                InputStream inputStream = null;
                try {
                    inputStream = pWebViewImpl.getContext().getContentResolver().openInputStream(contentUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                cursor.moveToFirst();
                bm=BitmapFactory.decodeStream(inputStream,null,opts);
                //TO 成功
                cursor.close();
            }
        } else {
            bm=BitmapFactory.decodeFile(path,opts);
        }
        return bm;
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private static Bitmap cpBitmap(Bitmap orgBitmap) {
        if (PdrUtil.isEmpty(orgBitmap)) {
            return null;
        }
        Bitmap tmp;
        while (orgBitmap.getHeight() * orgBitmap.getRowBytes() >= 32 * 1024) {
            tmp = Bitmap.createScaledBitmap(orgBitmap, orgBitmap.getWidth() * 2 / 3, orgBitmap.getHeight() * 2 / 3, true);
	        orgBitmap.recycle();
            orgBitmap = tmp;

        }
        return orgBitmap;
    }

    private byte[] buildThumbData(IWebview pWebViewImpl, String thumeImgPath) {
        byte[] ret = null;
        Bitmap bitmap = null;
        InputStream is = null;
        String AbsFullPath= pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(),thumeImgPath);
        try {
//			The thumeImg size should be within 32KB * 1024 = 32768
            if (PdrUtil.isNetPath(thumeImgPath)) {//是网络地址
                bitmap=ImageLoaderL.getInstance().loadImageSync(thumeImgPath);
//                try {
//                    is = new URL(thumeImgPath).openStream();
//                    if(is != null){
//                        bitmap = BitmapFactory.decodeStream(is);
//                    }
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            } else {
                bitmap=scaleLoadPic(pWebViewImpl,AbsFullPath);
                //bitmap=ImageLoaderL.getInstance().loadImageSync("file://"+AbsFullPath);
            }
        } catch (Exception e) {
            Logger.e("buildThumbData Exception=" + e);
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(pWebViewImpl.getActivity().getResources(), RInformation.DRAWABLE_ICON);
        }

        if (bitmap != null) {
            bitmap=cpBitmap(bitmap);
        }
        ret = bmpToByteArray(bitmap, true);  // 设置缩略图
        return ret;
    }


    private void sedMultiplePic(Activity act, ArrayList<Uri> localArrayList, String activityClassName) {
        Intent intent = new Intent();
        //com.tencent.mm.ui.chatting.ChattingUI
        //com.tencent.mm.plugin.favorite.ui.FavoriteIndexUI
        //com.tencent.mm.ui.tools.ShareToTimeLineUI
        ComponentName localComponentName = new ComponentName("com.tencent.mm", activityClassName);
        intent.setComponent(localComponentName);
        intent.setAction("android.intent.action.SEND_MULTIPLE");
        intent.setType("image/*");
        intent.putParcelableArrayListExtra("android.intent.extra.STREAM", localArrayList);
        act.startActivity(intent);
    }


    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.JPEG, 80, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void forbid(IWebview pWebViewImpl) {
        if (api == null) {
            api = WXAPIFactory.createWXAPI(pWebViewImpl.getActivity().getApplicationContext(), APPID, true);
        }
        api.unregisterApp();
    }

    public static final String AUTHORIZE_TEMPLATE = "{authenticated:%s,accessToken:'%s'}";

    @Override
    public void authorize(IWebview pWebViewImpl, String pCallbackId, String options) {

        JSONObject jsonOptions = JSONUtil.createJSONObject(options);
        if (jsonOptions != null) {
            APPID = jsonOptions.optString(KEY_APPID, APPID);
            Logger.e(TAG, "authorize: appId" + APPID);
        }
        if (hasGeneralError(pWebViewImpl, pCallbackId)) {
            return;
        }
        boolean ret = register(pWebViewImpl);
        if (ret) {
            String msg = String.format(AUTHORIZE_TEMPLATE, ret, "");
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.OK, true, false);
        } else {
            String msg = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(BaseResp.ErrCode.ERR_AUTH_DENIED, "Share微信分享", "授权失败", mLink));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }
    }

    private boolean register(IWebview pWebViewImpl) {
        if (api == null) {
            api = WXAPIFactory.createWXAPI(pWebViewImpl.getActivity().getApplicationContext(), APPID, true);
        }
        boolean ret = false;
        // 将该app注册到微信
        if (!PdrUtil.isEmpty(APPID)) {
            ret = api.registerApp(APPID);
        }
        return ret;
    }

    @Override
    public String getJsonObject(IWebview pWebViewImpl) {
        String _json = null;
        try {
            JSONObject _weiXinObj = new JSONObject();
            _weiXinObj.put(StringConst.JSON_SHARE_ID, WEIXIN_ID);
            _weiXinObj.put(StringConst.JSON_SHARE_DESCRIPTION, WEIXIN_DES);
            _weiXinObj.put(StringConst.JSON_SHARE_AUTHENTICATED, register(pWebViewImpl));
            _weiXinObj.put(StringConst.JSON_SHARE_ACCESSTOKEN, "");
            _weiXinObj.put(StringConst.JSON_SHARE_NATIVECLIENT, api.isWXAppInstalled());
            _json = _weiXinObj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return _json;
    }

    private boolean hasWXEntryActivity(Context context) {
        String clsName = context.getPackageName() + ".wxapi.WXEntryActivity";
        try {
            Class.forName(clsName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 打开微信小程序
     * 此方法会反射调用 查看shareApiManager.java
     * @param pOptions
     */
    public void launchMiniProgram(String pOptions) {
        try {
            JSONObject optionsJson = new JSONObject(pOptions);
            String id = optionsJson.optString("id");
            String path = optionsJson.optString("path");
            int type = optionsJson.optInt("type", 0);
            WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
            req.userName = id;
            req.path = path;
            req.miniprogramType = type;
            api.sendReq(req);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        if (null != api) {
            api.unregisterApp();
            api.detach();
        }
        api = null;
    }
}