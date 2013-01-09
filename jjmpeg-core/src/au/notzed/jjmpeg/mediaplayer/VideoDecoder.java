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

import au.notzed.jjmpeg.AVDiscard;
import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.exception.AVDecodingError;
import java.io.IOException;

/**
 * Takes AV packets and decodes video from them.
 * @author notzed
 */
public class VideoDecoder extends MediaDecoder {

	int width;
	int height;
	PixelFormat format;
	AVFrame frame;
	// FIXME: depends on impelementation
	final static boolean enqueueFrames = false;
	long decodeTime;

	/**
	 * Create a new video decoder for a given stream.
	 *
	 * @param src where encoded frames come from
	 * @param dest where decoded frames are sent
	 * @param stream stream information
	 * @param streamid corresponding stream id
	 * @throws IOException
	 */
	VideoDecoder(MediaReader src, MediaSink dest, AVStream stream, int streamid) throws IOException {
		super("VideoDecoder", src, dest, stream, streamid);

		// init some local stuff like picture frames
		height = cc.getHeight();
		width = cc.getWidth();
		format = cc.getPixFmt();
		frame = AVFrame.create();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	//public void setOutputSize(int swidth, int sheight) {
	//	scale.dispose();
	//	this.swidth = swidth;
	//	this.sheight = sheight;
	//	scale = SwsContext.create(width, height, format, swidth, sheight, PixelFormat.PIX_FMT_BGR24, SwsContext.SWS_BILINEAR);
	//}
	VideoFrame videoFrame;

	@Override
	public synchronized void postSeek() {
		super.postSeek();
	}

	@Override
	protected void flushCodec() {
		super.flushCodec();
		if (enqueueFrames) {
			if (videoFrame != null) {
				videoFrame.recycle();
				videoFrame = null;
			}
		}
	}
	/**
	 * Needs to be the same as the drop limit for the video output,
	 * so we can guess the true frame drop rate.
	 */
	int dropLimit = 8;
	/**
	 * Number of times we missed at this state
	 */
	int throttleMiss = 0;
	/**
	 * Throttle state - increments as we miss too many frames.
	 */
	int throttleState = 1;
	/**
	 * How many frames to miss in a row before jumping up a state.
	 */
	int throttleThreshold = 200;
	int frameCount;

	void decodePacket(AVPacket packet) throws AVDecodingError, InterruptedException {
		//System.out.println("video decode packet()");
		boolean frameFinished;
		do {
			// Experimental alternative - just queue up AVFrames and load textures in display callback
			if (enqueueFrames) {
				if (videoFrame == null) {
					videoFrame = dest.getVideoFrame();

					// This doesn't make sense - we want to drop frames by not decoding them when
					// too busy, not when not busy enough.
					if (videoFrame == null) {
						System.out.println("frame dropped");
						return;
					}

					frame = videoFrame.getFrame();
				}
			}


			long now = System.nanoTime();

			frameFinished = cc.decodeVideo(frame, packet);

			decodeTime += (System.nanoTime() - now) / 1000;

			if (frameFinished) {
				if (!enqueueFrames) {
					videoFrame = dest.getVideoFrame();
					// Allow for throttling
					if (videoFrame == null) {

						// Experiemental: try dropping decoding too if we can't keep up
						// It looks ugly but there's no alternative.
						throttleMiss++;
						if (throttleMiss > throttleThreshold
								&& throttleState < AVDiscard.values.length - 2) {
							throttleState += 1;
							throttleMiss = 0;
							cc.setSkipFrame(AVDiscard.values[throttleState]);
							System.out.println("CPU too slow, jumping throttle state: " + throttleState);
						}
						frameCount = 0;
						return;
					}
					// Need a couple of frames in a row to count as a reset
					if (frameCount > 0)
						throttleMiss = 0;
					frameCount += 1;
					videoFrame.setFrame(frame);
				}

				//videoFrame.pts = convertPTS(packet.getDTS());
				videoFrame.pts = convertPTS(frame.getBestEffortTimestamp());
				videoFrame.decodeTime = decodeTime;
				videoFrame.enqueue();

				if (enqueueFrames) {
					videoFrame = null;
				}
				decodeTime = 0;
			}
		} while (packet.getSize() == 0 && frameFinished);
	}
}
