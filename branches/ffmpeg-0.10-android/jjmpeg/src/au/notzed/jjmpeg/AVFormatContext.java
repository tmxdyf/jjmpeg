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
import java.nio.ByteBuffer;

/**
 *
 * @author notzed
 */
public class AVFormatContext extends AVFormatContextAbstract {

	public static final int AVSEEK_FLAG_BACKWARD = 1; ///< seek backward
	public static final int AVSEEK_FLAG_BYTE = 2; ///< seeking based on position in bytes
	public static final int AVSEEK_FLAG_ANY = 4; ///< seek to any frame, even non-keyframes
	public static final int AVSEEK_FLAG_FRAME = 8; ///< seeking based on frame number

	protected AVFormatContext(ByteBuffer p, int type) {
		setNative(new AVFormatContextNative(this, p, type));
	}

	static AVFormatContext create(ByteBuffer p) {
		return new AVFormatContext(p, 0);
	}

	static AVFormatContext create(ByteBuffer p, int type) {
		return new AVFormatContext(p, type);
	}

	static public AVFormatContext open(String name) throws AVIOException {
		ObjectHolder obj = new ObjectHolder(null);

		int err = AVFormatContextNative.open_input(obj, name, null, null);

		if (err != 0) {
			throw new AVIOException(err, "Opening: " + name);
		}

		return create((ByteBuffer) obj.value, 3);
	}

	/**
	 * This form allows the AVIOContext to be set manually.
	 *
	 * TODO: I don't pass the dictionary because for some inane reason the api
	 * is an in/out parameter ...
	 * @param name
	 * @param fmt
	 * @throws AVIOException
	 */
	public void openInput(String name, AVInputFormat fmt) throws AVIOException {
		ObjectHolder obj = new ObjectHolder(this.n.p);
		int err = AVFormatContextNative.open_input(obj, name, fmt != null ? fmt.n.p : null, null);

		if (err != 0) {
			throw new AVIOException(err, "Opening: " + name);
		}
	}

	public void findStreamInfo(AVDictionary[] options) throws AVIOException {
		ByteBuffer[] noptions = null;

		if (options != null && options.length > 0) {
			noptions = new ByteBuffer[options.length];
			for (int i = 0; i < options.length; i++) {
				noptions[i] = options[i].n.p;
			}
		}
		int res = AVFormatContextNative.findStreamInfo(n.p, options);

		if (res < 0) {
			throw new AVIOException(res);
		}

		if (noptions != null) {
			for (int i = 0; i < options.length; i++) {
				options[i].n.p = noptions[i];
			}
		}
	}

	public void findStreamInfo() throws AVIOException {
		findStreamInfo(null);
	}

	public void interleavedWriteFrame(AVPacket pkt) throws AVIOException {
		int res = AVFormatContextNativeAbstract.interleaved_write_frame(n.p, pkt.n.p);

		if (res < 0) {
			throw new AVIOException("error writing frame");
		}
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

	private final int type;

	AVFormatContextNative(AVObject o, ByteBuffer p, int type) {
		super(o, p);
		this.type = type;
	}

	@Override
	public void dispose() {
		if (p != null) {
			switch (type) {
				case 0:
					free_context(p);
					break;
				case 1:
					//close_input_file(p);
					break;
				case 2:
					//close_input_stream(p);
					break;
				case 3:
					close_input(new ObjectHolder(p));
			}
			super.dispose();
		}
	}

	static native int findStreamInfo(ByteBuffer p, Object[] options);
}
