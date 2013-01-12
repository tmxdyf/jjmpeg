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
import au.notzed.jjmpeg.AVSampleFormat;
import au.notzed.jjmpeg.util.CancellableThread;
import au.notzed.jjmpeg.util.JJQueue;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Audio renderer for JavaSound
 * @author notzed
 */
public class JavaAudioRenderer {

	final int BUFSIZE = 8192 * 2 * 4;
	final int NBUFFERS = 2;
	long audiolag;
	//
	long audiots;
	// position of line when audio resumed
	long audiopos;
//
	JavaAudioFrame dummy = new JavaAudioFrame();
	SourceDataLine line;
	JJQueue<JavaAudioFrame> buffers = new JJQueue(NBUFFERS);
	JJQueue<JavaAudioFrame> ready = new JJQueue(NBUFFERS);
	long positionOffset;
	private final MediaClock clock;
	AudioScheduler scheduler;

	public JavaAudioRenderer(MediaClock clock) {
		for (int i = 0; i < NBUFFERS; i++) {
			buffers.offer(new JavaAudioFrame());
		}
		this.clock = clock;
	}
	AudioFormat format;
	DataLine.Info info;

	public void start() {
		if (line != null) {
			release();
		}

		if (!AudioSystem.isLineSupported(info)) {
			System.out.println("JavaSound unsupported format: " + info);
			return;
		}
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, BUFSIZE);
			line.start();
		} catch (LineUnavailableException ex) {
			Logger.getLogger(JavaAudioRenderer.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		scheduler = new AudioScheduler();
		scheduler.start();
		clock.setAudioOut(scheduler);
	}

	public void stop() {
		if (scheduler != null) {
			clock.setAudioOut(null);
			scheduler.cancel();
			scheduler = null;
		}
		if (line != null) {
			line.close();
			line = null;
		}
	}

	void setAudioFormat(int sampleRate, int channels, AVSampleFormat fmt) {
		if (fmt != AVSampleFormat.SAMPLE_FMT_S16) {
			System.out.println("Unsupported sample format: " + fmt);
			return;
		}

		// estimate audio lag from frame rate buffer sizes in ms
		audiolag = 1000L * (NBUFFERS * BUFSIZE / channels / 4) / sampleRate;

		clock.setAudioLag(audiolag);

		format = new AudioFormat(sampleRate, 16, channels, true, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
		info = new DataLine.Info(SourceDataLine.class, format);
	}

	void postSeek(long ms) {
		line.flush();
		positionOffset = (line.getMicrosecondPosition() / 1000) - ms;
	}

	class JavaAudioFrame extends AudioFrame {

		byte[] bsamples;
		int bsize;

		@Override
		public void setSamples(AVFrame src, int channels, AVSampleFormat fmt, int nsamples) {
			bsize = AVFrame.getSamplesSize(fmt, channels, nsamples, 1);

			src.getBestEffortTimestamp();

			if (bsamples == null || bsamples.length < bsize) {
				bsamples = new byte[bsize];
			}

			src.getSamples(fmt, channels, bsamples);
			samplesLen = nsamples;
			channelsCount = channels;
		}
		long offset;

		@Override
		void enqueue() throws InterruptedException {
			ready.offer(this);
			/*
			 try {
			 if (line != null) {
			 int left = bsize;

			 if (clock.checkPauseAudio()) {
			 line.flush();
			 }
			 System.out.println("Audio play: " + (pts - audiolag));
			 clock.setAudioPosition(pts - audiolag);

			 while (left > 0) {
			 left -= line.write(bsamples, bsize - left, left);
			 }
			 }
			 } finally {
			 recycle();
			 }*/
		}

		@Override
		void recycle() {
			buffers.offer(this);
		}

		public void run() {
			int left = bsize;

			//System.out.println("Audio play: " + (pts - audiolag));
			//clock.setAudioPosition(pts);

			while (left > 0) {
				left -= line.write(bsamples, bsize - left, left);
			}
		}
	}

	public AudioFrame getFrame() throws InterruptedException {
		return buffers.take();
	}

	public void play() {
		if (line != null) {
			line.start();
		}
	}

	public long getPosition() {
		if (line != null)
			return (line.getMicrosecondPosition() / 1000) - positionOffset;
		return 0;
	}

	/*
	 public void stop() {
	 paused = true;
	 if (line != null) {
	 line.stop();
	 System.out.println("stopping audio!");
	 }
	 }*/
	public void release() {
		stop();
		//	if (line != null) {
		//		line.close();
		//		line = null;
		//	}
	}

	class AudioScheduler extends CancellableThread implements MediaClock.MediaClockListener {

		boolean resync = false;
		long headOffset;

		public AudioScheduler() {
			super("Audio Scheduler");
		}

		@Override
		public void run() {
			while (!cancelled) {
				JavaAudioFrame peek = null;
				try {
					long delay;

					if (clock.checkPauseAudio()) {
						//System.out.println("audio seeked, flushing");
						// post seek drop frames
						// This will not work ...
						//ready.drainTo(buffers);
						//line.flush();
						//line.drain();
						// TODO: update line position
					}

					peek = ready.take();

					if (peek.sequence == clock.getSequence()) {
						if (resync) {
							line.flush();
							line.drain();
							// Hmm, this is going to drift surely ...
							System.out.printf("resync audio, target %d line %d  offset old %d new %d\n", peek.pts, line.getMicrosecondPosition() / 1000, headOffset, peek.pts - (line.getMicrosecondPosition() / 1000));
							headOffset = peek.pts - (line.getMicrosecondPosition() / 1000);
							resync = false;
						}

						// TODO: + offset after seek!
						clock.setAudioPosition(line.getMicrosecondPosition() / 1000 + headOffset);
						//clock.setAudioPosition(peek.pts - audiolag);

						peek.run();
					} else {
						System.out.println("## Discarding audio frame " + peek.pts + " out of sequence: " + peek.sequence);
						resync = true;
					}
				} catch (InterruptedException x) {
				} finally {
					if (peek != null)
						peek.recycle();
				}
			}
		}

		@Override
		public void clockPause() {
			line.stop();
			line.drain();
			clock.setAudioPosition(line.getMicrosecondPosition() / 1000 + headOffset);
		}

		@Override
		public void clockResume() {
			line.start();
		}

		@Override
		public void clockSeekStart() {
			line.flush();
			line.drain();
			line.stop();
		}

		@Override
		public void clockSeekFinish(long pos) {
			resync = true;
			//seekms = pos;
			//headOffset = pos + (headOffset + line.getMicrosecondPosition() / 1000);
			//System.out.println("seek finished, head offset = " + headOffset);
			line.start();
		}
	}
}
