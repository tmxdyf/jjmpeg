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

import au.notzed.jjmpeg.util.CancellableThread;
import au.notzed.jjmpeg.AVCodec;
import au.notzed.jjmpeg.AVCodecContext;
import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.util.JJQueue;
import java.io.IOException;
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
	JJQueue<MediaPacket> queue;
	//ArrayBlockingQueue<AVPacket> queue;
	JJQueue<MediaPacket> syncQueue = new JJQueue<MediaPacket>(1);

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
	MediaDecoder(String name, MediaReader src, MediaSink dest, AVStream stream, int streamid) throws IOException {
		super(name);

		queue = new JJQueue<MediaPacket>(MediaReader.packetLimit + 1);
		//queue = new ArrayBlockingQueue<AVPacket>(MediaReader.packetLimit + 1);
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
			startms = AVRational.rescale(startpts * 1000, tb_Num, tb_Den);
			duration = AVRational.rescale(stream.getDuration() * 1000, tb_Num, tb_Den);

			System.out.println("stream start " + startms + " length " + duration);
		} catch (AVIOException ex) {
			throw new IOException("Unable to open video decoder", ex);
		}
	}
	int flushcount;

	/**
	 * Called after the reader has seeked to a new position.
	 *
	 * Tells the codec to flush, waits for it to flush.
	 */
	public synchronized void postSeek() throws InterruptedException {
		// Don't need this: handled by sequence numbers
		if (true)
			return;

		flushcount++;
		clearQueue();
		queue.offer(MediaPacket.flush);
		//wait();
	}

	void clearQueue() {
		MediaPacket p;

		while ((p = queue.poll()) != null) {
			if (p != MediaPacket.flush) {
				src.recyclePacket(p);
			} else {
				System.out.println("** discarding flush");
			}
		}
	}

	/**
	 * The MediaReader invokes this on packets destined for this stream.
	 *
	 * @param packet
	 * @throws InterruptedException
	 */
	public void enqueuePacket(MediaPacket packet) throws InterruptedException {
		queue.offer(packet);
	}

	/**
	 * Invoked to flush the codec, sub-classes that need to reset processing
	 * should hook into this.  Runs on decoder thread.
	 */
	protected void flushCodec() {
		cc.flushBuffers();
	}

	/**
	 * Decoders call this to find out if flushing is in progress,
	 * if so they should stop decoding and not output any frames.
	 *
	 * Must be called with the this monitor held.
	 * @return
	 */
	protected boolean isFlushing() {
		return flushcount > 0;
	}

	/**
	 * Main decoder thread, take packets from demux thread
	 * and send them to be decoded.
	 *
	 * It's getting a bit messy handling flush and so on.
	 */
	@Override
	public void run() {
		int lastSequence = 0;
		while (!cancelled) {
			MediaPacket packet = null;
			try {
				if (cancelled)
					break;

				packet = queue.take();

				if (packet == MediaPacket.flush) {
					System.out.println("loop got flush packet");
					packet = null;
					flushCodec();
					synchronized (this) {
						flushcount--;
						notify();
					}
				} else if (packet == MediaPacket.cancel) {
					packet = null;
					cancelled = true;
				} else {
					if (packet.sequence != src.getMediaClock().getSequence()) {
						System.out.println(getName() + " sequence changed, flushing packet");
						continue;
					}

					if (packet.sequence != lastSequence) {
						System.out.println(getName() + "sequence changed, flushing codec");
						flushCodec();
						lastSequence = packet.sequence;
					}

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
		System.out.println("Decoding done " + getName());
	}

	/**
	 * Convert PTS to milliseconds relative to the start of the stream
	 *
	 * @param pts
	 * @return
	 */
	public long convertPTS(long pts) {
		return AVRational.rescale(pts * 1000, tb_Num, tb_Den) - startms;
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
	 * Flush cleanly and close
	 */
	public void complete() {
		queue.offer(MediaPacket.cancel);
		try {
			join();
		} catch (InterruptedException ex) {
		}
	}

	/**
	 * Close the codec, forced immediate.
	 */
	@Override
	public void cancel() {
		System.out.println("Cancelling: " + this);
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
	abstract void decodePacket(MediaPacket packet) throws AVDecodingError, InterruptedException;

	static public String timeToString(long time) {
		return String.format("%02d:%02d:%02d.%03d",
				time / 1000 / 60 / 60,
				time / 1000 / 60 % 60,
				time / 1000 % 60,
				time % 1000);
	}
}
