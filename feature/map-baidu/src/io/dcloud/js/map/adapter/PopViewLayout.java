package io.dcloud.js.map.adapter;

import io.dcloud.common.adapter.util.CanvasHelper;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.util.PdrUtil;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

class PopViewLayout extends View {
	private Drawable mBubbleIcon = null;
	private String mLabel = null;
	Bitmap bg0 = null;
	Bitmap bg1 = null;
	int[] patchs = new int[] { 21, 21, 21, 21 };
	/** 气泡尾巴高度 */
	int mBottomHeight = 0;
	/** 气泡中图标的宽 */
	int mBubbleIconWidth = 0;
	/** 气泡中图标的高 */
	int mBubbleIconHeight = 0;

	public PopViewLayout(Context context) {
		super(context);
		bg0 = BitmapFactory.decodeResourceStream(context.getResources(), null,
				PlatformUtil.getResInputStream("res/bubble_background.png"),
				null, null);
		bg1 = BitmapFactory.decodeResourceStream(context.getResources(), null,
				PlatformUtil.getResInputStream("res/arrow.png"), null, null);
		mBottomHeight = bg1.getHeight();
	}

	Paint paint = new Paint();
	int mTitleWidth = 0;
	int mTitleHeight = 0;

	@Override
	protected void onDraw(Canvas canvas) {
		int l = this.getLeft();
		int t = this.getTop();
		int w = this.getWidth();
		int h = this.getHeight();
		// 九分法绘制底部遮罩
		CanvasHelper.drawNinePatchs(canvas, bg0, patchs, l, t, w, h
				- mBottomHeight);
		int bg1_w = bg1.getWidth();
		int bg1_l = (w - bg1_w) / 2;
		int bg1_t = h - mBottomHeight - 1;
		// 绘制气泡小尾巴
		canvas.drawBitmap(bg1, bg1_l, bg1_t, paint);
		// 绘制图标
		int d_l = this.getLeft();
		if (mBubbleIcon != null) {
			d_l += space_w;
			int d_t = t + (h - mBottomHeight - mBubbleIconHeight) / 2;
			mBubbleIcon.setBounds(d_l, d_t, d_l + mBubbleIconWidth, d_t
					+ mBubbleIconHeight);
			mBubbleIcon.draw(canvas);
		}
		// 绘制描述信息
		{
			paint.setColor(0x88111111);
			int t_l = d_l + mBubbleIconWidth
					+ ((w - d_l - mBubbleIconWidth) - mTitleWidth) / 2;
			int t_t = t
					+ (h - mBottomHeight - CanvasHelper.getFontHeight(paint))
					/ 2;
			paint.setAntiAlias(true);
			CanvasHelper.drawString(canvas, mLabel, t_l, t_t,
					CanvasHelper.VCENTER, paint);
		}
	}

	/** 间隔 */
	final static int space_w = (int) CanvasHelper.getViablePx(6);
	/** 设置icon最大图片高度 */
	final static int max_icon_width_height = (int) CanvasHelper.getViablePx(50);
	/** 字体高度 */
	final static int font_height = (int) CanvasHelper.getViablePx(15);
	boolean didOnLayout = false;

	@Override
	public void onLayout(boolean change, int l, int t, int r, int b) {
		boolean hasBubbleIcon = mBubbleIcon != null;
		int width = mTitleWidth + space_w * (hasBubbleIcon ? 3 : 2);// 存在icon时有三个间隙，不存在icon时有两个间隙
		int height = mTitleHeight;
		if (hasBubbleIcon) {
			mBubbleIconWidth = Math
					.min(mBubbleIconWidth, max_icon_width_height);
			mBubbleIconHeight = Math.min(mBubbleIconHeight,
					max_icon_width_height);
			width += mBubbleIconWidth;// 考虑图标宽
			height = Math.max(height, mBubbleIconHeight);// 考虑图标高与文字内容高度
		}
		height += mBottomHeight + space_w * 2;// 考虑气泡小尾巴高度
		change = true;
		if (!didOnLayout) {
			didOnLayout = true;
			super.layout(l, t, l + width, b + height);
		}
	}

	void setBubbleLabel(String title) {
		mLabel = PdrUtil.isEmpty(title) ? "" : title;
		paint.setTextSize(font_height);
		int w = (int) paint.measureText(mLabel);
		mTitleWidth = w;
		mTitleHeight = (int) (paint.getFontMetrics().bottom - paint
				.getFontMetrics().top);
	}

	void setBubbleIcon(Drawable drawable) {
		mBubbleIcon = drawable;
		mBubbleIconWidth = mBubbleIcon.getIntrinsicWidth();
		mBubbleIconHeight = mBubbleIcon.getIntrinsicHeight();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Logger.d("PopViewLayout", "onTouchEvent");
		return super.onTouchEvent(event);
	}
}
