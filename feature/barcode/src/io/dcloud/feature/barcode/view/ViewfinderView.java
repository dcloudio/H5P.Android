/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dcloud.feature.barcode.view;

import io.dcloud.feature.barcode.decoding.IBarHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.View;

import com.dcloud.zxing.ResultPoint;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

	private static final long ANIMATION_DELAY = 100L;
	private static final int OPAQUE = 0xFF;

	private final Paint paint;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;
	IBarHandler barHandler = null;
	private Rect lastRect=null;
	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context,IBarHandler pBarHandler) {
		super(context);
		barHandler = pBarHandler;
		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint();
		Resources resources = getResources();
		possibleResultPoints = new HashSet<ResultPoint>(5);
	}

	@Override
	public void onDraw(Canvas canvas) {
		// Rect detectorRect = CameraManager.get().getFramingRect();
		Rect detectorRect = DetectorViewConfig.getInstance().getDetectorRect();
		Rect gatherRect = DetectorViewConfig.getInstance().gatherRect;
		if (detectorRect == null) {
			return;
		}
		// Draw the exterior (i.e. outside the framing rect) darkened
		// 绘制非探测区alpha色值
		drawNonDetectorArea(canvas, detectorRect, gatherRect);

		// Draw a two pixel solid black border inside the framing rect
		// 绘制探测区边角
		drawDetectorCorner(canvas, detectorRect);

		// Draw a red "laser scanner" line through the middle to show decoding
		// is active
		// 绘制激光线束
		if(running){
			drawLaserLine(canvas, detectorRect);
		}else{
			lastRect = detectorRect;
			if(lastRect != null){
				drawLaserLine(canvas, lastRect);
			}
		}
		// 绘制可能的点
		// drawResultPoint(canvas, detectorRect);
	}

//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		barHandler.autoFocus();
//		return super.onTouchEvent(event);
//	}
	private void drawResultPoint(Canvas canvas, Rect detectorRect) {
		Collection<ResultPoint> currentPossible = possibleResultPoints;
		Collection<ResultPoint> currentLast = lastPossibleResultPoints;
		if (currentPossible.isEmpty()) {
			lastPossibleResultPoints = null;
		} else {
			possibleResultPoints = new HashSet<ResultPoint>(5);
			lastPossibleResultPoints = currentPossible;
			paint.setAlpha(OPAQUE);
			paint.setColor(DetectorViewConfig.resultPointColor);
			for (ResultPoint point : currentPossible) {
				canvas.drawCircle(detectorRect.left + point.getX(),
						detectorRect.top + point.getY(), 6.0f, paint);
			}
		}
		if (currentLast != null) {
			paint.setAlpha(OPAQUE / 2);
			paint.setColor(DetectorViewConfig.resultPointColor);
			for (ResultPoint point : currentLast) {
				canvas.drawCircle(detectorRect.left + point.getX(),
						detectorRect.top + point.getY(), 3.0f, paint);
			}
		}
	}

	ShapeDrawable laserBitmap = null;
	int laserY = 0;

	/**
	 * 绘制激光线束
	 * 
	 * @param canvas
	 * @param detectorRect
	 * <br/>
	 *            Create By: yanglei Email:yanglei@dcloud.io at 2013-6-4
	 *            下午05:38:00
	 */
	private void drawLaserLine(Canvas canvas, Rect detectorRect) {
			if (laserBitmap == null) {
				laserBitmap = new ShapeDrawable(new OvalShape());
				Shader Shader = new RadialGradient(detectorRect.width() / 2, DetectorViewConfig.LASER_WIDTH / 2, 240,DetectorViewConfig.laserColor, DetectorViewConfig.laserColor & 0x00ffffff,  TileMode.CLAMP);
				laserBitmap.getPaint().setShader(Shader);
			}
			paint.setAntiAlias(true);
			laserBitmap.setBounds(detectorRect.left, laserY, detectorRect.left + detectorRect.width(), laserY + DetectorViewConfig.LASER_WIDTH);
			laserBitmap.draw(canvas);
			paint.setShader(null);
	}

	Timer mUpdateProgressBar = null;
	public void stopUpdateScreenTimer() {
		if(running){
			if (mUpdateProgressBar != null) {
				mUpdateProgressBar.cancel();
				mUpdateProgressBar = null;
			}
			running = false;
			updateScreen();//更新页面
		}
	}

	private void updateScreen() {
		Rect detectorRect = DetectorViewConfig.getInstance().getDetectorRect();
		if (laserY > detectorRect.bottom) {
			laserY = detectorRect.top;
		} else {
			laserY += 1;
		}
		postInvalidate();
	}

	private boolean running = false;

	public void startUpdateScreenTimer() {
		if (!running) {
			stopUpdateScreenTimer();
			laserY = DetectorViewConfig.getInstance().getDetectorRect().top;
			mUpdateProgressBar = new Timer();
			mUpdateProgressBar.schedule(new TimerTask() {
				@Override
				public void run() {
					updateScreen();
				}
			}, 0, 10);
			running = true;
		}
	}

	/**
	 * 绘制非探测区域的alpha值
	 * 
	 * @param canvas
	 * @param detectorRect
	 * @param gatherRect
	 * <br/>
	 *            Create By: yanglei Email:yanglei@dcloud.io at 2013-6-4
	 *            下午05:21:00
	 */
	private void drawNonDetectorArea(Canvas canvas, Rect detectorRect,
			Rect gatherRect) {
		paint.setColor(DetectorViewConfig.maskColor);
		canvas.drawRect(0, 0, gatherRect.right, detectorRect.top, paint);
		canvas.drawRect(0, detectorRect.top, detectorRect.left,
				detectorRect.bottom, paint);
		canvas.drawRect(detectorRect.right, detectorRect.top, gatherRect.right,
				detectorRect.bottom, paint);
		canvas.drawRect(0, detectorRect.bottom, gatherRect.right,
				gatherRect.bottom, paint);
	}

	/**
	 * 绘制探测区的四角
	 * 
	 * @param canvas
	 * @param detectorRect
	 * <br/>
	 *            Create By: yanglei Email:yanglei@dcloud.io at 2013-6-4
	 *            下午05:21:28
	 */
	private void drawDetectorCorner(Canvas canvas, Rect detectorRect) {
		paint.setColor(DetectorViewConfig.cornerColor);
		int cornerOutter = DetectorViewConfig.CORNER_WIDTH / 2;
		int cornerInner = cornerOutter;
		int cornerLength = DetectorViewConfig.CORNER_HEIGHT;
		canvas.drawRect(detectorRect.left - cornerOutter, detectorRect.top
				- cornerOutter, detectorRect.left + cornerLength,
				detectorRect.top + cornerInner, paint);// 区域 -- 1
		canvas.drawRect(detectorRect.left - cornerOutter, detectorRect.top,
				detectorRect.left + cornerInner, detectorRect.top
						+ cornerLength, paint);// 区域 -- 2

		canvas.drawRect(detectorRect.right - cornerLength, detectorRect.top
				- cornerOutter, detectorRect.right + cornerOutter,
				detectorRect.top + cornerInner, paint);// 区域 -- 3
		canvas.drawRect(detectorRect.right - cornerInner, detectorRect.top,
				detectorRect.right + cornerOutter, detectorRect.top
						+ cornerLength, paint);// 区域 -- 4

		canvas.drawRect(detectorRect.left - cornerOutter, detectorRect.bottom
				- cornerLength, detectorRect.left + cornerInner,
				detectorRect.bottom, paint);// 区域 -- 5
		canvas.drawRect(detectorRect.left - cornerOutter, detectorRect.bottom
				- cornerInner, detectorRect.left + cornerLength,
				detectorRect.bottom + cornerOutter, paint);// 区域 -- 6

		canvas.drawRect(detectorRect.right - cornerLength, detectorRect.bottom
				- cornerInner, detectorRect.right + cornerOutter,
				detectorRect.bottom + cornerOutter, paint);// 区域 -- 7
		canvas.drawRect(detectorRect.right - cornerInner, detectorRect.bottom
				- cornerLength, detectorRect.right + cornerOutter,
				detectorRect.bottom + cornerOutter, paint);// 区域 -- 8

	}

	public void drawViewfinder() {
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}

}
