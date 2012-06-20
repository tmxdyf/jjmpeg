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

import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.SwsContext;
import au.notzed.jjmpeg.io.JJQueue;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Simple Swing media player component.
 * @author notzed
 */
public class SwingMediaPlayer extends JPanel implements MediaSink, MediaPlayer {

	//
	JLabel label;
	BufferedImage image;
	SwsContext scale;
	VideoRenderThread videoThread;
	AudioRenderThread audioThread;
	//
	MediaReader reader;
	VideoDecoder vd;
	AudioDecoder ad;
	static final int VBUFFERS = 3;
	static final int ABUFFERS = 30;
	JJQueue<SwingVideoFrame> buffers = new JJQueue<SwingVideoFrame>(VBUFFERS);
	JJQueue<SwingVideoFrame> ready = new JJQueue<SwingVideoFrame>(VBUFFERS);
	JJQueue<SwingAudioFrame> abuffers = new JJQueue<SwingAudioFrame>(ABUFFERS);
	JJQueue<SwingAudioFrame> aready = new JJQueue<SwingAudioFrame>(ABUFFERS);
	LinkedList<MediaSinkListener> listeners = new LinkedList<MediaSinkListener>();

	public SwingMediaPlayer() {
		image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
		label = new JLabel(new ImageIcon(image));

		setLayout(new BorderLayout());
		add(label, BorderLayout.CENTER);

		// fill frames
		for (int i = 0; i < VBUFFERS; i++) {
			buffers.offer(new SwingVideoFrame());
		}
		for (int i = 0; i < 30; i++) {
			abuffers.offer(new SwingAudioFrame());
		}
	}

	void open(String file) throws IOException {
		reader = new MediaReader(file);
		reader.createDefaultDecoders(this);

		initRenderers();

		//seek.setMax((int) reader.getDuration());

		audioThread = new AudioRenderThread();
		videoThread = new VideoRenderThread();

		reader.start();

		audioThread.start();
		videoThread.start();
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
					label.repaint();

					vf.recycle();

					// TODO: throttle properly
					sleep(40);
				} catch (InterruptedException ex) {
					Logger.getLogger(SwingMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	public void setVideoSize(int w, int h, PixelFormat fmt) {
		System.out.printf("Set video size %dx%d format %s\n", w, h, fmt);
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		label.setIcon(new ImageIcon(image));
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
			} else if (md instanceof AudioDecoder) {
				ad = (AudioDecoder) md;

				// Force stereo output for multi-channel data
				int cc = Math.min(2, ad.cc.getChannels());

				//ad.setOutputFormat(3, cc, SampleFormat.SAMPLE_FMT_S16, ad.cc.getSampleRate());
				//aRenderer.setAudioFormat(ad.cc.getSampleRate(), cc, ad.cc.getSampleFmt());
				//haveAudio = true;
			}
		}
	}

	@Override
	public void addMediaSinkListener(MediaSinkListener listener) {
		listeners.add(listener);
	}

	@Override
	public AudioFrame getAudioFrame() throws InterruptedException {
		return abuffers.take();
	}

	@Override
	public VideoFrame getVideoFrame() throws InterruptedException {
		return buffers.take();
	}

	@Override
	public void postSeek(long stampms) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void unpause() {
	}

	@Override
	public void finished() {
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
					smp.open("/home/notzed/Videos/Ministry.of.Sound.The.Annual.2003.avi");
					//smp.open("/home/notzed/Videos/Doctor_Who-20120402193100.mp4");
					//smp.open("/home/notzed/Videos/big-buck-bunny_trailer.webm");

				} catch (IOException ex) {
					Logger.getLogger(SwingMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
	}
}
