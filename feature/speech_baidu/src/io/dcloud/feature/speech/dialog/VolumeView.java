package io.dcloud.feature.speech.dialog;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Vrmlpad on 2017/10/23.<br/>
 * 自定义View-音量View,用来显示语音音量大小
 */

public class VolumeView extends View {
    /** 当前要显示的音量 */
    private int mCurrentVolume = 0;
    /** 最大音量 */
    private int mMaxVolume = 1;
    /** 画笔 */
    private Paint mPaint = null;
    /** 音量条颜色 */
    private int mVolumeColor = Color.RED;

    public VolumeView(Context context) {
        super(context);
        init();
    }

    public VolumeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VolumeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VolumeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * 初始化
     */
    private void init(){
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);//充满
        mPaint.setColor(mVolumeColor);
        mPaint.setAntiAlias(true);// 设置画笔的锯齿效果
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (1 <= mCurrentVolume && mCurrentVolume<=mMaxVolume) {
            float viewHeight = getHeight();
            float viewWidth = getWidth();
            float volumeHeight = viewHeight/(mMaxVolume+mMaxVolume-1);
            float volumeSpaceHeight = viewHeight/(mMaxVolume+mMaxVolume-1);
            float volumeLeft = 0,volumeRight,volumeTop,volumeBottom ;
            int volumeAndVolumeSpaceCount;
            for (int i = 1; i <= mCurrentVolume; i++) {
                volumeAndVolumeSpaceCount = mMaxVolume - i;
                volumeTop = volumeAndVolumeSpaceCount*(volumeHeight+volumeSpaceHeight);
                volumeRight = viewWidth*i/mMaxVolume;
                volumeBottom = volumeTop + volumeHeight;
                canvas.drawRect(volumeLeft, volumeTop, volumeRight, volumeBottom, mPaint);
            }
        }
    }

    public int getCurrentVolume() {
        return mCurrentVolume;
    }

    public void setCurrentVolume(int currentVolume) {
        this.mCurrentVolume = currentVolume;
        invalidate();
    }

    public int getMaxVolume() {
        return mMaxVolume;
    }

    public void setMaxVolume(int maxVolume) {
        this.mMaxVolume = maxVolume;
    }

    public int getVolumeColor() {
        return mVolumeColor;
    }

    public void setVolumeColor(int volumeColor) {
        this.mVolumeColor = volumeColor;
        if(null!=mPaint){
            try {
                mPaint.setColor(volumeColor);
            }catch (Exception e){}
        }
    }
}
