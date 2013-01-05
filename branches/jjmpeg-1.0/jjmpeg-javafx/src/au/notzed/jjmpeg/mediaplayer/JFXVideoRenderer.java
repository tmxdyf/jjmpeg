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
public class JFXVideoRenderer extends Pane {

	ImageView surface;
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
	// Frame throttling
	long lag;
	int dropped;
	int dropLimit = 8;
	int framesDropped = 0;
	//
	VideoScheduler scheduler;

	public JFXVideoRenderer() {
		surface = new ImageView();

		getChildren().add(surface);
	}

	public void start() {
		scheduler = new VideoScheduler();
		scheduler.start();
	}

	public void stop() {
		if (scheduler != null) {
			scheduler.cancel();
			scheduler = null;
		}
	}

	public void setVideoFormat(au.notzed.jjmpeg.PixelFormat fmt, int width, int height) {
		vwidth = width;
		vheight = height;

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
	// Frame scheduling
	long audioms;
	long audiosetms;
	long startms = -1;

	public synchronized void setAudioLocation(long ms) {
		audioms = ms;
		audiosetms = System.currentTimeMillis();
	}

	public synchronized long getClock(long now) {
		return (audioms + (now - audiosetms));
	}

	public synchronized long getDelay(long now, long pts) {
		return pts - (audioms + (now - audiosetms));
	}

	class JFXVideoFrame extends VideoFrame implements Runnable {

		WritableImage image;

		public JFXVideoFrame(int width, int height) {
			image = new WritableImage(width, height);
		}

		@Override
		public void setFrame(AVFrame frame) {
			// Just copy directly to writable image
			oscale.scale(frame, 0, vheight, oframe);
			image.getPixelWriter().setPixels(0, 0, vwidth, vheight, PixelFormat.getIntArgbPreInstance(), odata, vwidth);
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
			// Called to show the frame on GUI thread
			surface.setImage(image);
			// Need to keep this around until we're showing the next one
			if (current != null)
				current.recycle();
			current = this;
		}
	}

	class VideoScheduler extends CancellableThread {

		public VideoScheduler() {
			super("Video Scheduler");
		}

		@Override
		public void run() {
			while (!cancelled) {
				try {
					JFXVideoFrame peek;
					long delay;
					JFXVideoFrame displayNew = null;
					do {
						peek = ready.take();

						long pts = peek.pts;
						long targetms = pts + startms;
						long now = System.currentTimeMillis();

						now = getClock(now);

						if (startms == -1) {
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
						}

						if (delay < 0) {
							lag = -delay;
							System.out.println(" drop display, lagged: " + lag);
							// dump head
							ready.poll();
							if (displayNew != null)
								displayNew.recycle();
							displayNew = peek;
						} else {
							lag = 0;
						}
					} while (delay < 0);

					// Err, surely a better way to do this
					sleep(delay);

					Platform.runLater(peek);
				} catch (InterruptedException x) {
				}
			}
		}
	}
}