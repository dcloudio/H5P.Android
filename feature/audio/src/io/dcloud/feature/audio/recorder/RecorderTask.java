package io.dcloud.feature.audio.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.feature.audio.aac.AacEncode;
import io.dcloud.feature.audio.mp3.SimpleLame;


public class RecorderTask extends Thread {

    private static final String TAG = "RecorderTask";

    private int sampleRates[] = {44100, 22050, 11025, 8000};
    private final int configs[] = { AudioFormat.CHANNEL_IN_MONO,AudioFormat.CHANNEL_IN_STEREO};
    private final int formats[] = { AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT };

    // ======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 128kbps
     */
    public static final int DEFAULT_LAME_MP3_BIT_RATE = 128;

    private AudioRecord audioRecord = null;
    int bufsize = AudioRecord.ERROR_BAD_VALUE;
    private boolean mShouldRun = false;
    private boolean mShouldRecord = false;

    private long startTime = 0L;
    private long duration = 0L;

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    private int maxDuration;

    /**
     * 自定义 每220帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 220;
    private short[] mPCMBuffer;
    private byte[] mAacBuffer;
    private DataEncodeThread mEncodeThread;

    private File outputFile;
    private double mDuration;//录音时间,单位为毫秒
    private HighGradeRecorder.Callback mDurationListener;
    HighGradeRecorder mRecorder;
    boolean reallyStart;
    Handler handler;
    int waitingTime;
    private String mFormat;
//    private int mSamplingRate;
    public RecorderTask(File file, HighGradeRecorder recorder, RecordOption option) {
        outputFile = file;
        mRecorder = recorder;
        handler = new Handler();
        this.mFormat = option.mFormat;
        if(!option.isRateDeft) {
            sampleRates = new int[]{option.mSamplingRate, 44100, 22050, 11025, 8000};
        }
        if(recorder.getRecorderState() == HighGradeRecorder.State.PREPARED){
            waitingTime = 1000;
        }else {
            waitingTime = 10000;
        }
    }

    public void setCallback(HighGradeRecorder.Callback mDurationListener){
        this.mDurationListener = mDurationListener;
    }


    public void startRecording(){
        mShouldRecord = true;
    }

    public void resumeRecord(){
        mShouldRecord = true;
    }

    public void pauseRecord(){
        mShouldRecord = false;
    }

    public void stopRecord(){
        mShouldRecord = false;
        mShouldRun = false;

        if(audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        // stop the encoding thread and try to wait until the thread finishes its job
        Message msg = Message.obtain(mEncodeThread.getHandler(),
                DataEncodeThread.PROCESS_STOP);
        msg.sendToTarget();
    }

    private int mapFormat(int format){
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 8;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 16;
            default:
                return 0;
        }
    }

    private void cancel(){
        stopRecord();
    }

    public int getDuration(){
        return (int)mDuration;
    }

    @Override
    public void run() {
        super.run();
        if(!isFound()){
            Logger.e(TAG, "Sample rate, channel config or format not supported!");
            //SP.setBoolean("mp3permission",false);
            return;
        }
        init();
        mShouldRun = true;
        boolean oldShouldRecord = false;

        int bytesPerSecond = audioRecord.getSampleRate() * mapFormat(audioRecord.getAudioFormat()) / 8 * audioRecord.getChannelCount();
        mDuration = 0.0;
        while (mShouldRun) {
            if(mShouldRecord != oldShouldRecord){//只有状态切换的那一次会走这里
                if(mShouldRecord){
                    //监测8s内音频振幅大小,以判断是否拿到录音权限,还是空文件
                    startTime = System.currentTimeMillis();
                    try {
                        audioRecord.startRecording();//调用本地录音方法,如果有权限管理软件,会向系统申请权限
                        if (mDuration == 0 ){//第一次点击开始录音
                            reallyStart = true;
                            RecorderUtil.postTaskSafely(new Runnable() {
                                @Override
                                public void run() {
                                    mRecorder.onstart();
                                }
                            });
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    audioRecord.stop();
                }
                oldShouldRecord = mShouldRecord;
            }

            if(mShouldRecord){
                if(mFormat.equalsIgnoreCase("aac")) {
                    int readSize = audioRecord.read(mAacBuffer, 0, bufsize);
                    if(readSize > 0) {
                        mEncodeThread.addTask(mAacBuffer, readSize);
                    }

                } else {
                    int readSize = audioRecord.read(mPCMBuffer, 0, bufsize);
                    if (readSize > 0) {
                        final double read_ms = (1000.0 * readSize * 2) / bytesPerSecond;

                        final double volume = calVolume(mPCMBuffer,readSize);

                        mDuration += read_ms;
                        if (mDurationListener != null){
                            RecorderUtil.postTaskSafely(new Runnable() {
                                @Override
                                public void run() {
                                    //mDurationListener.onRecording(mDuration);
                                    mDurationListener.onRecording(mDuration,volume);
                                    if(maxDuration >0 && mDuration >= maxDuration){
                                        mRecorder.stop(HighGradeRecorder.ACTION_STOP);
                                        mDurationListener.onMaxDurationReached();
                                    }
                                }
                            });
                        }else {
                            //Log.e(TAG,"mDurationListener in audioRecorder is null!");
                        }

                        if(audioRecord!= null && audioRecord.getChannelCount()==1){
                            mEncodeThread.addTask(mPCMBuffer, readSize);
                        }else if(audioRecord!= null &&audioRecord.getChannelCount() == 2){
                            short[] leftData = new short[readSize / 2];
                            short[] rightData = new short[readSize / 2];
                            for(int i = 0;i< readSize /2; i = i + 2){
                                leftData[i] = mPCMBuffer[2 * i];
                                if( 2 * i + 1 < readSize){
                                    leftData[i+1] = mPCMBuffer[2 * i + 1];
                                }
                                if(2 * i + 2 < readSize){
                                    rightData[i] = mPCMBuffer[2 * i + 2];
                                }
                                if(2 * i + 3 < readSize){
                                    rightData[i + 1] = mPCMBuffer[2 * i + 3];
                                }
                            }
                            mEncodeThread.addTask(leftData,rightData, readSize / 2);
                        }
                    }
                }
            }
        }
    }

    private double calVolume(short[] buffer, double readSize) {
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < buffer.length; i++) {
            v += buffer[i] * buffer[i];
        }
        // 平方和除以数据总长度，得到音量大小。
        double mean = v / readSize;
        double volume = 10 * Math.log10(mean);
        return volume;
    }


    public boolean isRecording(){
        return mShouldRecord;
    }

    private void init() {
        int bytesPerFrame = audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT ? 2
                : 1;
        int frameSize = bufsize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            bufsize = frameSize * bytesPerFrame;
        }
        mPCMBuffer = new short[bufsize];
        mAacBuffer = new byte[bufsize];
        if(mFormat.equalsIgnoreCase("aac")) {
            AacEncode.getAacEncode(audioRecord.getSampleRate(), audioRecord.getChannelCount());
        } else {
            		/*
		 * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate
		 * The bit rate is 128kbps
		 */
            SimpleLame.init(audioRecord.getSampleRate(), audioRecord.getChannelCount(), audioRecord.getSampleRate(), DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        }


        // Create and run thread used to encode data
        // The thread will
        try {
            if(!outputFile.exists()){
                File parent = outputFile.getParentFile();
                if(!parent.exists()) {
                    parent.mkdirs();
                }
                outputFile.createNewFile();
            }
            mEncodeThread = new DataEncodeThread(outputFile, bufsize, mFormat);
            mEncodeThread.start();
            audioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
            audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * get the available AudioRecord
     * @return
     */
    private boolean isFound(){
        boolean isFound = false;

        int sample_rate = -1;
        int channel_config = -1;
        int format = -1;
        for(int x=0;!isFound &&x<formats.length;x++){
            format = formats[x];
            for(int y=0;!isFound && y<sampleRates.length;y++){
                sample_rate = sampleRates[y];
                for (int z = 0 ; !isFound && z < configs.length ; z++) {
                    channel_config = configs[z];

                    Logger.e(TAG, "Trying to create AudioRecord use: " + format + "/" + channel_config + "/" + sample_rate);
                    bufsize = AudioRecord.getMinBufferSize(sample_rate, channel_config, format);
                    Logger.e(TAG, "Bufsize: " + bufsize);
                    if (AudioRecord.ERROR_BAD_VALUE == bufsize) {
                        Logger.i(TAG, "invaild params!");
                        continue;
                    }
                    if(AudioRecord.ERROR == bufsize){
                        Logger.i(TAG, "Unable to query hardware!");
                        continue;
                    }

                    try {
                        createRecord(sample_rate, channel_config, format);
                        int state = audioRecord.getState();
                        if (state != AudioRecord.STATE_INITIALIZED) {
                            continue;
                        }
                    } catch (IllegalStateException e) {
                        Logger.i(TAG, "Failed to set up recorder!");
                        audioRecord = null;
                        continue;
                    }
                    isFound = true;
                    break;
                }
            }
        }

        return isFound;
    }

    private void createRecord(int sample_rate, int channel_config, int format) {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate,
                channel_config, format, bufsize);
    }
}
