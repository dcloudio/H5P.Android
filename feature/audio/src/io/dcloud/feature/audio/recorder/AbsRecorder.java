package io.dcloud.feature.audio.recorder;

public abstract class AbsRecorder {
    public abstract void start();
    public abstract void stop();
    public abstract void pause();
    public abstract void resume();
    public abstract void release();
}
