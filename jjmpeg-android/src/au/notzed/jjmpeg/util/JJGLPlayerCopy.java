package au.notzed.jjmpeg.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.SeekBar.OnSeekBarChangeListener;
import au.notzed.jjmpeg.*;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVException;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.io.JJMediaReader;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderAudio;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderStream;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderVideo;
import java.nio.ShortBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a really nasty hacked up player full of spaghetti code
 * and nastiness.
 *
 * Still experimenting to find out what works and what doesn't.
 *
 * This version copies the frames to a new frame as the buffering mechanism.
 *
 * @author notzed
 */
public class JJGLPlayerCopy extends Activity {

	boolean videoout = true;
	boolean videodecode = true;
	boolean audioout = false;
	boolean audiodecode = false;
	JJGLSurfaceView view;
	SeekBar seek;
	DecoderGL decoder;
	Throttle throttle;
	//
	Audio audio;
	AudioTrack track;
	//
	long startms;
	// commands (seek, etc)
	LinkedBlockingQueue<Command> commands = new LinkedBlockingQueue<Command>();
	// frames ready for being used
	//LinkedBlockingQueue<FrameData> recycle = new LinkedBlockingQueue<FrameData>();
	//ArrayBlockingQueue<FrameData> recycle = new ArrayBlockingQueue<FrameData>(NDECODED + 1);
	// frames queued for display
	//LinkedBlockingQueue<FrameData> frames = new LinkedBlockingQueue<FrameData>();
	//ArrayBlockingQueue<FrameData> frames = new ArrayBlockingQueue<FrameData>(NDECODED + 1);
	JJQueue<FrameData> frames = new JJQueue<FrameData>(16);
	JJQueue<FrameData> recycle = new JJQueue<FrameData>(16);
	// same for audio frames
	//LinkedBlockingQueue<AudioData> audioframes = new LinkedBlockingQueue<AudioData>();
	//LinkedBlockingQueue<AudioData> audiorecycle = new LinkedBlockingQueue<AudioData>();
	JJQueue<AudioData> audioframes = new JJQueue<AudioData>(32);
	JJQueue<AudioData> audiorecycle = new JJQueue<AudioData>(32);
	//
	String filename;

	public JJGLPlayerCopy() {
	}

	// blocks on offer if full
	static class Queue2<T> {

		Object[] data;
		int head = 0;
		int tail = 0;
		final int mask;

		public Queue2(int size) {
			data = new Object[size];
			mask = size - 1;
		}

		synchronized public T take() throws InterruptedException {
			// empty queue
			while (head == tail) {
				wait();
			}

			// if it's full, send out a notify
			int next = (tail + 1) & mask;
			boolean notify = next == head;

			Object o = data[head];
			head = (head + 1) & mask;

			if (notify)
				notify();

			return (T) o;
		}

		synchronized public void offer(T o) throws InterruptedException {
			int next = (tail + 1) & mask;

			while (head == next) {
				wait();
				next = (tail + 1) & mask;
			}

			boolean notify = head == tail;

			data[tail] = o;
			tail = next;
			if (notify)
				notify();
		}

		synchronized void clear() {
			head = tail = 0;
		}

		synchronized public T poll() {
			if (head == tail)
				return null;
			return (T) data[head];
		}

		synchronized public void drainTo(Queue<T> dst) {
			while (head != tail) {
				dst.offer((T) data[head]);
				head = (head + 1) & mask;
			}
		}
	}

	static public String getRealPathFromURI(Context ctx, Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DISPLAY_NAME};
		Cursor cursor = ctx.getContentResolver().query(contentUri, proj, null, null, null);

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
	//
	final static String[] skip = {"Skip None", "Skip Noref", "Skip Bidir", "Skip Nokey"};
	final static int[] skip_val = {-16, 8, 16, 32};
	final static String[] threads = {"1 thread", "2 threads", "3 threads", "4 threads"};

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
				filename = getRealPathFromURI(this, it.getData());
			else
				filename = it.getDataString();
		} else {
			filename = "/sdcard/Download/long.avi";
		}

		FrameLayout frame = new FrameLayout(this);

		view = new JJGLSurfaceView(this);
		frame.addView(view);

		LinearLayout ll = new LinearLayout(this);
		frame.addView(ll);

		Spinner sp = new Spinner(this);

		sp.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_item, skip));
		sp.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View iview, int position, long id) {
				int skip = skip_val[position];

				if (vs != null) {
					vs.getContext().setSkipFrame(skip);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		ll.addView(sp);

		sp = new Spinner(this);
		sp.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_item, threads));
		sp.setSelection(3);
		sp.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View iview, int position, long id) {
				if (vs != null) {
					vs.getContext().setThreadCount(position + 1);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		ll.addView(sp);

		seek = new SeekBar(this);

		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				seekTo(progress);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				fingerdown = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				fingerdown = false;
			}
		});

		FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
		frame.addView(seek, fp);

		if (false) {
			setContentView(frame);
		} else {
			TextView tv = new TextView(this);
			tv.setText("no display");
			setContentView(tv);
		}

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
		frames.offer(new FrameData(0, null));
		recycle.offer(new FrameData(0, null));
		try {
			decoder.interrupt();
			throttle.join();
			decoder.join();
			audio.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
		}

		AVNative.check();
	}
	boolean inreport = false;
	boolean fingerdown = false;

	void seekTo(long ms) {
		if (!inreport) {
			commands.offer(new Command(Command.SEEK, ms));
		}
	}

	// callback for file state
	void fileOpened(String path, long duration) {
		System.out.println("file opened, duration = " + duration);
		if (duration == 0)
			duration = 60000;
		seek.setMax((int) duration);
	}

	void positionChanged(long position) {
		if (!fingerdown) {
			inreport = true;
			seek.setProgress((int) position);
			inreport = false;
		}
	}

	static class Command {

		int cmd;
		String file;
		long position;
		static final int OPEN = 1;
		static final int PLAY = 2;
		static final int PAUSE = 3;
		static final int SEEK = 4;
		static final int QUIT = 5;

		public Command(int cmd, long position) {
			this.cmd = cmd;
			this.position = position;
		}

		public Command(int cmd, String file) {
			this.cmd = cmd;
			this.file = file;
		}
	}

	class FrameData implements Runnable, JJFrame {

		long pts;
		AVFrame frame;

		public FrameData(long pts, AVFrame frame) {
			this.pts = pts;
			this.frame = frame;
		}

		public void run() {
			// ""display frame""
			view.renderer.setFrame(this);
			//view.renderer.setFrameDirect(this);
			//recycle.offer(this);
		}

		public void recycle() {
//			vs.recycleFrame(frame);
			//this.frame = null;
			recycle.offer(this);
		}

		public AVFrame getFrame() {
			return frame;
		}

		public PixelFormat getFormat() {
			return fmt;
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

		void fix(int cc) {
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
			} catch (Throwable ex) {
				Logger.getLogger(JJAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				Log.i("jjmpeg", "Audio thread done");
			}
		}
	}

	/**
	 * Displays frames at some rate
	 */
	class Throttle extends Thread {

		long startpts = -1;
		long startms = -1;

		public Throttle() {
			super("jjmpeg: throttle");
		}

		void postSeek(long ms) {
			startms = -1;
		}

		@Override
		public void run() {
			long sleep = 0;
			long thread = Debug.threadCpuTimeNanos();
			int count = 0;
			try {
				while (true) {
					try {
						final FrameData fd = frames.take();

						if (fd.frame == null) {
							System.out.println("Throttle thread exit invoked");
							break;
						}

						// fix streams that don't start at 0
						if (startpts == -1) {
							startpts = fd.pts;
						}

						long pts = fd.pts;// - startpts;
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

						//delay = -1;

						if (delay >= 0) {
							sleep += delay;

							if (delay > 100) {
								System.out.println("waiting for frame: " + delay);
							}

							Thread.sleep(delay);
							//view.post(fd);
						} else {
							//Log.i("jjmpeg", "frame lag: " + delay);
							//recycle.offer(fd);
							//view.post(fd);
						}

						//fd.run();
						fd.recycle();

						// indicate frame shown
						postSeekFrame = false;

						count++;
						if ((count & 7) == 0) {
							// report location
							runOnUiThread(new Runnable() {

								public void run() {
									positionChanged(fd.pts);
								}
							});
						}

					} catch (InterruptedException ex) {
						Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} catch (Throwable ex) {
				Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				Log.i("jjmpeg", String.format("Throttle thread stopped, total sleep=%d.%03ds\n", sleep / 1000, sleep % 1000));
				thread = (Debug.threadCpuTimeNanos() - thread) / 1000L;
				Log.i("jjmpeg", String.format("Demux/decoder thread time=%d.%06ds", thread / 1000000L, thread % 1000000L));
			}
		}
	}
	JJMediaReader mr = null;
	JJReaderVideo vs;
	JJReaderAudio as;
	int w;
	int h;
	PixelFormat fmt;
	static final boolean enableaudio = true;
	static final boolean enablevideo = true;
	// If true, then output at least 1 video frame after a seek to make it feel more responsive
	boolean postSeekFrame = false;
	// how many decoder buffers to use, this is the only buffering
	static final int NDECODED = 5;
	// audioframes frames to buffer ahead.  Must be at least enough to fit betwen video frames
	static final int NAUDIO = 30;

	int roundup(int v, int m) {
		return (v + m - 1) & ~(m - 1);
	}

	// CPU thread for demux + decode, uses GL for colour conversion
	class DecoderGL extends Thread {

		public DecoderGL() {
			super("jjmpeg: decoder");
		}
		boolean cancelled;
		//
		long duration;

		void open() throws AVIOException, AVException {
			mr = new JJMediaReader(filename);

			for (JJReaderStream rs : mr.getStreams()) {
				switch (rs.getType()) {
					case AVCodecContext.AVMEDIA_TYPE_AUDIO:
						if (enableaudio && as == null) {
							as = (JJReaderAudio) rs;
							as.open();
							Log.i("jjplayer", String.format("Found Audio: %dHz channels %d", as.getContext().getSampleRate(), as.getContext().getChannels()));
						}
						break;
					case AVCodecContext.AVMEDIA_TYPE_VIDEO:
						if (enablevideo && vs == null) {
							vs = (JJReaderVideo) rs;
						}
						break;
				}
			}
			AVInputFormat avif = mr.getFormat().getInputFormat();
			int flags = avif.getFlags();
			if ((flags & AVFormat.AVFMT_NO_BYTE_SEEK) != 0) {
				System.out.println("No byte seek allowed");
			} else {
				System.out.println("Byte seek allowed");
			}

			if (vs != null) {
				// skip b frames
				//vs.getContext().setSkipFrame(16);
				vs.getContext().setThreadCount(4);

				vs.setFrameCount(1);
				vs.open();

				Log.i("jjplayer", String.format("Opened Video: %dx%d fmt %s", vs.getWidth(), vs.getHeight(), vs.getPixelFormat()));

				w = vs.getWidth();
				h = vs.getHeight();
				fmt = vs.getPixelFormat();
				duration = vs.getDurationMS();

				long pduration = vs.getDurationCalc();

				Log.i("jjplayer", "Duraton = " + duration + " post-open eudration = " + pduration);

				for (int i = 0; i < NDECODED; i++) {
					AVFrame frame = AVFrame.create(fmt, roundup(w, 128), h);
					recycle.offer(new FrameData(0, frame));
				}

				vs.getContext().debug();
			}

			// Setup audio streams
			if (as != null) {
				for (int i = 0; i < NAUDIO; i++) {
					audiorecycle.offer(new AudioData());
				}

				AVCodecContext cc = as.getContext();
				track = new AudioTrack(AudioManager.STREAM_MUSIC, cc.getSampleRate(),
						cc.getChannels() >= 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, 8192 * 2, AudioTrack.MODE_STREAM);
				if (audioout && audiodecode)
					track.play();
				if (duration == 0)
					duration = as.getDurationMS();
			}

			runOnUiThread(new Runnable() {

				public void run() {
					fileOpened(filename, duration);
				}
			});
		}

		@Override
		public void run() {
			long busy = 0;
			long start = System.currentTimeMillis();
			long loadtext = 0;
			long thread = Debug.threadCpuTimeNanos();

			try {
				open();
				Command seekCmd = null;

				view.renderer.setVideoSize(w, h);

				while (!cancelled) {
					long now = System.currentTimeMillis();
					JJReaderStream rs;

					// Collapse incoming commands here
					Command cmd;

					while ((cmd = commands.poll()) != null) {
						switch (cmd.cmd) {
							case Command.SEEK:
								seekCmd = cmd;
								break;
						}
					}

					if (!postSeekFrame && seekCmd != null && seekCmd.position > 0) {
						Log.i("jjplayer", "Seek to: " + seekCmd.position);

						audioframes.drainTo(audiorecycle);
						FrameData fd;
						while ((fd = frames.poll()) != null) {
							fd.recycle();
						}

						throttle.postSeek(seekCmd.position);

						long n = System.currentTimeMillis();
						Log.i("jjplayer", "CALL SEEK");
						try {
							mr.seekMS(seekCmd.position);
						} catch (AVIOException ex) {
							Logger.getLogger(JJGLPlayer.class.getName()).log(Level.SEVERE, null, ex);
						}
						n = System.currentTimeMillis() - n;
						Log.i("jjplayer", "post SEEK took:" + n);
						seekCmd = null;
						if (vs != null)
							postSeekFrame = true;
					}

					long r = System.currentTimeMillis();
					rs = mr.readFrame();
					r = System.currentTimeMillis() - r;
					if (rs == null)
						break;

					busy += r;

					if (r > 150) {
						Log.i("jjplayer", "Slow Read Frame: " + r);
					}

					if (rs.equals(vs)) {
						if (!videodecode)
							continue;
						AVFrame iframe = vs.getFrame();
						FrameData fd = recycle.take();

						fd.pts = vs.convertPTS(mr.getPTS());

						long n = System.currentTimeMillis();

						fd.frame.copy(iframe, fmt, w, h);

						loadtext += (System.currentTimeMillis() - n);

						//System.out.println("frame");
						if (videoout)
							frames.offer(fd);
						else
							fd.recycle();
					} else if (rs.equals(as)) {
						if (!audiodecode)
							continue;
						if (false) {
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
						} else {
							AudioData ad = null;
							try {
								boolean go = true;
								do {
									ad = audiorecycle.take();
									ad.size = as.getSamples(ad.data);
									if (ad.size > 0) {
										// HACK: resample properly
										ad.fix(as.getContext().getChannels());
										if (audioout)
											audioframes.offer(ad);
										else
											audiorecycle.offer(ad);
										ad = null;
									} else {
										go = false;
									}
								} while (go);
							} catch (AVDecodingError ex) {
								// ignore audio decoding errors
								Log.i("jjmpeg", String.format("Audio decode error ignored: " + ex.getLocalizedMessage()));
							} finally {
								if (ad != null) {
									audiorecycle.offer(ad);
								}
							}
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
				start = System.currentTimeMillis() - start;
				Log.i("jjmpeg", String.format("Demux/decoder thread busy=%d.%03ds total: %d.%03ds  loadtext=%d.%03ds",
						busy / 1000, busy % 1000, start / 1000, start % 1000, loadtext / 1000, loadtext % 1000));

				thread = (Debug.threadCpuTimeNanos() - thread) / 1000L;
				Log.i("jjmpeg", String.format("Demux/decoder thread time=%d.%06ds", thread / 1000000L, thread % 1000000L));

				thread = (view.renderer.threadLast - view.renderer.thread) / 1000L;
				Log.i("jjmpeg", String.format("GL thread time=%d.%06ds", thread / 1000000L, thread % 1000000L));

				if (mr != null)
					mr.dispose();
				frames.offer(new FrameData(0, null));
			}
		}
	}
}
