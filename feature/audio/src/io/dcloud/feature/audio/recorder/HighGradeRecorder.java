package io.dcloud.feature.audio.recorder;

import java.io.File;

/**
 * 高级录音  还有暂停 继续目前仅支持mp3 aac编码
 */
public class HighGradeRecorder extends AbsRecorder{

    private int mMaxDuration;// 最长录音时间，单位：毫秒
    private String outputFilePath;
    private RecorderTask audioRecorder = null;
    private int state = State.UNINITIALIZED;
    public static final int ACTION_RESET = 1;
    public static final int ACTION_STOP =2;
    Callback mStateListener;
    int stateBeforeFocusChange;

    private RecordOption mOption;

    public class State {
        public static final int UNINITIALIZED = -1;
        public static final int INITIALIZED = 0;
        public static final int PREPARED = 1;
        public static final int RECORDING = 2;
        public static final int PAUSED = 3;
        public static final int STOPPED = 4;
    }

    public HighGradeRecorder(){

    }

    public int getmMaxDuration() {
        return mMaxDuration;
    }


    /**
     *
     * @param maxDurationInSecond 单位为秒
     */
    public HighGradeRecorder setMaxDuration(int maxDurationInSecond) {
        this.mMaxDuration = maxDurationInSecond*1000;
        return this;
    }

    public HighGradeRecorder setOutputFile(String path){
        this.outputFilePath = path;
        return this;
    }

    public HighGradeRecorder setRecordOption(RecordOption option) {
        this.outputFilePath = option.mFileName;
        this.mOption = option;
        return this;
    }

    public HighGradeRecorder setCallback(Callback listener){
        this.mStateListener = listener;
        return this;
    }

    public void start(){
        if(state == State.INITIALIZED ||
                state == State.STOPPED || state == State.PREPARED
                || state == State.UNINITIALIZED){
            audioRecorder = new RecorderTask(new File(outputFilePath),this, this.mOption);
            audioRecorder.setCallback(mStateListener);
            audioRecorder.setMaxDuration(mMaxDuration);
            audioRecorder.start();
            state = State.PREPARED;
            audioRecorder.startRecording();
        }else if(state == State.PAUSED){
            resume();
        }
    }

    @Override
    public void stop() {
        stop(ACTION_STOP);
    }


    /**
     * 只供AudioRecorder调用,真正的开始
     */
     void onstart(){
        if(state == State.PREPARED){
            state = State.RECORDING;
            if (mStateListener != null){
                mStateListener.onStart();
            }
        }
    }

    public void pause(){
        if (audioRecorder != null && state == State.RECORDING){
            audioRecorder.pauseRecord();
            state = State.PAUSED;
            if (mStateListener != null){
                mStateListener.onPause();
            }
        }

    }

    public void resume(){
        if (audioRecorder != null && state == State.PAUSED){
            audioRecorder.resumeRecord();
            state = State.RECORDING;
            if (mStateListener != null){
                mStateListener.onResume();
            }
        }

    }

    @Override
    public void release() {

    }

    public void stop(int action){
        if (audioRecorder != null && state == State.RECORDING){
            audioRecorder.stopRecord();
            state = State.STOPPED;
            if (mStateListener != null){
                mStateListener.onStop(action);
            }
        }
    }

    public int getRecorderState(){
        return state;
    }

    public void reset(){
        if(null == audioRecorder){
            return;
        }
        if(null != audioRecorder && state != State.STOPPED){
            stop(ACTION_RESET);
        }
        audioRecorder = null;

        if (mStateListener != null){
            mStateListener.onReset();
        }
    }

    public interface Callback {
        void onStart();
        void onPause();
        void onResume();
        void onStop(int action);
        void onReset();

        /**
         *
         * @param duration 过了多长时间
         * @param volume 这个时间的段的分贝值
         */
        void onRecording(double duration, double volume);
        void onMaxDurationReached();
    }

}
