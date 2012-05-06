package au.notzed.jjmpeg.util;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import au.notzed.jjmpeg.*;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVException;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.io.JJMediaReader;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderAudio;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderStream;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderVideo;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Attempts to use GL for display.
 *
 * TBH it's hardly much different for the display part.
 *
 * But the colour conversion is better.
 *
 * Also include rudimentary, unfinished, unsynchronised
 * sound stuff.
 *
 * @author notzed
 */
public class JJGLPlayer extends Activity {

	JJGLSurfaceView view;
	DecoderGL decoder;
	Throttle throttle;
	//
	Audio audio;
	AudioTrack track;
	//
	long startms;
	// frames ready for being used
	LinkedBlockingQueue<FrameData> recycle = new LinkedBlockingQueue<FrameData>();
	// frames queued for display
	LinkedBlockingQueue<FrameData> frames = new LinkedBlockingQueue<FrameData>();
	// same for audio frames
	LinkedBlockingQueue<AudioData> audioframes = new LinkedBlockingQueue<AudioData>();
	LinkedBlockingQueue<AudioData> audiorecycle = new LinkedBlockingQueue<AudioData>();
	//
	String filename;

	public JJGLPlayer() {
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DISPLAY_NAME};
		Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);

		if (cursor == null)
			return contentUri.getPath();

		cursor.moveToFirst();

		int path = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		int type = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
		int name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

		System.out.println("path " + path + ": " + cursor.getString(path));
		System.out.println("type " + type + ": " + cursor.getString(type));
		System.out.println("name " + name + ": " + cursor.getString(name));

		String res = cursor.getString(path);

		cursor.close();

		return res;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Intent it = getIntent();
		System.out.println("intent action = " + it.getAction());
		System.out.println("intent datas = " + it.getDataString());
		System.out.println("intent data  = " + it.getData());

		if (it.getData() != null) {
			if (it.getData().getScheme().equals("content"))
				filename = getRealPathFromURI(it.getData());
			else
				filename = it.getDataString();
		} else {
			filename = "/sdcard/trailer.mp4";
		}

		view = new JJGLSurfaceView(this);

		setContentView(view);

		throttle = new Throttle();
		decoder = new DecoderGL();
		audio = new Audio();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!throttle.isAlive()) {
			throttle.start();
			decoder.start();
			audio.start();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (track != null)
			track.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (track != null)
			track.play();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (track != null)
			track.stop();

		audioframes.clear();
		audiorecycle.clear();
		AudioData ad = new AudioData();
		ad.size = -1;
		audioframes.offer(ad);

		decoder.cancelled = true;
		frames.clear();
		recycle.clear();
		frames.offer(new FrameData(0, null, null));
		recycle.offer(new FrameData(0, null, null));
		try {
			decoder.interrupt();
			throttle.join();
			decoder.join();
			audio.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	class FrameData implements Runnable, JJFrame {

		long pts;
		AVFrame frame;
		AVPlane[] plane;
		// incoming frame before conversion
		AVFrame iframe;

		public FrameData(long pts, AVFrame frame, AVPlane[] plane) {
			this.pts = pts;
			this.frame = frame;
			this.plane = plane;
		}

		public void run() {
			// ""display frame""
			view.renderer.setFrame(this);
		}

		public void recycle() {
			vs.recycleFrame(frame);
		}

		public ByteBuffer getBuffer(int index) {
			return plane[index].data;
		}

		public int getLineSize(int index) {
			return plane[index].lineSize;
		}
	}

	class AudioData implements Runnable {

		short[] data = new short[8192 * 6];
		int size;

		public void setData(ShortBuffer src, int cc) {
			size = src.remaining();
			src.get(data, 0, size);

			if (cc > 2) {
				for (int i = 0; i < size / 6; i++) {
					data[i * 2 + 0] = data[i * 6 + 0];
					data[i * 2 + 1] = data[i * 6 + 1];
				}
				size = size / 6 * 2;
			}
		}

		public void run() {
			track.write(data, 0, size);
			audiorecycle.offer(this);
		}
	}

	class Audio extends Thread {

		public Audio() {
			super("Audio Play Thread");
		}

		@Override
		public void run() {
			try {
				while (true) {
					try {
						AudioData ad = audioframes.take();

						if (ad.size == -1)
							break;

						ad.run();
					} catch (InterruptedException ex) {
						Logger.getLogger(JJAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
					} finally {
					}
				}
			} finally {
				Log.i("jjmpeg", "Audio thread done");
			}
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
							//	Log.i("jjmpeg", "frame dropped, lag: " + delay);
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
	JJMediaReader mr = null;
	JJReaderVideo vs;
	JJReaderAudio as;
	int w;
	int h;
	PixelFormat fmt = PixelFormat.PIX_FMT_RGB565LE;

	// CPU thread for demux + decode, uses GL for colour conversion
	class DecoderGL extends Thread {

		boolean cancelled;
		// how many decoder buffers to use, this is the only buffering now
		static final int NDECODED = 3;
		// audioframes frames to buffer ahead.  Must be at least enough to fit betwen video frames
		static final int NAUDIO = 30;

		void open() throws AVIOException, AVException {
			mr = new JJMediaReader(filename);

			for (JJReaderStream rs : mr.getStreams()) {
				switch (rs.getType()) {
					case AVCodecContext.AVMEDIA_TYPE_AUDIO:
						if (as == null) {
							as = (JJReaderAudio) rs;
							as.open();
							Log.i("jjplayer", String.format("Found Audio: %dHz channels %d", as.getContext().getSampleRate(), as.getContext().getChannels()));
						}
						break;
					case AVCodecContext.AVMEDIA_TYPE_VIDEO:
						if (vs == null) {
							vs = (JJReaderVideo) rs;
							Log.i("jjplayer", String.format("Found Video: %dx%d fmt %s", vs.getWidth(), vs.getHeight(), vs.getPixelFormat()));
						}
				}
			}

			if (vs != null) {
				// skip b frames
				//vs.getContext().setSkipFrame(16);
				vs.getContext().setThreadCount(4);

				vs.setFrameCount(NDECODED);
				vs.open();

				Log.i("jjplayer", String.format("Opened Video: %dx%d fmt %s", vs.getWidth(), vs.getHeight(), vs.getPixelFormat()));

				w = vs.getWidth();
				h = vs.getHeight();
			}

			// Setup audio streams
			if (as != null) {
				for (int i = 0; i < NAUDIO; i++) {
					audiorecycle.add(new AudioData());
				}

				AVCodecContext cc = as.getContext();
				track = new AudioTrack(AudioManager.STREAM_MUSIC, cc.getSampleRate(),
						cc.getChannels() >= 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, 8192 * 2, AudioTrack.MODE_STREAM);
				track.play();
			}
		}

		@Override
		public void run() {
			long busy = 0;
			long start = System.currentTimeMillis();
			try {
				open();

				view.renderer.setVideoSize(w, h);

				while (!cancelled) {
					long now = System.currentTimeMillis();
					JJReaderStream rs;

					rs = mr.readFrame();
					if (rs == null)
						break;

					if (rs.equals(vs)) {
						AVFrame iframe = vs.getFrame();
						AVPlane[] planes = {
							iframe.getPlaneAt(0, vs.getPixelFormat(), w, h),
							iframe.getPlaneAt(1, vs.getPixelFormat(), w, h),
							iframe.getPlaneAt(2, vs.getPixelFormat(), w, h)
						};

						FrameData fd = new FrameData(vs.convertPTS(mr.getPTS()), iframe, planes);

						busy += System.currentTimeMillis() - now;

						fd.pts = vs.convertPTS(mr.getPTS());
						fd.iframe = vs.getFrame();

						frames.offer(fd);
					} else if (rs.equals(as)) {
						AVSamples samples;
						try {
							while ((samples = as.getSamples()) != null) {
								AudioData ad = audiorecycle.take();

								ad.setData((ShortBuffer) samples.getSamples(), as.getContext().getChannels());

								audioframes.offer(ad);
							}
						} catch (AVDecodingError ex) {
							// ignore audio decoding errors
							Log.i("jjmpeg", String.format("Audio decode error ignored: " + ex.getLocalizedMessage()));
						}
					}
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} catch (AVIOException ex) {
				Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} catch (AVException ex) {
				Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (mr != null)
					mr.dispose();
				frames.offer(new FrameData(0, null, null));

				start = System.currentTimeMillis() - start;
				Log.i("jjmpeg", String.format("Demux/decoder thread busy=%d.%03ds total: %d.%03ds", busy / 1000, busy % 1000, start / 1000, start % 1000));
			}
		}
	}
}
