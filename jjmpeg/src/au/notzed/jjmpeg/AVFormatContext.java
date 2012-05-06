/*
 * Copyright (c) 2011 Michael Zucchi
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
package au.notzed.jjmpeg;

import au.notzed.jjmpeg.exception.AVIOException;

/**
 *
 * @author notzed
 */
public class AVFormatContext extends AVFormatContextAbstract {

	public static final int AVSEEK_FLAG_BACKWARD = 1; ///< seek backward
	public static final int AVSEEK_FLAG_BYTE = 2; ///< seeking based on position in bytes
	public static final int AVSEEK_FLAG_ANY = 4; ///< seek to any frame, even non-keyframes
	public static final int AVSEEK_FLAG_FRAME = 8; ///< seeking based on frame number

	AVFormatContext(int p) {
		setNative(new AVFormatContextNative32(this, p, 0));
	}

	AVFormatContext(long p) {
		setNative(new AVFormatContextNative64(this, p, 0));
	}

	//AVFormatContext(long p, int type) {
	//	setNative(new AVFormatContextNative(this, p, type));
	//}
	//static AVFormatContext create(ByteBuffer p) {
	//	return new AVFormatContext(p, 0);
	//}
	//static AVFormatContext create(ByteBuffer p, int type) {
	//	return new AVFormatContext(p, type);
	//}
	static public AVFormatContext open(String name) throws AVIOException {
		AVFormatContext obj = new AVFormatContext(0);

		int err = AVFormatContextNative.open_input(obj.n, name, null, null);

		if (err != 0) {
			throw new AVIOException(err, "Opening: " + name);
		}

		return obj;
	}

	/**
	 * This form allows the AVIOContext to be set manually.
	 *
	 * TODO: I don't pass the dictionary because for some inane reason the api
	 * is an in/out parameter ...
	 *
	 * @param name
	 * @param fmt
	 * @throws AVIOException
	 */
	public void openInput(String name, AVInputFormat fmt) throws AVIOException {
		int err = AVFormatContextNative.open_input(n, name, fmt != null ? fmt.n : null, null);

		if (err != 0) {
			throw new AVIOException(err, "Opening: " + name);
		}
	}

	public void findStreamInfo(AVDictionary[] options) throws AVIOException {
		AVDictionaryNative[] noptions = null;

		if (options != null && options.length > 0) {
			noptions = new AVDictionaryNative[options.length];
			for (int i = 0; i < options.length; i++) {
				noptions[i] = options[i].n;
			}
		}
		int res = n.findStreamInfo(options);

		if (res < 0) {
			throw new AVIOException(res);
		}

		if (noptions != null) {
			for (int i = 0; i < options.length; i++) {
				options[i].n = noptions[i];
			}
		}
	}

	public void findStreamInfo() throws AVIOException {
		findStreamInfo(null);
	}

	public void interleavedWriteFrame(AVPacket pkt) throws AVIOException {
		int res = n.interleaved_write_frame(pkt.n);

		if (res < 0) {
			throw new AVIOException("error writing frame");
		}
	}

	public void writeHeader(AVDictionary dict) throws AVIOException {
		n.write_header(dict != null ? dict.n : null);
	}

	@Override
	public int readFrame(AVPacket packet) {
		int res = super.readFrame(packet);

		if (res < 0) {
			switch (res) {
				case -32: // EPIPE
				// the superclass binding makes this a pain , not sure how to fix
				//		throw new AVIOException(-res);
				case -1: // EOF
					break;
				default:
					System.out.printf("some error reading frame %d\n", res);
					break;
			}
		}

		return res;
	}

	public void closeInput() {
		dispose();
	}
}

class AVFormatContextNative extends AVFormatContextNativeAbstract {

	public AVFormatContextNative(AVObject o) {
		super(o);
	}

	native int findStreamInfo(Object[] options);

	static native int open_input(AVFormatContextNative ps, String filename, AVInputFormatNative fmt, AVDictionary options);

	static native void close_input(AVFormatContextNative s);

	native int write_header(AVDictionaryNative options);
}

class AVFormatContextNative32 extends AVFormatContextNative {

	int p;
	final int type;

	AVFormatContextNative32(AVObject o, int p, int type) {
		super(o);
		this.p = p;
		this.type = type;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			switch (type) {
				case 0:
					this.free_context();
					break;
				case 1:
					//close_input_file(p);
					break;
				case 2:
					//close_input_stream(p);
					break;
				case 3:
					close_input(this);
					break;
			}
			super.dispose();
			p = 0;
		}
	}
}

class AVFormatContextNative64 extends AVFormatContextNative {

	long p;
	final int type;

	AVFormatContextNative64(AVObject o, long p, int type) {
		super(o);
		this.p = p;
		this.type = type;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			switch (type) {
				case 0:
					this.free_context();
					break;
				case 1:
					//close_input_file(p);
					break;
				case 2:
					//close_input_stream(p);
					break;
				case 3:
					close_input(this);
					break;
			}
			super.dispose();
			p = 0;
		}
	}
}
