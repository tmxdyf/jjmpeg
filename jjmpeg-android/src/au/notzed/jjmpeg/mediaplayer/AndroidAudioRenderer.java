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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import au.notzed.jjmpeg.AVSampleFormat;
import au.notzed.jjmpeg.util.CancellableThread;
import au.notzed.jjmpeg.util.JJQueue;

/**
 *
 * @author notzed
 */
public class AndroidAudioRenderer {

	final static int NBUFFERS = 21;
	AudioTrack track;
	AndroidAudioFrame[] bufferArray;
	JJQueue<AndroidAudioFrame> buffers = new JJQueue<AndroidAudioFrame>(NBUFFERS);
	JJQueue<AndroidAudioFrame> ready = new JJQueue<AndroidAudioFrame>(NBUFFERS);
	private final MediaClock clock;
	AudioScheduler scheduler;

	public AndroidAudioRenderer(MediaClock clock) {
		this.clock = clock;
	}

	void setAudioFormat(int sampleRate, int channels, AVSampleFormat fmt) {
		if (track != null) {
			track.stop();
			track.release();
		}

		// TODO: add resample shit or something

		// only support fmt.SAMPLE_FMT_S16
		if (fmt != AVSampleFormat.SAMPLE_FMT_S16) {
		}

		track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				channels >= 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, 8192 * 2 * 4, AudioTrack.MODE_STREAM);

		bufferArray = new AndroidAudioFrame[NBUFFERS];
		for (int i = 0; i < NBUFFERS; i++) {
			AndroidAudioFrame af = new AndroidAudioFrame();
			bufferArray[i] = af;
			buffers.offer(af);
		}

		System.out.println("track init");
	}

	public long getPosition() {
		if (track != null)
			return track.getPlaybackHeadPosition() * 1000L / track.getSampleRate();
		return 0;
	}

	public void start() {
		scheduler = new AudioScheduler();
		scheduler.start();
		clock.setAudioOut(scheduler);
		track.play();
	}

	public void stop() {
		if (scheduler != null) {
			clock.setAudioOut(null);
			scheduler.cancel();
			scheduler = null;
		}
		if (track != null) {
			track.stop();
		}
	}

	public void play() {
		if (track != null)
			track.play();
	}

	public void pause() {
		if (track != null)
			track.pause();
	}

	public void release() {
		stop();
		if (track != null) {
			track.stop();
			track.release();
			track = null;
		}
	}

	public AudioFrame getFrame() throws InterruptedException {
		return buffers.take();
	}

	class AndroidAudioFrame extends AudioFrame {

		public AndroidAudioFrame() {
		}

		@Override
		void enqueue() throws InterruptedException {
			ready.offer(this);
		}

		@Override
		void recycle() {
			buffers.offer(this);
		}

		void run() {
			if (track != null) {
				track.write(samples, 0, samplesLen * channelsCount);
			}
		}
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
				AndroidAudioFrame peek = null;
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
							track.flush();
							// Hmm, this is going to drift surely ...
							System.out.printf("resync audio, target %d line %d  offset old %d new %d\n", peek.pts, getPosition(), headOffset, peek.pts - getPosition());
							headOffset = peek.pts - getPosition();
							resync = false;
						}

						// TODO: + offset after seek!
						clock.setAudioPosition(getPosition() + headOffset);

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
			track.pause();
			clock.setAudioPosition(getPosition() + headOffset);
		}

		@Override
		public void clockResume() {
			track.play();
		}

		@Override
		public void clockSeekStart() {
			track.flush();
			track.stop();
		}

		@Override
		public void clockSeekFinish(long pos) {
			resync = true;
			//seekms = pos;
			//headOffset = pos + (headOffset + line.getMicrosecondPosition() / 1000);
			//System.out.println("seek finished, head offset = " + headOffset);
			track.play();
		}
	}
}
