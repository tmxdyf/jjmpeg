package jjmpeg;

import au.notzed.jjmpeg.AVCodec;
import au.notzed.jjmpeg.AVCodecContext;
import au.notzed.jjmpeg.AVFormatContext;
import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVPlane;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.PixelFormat;

/**
 *
 * @author notzed
 */
public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		String name = "/home/notzed/Videos/mike0.avi";

		AVCodecContext.registerAll();
		AVFormatContext format = AVFormatContext.openInputFile(name);

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
			} finally {
				packet.freePacket();
			}
		}
		System.out.printf("Read %d frames\n", count);
	}
}
