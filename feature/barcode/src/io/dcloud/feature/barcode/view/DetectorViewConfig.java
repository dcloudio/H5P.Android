package io.dcloud.feature.barcode.view;

import io.dcloud.common.DHInterface.IReflectAble;
import android.graphics.Rect;

public class DetectorViewConfig implements IReflectAble{
	private static final int MIN_FRAME_WIDTH = 240;
	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 640;
	private static final int MAX_FRAME_HEIGHT = 360;

	public static final int F_LASER_COLOR = 0xffff0000;//默认扫描条颜色
	public static final int F_CORNER_COLOR = 0xffff0000;
	public static int maskColor = 0x60000000;
//	public static int laserColor_ = 0x00ff0000;
	public static int laserColor = 0xffff0000;
	public static int cornerColor = laserColor;
	public static int resultPointColor = 0xc0ffff00;
	
	public static int CORNER_WIDTH = 8;
	public static int CORNER_HEIGHT = 40;
	
	public static int LASER_WIDTH = 8;
	
	public static int detectorRectOffestLeft = 0;
	public static int detectorRectOffestTop = 0;
	/**surface区域*/
	public Rect surfaceViewRect = null;
	
	/**数据采集区域*/
	public Rect gatherRect = new Rect();
	/**数据监测区域,将来截取图片的区域*/
	private Rect detectorRect = null;

	private static DetectorViewConfig instance;
	private DetectorViewConfig(){
	}
	public static DetectorViewConfig getInstance(){
		if(instance == null){
			instance = new DetectorViewConfig();
		}
		return instance;
	}
	
	public static void clearData(){
		instance = null;
	}
	
	public void initSurfaceViewRect(int svLeft,int svTop,int svWidth,int svHeight){
		surfaceViewRect = new Rect(svLeft, svTop, svLeft + svWidth, svTop + svHeight);
//		int gatherAreaW = svWidth - CORNER_WIDTH;
//		int gatherAreaH = svHeight - CORNER_WIDTH;
//		int width = gatherAreaW * 6 / 10;
//		if (width < MIN_FRAME_WIDTH) {
//			width = MIN_FRAME_WIDTH;
//		} else if (width > MAX_FRAME_WIDTH) {
//			width = MAX_FRAME_WIDTH;
//		}
//		int height = width;
//		CORNER_HEIGHT = width * 10 / 100;
//		int leftOffset = (gatherAreaW - width) / 2;
//		int topOffset = (gatherAreaH - height) / 2;
//		detectorRect = new Rect(leftOffset, topOffset, leftOffset + width,topOffset + height);
	}
	public Rect getDetectorRect(){
		if(detectorRect == null){
			int gatherAreaW = gatherRect.width() - CORNER_WIDTH;
			int gatherAreaH = gatherRect.height() - CORNER_WIDTH;
			int width = gatherAreaW * 6 / 10;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}
			int height = width;
			CORNER_HEIGHT = width * 10 / 100;
			int leftOffset = (gatherAreaW - width) / 2;//sp
			int topOffset = (gatherAreaH - height) / 2;//sp
			detectorRect = new Rect(leftOffset, topOffset, leftOffset + width,topOffset + height);
		}
		return detectorRect;
	}
}
