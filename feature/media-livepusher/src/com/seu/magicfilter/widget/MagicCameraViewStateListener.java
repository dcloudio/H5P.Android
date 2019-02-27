package com.seu.magicfilter.widget;

import android.content.res.Configuration;

public interface MagicCameraViewStateListener {
    public void onSurfaceChanged();
    public void onFilterChanged();
    public void onBeautyLevelChanged();
    public void onConfigurationChanged(Configuration newConfig);
}
