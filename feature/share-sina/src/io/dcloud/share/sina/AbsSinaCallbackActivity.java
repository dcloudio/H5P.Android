package io.dcloud.share.sina;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;

import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.src.dcloud.adapter.DCloudBaseActivity;

/**
 * 进入新浪微博客户端的分享管理类（有编辑分享内容界面的分享）
 */
public class AbsSinaCallbackActivity extends DCloudBaseActivity implements IWeiboHandler.Response {
    private static final String TAG=AbsSinaCallbackActivity.class.getName();
    /**
     * 微博微博分享接口实例
     */
    private IWeiboShareAPI mWeiboShareAPI = null;
    private String APP_KEY;
    private ImageObject imageObject = null;
    private TextObject textObject = null;
    private int flag=0;//为防止分享过程中某个环节终止了分享，添加flag来及时finish掉本activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        APP_KEY=getIntent().getStringExtra("appkey");
        // 创建微博分享接口实例
        mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(that, APP_KEY);
        // 注册第三方应用到微博客户端中，注册成功后该应用将显示在微博的应用列表中。
        // NOTE：请务必提前注册，即界面初始化的时候或是应用程序初始化时，进行注册
        mWeiboShareAPI.registerApp();
        imageObject = getIntent().getParcelableExtra("imageObject");
        textObject = getIntent().getParcelableExtra("textObject");
        sendMessage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (flag==1){
            finish();
        }
        flag++;
    }

    @Override
    public void onNewIntentImpl(Intent intent) {
        Log.e(TAG, "onNewIntentImpl: " );
        super.onNewIntentImpl(intent);
        // 从当前应用唤起微博并进行分享后，返回到当前应用时，需要在此处调用该函数
        // 来接收微博客户端返回的数据；执行成功，返回 true，并调用
        // {@link IWeiboHandler.Response#onResponse}；失败返回 false，不调用上述回调
        mWeiboShareAPI.handleWeiboResponse(intent, this);
    }
//    @Override
//    protected void onNewIntent(Intent intent) {
//        Log.e(TAG, "onNewIntent: " );
//        super.onNewIntent(intent);
//        // 从当前应用唤起微博并进行分享后，返回到当前应用时，需要在此处调用该函数
//        // 来接收微博客户端返回的数据；执行成功，返回 true，并调用
//        // {@link IWeiboHandler.Response#onResponse}；失败返回 false，不调用上述回调
//        mWeiboShareAPI.handleWeiboResponse(intent, this);
//    }

    /**
     * 分享后回调
     *
     * @param baseResponse
     */
    @Override
    public void onResponse(BaseResponse baseResponse) {
        FeatureMessageDispatcher.dispatchMessage(baseResponse);
        finish();
    }

    /**
     * 第三方应用发送请求消息到微博，唤起微博分享界面。
     */
    private void sendMessage() {
        if (mWeiboShareAPI.isWeiboAppSupportAPI()) {
            int supportApi = mWeiboShareAPI.getWeiboAppSupportAPI();
            if (supportApi >= 10351 /*ApiUtils.BUILD_INT_VER_2_2*/) {
                sendMultiMessage();
            } else {
                Toast.makeText(that.getApplicationContext(), "微博客户端版本过低，请升级", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(that.getApplicationContext(), "微博客户端不支持 SDK 分享或微博客户端未安装或微博客户端是非官方版本。", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 第三方应用发送请求消息到微博，唤起微博分享界面。
     * 注意：当 {@link IWeiboShareAPI#getWeiboAppSupportAPI()} >= 10351 时，支持同时分享多条消息，
     * 同时可以分享文本、图片以及其它媒体资源（网页、音乐、视频、声音中的一种）。
     */
    private void sendMultiMessage() {
        // 1. 初始化微博的分享消息
        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
        weiboMessage.textObject = textObject;
        weiboMessage.imageObject = imageObject;
        // 2. 初始化从第三方到微博的消息请求
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        // 用transaction唯一标识一个请求
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.multiMessage = weiboMessage;
        // 3. 发送请求消息到微博，唤起微博分享界面
        mWeiboShareAPI.sendRequest(that, request);
    }
}
