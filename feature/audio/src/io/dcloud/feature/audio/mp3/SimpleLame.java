package io.dcloud.feature.audio.mp3;

public class SimpleLame {
	// io.dcloud.feature.audio.mp3 类名和 package不能修改 会导致so无法调用
	static{
		System.loadLibrary("lamemp3");
	}

	public native static void close();

	public native static int encode(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf);

	public native static int flush(byte[] mp3buf);

	public native static void init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate, int quality);
}
