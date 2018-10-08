package io.dcloud.feature.audio.recorder;

import org.json.JSONObject;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.audio.AudioRecorderMgr;

public class RecordOption {
    public String mFileName;
    public int mSamplingRate;
    //录音输出格式类型：3gp/amr_nb/amr_wb/acc/mp3,默认amr
    public String mFormat;
    public IWebview mWebview;
    public boolean isRateDeft = false;

    public RecordOption(IWebview pWebview, JSONObject pOption){
        mWebview = pWebview;
        String rate = JSONUtil.getString(pOption, StringConst.JSON_KEY_SAMPLERATE);
        mFormat = JSONUtil.getString(pOption, StringConst.JSON_KEY_FORMAT);
        if(PdrUtil.isEmpty(mFormat)) {
            mFormat = "amr";
        }
        int pDeft = 8000;
        if(AudioRecorderMgr.isPause(mFormat)) {
            pDeft = 44100;
        }
        if(!PdrUtil.isEmpty(rate)) {
            isRateDeft = false;
            mSamplingRate = PdrUtil.parseInt(JSONUtil.getString(pOption, StringConst.JSON_KEY_SAMPLERATE), pDeft);
        } else {
            isRateDeft = true;
            mSamplingRate = pDeft;
        }

        String filename = JSONUtil.getString(pOption, StringConst.JSON_KEY_FILENAME);

        filename = PdrUtil.getDefaultPrivateDocPath(filename, mFormat);
        mFileName = mWebview.obtainFrameView().obtainApp().convert2AbsFullPath(mWebview.obtainFullUrl(),filename);
    }
}
