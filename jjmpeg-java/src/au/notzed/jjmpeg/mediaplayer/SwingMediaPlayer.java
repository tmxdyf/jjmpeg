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

import au.notzed.jjmpeg.util.CancellableThread;
import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.SwsContext;
import au.notzed.jjmpeg.util.JJQueue;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Simple Swing media player component.
 *
 * Still work in progress, not particularly efficient/bug free.
 * @author notzed
 */
public class SwingMediaPlayer extends JPanel implements MediaSink, MediaPlayer {

	MediaState state = MediaState.Init;
	// 0 = audio, 1 = video
	int timesync = 0;
	// time it started running
	long start;
	// current rendered ms, relative to stream start
	long clock;
	int seekseq;
	//
	JLabel label;
	BufferedImage image;
	JSlider seek;
	boolean updateSeek;
	Timer uitimer;
	SwsContext scale;
	VideoRenderThread videoThread;
	AudioRenderThread audioThread;
	//
	MediaReader reader;
	VideoDecoder vd;
	AudioDecoder ad;
	static final int VBUFFERS = 3;
	static final int ABUFFERS = 17;
	JJQueue<SwingVideoFrame> buffers = new JJQueue<SwingVideoFrame>(VBUFFERS);
	JJQueue<SwingVideoFrame> ready = new JJQueue<SwingVideoFrame>(VBUFFERS);
	JJQueue<SwingAudioFrame> abuffers = new JJQueue<SwingAudioFrame>(ABUFFERS);
	JJQueue<SwingAudioFrame> aready = new JJQueue<SwingAudioFrame>(ABUFFERS);

	public SwingMediaPlayer() {
		image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
		label = new JLabel(new IconSurface(image));
		label.setHorizontalAlignment(JLabel.LEFT);
		label.setVerticalAlignment(JLabel.TOP);
		seek = new JSlider();

		setLayout(new BorderLayout());
		add(label, BorderLayout.CENTER);
		add(seek, BorderLayout.SOUTH);

		// fill frames
		for (int i = 0; i < VBUFFERS; i++) {
			buffers.offer(new SwingVideoFrame());
		}
		for (int i = 0; i < ABUFFERS; i++) {
			abuffers.offer(new SwingAudioFrame());
		}

		seek.addChangeListener(sliderChanged);
		uitimer = new Timer(100, uiupdate);
	}
	ChangeListener sliderChanged = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (!updateSeek)
				//		if (!seek.getValueIsAdjusting())
				seek(seek.getValue());
		}
	};
	ActionListener uiupdate = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!seek.getValueIsAdjusting()) {
				updateSeek = true;
				seek.setValue((int) getPosition());
				updateSeek = false;
			}
		}
	};

	void startTimer() {
		uitimer.start();
	}

	void stopTimer() {
		uitimer.stop();
	}

	void open(String file) throws IOException {
		reader = new MediaReader(file);
		reader.createDefaultDecoders(this);

		initRenderers();

		seek.setMaximum((int) getDuration());

		audioThread = new AudioRenderThread();
		videoThread = new VideoRenderThread();

		clock = 0;
		start = System.currentTimeMillis();
		startTimer();
		reader.start();
		state = MediaState.Playing;
		audioThread.start();
		videoThread.start();
	}

	// todo: this state management wont work, need a listener to tell what the reader is really doing
	@Override
	public void play() {
		if (state == MediaState.Paused
				|| state == MediaState.Ready) {
			// TODO: need to reset start somehow
			reader.unpause();
			state = MediaState.Playing;
			startTimer();
		}
	}

	@Override
	public void pause() {
		if (state == MediaState.Playing) {
			reader.pause();
			state = MediaState.Playing;
			startTimer();
		}
	}

	@Override
	public void stop() {
		switch (state) {
			case Playing:
				reader.pause();
				state = MediaState.Paused;
				stopTimer();
				break;
		}
	}

	@Override
	public void seek(long ms) {
		reader.seek(ms, 0);
	}

	@Override
	public long getPosition() {
		return clock;
	}

	@Override
	public long getDuration() {
		return reader.getDuration();
	}

	@Override
	public void setListener(MediaListener l) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public MediaState getMediaState() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	class AudioRenderThread extends CancellableThread {

		public AudioRenderThread() {
			super("Audio Render");
		}

		@Override
		public void run() {
			while (!cancelled) {
				try {
					SwingAudioFrame vf = aready.take();

					// update clock based on audio
					if (timesync == 0) {
						clock = vf.pts;
					}
					// TODO: play frame

					vf.recycle();
				} catch (InterruptedException ex) {
					Logger.getLogger(SwingMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	class VideoRenderThread extends CancellableThread {

		public VideoRenderThread() {
			super("Swing Video Render");
		}

		@Override
		public void run() {
			while (!cancelled) {
				try {
					SwingVideoFrame vf = ready.take();
					AVFrame frame = vf.getFrame();

					// display frame
					int[] dst = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

					scale.scale(frame, 0, image.getHeight(), dst);

					long now = System.currentTimeMillis();
					long d = vf.pts - (now - start);

					if (d > 500 || vf.seekseq != seekseq) {
						// show without delay, it's probably an out-of-sequence frame
					} else {
						if (d > 0) {
							sleep(d);
						} else {
							System.out.println("frame offset " + d);
						}

						if (timesync == 1) {
							clock = vf.pts;
						}
					}
					label.repaint();
					vf.recycle();
				} catch (InterruptedException ex) {
					Logger.getLogger(SwingMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	public void setVideoSize(int w, int h, PixelFormat fmt) {
		System.out.printf("Set video size %dx%d format %s\n", w, h, fmt);
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		label.setIcon(new IconSurface(image));
		label.repaint();

		scale = SwsContext.create(w, h, fmt, w, h, PixelFormat.PIX_FMT_BGRA, SwsContext.SWS_BILINEAR);
	}

	public void initRenderers() {
		for (MediaDecoder md : reader.streamMap.values()) {
			if (md instanceof VideoDecoder) {
				vd = (VideoDecoder) md;
				//videoTB = vd.stream.getTimeBase();
				//videoStart = vd.stream.getStartTime();

				setVideoSize(vd.width, vd.height, vd.format);
				//haveVideo = true;
				timesync = 1;
			} else if (md instanceof AudioDecoder) {
				//ad = (AudioDecoder) md;
				// Force stereo output for multi-channel data
				//int cc = Math.min(2, ad.cc.getChannels());
				//ad.setOutputFormat(3, cc, SampleFormat.SAMPLE_FMT_S16, ad.cc.getSampleRate());
				//aRenderer.setAudioFormat(ad.cc.getSampleRate(), cc, ad.cc.getSampleFmt());
				//haveAudio = true;
			}
		}

		// Sync to audio by preference
		if (ad != null)
			timesync = 0;
		else if (vd != null)
			timesync = 1;
	}

	@Override
	public AudioFrame getAudioFrame() throws InterruptedException {
		return abuffers.take();
	}

	@Override
	public VideoFrame getVideoFrame() throws InterruptedException {
		SwingVideoFrame svf = buffers.take();

		svf.seekseq = seekseq;
		return svf;
	}

	@Override
	public void postSeek(long stampms) {
		start = System.currentTimeMillis() - stampms;
		seekseq++;
		System.out.println("post seek " + stampms + ", start set to " + start);
		aready.drainTo(abuffers);
		ready.drainTo(buffers);
	}

	@Override
	public void postPlay() {
		startTimer();
	}

	@Override
	public void postPause() {
		aready.drainTo(abuffers);
		ready.drainTo(buffers);
		stopTimer();
	}

	@Override
	public void postUnpause() {
		startTimer();
	}

	@Override
	public void postFinished() {
		stopTimer();
	}

	class SwingAudioFrame extends AudioFrame {

		@Override
		void enqueue() throws InterruptedException {
			aready.offer(this);
		}

		@Override
		void recycle() {
			abuffers.offer(this);
		}
	}

	class SwingVideoFrame extends VideoFrame {

		AVFrame frame;
		int seekseq;

		@Override
		public void setFrame(AVFrame frame) {
			this.frame = frame;
		}

		@Override
		public AVFrame getFrame() {
			return frame;
		}

		@Override
		void enqueue() throws InterruptedException {
			ready.offer(this);
		}

		@Override
		void recycle() {
			buffers.offer(this);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					SwingMediaPlayer smp = new SwingMediaPlayer();
					JFrame frame = new JFrame("Player");

					frame.setContentPane(smp);

					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.pack();
					frame.setVisible(true);
					smp.open("/home/notzed/Videos/big-buck-bunny_trailer.webm");

				} catch (IOException ex) {
					Logger.getLogger(SwingMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
	}
}
