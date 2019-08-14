package io.dcloud.feature.weex_amap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.TextView;

public class ArrowTextView extends TextView {
    private int mRadius = 0;
    private int mTextBgColor = Color.BLACK;
    private int defPadding = 10;
    private int mStrokeWidth = 0;
    private int mPadding = 0;
    private int mStrokeColor = Color.TRANSPARENT;
    private boolean isSharp = true;
    public ArrowTextView(Context context, boolean isSharp) {
        super(context);
        this.isSharp = isSharp;
        if(!isSharp) {
            defPadding = 0;
        }
    }

    public void setRadius(int r) {
        this.mRadius = r;
        invalidate();
    }

    public void setBgColor(int color) {
        mTextBgColor = color;
        invalidate();
    }

    public void setTextPadding(int padding) {
        mPadding = padding;
        int p = mPadding + defPadding + mStrokeWidth;
        setPadding(p, p, p, p);
        invalidate();
    }

    public void setStrokeWidth(int strokeWidth) {
        this.mStrokeWidth = strokeWidth * 2;
        setTextPadding(mPadding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);   //设置画笔抗锯齿

        int height = getHeight();   //获取View的高度
        int width = getWidth();     //获取View的宽度

        //框定文本显示的区域
        paint.setStrokeWidth(1);    //设置线宽
        int contentPading = defPadding + mStrokeWidth;
        RectF contentRect = new RectF(contentPading, contentPading, width - contentPading,height - contentPading);

        if(mStrokeWidth > 0) {//描边内容区域
            RectF rectStrok = new RectF(defPadding, defPadding, width - defPadding,height - defPadding);
            //设置线宽
            paint.setColor(mStrokeColor);  //设置线的颜色
            canvas.drawRoundRect(rectStrok, mRadius, mRadius, paint);
        }

        paint.setColor(mTextBgColor);  //设置线的颜色
        canvas.drawRoundRect(contentRect, mRadius, mRadius, paint);

        if(isSharp) {
            if(mStrokeWidth > 0) {//描边箭头
                Path pathStrok = new Path();
                //以下是绘制文本的那个箭头
                pathStrok.moveTo(width / 2, height);// 三角形顶点
                pathStrok.lineTo(width / 2 - defPadding, height - defPadding);   //三角形左边的点
                pathStrok.lineTo(width / 2 + defPadding, height - defPadding);   //三角形右边的点
                paint.setColor(mStrokeColor);
                pathStrok.close();
                canvas.drawPath(pathStrok, paint);
            }

            Path path = new Path();
            path.moveTo(width / 2, height- mStrokeWidth);// 三角形顶点
            path.lineTo(width / 2 - defPadding, height - contentPading);   //三角形左边的点
            path.lineTo(width / 2 + defPadding, height - contentPading);   //三角形右边的点
            paint.setColor(mTextBgColor);
            path.close();
            canvas.drawPath(path, paint);
        }
        super.onDraw(canvas);
    }

    public void setStrokeColor(int strokeColor) {
        this.mStrokeColor = strokeColor;
    }

}
