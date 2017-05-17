package io.dcloud.feature.barcode.decoding;

import io.dcloud.feature.barcode.view.ViewfinderView;
import android.graphics.Bitmap;
import android.os.Handler;

import com.dcloud.zxing.Result;

public interface IBarHandler {
	Handler getHandler();
	ViewfinderView getViewfinderView();
	void autoFocus();
	void handleDecode(Result obj, Bitmap barcode);
	void drawViewfinder();
	boolean isRunning();
}
