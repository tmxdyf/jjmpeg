/*
 * Copyright (c) 2012 Michael Zucchi
 *
 * This file is part of jjmpeg.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.jjmpeg.mediaplayer;

import android.os.Debug;
import au.notzed.jjmpeg.AVCodec;
import au.notzed.jjmpeg.AVCodecContext;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVIOException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for Decoders.
 *
 * @author notzed
 */
public abstract class MediaDecoder extends CancellableThread {

	final MediaReader src;
	final MediaSink dest;
	final AVStream stream;
	final AVCodecContext cc;
	final AVCodec codec;
	// Time base
	final int tb_Num;
	final int tb_Den;
	// Start time in ms
	final long startms;
	final long startpts;
	final long duration;
	// packet input queue
	LinkedBlockingQueue<AVPacket> queue = new LinkedBlockingQueue<AVPacket>(20);
	//
	static AVPacket flush = AVPacket.create();
	static AVPacket cancel = AVPacket.create();

	/**
	 * Initialise a media decoder.
	 *
	 * A suitable ffmpeg codec is found for the stream, and some timebase stuff set up.
	 *
	 * @param src
	 * @param dest
	 * @param stream
	 * @param streamid
	 * @throws IOException
	 */
	MediaDecoder(MediaReader src, MediaSink dest, AVStream stream, int streamid) throws IOException {
		super("Media Decoder");
		try {
			this.src = src;
			this.dest = dest;
			this.stream = stream;

			// find decoder for the video stream
			cc = stream.getCodec();
			codec = AVCodec.findDecoder(cc.getCodecID());
			if (codec == null) {
				throw new IOException("Unable to find video decoder " + cc.getCodecID());
			}

			cc.open(codec);

			System.out.println("Codec: " + codec.getName());

			AVRational tb = stream.getTimeBase();
			tb_Num = tb.getNum();
			tb_Den = tb.getDen();

			startpts = stream.getStartTime();
			startms = AVRational.starSlash(startpts * 1000, tb_Num, tb_Den);
			duration = AVRational.starSlash(stream.getDuration() * 1000, tb_Num, tb_Den);

			System.out.println("stream start " + startms + " length " + duration);
		} catch (AVIOException ex) {
			throw new IOException("Unable to open video decoder", ex);
		}
	}

	/**
	 * Called after the reader has seeked to a new position.
	 *
	 * Tells the codec to flush.
	 */
	public void postSeek() {
		clearQueue();
		queue.offer(flush);
	}

	void clearQueue() {
		AVPacket p;

		while ((p = queue.poll()) != null) {
			src.recyclePacket(p);
		}
	}

	/**
	 * The MediaReader invokes this on packets destined for this stream.
	 *
	 * @param packet
	 * @throws InterruptedException
	 */
	public void enqueuePacket(AVPacket packet) throws InterruptedException {
		queue.offer(packet);
	}

	@Override
	public void run() {
		long start = Debug.threadCpuTimeNanos();
		long time = System.currentTimeMillis();

		while (!cancelled) {
			AVPacket packet = null;
			try {
				packet = queue.take();

				if (packet == flush) {
					packet = null;
					cc.flushBuffers();
				} else if (packet == cancel) {
					packet = null;
					cancelled = true;
				} else {
					decodePacket(packet);
				}
			} catch (AVDecodingError ex) {
				Logger.getLogger(MediaDecoder.class.getName()).log(Level.SEVERE, null, ex);
			} catch (InterruptedException ex) {
			} catch (Exception ex) {
				Logger.getLogger(MediaDecoder.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (packet != null)
					src.recyclePacket(packet);
			}
		}

		start = (Debug.threadCpuTimeNanos() - start) / 1000;
		time = System.currentTimeMillis() - time;
		System.err.printf(getName() + " thread finished cpu time = %d.%06ds   real time = %d.%03ds\n", start / 1000000L, start % 1000000L, time / 1000, time % 1000);
	}

	/**
	 * Convert PTS to milliseconds relative to the start of the stream
	 *
	 * @param pts
	 * @return
	 */
	public long convertPTS(long pts) {
		return AVRational.starSlash(pts * 1000, tb_Num, tb_Den) - startms;
	}

	/**
	 * Initialise the media codec, if any is required.
	 *
	 * The default method starts the thread, so implementers should always
	 * pass up.
	 */
	public void init() {
		start();
	}

	/**
	 * Close the codec.
	 */
	@Override
	public void cancel() {
		System.out.println("Cancelling: " + this);
//		clearQueue();
		queue.offer(cancel);
		super.cancel();
		cc.close();
	}

	/**
	 * Pass a packet to the decoder. Decoders implement this by decoding the packet into
	 * a MediaFrame, and once that is complete, passing it to the media sink (this.dest).
	 *
	 * @param packet
	 * @throws AVDecodingError
	 * @throws InterruptedException
	 */
	abstract void decodePacket(AVPacket packet) throws AVDecodingError, InterruptedException;

	static public String timeToString(long time) {
		return String.format("%02d:%02d:%02d.%03d",
				time / 1000 / 60 / 60,
				time / 1000 / 60 % 60,
				time / 1000 % 60,
				time % 1000);
	}
}
