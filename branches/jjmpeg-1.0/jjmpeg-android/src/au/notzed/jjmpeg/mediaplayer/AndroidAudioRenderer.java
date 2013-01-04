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
import au.notzed.jjmpeg.util.JJQueue;

/**
 *
 * @author notzed
 */
public class AndroidAudioRenderer {

	final static int NBUFFERS = 60;
	AudioTrack track;
	AnAudioFrame[] bufferArray;
	JJQueue<AnAudioFrame> buffers = new JJQueue<AnAudioFrame>(NBUFFERS);
	JJQueue<AnAudioFrame> ready = new JJQueue<AnAudioFrame>(NBUFFERS);

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

		bufferArray = new AnAudioFrame[NBUFFERS];
		for (int i = 0; i < NBUFFERS; i++) {
			AnAudioFrame af = new AnAudioFrame();
			bufferArray[i] = af;
			buffers.offer(af);
		}
	}

	public long getPosition() {
		if (track != null)
			return track.getPlaybackHeadPosition() * 1000L / track.getSampleRate();
		return 0;
	}

	public void play() {
		if (track != null)
			track.play();
	}

	public void pause() {
		if (track != null)
			track.pause();
	}

	public void stop() {
		if (track != null)
			track.stop();
	}

	public void release() {
		if (track != null) {
			track.stop();
			track.release();
			track = null;
		}
	}
	public void postSeek(long position) {
		if (track != null)
			track.flush();
	}

	public AudioFrame getFrame() throws InterruptedException {
		return buffers.take();
	}

	class AnAudioFrame extends AudioFrame {

		public AnAudioFrame() {
		}

		@Override
		void enqueue() throws InterruptedException {
			try {
				if (track != null) {
					track.write(samples, 0, samplesLen * channelsCount);
				}
			} finally {
				recycle();
			}
		}

		@Override
		void recycle() {
			buffers.offer(this);
		}
	}
}
