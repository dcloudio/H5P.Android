package io.dcloud.media.weex.weex_video.ijkplayer.danmaku;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class StandardDanmaKuParser extends AcFunDanmakuParser {

    private int count = 0;

//    private int duration;
//
//    public StandardDanmaKuParser(int duration) {
//        this.duration = duration;
//    }

    /**
     * @param danmakuListData 弹幕数据
     *                        内容和颜色
     * @return 转换后的Danmakus
     */
    protected Danmakus doParse(JSONArray danmakuListData) {
        Danmakus danmakus = new Danmakus();
        if (danmakuListData == null) {
            return danmakus;
        }
        count = danmakuListData.length();
//        long item = duration / count;
        try {
            for (int i = 0; i < count; i++) {
                JSONObject danmakuArray = danmakuListData.getJSONObject(i);
                if (danmakuArray != null) {
//                    long time = (long) (Math.random()*item+item*i);
                    danmakus = _parse(danmakuArray, danmakus); }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return danmakus;
    }

    protected Danmakus _parse(JSONObject jsonObject, Danmakus danmakus) {
        if (danmakus == null) {
            danmakus = new Danmakus();
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return danmakus;
        }
        try {
            JSONObject obj = jsonObject;
            String text = obj.optString("text","....");
            String colorStr = obj.getString("color");
            int type = 1;
            BaseDanmaku item = mContext.mDanmakuFactory.createDanmaku(type,mContext);
            if (item != null) {
//                String timestamp = obj.optLong("time",0);
                long time = obj.optLong("time",0);
                item.setTime(time*1000);
                item.textSize = 25 * (mDispDensity - 0.6f);
                int color = Color.parseColor(colorStr);
                item.textColor = color;
                item.textShadowColor = color <= Color.BLACK ? Color.WHITE : Color.BLACK;
                DanmakuUtils.fillText(item,text);
                item.setTimer(mTimer);
                danmakus.addItem(item);
            }
        } catch (JSONException e) {
        }
        return danmakus;
    }
}
