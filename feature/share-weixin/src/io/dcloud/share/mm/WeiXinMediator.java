package io.dcloud.share.mm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;


import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


import io.dcloud.ProcessMediator;


import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;

import io.dcloud.common.util.PdrUtil;

import static io.dcloud.share.mm.WeiXinApiManager.bmpToByteArray;

public class WeiXinMediator implements ProcessMediator.Logic {
    private String APPID;
    private IWXAPI api;

    @Override
    public void exec(Context context, Intent intent) {
        APPID = AndroidResources.getMetaValue("WX_APPID");
        api = WXAPIFactory.createWXAPI(context, APPID, true);
        api.registerApp(APPID);
        Bundle bundle = intent.getBundleExtra(ProcessMediator.REQ_DATA);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.fromBundle(bundle);
        String pImg = bundle.getString("pThumbImg");
        String pThumbImg = bundle.getString("pImg");
        String absFullPath = bundle.getString("absFullPath");
        String AbsFullPathThumb = bundle.getString("AbsFullPathThumb");
        int mRunningMode = bundle.getInt("mRunningMode");
        if (PdrUtil.isEmpty(pImg) || PdrUtil.isEmpty(pThumbImg) || PdrUtil.isEmpty(absFullPath) || PdrUtil.isEmpty(AbsFullPathThumb)) {
            boolean ret = api.sendReq(req);
        } else {
            WXImageObject imgObj = new WXImageObject();
            byte[] thumbData = null;
            if (PdrUtil.isNetPath(pImg)) {
                Bitmap bmp = null;
                try {
                    bmp = BitmapFactory.decodeStream(new URL(pImg).openStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imgObj.imageData = bmpToByteArray(bmp, true);//content size within 10MB.

                pThumbImg = PdrUtil.isEmpty(pThumbImg) ? pImg : pThumbImg;
                thumbData = buildThumbData(context, pThumbImg, AbsFullPathThumb, mRunningMode);

            } else {//imagePath The length should be within 10KB and content size within 10MB.

                Bitmap bmp = scaleLoadPic(context,  absFullPath);
                imgObj = new WXImageObject(bmp);

//				WXImageObject imgObj = new WXImageObject();
//				imgObj.imagePath = pImg;//避免将来资源放置在程序私有目录第三方程序无权访问问题
                thumbData = buildThumbData(context, pThumbImg, AbsFullPathThumb, mRunningMode);

            }

            req.message.mediaObject = imgObj;
            req.message.thumbData = thumbData;
            // 在支付之前，如果应用没有注册到微信，应该先调用IWXMsg.registerApp将应用注册到微信
            boolean ret = api.sendReq(req);
        }


    }

    public Bitmap scaleLoadPic(Context context, String path) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        //默认为false，设为true，则decoder返回null，
        //即BitmapFactory.decodeResource(getResources(),R.drawable.juhua,opts);返回null
        //但会返回图片的参数的信息到Options对象里
        //不解析图片到内存里
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        //获取图片的宽，高
        int imageWidth = opts.outWidth;
        int imageHeigth = opts.outHeight;

        //获取屏幕的高宽
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display dp = wm.getDefaultDisplay();
        //在高版本里有新的方法获取，但图片加载是常用功能，要兼容低版本，所以过时了也用
        int screenWidth = dp.getWidth();
        int screenHeight = dp.getHeight();

        //计算缩放比例
        int scale = 1;
        int scaleWidth = imageWidth / screenWidth;
        int scaleHeight = imageHeigth / screenHeight;

        //取缩放比例，取那个大的值
        if (scaleWidth >= scaleHeight && scaleWidth >= 1) {
            scale = scaleWidth;
        } else if (scaleWidth < scaleHeight && scaleHeight >= 1) {
            scale = scaleHeight;
        }

        //设置缩放比例
        opts.inSampleSize = scale;
        opts.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path, opts);
        return bm;

    }


    private byte[] buildThumbData(Context context, String thumeImgPath, String absFullPathThumb, int mRuningMode) {
        byte[] ret = null;
        Bitmap bitmap = null;
        InputStream is = null;
        try {
//			The thumeImg size should be within 32KB * 1024 = 32768
            if (PdrUtil.isNetPath(thumeImgPath)) {//是网络地址
                is = new URL(thumeImgPath).openStream();
                if (is != null) {
                    bitmap = BitmapFactory.decodeStream(is);
                }

            } else {
                bitmap = scaleLoadPic(context,  absFullPathThumb);
            }
            if (bitmap != null) {
                bitmap = cpBitmap(bitmap);
            }
            ret = bmpToByteArray(bitmap, true);  // 设置缩略图
        } catch (Exception e) {
            Logger.e("buildThumbData Exception=" + e);
        }

        return ret;
    }

    private static Bitmap cpBitmap(Bitmap orgBitmap) {
        if (PdrUtil.isEmpty(orgBitmap)) {
            return null;
        }
        Bitmap tmp;
        while (orgBitmap.getHeight() * orgBitmap.getRowBytes() >= 32 * 1024) {
            tmp = Bitmap.createScaledBitmap(orgBitmap, orgBitmap.getWidth() * 2 / 3, orgBitmap.getHeight() * 2 / 3, true);
            if (orgBitmap != tmp) {
                orgBitmap.recycle();
            }
            orgBitmap = tmp;
        }
        return orgBitmap;
    }
}