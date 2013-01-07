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
//
	JavaAudioFrame dummy = new JavaAudioFrame();
	SourceDataLine line;
	JJQueue<JavaAudioFrame> buffers = new JJQueue(NBUFFERS);
	JJQueue<JavaAudioFrame> ready = new JJQueue(NBUFFERS);
	boolean paused;
	long positionOffset;

	public JavaAudioRenderer() {
		for (int i = 0; i < NBUFFERS; i++) {
			buffers.offer(new JavaAudioFrame());
		}
	}

	void setAudioFormat(int sampleRate, int channels, AVSampleFormat fmt) {
		if (line != null) {
			release();
		}

		if (fmt != AVSampleFormat.SAMPLE_FMT_S16) {
			System.out.println("Unsupported sample format: " + fmt);
			return;
		}

		AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			System.out.println("JavaSound unsupported format: " + info);
			return;
		}
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, BUFSIZE);
		} catch (LineUnavailableException ex) {
			Logger.getLogger(JavaAudioRenderer.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}

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

			if (bsamples == null || bsamples.length < bsize) {
				bsamples = new byte[bsize];
			}

			src.getSamples(fmt, channels, bsamples);
			samplesLen = nsamples;
			channelsCount = channels;
		}

		@Override
		void enqueue() throws InterruptedException {
			try {
				if (line != null) {
					int left = bsize;

					// Hack to handle pause
					while (paused) {
						Thread.sleep(100);
					}

					while (left > 0) {
						left -= line.write(bsamples, bsize - left, left);
					}
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

	public AudioFrame getFrame() throws InterruptedException {
		return buffers.take();
	}

	public void start() {
		paused = false;
		if (line != null)
			line.start();
	}

	public long getPosition() {
		if (line != null)
			return (line.getMicrosecondPosition() / 1000) - positionOffset;
		return 0;
	}

	public void stop() {
		paused = true;
		if (line != null) {
			line.stop();
			System.out.println("stopping audio!");
		}
	}

	public void release() {
		if (line != null) {
			line.close();
			line = null;
		}
	}
}
