package io.dcloud.feature.audio.aac;

import com.sinaapp.bashell.VoAACEncoder;

public class AacEncode {

    VoAACEncoder mVoAEncoder;

    public VoAACEncoder init(int sampleRate, int bitRate, short nChannels,short adtsUsed) {
        if(mVoAEncoder == null) {
            mVoAEncoder = new VoAACEncoder();
        }
        mVoAEncoder.Init(sampleRate, bitRate, nChannels, adtsUsed);
        return mVoAEncoder;
    }

    public byte[] offerEncoder(byte[] inputBuffer){
        if(mVoAEncoder != null) {
            return mVoAEncoder.Enc(inputBuffer);
        }
        return null;
    }

    public int close(){
        if(mVoAEncoder != null) {
            int v = mVoAEncoder.Uninit();
            mVoAEncoder = null;
            return v;
        }
        return 0;
    }

    private static AacEncode mInstance;
    public static AacEncode getAacEncode() {
        if(mInstance == null) {
            mInstance = new AacEncode();
        }
        return mInstance;
    }
}
