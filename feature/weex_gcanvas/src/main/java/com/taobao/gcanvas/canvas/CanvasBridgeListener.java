package com.taobao.gcanvas.canvas;

import com.taobao.weex.common.WXModule;

import java.util.ArrayList;
import java.util.List;

public class CanvasBridgeListener {

    private List<WXModule> modules = new ArrayList<>();

    public void setModules(WXModule modules) {
        this.modules.add(modules);
    }

    public void removeModules(WXModule modules){
        this.modules.remove(modules);
    }

    public void onEcecCallback(boolean success){
        for (WXModule m :this.modules) {
            if (m instanceof OnCallbackListener) {

            }
        }
    }

    public interface OnCallbackListener{
        void onCallback();
    }
}
