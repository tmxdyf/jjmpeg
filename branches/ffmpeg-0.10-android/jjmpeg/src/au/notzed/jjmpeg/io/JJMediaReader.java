/*
 * Copyright (c) 2012 Michael Zucchi
 *
 * This file is part of jjmpeg, a java binding to ffmpeg's libraries.
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
package au.notzed.jjmpeg.io;

import au.notzed.jjmpeg.AVAudioPacket;
import au.notzed.jjmpeg.AVCodec;
import au.notzed.jjmpeg.AVCodecContext;
import au.notzed.jjmpeg.AVFormatContext;
import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.AVSamples;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.SampleFormat;
import au.notzed.jjmpeg.SwsContext;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.exception.AVInvalidCodecException;
import au.notzed.jjmpeg.exception.AVInvalidStreamException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * High level interface for scanning audio and video frames.
 * 
 * TODO: handle all frames.
 * @author notzed
 */
public class JJMediaReader {

	static final boolean dump = false;
	LinkedList<JJReaderStream> streams = new LinkedList<JJReaderStream>();
	HashMap<Integer, JJReaderStream> streamsByID = new HashMap<Integer, JJReaderStream>();
	AVFormatContext format;
	private AVPacket packet;
	private boolean freePacket = false;
	//
	long seekid = -1;
	//

	public JJMediaReader(String name) throws AVInvalidStreamException, AVIOException, AVInvalidCodecException {
		//AVFormatContext.registerAll();
		format = AVFormatContext.open(name);

		format.findStreamInfo();

		// find first video/audio stream
		AVStream vstream = null;
		AVStream astream = null;
		int nstreams = format.getNBStreams();
		for (int i = 0; i < nstreams && (vstream == null | astream == null); i++) {
			AVStream s = format.getStreamAt(i);
			AVCodecContext cc = s.getCodec();
			switch (cc.getCodecType()) {
				case AVCodecContext.AVMEDIA_TYPE_VIDEO:
					JJReaderVideo vs = new JJReaderVideo(s);
					streams.add(vs);
					streamsByID.put(s.getIndex(), vs);
					break;
				case AVCodecContext.AVMEDIA_TYPE_AUDIO:
					JJReaderAudio as = new JJReaderAudio(s);
					streams.add(as);
					streamsByID.put(s.getIndex(), as);
					break;
			}
		}

		if (streams.isEmpty()) {
			throw new AVInvalidStreamException("No audio or video streams found");
		}

		packet = AVPacket.create();
	}

	/**
	 * Opens the first video and audio stream found
	 * 
	 * TODO: could open the first one we know how to decode
	 */
	public void openDefaultStreams() throws AVInvalidCodecException, AVIOException {
		boolean aopen = false;
		boolean vopen = false;

		for (JJReaderStream m : streams) {
			switch (m.getType()) {
				case AVCodecContext.AVMEDIA_TYPE_VIDEO:
					if (!vopen) {
						m.open();
						vopen = true;
					}
					break;
				case AVCodecContext.AVMEDIA_TYPE_AUDIO:
					if (!aopen) {
						m.open();
						aopen = true;
					}
					break;
			}
		}
	}

	/**
	 * Find and open the first video stream.
	 */
	public JJReaderVideo openFirstVideoStream() throws AVInvalidCodecException, AVIOException {
		for (JJReaderStream m : streams) {
			if (m.getType() == AVCodecContext.AVMEDIA_TYPE_VIDEO) {
				m.open();
				return (JJReaderVideo) m;
			}
		}
		return null;
	}

	public List<JJReaderStream> getStreams() {
		return streams;
	}

	public JJReaderStream getStreamByID(int id) {
		return streamsByID.get(id);
	}

	public void dispose() {
		for (JJReaderStream m : streams) {
			m.dispose();
		}
		format.dispose();
	}

	/**
	 * Set output rendered size.
	 * 
	 * If this is changed, must re-call createImage(), getOutputFrame(), etc.
	 * @param swidth
	 * @param sheight 
	 */
	/*
	public void setSize(int swidth, int sheight) {
	oframe.dispose();
	scale.dispose();
	this.swidth = swidth;
	this.sheight = sheight;
	
	oframe = AVFrame.create(PixelFormat.PIX_FMT_BGR24, swidth, sheight);
	scale = SwsContext.create(width, height, fmt, swidth, sheight, PixelFormat.PIX_FMT_BGR24, SwsContext.SWS_BILINEAR);
	}
	 * 
	 */
	/**
	 * Get raw format for images.
	 * @return 
	 */
	public AVFormatContext getFormat() {
		return format;
	}
	long pts;

	/**
	 * Retrieve (calculated) pts of the last frame decoded.
	 * 
	 * Well be -1 at EOF
	 * @return 
	 */
	public long getPTS() {
		return pts;
	}

	/**
	 * Reads and decodes packets until data is ready in one
	 * of the opened streams.
	 * @return 
	 */
	public JJReaderStream readFrame() {

		if (freePacket) {
			packet.freePacket();
		}
		freePacket = false;

		while (format.readFrame(packet) >= 0) {
			//System.out.println("read packet");
			try {
				JJReaderStream ms = streamsByID.get(packet.getStreamIndex());

				if (ms != null) {
					if (ms.decode(packet)) {
						pts = packet.getDTS();

						// If seeking, attempt to get to the exact frame
						if (seekid != -1
								&& pts < seekid) {
							continue;
						}
						seekid = -1;
						freePacket = true;
						return ms;
					}
				}
			} catch (AVDecodingError x) {
				System.err.println("Decoding error: " + x);
			} finally {
				if (!freePacket) {
					packet.freePacket();
				}
			}
		}

		return null;
	}

	/**
	 * Attempt to seek to the nearest millisecond.
	 * 
	 * The next frame read should match the stamp.
	 * 
	 * This only seeks to key frames
	 * 
	 * TODO: unimplemented
	 * @param stamp
	 * @throws AVIOException 
	 */
	public void seekMS(long stamp) throws AVIOException {
		int res;

		res = format.seekFile(-1, 0, stamp * 1000, stamp * 1000, 0);
		if (res < 0) {
			throw new AVIOException(res, "Cannot seek");
		}

		//vcontext.flushBuffers();
	}

	/**
	 * Seek in stream units
	 * 
	 * UNIMPLEMENTED
	 * @param stamp
	 * @throws AVIOException 
	 */
	public void seek(long stamp) throws AVIOException {
		int res;

		//res = format.seekFile(videoStream, 0, stamp, stamp, 0);
		//if (res < 0) {
		//	throw new AVIOException(res, "Cannot seek");
		//}

		//seekid = stamp;
		//vcontext.flushBuffers();
	}

	public abstract class JJReaderStream {

		AVStream stream;
		AVCodecContext c;
		int streamID = -1;
		AVCodec codec;
		// timebase
		int tb_Num;
		int tb_Den;
		// start pts
		long startpts;
		// start ms
		long startms;
		//
		long duration;
		long durationms;

		public JJReaderStream(AVStream stream) {
			this.stream = stream;

			c = stream.getCodec();

			AVRational tb = stream.getTimeBase();
			tb_Num = tb.getNum();
			tb_Den = tb.getDen();

			startpts = stream.getStartTime();
			startms = AVRational.starSlash(startpts * 1000, tb_Num, tb_Den);
			duration = stream.getDuration();
			durationms = AVRational.starSlash(duration * 1000, tb_Num, tb_Den);
		}

		public void open() throws AVInvalidCodecException, AVIOException {
		}

		public void dispose() {
			if (codec != null) {
				codec.dispose();
			}
			c.dispose();
		}

		public AVCodecContext getContext() {
			return c;
		}

		public AVCodec getCodec() {
			return codec;
		}

		/**
		 * Retrieve duration of sequence, in milliseconds.
		 * @return 
		 */
		public long getDurationMS() {
			return durationms;
		}

		/**
		 * Get duration in timebase units (i.e. frames?)
		 * @return 
		 */
		public long getDuration() {
			return duration;
		}

		/**
		 * Convert the 'pts' provided to milliseconds relative to the start of the
		 * stream.
		 * @param pts
		 * @return 
		 */
		public long convertPTS(long pts) {
			return AVRational.starSlash(pts * 1000, tb_Num, tb_Den) - startms;
		}

		/**
		 * Decode a packet.  Returns true if data is now ready.
		 * 
		 * It is ok to call this on an unopened stream: return false.
		 * @param packet
		 * @return 
		 */
		abstract public boolean decode(AVPacket packet) throws AVDecodingError;

		/**
		 * Retreive the AVMEDIA_TYPE_* for this stream.
		 * @return 
		 */
		abstract public int getType();
	}

	public class JJReaderVideo extends JJReaderStream {

		int swidth;
		int sheight;
		SwsContext scale;
		int height;
		int width;
		PixelFormat fmt;
		AVFrame iframe;
		//
		PixelFormat ofmt;
		AVFrame oframe;
		/**
		 * Is the scaled/converted frame stale
		 */
		boolean stale;

		public JJReaderVideo(AVStream stream) throws AVInvalidCodecException, AVIOException {
			super(stream);
		}

		@Override
		public void open() throws AVInvalidCodecException, AVIOException {
			if (dump) {
				System.out.println("Open video reader");
				System.out.printf(" video size %dx%d\n",
						c.getWidth(),
						c.getHeight());
				System.out.println(" video codec id = " + c.getCodecID());
				System.out.println(" pixel format: " + c.getPixFmt());
			}

			// find decoder for the video stream
			codec = AVCodec.findDecoder(c.getCodecID());

			if (codec == null) {
				throw new AVInvalidCodecException("Unable to decode video stream");
			}

			c.open(codec);


			iframe = AVFrame.create();

			height = c.getHeight();
			width = c.getWidth();
			fmt = c.getPixFmt();

			swidth = width;
			sheight = height;
		}

		@Override
		public void dispose() {
			super.dispose();

			iframe.dispose();

			if (scale != null) {
				scale.dispose();
				oframe.dispose();
			}
		}

		@Override
		public boolean decode(AVPacket packet) throws AVDecodingError {
			if (iframe == null) {
				return false;
			}

			stale = true;

			boolean frameFinished = c.decodeVideo(iframe, packet);

			return frameFinished;
		}

		@Override
		public int getType() {
			return AVCodecContext.AVMEDIA_TYPE_VIDEO;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public PixelFormat getPixelFormat() {
			return fmt;
		}

		/**
		 * Set the output format for use with getOutputFrame()
		 * 
		 * If using the BufferedImage version of getOutputFrame, currently ofmt
		 * must be PIX_FMT_BGR24.
		 * @param ofmt
		 * @param swidth
		 * @param sheight 
		 */
		public void setOutputFormat(PixelFormat ofmt, int swidth, int sheight) {
			if (scale != null) {
				scale.dispose();
				oframe.dispose();
			}
			this.swidth = swidth;
			this.sheight = sheight;
			this.ofmt = ofmt;
			oframe = AVFrame.create(ofmt, swidth, sheight);
			scale = SwsContext.create(width, height, fmt, swidth, sheight, ofmt, SwsContext.SWS_BILINEAR);
		}

		/**
		 * Allocate an image suitable for getOutputFrame()
		 * @return 
		 */
		public BufferedImage createImage() {
			return new BufferedImage(swidth, sheight, BufferedImage.TYPE_3BYTE_BGR);
		}

		/**
		 * Retrieve the scaled frame, or just the raw frame if no output format set
		 * @return 
		 */
		public AVFrame getOutputFrame() {
			if (oframe != null) {
				if (stale) {
					scale.scale(iframe, 0, height, oframe);
					stale = false;
				}

				return oframe;
			}
			return iframe;
		}

		/**
		 * Get the output frame into a buffered image.
		 * TODO: only works with FIX_FMT_BGR24!
		 * @param dst
		 * @return dst
		 */
		public BufferedImage getOutputFrame(BufferedImage dst) {
			assert (dst.getType() == BufferedImage.TYPE_3BYTE_BGR);

			if (ofmt == null) {
				setOutputFormat(PixelFormat.PIX_FMT_BGR24, width, height);
			}

			// Scale directly to target image
			byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
			scale.scale(iframe, 0, height, data);
			
			return dst;
		}

		/**
		 * Retrieve the decoded frame.
		 * @return 
		 */
		public AVFrame getFrame() {
			return iframe;
		}
	}

	public class JJReaderAudio extends JJReaderStream {

		AVAudioPacket apacket;
		AVSamples samples;

		public JJReaderAudio(AVStream stream) throws AVInvalidCodecException, AVIOException {
			super(stream);
		}

		@Override
		public void open() throws AVInvalidCodecException, AVIOException {
			System.out.println("Open Audio Reader");
			System.out.println(" audio codec id = " + c.getCodecID());

			// find decoder for the video stream
			codec = AVCodec.findDecoder(c.getCodecID());

			if (codec == null) {
				throw new AVInvalidCodecException("Unable to decode video stream");
			}

			c.open(codec);

			System.out.println(" codec : " + codec.getName());
			System.out.println(" sampleformat : " + c.getSampleFmt());
			System.out.println(" samplerate : " + c.getSampleRate());

			apacket = AVAudioPacket.create();
			samples = new AVSamples(c.getSampleFmt());
		}

		public boolean decode(AVPacket packet) throws AVDecodingError {
			if (samples == null) {
				return false;
			}

			apacket.setSrc(packet);

			return apacket.getSize() > 0;
		}

		@Override
		public int getType() {
			return AVCodecContext.AVMEDIA_TYPE_AUDIO;
		}

		public SampleFormat getSampleFormat() {
			return c.getSampleFmt();
		}

		/**
		 * Retrieve the next block of decoded samples: this will return
		 * a new AVSamples until there are no more samples left.
		 */
		public AVSamples getSamples() throws AVDecodingError {
			while (apacket.getSize() > 0) {
				int len = c.decodeAudio(samples, apacket);
				if (len > 0) {
					return samples;
				}
			}
			return null;
		}
	}
}
