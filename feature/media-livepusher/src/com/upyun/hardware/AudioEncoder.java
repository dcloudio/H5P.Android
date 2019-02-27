package com.upyun.hardware;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;

import com.seu.magicfilter.utils.MagicParams;

import net.ossrs.yasea.SrsFlvMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder {
    private final static String MINE_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final String TAG = "AudioEncoder";
    private MediaCodec mediaCodec;
    private String codecName;

    private int mAudioTrack;
    private SrsFlvMuxer mFlvMuxer;

    public AudioEncoder(SrsFlvMuxer flvMuxer) {
        mFlvMuxer = flvMuxer;
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void initialize() {

        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
            for (String type : mediaCodecInfo.getSupportedTypes()) {
                if (TextUtils.equals(type, MINE_TYPE)
                        && mediaCodecInfo.isEncoder()) {
                    codecName = mediaCodecInfo.getName();
                    break;
                }
            }
            if (null != codecName) {
                break;
            }
        }
        try {
            mediaCodec = MediaCodec.createByCodecName(codecName);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MINE_TYPE,
                    44100, 1);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioRecord.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT));

//                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
//                        MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(mediaFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            mAudioTrack = mFlvMuxer.addTrack(mediaFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void fireAudio(byte[] data, int length) {


        if (MagicParams.SILENCE) {
            data = new byte[data.length];
        }

        if (mFlvMuxer.getVideoFrameCacheNumber().get() > 5) {
            return;
        }

        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(data);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,
                    System.nanoTime() / 1000, 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

            mFlvMuxer.writeSampleData(mAudioTrack, outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }


    public void stop() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }
}
