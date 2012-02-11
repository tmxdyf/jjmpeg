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
import au.notzed.jjmpeg.AVInputFormat;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVPlane;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.io.JJFileInputStream;
import au.notzed.jjmpeg.util.JJFileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This tries to read from a Java file and feed it into ffmpeg.
 * 
 * @author notzed
 */
public class VideoJavaReaderExample {

	static File chooseFile() {
		JJFileChooser fc = new JJFileChooser();
		if (fc.showOpenDialog(null) == fc.APPROVE_OPTION) {
			return fc.getSelectedFile();
		} else {
			return null;
		}
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws FileNotFoundException, AVIOException {
		String name;

		if (args.length == 0) {
			File f = chooseFile();
			if (f == null) {
				return;
			}

			name = f.getPath();
		} else {
			name = args[0];
		}

		AVFormatContext.registerAll();

		FileInputStream fis = new FileInputStream(name);
		JJFileInputStream jjfis = JJFileInputStream.create(fis);

		AVInputFormat ifmt = jjfis.probeInput(name, 0, 4096);

		if (ifmt == null) {
			System.err.println("probe Input failed");
			System.exit(1);
		}

		AVFormatContext format = AVFormatContext.openInputStream(jjfis, name, ifmt);

		format.findStreamInfo();

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
		codecContext.open(codec);

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

		format.closeInput();

		System.out.printf("Read %d frames\n", count);
	}
}
