/*
 * Copyright (c) 2013 Michael Zucchi
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

import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.SwsContext;
import au.notzed.jjmpeg.util.CancellableThread;
import au.notzed.jjmpeg.util.JJQueue;
import java.nio.IntBuffer;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

/**
 *
 * @author notzed
 */
public class JFXVideoRenderer extends ImageView {

	ImageView surface;
	au.notzed.jjmpeg.PixelFormat ifmt;
	int vwidth, vheight;
	au.notzed.jjmpeg.PixelFormat ofmt = au.notzed.jjmpeg.PixelFormat.PIX_FMT_BGRA;
	SwsContext oscale;
	AVFrame oframe;
	IntBuffer odata;
	//
	final int NBUFFERS = 11;
	JJQueue<JFXVideoFrame> buffers = new JJQueue<JFXVideoFrame>(NBUFFERS);
	JJQueue<JFXVideoFrame> ready = new JJQueue<JFXVideoFrame>(NBUFFERS);
	JFXVideoFrame current;
	// Average frame delay - for interlace mode
	int frameDelay = 40;
	// Frame throttling
	long lag;
	int dropped;
	int dropLimit = 8;
	int framesDropped = 0;
	//
	VideoScheduler scheduler;
	DeinterlaceMode deinterlaceMode = DeinterlaceMode.Fields;

	enum DeinterlaceMode {

		/**
		 * Do nothing.
		 */
		None,
		/**
		 * Individual fields are shown.
		 */
		Fields,
		/**
		 * avpicture_deinterlace is used.
		 */
		Merge
	}

	public JFXVideoRenderer() {
		surface = this;

		//getChildren().add(surface);
	}

	public void setFrameDelay(int frameDelay) {
		if (frameDelay == -1)
			frameDelay = 40;
		this.frameDelay = frameDelay;
		System.out.println("Using field delay : " + (frameDelay/2));
	}

	public void start() {

		lag = 0;
		dropped = 0;
		startms = -1;
		videoms = 0;
		videosetms = 0;
		audioms = 0;
		audiosetms = 0;

		scheduler = new VideoScheduler();
		scheduler.start();
	}

	public void pause() {
		scheduler.pause();
	}

	public void unpause() {
		scheduler.unpause();
	}

	public long getPosition() {
		return getVideoClock(System.currentTimeMillis());
	}

	public void postSeek(long position) {
		scheduler.postSeek(position);
	}

	public void stop() {
		if (scheduler != null) {
			scheduler.cancel();
			scheduler = null;
		}
	}

	public void release() {
		stop();
		if (oscale != null)
			oscale.dispose();
		if (oframe != null)
			oframe.dispose();
	}

	public void setVideoFormat(au.notzed.jjmpeg.PixelFormat fmt, int width, int height) {
		vwidth = width;
		vheight = height;
		ifmt = fmt;

		if (oscale != null)
			oscale.dispose();
		if (oframe != null)
			oframe.dispose();
		oscale = SwsContext.create(width, height, fmt, width, height, ofmt, SwsContext.SWS_BILINEAR);
		oframe = AVFrame.create(ofmt, width, height);
		odata = oframe.getPlaneAt(0, ofmt, width, height).data.asIntBuffer();

		buffers.clear();
		ready.clear();

		// reset timestamp
		startms = -1;
		framesDropped = 0;
		lag = 0;

		for (int i = 0; i < NBUFFERS; i++) {
			JFXVideoFrame vf = new JFXVideoFrame(width, height);
			buffers.offer(vf);
			// Hack: make the image view the right size
			if (i == 0)
				surface.setImage(vf.image);
		}
	}

	public VideoFrame getFrame() throws InterruptedException {

		// It saves enough to catch up most of the time.
		if (lag > 100 && dropped < dropLimit) {
			dropped++;
			framesDropped++;
			System.out.println("lagged " + lag + ", dropping frame");
			return null;
		}
		dropped = 0;

		return buffers.take();
	}
	// Frame timestamp output, or synchronising to video
	// TODO: encapsulate in 'real time clock' object or somesuch
	long videoms;
	long videosetms;
	// Frame scheduling
	long audioms;
	long audiosetms;
	long startms = -1;

	private synchronized void setVideoLocation(long ms) {
		videoms = ms;
		videosetms = System.currentTimeMillis();
	}

	public synchronized long getVideoClock(long now) {
		return (videoms + (now - videosetms));
	}

	public synchronized void setAudioLocation(long ms) {
		// This stuff is to try to compensate for a poor implementation
		// of audio.getPlayPosition() and/or System.currentTimeMillis()
		long now = System.currentTimeMillis();
		long diff = Math.abs(getAudioClock(now) - ms);

		if (diff < 50)
			return;

		if (diff > 10) {
			System.out.println("audio clock jitter/drift: " + (getAudioClock(now) - ms));
		}
		audioms = ms;
		audiosetms = System.currentTimeMillis();
	}

	public synchronized long getAudioClock(long now) {
		return (audioms + (now - audiosetms));
	}

	class JFXVideoFrame extends VideoFrame implements Runnable {

		WritableImage image;
		WritableImage field0;
		WritableImage field1;
		boolean interlaced;
		boolean topFieldFirst;
		// Which field is being displayed next
		boolean firstField;

		public JFXVideoFrame(int width, int height) {
			image = new WritableImage(width, height);
			// For interlaced frames.
			field0 = new WritableImage(width, height / 2);
			field1 = new WritableImage(width, height / 2);
		}

		@Override
		public void setFrame(AVFrame frame) {

			interlaced = frame.isInterlacedFrame();
			topFieldFirst = frame.isTopFieldFirst();
			firstField = true;

			if (interlaced && deinterlaceMode == DeinterlaceMode.Merge) {
				int a= frame.deinterlace(frame, ifmt, vwidth, vheight);
				interlaced = false;
			}

			// Just copy directly to writable image
			oscale.scale(frame, 0, vheight, oframe);
			image.getPixelWriter().setPixels(0, 0, vwidth, vheight, PixelFormat.getIntArgbPreInstance(), odata, vwidth);

			if (interlaced) {
				field0.getPixelWriter().setPixels(0, 0, vwidth, vheight / 2, PixelFormat.getIntArgbPreInstance(), odata, vwidth * 2);
				odata.position(vwidth);
				field1.getPixelWriter().setPixels(0, 0, vwidth, vheight / 2, PixelFormat.getIntArgbPreInstance(), odata, vwidth * 2);
				odata.position(0);
			}
		}

		@Override
		public AVFrame getFrame() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		void enqueue() throws InterruptedException {
			ready.offer(this);
		}

		@Override
		void recycle() {
			buffers.offer(this);
		}

		@Override
		public void run() {
			// Must always be called twice on interleaved frames
			if (interlaced) {
				surface.setScaleY(2);
				// TODO: check this logic
				if (!(firstField ^ topFieldFirst)) {
					setVideoLocation(pts);
					surface.setImage(field0);
				} else {
					setVideoLocation(pts + frameDelay/2);
					surface.setImage(field1);
				}
				if (firstField) {
					firstField = false;
				} else {
					if (current != null)
						current.recycle();
					current = this;
				}
			} else {
				surface.setScaleY(1);
				setVideoLocation(pts);
				// Called to show the frame on GUI thread
				surface.setImage(image);
				// Need to keep this around until we're showing the next one
				if (current != null)
					current.recycle();
				current = this;
			}
		}
	}

	/**
	 * Retrieve a copy of the current image
	 * @return
	 */
	public WritableImage getCurrentFrame() {
		if (current == null)
			return null;

		WritableImage wi = new WritableImage(current.image.getPixelReader(), vwidth, vheight);

		return wi;
	}

	class VideoScheduler extends CancellableThread {

		public VideoScheduler() {
			super("Video Scheduler");
		}
		boolean paused = false;
		int seeked = 0;
		long seekms;

		public synchronized void postSeek(long ms) {
			seeked++;
			seekms = ms;
		}

		public synchronized void pause() {
			paused = true;
		}

		public synchronized void unpause() {
			paused = false;
			notify();
		}

		synchronized boolean checkPaused(long now) throws InterruptedException {
			// TODO: handle seeking within pause here too?
			if (seeked > 0) {
				seeked = 0;
				startms = now - seekms;
				System.out.println("post seek discard frames: " + ready.count());
				ready.drainTo(buffers);
			}

			boolean p = paused;
			while (paused) {
				wait();
			}

			return p;
		}

		@Override
		public void run() {
			while (!cancelled) {
				try {
					JFXVideoFrame peek;
					long delay;
					do {
						long now = System.currentTimeMillis();

						checkPaused(now);

						peek = ready.take();

						long pts = peek.pts;
						long targetms = pts + startms;

						now = getAudioClock(now);

						if (startms == -1) {
							System.out.println("reset startms, pts = " + pts);
							startms = now - pts;
							//startms = now - seekoffset;
							delay = 0;
						} else {
							delay = targetms - now;
						}

						// max speed
						//delay = -1;

						if (delay > 500) {
							System.out.println("weird delay " + delay + " pts " + peek.pts + " now = " + (now - startms));
							startms = -1;
							delay = 0;
						}

						if (delay < 0) {
							lag = -delay;
							System.out.println(" drop display pts " + pts + ", lagged: " + lag);
							// dump head
							peek.recycle();
						} else {
							lag = 0;
						}
					} while (delay < 0);

					// Err, surely a better way to do this
					sleep(delay);

					Platform.runLater(peek);

					// Interlaced: call it twice
					if (peek.interlaced) {
						sleep(frameDelay/2);
						Platform.runLater(peek);
					}
				} catch (InterruptedException x) {
				}
			}
		}
	}
}