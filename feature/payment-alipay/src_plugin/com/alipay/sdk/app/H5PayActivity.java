//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.alipay.sdk.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alipay.sdk.util.l;

import io.src.dcloud.adapter.DCloudBaseActivity;

public class H5PayActivity
        extends DCloudBaseActivity {
    private WebView a;
    private WebViewClient b;

    public H5PayActivity() {
    }

    public void onCreate(Bundle var1) {
        H5PayActivity var2 = this;

        try {
            var2.requestWindowFeature(1);
        } catch (Throwable var4) {

        }

        super.onCreate(var1);

        String var6;
        String var7;
        try {
            if (!l.b(var7 = (var1 = this.getIntent().getExtras()).getString("url"))) {
                this.finish();
                return;
            }

            var6 = var1.getString("cookie");
        } catch (Exception var5) {
            this.finish();
            return;
        }

        try {
            this.a = l.a(this, var7, var6);
            this.b = new b(this);
            this.a.setWebViewClient(this.b);
        } catch (Throwable var3) {
            // a.a("biz", "GetInstalledAppEx", var3);
            this.finish();
        }
    }

    private void b() {
        try {
            super.requestWindowFeature(1);
        } catch (Throwable var1) {

        }
    }

    public void onBackPressed() {
        if (this.a.canGoBack()) {
            if (((b) this.b).c) {
                i var1;
                h.a = h.a((var1 = i.a(i.d.h)).h, var1.i, "");
                this.finish();
            }
        } else {
            h.a = h.a();
            this.finish();
        }
    }

    public void finish() {
        this.a();
        super.finish();
    }

    public void a() {
        Object var1 = PayTask.a;
        synchronized (PayTask.a) {
            try {
                var1.notify();
            } catch (Exception var3) {

            }

        }
    }

    public void onConfigurationChanged(Configuration var1) {
        super.onConfigurationChanged(var1);
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.a != null) {
            this.a.removeAllViews();
            ((ViewGroup) this.a.getParent()).removeAllViews();

            try {
                this.a.destroy();
            } catch (Throwable var2) {

            }

            this.a = null;
        }

        if (this.b != null) {
            b var1;
            (var1 = (b) this.b).b = null;
            var1.a = null;
        }

    }
}

