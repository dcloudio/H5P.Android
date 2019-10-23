package io.dcloud.adapter.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

/**
 * Created by DCloud on 2017/9/14.
 */

public class SMSListener {
    IWebview mWebview;
    String mCallbackId;
    Context mContext;
    SMSBroadcastReceiver mSMSBroadcastReceiver;
    SMSContentObserver mSMSContentObserver;
    SMSListener(IWebview webview,String callbackid){
        mWebview = webview;
        mContext = webview.getContext();
        mCallbackId = callbackid;
        webview.obtainFrameView().addFrameViewListener(new IEventCallback() {
            @Override
            public Object onCallBack(String pEventType, Object pArgs) {
                if(PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE)) {
                    if(mSMSBroadcastReceiver != null){
                        mContext.unregisterReceiver(mSMSBroadcastReceiver);
                    }

                    if(mSMSContentObserver != null){
                        mContext.getContentResolver().unregisterContentObserver(mSMSContentObserver);
                    }
                }
                return null;
            }
        });
    }

    void onSucess(String address,String body,long timestamp){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("from",address);
            jsonObject.put("body",body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSUtil.execCallback(mWebview,mCallbackId,jsonObject,JSUtil.OK,true);
    }

    void onFail(String errorMsg){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message",errorMsg);
            jsonObject.put("code", DOMException.CODE_MESSAGING_ERROR);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSUtil.execCallback(mWebview,mCallbackId,jsonObject,JSUtil.ERROR,true);
    }
    void listen(){
        try {
            IntentFilter filter = new IntentFilter();
            filter.setPriority(1000);
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
            filter.addAction("android.provider.Telephony.SMS_READ");
            Handler mHandler = new Handler() {
                long lastTimestamp = System.currentTimeMillis();
                String lastBody;
                public void handleMessage(Message msg) {
                    SMS sms = (SMS)msg.obj;
                    if(sms.timestamp - lastTimestamp > 2000 && !TextUtils.equals(lastBody,sms.body)) {
                        switch (msg.what) {
                            case 1:
                                //此时认为有效
                                onSucess(sms.address,sms.body,sms.timestamp);
                             break;
                            case 2:{
                                onFail(sms.body);
                            }
                            default:
                                break;
                        }
                        lastTimestamp = sms.timestamp;
                        lastBody = sms.body;
                    }
                }
            };
            mSMSBroadcastReceiver = new SMSBroadcastReceiver(mHandler);
            mWebview.getContext().registerReceiver(mSMSBroadcastReceiver,filter);

            Uri smsUri = Uri.parse("content://sms");
            mSMSContentObserver = new SMSContentObserver(mWebview.getContext(),mHandler);
            mContext.getContentResolver().registerContentObserver(smsUri, true, mSMSContentObserver);
        } catch (Exception e) {
            e.printStackTrace();
            onFail(e.getMessage());
        }
    }

    class SMS{
        String address,body;
        long timestamp;
        SMS(String address,String body,long timestamp){
            this.address = address;
            this.body = body;
            this.timestamp = timestamp;
        }
    }
    class SMSContentObserver extends ContentObserver {
        Context mContext = null;
        Handler mHandler;
        public SMSContentObserver(Context context,Handler handler) {
            super(handler);
            mContext = context;
            mHandler = handler;
        }
        @Override
        public void onChange(boolean selfChange) {
//            onChange(selfChange,null);
//        }
//        @Override
//        public void onChange(boolean selfChange, Uri uri) {
            try {
//                if (uri == null) {
//                    uri = Uri.parse("content://sms/inbox");
//                }
//                if (uri.toString().equals("content://sms/raw")) {
//                    return;
//                }
                Cursor cursor = mContext.getContentResolver().query(Uri.parse("content://sms/inbox"),new String[]{"address","body","date"},null,null,"date desc");
                if(cursor != null){//新增短信才需要回调
                    cursor.moveToFirst();

//                    String[] clos = cursor.getColumnNames();
//                    for(int i = 0; i < clos.length ; i++){
//                        int index = cursor.getColumnIndex(clos[i]);
//                        try {
//                            String s = cursor.getString(index);
//                            Logger.e(clos[i] + "=" + s);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }

                    String address= cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    long  date= cursor.getLong(cursor.getColumnIndex("date"));
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = new SMS(address,body,date);
                    mHandler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = 2;
                message.obj = new SMS("none",e.getMessage(),System.currentTimeMillis());
                mHandler.sendMessage(message);
            }
            super.onChange(selfChange);
        }
    }

    class SMSBroadcastReceiver extends BroadcastReceiver {
        Handler mHandler;
        SMSBroadcastReceiver(Handler handler){
            mHandler = handler;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == "android.provider.Telephony.SMS_RECEIVED_DCLOUD"
                    || action == "android.provider.Telephony.SMS_RECEIVED"
                    || action == "android.provider.Telephony.SMS_READ"
                    ) {
                Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
                byte[][] pduObjs = new byte[messages.length][];

                for (int i = 0; i < messages.length; i++) {
                    pduObjs[i] = (byte[]) messages[i];
                }

                byte[][] pdus = new byte[pduObjs.length][];
                int pduCount = pdus.length;
                SmsMessage[] msgs = new SmsMessage[pduCount];
                for (int i = 0; i < pduCount; i++) {
                    pdus[i] = pduObjs[i];
                    msgs[i] = SmsMessage.createFromPdu(pdus[i]);
                }
                SmsMessage smsMessage = msgs[0];
                Message message = Message.obtain();
                message.what = 1;
                message.obj = new SMS(smsMessage.getDisplayOriginatingAddress(),smsMessage.getMessageBody(),smsMessage.getTimestampMillis());
                mHandler.sendMessage(message);
            }
        }
    }
}
