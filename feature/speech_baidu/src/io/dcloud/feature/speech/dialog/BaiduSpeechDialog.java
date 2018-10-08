package io.dcloud.feature.speech.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.util.Map;

import com.baidu.aip.core.mini.AutoCheck;
import com.baidu.aip.core.recog.MyRecognizer;
import com.baidu.aip.core.recog.listener.MessageStatusRecogListener;
import io.dcloud.common.util.PdrUtil;

public class BaiduSpeechDialog extends Dialog {

    private Activity mAttachActivity;
    private TextView dialogTitle;
    private VolumeView volumeView;
    private Context mContext;

    private MyRecognizer recognizer;
     public BaiduSpeechDialog(Context context) {
        super(context);
        mContext = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(SpeechR.LAYOUT_DIALOG);
        initView(mContext);
        getWindow().setBackgroundDrawable(null);
    }


    private void initView(Context context) {
         if (context instanceof Activity) {
             mAttachActivity = (Activity) context;
         } else {
             throw new RuntimeException("not an activity");
         }
         dialogTitle = (TextView) findViewById(SpeechR.ID_VOICE_TITLE);
         volumeView = (VolumeView) findViewById(SpeechR.ID_VOICE_VOLUME);
         volumeView.setMaxVolume(7);
         volumeView.setVisibility(View.VISIBLE);
         volumeView.setCurrentVolume(1);
         volumeView.setVolumeColor(Color.parseColor("#9C9C9C"));
    }

    @SuppressLint("HandlerLeak")
    public void startRecog(Map<String,Object> params, final Handler mHandler){
         //开始识别的时候引入布局

        recognizer = new MyRecognizer(mAttachActivity, new MessageStatusRecogListener(mHandler));
//        (new AutoCheck(mAttachActivity.getApplicationContext(), new Handler() {
//            public void handleMessage(Message msg) {
//                if (msg.what == 10086) {
//                    AutoCheck autoCheck = (AutoCheck) msg.obj;
//                    synchronized (autoCheck) {
//                        String message = autoCheck.obtainErrorMessage();
//                        Message msg1 = new Message();
//                        msg1.what = 10086;
//                        msg1.obj = message;
//                        mHandler.sendMessage(msg1);
//                    }
//                }
//            }
//        },false)).checkAsr(params);
        if ((Boolean) params.get("userInterface"))
            show();
        params.remove("userInterface");
        recognizer.start(params);
    }

    public boolean stopRecog(){
         if (isShowing()){
             dismiss();
         }
         if (recognizer != null) {
             try {
                 recognizer.stop();
                 recognizer.cancel();
                 recognizer.release();
                 return true;
             } catch (Exception e) {
             }

         }
         return false;
    }

    public void setVoiceText(String text){
         if (!PdrUtil.isEmpty(text)) {
             dialogTitle.setText(text);
         }
    }

    public int setVolume(int volume) {
         int voice = getVoiceVolume(volume);
         if (voice > 0)
            volumeView.setCurrentVolume(voice);
         return voice;
    }

    private int getVoiceVolume(int volume){
        int volumeIndex = 1;
        if(0<=volume && volume<200){
            volumeIndex = 1;
        }
        else if(200<=volume && volume<400){
            volumeIndex = 2;
        }
        else if(400<=volume && volume<600){
            volumeIndex = 3;
        }
        else if(600<=volume && volume<800){
            volumeIndex = 4;
        }
        else if(800<=volume && volume<1000){
            volumeIndex = 5;
        }
        else if(1000<=volume && volume<1200){
            volumeIndex = 6;
        }
        else if(1200<=volume){
            volumeIndex = 7;
        }
        return volumeIndex;
    }
}
