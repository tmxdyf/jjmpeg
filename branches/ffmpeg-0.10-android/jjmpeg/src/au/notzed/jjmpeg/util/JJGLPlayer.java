package au.notzed.jjmpeg.util;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import au.notzed.jjmpeg.*;
import au.notzed.jjmpeg.exception.AVException;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.exception.AVInvalidStreamException;
import au.notzed.jjmpeg.io.JJMediaReader;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderStream;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderVideo;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Attempts to use GL for display.
 *
 * TBH it's hardly much different for the display part.
 *
 * But it adds a separate CPU thread for the colour conversion,
 * which allows it to run a bit faster.
 * 
 * @author notzed
 */
public class JJGLPlayer extends Activity {

	JJGLSurfaceView view;
	Decoder decoder;
	Converter converter;
	Throttle throttle;
	//
	long startms;
	// frames ready for being used
	LinkedBlockingQueue<FrameData> recycle = new LinkedBlockingQueue<FrameData>();
	// source frames to be converted.
	LinkedBlockingQueue<FrameData> convert = new LinkedBlockingQueue<FrameData>();
	LinkedBlockingQueue<FrameData> frames = new LinkedBlockingQueue<FrameData>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		view = new JJGLSurfaceView(this);

		setContentView(view);

		throttle = new Throttle();
		decoder = new Decoder();
		converter = new Converter();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!throttle.isAlive()) {
			throttle.start();
			converter.start();
			decoder.start();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		frames.clear();
		recycle.clear();
		convert.clear();
		frames.offer(new FrameData(0, null, null));
		recycle.offer(new FrameData(0, null, null));
		convert.offer(new FrameData(0, null, null));
		try {
			throttle.join();
			decoder.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	class FrameData implements Runnable, JJFrame {

		long pts;
		AVFrame frame;
		AVPlane plane;
		// incoming frame before conversion
		AVFrame iframe;

		public FrameData(long pts, AVFrame frame, AVPlane plane) {
			this.pts = pts;
			this.frame = frame;
			this.plane = plane;
		}

		public void run() {
			// ""display frame""
			view.renderer.setFrame(this);
		}

		public void convert() {
			scale.scale(iframe, 0, plane.height, frame);
		}

		public void recycle() {
			recycle.offer(this);
		}

		public ByteBuffer getBuffer() {
			return plane.data;
		}
	}

	/**
	 * Displays frames at some rate
	 */
	class Throttle extends Thread {

		@Override
		public void run() {
			long startpts = -1;
			long sleep = 0;
			long startms = -1;

			try {
				while (true) {
					try {
						FrameData fd = frames.take();

						if (fd.frame == null)
							break;

						// fix streams that don't start at 0
						if (startpts == -1) {
							startpts = fd.pts;
						}

						long pts = fd.pts - startpts;
						long targetms = pts + startms;
						long now = System.currentTimeMillis();

						long delay;

						if (startms == -1) {
							startms = now - pts;
							//startms = now - seekoffset;
							delay = 0;
						} else {
							delay = targetms - now;
						}


						//delay = 0;

						if (delay >= 0) {
							sleep += delay;
							Thread.sleep(delay);
							//view.post(fd);
						} else {
							Log.i("jjmpeg", "frame dropped, lag: " + delay);
							//recycle.offer(fd);
							//view.post(fd);
						}
						fd.run();
					} catch (InterruptedException ex) {
						Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} finally {
				Log.i("jjmpeg", String.format("Throttle thread stopped, total sleep=%d.%03ds\n", sleep / 1000, sleep % 1000));
			}
		}
	}
	SwsContext scale;
	JJMediaReader mr = null;
	JJReaderVideo vs;
	int w;
	int h;
	PixelFormat fmt = PixelFormat.PIX_FMT_RGBA;

	// CPU thread for conversion
	class Converter extends Thread {

		@Override
		public void run() {
			long btime = 0;

			try {
				while (true) {
					try {
						FrameData fd = convert.take();

						if (fd.frame == null)
							break;

						long now = System.currentTimeMillis();

						fd.convert();

						btime += System.currentTimeMillis() - now;

						vs.recycleFrame(fd.iframe);
						fd.iframe = null;

						frames.offer(fd);
						//fd.recycle();
					} catch (InterruptedException ex) {
						Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} finally {
				frames.offer(new FrameData(0, null, null));

				Log.i("jjmpeg", String.format("Converter thread stopped: busy = %d.%03ds", btime / 1000, btime % 1000));
			}
		}
	}

	// CPU thread for demux + decode.
	class Decoder extends Thread {

		// how many decoded frames to 'buffer' ahead of time (assiv we'd ever get that far ahead)
		static final int NFRAMES = 10;
		// how many decoder buffers to use, only needs to be 2
		static final int NDECODED = 2;

		void open() throws AVIOException, AVException {
			//mr = new JJMediaReader("/sdcard/bbb.mov");
			//mr = new JJMediaReader("/sdcard/trailer.mp4");
			mr = new JJMediaReader("/sdcard/yard.mp4");

			for (JJReaderStream rs : mr.getStreams()) {
				if (rs.getType() == AVCodecContext.AVMEDIA_TYPE_VIDEO) {
					vs = (JJReaderVideo) rs;
					break;
				}
			}

			if (vs == null)
				throw new AVInvalidStreamException("No streams");

			vs.setFrameCount(NDECODED);
			vs.open();

			Log.i("jjplayer", String.format("Opened Video: %dx%d fmt %s", vs.getWidth(), vs.getHeight(), vs.getPixelFormat()));

			w = vs.getWidth();
			h = vs.getHeight();

			scale = SwsContext.create(w, h, vs.getPixelFormat(), w, h, fmt, SwsContext.SWS_BILINEAR);

			for (int i = 0; i < NFRAMES; i++) {
				AVFrame frame;

				frame = AVFrame.create(fmt, w, h);

				recycle.add(new FrameData(0, frame, frame.getPlaneAt(0, fmt, w, h)));
			}

			vs.setOutputFormat(fmt, w, h);
		}

		@Override
		public void run() {
			long busy = 0;
			long start = System.currentTimeMillis();
			try {
				open();

				view.renderer.setVideoSize(w, h);

				while (true) {
					try {
						FrameData fd = recycle.take();

						if (fd.frame == null)
							break;

						long now = System.currentTimeMillis();

						if (mr.readFrame() == null)
							break;

						busy += System.currentTimeMillis() - now;

						fd.pts = vs.convertPTS(mr.getPTS());
						fd.iframe = vs.getFrame();

						convert.offer(fd);
					} catch (InterruptedException ex) {
						Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} catch (AVIOException ex) {
				Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} catch (AVException ex) {
				Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (mr != null)
					mr.dispose();
				convert.offer(new FrameData(0, null, null));

				start = System.currentTimeMillis() - start;
				Log.i("jjmpeg", String.format("Demux/decoder thread busy=%d.%03ds total: %d.%03ds", busy / 1000, busy % 1000, start / 1000, start % 1000));
			}
		}
	}
}
