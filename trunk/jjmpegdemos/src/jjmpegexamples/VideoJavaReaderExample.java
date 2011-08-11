/*
 * Based on avcodec_sample.0.5.0.c
 *
 * http://web.me.com/dhoerl/Home/Tech_Blog/Entries/2009/1/22_Revised_avcodec_sample.c.html
 *
 * Presumably in public domain.
 */
package jjmpegexamples;

import au.notzed.jjmpeg.AVCodec;
import au.notzed.jjmpeg.AVCodecContext;
import au.notzed.jjmpeg.AVFormatContext;
import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVPlane;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.io.JJFileInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This tries to read from a Java file and feed it into ffmpeg.
 * 
 * **BROKEN BROKEN**
 * 
 * Unfortunately, the binding routines are completely fubar right now,
 * and this will not work.
 * 
 * **BROKEN BROKEN**
 * 
 * @author notzed
 */
public class VideoJavaReaderExample {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String name = "/home/notzed/Videos/mike0.avi";

		FileInputStream fis = new FileInputStream(name);
		JJFileInputStream jjfis = JJFileInputStream.create(fis);
		
		AVFormatContext.registerAll();
		AVFormatContext format = AVFormatContext.openInputStream(jjfis, name);

		if (format.findStreamInfo() < 0) {
			System.err.println("Could not find stream information");
			System.exit(1);
		}

		// find first video stream
		AVStream stream = null;
		AVCodecContext codecContext = null;
		int videoStream = -1;
		int nstreams = format.getNBStreams();
		for (int i = 0; i < nstreams; i++) {
			AVStream s = format.getStreamAt(i);
			codecContext = s.getCodec();
			if (codecContext.getCodecType() == AVCodecContext.AVMEDIA_TYPE_VIDEO) {
				videoStream = i;
				stream = s;
				break;
			}
		}

		if (stream == null) {
			System.err.println("could not find a video stream");
			System.exit(1);
		}

		System.out.printf("codec size %dx%d\n",
				codecContext.getWidth(),
				codecContext.getHeight());
		System.out.println("codec id = " + codecContext.getCodecID());

		// find decoder for the video stream
		AVCodec codec = AVCodec.findDecoder(codecContext.getCodecID());

		if (codec == null) {
			System.err.println("could not find suitable codec");
			System.exit(1);
		}

		System.out.println("opening codec");
		if (codecContext.open(codec) < 0) {
			System.err.println("error opening codec\n");
			System.exit(1);
		}

		System.out.println("pixel format: " + codecContext.getPixFmt());

		AVFrame frame = AVFrame.create();
		AVPacket packet = AVPacket.create();

		int height = codecContext.getHeight();
		int width = codecContext.getWidth();
		PixelFormat fmt = codecContext.getPixFmt();

		// read some frames
		int count = 0;
		while (format.readFrame(packet) >= 0) {
			System.out.println("read packet id = " + packet.getStreamIndex());
			try {
				// is this from the video stream?
				if (packet.getStreamIndex() == videoStream) {
					// decode video frame
					boolean frameFinished = codecContext.decodeVideo(frame, packet);

					if (frameFinished) {
						AVPlane plane = frame.getPlaneAt(0, fmt, width, height);

						System.out.printf("frame complete size = %d\n", plane.data.remaining());
						count++;
					}
				}
			} catch (AVDecodingError ex) {
				Logger.getLogger(VideoJavaReaderExample.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				packet.freePacket();
			}
		}
		
		format.closeInputStream();
		
		System.out.printf("Read %d frames\n", count);
	}
}
