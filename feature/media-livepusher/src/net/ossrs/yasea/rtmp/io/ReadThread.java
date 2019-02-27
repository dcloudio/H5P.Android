package net.ossrs.yasea.rtmp.io;

import android.util.Log;

import net.ossrs.yasea.rtmp.RtmpPublisher;
import net.ossrs.yasea.rtmp.packets.RtmpPacket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

/**
 * RTMPConnection's read thread
 * 
 * @author francois, leo
 */
public class ReadThread extends Thread {

    private static final String TAG = "ReadThread";

    private RtmpDecoder rtmpDecoder;
    private InputStream in;
    private PacketRxHandler packetRxHandler;
    private RtmpPublisher publisher;

    public ReadThread(RtmpSessionInfo rtmpSessionInfo, InputStream in, PacketRxHandler packetRxHandler, RtmpPublisher publisher) {
        super("RtmpReadThread");
        this.in = in;
        this.packetRxHandler = packetRxHandler;
        this.rtmpDecoder = new RtmpDecoder(rtmpSessionInfo);
        this.publisher = publisher;
    }

    @Override
    public void run() {

        while (!Thread.interrupted()) {
            try {
                // It will be blocked when no data in input stream buffer
                RtmpPacket rtmpPacket = rtmpDecoder.readPacket(in);
                packetRxHandler.handleRxPacket(rtmpPacket);
            } catch (EOFException eof) {
                this.interrupt();
//            } catch (WindowAckRequired war) {
//                Log.i(TAG, "Window Acknowledgment required, notifying packet handler...");
//                packetRxHandler.notifyWindowAckRequired(war.getBytesRead());
//                if (war.getRtmpPacket() != null) {
//                    // Pass to handler
//                    packetRxHandler.handleRxPacket(war.getRtmpPacket());
//                }
            } catch (SocketException se) {
                Log.e(TAG, "ReadThread: Caught SocketException while reading/decoding packet, shutting down: " + se.getMessage());
//                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(this, se);
                publisher.getEventHandler().onNetWorkError(se, 2);
            } catch (IOException ioe) {
                Log.e(TAG, "ReadThread: Caught exception while reading/decoding packet, shutting down: " + ioe.getMessage());
//                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(this, ioe);
                publisher.getEventHandler().onNetWorkError(ioe, 2);
            }
        }

        Log.i(TAG, "exit");
    }

    public void shutdown() {
        Log.d(TAG, "Stopping");
    }
}
