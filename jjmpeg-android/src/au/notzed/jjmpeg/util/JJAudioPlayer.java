package au.notzed.jjmpeg.util;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import au.notzed.jjmpeg.*;
import au.notzed.jjmpeg.exception.AVException;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.io.JJMediaReader;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderAudio;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderStream;
import java.nio.ShortBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plays audio from a video
 *
 * @author notzed
 */
public class JJAudioPlayer extends Activity {

	ImageView iview;
	Audio audio;
	Decoder decoder;
	// audio out
	AudioTrack track;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		iview = (ImageView) findViewById(R.id.image);

		decoder = new Decoder();
		audio = new Audio();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!audio.isAlive()) {
			audio.start();
			decoder.start();
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

		try {
			decoder.join();
			audio.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(JJAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
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

	long startms;
	LinkedBlockingQueue<AudioData> audioframes = new LinkedBlockingQueue<AudioData>();
	LinkedBlockingQueue<AudioData> audiorecycle = new LinkedBlockingQueue<AudioData>();

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

	class Decoder extends Thread {

		public Decoder() {
			super("DEMUX Decode Thread");
		}
		JJMediaReader mr = null;
		JJReaderAudio as;
		// audioframes frames to buffer ahead.  Must be at least enough to fit betwen video frames
		static final int NAUDIO = 30;

		void open() throws AVIOException, AVException {
			//mr = new JJMediaReader("/sdcard/bbb.mov");
			mr = new JJMediaReader("/sdcard/trailer.mp4");

			for (JJReaderStream rs : mr.getStreams()) {
				switch (rs.getType()) {
					case AVMediaType.AVMEDIA_TYPE_AUDIO:
						if (as == null) {
							as = (JJReaderAudio) rs;
							as.open();
							Log.i("jjplayer", String.format("Opened Audio: %dHz channels %d", as.getContext().getSampleRate(), as.getContext().getChannels()));
						}
						break;
				}
			}

			for (int i = 0; i < NAUDIO; i++) {
				audiorecycle.add(new AudioData());
			}

			AVCodecContext cc = as.getContext();
			track = new AudioTrack(AudioManager.STREAM_MUSIC, cc.getSampleRate(),
					cc.getChannels() >= 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 8192 * 2, AudioTrack.MODE_STREAM);
			track.play();
		}

		@Override
		public void run() {
			long busy = 0;
			long start = System.currentTimeMillis();
			try {
				open();

				while (true) {
					try {
						JJReaderStream rs = mr.readFrame();

						if (rs == null)
							break;

						if (rs.equals(as)) {
							AVSamples samples;
							while ((samples = as.getSamples()) != null) {
								AudioData ad = audiorecycle.take();

								ad.setData((ShortBuffer) samples.getSamples(), as.getContext().getChannels());

								audioframes.offer(ad);
							}
						}
					} catch (InterruptedException ex) {
						Logger.getLogger(JJAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} catch (AVIOException ex) {
				Logger.getLogger(JJAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} catch (AVException ex) {
				Logger.getLogger(JJAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (mr != null)
					mr.dispose();
				AudioData ad = new AudioData();
				ad.size = -1;
				audioframes.offer(ad);
				start = System.currentTimeMillis() - start;
				Log.i("jjmpeg", String.format("Demux/decoder thread busy=%d.%03ds total: %d.%03ds", busy / 1000, busy % 1000, start / 1000, start % 1000));
			}
		}
	}
}
