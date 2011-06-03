/*
 * Copyright (c) 2011 Michael Zucchi
 *
 * This file is part of jjmpeg, a java binding to ffmpeg's libraries.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.jjmpeg;

import au.notzed.jjmpeg.exception.AVDecodingError;
import au.notzed.jjmpeg.exception.AVEncodingError;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 *
 * @author notzed
 */
public class AVCodecContext extends AVCodecContextAbstract {

	public static final int AVMEDIA_TYPE_UNKNOWN = -1;
	public static final int AVMEDIA_TYPE_VIDEO = 0;
	public static final int AVMEDIA_TYPE_AUDIO = 1;
	public static final int AVMEDIA_TYPE_DATA = 2;
	public static final int AVMEDIA_TYPE_SUBTITLE = 3;
	public static final int AVMEDIA_TYPE_ATTACHMENT = 4;
	public static final int AVMEDIA_TYPE_NB = 5;

	private IntBuffer fin = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

	protected AVCodecContext(ByteBuffer p) {
		super(p);
	}

	static AVCodecContext create(ByteBuffer p) {
		return new AVCodecContext(p);
	}

	public void dispose() {
		AVFormatContext._free(p);
	}

	/**
	 * Returns true if decoding frame complete.
	 * @param frame
	 * @param packet
	 * @return
	 * @throws AVDecodingError
	 */
	public boolean decodeVideo(AVFrame frame, AVPacket packet) throws AVDecodingError {
		int res;

		res = _decode_video2(frame.p, fin, packet.p);
		if (res < 0) {
			throw new AVDecodingError(-res);
		}

		return (fin.get(0) != 0);
	}

	/**
	 * Encode video, writing result to buf.
	 *
	 * Note that it always writes to the start of the buffer, ignoring the position and limit.
	 * @param buf
	 * @param pict Picture to encode, use null to flush encoded frames.
	 * @return number of bytes written.  When 0 with a null picture, encoding is complete.
	 * @throws au.notzed.jjmpeg.exception.AVEncodingError
	 */
	public int encodeVideo(ByteBuffer buf, AVFrame pict) throws AVEncodingError {
		int buf_size = buf.capacity();
		int len = _encode_video(buf, buf_size, pict != null ? pict.p : null);

		if (len >= 0) {
			buf.limit(len);
			buf.position(0);
			return len;
		} else {
			throw new AVEncodingError(-len);
		}
	}
}
