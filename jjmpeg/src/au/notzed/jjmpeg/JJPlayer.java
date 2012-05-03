package au.notzed.jjmpeg;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import au.notzed.jjmpeg.exception.AVException;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.io.JJMediaReader;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderVideo;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Really slow video player just to see if
 * the api is binding properly.
 *
 * @author notzed
 */
public class JJPlayer extends Activity {

	ImageView iview;
	Throttle throttle;
	Decoder decoder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		iview = (ImageView) findViewById(R.id.image);

		throttle = new Throttle();
		decoder = new Decoder();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!throttle.isAlive()) {
			throttle.start();
			decoder.start();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		frames.clear();
		recycle.clear();
		frames.offer(new FrameData(0, null));
		recycle.offer(new FrameData(0, null));
		try {
			throttle.join();
			decoder.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	class FrameData implements Runnable {

		long pts;
		Bitmap bm;

		public FrameData(long pts, Bitmap bm) {
			this.pts = pts;
			this.bm = bm;
		}

		public void run() {
			iview.setImageBitmap(bm);
			recycle.offer(this);
		}
	}
	long startms;
	LinkedBlockingQueue<FrameData> frames = new LinkedBlockingQueue<FrameData>();
	LinkedBlockingQueue<FrameData> recycle = new LinkedBlockingQueue<FrameData>();

	/**
	 * Displays frames at some rate
	 */
	class Throttle extends Thread {

		@Override
		public void run() {
			long startpts = -1;
			startms = System.currentTimeMillis();

			try {
				while (true) {
					try {
						FrameData fd = frames.take();

						if (fd.bm == null)
							break;

						// fix streams that don't start at 0
						if (startpts == -1)
							startpts = fd.pts;

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

						if (delay >= 0) {
							Thread.sleep(delay);
							iview.post(fd);
						} else {
							//Log.i("jjmpeg", "frame dropped, lag: " + delay);
							//recycle.offer(fd);
							iview.post(fd);
						}
					} catch (InterruptedException ex) {
						Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} finally {
				recycle.offer(new FrameData(0, null));
				Log.i("jjmpeg", "Test throttle thread stopped");
			}
		}
	}

	class Decoder extends Thread {

		JJMediaReader mr = null;
		JJReaderVideo vs;
		int w;
		int h;
		PixelFormat fmt = PixelFormat.PIX_FMT_RGBA;
		// how many frames to 'buffer' ahead of time (assiv we'd ever get that far ahead)
		static final int NFRAMES = 10;

		void open() throws AVIOException, AVException {
			//mr = new JJMediaReader("/sdcard/bbb.mov");
			mr = new JJMediaReader("/sdcard/trailer.mp4");
			vs = mr.openFirstVideoStream();

			Log.i("jjplayer", String.format("Opened Video: %dx%d fmt %s", vs.getWidth(), vs.getHeight(), vs.getPixelFormat()));

			w = vs.getWidth();
			h = vs.getHeight();

			for (int i = 0; i < NFRAMES; i++) {
				Bitmap bm = Bitmap.createBitmap(w, h, Config.ARGB_8888);
				recycle.add(new FrameData(0, bm));
			}

			vs.setOutputFormat(fmt, w, h);
		}

		@Override
		public void run() {
			try {
				open();

				while (true) {
					try {
						FrameData fd = recycle.take();

						if (fd.bm == null)
							break;

						if (mr.readFrame() == null)
							break;

						AVFrame frame = vs.getOutputFrame();
						AVPlane plane = frame.getPlaneAt(0, fmt, w, h);

						fd.bm.copyPixelsFromBuffer(plane.data);
						fd.pts = vs.convertPTS(mr.getPTS());

						frames.offer(fd);
					} catch (InterruptedException ex) {
						Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} catch (AVIOException ex) {
				Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} catch (AVException ex) {
				Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (mr != null)
					mr.dispose();
				frames.offer(new FrameData(0, null));
				Log.i("jjmpeg", "Test decoder thread stopped");
			}
		}
	}
}
